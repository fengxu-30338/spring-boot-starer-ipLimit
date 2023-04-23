package com.fengxu.iplimit.util;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 工具类
 */
@Component
public class RedissonUtil {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 加锁
     *
     * @param lockKey
     * @return
     */
    public RLock lock(String lockKey) {
        RLock rLock = redissonClient.getLock(lockKey);
        rLock.lock();
        return rLock;
    }

    /**
     * 加锁
     *
     * @param lockKey
     * @param timeout 超时时间，单位：秒
     * @return
     */
    public RLock lock(String lockKey, int timeout) {
        RLock rLock = redissonClient.getLock(lockKey);
        rLock.lock(timeout, TimeUnit.SECONDS);
        return rLock;
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey
     * @return
     */
    public boolean tryLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.tryLock();
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey
     * @param waitTime 最多等待时间
     * @return
     */
    public boolean tryLock(String lockKey, int waitTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey
     * @param waitTime  最多等待时间
     * @param leaseTime 上锁后自动释放锁时间
     * @return
     */
    public boolean tryLock(String lockKey, int waitTime, int leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 解锁
     *
     * @param lockKey
     */
    public void unlock(String lockKey) {
        RLock rLock = redissonClient.getLock(lockKey);
        if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
            rLock.unlock();
        }
    }

}
