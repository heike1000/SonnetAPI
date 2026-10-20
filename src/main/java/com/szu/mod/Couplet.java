package com.szu.mod;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Couplet {
    public enum CoupletType {
        String, Integer, Double, List
    }
    static class Value {
        final CoupletType type;
        final Object data;
        final Long expireAt;

        Value(CoupletType type, Object data, Long expireAt) {
            this.type = type;
            this.data = data;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, Value> coupletData = new ConcurrentHashMap<>();
    private final Long maxSize;

    public Couplet(Long maxSize, Long autoDeletionTime) {
        this.maxSize = maxSize;
        if (autoDeletionTime != null) {
            ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "Couplet-cleaner");
                thread.setDaemon(true);
                return thread;
            });
            cleaner.scheduleWithFixedDelay(this::purgeExpired,
                    autoDeletionTime,
                    autoDeletionTime,
                    TimeUnit.SECONDS);
        }
    }

    public void purgeExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Value> e : coupletData.entrySet()) {
            Value v = e.getValue();
            if (v.expireAt != null && v.expireAt <= now) {
                coupletData.remove(e.getKey(), v);
            }
        }
    }

    // 四种类型的set
    private void put(String key, CoupletType type, Object data, Long expireTime) {
        Long expireAt = expireTime == null
                ? null
                : System.currentTimeMillis() + expireTime * 1000L;
        coupletData.put(key, new Value(type, data, expireAt));
    }
    public void set(String key, String data, Long expireTime) {
        put(key, CoupletType.String, data, expireTime);
    }
    public void set(String key, Integer data, Long expireTime) {
        put(key, CoupletType.Integer, data, expireTime);
    }
    public void set(String key, Double data, Long expireTime) {
        put(key, CoupletType.Double, data, expireTime);
    }
    public void set(String key, List data, Long expireTime) {
        put(key, CoupletType.List, data, expireTime);
    }

    public Object get(String key) {
        Value value = coupletData.get(key);
        if (value == null) {
            return null;
        }
        if (value.expireAt != null && value.expireAt <= System.currentTimeMillis()) {
            coupletData.remove(key, value);
            return null;
        }
        return value.data;
    }

    public boolean remove(String key) {
        Value value = coupletData.get(key);
        if (value == null) {
            return false;
        } else {
            coupletData.remove(key, value);
            return true;
        }
    }
}
