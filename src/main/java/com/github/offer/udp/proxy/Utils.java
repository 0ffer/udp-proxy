package com.github.offer.udp.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

/**
 *
 * Common app utils.
 *
 * @author Stas Melnichuk
 */
public class Utils {

    private static final int PACKET_PREFIX_OFFSET = 0;
    private static final int AIM_SERVER_ADDRESS_OFFSET = 4;
    private static final int PACKET_TYPE_BYTE_OFFSET = 10;

    public static int getPacketPrefix(final DatagramPacket packet) {
        return packet.content().getInt(PACKET_PREFIX_OFFSET);
    }

    public static int getPacketAimServerAddress(final DatagramPacket packet) {
        return packet.content().getInt(AIM_SERVER_ADDRESS_OFFSET);
    }

    public static byte getPacketTypeByte(final DatagramPacket packet) {
        return packet.content().getByte(PACKET_TYPE_BYTE_OFFSET);
    }

    public static byte[] readAllReadableBytes(final ByteBuf buffer) {
        return getBytes(buffer, 0, buffer.readableBytes());
    }

    public static byte[] getBytes(final ByteBuf buffer, final int startInd, final int length) {
        final byte[] result = new byte[length];
        buffer.getBytes(startInd, result);
        return result;
    }

}
