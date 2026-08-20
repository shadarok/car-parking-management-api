package com.example.carpark.controller;

import com.example.carpark.dto.*;
import com.example.carpark.service.ParkingService;
import com.example.carpark.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;
    private final ReportService reportService;

    /**
     * Gets available and occupied number of spaces
     *
     * @return {@link ParkingStatusResponse}
     */
    @GetMapping
    public ResponseEntity<ParkingStatusResponse> status() {
        return ResponseEntity.ok(parkingService.status());
    }

    /**
     * Gets a given vehicle status
     *
     * @return {@link VehicleStatusResponse}
     */
    @GetMapping("/{vehicleRegistrationNumber}")
    public ResponseEntity<VehicleStatusResponse> vehicleStatus(
            @PathVariable("vehicleRegistrationNumber") String vehicleRegistrationNumber) {
        return ResponseEntity.ok(parkingService.vehicleStatus(vehicleRegistrationNumber));
    }

    /**
     * Parks a given vehicle in the first available space and returns the vehicle and its space number
     *
     * @param request {{@link ParkVehicleRequest}
     * @return {@link ParkedVehicleResponse}
     */
    @PostMapping
    public ResponseEntity<ParkedVehicleResponse> parkVehicle(@Valid @RequestBody ParkVehicleRequest request) {
        final var response = parkingService.park(request.vehicleReg(), request.vehicleType());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Frees up this vehicles space and return its final charge from its parking time until now
     *
     * @param request {@link BillRequest}
     * @return {@link BillResponse}
     */
    @PostMapping("/bill")
    public ResponseEntity<BillResponse> exitAndBill(@Valid @RequestBody BillRequest request) {
        final var response = parkingService.exitAndBill(request.vehicleReg());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponses> summary() {
        final var summaries = reportService.summaryAllParkedVehicles();
        return ResponseEntity.ok(summaries);
    }
}
