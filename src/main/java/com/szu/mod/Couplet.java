package com.szu.mod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final Map<String, Value> coupletData;
    private final long maxSize;

    public Couplet(long maxSize, Long autoDeletionTime) {
        this.maxSize = maxSize;
        // 按访问顺序排序，最近访问的移到尾部。相当于LRU
        this.coupletData = new LinkedHashMap<String, Value>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Value> eldest) {
                return size() > Couplet.this.maxSize;
            }
        };

        if (autoDeletionTime != null) {
            ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "Couplet-cleaner");
                thread.setDaemon(true);
                return thread;
            });
            cleaner.scheduleWithFixedDelay(this::purgeExpired, autoDeletionTime, autoDeletionTime, TimeUnit.SECONDS);
        }
    }

    public void purgeExpired() {
        long now = System.currentTimeMillis();
        synchronized (coupletData) {
            var iterator = coupletData.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Value> entry = iterator.next();
                Value value = entry.getValue();
                if (value.expireAt != null && value.expireAt <= now) {
                    iterator.remove();
                }
            }
        }
    }

    private void _put(String key, CoupletType type, Object data, Long expireTime) {
        Long expireAt = expireTime == null ? null : System.currentTimeMillis() + expireTime * 1000L;
        synchronized (coupletData) {
            coupletData.put(key, new Value(type, data, expireAt));
        }
    }

    public void put(String key, String data, Long expireTime) {
        _put(key, CoupletType.String, data, expireTime);
    }

    public void put(String key, Integer data, Long expireTime) {
        _put(key, CoupletType.Integer, data, expireTime);
    }

    public void put(String key, Double data, Long expireTime) {
        _put(key, CoupletType.Double, data, expireTime);
    }

    public void put(String key, List data, Long expireTime) {
        _put(key, CoupletType.List, data, expireTime);
    }

    public Object get(String key) {
        synchronized (coupletData) {
            Value value = coupletData.get(key);
            if (value == null) {
                return null;
            }
            if (value.expireAt != null && value.expireAt <= System.currentTimeMillis()) {
                coupletData.remove(key);
                return null;
            }
            return value.data;
        }
    }

    public boolean remove(String key) {
        synchronized (coupletData) {
            return coupletData.remove(key) != null;
        }
    }
}