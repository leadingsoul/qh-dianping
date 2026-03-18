package com.qhdp.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 
 * @TableName tb_voucher
 */
@TableName(value ="tb_voucher")
@Data
public class Voucher extends BaseEntity{

    /**
     * 商铺id
     */
    private Long shopId;

    /**
     * 代金券标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subTitle;

    /**
     * 使用规则
     */
    private String rules;

    /**
     * 支付金额，单位是分。例如200代表2元
     */
    private Long payValue;

    /**
     * 抵扣金额，单位是分。例如200代表2元
     */
    private Long actualValue;

    /**
     * 0,普通券；1,秒杀券
     */
    private Integer type;

    /**
     * 1,上架; 2,下架; 3,过期
     */
    private Integer status;

    /**
     * 逻辑删除 0未删除 1已删除
     */
    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /**
     * 库存
     */
    @Version
    @TableField(exist = false)
    private Integer stock;

    /**
     * 生效时间
     */
    @TableField(exist = false)
    @Schema(description = "生效时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date beginTime;

    /**
     * 失效时间
     */
    @TableField(exist = false)
    @Schema(description = "失效时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

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
        Voucher other = (Voucher) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getShopId() == null ? other.getShopId() == null : this.getShopId().equals(other.getShopId()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getSubTitle() == null ? other.getSubTitle() == null : this.getSubTitle().equals(other.getSubTitle()))
            && (this.getRules() == null ? other.getRules() == null : this.getRules().equals(other.getRules()))
            && (this.getPayValue() == null ? other.getPayValue() == null : this.getPayValue().equals(other.getPayValue()))
            && (this.getActualValue() == null ? other.getActualValue() == null : this.getActualValue().equals(other.getActualValue()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getDeleted() == null ? other.getDeleted() == null : this.getDeleted().equals(other.getDeleted()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getStock() == null ? other.getStock() == null : this.getStock().equals(other.getStock()))
            && (this.getBeginTime() == null ? other.getBeginTime() == null : this.getBeginTime().equals(other.getBeginTime()))
            && (this.getEndTime() == null ? other.getEndTime() == null : this.getEndTime().equals(other.getEndTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getShopId() == null) ? 0 : getShopId().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getSubTitle() == null) ? 0 : getSubTitle().hashCode());
        result = prime * result + ((getRules() == null) ? 0 : getRules().hashCode());
        result = prime * result + ((getPayValue() == null) ? 0 : getPayValue().hashCode());
        result = prime * result + ((getActualValue() == null) ? 0 : getActualValue().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getDeleted() == null) ? 0 : getDeleted().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getStock() == null) ? 0 : getStock().hashCode());
        result = prime * result + ((getBeginTime() == null) ? 0 : getBeginTime().hashCode());
        result = prime * result + ((getEndTime() == null) ? 0 : getEndTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", shopId=").append(shopId);
        sb.append(", title=").append(title);
        sb.append(", subTitle=").append(subTitle);
        sb.append(", rules=").append(rules);
        sb.append(", payValue=").append(payValue);
        sb.append(", actualValue=").append(actualValue);
        sb.append(", type=").append(type);
        sb.append(", status=").append(status);
        sb.append(", deleted=").append(deleted);
        sb.append(", stock=").append(stock);
        sb.append(", beginTime=").append(beginTime);
        sb.append(", endTime=").append(endTime);
        sb.append("]");
        return sb.toString();
    }
}