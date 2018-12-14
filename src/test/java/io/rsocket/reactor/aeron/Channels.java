package io.rsocket.reactor.aeron;

import io.aeron.ChannelUriStringBuilder;
import java.net.InetSocketAddress;

public class Channels {

  public static final ChannelUriStringBuilder serverChannel =
      new ChannelUriStringBuilder().reliable(true).media("udp").endpoint("localhost:13000");

  public static final ChannelUriStringBuilder clientChannel =
      new ChannelUriStringBuilder().reliable(true).media("udp").endpoint("localhost:12001");

  public static ChannelUriStringBuilder from(InetSocketAddress address) {
    return new ChannelUriStringBuilder()
        .reliable(true)
        .media("udp")
        .endpoint(address.getHostString() + ":" + address.getPort());
  }

  public static ChannelUriStringBuilder from(int port) {
    return new ChannelUriStringBuilder().reliable(true).media("udp").endpoint("localhost:" + port);
  }
}
