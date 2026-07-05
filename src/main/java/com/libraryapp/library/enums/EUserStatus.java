package com.libraryapp.library.enums;

import lombok.Getter;

@Getter
public enum EUserStatus {
    ACTIVE("active"),
    SUSPENDED("suspended"),
    INACTIVE("inactive");

    private final String value;

    EUserStatus(String value) {
        this.value = value;
    }

    public static EUserStatus fromValue(String code) {
        for (EUserStatus cd : EUserStatus.values()) {
            if (cd.getValue().equals(code)) {
                return cd;
            }
        }
        return null;
    }

}