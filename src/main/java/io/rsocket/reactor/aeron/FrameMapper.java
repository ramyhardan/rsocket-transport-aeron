package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Frame;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.function.Function;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import reactor.aeron.DirectBufferHandler;

public final class FrameMapper
    implements DirectBufferHandler<Frame>, Function<DirectBuffer, Frame> {

  @Override
  public int estimateLength(Frame frame) {
    return frame.content().readableBytes();
  }

  @Override
  public void write(MutableDirectBuffer dstBuffer, int offset, Frame frame, int length) {
    UnsafeBuffer srcBuffer = new UnsafeBuffer(frame.content().memoryAddress(), length);
    dstBuffer.putBytes(offset, srcBuffer, 0, length);
  }

  @Override
  public DirectBuffer map(Frame frame, int length) {
    return new UnsafeBuffer(frame.content().memoryAddress(), length);
  }

  @Override
  public void dispose(Frame frame) {
    frame.release();
  }

  @Override
  public Frame apply(DirectBuffer srcBuffer) {
    ByteBuf byteBuf =
        Unpooled.wrappedBuffer(srcBuffer.addressOffset(), srcBuffer.capacity(), false);
    return Frame.from(byteBuf);
  }

  public static void main(String[] args) {
    int length = 1024;
    byte[] bytes = new byte[length];
    Random random = new Random();
    random.nextBytes(bytes);

    ByteBuffer buffer = (ByteBuffer) ByteBuffer.allocateDirect(length).put(bytes).rewind();

    UnsafeBuffer unsafeBuffer = new UnsafeBuffer(1, length);

    ByteBuffer byteBuffer = unsafeBuffer.byteBuffer();

    ByteBuf byteBuf = Unpooled.wrappedBuffer(byteBuffer);

    System.out.println(byteBuf.readableBytes());
  }
}
