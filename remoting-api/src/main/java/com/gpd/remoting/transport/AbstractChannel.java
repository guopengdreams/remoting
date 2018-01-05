package com.gpd.remoting.transport;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.common.URL;

public abstract class AbstractChannel extends AbstractPeer implements Channel {
  public AbstractChannel(URL url, ChannelHandler handler) {
    super(url, handler);
  }

  @Override
  public String toString() {
    return getLocalAddress() + " -> " + getRemoteAddress();
  }
}
