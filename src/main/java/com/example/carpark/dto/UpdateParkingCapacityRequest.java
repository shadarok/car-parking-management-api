package com.example.carpark.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateParkingCapacityRequest(

        @NotNull(message = "totalSpaces must be provided")
        @Min(value = 1, message = "totalSpaces must be at least 1")
        Integer totalSpaces
) {
}
