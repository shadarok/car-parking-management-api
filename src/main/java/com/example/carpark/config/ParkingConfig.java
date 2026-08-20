package com.example.carpark.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "parking")
@Getter
@Setter
public class ParkingConfig {

    /**
     * Mutable at runtime via {@code PUT /parking/capacity} (seeParkingService#updateCapacity),
     * not just at startup - hence {@code volatile}: ParkingService.getStatus() and the capacity update itself are
     * deliberately not synchronized with park()/exitAndBill() (no check-then-act sequence needs protecting there),
     * so this field needs to guarantee cross-thread visibility on its own rather than relying on a lock.
     */
    private volatile int totalSpaces;
    private Surcharge surcharge;

    public record Surcharge(
            Duration interval,
            BigDecimal feePerInterval
    ) {
    }
}
