package com.qhdp.servicelocker.info;


import com.qhdp.handler.AbstractLockInfoHandle;

/**
 * @description: 锁信息
 * @author: phoenix
 **/
public class ServiceLockInfoHandle extends AbstractLockInfoHandle {

    private static final String LOCK_PREFIX_NAME = "SERVICE_LOCK";
    
    @Override
    protected String getLockPrefixName() {
        return LOCK_PREFIX_NAME;
    }
}
