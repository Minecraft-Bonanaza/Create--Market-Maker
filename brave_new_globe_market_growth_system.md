# Brave New Globe Market Growth System

## 1. Purpose

This system coordinates **Villager Commerce** with the broader player economy to create markets that:

- are player-founded but server-governed
- grow through actual economic activity
- reward players for joining established markets
- create increasing NPC demand as trade volume grows
- avoid player-controlled money printing
- encourage towns, cities, trade hubs, and eventually nations
- remain grounded in believable economic behavior

The market should feel less like a configurable vending machine and more like a **local economy that develops because people actually use it**.

Core principle:

> Players decide where commerce happens.  
> The server decides how large that economy is allowed to become.

---

## 2. Design Goals

The system should encourage four player behaviors:

1. **Found settlements**
2. **Concentrate commerce into shared markets**
3. **Increase production and trade volume**
4. **Build infrastructure connecting successful markets**

A successful market should become materially more useful over time.

A player should look at an established town and think:

> “Selling there is worth the trip.”

rather than:

> “I’ll just put another Market Ledger beside my base.”

The system should therefore create a strong **network effect**.

---

## 3. Core Market Model

Each Villager Commerce **Market Ledger** represents one local economy.

The Ledger tracks:

- Market identity
- Registered villagers
- Registered sellers
- Associated stalls
- Trade history
- Commodity diversity
- Recent transaction volume
- Market maturity
- Current procurement budget

Players may create and join markets.

Players may **not** directly control:

- Daily NPC spending budget
- Commodity demand
- Purchase limits
- Growth rate
- Market tier
- Price ceilings
- Population multipliers

Those values are entirely server-controlled.

---

## 4. Player Controls

Players should only control elements that make sense socially or physically.

### Allowed

- Market name
- Market location
- Stall membership
- Shop inventory
- Shop asking prices
- Market signage / decoration

Potentially:

- Market ownership or administration
- Who may attach stalls
- Nation affiliation

### Not Allowed

- NPC spending budget
- Daily purchase quota
- Market tier
- Demand modifiers
- Growth score
- Currency issuance

The economic rules belong to the system.

---

## 5. Market Creation

A player may establish a new market using the normal Villager Commerce Market Ledger system.

New markets begin deliberately weak.

```text
New Market

Tier: Outpost
Daily NPC Budget: 20 Cogs
Registered Villagers: 4
Active Sellers: 1
Commodity Diversity: 2
```

Creating a market should be easy.

Creating a **good** market should be difficult.

This avoids requiring administrator intervention while still preventing every player from creating a personal high-liquidity money fountain.

---

## 6. Why Join an Existing Market?

Established markets must offer significantly greater economic opportunity than new ones.

```text
Private roadside market
Daily NPC budget: 20 Cogs
Few villagers
Few customers
Low commodity diversity
```

versus:

```text
Established town market
Daily NPC budget: 180 Cogs
30 villagers
8 sellers
15 commodities
Established trade history
```

A player joining the larger market gains access to much deeper demand.

Their participation also contributes to further market growth.

```text
More sellers
    ↓
More goods
    ↓
More trade volume
    ↓
Market grows
    ↓
More NPC demand
    ↓
Market becomes more attractive
    ↓
More sellers join
```

This is the desired network effect.

---

## 7. Market Growth

Markets grow through **measured economic activity**, not manual upgrades.

Each market has a **Market Activity Score**.

Recommended components:

```text
40% recent transaction value
25% unique active sellers
20% registered villager population
15% commodity diversity
```

These weights should be configurable.

Conceptually:

```text
Activity Score =
Trade Volume Score
+ Seller Diversity Score
+ Population Score
+ Commodity Diversity Score
```

The exact implementation may normalize each component independently.

---

## 8. Rolling Activity Window

Growth should not be based on a single day's activity.

Use a rolling period such as:

```text
7 Minecraft days
```

Evaluate:

- Total goods purchased
- Total currency spent
- Unique sellers
- Unique commodities
- Registered villagers

This prevents one giant sale from instantly creating a major city and rewards sustained commerce.

---

## 9. Market Tiers

Markets may expose visible tiers for readability.

```text
Tier 0 - Outpost
Tier 1 - Local Market
Tier 2 - Town Market
Tier 3 - Regional Market
Tier 4 - Major Exchange
```

