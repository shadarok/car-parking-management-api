package com.example.carpark.exception;

public class AlreadyParkedException extends RuntimeException {
    public AlreadyParkedException(String vehicleRegistrationNumber) {
        super("Vehicle with registration " + vehicleRegistrationNumber + " is already parked");
    }
}
