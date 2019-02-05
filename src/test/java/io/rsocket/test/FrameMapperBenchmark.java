package io.rsocket.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.rsocket.Frame;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@Threads(1)
@BenchmarkMode(Mode.AverageTime)
@Warmup(time = 3)
@Measurement(time = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class FrameMapperBenchmark {

  MutableDirectBuffer dstBuffer;
  DirectBuffer srcBuffer;
  int offset = 0;
  Frame frame;
  int length = 1024;

  @Setup
  public void setUp() {
    dstBuffer = new ExpandableArrayBuffer(length);

    byte[] srcBuf = new byte[length];
    Random random = new Random();
    random.nextBytes(srcBuf);
    ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(length);
    byteBuf.writeBytes(srcBuf);

    frame = Frame.from(byteBuf);

    srcBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(length));
  }

  /**
   *
   *
   * <pre>
   * public void write(MutableDirectBuffer dstBuffer, int offset, Frame frame, int length) {
   * UnsafeBuffer srcBuffer = new UnsafeBuffer(frame.content().memoryAddress(), length);
   * dstBuffer.putBytes(offset, srcBuffer, 0, length); }
   * </pre>
   */
  @Benchmark
  public void nettyBufferToAeronBuffer() {
    ByteBuf content = frame.content();
    content.clear();
    UnsafeBuffer srcBuffer = new UnsafeBuffer(content.memoryAddress(), length);
    dstBuffer.putBytes(offset, srcBuffer, 0, length);
  }

  /**
   *
   *
   * <pre>
   * public Frame apply(DirectBuffer source) {
   * return Frame.from(Unpooled.wrappedBuffer(source.addressOffset(), source.capacity(), false));
   * }
   * </pre>
   */
  @Benchmark
  public void aeronBufferToNettyBuffer() {
    Frame.from(Unpooled.wrappedBuffer(srcBuffer.addressOffset(), srcBuffer.capacity(), false));
  }

  public static void main(String[] args) throws RunnerException {
    new Runner(
            new OptionsBuilder()
                .include(FrameMapperBenchmark.class.getSimpleName())
                .forks(1)
                .build())
        .run();
  }
}
