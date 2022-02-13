package com.wang.ratelimiter.base;

/**
 * @Author: zhudawang
 * @Date: 2022/02/12/9:13 下午
 * @Description:
 */
public abstract class AbstractRateLimiter implements RateLimiter {

    public double acquire() {
        return acquire(1);
    }
}
