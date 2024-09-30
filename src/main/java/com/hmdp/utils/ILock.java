package com.hmdp.utils;

public interface ILock {

    /**
     *尝试获取锁
     * @param timeOutSec 锁持有的时间，过期后自动释放防止死锁
     * @return  获取成功或失败
     */
    boolean tryLock(long timeOutSec);

    void unlock();
}
