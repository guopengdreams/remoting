package com.gpd.remoting.exchange.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.internal.ObjectUtil;

import java.nio.ByteOrder;


@Sharable
public class LengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {

  private final ByteOrder byteOrder;
  private final int lengthFieldLength;
  private final int lengthAdjustment;

  public LengthFieldPrepender(int lengthFieldLength, int lengthAdjustment) {
    this(ByteOrder.BIG_ENDIAN, lengthFieldLength, lengthAdjustment);
  }

  public LengthFieldPrepender(ByteOrder byteOrder, int lengthFieldLength, int lengthAdjustment) {
    ObjectUtil.checkNotNull(byteOrder, "byteOrder");
    this.byteOrder = byteOrder;
    this.lengthFieldLength = lengthFieldLength;
    this.lengthAdjustment = lengthAdjustment;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
    int length = msg.readableBytes() - lengthAdjustment;

    if (length < 0) {
      throw new IllegalArgumentException("Adjusted frame length (" + length + ") is less than zero");
    }
    String format = "%0" + this.lengthFieldLength + "d";
    out.writeBytes(String.format(format, length).getBytes());
    out.writeBytes(msg, msg.readerIndex(), msg.readableBytes());
  }

  @Override
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
      throws Exception {
    return super.allocateBuffer(ctx, msg, preferDirect).order(byteOrder);
  }
}
