package com.wang.ratelimiter.base;

/**
 * @Author: zhudawang
 * @Date: 2022/02/12/9:09 下午
 * @Description:
 */
public interface RateLimiter {

    double acquire();

    double acquire(int permits);
}
