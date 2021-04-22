package com.pcy.redigrandson.redislock;

import com.google.common.collect.Lists;
import com.pcy.redigrandson.DistributedLock;
import com.pcy.redigrandson.LockPostpone;
import com.pcy.redigrandson.Postpone;
import com.pcy.redigrandson.postpone.DefaultPostpone;
import com.pcy.redigrandson.postpone.PostponeTask;
import com.pcy.redigrandson.properties.RedisConnectionInfo;
import com.pcy.redigrandson.util.JVMUtil;
import com.pcy.redigrandson.util.MACUtil;
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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:
 * @author: 彭椿悦
 * @data: 2021/4/21 16:35
 */
public class RedisDistributedLock implements DistributedLock, LockPostpone {
    private Integer database = null;
    Logger logger = LoggerFactory.getLogger(RedisDistributedLock.class);
    /**
     * 设置值成功的返回结果
     */
    public static final String SET_SUCCESS = "OK";
    /**
     * 锁默认的过期时间(单位毫秒)
     */
    private static final Long DEFAULT_EXPIRED_TIME = 30000L;
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

    private String sourceName;
    private String uniqueStr;

    public RedisDistributedLock(String sourceName) {
        this.sourceName = sourceName;
        try {
            this.uniqueStr = MACUtil.getLocalMac() + "-" + JVMUtil.jvmPid() + "-" + Thread.currentThread();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
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

    /**
     * 可重入加锁脚本，KEYS[1]：锁名字，ARGS[1]过期时间，ARGS[2]：加锁时的唯一标识
     */
    public static final String REENTRANT_LOCK_SCRIPT =
            "if (redis.call('exists', KEYS[1]) == 0) then \n" +
                    "    redis.call('hset', KEYS[1], ARGV[2], 1); \n" +
                    "    redis.call('pexpire', KEYS[1], ARGV[1]);\n" +
                    "    return nil;\n" +
                    "end;\n" +
                    "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then\n" +
                    "    redis.call('hincrby', KEYS[1], ARGV[2], 1);\n" +
                    "    redis.call('pexpire', KEYS[1], ARGV[1]);\n" +
                    "    return nil;\n" +
                    "end;\n" +
                    "return redis.call('pttl', KEYS[1]);\n";
    /**
     * 可重入解锁脚本，KEYS[1]是锁名字
     * KEYS[2]是 redisson_lock__channel:{锁名字} 这么一个东西，他其实也是个key，可以理解为主题， 发布订阅用的
     * ARGV[1]是解锁的标识符
     * ARGV[2] 过期时间
     * ARGV[3] 加锁时的唯一标识
     */
    public static final String REENTRANT_UNLOCK_SCRIPT =
            "if (redis.call('exists', KEYS[1]) == 0) then\n" +
                    "    redis.call('publish', KEYS[2], ARGV[1]);\n" +
                    "    return 1;\n" +
                    "end;\n" +
                    "if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then\n" +
                    " return nil;\n" +
                    "end;\n" +
                    "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1);\n" +
                    "if (counter > 0) then\n" +
                    " redis.call('pexpire', KEYS[1], ARGV[2]);\n" +
                    "    return 0;\n" +
                    "else \n" +
                    " redis.call('del', KEYS[1]);\n" +
                    "    redis.call('publish', KEYS[2], ARGV[1]);\n" +
                    "    return 1;\n" +
                    "end; \n" +
                    "return nil;\n";
    private JedisPool pool;

    public RedisDistributedLock(String sourceName, JedisPool jedisPool) {
        this(sourceName);
        this.pool = jedisPool;
    }

    public RedisDistributedLock(String sourceName, JedisPool jedisPool, Integer database) {
        this(sourceName, jedisPool);
        this.database = database;
    }

    @Override
    public Boolean tryLock() {
        return this.tryLock(DEFAULT_EXPIRED_TIME);
    }

    @Override
    public Boolean lock() {
        return lock(DEFAULT_EXPIRED_TIME);
    }

    @Override
    public Boolean tryLock(int timeout) {
        return tryLock(DEFAULT_EXPIRED_TIME, timeout);
    }

    @Override
    public Boolean tryLock(Long expireTime) {
        Boolean locked = false;
        while (true) {
            locked = setValueIfAbsent(LOCK_PREFIX + sourceName, uniqueStr, expireTime);
            if (locked) {
                if (logger.isDebugEnabled()) {
                    logger.debug(uniqueStr + "获取到了锁");
                }

                break;
            }
        }

        //如果获取锁成功则启动一个延时线程
        if (locked && needPostpone()) {
            startPostponeThread(expireTime);
        }
        if (!locked) {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "未获取到锁");
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean lock(Long expireTime) {
        Boolean locked = setValueIfAbsent(LOCK_PREFIX + sourceName, uniqueStr, expireTime);
        //如果获取锁成功则启动一个延时线程
        if (locked && needPostpone()) {
            startPostponeThread(expireTime);
        }
        if (!locked) {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "未获取到锁");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "获取到了锁");
            }
        }
        return locked;
    }

