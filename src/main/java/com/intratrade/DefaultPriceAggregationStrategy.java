package com.intratrade;

import java.math.BigDecimal;
import java.util.Collection;

class DefaultPriceAggregationStrategy implements IPriceAggregationStrategy {
    @Override
    public BigDecimal calculateVwap(Collection<PriceLevel> levels, long targetQuantity) {
        BigDecimal sumProduct = BigDecimal.ZERO;
        long remainingQuantity = targetQuantity;

        for (PriceLevel level : levels) {
            long quantityToUse = Math.min(remainingQuantity, level.quantity());
            if (quantityToUse <= 0) break;

            sumProduct = sumProduct.add(level.price().multiply(BigDecimal.valueOf(quantityToUse)));
            remainingQuantity -= quantityToUse;
        }

        if (remainingQuantity > 0) {
            throw new IllegalArgumentException("Insufficient quantity for VWAP calculation");
        }

        return sumProduct.divide(BigDecimal.valueOf(targetQuantity), 4, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public long calculateTotalQuantity(Collection<PriceLevel> levels, BigDecimal price) {
        return levels.stream()
                .filter(level -> level.price().equals(price))
                .mapToLong(PriceLevel::quantity)
                .sum();
    }
}