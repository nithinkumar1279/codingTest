package com.retailer.rewards.dto;

import java.time.YearMonth;

/**
 * Points earned in a single calendar month. {@code month} is serialized as "yyyy-MM".
 */
public record MonthlyRewardResponse(YearMonth month, long points) {
}
