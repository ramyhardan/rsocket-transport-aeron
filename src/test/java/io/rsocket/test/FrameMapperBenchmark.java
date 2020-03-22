package io.rsocket.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Frame;
import io.rsocket.reactor.aeron.FrameMapper;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.agrona.BufferUtil;
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
import org.openjdk.jmh.infra.Blackhole;
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

  MutableDirectBuffer aeronDst;
  DirectBuffer aeronSrc;
  int offset = 0;
  Frame nettySrc;
  int length = 1024;
  ByteBuf nettyDst;
  ByteBuffer byteBuffer;
  FrameMapper frameMapper;

  @Setup
  public void setUp() {
    aeronDst = new ExpandableArrayBuffer(length);
    nettyDst = ByteBufAllocator.DEFAULT.buffer(length);

    byte[] bytes = new byte[length];
    Random random = new Random();
    random.nextBytes(bytes);

    nettySrc = Frame.from(ByteBufAllocator.DEFAULT.buffer(length).writeBytes(bytes));

    byteBuffer = ByteBuffer.allocateDirect(length).put(bytes).rewind();

    aeronSrc = new UnsafeBuffer(BufferUtil.address(byteBuffer), length);

    frameMapper = new FrameMapper();
  }

  /**
   *
   *
   * <pre>
   * public void write(MutableDirectBuffer dstBuffer, int offset, Frame frame, int length)
   * UnsafeBuffer srcBuffer = new UnsafeBuffer(frame.content().memoryAddress(), length);
   * dstBuffer.putBytes(offset, srcBuffer, 0, length);
   * </pre>
   */
  @Benchmark
  public void nettyToAeron() {
    frameMapper.write(aeronDst, offset, nettySrc, length);
  }

  /**
   *
   *
   * <pre>
   * ByteBuf destination = ByteBufAllocator.DEFAULT.buffer(source.capacity());
   * source.getBytes(0, destination.internalNioBuffer(0, source.capacity()), source.capacity());
   * return Frame.from(destination);
   * </pre>
   */
  @Benchmark
  public void aeronToNettyBuffer(Blackhole blackhole) {
    Frame frame = frameMapper.apply(aeronSrc);
    blackhole.consume(frame);
    frame.release();
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
