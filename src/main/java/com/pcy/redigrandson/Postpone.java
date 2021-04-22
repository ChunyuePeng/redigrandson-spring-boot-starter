package com.pcy.redigrandson;

/**
 * 用于判断该判断持有该锁的客户端是否还需要进行自动延期
 * @author chunyue_peng
 */
public interface Postpone {
    /**
     * 是否需要停止自动延期操作
     * @return
     */
    boolean needStopPostPone();

    void stopPostPone();
}
