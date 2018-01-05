package com.gpd.remoting.exchange;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gpd.remoting.Channel;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.TimeoutException;
import com.gpd.remoting.common.Constants;

/**
 * DefaultFuture.
 * 
 */
public class DefaultFuture implements ResponseFuture {
  private static final Logger logger = LoggerFactory.getLogger(DefaultFuture.class);

  private static final Map<Long, Channel> CHANNELS = new ConcurrentHashMap<Long, Channel>();

  private static final Map<Long, DefaultFuture> FUTURES =
      new ConcurrentHashMap<Long, DefaultFuture>();

  // invoke id.
  private final long id;

  private final Channel channel;

  private final Request request;

  private final int timeout;

  private final Lock lock = new ReentrantLock();

  private final Condition done = lock.newCondition();

  private final long start = System.currentTimeMillis();

  private volatile long sent;

  private volatile Response response;

  private volatile ResponseCallback callback;

  public DefaultFuture(Channel channel, Request request, int timeout) {
    this.channel = channel;
    this.request = request;
    this.id = request.getId();
    // TODO 缺少通过URL获取参数
    this.timeout = timeout > 0 ? timeout : Constants.DEFAULT_TIMEOUT;
    // put into waiting map.
    FUTURES.put(id, this);
    CHANNELS.put(id, channel);
  }

  @Override
  public Object get() throws RemotingException {
    return get(timeout);
  }

