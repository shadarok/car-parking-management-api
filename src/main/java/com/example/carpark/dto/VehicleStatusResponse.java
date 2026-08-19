package com.example.carpark.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VehicleStatusResponse(
        String vehicleReg,
        Integer spaceNumber,
        LocalDateTime timeIn,
        BigDecimal ongoingCharge
) {
}
