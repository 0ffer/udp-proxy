package com.github.offer.udp.proxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.SocketUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ProtocolHandlerTests {
    private static final Logger LOG = LogManager.getLogger(ProtocolHandlerTests.class);

    private static App app;
    private static Config config;

    @BeforeClass
    public static void before() throws Exception {
        config = TestUtils.getDefaultConfig();
        app = new App(config);
        app.run();
    }

    @AfterClass
    public static void after() throws Exception {
        app.stop();
    }

    @Test
    public void pingPacketTest() throws Exception {
        final TestClient testClient = new TestClient();
        try {
            testClient.run();

            DatagramPacket request = new DatagramPacket(
                    Unpooled.copiedBuffer(TestUtils.PING_PACKET),
                    SocketUtils.socketAddress("127.0.0.1", config.listenPort()));

            final DatagramPacket response = testClient.send(request);

            assertEquals(15, response.content().readableBytes());

            final byte[] responseBytes = Utils.readAllReadableBytes(response.content());
            assertArrayEquals(TestUtils.PING_PACKET, responseBytes);

            LOG.info(response);
        } catch (Exception ex) {
            throw ex;
        } finally {
            testClient.stop();
        }

    }

    @Test
    public void cachablePacketTest() throws Exception {
        final TestEchoServer testEchoServer = new TestEchoServer();
        final TestClient testClient = new TestClient();
        try {
            testEchoServer.run();
            testClient.run();

            // Take from remote server.
            testClient.send(new DatagramPacket(
                    Unpooled.copiedBuffer(TestUtils.DATA_REQUEST_PACKET),
                    SocketUtils.socketAddress("127.0.0.1", config.listenPort())));
            assertEquals(1, testEchoServer.getHandledRequestsCount());

            // Already in cache.
            testClient.send(new DatagramPacket(
                    Unpooled.copiedBuffer(TestUtils.DATA_REQUEST_PACKET),
                    SocketUtils.socketAddress("127.0.0.1", config.listenPort())));
            assertEquals(1, testEchoServer.getHandledRequestsCount());

            // Cache is expiring - second request to remote server.
            Thread.sleep(1000);
            testClient.send(new DatagramPacket(
                    Unpooled.copiedBuffer(TestUtils.DATA_REQUEST_PACKET),
                    SocketUtils.socketAddress("127.0.0.1", config.listenPort())));
            assertEquals(2, testEchoServer.getHandledRequestsCount());

        } catch (Exception ex) {
            throw ex;
        } finally {
            testEchoServer.stop();
            testClient.stop();
        }
    }
}