# Market Coordination — Manual Testing Instructions

**Mod version:** `0.4.0-dev`  
**Platform:** NeoForge 1.21.1 (Brave New Globe target)

Use this document as a manual SMP checklist before calling v0.4 ready for in-game validation. Check each box and note any failures with world seed, coordinates, and log snippets.

---

## 1. Test environment

### Required mods

| Mod | Role |
|-----|------|
| **NeoForge** 21.1.x | Loader |
| **Create** ~6.0.x | Pack baseline |
| **Create: Numismatics** | Hard dependency — all prices/budgets in spurs |
| **Create: Villager Commerce** (`createvillagercommerce`) | Ledgers, stalls, villager purchases |
| **Market Coordination** | This mod |

### Optional (recommended for full v0.2 coverage)

| Mod | Role |
|-----|------|
| **Create Stock Market** (`stockmarket`, Deokma) | Reference prices for price ceilings |

### Install

1. Build the mod:
   ```powershell
   .\gradlew.bat build
   ```
2. Copy `build/libs/marketcoordination-0.4.0-dev.jar` into the instance `mods/` folder alongside VC, Numismatics, and Create.
3. Use a **Creative** or **flat test world** first, then repeat critical paths on a Brave New Globe test server if available.

### Operator permissions

All `/marketcoord` commands require **permission level 2** (OP). Give yourself OP before testing.

### VC / pack prerequisites

- Villager Commerce must allow **player stall purchases** (not preset-trades-only).
- Stalls must use **Numismatics coin stacks** as currency (not iron ingots).
- Each test market needs:
  - One **Market Ledger** lectern (named book)
  - One or more **Merchant Stalls** linked to that ledger
  - At least one **villager registered** to the ledger (VC workflow)

### Useful config tweaks for faster tests

Edit `config/marketcoordination-common.toml` (created on first launch):

| Setting | Default | Suggested for testing |
|---------|---------|------------------------|
| `[market] minimum_market_spacing` | 750 | `50` when testing spacing in a small area |
| `[market] starting_daily_budget` | 20 Cogs | Leave default unless testing tier curve |
| `[growth] maximum_daily_growth` | 0.08 | `0.25` to see budget rise faster over fewer days |
| `[anti_abuse] repeat_window_ticks` | 24000 | `6000` (~5 min) for quicker repeat-trade tests |
| `[categories] enabled` | true | Set `false` once to confirm total-budget-only fallback |

Restart the world after config changes.

---

## 2. Smoke test (5 minutes)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Launch server/client; check `latest.log` | No crash; log lines for VC probe and integration presence |
| 2 | Place VC Market Ledger lectern, name the market in the book | — |
| 3 | **Right-click** lectern | Chat: `Registered market: <name> (shift-click for details)` |
| 4 | `/marketcoord list` | Market appears with tier `Outpost` (or similar) |
| 5 | **Shift + right-click** lectern | Market Summary screen opens |
| 6 | `/marketcoord info <market name>` | Tier, activity %, budget remaining, 7d volume shown |

**Pass criteria:** Registration, commands, and UI all work without errors.

---

## 3. v0.1 — Core economic loop

### 3.1 Starting budget

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Register a **new** market (fresh ledger) | — |
| 2 | `/marketcoord info <market>` or shift-click UI | Daily budget ≈ **20 Cogs** (~1280 spurs); tier **Outpost** |

### 3.2 Budget exhaustion

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Set up stall(s) with low prices; register villager(s) to ledger | Villagers path to stalls |
| 2 | Let villagers buy until budget is spent | Purchases succeed while budget remains |
| 3 | Continue until `/marketcoord debug` shows remaining ≈ 0 | Further VC purchases **blocked** (villagers fail to buy) |
| 4 | `/marketcoord reset` | Spending counters reset; purchases resume |

### 3.3 Commodity caps

Uses `data/marketcoordination/commodity_caps.json` (e.g. `minecraft:iron_ingot` cap = 32/day).

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Stall selling **iron ingot** (or other capped item) | — |
| 2 | Drive purchases until cap reached | Purchases stop for that item even if budget remains |
| 3 | `/marketcoord reset` | Cap counter resets; item purchasable again |

### 3.4 Competitive seller selection

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Two stalls, same item, prices within **5%** band (default `competitive_price_band`) | — |
| 2 | Observe many villager purchases over several minutes | Purchases split across stalls (not 100% one stall) |
| 3 | Add a third stall priced **well above** the cheapest | Higher stall rarely or never selected |

