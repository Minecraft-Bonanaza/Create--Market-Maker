package com.bng.marketcoordination.purchasing;

import com.bng.marketcoordination.MarketServices;
import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.economy.CommodityCategory;
import com.bng.marketcoordination.market.MarketState;
import net.minecraft.world.item.Item;

/**
 * Models diminishing marginal demand for a commodity as its daily quota fills.
 *
 * <p>The market has a fixed willingness-to-pay per unit that starts at
 * {@code maximum_price_multiplier} of the reference price when a commodity's quota is empty and
 * slides down toward {@code demand_floor_multiplier} as that quota approaches full. Because stalls
 * sell at a fixed price, a falling willingness-to-pay means the market effectively "expects more
 * quantity per unit of money" the more it has already bought: pricier stalls fall below the ceiling
 * first, only the cheapest (most quantity per money) offers keep selling near saturation, and once a
 * quota is full nothing qualifies. Saturation is taken as the larger of the category money-quota fill
 * and the per-item unit-cap fill, so either running low pulls demand down.
 */
public final class DemandCurveService {

    /**
     * Saturation (0..1) for a commodity, i.e. how close its daily quota is to full. Uses the larger
     * of the category money-quota fill and the per-item unit-cap fill.
     */
    public double saturation(MarketState market, CommodityCategory category, Item item) {
        double categoryFill = MarketServices.CATEGORY_BUDGET.categoryFillFraction(market, category);
        double itemFill = MarketServices.COMMODITY_CAPS.capFillFraction(market.id(), item);
        return Math.max(categoryFill, itemFill);
    }

    /**
     * Willingness-to-pay expressed as a multiple of the reference price at the given saturation.
     * Interpolates from {@code maximum_price_multiplier} (empty quota) down to
     * {@code demand_floor_multiplier} (full quota) along {@code demand_curve_exponent}.
     */
    public double priceMultiplier(double saturation) {
        double max = MarketCoordConfig.PURCHASING.maximumPriceMultiplier.get();
        if (!MarketCoordConfig.PURCHASING.demandCurveEnabled.get()) {
            return max;
        }
        double floor = Math.min(MarketCoordConfig.PURCHASING.demandFloorMultiplier.get(), max);
        double exponent = MarketCoordConfig.PURCHASING.demandCurveExponent.get();
        double s = Math.max(0.0, Math.min(1.0, saturation));
        double remainingDemand = Math.pow(1.0 - s, exponent); // 1 when empty, 0 when full
        return floor + (max - floor) * remainingDemand;
    }

    /**
     * Demand-adjusted price ceiling in spurs for a commodity at the given saturation. Offers priced
     * above this are declined by the market.
     */
    public long ceilingSpurs(long referenceSpurs, double saturation) {
        return Math.round(referenceSpurs * priceMultiplier(saturation));
    }
}
