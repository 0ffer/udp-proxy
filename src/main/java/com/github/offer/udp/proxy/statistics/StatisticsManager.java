package com.github.offer.udp.proxy.statistics;


import io.netty.channel.socket.DatagramPacket;

/**
 * Manager for protocol statistics.
 *
 * @author Stas Melnichuk
 */
public interface StatisticsManager {

    void markIncomePacket();

    void markWrongPrefixPacket(int prefix);

    void markTooSmallPacket(int length);

    void markAimServerNotAllowedPacket(int aimServerAddress);

    void markTypePPacket();

    void markTypeRPacket();

    void markTypeIPacket();

    void markUnknownTypePacket(DatagramPacket packet);

    void markLateResponse(int serverAddress, byte packetTypeByte);

    void markBlockedPacket(int serverAddress, byte packetTypeByte);

}
