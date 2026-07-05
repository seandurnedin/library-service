package com.libraryapp.library.enums;

import lombok.Getter;

@Getter
public enum EBorrowingStatus {
    ON_LOAN("on_loan"),
    RETURNED("returned"),
    OVERDUE("overdue"),
    LOST("lost");

    private final String value;

    EBorrowingStatus(String value) {
        this.value = value;
    }

    public static EBorrowingStatus fromValue(String code) {
        for (EBorrowingStatus cd : EBorrowingStatus.values()) {
            if (cd.getValue().equals(code)) {
                return cd;
            }
        }
        return null;
    }

}