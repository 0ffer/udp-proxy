package com.github.offer.udp.proxy;

import com.github.offer.udp.proxy.routing.AddressCheckerImpl;
import com.github.offer.udp.proxy.statistics.StatisticsManagerImpl;
import com.github.offer.udp.proxy.validation.PacketValidator;
import com.github.offer.udp.proxy.validation.PacketValidatorImpl;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertFalse;

/**
 * Test for packet validation.
 * 
 * @author Stas Melnichuk 
 */
public class ValidationTests {

    @Test
    public void tooSmallSizeTest() {
        Config config = TestUtils.getDefaultConfig();
        final PacketValidator validator = new PacketValidatorImpl(new AddressCheckerImpl(config), new StatisticsManagerImpl(config));

        final DatagramPacket packet = new DatagramPacket(Unpooled.buffer(3), new InetSocketAddress("127.0.0.1", 1234));

        assertFalse(validator.isValid(packet));
    }

    @Test
    public void badPrefixTest() {
        Config config = TestUtils.getDefaultConfig();
        final PacketValidator validator = new PacketValidatorImpl(new AddressCheckerImpl(config), new StatisticsManagerImpl(config));

        final byte[] copiedPing = TestUtils.PING_PACKET.clone();
        copiedPing[0] = (byte) 0;
        copiedPing[1] = (byte) 1;

        final DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(copiedPing), new InetSocketAddress("127.0.0.1", 1234));

        assertFalse(validator.isValid(packet));
    }

    @Test
    public void notAllowedAddressTest() {
        Config config = TestUtils.getDefaultConfig();
        config.allowedSubnets().clear();
        final PacketValidator validator = new PacketValidatorImpl(new AddressCheckerImpl(config), new StatisticsManagerImpl(config));

        final DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(TestUtils.PING_PACKET), new InetSocketAddress("127.0.0.1", 1234));

        assertFalse(validator.isValid(packet));
    }

    @Test
    public void unknownTypeTest() {
        Config config = TestUtils.getDefaultConfig();
        final PacketValidator validator = new PacketValidatorImpl(new AddressCheckerImpl(config), new StatisticsManagerImpl(config));

        final byte[] copiedPing = TestUtils.PING_PACKET.clone();
        copiedPing[10] = (byte) 128;

        final DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(copiedPing), new InetSocketAddress("127.0.0.1", 1234));

        assertFalse(validator.isValid(packet));
    }
}
