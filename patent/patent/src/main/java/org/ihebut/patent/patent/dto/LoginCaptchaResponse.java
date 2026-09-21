package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginCaptchaResponse {
    private String captchaId;
    private String question;
    private long expiresInSeconds;
}
