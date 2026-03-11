package com.qhdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * Redis回滚失败日志表
 * @TableName tb_rollback_failure_log
 */
@TableName(value ="tb_rollback_failure_log")
@Data
public class RollbackFailureLog {
    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 优惠券id
     */
    private Long voucherId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 订单id
     */
    private Long orderId;

    /**
     * 追踪唯一标识
     */
    private Long traceId;

    /**
     * 失败详情
     */
    private String detail;

    /**
     * Lua返回码(BaseCode)
     */
    private Integer resultCode;

    /**
     * 已尝试的重试次数
     */
    private Integer retryAttempts;

    /**
     * 来源组件
     */
    private String source;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

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
        RollbackFailureLog other = (RollbackFailureLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getVoucherId() == null ? other.getVoucherId() == null : this.getVoucherId().equals(other.getVoucherId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getOrderId() == null ? other.getOrderId() == null : this.getOrderId().equals(other.getOrderId()))
            && (this.getTraceId() == null ? other.getTraceId() == null : this.getTraceId().equals(other.getTraceId()))
            && (this.getDetail() == null ? other.getDetail() == null : this.getDetail().equals(other.getDetail()))
            && (this.getResultCode() == null ? other.getResultCode() == null : this.getResultCode().equals(other.getResultCode()))
            && (this.getRetryAttempts() == null ? other.getRetryAttempts() == null : this.getRetryAttempts().equals(other.getRetryAttempts()))
            && (this.getSource() == null ? other.getSource() == null : this.getSource().equals(other.getSource()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getVoucherId() == null) ? 0 : getVoucherId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getOrderId() == null) ? 0 : getOrderId().hashCode());
        result = prime * result + ((getTraceId() == null) ? 0 : getTraceId().hashCode());
        result = prime * result + ((getDetail() == null) ? 0 : getDetail().hashCode());
        result = prime * result + ((getResultCode() == null) ? 0 : getResultCode().hashCode());
        result = prime * result + ((getRetryAttempts() == null) ? 0 : getRetryAttempts().hashCode());
        result = prime * result + ((getSource() == null) ? 0 : getSource().hashCode());
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
        sb.append(", id=").append(id);
        sb.append(", voucherId=").append(voucherId);
        sb.append(", userId=").append(userId);
        sb.append(", orderId=").append(orderId);
        sb.append(", traceId=").append(traceId);
        sb.append(", detail=").append(detail);
        sb.append(", resultCode=").append(resultCode);
        sb.append(", retryAttempts=").append(retryAttempts);
        sb.append(", source=").append(source);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}