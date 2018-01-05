package com.gpd.remoting.transport.dispatcher.base;

import java.util.concurrent.ExecutorService;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.ExecutionException;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.transport.dispatcher.ChannelEventRunnable;
import com.gpd.remoting.transport.dispatcher.WrappedChannelHandler;
import com.gpd.remoting.transport.dispatcher.ChannelEventRunnable.ChannelState;

/**
 * 默认的线程池配置
 * 
 */
public class DefaultChannelHandler extends WrappedChannelHandler {

  public DefaultChannelHandler(ChannelHandler handler, URL url) {
    super(handler, url);
  }

  @Override
  public void connected(Channel channel) throws RemotingException {
    ExecutorService cexecutor = getExecutorService();
    try {
      cexecutor.execute(new ChannelEventRunnable(channel, handler, ChannelState.CONNECTED));
    } catch (Throwable t) {
      throw new ExecutionException("connect event", getClass()
          + " error when process connected event .", t);
    }
  }

  @Override
  public void disconnected(Channel channel) throws RemotingException {
    ExecutorService cexecutor = getExecutorService();
    try {
      cexecutor.execute(new ChannelEventRunnable(channel, handler, ChannelState.DISCONNECTED));
    } catch (Throwable t) {
      throw new ExecutionException("disconnect event", getClass()
          + " error when process disconnected event .", t);
    }
  }

  @Override
  public void received(Channel channel, Object message) throws RemotingException {
    ExecutorService cexecutor = getExecutorService();
    try {
      cexecutor.execute(new ChannelEventRunnable(channel, handler, ChannelState.RECEIVED, message));
    } catch (Throwable t) {
      throw new ExecutionException(message, getClass() + " error when process received event .", t);
    }
  }

  @Override
  public void caught(Channel channel, Throwable exception) throws RemotingException {
    ExecutorService cexecutor = getExecutorService();
    try {
      cexecutor.execute(new ChannelEventRunnable(channel, handler, ChannelState.CAUGHT, exception));
    } catch (Throwable t) {
      throw new ExecutionException("caught event", getClass()
          + " error when process caught event .", t);
    }
  }

  private ExecutorService getExecutorService() {
    ExecutorService cexecutor = executor;
    if (cexecutor == null || cexecutor.isShutdown()) {
      cexecutor = SHARED_EXECUTOR;
    }
    return cexecutor;
  }
}
