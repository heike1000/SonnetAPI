package com.szu.mod;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class Couplet {
    public enum CoupletType {
        String, Integer, Double, List
    }

    static class Value {
        final CoupletType type;
        final Object data;
        final long expireAt;
        volatile long lastAccess;

        Value(CoupletType type, Object data, long expireAt) {
            this.type = type;
            this.data = data;
            this.expireAt = expireAt;
            this.lastAccess = System.nanoTime();
        }
    }

    // 过期时间最小堆元素
    static class Expiry implements Comparable<Expiry> {
        final String key;
        final long expireAt;
        Expiry(String key, long expireAt) {
            this.key = key;
            this.expireAt = expireAt;
        }

        // 让元素可以被排序
        @Override
        public int compareTo(Expiry expiry) {
            return Long.compare(this.expireAt, expiry.expireAt);
        }
    }

    private final ConcurrentHashMap<String, Value> coupletData;
    private final AtomicLong size = new AtomicLong(0);
    private final long maxSize;
    private final PriorityQueue<Expiry> expiryQueue = new PriorityQueue<>();// 过期时间最小堆，保证堆顶是离过期最近的元素
    private final Object expiryLock = new Object();
    private static final int SAMPLE_SIZE = 5;

    public Couplet(long maxSize, Long autoDeletionTime) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0");
        }
        this.maxSize = maxSize;
        this.coupletData = new ConcurrentHashMap<>();

        // 开启自动清理线程
        if (autoDeletionTime != null) {
            ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "Couplet-cleaner");
                thread.setDaemon(true);
                return thread;
            });
            cleaner.scheduleWithFixedDelay(
                    this::purgeExpired,
                    autoDeletionTime, autoDeletionTime, TimeUnit.SECONDS);
        }
    }

    // 通过过期时间最小堆间接获取过期元素，避免全局锁
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        while (true) {
            // 提取堆顶元素，过期则清理
            Expiry head;
            synchronized (expiryLock) {
                head = expiryQueue.peek();
                if (head == null || head.expireAt > now) {
                    return;
                }
                expiryQueue.poll();
            }

            Value value = coupletData.get(head.key);
            if (value != null && value.expireAt == head.expireAt) {
                if (coupletData.remove(head.key, value)) {
                    size.decrementAndGet();
                }
            }
        }
    }

    private void _put(String key, CoupletType type, Object data, Long expireTime) {
        long expireAt = (expireTime == null) ? Long.MAX_VALUE : System.currentTimeMillis() + expireTime * 1000L;
        Value old = coupletData.put(key, new Value(type, data, expireAt));
        if (old == null) {
            size.incrementAndGet();
        }
        if (expireAt != Long.MAX_VALUE) {
            synchronized (expiryLock) {
                expiryQueue.offer(new Expiry(key, expireAt));
            }
        }
        evictIfNeeded();
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

    // 近似LRU，避免全局锁
    private void evictIfNeeded() {
        if (size.get() <= maxSize) {
            return;
        }
        while (size.get() > maxSize) {
            String victim = null;
            long oldest = Long.MAX_VALUE;
            int samples = 0;
            // 保证近似均匀随机地抽取样本
            long currentSize = size.get();
            if (currentSize <= 0) {
                return;
            }
            double sampleRate = Math.min(1.0, (double) SAMPLE_SIZE / currentSize);
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (Map.Entry<String, Value> entry : coupletData.entrySet()) {
                if (samples >= SAMPLE_SIZE) {
                    break;
                }
                if (random.nextDouble() < sampleRate) {
                    samples++;
                    long time = entry.getValue().lastAccess;
                    if (time < oldest) {
                        oldest = time;
                        victim = entry.getKey();
                    }
                }
            }

            if (victim == null) {
                return;
            }
            Value removed = coupletData.remove(victim);
            if (removed != null) {
                size.decrementAndGet();
            }
        }
    }

    public Object get(String key) {
        Value value = coupletData.get(key);
        if (value == null) {
            return null;
        }
        if (value.expireAt <= System.currentTimeMillis()) {
            coupletData.remove(key, value);
            size.decrementAndGet();
            return null;
        }
        value.lastAccess = System.nanoTime();

        return value.data;
    }

    public boolean remove(String key) {
        Value removed = coupletData.remove(key);
        if (removed != null) {
            size.decrementAndGet();
            return true;
        }

        return false;
    }

    public int size() {
        return (int) Math.min(size.get(), Integer.MAX_VALUE);
    }
}