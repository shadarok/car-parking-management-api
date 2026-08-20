package com.example.carpark.repository;

import com.example.carpark.model.ParkedVehicle;
import com.example.carpark.model.VehicleRegistrationNumber;

import java.util.List;
import java.util.Optional;

public interface ParkingRepository {
    boolean isRegistrationParked(VehicleRegistrationNumber registrationNumber);

    Optional<Integer> findFirstAvailableSpace(int totalSpaces);

    void save(ParkedVehicle vehicle);

    Optional<ParkedVehicle> findVehicleByRegistration(VehicleRegistrationNumber registrationNumber);

    void remove(VehicleRegistrationNumber vehicleRegistrationNumber, int spaceNumber);

    int countOccupiedSpaces();

    List<ParkedVehicle> findAllParkedVehicles();
}
