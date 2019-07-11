package com.github.offer.udp.proxy.validation;

import com.github.offer.udp.proxy.Utils;
import com.github.offer.udp.proxy.routing.AddressChecker;
import com.github.offer.udp.proxy.statistics.StatisticsManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

/**
 * Implementation of {@link PacketValidator}.
 *
 * @author Stas Melnichuk
 */
public class PacketValidatorImpl implements PacketValidator {

    private static final int VALID_PACKET_PREFIX = 1396788560; // 0x53414d50

    private final StatisticsManager statisticsManager;
    private final AddressChecker addressChecker;

    public PacketValidatorImpl(final AddressChecker addressChecker, final StatisticsManager statisticsManager) {
        this.addressChecker = addressChecker;
        this.statisticsManager = statisticsManager;
    }

    @Override
    public boolean isValid(final DatagramPacket packet) {
        ByteBuf content = packet.content();

        // FIXME Убрана проверка на точное совпадение по размеру из-за использования единственного сокета для приема внешних запросов и внутренних ответов.

        if (content.readableBytes() < 11) {
            statisticsManager.markTooSmallPacket(content.readableBytes());
            return false;
        }

        final int packetPrefix = Utils.getPacketPrefix(packet);
        if (packetPrefix != VALID_PACKET_PREFIX) {
            statisticsManager.markWrongPrefixPacket(packetPrefix);
            return false;
        }

        final int aimServerAddr = Utils.getPacketAimServerAddress(packet);
        if ( ! addressChecker.isAddressAllowed(aimServerAddr)) {
            statisticsManager.markAimServerNotAllowedPacket(aimServerAddr);
            return false;
        }

        final byte packetTypeByte = Utils.getPacketTypeByte(packet);
        switch (packetTypeByte) {
            // FIXME Есть еще четвертый тип пакета - на него вроде можно не отвечать, но сделать то надо!
            case (114): // 0x72 - R type
            case (105): // 0x69 - I type
            case (112): // 0x70 - P type
                break;
            default:
                statisticsManager.markUnknownTypePacket(packet);
                return false;
        }

        return true;
    }
}
