package io.rsocket.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import io.rsocket.Frame;
import io.rsocket.reactor.aeron.FrameMapper;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
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
  FrameMapper frameMapper;

  @Setup
  public void setUp() {
    aeronDst = new ExpandableArrayBuffer(length);

    byte[] bytes = new byte[length];
    Random random = new Random();
    random.nextBytes(bytes);
    ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(length);
    byteBuf.writeBytes(bytes);

    nettySrc = Frame.from(byteBuf);

    aeronSrc = new UnsafeBuffer(ByteBuffer.allocateDirect(length));
    nettyDst = ByteBufAllocator.DEFAULT.buffer(aeronSrc.capacity());

    frameMapper = new FrameMapper();
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
  // @Benchmark
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
   * }
   * </pre>
   */
  // @Benchmark
  public void aeronToNetty(Blackhole blackhole) {
    Frame frame = frameMapper.apply(aeronSrc);
    blackhole.consume(frame);
    frame.release();
  }

  // @Benchmark
  public void aeronToNettyRnD(Blackhole blackhole) {
    aeronSrc.getBytes(0, nettyDst.internalNioBuffer(0, aeronSrc.capacity()), aeronSrc.capacity());
  }

  // @Benchmark
  public void byteBufAllocatorRnD(Blackhole blackhole) {
    ByteBufFactory byteBufFactory = ByteBufFactory.getOrCreate();
    blackhole.consume(byteBufFactory.byteBuf);
    byteBufFactory.recycle();
  }

  static class ByteBufFactory {

    static final Recycler<ByteBufFactory> recycler =
        new Recycler<ByteBufFactory>() {
          @Override
          protected ByteBufFactory newObject(Handle<ByteBufFactory> handle) {
            ByteBufFactory byteBufFactory = new ByteBufFactory(handle);
            byteBufFactory.byteBuf = PooledByteBufAllocator.DEFAULT.buffer(1024);
            System.err.println("byteBufFactory.byteBuf: " + byteBufFactory.byteBuf);
            return byteBufFactory;
          }
        };

    final Handle<ByteBufFactory> handle;

    ByteBuf byteBuf;

    ByteBufFactory(Handle<ByteBufFactory> handle) {
      this.handle = handle;
    }

    void recycle() {
      byteBuf.release();
      handle.recycle(this);
    }

    static ByteBufFactory getOrCreate() {
      ByteBufFactory byteBufFactory = recycler.get();
      byteBufFactory.byteBuf.retain();
      return byteBufFactory;
    }
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
