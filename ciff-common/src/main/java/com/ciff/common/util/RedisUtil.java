package com.ciff.common.util;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
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
}