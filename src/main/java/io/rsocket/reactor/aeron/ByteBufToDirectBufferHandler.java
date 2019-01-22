package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import reactor.aeron.DirectBufferHandler;

class ByteBufToDirectBufferHandler implements DirectBufferHandler<ByteBuf> {

  @Override
  public int estimateLength(ByteBuf buffer) {
    return buffer.readableBytes();
  }

  @Override
  public void write(MutableDirectBuffer dstBuffer, int offset, ByteBuf srcBuffer, int length) {
    srcBuffer.forEachByte(new ContentByteProcessor(dstBuffer, offset, length));
  }

  @Override
  public DirectBuffer map(ByteBuf buffer, int length) {
    return new UnsafeBuffer(buffer.memoryAddress(), length);
  }

  @Override
  public void dispose(ByteBuf buffer) {
    RefCountUtil.safestRelease(buffer);
  }

  private static class ContentByteProcessor implements ByteProcessor {

    final int length;
    final int offset;
    final MutableDirectBuffer dstBuffer;
    int i;

    ContentByteProcessor(MutableDirectBuffer dstBuffer, int offset, int length) {
      this.length = length;
      this.offset = offset;
      this.dstBuffer = dstBuffer;
    }

    @Override
    public boolean process(byte b) {
      int index = offset + i;
      dstBuffer.putByte(index, b);
      return ++i < length;
    }
  }
}
