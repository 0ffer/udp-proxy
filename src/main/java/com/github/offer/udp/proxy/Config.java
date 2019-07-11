package com.github.offer.udp.proxy;

import com.github.offer.udp.proxy.statistics.MetricsOutputType;

import java.util.List;

/**
 * Application configuration.
 *
 * @author Stas Melnichuk
 */
@org.aeonbits.owner.Config.Sources("file:/app.properties")
public interface Config extends org.aeonbits.owner.Config {

    @Key("app.thread.count")
    @DefaultValue("4")
    int processThreadsCount();

    @Key("app.bind.address")
    @DefaultValue("127.0.0.1")
    String listenAddress();

    @Key("app.bind.port")
    @DefaultValue("8080")
    int listenPort();

    @Key("app.port.remote")
    @DefaultValue("1234")
    int proxyingServerPort();

    @Key("app.metrics.enable")
    @DefaultValue("false")
    boolean isMetricsEnable();

    @Key("app.metrics.output")
    @DefaultValue("CONSOLE")
    MetricsOutputType metricsOutput();

    @Key("app.metrics.output.frequency")
    @DefaultValue("5000")
    int metricsOutputFrequency();

    @Key("app.allowed.networks")
    @DefaultValue("127.0.0.1/24")
    List<String> allowedSubnets();

    @Key("app.cache.response.expiration")
    @DefaultValue("1000")
    int responseCacheExpirationTimeMs();

    @Key("app.cache.request.block")
    @DefaultValue("1000")
    int requestBlockingTimeMs();

    @Key("app.cache.response.wait")
    @DefaultValue("500")
    int responseWaitingTimeMs();

}
