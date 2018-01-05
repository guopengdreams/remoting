package com.gpd.remoting.transport.netty4;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gpd.remoting.common.Constants;
import com.gpd.remoting.exchange.Request;
import com.gpd.remoting.exchange.Response;

public class NettyServerHeartBeatHandler extends ChannelDuplexHandler {
  private static final Logger logger = LoggerFactory.getLogger(NettyServerHeartBeatHandler.class);

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Response && Constants.PING_MESSAGE.equals(((Response) msg).getResult())) {
      logger
          .info("<!-- server recevie heart beat from [" + ctx.channel().remoteAddress() + "] -->");
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent) evt;
      if (e.state() == IdleState.READER_IDLE) {
        ctx.close();
      } else if (e.state() == IdleState.WRITER_IDLE) {
        logger.info("<!-- server send heart beat to [" + ctx.channel().remoteAddress() + "] -->");
        Request req = new Request();
        req.setData(Constants.PING_MESSAGE);
        ctx.writeAndFlush(req);
      }
    } else {
      ctx.fireUserEventTriggered(evt);
    }
  }
}
