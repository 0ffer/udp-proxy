package com.github.offer.udp.proxy.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheWriter;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.offer.udp.proxy.Config;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link PacketCache}
 *
 * @author Stas Melnichuk
 */
public class PacketCacheImpl implements PacketCache {

    private static final InetSocketAddress[] EMPTY_WAITERS = new InetSocketAddress[0];

    private final Cache<Integer, List<InetSocketAddress>> responseWaitersCache;
    private final Cache<Integer, Boolean> blockedRequestsCache;
    private final Cache<Integer, ByteBuf> responseCache;


    public PacketCacheImpl(final Config config) {

        responseCache = Caffeine.newBuilder()
                .expireAfterWrite(config.responseCacheExpirationTimeMs(), TimeUnit.MILLISECONDS)
                .removalListener((key, value, cause) -> ReferenceCountUtil.release(value))
                .build();

        blockedRequestsCache = Caffeine.newBuilder()
                .expireAfterWrite(config.requestBlockingTimeMs(), TimeUnit.MILLISECONDS)
                .build();

        final CacheWriter<Integer, List<InetSocketAddress>> waitersRemovalListener = new CacheWriter<Integer, List<InetSocketAddress>>() {
            @Override
            public void write(@NonNull Integer key, @NonNull List<InetSocketAddress> value) {}

            @Override
            public void delete(@NonNull Integer key, @Nullable List<InetSocketAddress> value, @NonNull RemovalCause cause) {
                if (cause == RemovalCause.EXPIRED) { // If it expired and not deleted manually -> proxying server not send response in time.
                    blockedRequestsCache.put(key, true); // then set response block to proxying server.
                }
            }
        };

        responseWaitersCache = Caffeine.newBuilder()
                .expireAfterWrite(config.responseWaitingTimeMs(), TimeUnit.MILLISECONDS)
                .writer(waitersRemovalListener)
                .build();

        // Schedule more frequent waiters cache clean. It need for requests blocking correctness.
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(responseWaitersCache::cleanUp,
                config.responseWaitingTimeMs(),
                config.responseWaitingTimeMs()/2, TimeUnit.MILLISECONDS);
    }

    @Override
    public InetSocketAddress[] getAndReleaseWaitedRecipients(int recipientAddress, byte packetType) {
        final int hash = hash(recipientAddress, packetType);
        final List<InetSocketAddress> cached = responseWaitersCache.getIfPresent(hash);

        if (cached == null) {
            return EMPTY_WAITERS;
        }

        responseWaitersCache.invalidate(hash);
        return cached.toArray(EMPTY_WAITERS);
    }

    @Override
    public void addToResponseWaitersQueue(final InetSocketAddress senderAddress, final int recipientAddress, final byte packetType) {
        final List<InetSocketAddress> waiters = responseWaitersCache.get(hash(recipientAddress, packetType), key -> new ArrayList<>());

        waiters.add(senderAddress);
    }

    @Override
    public boolean isAlreadyWaitResponse(int recipientAddress, byte packetType) {
        return responseWaitersCache.getIfPresent(hash(recipientAddress, packetType)) != null;
    }

    @Override
    public ByteBuf tryGet(int recipientAddress, byte packetType) {
        return responseCache.getIfPresent(hash(recipientAddress, packetType));
    }

    @Override
    public void cache(int recipientAddress, byte packetType, ByteBuf content) {
        responseCache.put(hash(recipientAddress, packetType), content);
    }

    @Override
    public boolean isNeedBlock(int recipientAddress, byte paccketType) {
        Boolean cacheValue = blockedRequestsCache.getIfPresent(hash(recipientAddress, paccketType));
        if (cacheValue == null) {
            cacheValue = false;
        }

        return cacheValue;
    }

    /**
     * Get hash code from server address and packet type.
     *
     * See algo in {@link HashCodeBuilder}
     *
     * @param serverAddress Aim server address.
     * @param packetType Packet type byte
     * @return Hash code from input values.
     */
    private static int hash(final int serverAddress, final byte packetType) {
        int iTotal = 17;
        iTotal = iTotal * 37 + serverAddress;
        iTotal = iTotal * 37 + packetType;
        return iTotal;
    }
}
