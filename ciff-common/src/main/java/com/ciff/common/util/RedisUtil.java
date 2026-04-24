package com.ciff.common.util;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedissonClient redissonClient;

    public <T> T get(String key) {
        return redissonClient.<T>getBucket(key).get();
    }

    public <T> void set(String key, T value) {
        redissonClient.<T>getBucket(key).set(value);
    }

    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        redissonClient.<T>getBucket(key).set(value, timeout, unit);
    }

    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    public long delete(Collection<String> keys) {
        return redissonClient.getKeys().delete(keys.toArray(new String[0]));
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(key).expire(timeout, unit);
    }

    // --- List operations ---

    public <T> void listPush(String key, T value) {
        redissonClient.<T>getList(key).add(value);
    }

    public <T> void listPushWithTtl(String key, T value, long timeout, TimeUnit unit) {
        RList<T> list = redissonClient.getList(key);
        list.add(value);
        list.expire(timeout, unit);
    }

    public <T> List<T> listRange(String key) {
        return redissonClient.<T>getList(key).readAll();
    }

    public <T> void listUpdateEntry(String key, int index, T value) {
        RList<T> list = redissonClient.getList(key);
        if (index >= 0 && index < list.size()) {
            list.set(index, value);
        }
    }

    public <T> int listIndexOf(String key, T value) {
        return redissonClient.<T>getList(key).indexOf(value);
    }

    public Iterable<String> getKeysByPattern(String pattern) {
        return redissonClient.getKeys().getKeysByPattern(pattern);
    }
}
