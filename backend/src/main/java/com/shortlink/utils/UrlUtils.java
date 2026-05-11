package com.shortlink.utils;

import cn.hutool.core.util.StrUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class UrlUtils {

    private static final String URL_REGEX = "^(https?://)"
            + "(([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,}"
            + "|localhost"
            + "|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})"
            + "(:\\d+)?"
            + "(/[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?"
            + "(\\?[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?"
            + "(#[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?$";

    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE);

    private static final int MAX_URL_LENGTH = 2048;

    private static final String[] BLOCKED_PROTOCOLS = {"javascript:", "data:", "vbscript:", "file:"};

    private static final String[] BLOCKED_DOMAINS = {"localhost", "127.0.0.1", "0.0.0.0"};

    private UrlUtils() {
    }

    public static boolean isValid(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }

        if (url.length() > MAX_URL_LENGTH) {
            return false;
        }

        String trimmedUrl = url.trim();
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            return false;
        }

        for (String protocol : BLOCKED_PROTOCOLS) {
            if (trimmedUrl.toLowerCase().startsWith(protocol)) {
                return false;
            }
        }

        return URL_PATTERN.matcher(trimmedUrl).matches();
    }

    public static boolean isValidWithStrict(String url) {
        if (!isValid(url)) {
            return false;
        }

        try {
            URL parsedUrl = new URL(url.trim());
            String host = parsedUrl.getHost();

            if (StrUtil.isBlank(host)) {
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static String normalize(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        String trimmedUrl = url.trim();

        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            trimmedUrl = "https://" + trimmedUrl;
        }

        try {
            URL parsedUrl = new URL(trimmedUrl);
            String protocol = parsedUrl.getProtocol().toLowerCase();
            String host = parsedUrl.getHost().toLowerCase();
            int port = parsedUrl.getPort();
            String path = parsedUrl.getPath();
            String query = parsedUrl.getQuery();
            String ref = parsedUrl.getRef();

            StringBuilder normalized = new StringBuilder();
            normalized.append(protocol).append("://").append(host);

            if (port != -1 && port != 80 && port != 443) {
                normalized.append(":").append(port);
            }

            if (StrUtil.isNotBlank(path)) {
                normalized.append(path);
            } else {
                normalized.append("/");
            }

            if (StrUtil.isNotBlank(query)) {
                normalized.append("?").append(query);
            }

            if (StrUtil.isNotBlank(ref)) {
                normalized.append("#").append(ref);
            }

            return normalized.toString();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static String getDomain(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        try {
            URL parsedUrl = new URL(url.trim());
            return parsedUrl.getHost().toLowerCase();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static String getProtocol(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        try {
            URL parsedUrl = new URL(url.trim());
            return parsedUrl.getProtocol().toLowerCase();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static String getPath(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        try {
            URL parsedUrl = new URL(url.trim());
            String path = parsedUrl.getPath();
            return StrUtil.isBlank(path) ? "/" : path;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static boolean isBlockedDomain(String url) {
        if (StrUtil.isBlank(url)) {
            return true;
        }

        String domain = getDomain(url);
        if (StrUtil.isBlank(domain)) {
            return true;
        }

        for (String blocked : BLOCKED_DOMAINS) {
            if (domain.equalsIgnoreCase(blocked)) {
                return true;
            }
        }

        return false;
    }

    public static String getFaviconUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        try {
            URL parsedUrl = new URL(url.trim());
            String protocol = parsedUrl.getProtocol();
            String host = parsedUrl.getHost();
            int port = parsedUrl.getPort();

            StringBuilder faviconUrl = new StringBuilder();
            faviconUrl.append(protocol).append("://").append(host);

            if (port != -1 && port != 80 && port != 443) {
                faviconUrl.append(":").append(port);
            }

            faviconUrl.append("/favicon.ico");
            return faviconUrl.toString();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static String buildShortLinkUrl(String domain, String shortCode) {
        if (StrUtil.isBlank(domain) || StrUtil.isBlank(shortCode)) {
            return null;
        }

        String normalizedDomain = domain.trim();
        if (!normalizedDomain.startsWith("http://") && !normalizedDomain.startsWith("https://")) {
            normalizedDomain = "https://" + normalizedDomain;
        }

        if (normalizedDomain.endsWith("/")) {
            normalizedDomain = normalizedDomain.substring(0, normalizedDomain.length() - 1);
        }

        return normalizedDomain + "/" + shortCode;
    }
}
