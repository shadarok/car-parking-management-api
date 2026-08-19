package com.example.carpark.dto;

public record ParkingCapacityResponse(
        int totalSpaces,
        int availableSpaces,
        int occupiedSpaces
) {
}
