package com.pcy.redigrandson;

import com.pcy.redigrandson.properties.RedisConnectionInfo;
import com.pcy.redigrandson.redislock.RedisDistributedLock;
import redis.clients.jedis.JedisPool;

/**
 * @description:
 * @author: 彭椿悦
 * @data: 2021/4/22 17:07
 */
public class RedigrandsonClient {
    private JedisPool jedisPool;
    private Integer database;

    public RedigrandsonClient(JedisPool pool, Integer database) {
        this.jedisPool = pool;
        this.database = database;
    }

    public  DistributedLock getLock(String sourceName){
        return new RedisDistributedLock(sourceName,jedisPool,database);
    }
}
