package com.github.offer.udp.proxy.statistics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.github.offer.udp.proxy.Config;
import com.github.offer.udp.proxy.Utils;
import com.google.common.net.InetAddresses;
import io.netty.channel.socket.DatagramPacket;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Manager for packet statistics.
 *
 * TODO Оставить один метод для любого типа пакета.
 *
 * @author Stas Melnichuk
 */
public class StatisticsManagerImpl implements StatisticsManager {
    private static final Logger LOG = LogManager.getLogger(StatisticsManager.class);
    private static final MetricRegistry metrics = new MetricRegistry();

    private final Config config;

    // It is to more fast access. In metrics ConcurrentMap was used - it may be slow.
    private final Meter inputPacketMeter;
    private final Meter wrongPrefixMeter;
    private final Meter tooSmallPacketMeter;
    private final Meter aimServerNotAllowedMeter;
    private final Meter typePPacketMeter;
    private final Meter typeIPacketMeter;
    private final Meter typeRPacketMeter;
    private final Meter unknownPacketTypeMeter;
    private final Meter lateResponseMeter;
    private final Meter blockedRequestMeter;

    public StatisticsManagerImpl(final Config config) {
        this.config = config;

        if (config.isMetricsEnable()) {
            this.inputPacketMeter = metrics.meter("packets.income");
            this.wrongPrefixMeter = metrics.meter("packets.validate.prefix.wrong");
            this.tooSmallPacketMeter = metrics.meter("packets.validate.length.small");
            this.aimServerNotAllowedMeter = metrics.meter("packets.validate.address.not-allowed");
            this.typePPacketMeter = metrics.meter("packets.type.P");
            this.typeIPacketMeter = metrics.meter("packets.type.I");
            this.typeRPacketMeter = metrics.meter("packets.type.R");
            this.unknownPacketTypeMeter = metrics.meter("packets.type.unknown");
            this.lateResponseMeter = metrics.meter("packets.response.late");
            this.blockedRequestMeter = metrics.meter("packets.blocked");
        } else {
            this.inputPacketMeter = null;
            this.wrongPrefixMeter = null;
            this.tooSmallPacketMeter = null;
            this.aimServerNotAllowedMeter = null;
            this.typePPacketMeter = null;
            this.typeIPacketMeter = null;
            this.typeRPacketMeter = null;
            this.unknownPacketTypeMeter = null;
            this.lateResponseMeter = null;
            this.blockedRequestMeter = null;
        }

        if (config.isMetricsEnable()) {
            switch (config.metricsOutput()) {
                case LOG: {
                    final Slf4jReporter reporter = Slf4jReporter.forRegistry(metrics)
                            .outputTo(LoggerFactory.getLogger("com.github.offer.udp.proxy.metrics"))
                            .convertRatesTo(TimeUnit.SECONDS)
                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                            .build();
                    reporter.start(config.metricsOutputFrequency(), TimeUnit.MILLISECONDS);

                    break; }
                case CONSOLE: {
                    final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                            .convertRatesTo(TimeUnit.SECONDS)
                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                            .build();
                    reporter.start(config.metricsOutputFrequency(), TimeUnit.MILLISECONDS);

                    break; }
                default:
                    throw new IllegalStateException("Unknown metrics output type.");
            }
        }
    }

    @Override
    public void markIncomePacket() {
        if(config.isMetricsEnable()) {
            inputPacketMeter.mark();
        }
    }

    @Override
    public void markWrongPrefixPacket(int prefix) {
        LOG.info("Ignore packet with wrong prefix: {}", () -> Integer.toHexString(prefix));

        if(config.isMetricsEnable()) {
            wrongPrefixMeter.mark();
        }
    }

    @Override
    public void markTooSmallPacket(final int length) {
        LOG.info("Ignore packet with too small length: {}", length);

        if(config.isMetricsEnable()) {
            tooSmallPacketMeter.mark();
        }
    }

    @Override
    public void markAimServerNotAllowedPacket(int aimServerAddress) {
        LOG.info("Ignore packet with not allowed address: {}", () -> InetAddresses.fromInteger(aimServerAddress));

        if(config.isMetricsEnable()) {
            aimServerNotAllowedMeter.mark();
        }
    }

    @Override
    public void markTypePPacket() {
        LOG.info("Receive 0x70 (P) packet");

        if(config.isMetricsEnable()) {
            typePPacketMeter.mark();
        }
    }

    @Override
    public void markTypeRPacket() {
        LOG.info("Receive 0x72 (R) packet");

        if(config.isMetricsEnable()) {
            typeRPacketMeter.mark();
        }
    }

    @Override
    public void markTypeIPacket() {
        LOG.info("Receive 0x69 (I) packet");

        if(config.isMetricsEnable()) {
            typeIPacketMeter.mark();
        }
    }

    @Override
    public void markUnknownTypePacket(final DatagramPacket packet) {
        LOG.warn("Ignore packet with unknown type with next content: {}", () -> Hex.encodeHexString(Utils.readAllReadableBytes(packet.content())));

        if(config.isMetricsEnable()) {
            unknownPacketTypeMeter.mark();
        }
    }

    @Override
    public void markLateResponse(int serverAddress, byte packetTypeByte) {
        LOG.warn("Receive late response packet from {}, with type {}", () -> InetAddresses.fromInteger(serverAddress), () -> Hex.encodeHexString(new byte[] {packetTypeByte}));

        if(config.isMetricsEnable()) {
            lateResponseMeter.mark();
        }
    }

    @Override
    public void markBlockedPacket(int serverAddress, byte packetTypeByte) {
        LOG.warn("Blocked packet to address {}, with type {}", () -> InetAddresses.fromInteger(serverAddress), () -> Hex.encodeHexString(new byte[] {packetTypeByte}));

        if(config.isMetricsEnable()) {
            blockedRequestMeter.mark();
        }
    }
}
