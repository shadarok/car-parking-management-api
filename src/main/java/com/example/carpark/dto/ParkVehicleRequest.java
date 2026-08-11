package com.example.carpark.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParkVehicleRequest(
        @NotBlank(message = "vehicleReg must not be blank")
        String vehicleReg,

        @NotNull(message = "vehicleType must be provided")
        @Min(value = 1, message = "vehicleType must be 1 (Small), 2 (Medium) or 3 (Large)")
        @Max(value = 3, message = "vehicleType must be 1 (Small), 2 (Medium) or 3 (Large)")
        Integer vehicleType
) {
}
