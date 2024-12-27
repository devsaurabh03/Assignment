package com.intratrade;

import java.math.BigDecimal;
import java.util.Collection;

class DefaultPriceAggregationStrategy implements IPriceAggregationStrategy {
    @Override
    public BigDecimal calculateVwap(Collection<PriceLevel> levels, long targetQuantity) {
        record VwapAccumulator(BigDecimal sumProduct, long remainingQuantity) {}

        var result = levels.stream()
                .reduce(
                        new VwapAccumulator(BigDecimal.ZERO, targetQuantity),
                        (acc, level) -> {
                            if (acc.remainingQuantity <= 0) return acc;
                            var quantityToUse = Math.min(acc.remainingQuantity, level.quantity());
                            return new VwapAccumulator(
                                    acc.sumProduct.add(level.price().multiply(BigDecimal.valueOf(quantityToUse))),
                                    acc.remainingQuantity - quantityToUse
                            );
                        },
                        (a, b) -> a
                );

        if (result.remainingQuantity > 0) {
            throw new IllegalArgumentException("Insufficient quantity for VWAP calculation");
        }

        return result.sumProduct.divide(BigDecimal.valueOf(targetQuantity), 4, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public long calculateTotalQuantity(Collection<PriceLevel> levels, BigDecimal price) {
        return levels.stream()
                .filter(level -> level.price().equals(price))
                .mapToLong(PriceLevel::quantity)
                .sum();
    }
}