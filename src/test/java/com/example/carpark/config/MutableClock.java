package com.example.carpark.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public final class MutableClock extends Clock {

    private Instant fixedInstant;
    private final ZoneId zone;

    public MutableClock(Instant fixedInstant, ZoneId zone) {
        this.fixedInstant = fixedInstant;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(fixedInstant, zone);
    }

    @Override
    public Instant instant() {
        return fixedInstant;
    }

    public void advanceBy(Duration duration) {
        fixedInstant = fixedInstant.plus(duration);
    }
}
