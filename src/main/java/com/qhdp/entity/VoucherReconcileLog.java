package com.qhdp.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName tb_voucher_reconcile_log
 */
@TableName(value ="tb_voucher_reconcile_log")
@Data
public class VoucherReconcileLog extends BaseEntity{

    /**
     * 订单id
     */
    private Long orderId;

    /**
     * 下单的用户id
     */
    private Long userId;

    /**
     * 购买的代金券id
     */
    private Long voucherId;

    /**
     * Kafka消息uuid
     */
    private String messageId;

    /**
     * 差异说明
     */
    private String detail;

    /**
     * 改变之前库存数量
     */
    private Integer beforeQty;

    /**
     * 本次改变数量
     */
    private Integer changeQty;

    /**
     * 改变之后库存数量
     */
    private Integer afterQty;

    /**
     * 追踪唯一标识
     */
    private Long traceId;

    /**
     * 记录类型 -1:扣减 1:恢复
     */
    private Integer logType;

    /**
     * 业务类型：1创建订单成功；2创建订单超时；3创建订单失败
     */
    private Integer businessType;

    /**
     * 对账状态：1待处理；2异常；3不一致；4一致
     */
    private Integer reconciliationStatus;


    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        VoucherReconcileLog other = (VoucherReconcileLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getOrderId() == null ? other.getOrderId() == null : this.getOrderId().equals(other.getOrderId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getVoucherId() == null ? other.getVoucherId() == null : this.getVoucherId().equals(other.getVoucherId()))
            && (this.getMessageId() == null ? other.getMessageId() == null : this.getMessageId().equals(other.getMessageId()))
            && (this.getDetail() == null ? other.getDetail() == null : this.getDetail().equals(other.getDetail()))
            && (this.getBeforeQty() == null ? other.getBeforeQty() == null : this.getBeforeQty().equals(other.getBeforeQty()))
            && (this.getChangeQty() == null ? other.getChangeQty() == null : this.getChangeQty().equals(other.getChangeQty()))
            && (this.getAfterQty() == null ? other.getAfterQty() == null : this.getAfterQty().equals(other.getAfterQty()))
            && (this.getTraceId() == null ? other.getTraceId() == null : this.getTraceId().equals(other.getTraceId()))
            && (this.getLogType() == null ? other.getLogType() == null : this.getLogType().equals(other.getLogType()))
            && (this.getBusinessType() == null ? other.getBusinessType() == null : this.getBusinessType().equals(other.getBusinessType()))
            && (this.getReconciliationStatus() == null ? other.getReconciliationStatus() == null : this.getReconciliationStatus().equals(other.getReconciliationStatus()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getOrderId() == null) ? 0 : getOrderId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getVoucherId() == null) ? 0 : getVoucherId().hashCode());
        result = prime * result + ((getMessageId() == null) ? 0 : getMessageId().hashCode());
        result = prime * result + ((getDetail() == null) ? 0 : getDetail().hashCode());
        result = prime * result + ((getBeforeQty() == null) ? 0 : getBeforeQty().hashCode());
        result = prime * result + ((getChangeQty() == null) ? 0 : getChangeQty().hashCode());
        result = prime * result + ((getAfterQty() == null) ? 0 : getAfterQty().hashCode());
        result = prime * result + ((getTraceId() == null) ? 0 : getTraceId().hashCode());
        result = prime * result + ((getLogType() == null) ? 0 : getLogType().hashCode());
        result = prime * result + ((getBusinessType() == null) ? 0 : getBusinessType().hashCode());
        result = prime * result + ((getReconciliationStatus() == null) ? 0 : getReconciliationStatus().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", orderId=").append(orderId);
        sb.append(", userId=").append(userId);
        sb.append(", voucherId=").append(voucherId);
        sb.append(", messageId=").append(messageId);
        sb.append(", detail=").append(detail);
        sb.append(", beforeQty=").append(beforeQty);
        sb.append(", changeQty=").append(changeQty);
        sb.append(", afterQty=").append(afterQty);
        sb.append(", traceId=").append(traceId);
        sb.append(", logType=").append(logType);
        sb.append(", businessType=").append(businessType);
        sb.append(", reconciliationStatus=").append(reconciliationStatus);
        sb.append("]");
        return sb.toString();
    }
}