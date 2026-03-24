package com.qhdp.delay.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @description: 开抢提醒消息DTO
 * @author: phoenix
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DelayedVoucherReminderMessage {
    
    private Long voucherId;
    
    private Date beginTime;
}