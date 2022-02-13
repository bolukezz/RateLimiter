package com.wang.ratelimiter.algorithm;

import com.wang.ratelimiter.base.AbstractRateLimiter;
import com.wang.ratelimiter.base.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @Author: zhudawang
 * @Date: 2022/02/13/1:25 下午
 * @Description: 漏桶实现
 */
@Slf4j
public class LeakyBucketLimiter {
    //桶的大小
    private int capacity;

    //定时流水
    public ScheduledThreadPoolExecutor constantFlow;

    //桶
    private BlockingQueue<Thread> bucket;


    public LeakyBucketLimiter(int capacity, double rate) {
        this.capacity = capacity;
        bucket = new LinkedBlockingQueue<>(capacity);
        double period = SECONDS.toMicros(1L) / rate;
        constantFlow = new ScheduledThreadPoolExecutor(1);
        constantFlow.scheduleAtFixedRate(() -> {
            try {
                LockSupport.unpark(bucket.take());
            } catch (InterruptedException e) {
                log.error("failed to take bucket，be interrupted");
            }
        }, 0, (long) period, TimeUnit.MICROSECONDS);
    }


    public void acquire() {
        try {
            bucket.put(Thread.currentThread());
            LockSupport.park();
        } catch (InterruptedException e) {
            log.error("failed to save bucket，be interrupted");
        }
    }

    public static void main(String[] args) {
        // 定义一个 RateLimiter ，单位时间（默认为秒）的设置为 0.5【访问速率为 0.5 / 秒】
        LeakyBucketLimiter leakyBucketLimiter = new LeakyBucketLimiter(10, 1);
        // RateLimiter rateLimiter = RateLimiter.create(1);
        //private static RateLimiter rateLimiter = new T
        for (; ; ) {
            // 在访问该方法之前首先要进行 RateLimiter 的获取，返回值为实际的获取等待开销时间
            leakyBucketLimiter.acquire();
            System.out.println(currentThread() + ": elapsed seconds " + LocalDateTime.now());
        }
    }


}
