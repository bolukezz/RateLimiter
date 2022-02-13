package com.wang.ratelimiter;

import com.wang.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.wang.ratelimiter.base.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static java.lang.Thread.currentThread;

@SpringBootTest
class RateLimiterApplicationTests {
    // 定义一个 RateLimiter ，单位时间（默认为秒）的设置为 0.5【访问速率为 0.5 / 秒】
    private static RateLimiter rateLimiter = new TokenBucketRateLimiter(0.5);

    //private static RateLimiter rateLimiter = new T
    @Test
    void contextLoads() {
        for (; ; ) {
            testRateLimiter();
        }
    }


    private static void testRateLimiter() {
        // 在访问该方法之前首先要进行 RateLimiter 的获取，返回值为实际的获取等待开销时间
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                while (true) {
                    double acquire = rateLimiter.acquire();
                    System.out.println(currentThread() + ": elapsed seconds " + acquire);
                }
            }).start();
        }


    }


}
