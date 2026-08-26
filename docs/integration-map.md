# Phase 0 — Integration Map

Reverse-engineered from JARs in `libs/` (Aug 2026):

- `createvillagercommerce-1.0.1.jar` — mod ID **`createvillagercommerce`**
- `create-stockmarket-1.1.0+mc1.21.1.jar` — mod ID **`stockmarket`** (Deokma / Create Stock Market — **not** KROIA)

## Villager Commerce (mixins — no public API)

Package: `com.beakbock.createvillagercommerce`

### Ledger / stall discovery (direct compile against JAR)

| Class | Methods used |
|-------|----------------|
| `server.MarketLedgerManager` | `isMarketLedgerBook`, `isValidMarketLedger`, `registerKnownMarketLedger`, `getMarketName`, `getRegisteredStalls`, `getDailyPurchaseLimit`, `canPurchase`, `recordPurchase` |
| `blockentity.MerchantStallBlockEntity` | `getLinkedMarketLedgerPos`, `getSaleItem`, `getCurrencyItem`, `getSaleQuantity`, `getPriceQuantity`, `tryVillagerPurchase` |
| `menu.MarketLedgerMenu.StallInfo` | Stall listing for UI / cache |

### Purchase pipeline (villager → stall)

Flow in `event.CommonEvents.onEntityTick` → `completePurchase` → `MerchantStallBlockEntity.tryVillagerPurchase`, gated by `MarketLedgerManager.canPurchase`, recorded via `MarketLedgerManager.recordPurchase`.

Stall selection also calls `canPurchase` from `findAllowedStallInRegisteredMarket` / `findAllowedStallNearPosition`.

### Our mixins (`integration/vc/mixin/`)

| Target | Injection | Purpose |
|--------|-----------|---------|
| `MarketLedgerManager.canPurchase` | `@Inject` RETURN, cancellable | Spur budget + SM price ceiling via `VcIntegration.allowPurchase` |
| `MarketLedgerManager.getDailyPurchaseLimit` | `@Inject` RETURN | Neutralize VC **count-based** daily cap (`Integer.MAX_VALUE`); spur budget is authoritative |
| `MarketLedgerManager.recordPurchase` | `@Inject` TAIL | Spend budget, record activity window, bind stall cache |

Startup: `VcVersionProbe` logs if expected methods are missing.

## Stock Market — Deokma registry read (no plugin API)

Package: `by.deokma.stockmarket.market`

| Class | Methods |
|-------|---------|
| `MarketData` | `get()` — current snapshot list |
| `MarketRegistry` | `build(server)` / static `build` — refresh entries |
| `MarketEntry` | `itemId()`, `minPrice()`, `avgPrice()`, `displayStack()` |

`StockMarketAdapter.referenceSpurs(itemId)` uses `avgPrice` (fallback `minPrice`). Values are treated as **Numismatics spurs** when Numismatics is present on the pack.

No KROIA `ServerPlugin` in this JAR — accessor/registry read only.

## Numismatics (hard dependency — mod API)

- [x] `Coin` / `CoinItem` spur conversion — `NumismaticsAdapter`
- [ ] CoinBag parsing for multi-coin prices (Phase 1)

## Version pins

See `gradle.properties` — Create 6.0.10-280, Numismatics 1.0.20, NeoForge 21.1.248.
