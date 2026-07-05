package com.libraryapp.library.enums;

import lombok.Getter;

@Getter
public enum EPaymentType {
    LATE_FEE("late_fee"),
    RESERVATION_FEE("reservation_fee"),
    LOST_BOOK_CHARGE("lost_book_charge"),
    OTHER("other");

    private final String value;

    EPaymentType(String value) {
        this.value = value;
    }

    public static EPaymentType fromValue(String code) {
        for (EPaymentType cd : EPaymentType.values()) {
            if (cd.getValue().equals(code)) {
                return cd;
            }
        }
        return null;
    }

}