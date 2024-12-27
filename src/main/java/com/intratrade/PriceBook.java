package com.intratrade;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

class PriceBook implements IPriceBook {
    private final Map<String, Map<BigDecimal, PriceLevel>> lpBids = new HashMap<>();
    private final Map<String, Map<BigDecimal, PriceLevel>> lpOffers = new HashMap<>();
    private final NavigableSet<PriceLevel> bids;
    private final NavigableSet<PriceLevel> offers;
    private final IPriceAggregationStrategy aggregationStrategy;

    public PriceBook(IPriceAggregationStrategy aggregationStrategy) {
        this.aggregationStrategy = aggregationStrategy;

        // comparator for bids (highest to lowest price)
        this.bids = new TreeSet<>(
                Comparator.<PriceLevel, BigDecimal>comparing(PriceLevel::price).reversed()
                        .thenComparing(Comparator.<PriceLevel, Long>comparing(PriceLevel::quantity).reversed())
                        .thenComparing(PriceLevel::source)
        );

        //  comparator for offers (lowest to highest price)
        this.offers = new TreeSet<>(
                Comparator.<PriceLevel, BigDecimal>comparing(PriceLevel::price)
                        .thenComparing(Comparator.<PriceLevel, Long>comparing(PriceLevel::quantity).reversed())
                        .thenComparing(PriceLevel::source)
        );
    }

    @Override
    public void reset() {
        lpBids.clear();
        lpOffers.clear();
        bids.clear();
        offers.clear();
    }

    @Override
    public void update(List<MarketData> messages) {
        if (messages.isEmpty()) return;

        var source = messages.getFirst().source();
        updateExistingOrders(source);
        processNewOrders(messages, source);
    }

    private void updateExistingOrders(String source) {
        Optional.ofNullable(lpBids.get(source)).ifPresent(existing -> existing.values().forEach(bids::remove));
        Optional.ofNullable(lpOffers.get(source)).ifPresent(existing -> existing.values().forEach(offers::remove));
    }

    private void processNewOrders(List<MarketData> messages, String source) {
        var bidOfferPair = messages.stream()
                .collect(Collectors.partitioningBy(md -> md.side() == Side.BUY,
                        Collectors.toMap(
                                MarketData::price,
                                md -> new PriceLevel(md.source(), md.price(), md.quantity()),
                                (existing, replacement) -> replacement
                        )));

        updateSide(source, bidOfferPair.get(true), lpBids, bids);
        updateSide(source, bidOfferPair.get(false), lpOffers, offers);
    }

    private void updateSide(String source, Map<BigDecimal, PriceLevel> newLevels,
                            Map<String, Map<BigDecimal, PriceLevel>> lpMap,
                            Set<PriceLevel> levels) {
        if (newLevels.isEmpty()) {
            lpMap.remove(source);
        } else {
            lpMap.put(source, newLevels);
            levels.addAll(newLevels.values());
        }
    }

    @Override
    public BigDecimal getVwapForQuantityAndSide(long quantity, Side side) {
        return aggregationStrategy.calculateVwap(side == Side.BUY ? bids : offers, quantity);
    }

    @Override
    public long getTotalQuantityForPriceAndSide(BigDecimal price, Side side) {
        return aggregationStrategy.calculateTotalQuantity(side == Side.BUY ? bids : offers, price);
    }
}