    public Boolean setValueIfAbsent(String key, String value, Long expireTime) {
        Jedis jedis = pool.getResource();
        if (database != null) {
            jedis.select(0);
        }
        Object result;
        try {
            result = jedis.eval(REENTRANT_LOCK_SCRIPT, Lists.newArrayList(key), Lists.newArrayList(String.valueOf(expireTime),
                    value));
//            SetParams params = new SetParams();
//            params.nx();
//            if (expireTime != null) {
//                params.px(expireTime);
//            }
//            result = jedis.set(key, value, params);
        } finally {
            jedis.close();
        }

        return result == null;
    }

    @Override
    public Boolean tryLock(Long expireTime, int timeout) {
        Boolean locked = false;
        long begin = System.currentTimeMillis();
        while ((System.currentTimeMillis() - begin) <= timeout) {
            locked = setValueIfAbsent(LOCK_PREFIX + sourceName, uniqueStr, expireTime);
            if (locked) {
                if (logger.isDebugEnabled()) {
                    logger.debug(uniqueStr + "获取到了锁");
                }

                break;
            }
        }

        //如果获取锁成功则启动一个延时线程
        if (locked && needPostpone()) {
            startPostponeThread(expireTime);
        }
        if (!locked) {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "未获取到锁");
            }
        }
        return locked;
    }

    private void startPostponeThread(Long expireTime) {
        if (postponeMap.get(uniqueStr) != null) {
            return;
        }
        //如果获取到锁了，启动一个延时线程，防止业务逻辑未执行完毕就因锁超时而使锁释放
        Postpone postpone = new DefaultPostpone();
        postponeMap.put(uniqueStr, postpone);
        Thread postponeThread = new Thread(new PostponeTask(uniqueStr, expireTime,
                this, postpone));
        //将该线程设置为守护线程
        postponeThread.setDaemon(Boolean.TRUE);
        postponeThread.start();
        if (logger.isDebugEnabled()) {
            logger.debug("为" + uniqueStr + "开启了延时线程");
        }
    }

    @Override
    public Boolean unlock() {
//        //通知守护线程关闭
//        Postpone postpone = postponeMap.get(uniqueStr);
//        postpone.stopPostPone();
//        postponeMap.remove(uniqueStr);

        Jedis jedis = pool.getResource();
        if (database != null) {
            jedis.select(0);
        }
        Object result;
        try {
//            result = jedis.eval(RELEASE_LOCK_SCRIPT, Collections.singletonList(LOCK_PREFIX + sourceName),
//                    Collections.singletonList(uniqueStr));
            result = jedis.eval(REENTRANT_UNLOCK_SCRIPT, Lists.newArrayList(LOCK_PREFIX + sourceName,
                    "redisson_lock__channel:{" + LOCK_PREFIX + sourceName + "}"), Lists.newArrayList(uniqueStr,
                    String.valueOf(DEFAULT_EXPIRED_TIME), uniqueStr));
        } finally {
            jedis.close();
        }
        if (RELEASE_SUCCESS.equals(result)) {
            //通知守护线程关闭
            Postpone postpone = postponeMap.get(uniqueStr);
            postpone.stopPostPone();
            postponeMap.remove(uniqueStr);
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "释放了锁");
            }

            return Boolean.TRUE;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(uniqueStr + "未释放锁");
            }

            return Boolean.FALSE;
        }
    }

    @Override
    public Boolean postpone(long expireTime) {
        Jedis jedis = pool.getResource();
        if (database != null) {
            jedis.select(0);
        }
        try {
            Object result = jedis.eval(POSTPONE_LOCK_SCRIPT, Lists.newArrayList(LOCK_PREFIX + sourceName),
                    Lists.newArrayList(uniqueStr,
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
