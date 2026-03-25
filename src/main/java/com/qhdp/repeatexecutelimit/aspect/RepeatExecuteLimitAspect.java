package com.qhdp.repeatexecutelimit.aspect;

import com.qhdp.annotation.RepeatExecuteLimit;
import com.qhdp.constant.LockInfoType;
import com.qhdp.exception.qhdpFrameException;
import com.qhdp.factory.LockInfoHandleFactory;
import com.qhdp.factory.ServiceLockFactory;
import com.qhdp.handler.RedissonDataHandler;
import com.qhdp.handler.handlerInterface.LockInfoHandle;
import com.qhdp.servicelocker.LockType;
import com.qhdp.servicelocker.ServiceLocker;
import com.qhdp.servicelocker.locallock.LocalLockCache;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.qhdp.constant.RepeatExecuteLimitConstant.PREFIX_NAME;
import static com.qhdp.constant.RepeatExecuteLimitConstant.SUCCESS_FLAG;


/**
 /**
 * &#064;description:  切面
 * &#064;author: phoenix
 **/
@Slf4j
@Aspect
@Order(-11)
@AllArgsConstructor
public class RepeatExecuteLimitAspect {
    
    private final LocalLockCache localLockCache;
    
    private final LockInfoHandleFactory lockInfoHandleFactory;
    
    private final ServiceLockFactory serviceLockFactory;
    
    private final RedissonDataHandler redissonDataHandler;
    
    
    @Around("@annotation(repeatLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatExecuteLimit repeatLimit) throws Throwable {
        long durationTime = repeatLimit.durationTime();
        String message = repeatLimit.message();
        Object obj;
        LockInfoHandle lockInfoHandle = lockInfoHandleFactory.getLockInfoHandle(LockInfoType.REPEAT_EXECUTE_LIMIT);
        String lockName = lockInfoHandle.getLockName(joinPoint,repeatLimit.name(), repeatLimit.keys());
        String repeatFlagName = PREFIX_NAME + lockName;
        String flagObject = redissonDataHandler.get(repeatFlagName);
        if (SUCCESS_FLAG.equals(flagObject)) {
            throw new qhdpFrameException(message);
        }
        ReentrantLock localLock = localLockCache.getLock(lockName,true);
        boolean localLockResult = localLock.tryLock();
        if (!localLockResult) {
            throw new qhdpFrameException(message);
        }
        try {
            ServiceLocker lock = serviceLockFactory.getLock(LockType.Fair);
            boolean result = lock.tryLock(lockName, TimeUnit.SECONDS, 0);
            if (result) {
                try{
                    flagObject = redissonDataHandler.get(repeatFlagName);
                    if (SUCCESS_FLAG.equals(flagObject)) {
                        throw new qhdpFrameException(message);
                    }
                    obj = joinPoint.proceed();
                    if (durationTime > 0) {
                        try {
                            redissonDataHandler.set(repeatFlagName,SUCCESS_FLAG,durationTime,TimeUnit.SECONDS);
                        }catch (Exception e) {
                            log.error("getBucket error",e);
                        }
                    }
                    return obj;
                } finally {
                    lock.unlock(lockName);
                }
            }else{
                throw new qhdpFrameException(message);
            }
        }finally {
            localLock.unlock();
        }
    }
}
