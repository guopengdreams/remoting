package com.gpd.remoting.transport;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.Client;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.common.utils.NamedThreadFactory;
import com.gpd.remoting.common.utils.NetUtils;
import com.gpd.remoting.exchange.DefaultFuture;
import com.gpd.remoting.exchange.Request;
import com.gpd.remoting.exchange.ResponseFuture;

public abstract class AbstractClient extends AbstractPeer implements Client {
  private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);
  private final Lock connectLock = new ReentrantLock();
  private static final ScheduledThreadPoolExecutor reconnectExecutorService =
      new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("RemotingClientReconnectTimer",
          true));
  private volatile ScheduledFuture<?> reconnectExecutorFuture = null;

  public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
    super(url, handler);
    try {
      doOpen();
    } catch (Throwable t) {
      close();
      throw new RemotingException("Failed to start " + getClass().getSimpleName() + " "
          + " connect to the server " + ", cause: " + t.getMessage(), t);
    }
    try {
      connect();
      if (logger.isInfoEnabled()) {
        logger.info("Start " + getClass().getSimpleName() + " " + " connect to the server ");
      }
    } catch (RemotingException t) {
      close();
      throw new RemotingException("Failed to start " + getClass().getSimpleName() + " "
          + " connect to the server " + ", cause: " + t.getMessage(), t);

    } catch (Throwable t) {
      close();
      throw new RemotingException("Failed to start " + getClass().getSimpleName() + " "
          + " connect to the server " + ", cause: " + t.getMessage(), t);
    }
  }

  /**
   * init reconnect thread
   */
  private synchronized void initConnectStatusCheckCommand() {
    // reconnect=false to close reconnect
    int reconnect = getReconnectParam(getUrl());
    if (reconnect > 0 && (reconnectExecutorFuture == null || reconnectExecutorFuture.isCancelled())) {
      Runnable connectStatusCheckCommand = new Runnable() {
        @Override
        public void run() {
          try {
            if (!isConnected()) {
              connect();
            }
          } catch (Throwable t) {
            logger.error("client reconnect to " + " find error .", t);
          }
        }
      };
      reconnectExecutorFuture =
          reconnectExecutorService.scheduleWithFixedDelay(connectStatusCheckCommand, reconnect,
              reconnect, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * @param url
   * @return 0-false
   */
  private static int getReconnectParam(URL url) {
    int reconnect;
    String param = url.getParameter(Constants.RECONNECT_KEY);
    if (param == null || param.length() == 0 || "true".equalsIgnoreCase(param)) {
      reconnect = Constants.DEFAULT_RECONNECT_PERIOD;
    } else if ("false".equalsIgnoreCase(param)) {
      reconnect = 0;
    } else {
      try {
        reconnect = Integer.parseInt(param);
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "reconnect param must be nonnegative integer or false/true. input is:" + param);
      }
      if (reconnect < 0) {
        throw new IllegalArgumentException(
            "reconnect param must be nonnegative integer or false/true. input is:" + param);
      }
    }
    return reconnect;
  }

  private synchronized void destroyConnectStatusCheckCommand() {
    try {
      if (reconnectExecutorFuture != null && !reconnectExecutorFuture.isDone()) {
        reconnectExecutorFuture.cancel(true);
        reconnectExecutorService.purge();
      }
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }
  }

  protected InetSocketAddress getConnectAddress() {
    return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    Channel channel = getChannel();
    if (channel == null) return getUrl().toInetSocketAddress();
    return channel.getRemoteAddress();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    Channel channel = getChannel();
    if (channel == null) return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
    return channel.getLocalAddress();
  }

  @Override
  public boolean isConnected() {
    Channel channel = getChannel();
    if (channel == null) return false;
    return channel.isConnected();
  }

  @Override
  public void send(Object message, boolean sent) throws RemotingException {
    if (!isConnected()) {
      connect();
    }
    Channel channel = getChannel();
    if (channel == null || !channel.isConnected()) {
      throw new RemotingException("message can not send, because channel is inactive . url:"
          + getUrl());
    }
    channel.send(message, sent);
  }

  public ResponseFuture request(Object request) throws RemotingException {
    return request(request,
        getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
  }

  public ResponseFuture request(Object request, int timeout) throws RemotingException {
    if (!isConnected()) {
      connect();
    }

    Channel channel = getChannel();
    if (channel == null || !channel.isConnected()) {
      throw new RemotingException("message can not send, because channel is inactive . url:"
          + getUrl());
    }

    // create request.
    Request req = new Request();
    req.setData(request);
    DefaultFuture future = new DefaultFuture(getChannel(), req, timeout);
    try {
      this.send(req);
    } catch (RemotingException e) {
      future.cancel();
      throw e;
    }
    return future;
  }

  private void connect() throws RemotingException {
    connectLock.lock();
    try {
      if (isConnected()) {
        return;
      }
      initConnectStatusCheckCommand();
      doConnect();
      if (!isConnected()) {
        throw new RemotingException("Failed connect to server "
            + " from "
            + getClass().getSimpleName()
            + " "
            + ", cause: Connect wait timeout: "
            + getUrl().getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY,
                Constants.DEFAULT_CONNECT_TIMEOUT) + "ms.");
      } else {
        if (logger.isInfoEnabled()) {
          logger.info("Successed connect to server " + " from " + getClass().getSimpleName() + " "
              + ", channel is " + getChannel());
        }
      }
    } catch (RemotingException e) {
      throw e;
    } catch (Throwable e) {
      throw new RemotingException("Failed connect to server " + " from "
          + getClass().getSimpleName() + " " + ", cause: " + e.getMessage(), e);
    } finally {
      connectLock.unlock();
    }
  }

  public void disconnect() {
    connectLock.lock();
    try {
      destroyConnectStatusCheckCommand();
      try {
        Channel channel = getChannel();
        if (channel != null) {
          channel.close();
        }
      } catch (Throwable e) {
        logger.warn(e.getMessage(), e);
      }
      try {
        doDisConnect();
      } catch (Throwable e) {
        logger.warn(e.getMessage(), e);
      }
    } finally {
      connectLock.unlock();
    }
  }

  @Override
  public void reconnect() throws RemotingException {
    disconnect();
    connect();
  }

  @Override
  public void close() {
    try {
      disconnect();
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }
    try {
      doClose();
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }
  }

  @Override
  public String toString() {
    return getClass().getName() + " [" + getLocalAddress() + " -> " + getRemoteAddress() + "]";
  }

  /**
   * Open client.
   * 
   * @throws Throwable
   */
  protected abstract void doOpen() throws Throwable;

  /**
   * Close client.
   * 
   * @throws Throwable
   */
  protected abstract void doClose() throws Throwable;

  /**
   * Connect to server.
   * 
   * @throws Throwable
   */
  protected abstract void doConnect() throws Throwable;

  /**
   * disConnect to server.
   * 
   * @throws Throwable
   */
  protected abstract void doDisConnect() throws Throwable;

  /**
   * Get the connected channel.
   * 
   * @return channel
   */
  protected abstract Channel getChannel();

}
