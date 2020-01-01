package io.rsocket.reactor.aeron;

import io.aeron.logbuffer.Header;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.frame.FrameLengthFlyweight;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import reactor.aeron.DirectBufferHandler;
import reactor.aeron.FragmentMapper;

public class FrameMapper implements DirectBufferHandler<ByteBuf>, FragmentMapper<ByteBuf> {

  private final ByteBufAllocator allocator;
  private final boolean encodeLength;

  public FrameMapper(ByteBufAllocator allocator) {
    this(allocator, true);
  }

  public FrameMapper(ByteBufAllocator allocator, boolean encodeLength) {
    this.allocator = allocator;
    this.encodeLength = encodeLength;
  }

  @Override
  public DirectBuffer map(ByteBuf buffer) {
    ByteBuf frame = encode(buffer);
    return new UnsafeBuffer(frame.memoryAddress(), frame.readableBytes());
  }

  @Override
  public void dispose(ByteBuf buffer) {
    buffer.release();
  }

  @Override
  public ByteBuf apply(DirectBuffer srcBuffer, int offset, int length, Header header) {
    ByteBuf dstBuffer = allocator.buffer(length);
    srcBuffer.getBytes(offset, dstBuffer.nioBuffer(), length);
    return decode(dstBuffer);
  }

  private ByteBuf encode(ByteBuf frame) {
    if (encodeLength) {
      return FrameLengthFlyweight.encode(allocator, frame.readableBytes(), frame);
    } else {
      return frame;
    }
  }

  private ByteBuf decode(ByteBuf frame) {
    if (encodeLength) {
      return FrameLengthFlyweight.frame(frame).retain();
    } else {
      return frame;
    }
  }
}
