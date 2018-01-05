package com.gpd.remoting;

import java.net.InetSocketAddress;

import com.gpd.remoting.common.URL;

public interface Endpoint {

  /**
   * get url.
   * 
   * @return url
   */
  URL getUrl();

  /**
   * get channel handler.
   * 
   * @return channel handler
   */
  ChannelHandler getChannelHandler();

  /**
   * get local address.
   * 
   * @return local address.
   */
  InetSocketAddress getLocalAddress();

  /**
   * send message.
   * 
   * @param message
   * @throws RemotingException
   */
  void send(Object message) throws RemotingException;

  /**
   * send message.
   * 
   * @param message
   * @param sent 是否已发送完成
   */
  void send(Object message, boolean sent) throws RemotingException;

  /**
   * close the channel.
   */
  void close();
}
