package com.qhdp.utils;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.qhdp.vo.SeckillVoucherFullVO;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SeckillVoucherCaffeineUtils {
    private final Cache<String, SeckillVoucherFullVO> cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfter(new Expiry<String, SeckillVoucherFullVO>() {
                @Override
                public long expireAfterCreate(String key, SeckillVoucherFullVO value, long currentTime) {
                    long ttlSeconds = 60L;
                    if (value != null && value.getEndTime() != null) {
                        ttlSeconds = Math.max(
                                LocalDateTimeUtil.between(LocalDateTimeUtil.now(), value.getEndTime()).getSeconds(),
                                1L
                        );
                    }
                    return TimeUnit.NANOSECONDS.convert(ttlSeconds, TimeUnit.SECONDS);
                }

                @Override
                public long expireAfterUpdate(String key, SeckillVoucherFullVO value, long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(String key, SeckillVoucherFullVO value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    public SeckillVoucherFullVO get(String voucherId) {
        return cache.getIfPresent(voucherId);
    }

    public void put(String voucherId, SeckillVoucherFullVO voucher) {
        if (voucherId != null && voucher != null) {
            cache.put(voucherId, voucher);
        }
    }

    public void invalidate(String voucherId) {
        cache.invalidate(voucherId);
    }
}
