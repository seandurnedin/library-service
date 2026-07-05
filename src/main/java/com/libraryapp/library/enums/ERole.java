package com.libraryapp.library.enums;

import lombok.Getter;

@Getter
public enum ERole {
    USER("user"),
    MANAGER("manager"),
    ADMIN("admin");

    private final String value;

    ERole(String value) {
        this.value = value;
    }

    public static ERole fromValue(String code) {
        for (ERole cd : ERole.values()) {
            if (cd.getValue().equals(code)) {
                return cd;
            }
        }
        return null;
    }

}