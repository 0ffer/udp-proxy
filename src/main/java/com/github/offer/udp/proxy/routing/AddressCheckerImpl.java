package com.github.offer.udp.proxy.routing;

import com.github.offer.udp.proxy.Config;
import org.apache.commons.net.util.SubnetUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implemantation of {@link AddressChecker}
 *
 * @author Stas Melnichuk
 */
public class AddressCheckerImpl implements AddressChecker {
    private List<SubnetUtils.SubnetInfo> allowedSubnets;

    public AddressCheckerImpl(Config config) {
        allowedSubnets = config.allowedSubnets().stream()
                .map(cidrNotation -> new SubnetUtils(cidrNotation).getInfo()).collect(Collectors.toList());
    }

    @Override
    public boolean isAddressAllowed(int address) {
        for (SubnetUtils.SubnetInfo subnetInfo : allowedSubnets) {
            if (subnetInfo.isInRange(address)) {
                return true;
            }
        }

        return false;
    }
}
