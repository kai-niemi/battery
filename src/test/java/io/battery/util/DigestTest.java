package io.battery.util;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.model.Agent;
import io.battery.model.Network;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class DigestTest {
    @Test
    public void whenConvertingBytesToHex_expectHexString() {
        Agent a = new Agent();
        a.setName("a");
        a.setUrl("b");

        Agent b = new Agent();
        b.setName("b");
        b.setUrl("b");

        Network network1 = new Network();
        network1.setAgents(List.of(a, b));

        Network network2 = new Network();
        network2.setAgents(List.of(b, a));

        String hex1 = DigestUtils.toSecureHash(network1);
        String hex2 = DigestUtils.toSecureHash(network1);
        assertThat(hex2).isEqualTo(hex1);

        String hex3 = DigestUtils.toSecureHash(network2);
        String hex4 = DigestUtils.toSecureHash(network2);
        assertThat(hex2).isEqualTo(hex1);

        assertThat(hex1).isNotEqualTo(hex3);
        assertThat(hex1).isNotEqualTo(hex4);
        assertThat(hex2).isNotEqualTo(hex3);
        assertThat(hex2).isNotEqualTo(hex4);

        System.out.println(hex1);
        System.out.println(hex2);
        System.out.println(hex3);
        System.out.println(hex4);
    }
}
