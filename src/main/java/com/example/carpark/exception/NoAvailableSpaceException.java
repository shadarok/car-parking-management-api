package com.example.carpark.exception;

public class NoAvailableSpaceException extends RuntimeException {
    public NoAvailableSpaceException() {
        super("No available parking spaces");
    }
}
