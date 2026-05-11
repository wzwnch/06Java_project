package com.shortlink.utils;

import cn.hutool.core.util.StrUtil;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

public class IpUtils {

    private static final Logger log = LoggerFactory.getLogger(IpUtils.class);

    private static final String IP_REGION_DB = "ip2region.xdb";

    private static final String UNKNOWN = "未知";

    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    private static byte[] dbContent = null;

    private static final ConcurrentHashMap<String, IpInfo> CACHE = new ConcurrentHashMap<>();

    private static final int CACHE_MAX_SIZE = 10000;

    static {
        try {
            ClassPathResource resource = new ClassPathResource(IP_REGION_DB);
            try (InputStream is = resource.getInputStream()) {
                dbContent = is.readAllBytes();
            }
        } catch (IOException e) {
            log.warn("IP地区数据库加载失败，IP解析将返回'未知': {}", e.getMessage());
            dbContent = null;
        }
    }

    private IpUtils() {
    }

    public static IpInfo parse(String ip) {
        if (StrUtil.isBlank(ip)) {
            log.debug("IP为空，返回'未知'");
            return new IpInfo(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
        }

        String trimmedIp = ip.trim();

        if (isLocalIp(trimmedIp)) {
            return new IpInfo("本地", "本地", "本地", "本地");
        }

        IpInfo cached = CACHE.get(trimmedIp);
        if (cached != null) {
            return cached;
        }

        IpInfo ipInfo = doParse(trimmedIp);

        if (CACHE.size() < CACHE_MAX_SIZE) {
            CACHE.put(trimmedIp, ipInfo);
        }

        return ipInfo;
    }

    private static IpInfo doParse(String ip) {
        if (dbContent == null) {
            log.debug("IP地区数据库未加载，IP: {} 解析失败，标记为'未知'", ip);
            return new IpInfo(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
        }

        try {
            Searcher searcher = Searcher.newWithBuffer(dbContent);
            String region = searcher.search(ip);
            searcher.close();

            return parseRegion(region, ip);
        } catch (Exception e) {
            log.debug("IP解析失败，IP: {}，错误: {}，标记为'未知'", ip, e.getMessage());
            return new IpInfo(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
        }
    }

    private static IpInfo parseRegion(String region, String ip) {
        if (StrUtil.isBlank(region)) {
            log.debug("IP地区信息为空，IP: {}，标记为'未知'", ip);
            return new IpInfo(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
        }

        String[] parts = region.split("\\|");

        String country = getValue(parts, 0);
        String province = getValue(parts, 2);
        String city = getValue(parts, 3);
        String isp = getValue(parts, 4);

        if (country.equals("中国") && !province.equals(UNKNOWN)) {
            if (!city.equals(UNKNOWN) && !city.equals(province)) {
                province = province.replace("省", "");
                city = city.replace("市", "");
            }
        }

        return new IpInfo(country, province, city, isp);
    }

    private static String getValue(String[] parts, int index) {
        if (index >= parts.length) {
            return UNKNOWN;
        }
        String value = parts[index];
        if (StrUtil.isBlank(value) || "0".equals(value)) {
            return UNKNOWN;
        }
        return value;
    }

    public static boolean isLocalIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }

        String trimmedIp = ip.trim();

        if (LOCALHOST_IPV4.equals(trimmedIp) || LOCALHOST_IPV6.equals(trimmedIp)) {
            return true;
        }

        if (trimmedIp.startsWith("192.168.") || trimmedIp.startsWith("10.") || trimmedIp.startsWith("172.")) {
            return true;
        }

        return false;
    }

    public static boolean isValidIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }

        String trimmedIp = ip.trim();

        String ipv4Pattern = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        if (trimmedIp.matches(ipv4Pattern)) {
            return true;
        }

        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
        if (trimmedIp.matches(ipv6Pattern)) {
            return true;
        }

        String ipv6ShortPattern = "^(([0-9a-fA-F]{1,4}:){1,7}|:)(([0-9a-fA-F]{1,4}:){1,7}|:)$";
        if (trimmedIp.matches(ipv6ShortPattern)) {
            return true;
        }

        return false;
    }

    public static String getRegion(String ip) {
        return parse(ip).getProvince();
    }

    public static String getFullRegion(String ip) {
        IpInfo ipInfo = parse(ip);
        StringBuilder sb = new StringBuilder();

        if (!ipInfo.getCountry().equals(UNKNOWN)) {
            sb.append(ipInfo.getCountry());
        }
        if (!ipInfo.getProvince().equals(UNKNOWN)) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append(ipInfo.getProvince());
        }
        if (!ipInfo.getCity().equals(UNKNOWN) && !ipInfo.getCity().equals(ipInfo.getProvince())) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append(ipInfo.getCity());
        }

        return sb.length() > 0 ? sb.toString() : UNKNOWN;
    }

    public static String getIsp(String ip) {
        return parse(ip).getIsp();
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static class IpInfo {
        private final String country;
        private final String province;
        private final String city;
        private final String isp;

        public IpInfo(String country, String province, String city, String isp) {
            this.country = country;
            this.province = province;
            this.city = city;
            this.isp = isp;
        }

        public String getCountry() {
            return country;
        }

        public String getProvince() {
            return province;
        }

        public String getCity() {
            return city;
        }

        public String getIsp() {
            return isp;
        }

        @Override
        public String toString() {
            return "IpInfo{" +
                    "country='" + country + '\'' +
                    ", province='" + province + '\'' +
                    ", city='" + city + '\'' +
                    ", isp='" + isp + '\'' +
                    '}';
        }
    }
}
