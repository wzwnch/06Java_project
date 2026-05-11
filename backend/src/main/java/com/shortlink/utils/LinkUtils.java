package com.shortlink.utils;

import cn.hutool.core.util.StrUtil;

public class LinkUtils {

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE62_LENGTH = 62;

    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private static long workerId = 1L;
    private static long datacenterId = 1L;
    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    private LinkUtils() {
    }

    public static synchronized long nextId() {
        long timestamp = System.currentTimeMillis() - EPOCH;

        if (timestamp < 0) {
            throw new RuntimeException("Clock moved backwards!");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence);
    }

    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis() - EPOCH;
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - EPOCH;
        }
        return timestamp;
    }

    public static String generateShortCode() {
        long id = nextId();
        return encodeBase62(id);
    }

    public static String generateShortCode(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be positive");
        }
        return encodeBase62(id);
    }

    public static String encodeBase62(long number) {
        if (number <= 0) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        while (number > 0) {
            int remainder = (int) (number % BASE62_LENGTH);
            result.insert(0, BASE62_CHARS.charAt(remainder));
            number /= BASE62_LENGTH;
        }

        return result.toString();
    }

    public static long decodeBase62(String str) {
        if (StrUtil.isBlank(str)) {
            throw new IllegalArgumentException("Input string cannot be blank");
        }

        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int index = BASE62_CHARS.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * BASE62_LENGTH + index;
        }

        return result;
    }

    public static boolean isValidShortCode(String shortCode) {
        if (StrUtil.isBlank(shortCode)) {
            return false;
        }

        if (shortCode.length() > 16) {
            return false;
        }

        for (int i = 0; i < shortCode.length(); i++) {
            char c = shortCode.charAt(i);
            if (BASE62_CHARS.indexOf(c) == -1) {
                return false;
            }
        }

        return true;
    }

    public static void setWorkerId(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("Worker ID must be between 0 and " + MAX_WORKER_ID);
        }
        LinkUtils.workerId = workerId;
    }

    public static void setDatacenterId(long datacenterId) {
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("Datacenter ID must be between 0 and " + MAX_DATACENTER_ID);
        }
        LinkUtils.datacenterId = datacenterId;
    }
}