Illustrative budgets:

| Tier | Daily NPC Budget |
|---|---:|
| Outpost | 20 Cogs |
| Local Market | 50 Cogs |
| Town Market | 100 Cogs |
| Regional Market | 200 Cogs |
| Major Exchange | 400 Cogs |

These values are illustrative and configurable.

Tier advancement should depend on activity score, not a manual upgrade button.

---

## 10. Continuous Growth Option

Internally, market budget should preferably be continuous rather than strictly tier-based.

```text
Activity Score 0     → 20 Cogs/day
Activity Score 25    → 40 Cogs/day
Activity Score 50    → 85 Cogs/day
Activity Score 75    → 170 Cogs/day
Activity Score 100   → 300 Cogs/day
```

Visible tiers can describe ranges while the actual budget changes smoothly.

---

## 11. Market Decline

Markets should shrink if abandoned.

Use activity decay based on the rolling activity window.

A market that stops trading gradually loses:

- Activity score
- Daily budget
- Market tier

Population may slow this decay, but should not prevent it entirely.

> Active towns remain economically important.  
> Dead towns become economically quiet.

---

## 12. Daily Procurement Budget

The central monetary-control mechanism is a **fixed daily spending budget**.

```text
Redwater Market
Daily NPC Procurement Budget: 120 Cogs
```

Villagers collectively cannot create more than 120 Cogs through market purchases that day.

```text
Maximum newly issued currency = 120 Cogs/day
```

This gives the server a hard ceiling on NPC currency issuance regardless of commodity prices.

---

## 13. Category Budgets

The total budget may optionally be divided into economic categories.

```text
Total Budget: 120 Cogs

Food:        35
Fuel:        20
Materials:   30
Textiles:    15
Tools:       20
```

This prevents one commodity from consuming the entire economy.

Category allocation is server-controlled.

---

## 14. Commodity Caps

Budgets control **money creation**.

Commodity caps control **believable consumption**.

```text
Bread: max 64/day
Iron:  max 32/day
Coal:  max 64/day
Wheat: max 128/day
```

Even if a commodity becomes extremely cheap, villagers should not buy absurd quantities simply because their money budget stretches farther.

---

## 15. Purchase Selection

Villagers should primarily purchase from the **lowest-priced eligible sellers**.

```text
Find all valid stalls
        ↓
Reject prices above willingness-to-pay ceiling
        ↓
Find cheapest current offer
        ↓
Determine competitive price band
        ↓
Purchase from qualifying sellers
        ↓
Stop when:
budget exhausted
OR commodity cap reached
OR supply exhausted
```

---

## 16. Competitive Price Band

Do not use pure winner-take-all cheapest pricing.

Example:

```text
Lowest price: 1.00 Cog
Competitive band: +5%
```

Eligible:

```text
Alice  1.00
Bob    1.02
Carol  1.04
```

Not eligible:

```text
Dave   1.10
```

Purchases are distributed across qualifying sellers.

Cheaper sellers may receive slightly greater weighting, but tiny undercuts should not capture the entire market.

---

## 17. Natural Price Elasticity

No artificial demand equation is required.

A fixed money budget naturally creates price-sensitive quantity demand.

```text
Iron Budget: 64 Cogs/day

1 Cog/iron → up to 64 iron
2 Cogs/iron → up to 32 iron
4 Cogs/iron → up to 16 iron
```

> Higher prices naturally reduce quantity purchased.

---

## 18. Maximum Willingness to Pay

Markets need protection against monopoly pricing.

If the only seller lists iron at 999 Cogs each, the village should not spend its entire daily budget on one ingot.

Each commodity therefore requires a **maximum acceptable price**.

This may use:

- a server baseline price, or
- `Stock Market reference price × configured multiplier`

Example:

```text
Market reference: 10 Spur
Maximum village price: 12.5 Spur
```

Anything above the ceiling is ignored.

---

## 19. Stock Market Integration

Create: Stock Market should remain the **player price-discovery layer**.

It should not dictate exact NPC prices.

Its roles are:

- Show current player-market prices
- Show trends
- Show historical prices
- Provide a reference for maximum NPC willingness-to-pay
- Help players determine competitive asking prices

Example:

```text
IRON

Player Market Average: 1.4 Cogs
Player Market Low:     1.2 Cogs
Trend:                 +8%
```

