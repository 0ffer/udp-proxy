package com.github.offer.udp.proxy.validation;


import io.netty.channel.socket.DatagramPacket;

/**
 * Interface to validate incoming packets.
 *
 * @author Stas Melnichuk
 */
public interface PacketValidator {

    boolean isValid(DatagramPacket packet);

}
