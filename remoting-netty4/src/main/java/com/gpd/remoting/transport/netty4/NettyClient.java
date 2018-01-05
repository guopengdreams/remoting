package com.gpd.remoting.transport.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.common.utils.NamedThreadFactory;
import com.gpd.remoting.common.utils.NetUtils;
import com.gpd.remoting.transport.AbstractClient;

/**
 * 数据传递使用Object，数据编解码使用系统预定义的ObjectDecoder、ObjectEncoder.<br/>
 * 客户端连接使用长连接，需要自己处理重连和心跳回复<br/>
 */
public final class NettyClient extends AbstractClient {
  private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);
  private volatile Channel channel; // volatile, please copy reference to use
  private Bootstrap bootstrap;

  private static final EventLoopGroup WORKER_GROUP = new NioEventLoopGroup(
      Constants.DEFAULT_IO_THREADS, new NamedThreadFactory("NettyClientTCPWorker", true));

  public NettyClient(final URL url, final ChannelHandler handler) throws RemotingException {
    super(url, handler);
  }

  @Override
  protected void doOpen() throws Exception {
    bootstrap = new Bootstrap();
    bootstrap.group(WORKER_GROUP);
    bootstrap.channel(NioSocketChannel.class);
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    bootstrap.option(ChannelOption.TCP_NODELAY, true);
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
        getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
    final NettyClientHandler nettyClientHandler = new NettyClientHandler(getUrl(), this);
    bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) {
        ChannelPipeline channelPipeline = ch.pipeline();
        channelPipeline.addLast("encoderObject", new ObjectEncoder());
        channelPipeline.addLast(
            "decoderObject",
            new ObjectDecoder(1024 * 1024, ClassResolvers.cacheDisabled(this.getClass()
                .getClassLoader())));
        channelPipeline.addLast("decoderHeartBeat", new NettyClientHeartBeatHandler());
        channelPipeline.addLast("handler", nettyClientHandler);
      }
    });
  }

  @Override
  protected void doConnect() throws Exception {
    long start = System.currentTimeMillis();
    ChannelFuture future = bootstrap.connect(getConnectAddress());
    try {
      boolean ret =
          future.awaitUninterruptibly(
              getUrl().getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY,
                  Constants.DEFAULT_CONNECT_TIMEOUT), TimeUnit.MILLISECONDS);
      if (ret && future.isSuccess()) {
        Channel newChannel = future.channel();
        // 关闭旧的连接
        Channel oldChannel = this.channel; // copy reference
        if (oldChannel != null) {
          if (logger.isInfoEnabled()) {
            logger.info("Close old netty channel " + oldChannel + " on create new netty channel "
                + newChannel);
          }
          oldChannel.close().syncUninterruptibly();
        }
        this.channel = newChannel;
      } else if (future.cause() != null) {
        throw new RemotingException("client(url: " + getUrl() + ") failed to connect to server "
            + getRemoteAddress() + ", error message is:" + future.cause().getMessage(),
            future.cause());
      } else {
        throw new RemotingException("client(url: "
            + getUrl()
            + ") failed to connect to server "
            + getRemoteAddress()
            + " client-side timeout "
            + getUrl().getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY,
                Constants.DEFAULT_CONNECT_TIMEOUT) + "ms (elapsed: "
            + (System.currentTimeMillis() - start) + "ms) from netty client "
            + NetUtils.getLocalHost());
      }
    } finally {
      if (!isConnected()) {
        future.cancel(true);
      }
    }
  }

  @Override
  protected com.gpd.remoting.Channel getChannel() {
    Channel c = channel;
    if (c == null || !c.isActive()) return null;
    return NettyChannel.getOrAddChannel(c, getUrl(), this);
  }

  @Override
  protected void doClose() throws Throwable {
    // WORKER_GROUP.shutdownGracefully()
  }

  @Override
  protected void doDisConnect() throws Throwable {

  }
}
