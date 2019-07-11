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
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
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

    private MultithreadEventLoopGroup serverEventLoopGroup;

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

        final Bootstrap bootstrap = prepareServer();

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

    /**
     * Prepare server bootstrap.
     *
     * If EPOLL is available - use it, otherwise use NIO.
     *
     * @return preapred server bootstrap.
     */
    private Bootstrap prepareServer() {

        final Class channelClass;
        if (Epoll.isAvailable()) {
            LOG.info("EPOLL is available - use it.");
            serverEventLoopGroup = new EpollEventLoopGroup(config.processThreadsCount());
            channelClass = EpollDatagramChannel.class;
        } else {
            LOG.info("EPOLL is not available - use NIO.");
            serverEventLoopGroup = new NioEventLoopGroup(config.processThreadsCount());
            channelClass = NioDatagramChannel.class;
        }

        serverEventLoopGroup = new EpollEventLoopGroup(config.processThreadsCount());
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(serverEventLoopGroup)
                .channel(channelClass)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    public void initChannel(DatagramChannel ch) throws Exception {
                        ch.pipeline().addLast(new ProtocolHandler(config, packetValidator, statisticsManager, packetCache));
                    }
                });
        return bootstrap;
    }

    public void stop() {
        if (serverEventLoopGroup != null) {
            serverEventLoopGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        final Config config = ConfigFactory.create(Config.class);
        new App(config).run();
    }

}
