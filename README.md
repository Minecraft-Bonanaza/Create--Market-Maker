# Create: Market Maker

A **NeoForge 1.21.1** mod that turns [Create: Villager Commerce](https://www.curseforge.com/minecraft/mc-mods/create-villager-commerce) stalls into a living, server-authoritative economy. Villagers shop with real budgets, prices respond to demand, markets grow and decay based on activity, and everything is priced in [Create: Numismatics](https://modrinth.com/mod/numismatics) currency. When [Create: Stock Market](https://modrinth.com/mod/create-stock-market) is present, stalls are indexed into a global price board with a per-player trade-volume graph.

**Version:** `0.5.0` · **Minecraft:** 1.21.1 · **Loader:** NeoForge 21.1.x · **License:** LGPL-3.0

> Built for the **Brave New Globe** pack, but it works in any 1.21.1 NeoForge pack that includes the required dependencies below.

---

## Features

- **Budget-driven villager shopping** — each market has a daily spending budget; villagers draw from it and from a per-villager daily cap, so more villagers = faster throughput.
- **Competitive stall selection** — villagers compare offers across stalls per commodity, buy from the cheapest viable seller, and skip stalls that are out of stock or unaffordable *before* walking to them.
- **Demand curve** — the acceptable price ceiling falls as a commodity/category fills its daily quota, so you get more goods per Spur early and diminishing willingness to pay as demand saturates.
- **Growth & decay** — markets grow when daily budget utilization is high and decay slowly when idle, with a decay floor so players aren't punished for logging off.
- **Category budgets & commodity caps** — per-market, per-category spend limits and per-item daily purchase caps keep any single good from dominating.
- **Custom Stall Config GUI** — right-click a Merchant Stall with an empty hand to set the item (click it in without consuming it) and a price in Cogs / Sprockets / Bevels / Spurs.
- **Flexible food matching** — food items are matched by base type, so freshness/quality data components from other mods don't invalidate stock.
- **Stock Market integration** *(optional)* — stalls are registered as shops in Create: Stock Market for a global reference price, with owner attribution and a **Volume** tab showing a per-player daily trade-volume line graph.
- **Nations** — link markets into nations and view aggregate statistics.

---

## Dependencies

| Mod | Required | Notes |
|-----|----------|-------|
| [Create](https://www.curseforge.com/minecraft/mc-mods/create) | ✅ | 6.0.9–6.1.0 |
| [Create: Numismatics](https://modrinth.com/mod/numismatics) | ✅ | currency backend |
| [Create: Villager Commerce](https://www.curseforge.com/minecraft/mc-mods/create-villager-commerce) | ✅ | stalls + villager AI this mod extends |
| [Create: Stock Market](https://modrinth.com/mod/create-stock-market) | ⭐ optional | global pricing + Volume graph tab |

> **Note on Stock Market / Villager Commerce:** in packs where these are Fabric mods bridged through **Sinytra Connector**, Stock Market is declared as an *optional* dependency on purpose. A hard NeoForge dependency on a Connector-loaded mod reshuffles the module layers and breaks cross-loader class visibility. The integration activates automatically whenever Stock Market is present.

---

## Installation

1. Install NeoForge `21.1.x` for Minecraft `1.21.1`.
2. Drop the required dependency jars into your `mods/` folder.
3. Drop `marketcoordination-0.5.0.jar` into `mods/`.
4. (Optional) Add Create: Stock Market for global pricing and the Volume graph.

---

## Getting started in-game

1. **Register a market** — right-click a **Market Ledger** lectern. Spacing rules keep markets from overlapping.
2. **Place Merchant Stalls** near the ledger and bind them to it.
3. **Configure a stall** — right-click it with an **empty hand** to open the Stall Config GUI. Click the item you want to sell into the slot (it isn't consumed) and set a price.
4. Villagers registered to the market will begin shopping against the market budget.
5. **Shift + right-click** the ledger for a read-only Market Summary (tier, nation, category budgets, volume history, growth trend, issuance).

---

## Commands (op level 2)

| Command | Description |
|---------|-------------|
| `/marketcoord info [market]` | Summary for one market, or the market count |
| `/marketcoord list` | Registered markets (with nation link if set) |
| `/marketcoord activity` | Activity scores, volume, sellers |
| `/marketcoord debug` | Budget, issuance, specialization, per-category baskets, trade volume |
| `/marketcoord prices` | Stock Market global reference prices + indexed stalls |
| `/marketcoord traders` | Per-market attributed volume + global top-trader leaderboard |
| `/marketcoord reset` | Reset daily spend + commodity/category caps |
| `/marketcoord setbudget <cogs>` | Override daily budget (all markets) |
| `/marketcoord nation list` | Linked nations |
| `/marketcoord nation info <name>` | Aggregate nation statistics |
| `/marketcoord nation create <name>` | Create a nation metadata record |
| `/marketcoord nation link <nation> <market> [capital]` | Link a market to a nation |

---

## Configuration

TOML config sections: `[market]`, `[growth]`, `[purchasing]`, `[population]`, `[anti_abuse]`, `[categories]`, `[performance]`.

Datapack-overridable data files (`data/marketcoordination/`):

- `commodity_caps.json` — per-item daily purchase caps (`default_daily_cap` + overrides)
- `commodity_categories.json` — item → category mappings (food, fuel, materials, textiles, tools)
- `category_budget_weights.json` — per-category share of the market budget

---

## Building from source

Requirements: JDK 21 (the build is pinned to run on JDK 24 via `org.gradle.java.home`; adjust in `gradle.properties` if yours differs).

```powershell
.\gradlew.bat build
```

Output: `build/libs/marketcoordination-0.5.0.jar`

For compile-time mixin/API work against Villager Commerce and Stock Market, drop their jars into `libs/` (see `libs/README.txt`). They are `compileOnly` and never bundled.

Dev run:

```powershell
.\gradlew.bat runClient   # or runServer
```

---

## Using with packwiz

Each GitHub release attaches `marketcoordination-<version>.jar`. To add it to a [packwiz](https://packwiz.infra.link/) pack from the release URL, run this from your pack root:

```bash
packwiz url add marketcoordination \
  "https://github.com/Minecraft-Bonanaza/Create--Market-Maker/releases/download/v0.5.0/marketcoordination-0.5.0.jar"
```

packwiz will download the jar, compute its hash, and write `mods/marketcoordination.pw.toml`. A ready-made, drop-in copy of that file (with the correct `sha512` for v0.5.0) is provided in [`packwiz/mods/marketcoordination.pw.toml`](packwiz/mods/marketcoordination.pw.toml) — copy it into your pack's `mods/` directory and run `packwiz refresh`.

---

## Documentation

- [Design spec](brave_new_globe_market_growth_system.md)
- [Build plan](BUILD_PLAN.md)
- [Integration map](docs/integration-map.md)
- [Performance baseline](docs/performance.md)
- [Changelog](CHANGELOG.md)
- [Testing guide](TEST_INSTR.md)
