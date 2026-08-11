package com.example.carpark.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;

@Getter
public enum VehicleType {

    SMALL(1, new BigDecimal("0.10")),
    MEDIUM(2, new BigDecimal("0.20")),
    LARGE(3, new BigDecimal("0.40"));

    private final int code;
    private final BigDecimal ratePerMinute;

    VehicleType(int code, BigDecimal ratePerMinute) {
        this.code = code;
        this.ratePerMinute = ratePerMinute;
    }

    public static VehicleType of(int code) {
        return Arrays.stream(VehicleType.values())
                .filter(vehicleType -> vehicleType.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid vehicle type: " + code));
    }
}
