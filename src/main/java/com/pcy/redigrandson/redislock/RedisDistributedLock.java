package com.pcy.redigrandson.redislock;

import com.google.common.collect.Lists;
import com.pcy.redigrandson.DistributedLock;
import com.pcy.redigrandson.LockPostpone;
import com.pcy.redigrandson.Postpone;
import com.pcy.redigrandson.postpone.DefaultPostpone;
import com.pcy.redigrandson.postpone.PostponeTask;
import com.pcy.redigrandson.properties.RedisConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:
 * @author: 彭椿悦
 * @data: 2021/4/21 16:35
 */
@Component
public class RedisDistributedLock implements DistributedLock, LockPostpone {
    Logger logger = LoggerFactory.getLogger(RedisDistributedLock.class);
    /**
     * 设置值成功的返回结果
     */
    public static final String SET_SUCCESS = "OK";
    /**
     * 锁默认的过期时间(单位毫秒)
     */
    private static final Long DEFAULT_EXPIRED_TIME = 1000L;
    /**
     * 获取锁默认的超时时间
     */
    private static final int DEFAULT_TIMEOUT = 200;
    /**
     * 是否需要开启延时线程
     */
    private boolean needPostpone = true;
    /**
     * 释放锁成功的返回结果
     */
    public static final Long RELEASE_SUCCESS = 1L;
    /**
     * 延时成功的返回结果
     */
    private static final Long POSTPONE_SUCCESS = 1L;
    private static Map<String, Postpone> postponeMap = new ConcurrentHashMap<>();
    /**
     * redis连接信息
     */
    private RedisConnectionInfo redisConnectionInfo;


    @Resource
    public void setRedisConnectionInfo(RedisConnectionInfo redisConnectionInfo) {
        this.redisConnectionInfo = redisConnectionInfo;
    }

    /**
     * 锁前缀
     */
    static public String LOCK_PREFIX = "DCS_LOCK_";
    /**
     * 释放锁脚本
     */
    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del',KEYS[1]) " +
                    "else return 0 " +
                    "end";
    /**
     * 锁延时脚本
     */
    private static final String POSTPONE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('pexpire', KEYS[1], ARGV[2]) " +
                    "else return 0 " +
                    "end";
    private JedisPoolConfig jedisPoolConfig;
    private JedisPool pool;

    @PostConstruct
    public void init() {
        jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(8);
        jedisPoolConfig.setMaxTotal(18);
        pool = new JedisPool(jedisPoolConfig, redisConnectionInfo.getHost(), redisConnectionInfo.getPort(), 2000,
                redisConnectionInfo.getPassword());
    }

    @PreDestroy
    public void destroy() {
        pool.close();
        pool.destroy();
    }


    @Override
    public Boolean tryLock(String lockName, String uniqueStr) {
        return this.tryLock(lockName, uniqueStr, DEFAULT_EXPIRED_TIME);
    }

    @Override
    public Boolean lock(String lockName, String uniqueStr) {
        return lock(lockName, uniqueStr, DEFAULT_EXPIRED_TIME);
    }

    @Override
    public Boolean tryLock(String lockName, String uniqueStr, int timeout) {
        return tryLock(lockName, uniqueStr, DEFAULT_EXPIRED_TIME, timeout);
    }

