package io.battery.script.function;

import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import io.battery.util.HexUtils;

/**
 * Network functions in the {@code network} namespace for random IPv4 and IPv6 addresses
 * and for looking up the local and public IP of the host, with timeouts. A mixin of default methods.
 */
public interface NetworkFunctions {
    String NAMESPACE = "network";

    @Description(value = """
            Returns a random IPv4 address.
            """, volatility = Volatility.Volatile)
    default String randomIPv4() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringJoiner joiner = new StringJoiner(".");
        // Upper bound is exclusive, so 256 to include 255
        joiner.add(r.nextInt(0, 256) + "");
        joiner.add(r.nextInt(0, 256) + "");
        joiner.add(r.nextInt(0, 256) + "");
        joiner.add(r.nextInt(0, 256) + "");
        return joiner.toString();
    }

    @Description(value = """
            Returns a random IPv6 address.
            """, volatility = Volatility.Volatile)
    default String randomIPv6() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringJoiner joiner = new StringJoiner(":");
        IntStream.range(0, 8).mapToObj(i -> new StringBuilder()).forEach(b -> {
            IntStream.rangeClosed(1, 4).forEach(value ->
                    b.append(HexUtils.HEX_CHARS[random.nextInt(0, 16)]));
            joiner.add(b.toString());
        });
        return joiner.toString();
    }

    @Description(value = """
            Returns the host local IPv4 address (behind NAT), with a 5 second connect timeout.
            """, volatility = Volatility.Volatile)
    default String localIP() {
        return NetworkLookups.localIP();
    }

    @Description(value = """
            Returns the host public IPv4 address, looked up once with a 5 second timeout and cached.
            """, volatility = Volatility.Volatile)
    default String publicIP() {
        return NetworkLookups.publicIP();
    }
}
