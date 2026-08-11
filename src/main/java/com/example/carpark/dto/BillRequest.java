package com.example.carpark.dto;

import jakarta.validation.constraints.NotBlank;

public record BillRequest(
        @NotBlank(message = "vehicleReg must not be blank")
        String vehicleReg
) {
}
