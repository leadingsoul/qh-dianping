package com.qhdp.service;


/**
 * @description: 自动发券成功后的用户通知服务接口
 * @author: phoenix
 **/
public interface AutoIssueNotifyService {
    
    void sendAutoIssueNotify(Long voucherId, Long userId, Long orderId);
}