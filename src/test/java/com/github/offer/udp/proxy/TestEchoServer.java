package com.github.offer.udp.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple test server with echo + "hello" functionality.
 *
 * @author Stas Melnichuk
 */
public class TestEchoServer {
    private static final Logger LOG = LogManager.getLogger(TestClient.class);

    public static final int DEFAULT_BIND_PORT = 8082;

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private Channel channel;

    private AtomicInteger handledRequestsCount = new AtomicInteger();

    public TestEchoServer() {

        group = new NioEventLoopGroup();

        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new TestEchoServer.WriteHandler());
    }

    public void run() throws Exception {
        this.run(DEFAULT_BIND_PORT);
    }

    public void run(final int port) throws Exception {
        ChannelFuture future = bootstrap.bind(port).sync();
        channel = future.channel();
    }

    public int getHandledRequestsCount() {
        return handledRequestsCount.get();
    }

    public void stop() {
        group.shutdownGracefully();
    }

    /**
     * Simple handler to write all input data.
     */
    private class WriteHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, io.netty.channel.socket.DatagramPacket msg) throws Exception {
            TestEchoServer.this.handledRequestsCount.incrementAndGet();

            int msgSize = msg.content().readableBytes();
            ByteBuf buffer = ctx.alloc().buffer(msgSize + 5);

            buffer.writeBytes(msg.content());
            buffer.writeChar('h');
            buffer.writeChar('e');
            buffer.writeChar('l');
            buffer.writeChar('l');
            buffer.writeChar('o');

            String response = buffer.toString();
            System.out.println("Echo... " + response);

            ctx.writeAndFlush(new DatagramPacket(buffer, msg.sender(), msg.recipient()));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
