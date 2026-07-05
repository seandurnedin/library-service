package com.libraryapp.library.enums;

import lombok.Getter;

@Getter
public enum EBookStatus {
    IN_STORE("in_store"),
    ON_LOAN("on_loan"),
    RESERVED("reserved"),
    LOST("lost");

    private final String value;

    EBookStatus(String value) {
        this.value = value;
    }

    public static EBookStatus fromValue(String code) {
        for (EBookStatus cd : EBookStatus.values()) {
            if (cd.getValue().equals(code)) {
                return cd;
            }
        }
        return null;
    }

}
