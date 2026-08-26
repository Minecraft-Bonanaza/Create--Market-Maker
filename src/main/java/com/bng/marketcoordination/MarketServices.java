package com.bng.marketcoordination;

import com.bng.marketcoordination.economy.ObservedPriceIndex;
import com.bng.marketcoordination.purchasing.AntiAbuseService;
import com.bng.marketcoordination.purchasing.CategoryBudgetService;
import com.bng.marketcoordination.purchasing.CommodityCapService;
import com.bng.marketcoordination.purchasing.PriceCeilingService;
import com.bng.marketcoordination.purchasing.ProcurementBudgetService;
import com.bng.marketcoordination.purchasing.SellerSelectionService;

public final class MarketServices {
    public static final ProcurementBudgetService BUDGET = new ProcurementBudgetService();
    public static final CommodityCapService COMMODITY_CAPS = new CommodityCapService();
    public static final PriceCeilingService CEILING = new PriceCeilingService();
    public static final SellerSelectionService SELLER_SELECTION = new SellerSelectionService();
    public static final AntiAbuseService ANTI_ABUSE = new AntiAbuseService();
    public static final CategoryBudgetService CATEGORY_BUDGET = new CategoryBudgetService();
    public static final ObservedPriceIndex OBSERVED_PRICES = new ObservedPriceIndex();

    private MarketServices() {}
}
