package com.github.offer.udp.proxy;

import com.github.offer.udp.proxy.cache.PacketCache;
import com.github.offer.udp.proxy.cache.PacketCacheImpl;
import com.github.offer.udp.proxy.protocol.ProtocolHandler;
import com.github.offer.udp.proxy.routing.AddressChecker;
import com.github.offer.udp.proxy.routing.AddressCheckerImpl;
import com.github.offer.udp.proxy.statistics.MetricsOutputType;
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

import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * FIXME Требуется сделать возможность выбора типа используемого алгоритма чтений. (OIO, NIO, EPOLL)
 *
 * @author Stas Melnichuk
 */
public class App {
    private static final Logger LOG = Logger.getLogger(App.class.getName());

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
        LOG.info(() -> String.format("Server binds to %s:%s", config.listenAddress(), config.listenPort()));

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
        // Требуется отдельно настраивать JNI для graal - https://github.com/oracle/graal/blob/master/substratevm/JNI.md
//        if (Epoll.isAvailable()) {
//            LOG.info("EPOLL is available - use it.");
//            serverEventLoopGroup = new EpollEventLoopGroup(config.processThreadsCount());
//            channelClass = EpollDatagramChannel.class;
//        } else {
            LOG.info("EPOLL is not available - use NIO.");
            serverEventLoopGroup = new NioEventLoopGroup(config.processThreadsCount());
            channelClass = NioDatagramChannel.class;
//        }

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
        final Config config = getDefaultConfig();

        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setLevel(Level.WARNING);

        new App(config).run();
    }

    public static Config getDefaultConfig() {
        final ConfigImpl result = new ConfigImpl();
        result.setProcessThreadsCount(8);
        result.setListenAddress("127.0.0.1");
        result.setListenPort(8080); // default app port
        result.setProxyingServerPort(8082); // test echo server default port

        result.setMetricsEnable(false);
        result.setMetricsOutputType(MetricsOutputType.CONSOLE);
        result.setMetricsOutputFrequency(5000);

        result.setAllowedSubnets(new ArrayList<>());
        result.allowedSubnets().add("127.0.0.1/16");
        result.allowedSubnets().add("176.32.0.0/16");

        result.setResponseCacheExpirationTimeMs(1000);
        result.setRequestBlockingTimeMs(1000);
        result.setResponseWaitingTimeMs(500);

        return result;
    }

}
