package com.gpd.remoting;

import java.net.InetSocketAddress;

public interface Channel extends Endpoint {

  /**
   * get remote address.
   * 
   * @return remote address.
   */
  InetSocketAddress getRemoteAddress();

  /**
   * is connected.
   * 
   * @return connected
   */
  boolean isConnected();
}
