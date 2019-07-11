package com.github.offer.udp.proxy.protocol;

import com.github.offer.udp.proxy.Config;
import com.github.offer.udp.proxy.Utils;
import com.github.offer.udp.proxy.cache.PacketCache;
import com.github.offer.udp.proxy.statistics.StatisticsManager;
import com.github.offer.udp.proxy.validation.PacketValidator;
import com.google.common.net.InetAddresses;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;

/**
 * Central UDP datagrams handler.
 *
 * FIXME Много вопросов относительно того, как кешировать буфер - обязательно ли его копировать, или можно так кидать... Не знаю в общем.
 *
 * @author Stas Melnichuk
 */
@ChannelHandler.Sharable
public class ProtocolHandler extends ChannelInboundHandlerAdapter {

    private final PacketCache packetCache;
    private final PacketValidator packetValidator;
    private final StatisticsManager statisticsManager;

    private final int proxyingServerPort;

    public ProtocolHandler(final Config config,
                           final PacketValidator packetValidator,
                           final StatisticsManager statisticsManager,
                           final PacketCache packetCache) {
        this.packetValidator = packetValidator;
        this.statisticsManager = statisticsManager;
        this.packetCache = packetCache;

        this.proxyingServerPort = config.proxyingServerPort();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket packet = (DatagramPacket) msg;

        try{
            processDatagramPacket(ctx, packet);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void processDatagramPacket(ChannelHandlerContext ctx, DatagramPacket packet) {

        statisticsManager.markIncomePacket();

        if ( ! packetValidator.isValid(packet)){
            return;
        }

        final int aimServerAddress = Utils.getPacketAimServerAddress(packet);
        final byte packetTypeByte = Utils.getPacketTypeByte(packet);

        if (packet.content().readableBytes() > 15) { // Response from proxying server.
            processResponse(ctx, packet, aimServerAddress, packetTypeByte);

            return;
        }

        switch (packetTypeByte) { // External request.
            // FIXME Есть еще четвертый тип пакета - на него вроде можно не отвечать, но сделать-то надо!
            case (114): // 0x72 - R type
                statisticsManager.markTypeRPacket();
            case (105): // 0x69 - I type
                statisticsManager.markTypeIPacket();
                processRedirectableRequest(ctx, packet, aimServerAddress, packetTypeByte);
                break;
            case (112): // 0x70 - P type
                statisticsManager.markTypePPacket();
                ctx.writeAndFlush(new DatagramPacket(packet.content().copy(), packet.sender(), null));
                break;
        }
    }

    /**
     * Proxying server response processing.
     *
     * @param ctx
     * @param packet
     * @param aimServerAddress
     * @param packetTypeByte
     */
    private void processResponse(ChannelHandlerContext ctx, DatagramPacket packet, int aimServerAddress, byte packetTypeByte) {
        final InetSocketAddress[] recipients = packetCache.getAndReleaseWaitedRecipients(aimServerAddress, packetTypeByte);

        if (recipients.length > 0) {
            packetCache.cache(aimServerAddress, packetTypeByte, packet.content().copy());

            for (InetSocketAddress recipient : recipients) {
                ctx.write(new DatagramPacket(packet.content().copy(), recipient));
            }
            ctx.flush();
        } else {
            statisticsManager.markLateResponse(aimServerAddress, packetTypeByte);
        }
    }

    /**
     * Redirectable request processing.
     *
     * @param ctx
     * @param packet
     * @param aimServerAddress
     * @param packetTypeByte
     */
    private void processRedirectableRequest(ChannelHandlerContext ctx, DatagramPacket packet, int aimServerAddress, byte packetTypeByte) {
        if (packetCache.isNeedBlock(aimServerAddress, packetTypeByte)) {
            statisticsManager.markBlockedPacket(aimServerAddress, packetTypeByte);
            return;
        }

        final ByteBuf cachedResponse = packetCache.tryGet(aimServerAddress, packetTypeByte);
        if (cachedResponse != null) {
            ctx.writeAndFlush(new DatagramPacket(cachedResponse.copy(), packet.sender()));
        } else {
            if ( ! packetCache.isAlreadyWaitResponse(aimServerAddress, packetTypeByte)) {
                ctx.writeAndFlush(new DatagramPacket(packet.content().copy(), new InetSocketAddress(InetAddresses.fromInteger(aimServerAddress), proxyingServerPort)));
            }

            packetCache.addToResponseWaitersQueue(packet.sender(), aimServerAddress, packetTypeByte);
        }
    }

}
