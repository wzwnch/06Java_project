package com.shortlink.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebInfoUtils {

    private static final String DEFAULT_TITLE = "未知标题";
    private static final String DEFAULT_FAVICON = "";

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<title[^>]*>([^<]+)</title>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FAVICON_PATTERN = Pattern.compile(
            "<link[^>]+rel=[\"'](?:icon|shortcut icon|apple-touch-icon)[\"'][^>]*href=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FAVICON_PATTERN2 = Pattern.compile(
            "<link[^>]+href=[\"']([^\"']+)[\"'][^>]*rel=[\"'](?:icon|shortcut icon|apple-touch-icon)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern OG_IMAGE_PATTERN = Pattern.compile(
            "<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static final int TIMEOUT = 5000;
    private static final int MAX_CONTENT_LENGTH = 1024 * 1024;

    private WebInfoUtils() {
    }

    public static WebInfo fetch(String url) {
        WebInfo webInfo = new WebInfo();

        if (StrUtil.isBlank(url)) {
            webInfo.setTitle(DEFAULT_TITLE);
            webInfo.setFaviconUrl(DEFAULT_FAVICON);
            return webInfo;
        }

        if (!UrlUtils.isValid(url)) {
            webInfo.setTitle(DEFAULT_TITLE);
            webInfo.setFaviconUrl(DEFAULT_FAVICON);
            return webInfo;
        }

        try {
            String html = fetchHtml(url);
            if (StrUtil.isNotBlank(html)) {
                String title = extractTitle(html);
                String favicon = extractFavicon(url, html);

                webInfo.setTitle(StrUtil.isNotBlank(title) ? title : DEFAULT_TITLE);
                webInfo.setFaviconUrl(StrUtil.isNotBlank(favicon) ? favicon : DEFAULT_FAVICON);
            } else {
                webInfo.setTitle(DEFAULT_TITLE);
                webInfo.setFaviconUrl(DEFAULT_FAVICON);
            }
        } catch (Exception e) {
            webInfo.setTitle(DEFAULT_TITLE);
            webInfo.setFaviconUrl(DEFAULT_FAVICON);
        }

        return webInfo;
    }

    public static String getTitle(String url) {
        if (StrUtil.isBlank(url) || !UrlUtils.isValid(url)) {
            return DEFAULT_TITLE;
        }

        try {
            String html = fetchHtml(url);
            if (StrUtil.isNotBlank(html)) {
                String title = extractTitle(html);
                return StrUtil.isNotBlank(title) ? title : DEFAULT_TITLE;
            }
        } catch (Exception e) {
            return DEFAULT_TITLE;
        }

        return DEFAULT_TITLE;
    }

    public static String getFavicon(String url) {
        if (StrUtil.isBlank(url) || !UrlUtils.isValid(url)) {
            return DEFAULT_FAVICON;
        }

        try {
            String html = fetchHtml(url);
            if (StrUtil.isNotBlank(html)) {
                String favicon = extractFavicon(url, html);
                return StrUtil.isNotBlank(favicon) ? favicon : DEFAULT_FAVICON;
            }
        } catch (Exception e) {
            return DEFAULT_FAVICON;
        }

        return DEFAULT_FAVICON;
    }

    private static String fetchHtml(String url) {
        try {
            HttpResponse response = HttpRequest.get(url)
                    .timeout(TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .execute();

            if (response.isOk()) {
                String content = response.body();
                if (content != null && content.length() > MAX_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_CONTENT_LENGTH);
                }
                return content;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static String extractTitle(String html) {
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (matcher.find()) {
            String title = matcher.group(1);
            title = title.replaceAll("\\s+", " ").trim();
            if (title.length() > 200) {
                title = title.substring(0, 200) + "...";
            }
            return title;
        }
        return null;
    }

    private static String extractFavicon(String baseUrl, String html) {
        Matcher matcher = FAVICON_PATTERN.matcher(html);
        if (matcher.find()) {
            return resolveUrl(baseUrl, matcher.group(1));
        }

        matcher = FAVICON_PATTERN2.matcher(html);
        if (matcher.find()) {
            return resolveUrl(baseUrl, matcher.group(1));
        }

        matcher = OG_IMAGE_PATTERN.matcher(html);
        if (matcher.find()) {
            return resolveUrl(baseUrl, matcher.group(1));
        }

        return UrlUtils.getFaviconUrl(baseUrl);
    }

    private static String resolveUrl(String baseUrl, String relativeUrl) {
        if (StrUtil.isBlank(relativeUrl)) {
            return null;
        }

        String trimmedUrl = relativeUrl.trim();

        if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            return trimmedUrl;
        }

        if (trimmedUrl.startsWith("//")) {
            String protocol = UrlUtils.getProtocol(baseUrl);
            return (protocol != null ? protocol : "https") + ":" + trimmedUrl;
        }

        try {
            String domain = UrlUtils.getDomain(baseUrl);
            String protocol = UrlUtils.getProtocol(baseUrl);

            if (StrUtil.isBlank(domain) || StrUtil.isBlank(protocol)) {
                return null;
            }

            if (trimmedUrl.startsWith("/")) {
                return protocol + "://" + domain + trimmedUrl;
            } else {
                String path = UrlUtils.getPath(baseUrl);
                String parentPath = path.substring(0, path.lastIndexOf('/') + 1);
                return protocol + "://" + domain + parentPath + trimmedUrl;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static class WebInfo {
        private String title;
        private String faviconUrl;

        public WebInfo() {
        }

        public WebInfo(String title, String faviconUrl) {
            this.title = title;
            this.faviconUrl = faviconUrl;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getFaviconUrl() {
            return faviconUrl;
        }

        public void setFaviconUrl(String faviconUrl) {
            this.faviconUrl = faviconUrl;
        }

        @Override
        public String toString() {
            return "WebInfo{" +
                    "title='" + title + '\'' +
                    ", faviconUrl='" + faviconUrl + '\'' +
                    '}';
        }
    }
}