Meanwhile the local market may report:

```text
REDWATER IRON PROCUREMENT

Budget Remaining:      42 Cogs
Lowest Offer:           1.1 Cogs
Acceptable Ceiling:     1.6 Cogs
Purchased Today:       18
```

The two systems complement one another.

---

## 20. Villager Population

Registered villager population should influence market growth, but should not directly create unlimited currency.

Population contributes to:

- Activity score
- Maximum market maturity
- Potential daily budget
- Commodity demand

Suggested nonlinear scaling:

```text
Population contribution = sqrt(registered villagers)
```

or another diminishing-return function.

This prevents breeding hundreds of villagers purely to generate money.

---

## 21. Seller Diversity

Unique sellers should matter significantly.

```text
Market A:
1 seller
3 commodities
500 Cogs weekly volume

Market B:
7 sellers
12 commodities
500 Cogs weekly volume
```

Market B should receive a higher maturity score.

This directly encourages cooperation and gives players a reason to join existing markets.

---

## 22. Commodity Diversity

Markets should benefit from supporting multiple types of commerce.

A market trading bread, coal, iron, tools, textiles, timber, and medicine should score better than one whose entire economy is wheat.

Use diminishing returns and preferably score **recognized commodity categories**, not raw unique item count.

---

## 23. Anti-Abuse Rules

At minimum:

- Same-player-controlled shops must not count as multiple sellers.
- Repeated trivial trades should have diminishing growth value.
- Market budget growth must be capped per day.
- Population contribution must have diminishing returns.
- Market activity must decay.
- Markets should have minimum spacing.
- Listings alone do not count toward growth.
- Only completed economic activity should meaningfully increase maturity.

A market grows because commerce happens, not because shelves exist.

---

## 24. Market Spacing

Markets should not be created every few dozen blocks.

Suggested configurable minimum separation:

```text
500–1000 blocks
```

Exact value depends on Big Globe geography and server population.

Players can still build shops anywhere, but economically significant markets become geographic anchors.

---

## 25. Market Consolidation Incentive

Fragmentation should be inefficient.

Six independent starter markets might each have only 20 Cogs/day of shallow demand, while a combined mature market could reach 150 Cogs/day because it has:

- more sellers
- more villagers
- more commodities
- more transaction volume

The system does not forbid independent settlements. It rewards urban concentration.

---

## 26. Regional Economic Identity

Future versions may allow markets to develop specialization based on actual trade history.

```text
Redwater:
60% food trade
→ agricultural market identity

Ironhaven:
55% metals/tools/fuel
→ industrial identity

Southport:
high manufactured goods + food
→ commercial port identity
```

This should be inferred from actual trade rather than manually selected by players.

Market identity may later influence procurement category weighting.

---

## 27. Nation-Building Integration

This system should provide the economic foundation for nations without requiring a full political system.

Multiple markets naturally create structures such as:

```text
Capital Market
Industrial Town
Agricultural Town
Port
Frontier Settlement
```

Infrastructure connects them.

A nation gains power because it controls a network of:

- population
- markets
- production
- transport infrastructure
- trade

rather than because it owns arbitrary claimed chunks.

---

## 28. Example Market Lifecycle

### Day 1

```text
Northbridge Market

4 villagers
1 seller
2 commodities

Budget:
20 Cogs/day
```

### Day 7

```text
8 villagers
3 sellers
6 commodities
steady transactions

Budget:
45 Cogs/day
```

### Day 20

```text
21 villagers
6 sellers
11 commodities
high weekly trade volume

Budget:
110 Cogs/day
```

### Day 40

```text
38 villagers
10 sellers
multiple industries
major trade throughput

Budget:
220 Cogs/day
```

Players now have strong reasons to build housing, bring villagers, open shops, construct rail links, haul commodities, and protect the town.

---

## 29. Example Decline

Suppose Northbridge loses its railway connection and players move elsewhere.

```text
Volume ↓
Active sellers ↓
Commodity diversity ↓
```

Market maturity follows:

```text
220 Cogs/day
→ 190
→ 155
→ 120
→ 90
```

The settlement remains usable, but no longer generates metropolitan levels of demand merely because it was once successful.

---

## 30. Server Configuration

Suggested configuration:

