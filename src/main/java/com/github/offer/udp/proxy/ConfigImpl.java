package com.github.offer.udp.proxy;

import com.github.offer.udp.proxy.statistics.MetricsOutputType;

import java.util.List;


/**
 * Simple POJO implementation of {@link Config} interface.
 *
 * @author Stas Melnichuk
 */
public class ConfigImpl implements Config {
    private int processThreadsCount;
    private String listenAddress;
    private int listenPort;
    private int proxyingServerPort;
    private boolean isMetricsEnable;
    private MetricsOutputType metricsOutputType;
    private int metricsOutputFrequency;
    private List<String> allowedSubnets;
    private int responseCacheExpirationTimeMs;
    private int requestBlockingTimeMs;
    private int responseWaitingTimeMs;

    @Override
    public int processThreadsCount() {
        return processThreadsCount;
    }
    public void setProcessThreadsCount(int processThreadsCount) {
        this.processThreadsCount = processThreadsCount;
    }

    @Override
    public int listenPort() {
        return listenPort;
    }
    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    @Override
    public String listenAddress() {
        return listenAddress;
    }
    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }

    @Override
    public int proxyingServerPort() {
        return proxyingServerPort;
    }
    public void setProxyingServerPort(int proxyingServerPort) {
        this.proxyingServerPort = proxyingServerPort;
    }

    @Override
    public boolean isMetricsEnable() {
        return isMetricsEnable;
    }
    public void setMetricsEnable(boolean metricsEnable) {
        isMetricsEnable = metricsEnable;
    }

    @Override
    public MetricsOutputType metricsOutput() {
        return metricsOutputType;
    }
    public void setMetricsOutputType(MetricsOutputType metricsOutputType) {
        this.metricsOutputType = metricsOutputType;
    }

    @Override
    public int metricsOutputFrequency() {
        return metricsOutputFrequency;
    }
    public void setMetricsOutputFrequency(int metricsOutputFrequency) {
        this.metricsOutputFrequency = metricsOutputFrequency;
    }

    @Override
    public List<String> allowedSubnets() {
        return allowedSubnets;
    }
    public void setAllowedSubnets(List<String> allowedSubnets) {
        this.allowedSubnets = allowedSubnets;
    }

    @Override
    public int responseCacheExpirationTimeMs() {
        return responseCacheExpirationTimeMs;
    }
    public void setResponseCacheExpirationTimeMs(int responseCacheExpirationTimeMs) {
        this.responseCacheExpirationTimeMs = responseCacheExpirationTimeMs;
    }

    @Override
    public int requestBlockingTimeMs() {
        return requestBlockingTimeMs;
    }
    public void setRequestBlockingTimeMs(int requestBlockingTimeMs) {
        this.requestBlockingTimeMs = requestBlockingTimeMs;
    }

    @Override
    public int responseWaitingTimeMs() {
        return responseWaitingTimeMs;
    }
    public void setResponseWaitingTimeMs(int responseWaitingTimeMs) {
        this.responseWaitingTimeMs = responseWaitingTimeMs;
    }
}
