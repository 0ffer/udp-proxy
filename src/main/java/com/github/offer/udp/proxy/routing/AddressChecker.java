package com.github.offer.udp.proxy.routing;

/**
 * Manager to check the allowed addresses.
 *
 * @author Stas Melnichuk
 */
public interface AddressChecker {

    boolean isAddressAllowed(int address);

}
