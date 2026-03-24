package com.qhdp.kafka.producer;

import com.qhdp.enums.SeckillVoucherOrderOperate;
import com.qhdp.handler.AbstractProducerHandler;
import com.qhdp.kafka.message.MessageExtend;
import com.qhdp.kafka.message.SeckillVoucherMessage;
import com.qhdp.kafka.redis.RedisVoucherData;
import com.qhdp.toolkit.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @description: Kafka 生产者：发送秒杀券
 * @author: phoenix
 **/
@Slf4j
@Component
public class SeckillVoucherProducer extends AbstractProducerHandler<MessageExtend<SeckillVoucherMessage>> {

    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final RedisVoucherData redisVoucherData;

    /**
     * 手动构造器：注入所有依赖，同时调用父类构造器
     */
    public SeckillVoucherProducer(
            KafkaTemplate<String, MessageExtend<SeckillVoucherMessage>> kafkaTemplate,
            SnowflakeIdGenerator snowflakeIdGenerator,
            RedisVoucherData redisVoucherData) {
        super(kafkaTemplate);
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.redisVoucherData = redisVoucherData;
    }

    @Override
    protected void afterSendFailure(final String topic, final MessageExtend<SeckillVoucherMessage> message, final Throwable throwable) {
        super.afterSendFailure(topic, message, throwable);
        long traceId = snowflakeIdGenerator.nextId();
        redisVoucherData.rollbackRedisVoucherData(
                SeckillVoucherOrderOperate.YES,
                traceId,
                message.getMessageBody().getVoucherId(),
                message.getMessageBody().getUserId(),
                message.getMessageBody().getOrderId(),
                message.getMessageBody().getAfterQty(),
                message.getMessageBody().getChangeQty(),
                message.getMessageBody().getBeforeQty());
    }
}
