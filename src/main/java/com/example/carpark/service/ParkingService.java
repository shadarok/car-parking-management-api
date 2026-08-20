package com.example.carpark.service;

import com.example.carpark.config.ParkingConfig;
import com.example.carpark.dto.*;
import com.example.carpark.dto.ParkingCapacityResponse;
import com.example.carpark.dto.VehicleStatusResponse;
import com.example.carpark.exception.AlreadyParkedException;
import com.example.carpark.exception.NoAvailableSpaceException;
import com.example.carpark.exception.VehicleNotFoundException;
import com.example.carpark.model.ParkedVehicle;
import com.example.carpark.model.VehicleRegistrationNumber;
import com.example.carpark.model.VehicleType;
import com.example.carpark.repository.InMemoryParkingRepository;
import com.example.carpark.repository.ParkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrates the car park's business rules - allocation, status,
 * billing and de-allocation. Delegates all state storage/lookup to
 * {@link InMemoryParkingRepository}.
 * <p>
 * Space numbers run from 1 to {@link ParkingConfig#getTotalSpaces()}.
 */

@Service
@RequiredArgsConstructor
public class ParkingService {

    private final ParkingRepository parkingRepository;
    private final ParkingConfig parkingConfig;
    private final Clock clock;

    /**
     * Returns current occupancy against total configured capacity.
     */
    public ParkingStatusResponse status() {
        final int occupiedSpaces = parkingRepository.countOccupiedSpaces();

        return new ParkingStatusResponse(
                ParkingCalculator.availableSpaces(parkingConfig.getTotalSpaces(), occupiedSpaces),
                occupiedSpaces
        );
    }

    /**
     * Allocates the given vehicle to the first available space (lowest space number).
     * Synchronized so two concurrent requests can't both be allocated the same space.
     *
     * @throws AlreadyParkedException    if the registration number is already parked
     * @throws IllegalArgumentException  if vehicleTypeCode isn't recognized
     * @throws NoAvailableSpaceException if the car park is full
     */
    public synchronized ParkedVehicleResponse park(String registrationNumber, int vehicleTypeCode) {
        final var normalizedRegistrationNumber = new VehicleRegistrationNumber(registrationNumber);

        if (parkingRepository.isRegistrationParked(normalizedRegistrationNumber)) {
            throw new AlreadyParkedException(registrationNumber);
        }

        final var availableSpaceNumber = parkingRepository
                .findFirstAvailableSpace(parkingConfig.getTotalSpaces())
                .orElseThrow(NoAvailableSpaceException::new);

        final var vehicleType = VehicleType.of(vehicleTypeCode);

        final var vehicle = ParkedVehicle
                .builder()
                .registrationNumber(normalizedRegistrationNumber)
                .spaceNumber(availableSpaceNumber)
                .vehicleType(vehicleType)
                .parkedAt(LocalDateTime.now(clock))
                .build();

        parkingRepository.save(vehicle);

        return new ParkedVehicleResponse(
                vehicle.vehicleRegistrationNumber(),
                vehicle.getSpaceNumber(),
                vehicle.getParkedAt()
        );
    }

    /**
     * Frees up the vehicle's space and returns its final bill.
     *
     * @throws VehicleNotFoundException if the registration number isn't currently parked
     */
    public synchronized BillResponse exitAndBill(String registrationNumber) {
        final var normalizedRegistrationNumber = new VehicleRegistrationNumber(registrationNumber);

        final var parkedVehicle = parkingRepository
                .findVehicleByRegistration(normalizedRegistrationNumber)
                .orElseThrow(() -> new VehicleNotFoundException(registrationNumber));

        final var timeOut = LocalDateTime.now(clock);
        final var charge = calculateCharge(
                parkedVehicle.getVehicleType(),
                parkedVehicle.getParkedAt(),
                timeOut
        );

        parkingRepository.remove(normalizedRegistrationNumber, parkedVehicle.getSpaceNumber());

        return new BillResponse(
                UUID.randomUUID().toString(),
                parkedVehicle.vehicleRegistrationNumber(),
                charge,
                parkedVehicle.getParkedAt(),
                timeOut
        );
    }

    /**
     * Calculates the parking charge for a stay:
     * - Billed per whole elapsed minute (partial minutes aren't rounded up),
     *   with a minimum of 1 minute so a very short stay is never free.
     * - The GBP 1 surcharge applies once per COMPLETE 5-minute block/interval (not rounded up),
     *   E.g. 12 minutes = 2 complete blocks = GBP 2 surcharge (not 3).
     */
    BigDecimal calculateCharge(VehicleType vehicleType, LocalDateTime timeIn, LocalDateTime timeOut) {
        final var minutesParked = ParkingCalculator.minutesParked(timeIn, timeOut);

        final var baseCharge = vehicleType
                .getRatePerMinute()
                .multiply(BigDecimal.valueOf(minutesParked));

        final var intervalInMinutes = parkingConfig
                .getSurcharge()
                .interval()
                .toMinutes();
        final var completedSurchargeBlocks = minutesParked / intervalInMinutes;
        final var surcharge = parkingConfig
                .getSurcharge()
                .feePerInterval()
                .multiply(BigDecimal.valueOf(completedSurchargeBlocks));

        return baseCharge.add(surcharge).setScale(2, RoundingMode.HALF_UP);
    }

    public VehicleStatusResponse vehicleStatus(String vehicleRegistrationNumber) {
        final var normalizedRegistrationNumber = new VehicleRegistrationNumber(vehicleRegistrationNumber);

        final var parkedVehicle = parkingRepository
                .findVehicleByRegistration(normalizedRegistrationNumber)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleRegistrationNumber));

        final var currentTime = LocalDateTime.now(clock);
        final var ongoingCharge = calculateCharge(
                parkedVehicle.getVehicleType(),
                parkedVehicle.getParkedAt(),
                currentTime
        );

        return new VehicleStatusResponse(
                parkedVehicle.vehicleRegistrationNumber(),
                parkedVehicle.getSpaceNumber(),
                parkedVehicle.getParkedAt(),
                ongoingCharge
        );
    }
    /**
     * Updates the car park's total capacity.
     * <p>
     * Deliberately NOT synchronized with {@link ParkingService#park}/{@link ParkingService#exitAndBill}:
     * unlike those methods, this is a single field write with no check-then-act sequence to protect,
     * so plain int write-atomicity plus {@code volatile} on {@link ParkingConfig#getTotalSpaces()}
     * (for cross-thread visibility)
     * <p>
     * Deliberately allows shrinking capacity below current occupancy
     * (e.g. a section of the car park closed for maintenance) rather than rejecting the change:
     * already-parked vehicles keep their spaces and can still exit normally via exitAndBill;
     * {@link ParkingRepository#findFirstAvailableSpace} naturally won't offer spaces beyond the new lower limit;
     */
    public ParkingCapacityResponse updateCapacity(Integer newTotalCapacity) {
        parkingConfig.setTotalSpaces(newTotalCapacity);

        final var occupiedSpaces = parkingRepository.countOccupiedSpaces();
        final var totalSpaces = parkingConfig.getTotalSpaces();

        return new ParkingCapacityResponse(
                totalSpaces,
                ParkingCalculator.availableSpaces(totalSpaces, occupiedSpaces),
                occupiedSpaces
        );
    }

}
