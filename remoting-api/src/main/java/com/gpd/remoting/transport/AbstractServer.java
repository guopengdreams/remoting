package com.gpd.remoting.transport;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.Server;
import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.common.utils.ExecutorUtil;
import com.gpd.remoting.common.utils.NetUtils;
import com.gpd.remoting.transport.dispatcher.WrappedChannelHandler;

/**
 * AbstractServer
 */
public abstract class AbstractServer extends AbstractPeer implements Server {
  private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);
  private final InetSocketAddress localAddress;
  private final InetSocketAddress bindAddress;
  private final int accepts;
  ExecutorService executor;

  public AbstractServer(URL url, ChannelHandler handler) throws RemotingException {
    super(url, handler);
    localAddress = getUrl().toInetSocketAddress();
    String host =
        NetUtils.isInvalidLocalHost(getUrl().getHost()) ? NetUtils.ANYHOST : getUrl().getHost();
    bindAddress = new InetSocketAddress(host, getUrl().getPort());
    this.accepts = url.getParameter(Constants.ACCEPTS_KEY, Constants.DEFAULT_ACCEPTS);

    try {
      doOpen();
      if (logger.isInfoEnabled()) {
        logger.info("Start " + getClass().getSimpleName() + " bind " + getBindAddress()
            + ", export " + getLocalAddress());
      }
    } catch (Throwable t) {
      throw new RemotingException("Failed to bind " + getClass().getSimpleName() + " on "
          + getLocalAddress() + ", cause: " + t.getMessage(), t);
    }

    if (handler instanceof WrappedChannelHandler) {
      executor = ((WrappedChannelHandler) handler).getExecutor();
    }
  }

  protected abstract void doOpen() throws Throwable;

  protected abstract void doClose() throws Throwable;

  @Override
  public void send(Object message, boolean sent) throws RemotingException {
    Collection<Channel> channels = getChannels();
    for (Channel channel : channels) {
      if (channel.isConnected()) {
        channel.send(message, sent);
      }
    }
  }

  @Override
  public void close() {
    logger.info("Close " + getClass().getSimpleName() + " bind " + getBindAddress() + ", export "
        + getLocalAddress());

    ExecutorUtil.shutdownNow(executor, 100);

    try {
      doClose();
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }
  }

  public void close(int timeout) {
    ExecutorUtil.gracefulShutdown(executor, timeout);
    close();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return localAddress;
  }

  public InetSocketAddress getBindAddress() {
    return bindAddress;
  }

  public int getAccepts() {
    return accepts;
  }

  @Override
  public void connected(Channel ch) throws RemotingException {
    Collection<Channel> channels = getChannels();
    if (accepts > 0 && channels.size() > accepts) {
      logger.error("Close channel " + ch + ", cause: The server " + ch.getLocalAddress()
          + " connections greater than max config " + accepts);
      ch.close();
      return;
    }
    super.connected(ch);
  }

  @Override
  public void disconnected(Channel ch) throws RemotingException {
    Collection<Channel> channels = getChannels();
    if (channels.size() == 0) {
      logger.warn("All clients has discontected from " + getLocalAddress()
          + ". You can graceful shutdown now.");
    }
    super.disconnected(ch);
  }
}
