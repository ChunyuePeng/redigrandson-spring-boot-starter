package com.pcy.redigrandson.postpone;

import com.pcy.redigrandson.DistributedLock;
import com.pcy.redigrandson.Postpone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @description:
 * @author: 彭椿悦
 * @data: 2021/4/20 20:17
 */
public class PostponeTask implements Runnable {
    /**
     * 锁名
     */
    private String key;
    /**
     * 锁名所对应的值
     */
    private String value;
    /**
     * 设置的过期时间
     */
    private long expireTime;
    private Postpone postPone;
    private DistributedLock distributedLock;
    Logger logger = LoggerFactory.getLogger(PostponeTask.class);

    public PostponeTask(String key, String value, long expireTime, DistributedLock distributedLock, Postpone postPone) {
        this.key = key;
        this.value = value;
        this.expireTime = expireTime;
        this.distributedLock = distributedLock;
        this.postPone = postPone;
    }

    @Override
    public void run() {
        //等待waitTime之后对锁续期
        long waitTime = expireTime * 2 / 3;
        while (!postPone.needStopPostPone()) {
            try {
                Thread.sleep(waitTime);
                //延时成功
                if (distributedLock.postpone(key, value, expireTime)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(value + "延期成功");
                    }

                }
                //延时失败
                else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(value + "延期失败");
                    }

                    break;
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("关闭了" + value + "的延时线程");
        }

    }

}