```toml
[market]
activity_window_days = 7
minimum_market_spacing = 750
starting_daily_budget = 20
maximum_daily_budget = 400

[growth]
trade_volume_weight = 0.40
unique_seller_weight = 0.25
population_weight = 0.20
commodity_diversity_weight = 0.15
maximum_daily_growth = 0.08
activity_decay_rate = 0.05

[purchasing]
competitive_price_band = 0.05
use_stock_market_ceiling = true
maximum_price_multiplier = 1.25

[population]
minimum_registered_villagers = 3
population_scaling = true
population_soft_cap = 40

[anti_abuse]
count_unique_player_uuid = true
ignore_listing_volume = true
completed_transactions_only = true
```

---

## 31. Market State Persistence

Each Market Ledger needs persistent server-side data such as:

- Market UUID
- Market name
- Location
- Creation date
- Current activity score
- Current maturity tier
- Daily procurement budget
- Today's spending
- Historical volume
- Rolling seller count
- Rolling commodity diversity
- Registered villager history

This data must survive restarts.

The physical Ledger identifies the market, while authoritative economic state should live in server-side saved data.

---

## 32. UI

Players should be able to inspect market health without being able to modify it.

```text
NORTHBRIDGE MARKET

Status: Town Market
Registered Villagers: 24
Active Sellers: 6

Daily Procurement:
120 / 150 Cogs remaining

7-Day Trade Volume:
684 Cogs

Commodity Diversity:
11 categories

Market Activity:
72%

Growth Trend:
Rising
```

Players may inspect budgets, remaining demand, growth, and market history, but not edit them.

---

## 33. Admin Controls

Suggested commands:

```text
/marketcoord info <market>
/marketcoord list
/marketcoord activity <market>
/marketcoord reset <market>
/marketcoord setbudget <market> <amount>
/marketcoord debug <market>
```

Manual budget modification exists for administration but is not normal gameplay.

---

## 34. Currency Issuance Monitoring

Because Villager Commerce purchases create currency, track issuance.

Per market:

- Currency issued today
- 7-day issuance
- Lifetime issuance
- Issuance by commodity

Global:

- All-market currency issuance
- Bounty issuance
- NPC currency sinks
- Estimated net monetary growth

This provides the server operator with tools to identify excessive inflation.

---

## 35. Suggested Internal Architecture

```text
MarketCoordinationMod
│
├── integration/
│   ├── VillagerCommerceAdapter
│   ├── StockMarketAdapter
│   └── NumismaticsAdapter
│
├── market/
│   ├── MarketState
│   ├── MarketRegistry
│   ├── MarketGrowthService
│   ├── MarketTier
│   └── RollingActivityWindow
│
├── purchasing/
│   ├── ProcurementBudgetService
│   ├── CommodityCapService
│   ├── SellerSelectionService
│   └── PriceCeilingService
│
├── economy/
│   ├── CurrencyValue
│   ├── IssuanceTracker
│   └── CommodityCategory
│
├── config/
│   ├── MarketConfig
│   ├── GrowthConfig
│   ├── PurchasingConfig
│   └── CommodityConfig
│
├── command/
│   └── MarketCoordinationCommands
│
└── ui/
    └── MarketInfoProvider
```

Important architectural rule:

> Adapters know about external mods. Core market logic does not.

This keeps the system maintainable if Villager Commerce or Stock Market changes APIs.

---

## 36. Suggested Daily Market Cycle

At each Minecraft-day reset:

```text
1. Finalize previous day's transaction statistics.
2. Update rolling activity history.
3. Calculate market activity score.
4. Apply growth or decay.
5. Calculate today's procurement budget.
6. Reset daily spending.
7. Reset commodity/category caps.
8. Refresh Stock Market reference prices.
9. Calculate maximum willingness-to-pay ceilings.
10. Begin new trading day.
```

Do not recalculate all economic state every tick.

---

## 37. Suggested Purchase Algorithm

For each potential Villager Commerce purchase:

```text
1. Identify the Market Ledger.
2. Identify the requested commodity.
3. Check remaining total/category budget.
4. Check remaining commodity cap.
5. Discover valid player stalls.
6. Reject offers above the price ceiling.
7. Determine lowest valid offer.
8. Build competitive price band.
9. Select an eligible seller.
10. Execute the normal Villager Commerce purchase.
11. Deduct actual price from the market budget.
12. Increment commodity quantity purchased.
13. Record seller participation and transaction volume.
14. Record currency issuance.
```

