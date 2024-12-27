package com.intratrade;

import java.math.BigDecimal;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        // Initialize system
        Set<String> authorizedLPs = new HashSet<>(Arrays.asList("LP1", "LP2", "LP3"));
        IPriceBook priceBook = new PriceBookProxy(
                new PriceBook(new DefaultPriceAggregationStrategy()),
                authorizedLPs
        );

        // Test Case 1: Basic order update and retrieval
        System.out.println("Test Case 1: Basic Order Processing");
        List<MarketData> lp1Orders = Arrays.asList(
                new MarketData("LP1", "USDINR", Side.BUY, new BigDecimal("82.1500"), 1_000_000L),
                new MarketData("LP1", "USDINR", Side.SELL, new BigDecimal("82.1800"), 5_000_000L)
        );
        priceBook.update(lp1Orders);

        System.out.printf("Total BUY quantity at 82.1500: %d%n",
                priceBook.getTotalQuantityForPriceAndSide(new BigDecimal("82.1500"), Side.BUY));
        System.out.printf("VWAP for SELL 5,000,000: %.4f%n",
                priceBook.getVwapForQuantityAndSide(5_000_000L, Side.SELL));

        // Test Case 2: Multiple LPs at same price
        System.out.println("\nTest Case 2: Multiple LPs at Same Price");
        List<MarketData> lp2Orders = Arrays.asList(
                new MarketData("LP2", "USDINR", Side.BUY, new BigDecimal("82.1500"), 2_000_000L)
        );
        priceBook.update(lp2Orders);

        System.out.printf("Total BUY quantity at 82.1500 after LP2: %d%n",
                priceBook.getTotalQuantityForPriceAndSide(new BigDecimal("82.1500"), Side.BUY));

        // Test Case 3: Order replacement
        System.out.println("\nTest Case 3: Order Replacement");
        List<MarketData> lp1UpdatedOrders = Arrays.asList(
                new MarketData("LP1", "USDINR", Side.BUY, new BigDecimal("82.1600"), 3_000_000L)
        );
        priceBook.update(lp1UpdatedOrders);

        System.out.printf("Total BUY quantity at 82.1500 after LP1 update: %d%n",
                priceBook.getTotalQuantityForPriceAndSide(new BigDecimal("82.1500"), Side.BUY));

        // Test Case 4: Multiple price levels VWAP
        System.out.println("\nTest Case 4: Multi-level VWAP");
        List<MarketData> multiLevelOrders = Arrays.asList(
                new MarketData("LP3", "USDINR", Side.SELL, new BigDecimal("82.1800"), 1_000_000L),
                new MarketData("LP3", "USDINR", Side.SELL, new BigDecimal("82.1900"), 2_000_000L),
                new MarketData("LP3", "USDINR", Side.SELL, new BigDecimal("82.2000"), 3_000_000L)
        );
        priceBook.update(multiLevelOrders);

        System.out.printf("VWAP for SELL 3,000,000 across levels: %.4f%n",
                priceBook.getVwapForQuantityAndSide(3_000_000L, Side.SELL));

        // Test Case 5: Security check
        System.out.println("\nTest Case 5: Unauthorized LP");
        List<MarketData> unauthorizedOrders = Arrays.asList(
                new MarketData("LP4", "USDINR", Side.BUY, new BigDecimal("82.1500"), 1_000_000L)
        );
        try {
            priceBook.update(unauthorizedOrders);
        } catch (SecurityException e) {
            System.out.println("Security check passed: " + e.getMessage());
        }

        // Test Case 6: Reset functionality
        System.out.println("\nTest Case 6: Reset");
        priceBook.reset();
        System.out.printf("Total BUY quantity at 82.1500 after reset: %d%n",
                priceBook.getTotalQuantityForPriceAndSide(new BigDecimal("82.1500"), Side.BUY));

        // Test Case 7: Large order handling
        System.out.println("\nTest Case 7: Large Order");
        priceBook.update(lp1Orders); // Restore some orders
        try {
            priceBook.getVwapForQuantityAndSide(10_000_000L, Side.SELL);
        } catch (IllegalArgumentException e) {
            System.out.println("Large order handling passed: " + e.getMessage());
        }
    }
}