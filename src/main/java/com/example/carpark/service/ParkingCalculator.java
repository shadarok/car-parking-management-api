package com.example.carpark.service;

import java.time.Duration;
import java.time.LocalDateTime;

public class ParkingCalculator {

    public static long minutesParked(LocalDateTime timeIn, LocalDateTime timeOut) {
        return Math.max(Duration.between(timeIn, timeOut).toMinutes(), 1);
    }

    /**
     * Math::max is a safeguard against config runtime mutation.
     */
    public static int availableSpaces(int totalSpaces, int occupiedSpaces) {
        return Math.max(totalSpaces - occupiedSpaces, 0);
    }
}