### 3.5 Growth and decay

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | **Active market:** sustained trading for several Minecraft days | `/marketcoord activity` score rises; budget increases over days |
| 2 | **Idle market:** no trades for several Minecraft days | Activity score decays; budget drifts toward starting level |
| 3 | Compare `/marketcoord info` before/after | Growth trend on UI shows **Rising** / **Stable** / **Falling** appropriately |

> **Tip:** Sleep or `/time add 24000` to advance days. Daily processing runs near **dawn** (first ~60 seconds of the day).

### 3.6 Persistence

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Trade on a market; note budget, activity %, 7d volume | — |
| 2 | Stop server cleanly; restart | `/marketcoord info` matches pre-restart values |
| 3 | Repeat after a day-cycle boundary | Rolling window and tier persist |

---

## 4. v0.2 — Activity score, Stock Market, anti-abuse, spacing

### 4.1 Diverging activity scores

Setup **two markets** with similar trade volume but different structure:

| Market A | Market B |
|----------|----------|
| 1–2 stalls, 1 seller | 4+ stalls, 4+ sellers |
| Same item category | Mixed categories (food + materials + fuel) |
| Few registered villagers | Many registered villagers |

After **3–7 Minecraft days**:

| Check | Expected result |
|-------|-----------------|
| `/marketcoord activity` | Market B scores **higher** than A despite similar volume |
| `/marketcoord info` | Market B daily budget **greater** than A |

### 4.2 Stock Market price ceiling

**Requires Stock Market mod installed.**

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Confirm item is listed on SM with known avg price | — |
| 2 | Stall priced **below** `reference × maximum_price_multiplier` (default 1.25×) | Purchases allowed |
| 3 | Stall priced **above** ceiling | Purchases **blocked** |
| 4 | After day boundary | SM cache refresh in log (`Stock Market price cache refreshed`) |

**Without Stock Market:** ceiling falls back to **observed purchase median** after a few trades on that item, then enforces multiplier against that.

### 4.3 Anti-abuse (diminishing growth weight)

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Same stall + same item; trigger **4+** purchases quickly | — |
| 2 | `/marketcoord activity` volume contribution | Repeated trades contribute **less** to activity than first trades (growth weight diminishes after `max_repeats_before_diminish`, default 3) |
| 3 | Wait for `repeat_window_ticks` or new Minecraft day | Repeat counter resets |

### 4.4 Minimum market spacing

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Register market A at position P | Success |
| 2 | Place second ledger within `minimum_market_spacing` blocks (default 750; use `50` in config for lab test) | **Right-click does not register**; warning in server log |
| 3 | Place second ledger **outside** spacing radius | Registers normally |
| 4 | `/marketcoord list` | Only valid-spaced markets listed |

---

## 5. v0.3 — Category budgets, specialization, advanced UI

### 5.1 Category budget split

With `[categories] enabled = true`:

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Shift-click market UI | **Category Budgets** section lists food, fuel, materials, etc. with remaining / total |
| 2 | Drive purchases in one category only (e.g. food stalls) | That category's remaining drops; others unchanged |
| 3 | Exhaust a **single category** budget | Purchases in that category blocked; other categories still work if total budget remains |

Category weights come from `data/marketcoordination/category_budget_weights.json`. Item→category mapping from `commodity_categories.json`.

### 5.2 Regional specialization

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Trade heavily in one category over several days (e.g. only `materials`) | — |
| 2 | Shift-click UI | **Specialization** field shows dominant category (`materials`) |
| 3 | `/marketcoord debug` | `dominant=materials` (or matching category) |
| 4 | With `specialization_bias > 0` | Dominant category gets slightly larger share of next day's category budget |

### 5.3 Issuance tracking

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Run several purchases | — |
| 2 | `/marketcoord debug` | `issuance daily=` and `lifetime=` increase in spur/cog terms |
| 3 | Shift-click UI | **Lifetime Issuance** line updates |
| 4 | Cross day boundary | Daily issuance resets; lifetime persists |

### 5.4 Advanced UI packet

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Open summary screen; leave open ~5 seconds | Screen refreshes (~1 Hz); values update without reopening |
| 2 | Check 7-day history line | Compact daily volumes shown (e.g. `12c \| 8c \| 0c \| ...`) |
| 3 | Close screen | No ongoing client lag; no background tick work |

