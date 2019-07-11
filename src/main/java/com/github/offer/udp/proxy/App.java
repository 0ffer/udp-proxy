package com.github.offer.udp.proxy;

import com.github.offer.udp.proxy.cache.PacketCache;
import com.github.offer.udp.proxy.cache.PacketCacheImpl;
import com.github.offer.udp.proxy.protocol.ProtocolHandler;
import com.github.offer.udp.proxy.routing.AddressChecker;
import com.github.offer.udp.proxy.routing.AddressCheckerImpl;
import com.github.offer.udp.proxy.statistics.StatisticsManager;
import com.github.offer.udp.proxy.statistics.StatisticsManagerImpl;
import com.github.offer.udp.proxy.validation.PacketValidator;
import com.github.offer.udp.proxy.validation.PacketValidatorImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * FIXME Требуется сделать возможность выбора типа используемого алгоритма чтений. (OIO, NIO, EPOLL)
 *
 * @author Stas Melnichuk
 */
public class App {
    private static final Logger LOG = LogManager.getLogger(App.class);

    private EpollEventLoopGroup nioEventLoopGroup;

    private final Config config;
    private final PacketValidator packetValidator;
    private final AddressChecker addressChecker;
    private final StatisticsManager statisticsManager;
    private final PacketCache packetCache;

    public App(final Config config) {
        this.config = config;
        statisticsManager = new StatisticsManagerImpl(config);
        addressChecker = new AddressCheckerImpl(config);
        packetValidator = new PacketValidatorImpl(addressChecker, statisticsManager);
        packetCache = new PacketCacheImpl(config);
    }

    public void run() throws Exception {
        LOG.info("Server starts...");

        nioEventLoopGroup = new EpollEventLoopGroup(config.processThreadsCount());
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(nioEventLoopGroup)
                .channel(EpollDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    public void initChannel(DatagramChannel ch) throws Exception {
                        ch.pipeline().addLast(new ProtocolHandler(config, packetValidator, statisticsManager, packetCache));
                    }
                });

        bootstrap.bind(config.listenAddress(), config.listenPort()).sync();
        LOG.info("Server binds to {}:{}", config.listenAddress(), config.listenPort());

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                App.this.stop();
            }
        }, "shutDownHook"));

        LOG.info("Server is ready.");
    }

    public void stop() {
        if (nioEventLoopGroup != null) {
            nioEventLoopGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        final Config config = ConfigFactory.create(Config.class);
        new App(config).run();
    }

}
