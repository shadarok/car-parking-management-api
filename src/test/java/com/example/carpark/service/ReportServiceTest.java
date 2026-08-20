package com.example.carpark.service;

import com.example.carpark.config.MutableClock;
import com.example.carpark.dto.SummaryResponse;
import com.example.carpark.model.ParkedVehicle;
import com.example.carpark.model.VehicleRegistrationNumber;
import com.example.carpark.repository.InMemoryParkingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T10:00:00Z");

    @Spy
    private InMemoryParkingRepository parkingRepository = new InMemoryParkingRepository();

    @Spy
    private MutableClock clock = new MutableClock(FIXED_INSTANT, ZoneOffset.UTC);

    @InjectMocks
    private ReportService reportService;

    @Test
    void reportsEmptySummaryOfAllParkedVehicles() {
        final var report = reportService.summaryAllParkedVehicles();

        assertThat(report.summaries()).isEmpty();
    }

    @Test
    void reflectSummaryOfAllParkedVehicles() {
        parkingRepository.save(ParkedVehicle
                .builder()
                        .registrationNumber(new VehicleRegistrationNumber("AB123CDE"))
                        .parkedAt(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC))
                .build()
        );
        clock.advanceBy(Duration.ofMinutes(12));

        final var report = reportService.summaryAllParkedVehicles();

        assertThat(report.summaries()).containsExactly(new SummaryResponse("AB123CDE", 12));
    }
}