Pseudo-code:

```java
MarketState market = markets.get(ledger);

long remainingBudget = market.getRemainingBudget();
if (remainingBudget <= 0) {
    return NO_PURCHASE;
}

CommodityState commodity = market.getCommodity(item);

if (commodity.getPurchasedToday() >= commodity.getDailyCap()) {
    return NO_PURCHASE;
}

List<Offer> offers = commerce.findOffers(market, item);

long ceiling = pricing.getMaximumAcceptedPrice(market, item);
offers.removeIf(offer -> offer.price() > ceiling);

if (offers.isEmpty()) {
    return NO_PURCHASE;
}

long lowestPrice = offers.stream()
    .mapToLong(Offer::price)
    .min()
    .orElseThrow();

double band = config.competitivePriceBand();

List<Offer> competitive = offers.stream()
    .filter(offer -> offer.price() <= lowestPrice * (1.0 + band))
    .toList();

Offer selected = sellerSelection.choose(competitive);

if (selected.price() > remainingBudget) {
    return NO_PURCHASE;
}

commerce.executePurchase(selected);

market.spend(selected.price());
commodity.recordPurchase(1);

activity.recordTransaction(
    selected.sellerUuid(),
    item,
    selected.price()
);

issuance.record(market, item, selected.price());
```

Use integer currency units internally.

---

## 38. MVP Scope

### v0.1

Implement:

- Market-specific daily money budget
- Lowest-price purchasing
- Competitive price band
- Commodity caps
- Server-owned parameters
- Rolling trade-volume tracking
- Simple market growth
- Simple market decay
- Persistent market state
- Basic player information UI
- Basic admin diagnostics

### v0.2

Add:

- Unique seller scoring
- Registered population scoring
- Commodity diversity scoring
- Visible market tiers
- Stock Market price-ceiling integration
- Better anti-abuse logic

### v0.3

Add:

- Category budgets
- Regional specialization
- Currency issuance statistics
- Advanced market UI
- Historical market data

### v0.4

Potential nation-level integration:

- Linked markets
- National trade statistics
- National procurement
- Public works funding
- Military procurement
- Nation affiliation metadata

---

## 39. Explicit Non-Goals

The system should not introduce:

- Teleportation
- Global item delivery
- Virtual inventories
- Infinite NPC shops
- Player-configurable NPC budgets
- Passive money generation
- Automatic resource generation
- Global auction delivery
- Magical market bonuses
- Land-value buffs
- Arbitrary chunk ownership rewards

Physical logistics must continue to matter.

---

## 40. Implementation Priorities for Cursor

Before implementing the full system, investigate the integration surface of the required mods.

### First Questions to Resolve

1. How does Villager Commerce identify Market Ledgers internally?
2. How are registered villagers associated with a ledger?
3. How are Merchant Stalls associated with a ledger?
4. What event or method executes a villager purchase?
5. Can purchase eligibility or seller selection be intercepted cleanly?
6. Is there an existing event for completed purchases?
7. Does Villager Commerce expose player UUID ownership for stalls?
8. Where are daily purchase limits currently stored?
9. Can those limits be overridden without directly modifying Villager Commerce?
10. What price/transaction data does Create: Stock Market expose?
11. Can Stock Market provide current low/average/reference prices by item?
12. How does Numismatics represent denominations internally?
13. What is the safest way to persist per-market custom data in NeoForge 1.21.1?

### Integration Preference

Use, in order of preference:

1. Public API
2. NeoForge events
3. Capabilities/interfaces
4. Accessors/reflection against stable public classes
5. Mixins only when necessary

Avoid copying or redistributing Villager Commerce implementation code.

---

# Core Rule

The entire design can be summarized as:

> **Markets become powerful because people use them.**

Players create markets.

Players bring villagers.

Players sell goods.

Players compete on price.

More activity produces greater market maturity.

Greater maturity produces larger but controlled NPC purchasing budgets.

Larger budgets attract more sellers.

Established markets therefore become towns and cities because **economic concentration has tangible benefits**.

This system should become the connective tissue between production, transportation, settlement growth, trade, and nation-building in **Brave New Globe**.
