package com.example.carpark.repository;

import com.example.carpark.model.ParkedVehicle;
import com.example.carpark.model.VehicleRegistrationNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class InMemoryParkingRepository implements ParkingRepository {

    private final Map<Integer, ParkedVehicle> vehicleBySpace = new ConcurrentHashMap<>();
    private final Map<VehicleRegistrationNumber, Integer> spaceByRegistrationNumber = new ConcurrentHashMap<>();

    @Override
    public boolean isRegistrationParked(VehicleRegistrationNumber registrationNumber) {
        return spaceByRegistrationNumber.containsKey(registrationNumber);
    }

    /**
     * Returns the lowest unoccupied space number in the range [1, totalSpaces]
     * or empty if every space is taken.
     */
    @Override
    public Optional<Integer> findFirstAvailableSpace(int totalSpaces) {
        return IntStream.rangeClosed(1, totalSpaces)
                .filter(spaceNumber -> !vehicleBySpace.containsKey(spaceNumber))
                .boxed()
                .findFirst();
    }

    @Override
    public void save(ParkedVehicle vehicle) {
        vehicleBySpace.put(vehicle.getSpaceNumber(), vehicle);
        spaceByRegistrationNumber.put(vehicle.getRegistrationNumber(), vehicle.getSpaceNumber());
    }

    @Override
    public Optional<ParkedVehicle> findVehicleByRegistration(VehicleRegistrationNumber registrationNumber) {
        return Optional
                .ofNullable(spaceByRegistrationNumber.get(registrationNumber))
                .map(vehicleBySpace::get);
    }

    @Override
    public void remove(VehicleRegistrationNumber vehicleRegistrationNumber, int spaceNumber) {
        vehicleBySpace.remove(spaceNumber);
        spaceByRegistrationNumber.remove(vehicleRegistrationNumber);
    }

    @Override
    public int countOccupiedSpaces() {
        return vehicleBySpace.size();
    }
}
