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
     * @return 如果获取到了该资源返回Boolean.TRUE
     */
    Boolean tryLock();

    /**
     * 非阻塞式获取锁，只会去获取一次，锁的过期时间使用默认值
     *
     * @return
     */
    Boolean lock();

    /**
     * 阻塞式获取锁在超时时间内，锁的过期时间使用默认值
     *
     * @param timeout
     * @return
     */
    Boolean tryLock(int timeout);

    /**
     * 阻塞式获取锁，并设置锁的过期时间
     *
     * @param expireTime 在该时间之后会释放掉该锁，时间单位毫秒
     * @return
     */
    Boolean tryLock(Long expireTime);

    /**
     * 非阻塞式获取锁，只会尝试获取一次并设置锁的过期时间
     *
     * @param expireTime
     * @return
     */
    Boolean lock(Long expireTime);

    /**
     * 在超时时间内阻塞式获取锁并设置锁的过期时间
     *
     * @param expireTime
     * @param timeout
     * @return
     */
    Boolean tryLock(Long expireTime, int timeout);

    /**
     * 释放锁
     * <p>
     * 其它原因失去了对该资源的占有权
     *
     * @return
     */
    Boolean unlock();

    /**
     * 对锁进行延时
     *
     * @param expireTime
     * @return
     */
    Boolean postpone(long expireTime);
}
