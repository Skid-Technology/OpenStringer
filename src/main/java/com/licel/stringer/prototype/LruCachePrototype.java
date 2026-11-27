package com.licel.stringer.prototype;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LruCachePrototype<K, V> {
    private final LinkedHashMap<K, V> map;
    private int size;
    private int maxSize;
    private int putCount;
    private int createCount;
    private int evictionCount;
    private int hitCount;
    private int missCount;
    private static LruCachePrototype<Object, Object> lruCache = new LruCachePrototype<>(10);

    public static Object getCachedValue(Object key) {
        if (lruCache.get(key) != null) {
            System.out.println(555);
            return lruCache.get(key);
        } else {
            char[] chars = new char[0];
            String value = new String(chars);
            lruCache.put(key, value);
            return value;
        }
    }

    public LruCachePrototype(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(0, 0.75F, true);
    }

    public final synchronized V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V mapValue = this.map.get(key);
        if (mapValue != null) {
            this.hitCount++;
            return mapValue;
        }

        this.missCount++;
        mapValue = this.create(key);
        if (mapValue != null) {
            this.createCount++;
            this.size += this.safeSizeOf(key, mapValue);
            this.map.put(key, mapValue);
            this.trimToSize(this.maxSize);
        }

        return mapValue;
    }

    public final synchronized void put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        this.putCount++;
        this.size += this.safeSizeOf(key, value);
        V previous = this.map.put(key, value);
        if (previous != null) {
            this.size -= this.safeSizeOf(key, previous);
        }

        this.trimToSize(this.maxSize);
    }

    private void trimToSize(int maxSize) {
        while (this.size > maxSize && !this.map.isEmpty()) {
            Entry<K, V> toEvict = this.map.entrySet().iterator().next();
            if (toEvict == null) {
                break;
            }

            K key = toEvict.getKey();
            V value = toEvict.getValue();
            this.map.remove(key);
            this.size -= this.safeSizeOf(key, value);
            this.evictionCount++;
            this.entryEvicted(key, value);
        }

        if (this.size < 0 || this.map.isEmpty() && this.size != 0) {
            throw new IllegalStateException(this.getClass().getName() + ".sizeOf() is reporting inconsistent results!");
        }
    }

    public final synchronized V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V previous = this.map.remove(key);
        if (previous != null) {
            this.size -= this.safeSizeOf(key, previous);
        }

        return previous;
    }

    protected void entryEvicted(K key, V value) {
    }

    protected V create(K key) {
        return null;
    }

    private int safeSizeOf(K key, V value) {
        int result = this.sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        } else {
            return result;
        }
    }

    protected int sizeOf(K key, V value) {
        return 1;
    }

    public final synchronized void evictAll() {
        this.trimToSize(-1);
    }

    public final synchronized int size() {
        return this.size;
    }

    public final synchronized int maxSize() {
        return this.maxSize;
    }

    public final synchronized int hitCount() {
        return this.hitCount;
    }

    public final synchronized int missCount() {
        return this.missCount;
    }

    public final synchronized int createCount() {
        return this.createCount;
    }

    public final synchronized int putCount() {
        return this.putCount;
    }

    public final synchronized int evictionCount() {
        return this.evictionCount;
    }

    public final synchronized Map<K, V> snapshot() {
        return new LinkedHashMap<>(this.map);
    }
}
