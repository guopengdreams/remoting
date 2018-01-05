package com.gpd.remoting.transport.netty4;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.transport.AbstractChannel;

/**
 * NettyChannel.
 * 
 */
final class NettyChannel extends AbstractChannel {

  private static final Logger logger = LoggerFactory.getLogger(NettyChannel.class);

  private static final ConcurrentMap<Channel, NettyChannel> channelMap =
      new ConcurrentHashMap<Channel, NettyChannel>();

  private final Channel channel;

  private NettyChannel(Channel channel, URL url, ChannelHandler handler) {
    super(url, handler);
    if (channel == null) {
      throw new IllegalArgumentException("netty channel == null;");
    }
    this.channel = channel;
  }

  static NettyChannel getOrAddChannel(Channel ch, URL url, ChannelHandler handler) {
    if (ch == null) {
      return null;
    }
    NettyChannel ret = channelMap.get(ch);
    if (ret == null) {
      NettyChannel nc = new NettyChannel(ch, url, handler);
      if (ch.isActive()) {
        ret = channelMap.putIfAbsent(ch, nc);
      }
      if (ret == null) {
        ret = nc;
      }
    }
    return ret;
  }

  static void removeChannelIfDisconnected(Channel ch) {
    if (ch != null && !ch.isActive()) {
      channelMap.remove(ch);
    }
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return (InetSocketAddress) channel.localAddress();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) channel.remoteAddress();
  }

  @Override
  public boolean isConnected() {
    return channel.isActive();
  }

  @Override
  public void send(Object message, boolean sent) throws RemotingException {
    boolean success = true;
    int timeout = 0;
    try {
      ChannelFuture future = channel.writeAndFlush(message);
      if (sent) {
        timeout = getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        success = future.syncUninterruptibly().await(timeout);
      }
      Throwable cause = future.cause();
      if (cause != null) {
        throw cause;
      }
    } catch (Throwable e) {
      throw new RemotingException("Failed to send message " + message + " to " + getRemoteAddress()
          + ", cause: " + e.getMessage(), e);
    }

    if (!success) {
      throw new RemotingException("Failed to send message " + message + " to " + getRemoteAddress()
          + "in timeout(" + timeout + "ms) limit");
    }
  }

  @Override
  public void close() {
    try {
      removeChannelIfDisconnected(channel);
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Close netty channel " + channel);
      }
      channel.close().syncUninterruptibly();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (channel.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    NettyChannel other = (NettyChannel) obj;
    return channel.equals(other.channel);
  }

  @Override
  public String toString() {
    return "NettyChannel [channel=" + channel + "]";
  }

}
