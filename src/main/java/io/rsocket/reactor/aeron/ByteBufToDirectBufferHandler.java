package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
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
    UnsafeBuffer unsafeBuffer = new UnsafeBuffer(srcBuffer.memoryAddress(), length);
    dstBuffer.putBytes(offset, unsafeBuffer, 0, length);
  }

  @Override
  public DirectBuffer map(ByteBuf buffer, int length) {
    return new UnsafeBuffer(buffer.memoryAddress(), length);
  }

  @Override
  public void dispose(ByteBuf buffer) {
    RefCountUtil.safestRelease(buffer);
  }
}
