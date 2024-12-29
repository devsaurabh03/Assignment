package com.intratrade;

import java.math.BigDecimal;
import java.util.Collection;

public interface IPriceAggregationStrategy {
    BigDecimal calculateVwap(Collection<PriceLevel> levels, long targetQuantity);
    long calculateTotalQuantity(Collection<PriceLevel> levels, BigDecimal price);
}