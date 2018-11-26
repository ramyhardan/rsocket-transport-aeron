package io.rsocket.reactor.aeron;

import static java.lang.Boolean.TRUE;

import io.aeron.ChannelUriStringBuilder;
import java.net.InetSocketAddress;

public class Channels {

  public static final String serverChannel =
      new ChannelUriStringBuilder()
          .reliable(TRUE)
          .media("udp")
          .endpoint("localhost:13000")
          .build();

  public static final String clientChannel =
      new ChannelUriStringBuilder()
          .reliable(TRUE)
          .media("udp")
          .endpoint("localhost:12001")
          .build();

  public static String from(InetSocketAddress address) {
    return new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .endpoint(address.getHostString() + ":" + address.getPort())
        .build();
  }

  public static String from(int port) {
    return new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .endpoint("localhost:" + port)
        .build();
  }

}
