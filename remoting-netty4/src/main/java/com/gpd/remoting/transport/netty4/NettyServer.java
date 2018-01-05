package com.gpd.remoting.transport.netty4;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.Server;
import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.common.utils.NamedThreadFactory;
import com.gpd.remoting.common.utils.NetUtils;
import com.gpd.remoting.transport.AbstractServer;

public final class NettyServer extends AbstractServer implements Server {
  private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
  private Map<String, Channel> channels; // <ip:port, channel>
  private io.netty.channel.Channel channel;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
    super(url, handler);
  }

  @Override
  protected void doOpen() throws Throwable {
    ServerBootstrap bootstrap = new ServerBootstrap();
    final NettyServerHandler nettyServerHandler = new NettyServerHandler(getUrl(), this);
    channels = nettyServerHandler.getChannels();
    bossGroup = new NioEventLoopGroup(1, (new NamedThreadFactory("NettyServerBoss", true)));
    workerGroup =
        new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1,
            new NamedThreadFactory("NettyServerWorker", true));
    bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
    bootstrap.childOption(ChannelOption.TCP_NODELAY, false);
    bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) {
        ChannelPipeline channelPipeline = ch.pipeline();
        channelPipeline.addLast("encoderObject", new ObjectEncoder());
        channelPipeline.addLast(
            "decoderObject",
            new ObjectDecoder(1024 * 1024, ClassResolvers.weakCachingConcurrentResolver(this
                .getClass().getClassLoader())));
        channelPipeline.addLast("idleStateHandler", new IdleStateHandler(
            Constants.READER_IDLE_TIME_SECONDS, Constants.WRITER_IDLE_TIME_SECONDS, 0));
        channelPipeline.addLast("decoderHeartBeat", new NettyServerHeartBeatHandler());
        channelPipeline.addLast("handler", nettyServerHandler);
      }
    });

    // bind
    ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
    channelFuture.awaitUninterruptibly();
    channel = channelFuture.channel();
  }

  @Override
  protected void doClose() throws Throwable {
    try {
      if (channel != null) {
        // unbind.
        channel.close().syncUninterruptibly();
      }
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }

    try {
      Collection<Channel> channels = getChannels();
      if (channels != null && channels.size() > 0) {
        for (Channel channel : channels) {
          try {
            channel.close();
          } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
          }
        }
      }
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }

    try {
      // and then shutdown the thread pools
      if (bossGroup != null) {
        bossGroup.shutdownGracefully();
      }
      if (workerGroup != null) {
        workerGroup.shutdownGracefully();
      }
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }

    try {
      if (channels != null) {
        channels.clear();
      }
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }
  }

  @Override
  public Collection<Channel> getChannels() {
    Collection<Channel> chs = new HashSet<Channel>();
    for (Channel channel : this.channels.values()) {
      if (channel.isConnected()) {
        chs.add(channel);
      } else {
        channels.remove(NetUtils.toAddressString(channel.getRemoteAddress()));
      }
    }
    return chs;
  }

  @Override
  public Channel getChannel(InetSocketAddress remoteAddress) {
    return channels.get(NetUtils.toAddressString(remoteAddress));
  }

  @Override
  public boolean isBound() {
    return channel.isRegistered();
  }

}
