package com.qhdp.service;

import com.qhdp.entity.RollbackFailureLog;
import org.springframework.stereotype.Service;

@Service
public interface RollbackAlertService {
    void sendRollbackAlert(RollbackFailureLog log);
}
