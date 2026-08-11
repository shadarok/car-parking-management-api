package com.example.carpark.dto;

public record ParkingStatusResponse(
        int availableSpaces,
        int occupiedSpaces
) {
}
