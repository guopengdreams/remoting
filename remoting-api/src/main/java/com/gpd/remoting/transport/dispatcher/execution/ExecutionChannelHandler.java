package com.gpd.remoting.transport.dispatcher.execution;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.transport.dispatcher.ChannelEventRunnable;
import com.gpd.remoting.transport.dispatcher.WrappedChannelHandler;
import com.gpd.remoting.transport.dispatcher.ChannelEventRunnable.ChannelState;

public class ExecutionChannelHandler extends WrappedChannelHandler {

  public ExecutionChannelHandler(ChannelHandler handler, URL url) {
    super(handler, url);
  }

  @Override
  public void connected(Channel channel) throws RemotingException {
    executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.CONNECTED));
  }

  @Override
  public void disconnected(Channel channel) throws RemotingException {
    executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.DISCONNECTED));
  }

  @Override
  public void received(Channel channel, Object message) throws RemotingException {
    executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.RECEIVED, message));
  }

  @Override
  public void caught(Channel channel, Throwable exception) throws RemotingException {
    executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.CAUGHT, exception));
  }

}
