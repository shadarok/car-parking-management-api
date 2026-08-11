package com.example.carpark.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ParkedVehicle {
    private final VehicleRegistrationNumber registrationNumber;
    private final int spaceNumber;
    private final VehicleType vehicleType;
    private final LocalDateTime parkedAt;

    public String vehicleRegistrationNumber() {
        return registrationNumber.number();
    }

}
