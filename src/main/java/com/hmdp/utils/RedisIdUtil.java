package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class RedisIdUtil {

    private static final long BEGIN_TIMESTAMP = 1704067200L;
    private static final int COUNT_BITS = 32;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String idKeyPrefix) {

        long curTimeStamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long idTimeStamp = curTimeStamp - BEGIN_TIMESTAMP;

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = stringRedisTemplate.opsForValue().increment("inc:" + idKeyPrefix + ":" + date);
        count = Optional.ofNullable(count).orElse(0L);

        return idTimeStamp << COUNT_BITS | count;
    }
}