    @Override
    public Boolean tryLock(String lockName, String uniqueStr, Long expireTime) {
        Boolean locked = false;
        while (true) {
            locked = setValueIfAbsent(LOCK_PREFIX + lockName, uniqueStr, expireTime);
            if (locked) {
                if (logger.isDebugEnabled()) {
                    logger.debug(uniqueStr + "获取到了锁");
                }

                break;
            }
        }

        //如果获取锁成功则启动一个延时线程
        if (locked && needPostpone()) {
            startPostponeThread(lockName, uniqueStr, expireTime);
        }
        if (!locked) {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "未获取到锁");
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean lock(String lockName, String uniqueStr, Long expireTime) {
        Boolean locked = setValueIfAbsent(LOCK_PREFIX + lockName, uniqueStr, expireTime);
        //如果获取锁成功则启动一个延时线程
        if (locked && needPostpone()) {
            startPostponeThread(lockName, uniqueStr, expireTime);
        }
        if (!locked) {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "未获取到锁");
            }
        }else {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "获取到了锁");
            }
        }
        return locked;
    }

    public Boolean setValueIfAbsent(String key, String value, Long expireTime) {
        Jedis jedis = pool.getResource();
        if (redisConnectionInfo.getDatabase() != null) {
            jedis.select(0);
        }
        String result;
        try {
            SetParams params = new SetParams();
            params.nx();
            if (expireTime != null) {
                params.px(expireTime);
            }
            result = jedis.set(key, value, params);
        } finally {
            jedis.close();
        }

        return SET_SUCCESS.equals(result);
    }

    @Override
    public Boolean tryLock(String lockName, String uniqueStr, Long expireTime, int timeout) {
        Boolean locked = false;
        long begin = System.currentTimeMillis();
        while ((System.currentTimeMillis() - begin) <= timeout) {
            locked = setValueIfAbsent(LOCK_PREFIX + lockName, uniqueStr, expireTime);
            if (locked) {
                if (logger.isDebugEnabled()) {
                    logger.debug(uniqueStr + "获取到了锁");
                }

                break;
            }
        }

        //如果获取锁成功则启动一个延时线程
        if (locked && needPostpone()) {
            startPostponeThread(lockName, uniqueStr, expireTime);
        }
        if (!locked) {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "未获取到锁");
            }
        }
        return locked;
    }

    private void startPostponeThread(String lockName, String uniqueStr, Long expireTime) {
        //如果获取到锁了，启动一个延时线程，防止业务逻辑未执行完毕就因锁超时而使锁释放
        Postpone postpone = new DefaultPostpone();
        postponeMap.put(uniqueStr, postpone);
        Thread postponeThread = new Thread(new PostponeTask(LOCK_PREFIX + lockName, uniqueStr, expireTime,
                this, postpone));
        //将该线程设置为守护线程
        postponeThread.setDaemon(Boolean.TRUE);
        postponeThread.start();
        if (logger.isDebugEnabled()) {
            logger.debug("为" + uniqueStr + "开启了延时线程");
        }
    }

    @Override
    public Boolean unlock(String lockName, String uniqueStr) {
        //通知守护线程关闭
        Postpone postpone = postponeMap.get(uniqueStr);
        postpone.stopPostPone();
        postponeMap.remove(uniqueStr);

        Jedis jedis = pool.getResource();
        if (redisConnectionInfo.getDatabase() != null) {
            jedis.select(0);
        }
        Object result;
        try {
            result = jedis.eval(RELEASE_LOCK_SCRIPT, Collections.singletonList(LOCK_PREFIX + lockName),
                    Collections.singletonList(uniqueStr));
        } finally {
            jedis.close();
        }

        if (RELEASE_SUCCESS.equals(result)) {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "释放了锁");
            }

            return Boolean.TRUE;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "释放锁失败");
            }

            return Boolean.FALSE;
        }
    }

    @Override
    public Boolean postpone(String lockName, String uniqueStr, long expireTime) {
        Jedis jedis = pool.getResource();
        if (redisConnectionInfo.getDatabase() != null) {
            jedis.select(0);
        }
        try {
            Object result = jedis.eval(POSTPONE_LOCK_SCRIPT, Lists.newArrayList(lockName), Lists.newArrayList(uniqueStr,
                    String.valueOf(expireTime)));
            if (POSTPONE_SUCCESS.equals(result)) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } finally {
            jedis.close();
        }

    }

    @Override
    public boolean needPostpone() {
        return needPostpone;
    }


}
