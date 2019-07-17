package com.github.offer.udp.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *
 * Simple netty upd client to send single datagram and wait some time to response datagram.
 *
 * @author Stas Melnichuk
 */
public class TestClient {
    private static final Logger LOG = Logger.getLogger(TestClient.class.getName());

    private static final int DEFAULT_TIMEOUT = 5000;
    public static final int DEFAULT_BIND_PORT = 8081;

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private Channel channel;

    private final int timeout;

    private CyclicBarrier barrier = new CyclicBarrier(2);
    private volatile boolean isReceiveResponse = false;

    private volatile DatagramPacket responce;

    public TestClient(final int timeout) {

        this.timeout = timeout;

        group = new NioEventLoopGroup();

        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new TestClient.WriteHandler());
    }

    public TestClient() {
        this(DEFAULT_TIMEOUT);
    }

    public void run() throws Exception {
        this.run(DEFAULT_BIND_PORT);
    }

    public void run(final int port) throws Exception {
        ChannelFuture future = bootstrap.bind(port).sync();
        channel = future.channel();
    }

    public void stop() {
        group.shutdownGracefully();
    }

    public DatagramPacket send(final DatagramPacket datagramPacket) throws Exception {
        barrier.reset();
        responce = null;

        channel.writeAndFlush(datagramPacket);

        barrier.await(timeout, TimeUnit.MILLISECONDS);

        isReceiveResponse = false;

        return responce;
    }

    /**
     * Simple handler to write all input data.
     */
    private class WriteHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, io.netty.channel.socket.DatagramPacket msg) throws Exception {
            String response = msg.content().toString();
            System.out.println(response);

            TestClient.this.responce = msg.copy();

//            ctx.close();
            isReceiveResponse = true;
            barrier.await();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

}
