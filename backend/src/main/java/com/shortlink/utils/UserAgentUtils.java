package com.shortlink.utils;

import cn.hutool.core.util.StrUtil;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;

public class UserAgentUtils {

    private static final String UNKNOWN = "未知";

    private UserAgentUtils() {
    }

    public static UaInfo parse(String userAgentString) {
        if (StrUtil.isBlank(userAgentString)) {
            return new UaInfo(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
        }

        try {
            UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString.trim());

            Browser browser = userAgent.getBrowser();
            OperatingSystem os = userAgent.getOperatingSystem();
            DeviceType deviceType = os != null ? os.getDeviceType() : null;

            String browserName = getBrowserName(browser);
            String osName = getOsName(os, userAgentString);
            String deviceName = getDeviceName(deviceType);
            String networkType = parseNetworkType(userAgentString);

            return new UaInfo(browserName, osName, deviceName, networkType);
        } catch (Exception e) {
            return new UaInfo(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
        }
    }

    private static String getBrowserName(Browser browser) {
        if (browser == null) {
            return UNKNOWN;
        }

        String name = browser.getName();
        if (StrUtil.isBlank(name)) {
            return UNKNOWN;
        }

        return simplifyBrowserName(name);
    }

    private static String simplifyBrowserName(String name) {
        if (name == null) {
            return UNKNOWN;
        }

        String lowerName = name.toLowerCase();

        if (lowerName.contains("chrome") && !lowerName.contains("edge") && !lowerName.contains("opera")) {
            return "Chrome";
        }
        if (lowerName.contains("firefox")) {
            return "Firefox";
        }
        if (lowerName.contains("safari") && !lowerName.contains("chrome")) {
            return "Safari";
        }
        if (lowerName.contains("edge") || lowerName.contains("edg")) {
            return "Edge";
        }
        if (lowerName.contains("opera") || lowerName.contains("opr")) {
            return "Opera";
        }
        if (lowerName.contains("ie") || lowerName.contains("internet explorer") || lowerName.contains("msie")) {
            return "IE";
        }
        if (lowerName.contains("micromessenger")) {
            return "微信";
        }
        if (lowerName.contains("weibo")) {
            return "微博";
        }
        if (lowerName.contains("qq")) {
            return "QQ";
        }
        if (lowerName.contains("ucbrowser") || lowerName.contains("ucweb")) {
            return "UC浏览器";
        }
        if (lowerName.contains("baidu") || lowerName.contains("bidu")) {
            return "百度浏览器";
        }
        if (lowerName.contains("360")) {
            return "360浏览器";
        }
        if (lowerName.contains("sogou")) {
            return "搜狗浏览器";
        }

        return name.length() > 20 ? name.substring(0, 20) : name;
    }

    private static String getOsName(OperatingSystem os, String userAgentString) {
        if (os == null) {
            return UNKNOWN;
        }

        String name = os.getName();
        if (StrUtil.isBlank(name)) {
            return UNKNOWN;
        }

        return simplifyOsName(name, userAgentString);
    }

    private static String simplifyOsName(String name, String userAgentString) {
        if (name == null) {
            return UNKNOWN;
        }

        String lowerName = name.toLowerCase();
        String lowerUa = userAgentString != null ? userAgentString.toLowerCase() : "";

        if (lowerName.contains("windows") || lowerUa.contains("windows")) {
            if (isWindows11(userAgentString)) {
                return "Windows 11";
            }
            if (lowerName.contains("10") || lowerUa.contains("windows nt 10")) {
                return "Windows 10";
            }
            if (lowerName.contains("7")) {
                return "Windows 7";
            }
            if (lowerName.contains("8")) {
                return "Windows 8";
            }
            if (lowerName.contains("xp")) {
                return "Windows XP";
            }
            return "Windows";
        }
        if (lowerName.contains("mac") || lowerName.contains("osx") || lowerName.contains("os x")) {
            return "macOS";
        }
        if (lowerName.contains("linux")) {
            return "Linux";
        }
        if (lowerName.contains("android")) {
            return "Android";
        }
        if (lowerName.contains("iphone") || lowerName.contains("ios") || lowerName.contains("ipad")) {
            return "iOS";
        }
        if (lowerName.contains("ubuntu")) {
            return "Ubuntu";
        }
        if (lowerName.contains("centos")) {
            return "CentOS";
        }
        if (lowerName.contains("debian")) {
            return "Debian";
        }

        return name.length() > 20 ? name.substring(0, 20) : name;
    }

    private static boolean isWindows11(String userAgentString) {
        if (userAgentString == null) {
            return false;
        }
        
        String lowerUa = userAgentString.toLowerCase();
        
        if (!lowerUa.contains("windows nt 10")) {
            return false;
        }
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("windows nt 10\\.0;(\\s)*\\d+(\\.\\d+)*");
        java.util.regex.Matcher matcher = pattern.matcher(lowerUa);
        if (matcher.find()) {
            return true;
        }
        
        if (lowerUa.contains("windows nt 10.0; win64") && !lowerUa.contains("windows nt 10.0; win64; x64; trident")) {
            int ntIndex = lowerUa.indexOf("windows nt 10.0");
            if (ntIndex > 0) {
                String afterNt = lowerUa.substring(ntIndex);
                java.util.regex.Pattern versionPattern = java.util.regex.Pattern.compile("windows nt 10\\.0[^;]*;\\s*([^;\\)]+)");
                java.util.regex.Matcher versionMatcher = versionPattern.matcher(afterNt);
                if (versionMatcher.find()) {
                    String buildPart = versionMatcher.group(1);
                    try {
                        String numStr = buildPart.replaceAll("[^0-9]", "");
                        if (!numStr.isEmpty()) {
                            long buildNumber = Long.parseLong(numStr);
                            return buildNumber >= 22000;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        
        return false;
    }

    private static String getDeviceName(DeviceType deviceType) {
        if (deviceType == null) {
            return UNKNOWN;
        }

        return switch (deviceType) {
            case COMPUTER -> "电脑";
            case MOBILE -> "手机";
            case TABLET -> "平板";
            case WEARABLE -> "可穿戴设备";
            case GAME_CONSOLE -> "游戏机";
            case DMR -> "数字媒体接收器";
            default -> UNKNOWN;
        };
    }

    private static String parseNetworkType(String userAgentString) {
        if (StrUtil.isBlank(userAgentString)) {
            return UNKNOWN;
        }

        String lowerUa = userAgentString.toLowerCase();

        if (lowerUa.contains("wifi") || lowerUa.contains("wlan")) {
            return "WiFi";
        }
        if (lowerUa.contains("4g") || lowerUa.contains("lte")) {
            return "4G";
        }
        if (lowerUa.contains("5g") || lowerUa.contains("nr")) {
            return "5G";
        }
        if (lowerUa.contains("3g")) {
            return "3G";
        }
        if (lowerUa.contains("2g")) {
            return "2G";
        }
        if (lowerUa.contains("ethernet")) {
            return "以太网";
        }

        return UNKNOWN;
    }

    public static String getBrowser(String userAgentString) {
        return parse(userAgentString).getBrowser();
    }

    public static String getOs(String userAgentString) {
        return parse(userAgentString).getOs();
    }

    public static String getDevice(String userAgentString) {
        return parse(userAgentString).getDevice();
    }

    public static String getNetwork(String userAgentString) {
        return parse(userAgentString).getNetwork();
    }

    public static boolean isMobile(String userAgentString) {
        if (StrUtil.isBlank(userAgentString)) {
            return false;
        }

        String lowerUa = userAgentString.toLowerCase();

        return lowerUa.contains("mobile") || lowerUa.contains("android") || lowerUa.contains("iphone") || lowerUa.contains("ipad") || lowerUa.contains("phone");
    }

    public static boolean isWechat(String userAgentString) {
        if (StrUtil.isBlank(userAgentString)) {
            return false;
        }

        return userAgentString.toLowerCase().contains("micromessenger");
    }

    public static boolean isSpider(String userAgentString) {
        if (StrUtil.isBlank(userAgentString)) {
            return false;
        }

        String lowerUa = userAgentString.toLowerCase();

        return lowerUa.contains("spider") || lowerUa.contains("bot") || lowerUa.contains("crawler") || lowerUa.contains("slurp");
    }

    public static class UaInfo {
        private final String browser;
        private final String os;
        private final String device;
        private final String network;

        public UaInfo(String browser, String os, String device, String network) {
            this.browser = browser;
            this.os = os;
            this.device = device;
            this.network = network;
        }

        public String getBrowser() {
            return browser;
        }

        public String getOs() {
            return os;
        }

        public String getDevice() {
            return device;
        }

        public String getNetwork() {
            return network;
        }

        @Override
        public String toString() {
            return "UaInfo{" +
                    "browser='" + browser + '\'' +
                    ", os='" + os + '\'' +
                    ", device='" + device + '\'' +
                    ", network='" + network + '\'' +
                    '}';
        }
    }
}
