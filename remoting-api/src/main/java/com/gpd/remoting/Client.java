package com.gpd.remoting;


public interface Client extends Endpoint, Channel {
  /**
   * reconnect.
   */
  void reconnect() throws RemotingException;
}
