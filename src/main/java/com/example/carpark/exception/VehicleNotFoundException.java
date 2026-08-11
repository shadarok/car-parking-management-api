package com.example.carpark.exception;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(String vehicleRegistrationNumber) {
        super("No parked vehicle found with registration: " + vehicleRegistrationNumber);
    }
}
