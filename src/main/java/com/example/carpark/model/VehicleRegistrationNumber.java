package com.example.carpark.model;

/**
 * Registrations are treated case-insensitively (and trimmed) so
 * "ab12cde" and "AB12CDE" are recognized as the same vehicle
 */
public record VehicleRegistrationNumber(String number) {
    public VehicleRegistrationNumber {
        number = number.trim().toUpperCase();
    }
}
