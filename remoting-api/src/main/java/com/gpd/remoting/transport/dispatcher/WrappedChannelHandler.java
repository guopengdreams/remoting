package com.gpd.remoting.transport.dispatcher;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.common.threadpool.ThreadPoolFactory;
import com.gpd.remoting.common.utils.NamedThreadFactory;

public class WrappedChannelHandler implements ChannelHandler {

  protected static final Logger logger = LoggerFactory.getLogger(WrappedChannelHandler.class);

  protected static final ExecutorService SHARED_EXECUTOR = Executors
      .newCachedThreadPool(new NamedThreadFactory("NettySharedHandler", true));

  protected final ExecutorService executor;

  protected final ChannelHandler handler;

  protected final URL url;

  public WrappedChannelHandler(ChannelHandler handler, URL url) {
    if (handler == null) {
      throw new IllegalArgumentException("handler == null");
    }
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    this.handler = handler;
    this.url = url;
    executor =
        (ExecutorService) ThreadPoolFactory.getThreadPool(
            url.getParameter(Constants.THREADPOOL_KEY, Constants.DEFAULT_THREADPOOL)).getExecutor(
            url);
  }

  public void close() {
    try {
      if (executor instanceof ExecutorService) {
        executor.shutdown();
      }
    } catch (Throwable t) {
      logger.warn("fail to destroy thread pool of server: " + t.getMessage(), t);
    }
  }

  @Override
  public void connected(Channel channel) throws RemotingException {
    handler.connected(channel);
  }

  @Override
  public void disconnected(Channel channel) throws RemotingException {
    handler.disconnected(channel);
  }

  @Override
  public void sent(Channel channel, Object message) throws RemotingException {
    handler.sent(channel, message);
  }

  @Override
  public void received(Channel channel, Object message) throws RemotingException {
    handler.received(channel, message);
  }

  @Override
  public void caught(Channel channel, Throwable exception) throws RemotingException {
    handler.caught(channel, exception);
  }

  public ExecutorService getExecutor() {
    return executor;
  }

  public URL getUrl() {
    return url;
  }

}
