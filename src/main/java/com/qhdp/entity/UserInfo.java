package com.qhdp.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 
 * @TableName tb_user_info
 */
@TableName(value ="tb_user_info")
@Data
public class UserInfo extends BaseEntity{

    /**
     * 主键，用户id
     */
    private Long userId;

    /**
     * 城市名称
     */
    private String city;

    /**
     * 个人介绍，不要超过128个字符
     */
    private String introduce;

    /**
     * 粉丝数量
     */
    private Integer fans;

    /**
     * 关注的人的数量
     */
    private Integer followee;

    /**
     * 性别，0：男，1：女
     */
    private Integer gender;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 积分
     */
    private Integer credits;

    /**
     * 会员级别，0~9级,0代表未开通会员
     */
    private Integer level;

    /**
     * '逻辑删除 0未删除 1已删除'
     */
    @TableLogic
    @JsonIgnore
    private Integer deleted;


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
        UserInfo other = (UserInfo) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getCity() == null ? other.getCity() == null : this.getCity().equals(other.getCity()))
            && (this.getIntroduce() == null ? other.getIntroduce() == null : this.getIntroduce().equals(other.getIntroduce()))
            && (this.getFans() == null ? other.getFans() == null : this.getFans().equals(other.getFans()))
            && (this.getFollowee() == null ? other.getFollowee() == null : this.getFollowee().equals(other.getFollowee()))
            && (this.getGender() == null ? other.getGender() == null : this.getGender().equals(other.getGender()))
            && (this.getBirthday() == null ? other.getBirthday() == null : this.getBirthday().equals(other.getBirthday()))
            && (this.getCredits() == null ? other.getCredits() == null : this.getCredits().equals(other.getCredits()))
            && (this.getLevel() == null ? other.getLevel() == null : this.getLevel().equals(other.getLevel()))
            && (this.getDeleted() == null ? other.getDeleted() == null : this.getDeleted().equals(other.getDeleted()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getCity() == null) ? 0 : getCity().hashCode());
        result = prime * result + ((getIntroduce() == null) ? 0 : getIntroduce().hashCode());
        result = prime * result + ((getFans() == null) ? 0 : getFans().hashCode());
        result = prime * result + ((getFollowee() == null) ? 0 : getFollowee().hashCode());
        result = prime * result + ((getGender() == null) ? 0 : getGender().hashCode());
        result = prime * result + ((getBirthday() == null) ? 0 : getBirthday().hashCode());
        result = prime * result + ((getCredits() == null) ? 0 : getCredits().hashCode());
        result = prime * result + ((getLevel() == null) ? 0 : getLevel().hashCode());
        result = prime * result + ((getDeleted() == null) ? 0 : getDeleted().hashCode());
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
        sb.append(", userId=").append(userId);
        sb.append(", city=").append(city);
        sb.append(", introduce=").append(introduce);
        sb.append(", fans=").append(fans);
        sb.append(", followee=").append(followee);
        sb.append(", gender=").append(gender);
        sb.append(", birthday=").append(birthday);
        sb.append(", credits=").append(credits);
        sb.append(", level=").append(level);
        sb.append(", deleted=").append(deleted);
        sb.append("]");
        return sb.toString();
    }
}