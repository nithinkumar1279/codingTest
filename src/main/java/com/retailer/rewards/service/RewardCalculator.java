package com.retailer.rewards.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * Points rules:
 * <ul>
 *     <li>2 points for every dollar spent over $100</li>
 *     <li>1 point for every dollar spent between $50 and $100</li>
 * </ul>
 * Only whole dollars count, so cents are dropped before applying the rules
 * (e.g. $120.75 is treated as $120 and earns 90 points).
 */
@Component
public class RewardCalculator {

    private static final long LOWER_THRESHOLD = 50;
    private static final long UPPER_THRESHOLD = 100;
    private static final long POINTS_ABOVE_UPPER_THRESHOLD = 2;

    public long calculatePoints(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Transaction amount must be zero or positive, but was " + amount);
        }

        long wholeDollars = amount.setScale(0, RoundingMode.DOWN).longValueExact();

        long dollarsOverUpper = Math.max(0, wholeDollars - UPPER_THRESHOLD);
        long dollarsBetweenThresholds = Math.min(Math.max(0, wholeDollars - LOWER_THRESHOLD),
                UPPER_THRESHOLD - LOWER_THRESHOLD);

        return dollarsOverUpper * POINTS_ABOVE_UPPER_THRESHOLD + dollarsBetweenThresholds;
    }
}
