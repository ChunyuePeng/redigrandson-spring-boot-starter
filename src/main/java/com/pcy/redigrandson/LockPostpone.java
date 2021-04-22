package com.pcy.redigrandson;

/**
 * 通过此类能够判断该分布式锁是否开启了自动延期功能
 * @author Chunyue Peng
 */
public interface LockPostpone {
    /**
     * 用于判断分布式锁是否需要开启自动延期，如果开启了自动续期功能会开启一个守护线程
     * ，在锁快要过期的时候会去判断客户端的操作是否执行完成了，如果此时还没有执行
     * 完成会重新设置该锁的过期时间。
     * @return
     */
    boolean needPostpone();


}
