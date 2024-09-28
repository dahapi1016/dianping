package com.hmdp;

import com.hmdp.utils.RedisIdUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    RedisIdUtil redisIdUtil;

    private static final ExecutorService es = Executors.newFixedThreadPool(300);

    @Test
    public void testGeneratingId() throws InterruptedException {
        CountDownLatch latch =  new CountDownLatch(300);
        Runnable task = () -> {
            try {
                for(int i = 0; i < 100; i++) {
                    System.out.println(redisIdUtil.nextId("test"));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        };
        System.out.println(LocalDateTime.now());
        for(int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        System.out.println(LocalDateTime.now());
    }
}
