package com.gpd.remoting.transport.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import com.gpd.remoting.common.Constants;
import com.gpd.remoting.exchange.Request;
import com.gpd.remoting.exchange.Response;


public class NettyClientHeartBeatHandler extends ChannelInboundHandlerAdapter {
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Request && Constants.PING_MESSAGE.equals(((Request) msg).getData())) {
      Response resp = new Response();
      resp.setResult(Constants.PING_MESSAGE);
      ctx.writeAndFlush(resp);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

}
