package com.github.offer.udp.proxy.cache;

import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;

/**
 * Cache to save packets and store clients that needs to get response.
 *
 * @author Stas Melnichuk
 */
public interface PacketCache {

    /**
     * TODO Треуется очищать список.
     *
     * @param recipientAddress
     * @param packetType
     * @return
     */
    InetSocketAddress[] getAndReleaseWaitedRecipients(int recipientAddress, byte packetType);

    /**
     *
     * @param senderAddress
     * @param recipientAddress
     * @param packetType
     */
    void addToResponseWaitersQueue(InetSocketAddress senderAddress, int recipientAddress, byte packetType);

    /**
     *
     * TODO Требуется сделать ожидание ответа ограниченным по времени, если удаленный сервер не справляется.
     *
     * Просто по таймауту очищать этот список.
     *
     * @param recipientAddress
     * @param packetType
     * @return
     */
    boolean isAlreadyWaitResponse(int recipientAddress, byte packetType);

    ByteBuf tryGet(int recipientAddress, byte packetType);

    void cache(int recipientAddress, byte packetType, ByteBuf content);

    /**
     * If server not receive response in configured time delay we need to ignore all request to the server some time.
     *
     * FIXME Тут похоже третий кеш будет - записываем на некоторое время булево что требуется игнорировать входящие запросы.
     *  Опирается на протухание кеша со списком ожидающих адресатов.
     *
     * @param recipientAddress
     * @param paccketType
     * @return
     */
    boolean isNeedBlock(int recipientAddress, byte paccketType);

}
