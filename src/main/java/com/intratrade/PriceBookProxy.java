package com.intratrade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

class PriceBookProxy implements IPriceBook {
    private final IPriceBook priceBook;
    private final Set<String> authorizedSources;

    public PriceBookProxy(IPriceBook priceBook, Set<String> authorizedSources) {
        this.priceBook = priceBook;
        this.authorizedSources = authorizedSources;
    }

    @Override
    public void update(List<MarketData> messages) {
        if (messages.isEmpty()) return;

        var source = messages.getFirst().source();
        if (!authorizedSources.contains(source)) {
            throw new SecurityException("Unauthorized source: " + source);
        }

        logOperation("update", "Processing " + messages.size() + " orders from " + source);
        priceBook.update(messages);
    }

    @Override
    public void reset() {
        logOperation("reset", "Resetting price book");
        priceBook.reset();
    }

    @Override
    public BigDecimal getVwapForQuantityAndSide(long quantity, Side side) {
        logOperation("getVwap", "Calculating VWAP for " + quantity + " on " + side);
        return priceBook.getVwapForQuantityAndSide(quantity, side);
    }

    @Override
    public long getTotalQuantityForPriceAndSide(BigDecimal price, Side side) {
        logOperation("getTotalQuantity", "Calculating total quantity at " + price + " on " + side);
        return priceBook.getTotalQuantityForPriceAndSide(price, side);
    }

    private void logOperation(String operation, String details) {
        System.out.printf("[%s] %s: %s%n",
                java.time.LocalDateTime.now(), operation, details);
    }
}