package com.bng.marketcoordination.integration;

import dev.ithundxr.createnumismatics.content.backend.Coin;
import dev.ithundxr.createnumismatics.content.coins.CoinItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class NumismaticsAdapter {
    private NumismaticsAdapter() {}

    public static long toSpurs(Coin coin, int count) {
        return coin.toSpurs(count);
    }

    public static long toSpurs(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        if (stack.getItem() instanceof CoinItem coinItem) {
            return coinItem.coin.toSpurs(stack.getCount());
        }
        return 0L;
    }

    public static long coinValueSpurs(Coin coin) {
        return coin.value;
    }

    public static List<Map.Entry<Coin, Integer>> fromSpurs(long spurs) {
        int remaining = (int) Math.min(spurs, Integer.MAX_VALUE);
        List<Map.Entry<Coin, Integer>> breakdown = new ArrayList<>();
        for (Coin coin : Arrays.stream(Coin.values()).sorted(Comparator.comparingInt(c -> -c.value)).toList()) {
            var converted = coin.convert(remaining);
            int amount = converted.getFirst();
            remaining = converted.getSecond();
            if (amount > 0) {
                breakdown.add(Map.entry(coin, amount));
            }
        }
        return breakdown;
    }

    public static String formatSpurs(long spurs) {
        if (spurs <= 0L) {
            return "0 spurs";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Coin, Integer> entry : fromSpurs(spurs)) {
            int amount = entry.getValue();
            if (amount <= 0) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(amount).append(' ').append(entry.getKey().getName(amount));
        }
        return builder.isEmpty() ? spurs + " spurs" : builder.toString();
    }
}
