# Market Coordination (Create: Market Maker)

NeoForge 1.21.1 mod for the **Brave New Globe** pack. Layers server-authoritative market budgets, growth, and decay on top of **Create: Villager Commerce**, with **Create: Numismatics** currency and optional **Stock Market** price references.

**Version:** `0.4.0-dev`

## Docs

- [Design spec](brave_new_globe_market_growth_system.md)
- [Build plan](BUILD_PLAN.md)
- [Integration map](docs/integration-map.md)
- [Performance baseline](docs/performance.md)

## Build

Requirements: Java 21, Gradle wrapper included.

```powershell
.\gradlew.bat build
```

Output: `build/libs/marketcoordination-0.4.0-dev.jar`

Optional: drop **Villager Commerce** and **Stock Market** JARs into `libs/` for compile-time mixin/API work (see `libs/README.txt`).

## Dev run

```powershell
.\gradlew.bat runServer
```

## Commands (op level 2)

- `/marketcoord info [market]` — summary for one market or count
- `/marketcoord list` — registered markets (with nation link if set)
- `/marketcoord activity` — activity scores, volume, sellers
- `/marketcoord reset` — reset daily spend + commodity/category caps
- `/marketcoord setbudget <cogs>` — override daily budget (all markets)
- `/marketcoord debug` — budget, issuance, specialization per market
- `/marketcoord nation list` — linked nations
- `/marketcoord nation info <name>` — aggregate nation statistics
- `/marketcoord nation create <name>` — create a nation metadata record
- `/marketcoord nation link <nation> <market> [capital]` — link market to nation

## In-game UI

- **Right-click** a Market Ledger lectern — registers the market (spacing rules apply)
- **Shift + right-click** — read-only Market Summary: tier, nation, specialization, category budgets, 7-day volume history, growth trend, issuance

## Status (v0.4)

| Phase | Features |
|-------|----------|
| **v0.1** | VC purchase mixins, budget/caps/ceiling, competitive stall selection, growth/decay, SavedData, basic UI, admin commands |
| **v0.2** | Full activity score (volume, sellers, population, category diversity), SM price cache at day boundary, observed-price fallback, anti-abuse diminishing returns, minimum market spacing |
| **v0.3** | Category budgets, regional specialization bias, issuance tracking, extended summary UI with category lines and 7-day history |
| **v0.4** | Nation registry, cross-market statistics, `/marketcoord nation` commands, procurement pool metadata stubs (no virtual delivery) |

Config sections: `[market]`, `[growth]`, `[purchasing]`, `[population]`, `[anti_abuse]`, `[categories]`, `[performance]`.

Data files: `commodity_caps.json`, `commodity_categories.json`, `category_budget_weights.json`.
