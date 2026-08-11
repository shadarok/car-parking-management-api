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

    private int totalSpaces;
    private Surcharge surcharge;

    public record Surcharge(
            Duration interval,
            BigDecimal feePerInterval
    ) {
    }
}
