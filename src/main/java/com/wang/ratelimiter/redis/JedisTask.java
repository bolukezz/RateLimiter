package com.wang.ratelimiter.redis;

import redis.clients.jedis.Jedis;

@FunctionalInterface
public interface JedisTask<T> {

  T run(Jedis jedis);

}
