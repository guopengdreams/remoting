package com.gpd.remoting.transport.dispatcher.message;

import java.util.concurrent.ExecutorService;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.ExecutionException;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.transport.dispatcher.ChannelEventRunnable;
import com.gpd.remoting.transport.dispatcher.WrappedChannelHandler;
import com.gpd.remoting.transport.dispatcher.ChannelEventRunnable.ChannelState;

public class MessageOnlyChannelHandler extends WrappedChannelHandler {

  public MessageOnlyChannelHandler(ChannelHandler handler, URL url) {
    super(handler, url);
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

  private ExecutorService getExecutorService() {
    ExecutorService cexecutor = executor;
    if (cexecutor == null || cexecutor.isShutdown()) {
      cexecutor = SHARED_EXECUTOR;
    }
    return cexecutor;
  }
}
