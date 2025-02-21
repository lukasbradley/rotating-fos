package com.vlkan.rfos.policy;

import com.vlkan.rfos.Clock;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeeklyRotationPolicy extends TimeBasedRotationPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyRotationPolicy.class);

    private static final WeeklyRotationPolicy INSTANCE = new WeeklyRotationPolicy();

    private WeeklyRotationPolicy() {
        // Do nothing.
    }

    public static WeeklyRotationPolicy getInstance() {
        return INSTANCE;
    }

    @Override
    public LocalDateTime getTriggerDateTime(Clock clock) {
        return clock.sundayMidnight();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String toString() {
        return "WeeklyRotationPolicy";
    }

}