  @Override
  public Object get(int timeout) throws RemotingException {
    if (timeout <= 0) {
      timeout = Constants.DEFAULT_TIMEOUT;
    }
    if (!isDone()) {
      long start = System.currentTimeMillis();
      lock.lock();
      try {
        while (!isDone()) {
          done.await(timeout, TimeUnit.MILLISECONDS);
          if (isDone() || System.currentTimeMillis() - start > timeout) {
            break;
          }
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        lock.unlock();
      }
      if (!isDone()) {
        throw new TimeoutException(sent > 0, getTimeoutMessage(false));
      }
    }
    return returnFromResponse();
  }

  public void cancel() {
    Response errorResult = new Response(id);
    errorResult.setErrorMessage("request future has been canceled.");
    response = errorResult;
    FUTURES.remove(id);
    CHANNELS.remove(id);
  }

  @Override
  public boolean isDone() {
    return response != null;
  }

  @Override
  public void setCallback(ResponseCallback callback) {
    if (isDone()) {
      invokeCallback(callback);
    } else {
      boolean isdone = false;
      lock.lock();
      try {
        if (!isDone()) {
          this.callback = callback;
        } else {
          isdone = true;
        }
      } finally {
        lock.unlock();
      }
      if (isdone) {
        invokeCallback(callback);
      }
    }
  }

  private void invokeCallback(ResponseCallback c) {
    ResponseCallback callbackCopy = c;
    if (callbackCopy == null) {
      throw new NullPointerException("callback cannot be null.");
    }
    c = null;
    Response res = response;
    if (res == null) {
      // TODO throw new IllegalStateException("response cannot be null. url:" + channel.getUrl());
      throw new IllegalStateException("response cannot be null. url:");
    }

    if (res.getStatus() == Response.OK) {
      try {
        callbackCopy.done(res.getResult());
      } catch (Exception e) {
        // logger.error(
        // "callback invoke error .reasult:" + res.getResult() + ",url:" + channel.getUrl(), e);
        logger.error("callback invoke error .reasult:" + res.getResult() + ",url:", e);
      }
    } else if (res.getStatus() == Response.CLIENT_TIMEOUT
        || res.getStatus() == Response.SERVER_TIMEOUT) {
      try {
        TimeoutException te =
            new TimeoutException(res.getStatus() == Response.SERVER_TIMEOUT, res.getErrorMessage());
        callbackCopy.caught(te);
      } catch (Exception e) {
        // logger.error("callback invoke error ,url:" + channel.getUrl(), e);
        logger.error("callback invoke error ,url:", e);
      }
    } else {
      try {
        RuntimeException re = new RuntimeException(res.getErrorMessage());
        callbackCopy.caught(re);
      } catch (Exception e) {
        // logger.error("callback invoke error ,url:" + channel.getUrl(), e);
        logger.error("callback invoke error ,url:", e);
      }
    }
  }

  private Object returnFromResponse() throws RemotingException {
    Response res = response;
    if (res == null) {
      throw new IllegalStateException("response cannot be null");
    }
    if (res.getStatus() == Response.OK) {
      return res.getResult();
    }
    if (res.getStatus() == Response.CLIENT_TIMEOUT || res.getStatus() == Response.SERVER_TIMEOUT) {
      throw new TimeoutException(res.getStatus() == Response.SERVER_TIMEOUT, res.getErrorMessage());
    }
    throw new RemotingException(res.getErrorMessage());
  }

  private long getId() {
    return id;
  }

  private Channel getChannel() {
    return channel;
  }

  private boolean isSent() {
    return sent > 0;
  }

  public Request getRequest() {
    return request;
  }

  private int getTimeout() {
    return timeout;
  }

  private long getStartTimestamp() {
    return start;
  }

  public static DefaultFuture getFuture(long id) {
    return FUTURES.get(id);
  }

  public static boolean hasFuture(Channel channel) {
    return CHANNELS.containsValue(channel);
  }

  public static void sent(Channel channel, Request request) {
    DefaultFuture future = FUTURES.get(request.getId());
    if (future != null) {
      future.doSent();
    }
  }

  private void doSent() {
    sent = System.currentTimeMillis();
  }

  private void doReceived(Response res) {
    lock.lock();
    try {
      response = res;
      if (done != null) {
        done.signal();
      }
    } finally {
      lock.unlock();
    }
    if (callback != null) {
      invokeCallback(callback);
    }
  }

  private String getTimeoutMessage(boolean scan) {
    long nowTimestamp = System.currentTimeMillis();
    return (sent > 0
        ? "Waiting server-side response timeout"
        : "Sending request timeout in client-side")
        + (scan ? " by scan timer" : "")
        + ". start time: "
        + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(start)))
        + ", end time: "
        + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
        + ","
        + (sent > 0 ? " client elapsed: " + (sent - start) + " ms, server elapsed: "
            + (nowTimestamp - sent) : " elapsed: " + (nowTimestamp - start))
        + " ms, timeout: "
        + timeout
        + " ms, request: "
        + request
        + ", channel: "
        + channel.getLocalAddress()
        + " -> "
        + channel.getRemoteAddress();
  }

  public static void received(Channel channel, Response response) {
    try {
      DefaultFuture future = FUTURES.remove(response.getId());
      if (future != null) {
        future.doReceived(response);
      } else {
        logger.warn("The timeout response finally returned at "
            + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
            + ", response "
            + response
            + (channel == null ? "" : ", channel: " + channel.getLocalAddress() + " -> "
                + channel.getRemoteAddress()));
      }
    } finally {
      CHANNELS.remove(response.getId());
    }
  }

  private static class RemotingInvocationTimeoutScan implements Runnable {

    @Override
    public void run() {
      while (true) {
        try {
          for (DefaultFuture future : FUTURES.values()) {
            if (future == null || future.isDone()) {
              continue;
            }
            if (System.currentTimeMillis() - future.getStartTimestamp() > future.getTimeout()) {
              // create exception response.
              Response timeoutResponse = new Response(future.getId());
              // set timeout status.
              timeoutResponse.setStatus(future.isSent()
                  ? Response.SERVER_TIMEOUT
                  : Response.CLIENT_TIMEOUT);
              timeoutResponse.setErrorMessage(future.getTimeoutMessage(true));
              // handle response.
              DefaultFuture.received(future.getChannel(), timeoutResponse);
            }
          }
          Thread.sleep(30);
        } catch (Throwable e) {
          logger.error("Exception when scan the timeout invocation of remoting.", e);
        }
      }
    }
  }

  static {
    Thread th = new Thread(new RemotingInvocationTimeoutScan(), "ResponseTimeoutScanTimer");
    th.setDaemon(true);
    th.start();
  }

}
