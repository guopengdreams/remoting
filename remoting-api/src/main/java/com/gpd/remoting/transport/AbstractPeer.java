package com.gpd.remoting.transport;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.Endpoint;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;

public abstract class AbstractPeer implements Endpoint, ChannelHandler {
  private final ChannelHandler handler;
  private volatile URL url;

  public AbstractPeer(URL url, ChannelHandler handler) {
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    if (handler == null) {
      throw new IllegalArgumentException("handler == null");
    }
    this.url = url;
    this.handler = handler;
  }

  @Override
  public void send(Object message) throws RemotingException {
    send(message, url.getParameter(Constants.SENT_KEY, false));
  }

  @Override
  public URL getUrl() {
    return url;
  }

  @Override
  public ChannelHandler getChannelHandler() {
    return handler;
  }

  @Override
  public void connected(Channel ch) throws RemotingException {
    handler.connected(ch);
  }

  @Override
  public void disconnected(Channel ch) throws RemotingException {
    handler.disconnected(ch);
  }

  @Override
  public void sent(Channel ch, Object msg) throws RemotingException {
    handler.sent(ch, msg);
  }

  @Override
  public void received(Channel ch, Object msg) throws RemotingException {
    handler.received(ch, msg);
  }

  @Override
  public void caught(Channel ch, Throwable ex) throws RemotingException {
    handler.caught(ch, ex);
  }
}
