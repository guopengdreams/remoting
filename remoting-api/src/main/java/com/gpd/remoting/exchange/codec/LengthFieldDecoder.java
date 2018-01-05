package com.gpd.remoting.exchange.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;

public class LengthFieldDecoder extends LengthFieldBasedFrameDecoder {

  public LengthFieldDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
      int lengthAdjustment, int initialBytesToStrip) {
    super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment,
        initialBytesToStrip);
  }

  @Override
  protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length, ByteOrder order) {
    buf = buf.order(order);
    byte[] dst = new byte[length];
    buf.getBytes(offset, dst);
    long frameLength;
    try {
      frameLength = Long.valueOf(new String(dst, "utf-8"));
    } catch (NumberFormatException e) {
      throw new DecoderException("unsupported lengthFieldLength: ");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new DecoderException("unsupported lengthFieldLength: ");
    }
    return frameLength;
  }
}
