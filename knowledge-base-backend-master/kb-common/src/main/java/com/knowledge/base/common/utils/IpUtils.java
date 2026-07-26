package com.knowledge.base.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP utility class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
public class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final String LOCALHOST_IP_16 = "0:0:0:0:0:0:0:1";
    private static final int IP_LEN = 15;

    /**
     * Get the client's IP address
     *
     * @param request the request object
     * @return the IP address
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = null;

        // X-Forwarded-For
        ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            // Proxy-Client-IP
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            // WL-Proxy-Client-IP
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            // HTTP_CLIENT_IP
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            // HTTP_X_FORWARDED_FOR
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            // X-Real-IP
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            // Remote Addr
            ip = request.getRemoteAddr();
        }

        // When the request passes through multiple proxies, the first IP is the real client IP, IPs are separated by ','
        if (ip != null && ip.length() > IP_LEN) {
            int index = ip.indexOf(",");
            if (index > 0) {
                ip = ip.substring(0, index);
            }
        }

        // Handle the local IP
        if (LOCALHOST_IP_16.equals(ip)) {
            ip = LOCALHOST_IP;
        }

        return ip;
    }

    /**
     * Get the real-world address from an IP address
     *
     * @param ip the IP address
     * @return the real-world address
     */
    public static String getRealAddressByIP(String ip) {
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            return "Unknown";
        }

        // Local address
        if (LOCALHOST_IP.equals(ip) || LOCALHOST_IP_16.equals(ip)) {
            return "Local";
        }

        try {
            // A third-party IP geolocation library could be integrated here, e.g. ip2region, GeoIP2, etc.
            // For now, just return the IP address
            InetAddress address = InetAddress.getByName(ip);
            return address.getHostName();
        } catch (UnknownHostException e) {
            log.error("Failed to resolve IP address: {}", e.getMessage());
            return ip;
        }
    }

    /**
     * Check whether an IP is an internal (private) IP
     *
     * @param ip the IP address
     * @return whether it is an internal IP
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Local loopback address
        if (LOCALHOST_IP.equals(ip) || LOCALHOST_IP_16.equals(ip)) {
            return true;
        }

        // Private IP ranges
        return ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                ip.startsWith("172.16.") ||
                ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") ||
                ip.startsWith("172.19.") ||
                ip.startsWith("172.20.") ||
                ip.startsWith("172.21.") ||
                ip.startsWith("172.22.") ||
                ip.startsWith("172.23.") ||
                ip.startsWith("172.24.") ||
                ip.startsWith("172.25.") ||
                ip.startsWith("172.26.") ||
                ip.startsWith("172.27.") ||
                ip.startsWith("172.28.") ||
                ip.startsWith("172.29.") ||
                ip.startsWith("172.30.") ||
                ip.startsWith("172.31.");
    }

    /**
     * Validate an IP address format
     *
     * @param ip the IP address
     * @return whether it is valid
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        String regex = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        return ip.matches(regex);
    }
}
