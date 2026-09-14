package com.bng.marketcoordination.integration.vc;

/**
 * Implemented on VC's Merchant Stall block entity (via mixin) to expose a read-only check for
 * whether the stall could currently fulfil its sale — i.e. it has stock and room for payment —
 * without performing (or mutating) an actual purchase. Used so villagers skip empty/stockless
 * stalls at selection time instead of walking to them and failing.
 */
public interface StallStockProbe {
    boolean marketcoord$hasStockForSale();
}
