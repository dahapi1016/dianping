package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private static final String LOCK_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final String UNLOCK_SCRIPT_FILE_NAME = "unlock.lua";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    private final String lockName;
    private final StringRedisTemplate stringRedisTemplate;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource(UNLOCK_SCRIPT_FILE_NAME));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }


    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String lockName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockName = lockName;
    }

    @Override
    public boolean tryLock(long timeOutSec) {
        //防止不同JVM线程ID冲突，使用UUID + 线程ID。参数为ture是为了去掉UUID中的横线
        String threadId = ID_PREFIX + Thread.currentThread().threadId();

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + lockName, threadId, timeOutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(LOCK_PREFIX + lockName),
                ID_PREFIX + Thread.currentThread().threadId()
        );
    }
}
