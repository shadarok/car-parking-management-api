package com.example.carpark.service;

import com.example.carpark.config.ParkingConfig;
import com.example.carpark.dto.BillResponse;
import com.example.carpark.exception.AlreadyParkedException;
import com.example.carpark.exception.NoAvailableSpaceException;
import com.example.carpark.exception.VehicleNotFoundException;
import com.example.carpark.model.VehicleType;
import com.example.carpark.repository.InMemoryParkingRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    private static final int TOTAL_SPACES = 2;

    @Spy
    private ParkingConfig parkingConfig = createConfig();

    @Spy
    private InMemoryParkingRepository parkingRepository = new InMemoryParkingRepository();

    @InjectMocks
    private ParkingService parkingService;

    private static ParkingConfig createConfig() {
        final var config = new ParkingConfig();
        config.setTotalSpaces(TOTAL_SPACES);
        config.setSurcharge(new ParkingConfig.Surcharge(Duration.of(5, ChronoUnit.MINUTES), BigDecimal.ONE));
        return config;
    }

    @Nested
    class Status {

        @Test
        void reportsAllSpacesAvailableWhenEmpty() {
            final var status = parkingService.status();

            assertThat(status.availableSpaces()).isEqualTo(2);
            assertThat(status.occupiedSpaces()).isEqualTo(0);
        }

        @Test
        void reflectsOccupiedSpacesAfterParking() {
            parkingService.park("AB12CDE", 1);

            final var status = parkingService.status();

            assertThat(status.availableSpaces()).isEqualTo(1);
            assertThat(status.occupiedSpaces()).isEqualTo(1);
        }
    }

    @Nested
    class Parking {

        @Test
        void allocatesFirstAvailableSpace() {
            final var response = parkingService.park("AB12CDE", 1);

            assertThat(response.vehicleReg()).isEqualTo("AB12CDE");
            assertThat(response.spaceNumber()).isEqualTo(1);
            assertThat(response.timeIn()).isNotNull();
        }

        @Test
        void allocatesNextSpaceWhenFirstIsTaken() {
            parkingService.park("AB12CDE", 1);

            final var response = parkingService.park("XY34FGH", 2);

            assertThat(response.spaceNumber()).isEqualTo(2);
            assertThat(response.vehicleReg()).isEqualTo("XY34FGH");
            assertThat(response.timeIn()).isNotNull();
        }

        @Test
        void throwsWhenCarParkIsFull() {
            parkingService.park("AB12CDE", 1);
            parkingService.park("XY34FGH", 2);

            assertThatThrownBy(() -> parkingService.park("ZZ99ZZZ", 1))
                    .isInstanceOf(NoAvailableSpaceException.class);
        }

        @Test
        void throwsWhenVehicleAlreadyParked() {
            parkingService.park("AB12CDE", 1);

            assertThatThrownBy(() -> parkingService.park("AB12CDE", 2))
                    .isInstanceOf(AlreadyParkedException.class);
        }

        @Test
        void treatsRegistrationsCaseInsensitively() {
            final var response = parkingService.park("ab12cde", 1);

            assertThat(response.spaceNumber()).isEqualTo(1);
            assertThat(response.vehicleReg()).isEqualTo("AB12CDE");
            assertThat(response.timeIn()).isNotNull();
        }

        @Test
        void rejectsInvalidVehicleTypeCode() {
            assertThatThrownBy(() -> parkingService.park("AB12CDE", 9))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Billing {

        @Test
        void throwsWhenVehicleNotParked() {
            assertThatThrownBy(() -> parkingService.exitAndBill("UNKNOWN"))
                    .isInstanceOf(VehicleNotFoundException.class);
        }

        @Test
        void freesUpSpaceOnExit() {
            parkingService.park("AB12CDE", 1);

            parkingService.exitAndBill("AB12CDE");

            final var status = parkingService.status();
            assertThat(status.occupiedSpaces()).isEqualTo(0);
            assertThat(status.availableSpaces()).isEqualTo(2);
        }

        @Test
        void allowsSpaceToBeReallocatedAfterExit() {
            parkingService.park("AB12CDE", 1);
            parkingService.exitAndBill("AB12CDE");

            final var response = parkingService.park("XY34FGH", 1);

            assertThat(response.spaceNumber()).isEqualTo(1);
        }

        @Test
        void returnsBillWithGeneratedIdAndConsistentTimestamps() {
            parkingService.park("AB12CDE", 1);

            BillResponse bill = parkingService.exitAndBill("AB12CDE");

            assertThat(bill.billId()).isNotBlank();
            assertThat(bill.vehicleReg()).isEqualTo("AB12CDE");
            assertThat(bill.timeOut()).isAfterOrEqualTo(bill.timeIn());
        }
    }

    @Nested
    class ChargeCalculation {

        @Test
        void chargesBaseRateOnlyUnderFirstFiveMinuteBlock() {
            final var in = LocalDateTime.of(2026, 1, 1, 10, 0);
            final var out = in.plusMinutes(4);

            final var charge = parkingService.calculateCharge(VehicleType.SMALL, in, out);

            // 4 * 0.10 = 0.40, no completed 5-minute block yet
            assertThat(charge).isEqualByComparingTo("0.40");
        }

        @Test
        void addsSurchargeAtExactlyFiveMinutes() {
            final var in = LocalDateTime.of(2026, 1, 1, 10, 0);
            final var out = in.plusMinutes(5);

            final var charge = parkingService.calculateCharge(VehicleType.SMALL, in, out);

            // 5 * 0.10 = 0.50 + 1 block * £1 = 1.50
            assertThat(charge).isEqualByComparingTo("1.50");
        }

        @Test
        void doesNotChargeForAPartialFiveMinuteBlock() {
            final var in = LocalDateTime.of(2026, 1, 1, 10, 0);
            final var out = in.plusMinutes(12);

            final var charge = parkingService.calculateCharge(VehicleType.SMALL, in, out);

            // 12 * 0.10 = 1.20 + 2 complete blocks * £1 = 3.20 (not 3 blocks)
            assertThat(charge).isEqualByComparingTo("3.20");
        }

        @Test
        void appliesMediumCarRate() {
            final var in = LocalDateTime.of(2026, 1, 1, 10, 0);
            final var out = in.plusMinutes(10);

            final var charge = parkingService.calculateCharge(VehicleType.MEDIUM, in, out);

            // 10 * 0.20 = 2.00 + 2 blocks * £1 = 4.00
            assertThat(charge).isEqualByComparingTo("4.00");
        }

        @Test
        void appliesLargeCarRate() {
            final var in = LocalDateTime.of(2026, 1, 1, 10, 0);
            final var out = in.plusMinutes(10);

            BigDecimal charge = parkingService.calculateCharge(VehicleType.LARGE, in, out);

            // 10 * 0.40 = 4.00 + 2 blocks * £1 = 6.00
            assertThat(charge).isEqualByComparingTo("6.00");
        }

        @Test
        void chargesMinimumOneMinuteForAVeryShortStay() {
            final var in = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
            final var out = in.plusSeconds(30);

            final var charge = parkingService.calculateCharge(VehicleType.SMALL, in, out);

            assertThat(charge).isEqualByComparingTo("0.10");
        }
    }
}
