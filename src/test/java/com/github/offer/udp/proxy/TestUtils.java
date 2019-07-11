package com.github.offer.udp.proxy;

import com.github.offer.udp.proxy.statistics.MetricsOutputType;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;

/**
 * Some info for testing.
 *
 * @author Stas Melnichuk
 */
public class TestUtils {

    public static byte[] PING_PACKET = new byte[0];

    static {
        try {
            PING_PACKET = Hex.decodeHex("53 41 4d 50 b0 20 27 c8 87 09 70 3e 2a c3 02".replaceAll(" ", ""));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
    }

    public static Config getDefaultConfig() {
        final ConfigImpl result = new ConfigImpl();
        result.setProcessThreadsCount(8);
        result.setListenAddress("127.0.0.1");
        result.setListenPort(8080);
        result.setProxyingServerPort(1234);

        result.setMetricsEnable(true);
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
