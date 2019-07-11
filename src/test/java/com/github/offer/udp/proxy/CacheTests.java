package com.github.offer.udp.proxy;

import com.github.offer.udp.proxy.cache.PacketCache;
import com.github.offer.udp.proxy.cache.PacketCacheImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;

/**
 * Simple one-thread tests to ensure that time rules is right.
 *
 * @author Stas Melnichuk
 */
public class CacheTests {

    @Test
    public void takeFromCacheTest() throws Exception {
        final PacketCache packetCache = new PacketCacheImpl(TestUtils.getDefaultConfig());

        ByteBuf bufferToCache = Unpooled.buffer(4);
        bufferToCache.writeInt(100500);

        packetCache.cache(1, (byte) 1, bufferToCache);

        Thread.sleep(150);

        final ByteBuf cached = packetCache.tryGet(1, (byte) 1);
        assertEquals(100500, cached.readInt());
    }

    @Test
    public void packetCacheExpiredTest() throws Exception {
        final PacketCache packetCache = new PacketCacheImpl(TestUtils.getDefaultConfig());

        ByteBuf bufferToCache = Unpooled.buffer(4);
        bufferToCache.writeInt(100500);

        packetCache.cache(1, (byte) 1, bufferToCache);

        Thread.sleep(1050);

        final ByteBuf cached = packetCache.tryGet(1, (byte) 1);
        assertNull(cached);
    }

    @Test
    public void addAndGetResponseWaiters() throws Exception {
        final PacketCache packetCache = new PacketCacheImpl(TestUtils.getDefaultConfig());

        packetCache.addToResponseWaitersQueue(new InetSocketAddress("127.0.0.1", 1234), 1, (byte) 1);

        assertTrue(packetCache.isAlreadyWaitResponse(1, (byte) 1));

        final InetSocketAddress[] cached = packetCache.getAndReleaseWaitedRecipients(1, (byte) 1);

        assertEquals(1, cached.length);
        assertFalse(packetCache.isAlreadyWaitResponse(1, (byte) 1));
    }

    @Test
    public void deleteWaitersAndPacketsBlockingTest() throws Exception {
        final PacketCache packetCache = new PacketCacheImpl(TestUtils.getDefaultConfig());

        packetCache.addToResponseWaitersQueue(new InetSocketAddress("127.0.0.1", 1234), 1, (byte) 1);

        assertTrue(packetCache.isAlreadyWaitResponse(1, (byte) 1));

        Thread.sleep(800);

        assertTrue(packetCache.isNeedBlock(1, (byte) 1));
        assertArrayEquals(new InetSocketAddress[0], packetCache.getAndReleaseWaitedRecipients(1, (byte) 1));

        Thread.sleep(1000);
        assertFalse(packetCache.isNeedBlock(1, (byte) 1));
    }
}
