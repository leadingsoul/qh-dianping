package com.qhdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.qhdp.enums.RedisKeyManage;
import com.qhdp.service.AutoIssueNotifyService;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * &#064;description:  自动发券成功后的用户通知服务接口实现
 * &#064;author:  phoenix
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoIssueNotifyServiceImpl implements AutoIssueNotifyService {

    @Value("${seckill.autoissue.notify.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${seckill.autoissue.notify.app.enabled:false}")
    private boolean appEnabled;

    @Value("${seckill.autoissue.notify.sms.to:}")
    private String smsTo;

    @Value("${seckill.autoissue.notify.dedup.window.seconds:300}")
    private long dedupWindowSeconds;

    private final RedisUtils redisUtils;

    @Override
    public void sendAutoIssueNotify(Long voucherId, Long userId, Long orderId) {
        try {
            if (!shouldNotify(voucherId, userId)) {
                return;
            }
            String content = String.format("自动发券成功 | voucherId=%s userId=%s orderId=%s", voucherId, userId, orderId);
            if (smsEnabled && StrUtil.isNotBlank(smsTo)) {
                log.info("[AUTOISSUE_SMS] to={} content={}", smsTo, content);
            }
            if (appEnabled) {
                log.info("[AUTOISSUE_APP] userId={} content={}", userId, content);
            }
        } catch (Exception e) {
            log.warn("发送自动发券通知异常", e);
        }
    }

    private boolean shouldNotify(Long voucherId, Long userId) {
        try {
            return redisUtils.setIfAbsent(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_AUTO_ISSUE_NOTIFY_DEDUP_KEY, voucherId, userId),
                    "1",
                    dedupWindowSeconds,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            return true;
        }
    }
}