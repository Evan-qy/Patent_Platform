package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String nickname;
    private String avatarUrl;
    private String realName;
    private String idNumber;
}

