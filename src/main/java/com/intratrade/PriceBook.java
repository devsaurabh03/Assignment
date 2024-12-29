package com.intratrade;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class PriceBook implements IPriceBook {
    // Store orders by LP and price for each side
    private final Map<String, Map<BigDecimal, PriceLevel>> lpBids = new HashMap<>();
    private final Map<String, Map<BigDecimal, PriceLevel>> lpOffers = new HashMap<>();

    // Maintain sorted price levels for quick access
    private final NavigableSet<PriceLevel> bids;
    private final NavigableSet<PriceLevel> offers;

    private final IPriceAggregationStrategy aggregationStrategy;

    public PriceBook(IPriceAggregationStrategy aggregationStrategy) {
        this.aggregationStrategy = aggregationStrategy;

        // Bids sorted by price descending
        this.bids = new TreeSet<>(
                Comparator.<PriceLevel, BigDecimal>comparing(PriceLevel::price).reversed()
                        .thenComparing(Comparator.<PriceLevel, Long>comparing(PriceLevel::quantity).reversed())
                        .thenComparing(PriceLevel::source)
        );

        // Offers sorted by price ascending
        this.offers = new TreeSet<>(
                Comparator.<PriceLevel, BigDecimal>comparing(PriceLevel::price)
                        .thenComparing(Comparator.<PriceLevel, Long>comparing(PriceLevel::quantity).reversed())
                        .thenComparing(PriceLevel::source)
        );
    }

    public void update(List<MarketData> messages) {
        if (messages.isEmpty()) return;

        String source = messages.getFirst().source();

        // Group messages by side
        Map<Side, List<MarketData>> ordersBySide = messages.stream()
                .collect(Collectors.groupingBy(MarketData::side));

        // Process BUY orders
        processOrders(source, ordersBySide.getOrDefault(Side.BUY, List.of()), lpBids, bids);

        // Process SELL orders
        processOrders(source, ordersBySide.getOrDefault(Side.SELL, List.of()), lpOffers, offers);
    }

    private void processOrders(String source, List<MarketData> orders,
                               Map<String, Map<BigDecimal, PriceLevel>> lpMap,
                               Set<PriceLevel> levels) {
        // Remove existing orders for this LP
        Optional.ofNullable(lpMap.get(source))
                .ifPresent(existing -> {
                    levels.removeAll(existing.values());
                    if (orders.isEmpty()) {
                        lpMap.remove(source);
                    }
                });

        if (!orders.isEmpty()) {
            // Create new price levels for this LP
            Map<BigDecimal, PriceLevel> newLevels = orders.stream()
                    .map(md -> new PriceLevel(md.source(), md.price(), md.quantity()))
                    .collect(Collectors.toMap(
                            PriceLevel::price,
                            level -> level
                    ));

            // Update LP map and sorted levels
            lpMap.put(source, newLevels);
            levels.addAll(newLevels.values());
        }
    }

    public void reset() {
        lpBids.clear();
        lpOffers.clear();
        bids.clear();
        offers.clear();
    }

    public BigDecimal getVwapForQuantityAndSide(long quantity, Side side) {
        Collection<PriceLevel> levels = side == Side.BUY ? bids : offers;
        return aggregationStrategy.calculateVwap(levels, quantity);
    }

    public long getTotalQuantityForPriceAndSide(BigDecimal price, Side side) {
        Collection<PriceLevel> levels = side == Side.BUY ? bids : offers;
        return aggregationStrategy.calculateTotalQuantity(levels, price);
    }
}