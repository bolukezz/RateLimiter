package com.wang.ratelimiter.algorithm;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.currentThread;

/**
 * @Author: zhudawang
 * @Date: 2022/02/13/2:39 下午
 * @Description: 固定时间窗口限流器
 */
@Slf4j
public class FixedTimeWindowRateLimiter {

    private AtomicInteger count = new AtomicInteger(0);

    /* the max permitted access count per second*/
    private final int limit;

    private final Stopwatch stopwatch;

    private final Lock lock;

    public FixedTimeWindowRateLimiter(int limit) {
        this(limit, Stopwatch.createStarted());
    }

    public FixedTimeWindowRateLimiter(int limit, Stopwatch stopwatch) {
        this.limit = limit;
        this.stopwatch = stopwatch;
        this.lock = new ReentrantLock();
    }

    public boolean tryAcquire() {
        int addedCount = count.incrementAndGet();
        if (addedCount <= limit) {
            return true;
        }
        try {
            if (lock.tryLock(200,TimeUnit.MILLISECONDS)) {
                try {
                    if (stopwatch.elapsed(TimeUnit.MILLISECONDS) > TimeUnit.SECONDS.toMillis(1)) {
                        count.set(0);
                        stopwatch.reset().start();
                    }
                    addedCount = count.incrementAndGet();
                    return addedCount <= limit;
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.error("tryAcquire() is interrupted by lock-time-out.", e);
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        FixedTimeWindowRateLimiter fixedTimeWindowRateLimiter = new FixedTimeWindowRateLimiter(1);
        // RateLimiter rateLimiter = RateLimiter.create(1);
        //private static RateLimiter rateLimiter = new T
        for (; ; ) {
            // 在访问该方法之前首先要进行 RateLimiter 的获取，返回值为实际的获取等待开销时间
            System.out.println(currentThread() + ": elapsed seconds " + LocalDateTime.now() + " tryAcquire ->" + fixedTimeWindowRateLimiter.tryAcquire());
            Thread.sleep(200);
        }
//        Stopwatch stopwatch = Stopwatch.createStarted();
//        for (; ; ) {
//            Thread.sleep(200);
//            System.out.println(stopwatch.elapsed(TimeUnit.MILLISECONDS));
//            if (stopwatch.elapsed(TimeUnit.MILLISECONDS) - TimeUnit.SECONDS.toMillis(1) > 0) {
//                stopwatch.reset();
//                stopwatch.start();
//                System.out.println("entry");
//            }
//        }
    }
}
