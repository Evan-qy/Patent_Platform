package org.ihebut.patent.patent.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRoleId implements Serializable {
    private Long userId;
    private Long roleId;
}

