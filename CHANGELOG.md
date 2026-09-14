# Changelog

All notable changes to Create: Market Maker are documented here. This project follows a
loose [semantic versioning](https://semver.org/) scheme for a Minecraft mod.

## [0.5.0] - 2026-09-14

First public release.

### Added
- **Custom Stall Config GUI** — empty-hand right-click on a Merchant Stall opens a clean
  item + price editor. The item is set by clicking it into a slot without consuming it, and
  the price is entered in Cogs / Sprockets / Bevels / Spurs.
- **Demand curve** — acceptable price ceiling scales down as a commodity/category fills its
  daily quota (`[purchasing]` config: enable flag, floor multiplier, curve exponent).
- **Create: Stock Market integration** — Villager Commerce stalls are indexed as shops for a
  global reference price, with owner attribution on each shop entry.
- **Volume tab** — a fourth "Volume" tab in the Stock Market screen renders a per-player
  daily trade-volume line graph (global, cross-market, rolling 14-day window).
- **Trade attribution** — per-market `TraderLedger` and global `TraderHistoryService` track
  volume/trades per owner; new `/marketcoord prices` and `/marketcoord traders` commands.
- **Flexible food matching** — food items are matched by base type so freshness/quality data
  components from other mods no longer invalidate stall stock.
- **Read-only stock probe + affordability filter** — villagers skip out-of-stock and
  unaffordable stalls before pathing to them; sticky targeting avoids per-tick thrashing.
- **Per-villager daily spend cap** and **self-scaling shopping cadence** for performance.

### Changed
- **Growth model** rewritten around daily budget utilization with a sliding growth/decay
  scale and a decay floor, replacing the old weighted activity score.
- **Debug/activity commands** reformatted with clear per-market sections and per-category baskets.
- Commodity caps raised and category mappings broadened across common vanilla items.

### Fixed
- UTF-8 BOMs stripped from `neoforge.mods.toml`, mixin config, and lang file that crashed
  bootstrap under Sinytra Connector.
- Create dependency range relaxed to `[6.0.9,6.1.0)`.
- Cross-loader crash from declaring Stock Market as a hard/`AFTER` dependency: it is kept
  `optional` so Connector module layers stay readable to the Villager Commerce integration.

## [0.4.0]
- Nation registry, cross-market statistics, `/marketcoord nation` commands, procurement pool
  metadata stubs.

## [0.3.0]
- Category budgets, regional specialization bias, issuance tracking, extended summary UI.

## [0.2.0]
- Full activity score, Stock Market price cache at day boundary, observed-price fallback,
  anti-abuse diminishing returns, minimum market spacing.

## [0.1.0]
- Villager Commerce purchase mixins, budget/caps/ceiling, competitive stall selection,
  growth/decay, SavedData persistence, basic UI, admin commands.
