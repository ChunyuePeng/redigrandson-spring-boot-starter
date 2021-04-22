package com.pcy.redigrandson;


/**
 * 分布式锁操作的顶级抽象，包含了分布式锁的基本操作
 *
 * @author Chunyue Peng
 * @mail 1399068529@qq.com
 */
public interface DistributedLock {
    /**
     * 阻塞式获取锁，锁的过期时间使用默认值
     *
     * @param lockName  资源名称（锁名称）
     * @param uniqueStr 创建锁时传入的唯一标识，释放锁时会根据此值来确定是否释放锁。
     * @return 如果获取到了该资源返回Boolean.TRUE
     */
    Boolean tryLock(String lockName, String uniqueStr);

    /**
     * 非阻塞式获取锁，只会去获取一次，锁的过期时间使用默认值
     * @param lockName
     * @param uniqueStr
     * @return
     */
    Boolean lock(String lockName, String uniqueStr);

    /**
     * 阻塞式获取锁在超时时间内，锁的过期时间使用默认值
     * @param lockName
     * @param uniqueStr
     * @param timeout
     * @return
     */
    Boolean tryLock(String lockName, String uniqueStr, int timeout);
    /**
     * 阻塞式获取锁，并设置锁的过期时间
     * @param lockName
     * @param uniqueStr
     * @param expireTime 在该时间之后会释放掉该锁，时间单位毫秒
     * @return
     */
    Boolean tryLock(String lockName, String uniqueStr, Long expireTime);

    /**
     * 非阻塞式获取锁，只会尝试获取一次并设置锁的过期时间
     * @param lockName
     * @param uniqueStr
     * @param expireTime
     * @return
     */
    Boolean lock(String lockName, String uniqueStr, Long expireTime);

    /**
     * 在超时时间内阻塞式获取锁并设置锁的过期时间
     * @param lockName
     * @param uniqueStr
     * @param expireTime
     * @param timeout
     * @return
     */
    Boolean tryLock(String lockName, String uniqueStr, Long expireTime, int timeout);

    /**
     * 释放锁
     *
     * @param lockName  资源名称（锁名称）
     * @param uniqueStr 创建锁时传入的唯一标识，解锁时会根据此值判断是否此线程是否因为超时释放掉锁或因为
     *                  其它原因失去了对该资源的占有权
     * @return
     */
    Boolean unlock(String lockName, String uniqueStr);

    /**
     * 对锁进行延时
     *
     * @param lockName
     * @param uniqueStr
     * @param expireTime
     * @return
     */
    Boolean postpone(String lockName, String uniqueStr, long expireTime);
}
