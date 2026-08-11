package com.example.carpark.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillResponse(
        String billId,
        String vehicleReg,
        BigDecimal vehicleCharge,
        LocalDateTime timeIn,
        LocalDateTime timeOut
) {
}
