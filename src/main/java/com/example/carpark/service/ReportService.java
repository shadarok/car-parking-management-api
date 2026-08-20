package com.example.carpark.service;

import com.example.carpark.dto.SummaryResponse;
import com.example.carpark.dto.SummaryResponses;
import com.example.carpark.model.ParkedVehicle;
import com.example.carpark.repository.ParkingRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ParkingRepository parkingRepository;
    private final Clock clock;

    public SummaryResponses summaryAllParkedVehicles() {
        final var summaries = parkingRepository
                .findAllParkedVehicles()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
        return new SummaryResponses(summaries);
    }

    private @NonNull SummaryResponse toSummaryResponse(ParkedVehicle parkedVehicle) {
        final var minutesParked = ParkingTimeCalculator.minutesParked(
                parkedVehicle.getParkedAt(),
                LocalDateTime.now(clock)
        );
        return SummaryResponse.of(parkedVehicle, minutesParked);
    }

}
