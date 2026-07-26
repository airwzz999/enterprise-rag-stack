package com.knowledge.base.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Security utility class
 *
 * <p>Provides security-related utility methods</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class SecurityUtil {

    /**
     * Get the client's IP address
     *
     * @param request the HttpServletRequest
     * @return the IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Handle the case of multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * Get the user agent
     *
     * @param request the HttpServletRequest
     * @return the user agent
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * Check whether the request is from a mobile device
     *
     * @param request the HttpServletRequest
     * @return whether it is a mobile device
     */
    public static boolean isMobileDevice(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (userAgent == null) {
            return false;
        }

        return userAgent.toLowerCase().matches(".*(android|iphone|ipad|ipod|windows phone|mobile).*");
    }

    /**
     * Get the browser type
     *
     * @param request the HttpServletRequest
     * @return the browser type
     */
    public static String getBrowserType(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (userAgent == null) {
            return "Unknown";
        }

        if (userAgent.contains("Chrome")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari")) {
            return "Safari";
        } else if (userAgent.contains("Edge")) {
            return "Edge";
        } else if (userAgent.contains("Opera")) {
            return "Opera";
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            return "Internet Explorer";
        } else {
            return "Unknown";
        }
    }

    /**
     * Get the operating system
     *
     * @param request the HttpServletRequest
     * @return the operating system
     */
    public static String getOperatingSystem(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (userAgent == null) {
            return "Unknown";
        }

        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac")) {
            return "MacOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iPod")) {
            return "iOS";
        } else {
            return "Unknown";
        }
    }

    /**
     * HTML escaping
     *
     * @param input the input string
     * @return the escaped string
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }

        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    /**
     * SQL injection check
     *
     * @param input the input string
     * @return whether it contains a SQL injection pattern
     */
    public static boolean containsSqlInjection(String input) {
        if (input == null) {
            return false;
        }

        String[] sqlKeywords = {
                "select", "insert", "update", "delete", "drop", "union",
                "exec", "execute", "script", "javascript", "alert",
                "--", "/*", "*/", ";", "'", "\"", "=", "or", "and"
        };

        String lowerInput = input.toLowerCase();
        for (String keyword : sqlKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * XSS attack check
     *
     * @param input the input string
     * @return whether it contains an XSS attack pattern
     */
    public static boolean containsXssAttack(String input) {
        if (input == null) {
            return false;
        }

        String[] xssPatterns = {
                "<script", "</script>", "javascript:", "onerror=", "onload=",
                "onclick=", "onmouseover=", "onfocus=", "onblur=",
                "<iframe", "</iframe>", "<object", "</object>", "<embed"
        };

        String lowerInput = input.toLowerCase();
        for (String pattern : xssPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sanitize an input string
     *
     * @param input the input string
     * @return the sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Remove dangerous characters
        return input.replaceAll("[<>\"'']", "")
                     .replaceAll("[/\\\\*]", "")
                     .trim();
    }
}
