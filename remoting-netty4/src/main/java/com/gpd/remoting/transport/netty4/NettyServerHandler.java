package com.gpd.remoting.transport.netty4;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.common.utils.NetUtils;
import com.gpd.remoting.exchange.Request;

/**
 * NettyServerHandler
 * 
 */
@io.netty.channel.ChannelHandler.Sharable
public class NettyServerHandler extends ChannelHandlerAdapter
    implements
      ChannelOutboundHandler,
      ChannelInboundHandler {
  // <ip:port,channel>
  private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>();

  private final URL url;

  private final ChannelHandler handler;

  public NettyServerHandler(URL url, ChannelHandler handler) {
    if (url == null) {
      throw new IllegalArgumentException("url == null");
    }
    if (handler == null) {
      throw new IllegalArgumentException("handler == null");
    }
    this.url = url;
    this.handler = handler;
  }

  public Map<String, Channel> getChannels() {
    return channels;
  }

  @Override
  public void bind(ChannelHandlerContext ctx, SocketAddress socketAddress,
      ChannelPromise channelPromise) throws Exception {
    ctx.bind(socketAddress, channelPromise);
  }

  @Override
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
      SocketAddress localAddress, ChannelPromise promise) throws Exception {
    ctx.connect(remoteAddress, localAddress, promise);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      if (channel != null) {
        channels.put(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()),
            channel);
      }
      handler.connected(channel);
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }
  }

  @Override
  public void disconnect(io.netty.channel.ChannelHandlerContext ctx,
      io.netty.channel.ChannelPromise channelPromise) throws java.lang.Exception {
    ctx.disconnect(channelPromise);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      channels.remove(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
      handler.disconnected(channel);
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }
  }

  @Override
  public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg)
      throws java.lang.Exception {
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      if (msg instanceof Request) {
        handler.received(channel, msg);
      }
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }

  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    ctx.writeAndFlush(msg, promise);
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      handler.sent(channel, msg);
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    try {
      handler.caught(channel, cause);
    } finally {
      NettyChannel.removeChannelIfDisconnected(ctx.channel());
    }
  }


  @Override
  public void close(ChannelHandlerContext ctx, ChannelPromise channelPromise) throws Exception {
    ctx.close(channelPromise);
  }

  @Override
  public void deregister(ChannelHandlerContext ctx, ChannelPromise channelPromise) throws Exception {
    ctx.deregister(channelPromise);
  }

  @Override
  public void read(ChannelHandlerContext channelHandlerContext) throws Exception {
    channelHandlerContext.read();
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelRegistered();
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelUnregistered();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object o) throws Exception {
    ctx.fireUserEventTriggered(o);
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelWritabilityChanged();
  }

  @Override
  public void flush(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

}