---

## 6. v0.4 — Nations and aggregate statistics

### 6.1 Nation create and link

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | `/marketcoord nation create TestNation` | Nation created; UUID in chat |
| 2 | `/marketcoord nation list` | `TestNation` listed |
| 3 | `/marketcoord nation link TestNation <MarketA>` | Market linked |
| 4 | `/marketcoord nation link TestNation <MarketB> capital` | Second market linked as capital |
| 5 | `/marketcoord list` | Markets show nation UUID |
| 6 | Shift-click linked market UI | **Nation:** shows `TestNation` |

### 6.2 Nation statistics

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Generate trade volume on both linked markets | — |
| 2 | `/marketcoord nation info TestNation` | Combined markets count, volume, issuance; capital tier ordinal |
| 3 | Stop/start server | Nation links and stats persist |

### 6.3 Procurement stubs (metadata only)

Procurement pools exist in saved nation data but have **no in-game delivery or spending UI yet**.

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Inspect `marketcoordination_nations` saved data (NBT) after linking | Nation entry contains `procurement` compound (public works / military fields) |
| 2 | Normal gameplay | No teleportation, virtual delivery, or procurement orders |

---

## 7. Admin commands reference

Run these during regression passes:

```text
/marketcoord list
/marketcoord info [market name]
/marketcoord activity
/marketcoord debug
/marketcoord reset
/marketcoord setbudget <cogs>
/marketcoord nation list
/marketcoord nation info <nation name>
/marketcoord nation create <name>
/marketcoord nation link <nation> <market> [capital]
```

Verify each returns sensible output and does not throw server exceptions.

---

## 8. Performance checks

Record results in [docs/performance.md](docs/performance.md).

| Test | Procedure | Pass criteria |
|------|-----------|---------------|
| MSPT baseline | Spark profile: mod off vs on, 3 markets × ~5 stalls, active purchasing | No noticeable TPS drop at 20 TPS baseline |
| Daily cycle spike | `/time add 24000` with 10+ markets registered | No single-tick freeze > 100 ms |
| UI idle cost | Open/close summary 20×; profile client | No memory leak; no frame stutter when closed |
| UI open cost | Leave summary open 30 s | Refresh at ~1 Hz only; no chunk scans |

---

## 9. Regression matrix (quick pass/fail)

| # | Area | Pass |
|---|------|------|
| 1 | New market ≈ 20 Cog starting budget | ☐ |
| 2 | Budget exhaustion blocks purchases | ☐ |
| 3 | Commodity cap blocks item | ☐ |
| 4 | Competitive band splits sellers | ☐ |
| 5 | Active market budget grows over days | ☐ |
| 6 | Idle market decays | ☐ |
| 7 | Restart preserves state | ☐ |
| 8 | Two markets diverge by seller/diversity score | ☐ |
| 9 | Price ceiling blocks overpriced stall | ☐ |
| 10 | Spacing blocks nearby ledger | ☐ |
| 11 | Category budget shown and enforced | ☐ |
| 12 | Specialization + issuance in UI/debug | ☐ |
| 13 | Nation create/link/info works | ☐ |
| 14 | Commands + UI no server errors | ☐ |

---

## 10. Troubleshooting

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| Purchases never happen | VC villager not registered to ledger | VC registration flow |
| Purchases never blocked at budget 0 | Mixin not applying | Log for `VcVersionProbe`; VC JAR version |
| Budget always 0 / market not registered | Spacing rejection or wrong lectern book | Server log; ledger book title |
| Price ceiling always passes | SM absent and no observed prices yet | Install SM or complete a few trades first |
| UI blank or crash | Client/server version mismatch | Same JAR both sides |
| Nation link "Unknown market" | Name must match display name exactly | `/marketcoord list` for exact string |
| Config ignored | Forgot restart | Reload world after TOML edit |

### Log markers to grep

```text
Market Coordination initialized
Villager Commerce integration probe
Stock Market price cache refreshed
Rejected market ledger
Blocked VC purchase
```

---

## 11. Reporting failures

When filing an issue, include:

1. Mod JAR version (`0.4.0-dev`)
2. VC, Numismatics, Create, SM versions
3. Steps to reproduce from this document (section + step number)
4. Expected vs actual behavior
5. Relevant `latest.log` excerpt
6. Market name, coordinates, and whether SM was present
