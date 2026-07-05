package com.libraryapp.library.enums;

import lombok.Getter;

@Getter
public enum EReservationStatus {
    RESERVED("reserved"),
    NOTIFIED("notified"),
    FULFILLED("fulfilled"),
    CANCELLED("cancelled"),
    EXPIRED("expired");

    private final String value;

    EReservationStatus(String value) {
        this.value = value;
    }

    public static EReservationStatus fromValue(String code) {
        for (EReservationStatus cd : EReservationStatus.values()) {
            if (cd.getValue().equals(code)) {
                return cd;
            }
        }
        return null;
    }

}