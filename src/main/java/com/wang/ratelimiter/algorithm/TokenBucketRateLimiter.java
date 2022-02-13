package com.wang.ratelimiter.algorithm;

import com.google.common.base.Stopwatch;
import com.google.common.math.LongMath;
import com.wang.ratelimiter.base.AbstractRateLimiter;
import com.wang.ratelimiter.base.RateLimiter;


import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.*;

/**
 * @Author: zhudawang
 * @Date: 2022/02/12/9:17 下午
 * @Description: 令牌桶实现
 */
public class TokenBucketRateLimiter extends AbstractRateLimiter {

    //互斥锁
    private volatile Object mutex;

    //当前存储的令牌
    private double storedPermits;

    //最大存储令牌数
    private double maxPermits;

    //下次可以正常获取令牌的时间
    private long nextFreeTicketMicros;

    //产生一个令牌需要花费的毫秒数
    private double stableIntervalMicros;

    //最大存储多少秒的令牌
    private final double maxBurstSeconds;


    private final Stopwatch stopwatch;

    public TokenBucketRateLimiter(double permitsPerSecond) {
        checkArgument(permitsPerSecond > 0.0 && !Double.isNaN(permitsPerSecond), "rate must be positive");
        this.mutex = new Object();
        maxBurstSeconds = 1.0;
        this.stopwatch = Stopwatch.createStarted();
        synchronized (mutex) {
            doSetRate(permitsPerSecond, stopwatch.elapsed(MICROSECONDS));
        }
    }

    private void doSetRate(double permitsPerSecond, long nowMicros) {
        resync(nowMicros);
        stableIntervalMicros = SECONDS.toMicros(1L) / permitsPerSecond;
        doSetRate(permitsPerSecond, stableIntervalMicros);
    }

    private void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
        double oldMaxPermits = this.maxPermits;
        maxPermits = maxBurstSeconds * permitsPerSecond;
        if (oldMaxPermits == Double.POSITIVE_INFINITY) {
            // if we don't special-case this, we would get storedPermits == NaN, below
            storedPermits = maxPermits;
        } else {
            storedPermits =
                    (oldMaxPermits == 0.0)
                            ? 0.0 // initial state
                            : storedPermits * maxPermits / oldMaxPermits;
        }
    }

    public double acquire(int permits) {
        long microsToWait = reserve(permits);
        sleep(microsToWait);
        //这里返回给用户就不是微妙，而是秒单位
        return 1.0 * microsToWait / SECONDS.toMicros(1L);
    }

    private final void sleep(long microsToWait) {
        long remainingNanos = MICROSECONDS.toNanos(microsToWait);
        try {
            NANOSECONDS.sleep(remainingNanos);
        } catch (InterruptedException e) {
            //todo 重新再次恢复，然后继续sleep？
        }
    }

    private final long reserve(int permits) {
        checkPermits(permits);
        synchronized (mutex) {
            return reserveAndGetWaitLength(permits, stopwatch.elapsed(MICROSECONDS));
        }
    }

    private final long reserveAndGetWaitLength(int permits, long now) {
        //返回的是上次预消费后的时间戳，本次请求需要为上次买单
        long momentAvailable = reserveEarliestAvailable(permits, now);
        //现在是否还继续需要给上次请求买单
        return max(momentAvailable - now, 0);
    }

    private long reserveEarliestAvailable(int requiredPermits, long now) {
        resync(now);
        //这里返回的是上次请求预消费后的时间戳
        long returnValue = nextFreeTicketMicros;
        double storedPermitsToSpend = min(requiredPermits, storedPermits);
        //还需要的令牌数
        double freshPermits = requiredPermits - storedPermitsToSpend;
        long waitMicros = (long) (freshPermits * stableIntervalMicros);
        //因为现在的时间生产的令牌不够，所以需要预支一些令牌，那么下次请求来就需要等+waitMicros时间后才能继续生产令牌
        //这里发现可以支持预消费
        nextFreeTicketMicros = LongMath.saturatedAdd(nextFreeTicketMicros, waitMicros);
        storedPermits -= storedPermitsToSpend;
        return returnValue;
    }

    /**
     * 这里是延迟计算，每次获取的时候同步一下这段时间产生了多少令牌数量
     *
     * @param now
     */
    private void resync(long now) {
        //这里判断如果大于的话，说明有多余的时间可以生产令牌
        if (now > nextFreeTicketMicros) {
            //计算出来这些时间内能生产出来多少令牌
            double newPermits = (now - nextFreeTicketMicros) / coolDownIntervalMicros();
            //和最大限制取一个最小的
            storedPermits = min(maxPermits, storedPermits + newPermits);
            //记录一下当前的时间戳
            nextFreeTicketMicros = now;
        }
    }

    /**
     * 每个令牌产生需要多少时间
     *
     * @return 产生一个令牌需要的时间戳
     */
    private double coolDownIntervalMicros() {
        return stableIntervalMicros;
    }

    private void checkPermits(int permits) {
        checkArgument(permits > 0, "Requested permits (%s) must be positive", permits);
    }

    public static void main(String[] args) {
        // 定义一个 RateLimiter ，单位时间（默认为秒）的设置为 0.5【访问速率为 0.5 / 秒】
        RateLimiter rateLimiter = new TokenBucketRateLimiter(1);
        // RateLimiter rateLimiter = RateLimiter.create(1);
        //private static RateLimiter rateLimiter = new T
        for (; ; ) {
            // 在访问该方法之前首先要进行 RateLimiter 的获取，返回值为实际的获取等待开销时间
            double acquire = rateLimiter.acquire();
            System.out.println(currentThread() + ": elapsed seconds " + LocalDateTime.now());
        }
    }
}
