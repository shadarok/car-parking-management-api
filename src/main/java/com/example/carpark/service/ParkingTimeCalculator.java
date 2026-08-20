package com.example.carpark.service;

import java.time.Duration;
import java.time.LocalDateTime;

public class ParkingTimeCalculator {

    public static long minutesParked(LocalDateTime timeIn, LocalDateTime timeOut) {
        return Math.max(Duration.between(timeIn, timeOut).toMinutes(), 1);
    }
}
