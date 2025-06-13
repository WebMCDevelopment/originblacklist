package dev.colbster937.originblacklist.base;

import java.util.logging.Logger;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

import static dev.colbster937.originblacklist.base.Base.config;

public class IPBlacklist {
    private Logger logger = null;

    public IPBlacklist() {
        this.logger = logger;
    }

    public boolean check(String addr) {
        IPAddress ip;
        String addr1 = addr;
        try {
            if (addr.startsWith("/")) {
                addr1 = addr.substring(1);
            }
            ip = new IPAddressString(addr1).toAddress();
        } catch (AddressStringException e) {
            throw new RuntimeException("Invalid IP address: " + addr, e);
        }

        return config.blacklist.ips1.stream().anyMatch(s -> {
            try {
                return s.contains(ip);
            } catch (Exception e) {
                return false;
            }
        });
    }
}
