package com.example.carpark.dto;

import java.time.LocalDateTime;

public record ParkedVehicleResponse(
        String vehicleReg,
        Integer spaceNumber,
        LocalDateTime timeIn
) {
}
