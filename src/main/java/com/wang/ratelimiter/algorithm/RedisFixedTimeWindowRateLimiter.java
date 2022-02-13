package com.wang.ratelimiter.algorithm;

import com.wang.ratelimiter.redis.JedisTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisNoScriptException;

import static com.wang.ratelimiter.algorithm.SHA1.sha1Hex;

/**
 * @Author: zhudawang
 * @Date: 2022/02/13/3:23 下午
 * @Description:
 */
@Slf4j
public class RedisFixedTimeWindowRateLimiter {
    private final String key;

    private final int limit;

    private JedisTaskExecutor jedisTaskExecutor;

    /* TPS:limit/1s KEYS[1]=key,ARGV[1]=limit,return=result */
    public static final String REDIS_LIMIT_SCRIPT =
            "local key = KEYS[1] " +
                    "local limit = tonumber(ARGV[1]) " +
                    "local current = tonumber(redis.call('incr', key)) " +
                    "if current > limit then " +
                    "   return 0 " +
                    "elseif current == 1 then " +
                    "   redis.call('expire', key, '1') " +
                    "end " +
                    "return 1 ";

    /* Redis cache for Lua script. */
    public static final String REDIS_LIMIT_SCRIPT_SHA1 = sha1Hex(REDIS_LIMIT_SCRIPT);

    public RedisFixedTimeWindowRateLimiter(String key, int limit, JedisTaskExecutor jedisTaskExecutor) {
        this.key = key;
        this.limit = limit;
        this.jedisTaskExecutor = jedisTaskExecutor;
    }


    public boolean tryAcquire() {
        long result = 0;
        try {
            result = (long) jedisTaskExecutor.evalsha(REDIS_LIMIT_SCRIPT_SHA1, key,
                    String.valueOf(limit));
            return 1 == result;
        } catch (JedisNoScriptException e) {
            log.error("no lua script cache on redis server.");
        } catch (JedisConnectionException e) {
            log.error("Read redis error.");
        } catch (JedisException e) {
            log.error("Read redis error.");

        }

        try {
            result = (long) jedisTaskExecutor.eval(REDIS_LIMIT_SCRIPT, key, String.valueOf(limit));
        } catch (JedisConnectionException ee) {
            log.error("Read redis error.");
        }

        return 1 == result;
    }
}


class SHA1 {
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String CHARSET_GBK = "GBK";

    public static String sha1Hex(String source) {
        return sha1Hex(source, CHARSET_UTF8);
    }

    public static String sha1Hex(String source, String charset) {
        if (!CHARSET_GBK.equals(charset)) {
            charset = CHARSET_UTF8;
        }
        byte[] bSource;
        try {
            bSource = source.getBytes(charset);
        } catch (Exception ex) {
            bSource = source.getBytes();
        }
        return DigestUtils.sha1Hex(bSource);
    }
}
