package com.ld.poetry.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Integer id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String phoneNumber;

    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    private Integer gender;

    /**
     * 用户状态[true:启用，false:禁用]
     */
    private Boolean userStatus;

    private String avatar;

    private String introduction;

    private String subscribe;

    private String openId;

    private String platformType;

    private String uid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean isBoss = false;

    private String accessToken;

    /**
     * 赞赏
     */
    private String admire;

    /**
     * 是否为第三方登录用户
     */
    private Boolean isThirdPartyUser;

    /**
     * 获取是否为第三方登录用户
     */
    public Boolean getIsThirdPartyUser() {
        if (isThirdPartyUser == null) {
            isThirdPartyUser = platformType != null && !platformType.trim().isEmpty();
        }
        return isThirdPartyUser;
    }

    /**
     * 设置是否为第三方登录用户
     */
    public void setIsThirdPartyUser(Boolean isThirdPartyUser) {
        this.isThirdPartyUser = isThirdPartyUser;
    }

    /**
     * 获取用户类型显示名称
     */
    public String getUserTypeDisplayName() {
        if (getIsThirdPartyUser()) {
            return "第三方用户";
        } else {
            return "普通用户";
        }
    }

    private String code;

    private Integer userType;

    /**
     * 验证码token（用于登录/注册时的验证码校验）
     */
    private String verificationToken;
}
