package com.example.carpark.dto;

import com.example.carpark.model.ParkedVehicle;

public record SummaryResponse(String vehicleReg, long minutesParked) {
    public static SummaryResponse of(ParkedVehicle parkedVehicle, long minutesParked) {
        return new SummaryResponse(parkedVehicle.vehicleRegistrationNumber(), minutesParked);
    }
}