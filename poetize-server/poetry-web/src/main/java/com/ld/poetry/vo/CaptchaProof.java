package com.ld.poetry.vo;

import lombok.Data;

@Data
public class CaptchaProof {
    private Integer version;
    private String action;
    private Long issuedAt;
    private String fingerprintHash;
    private String ipHash;
}
