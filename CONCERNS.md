# BeakBock Concerns & Our Mitigations

BeakBock noted that currency budgets, richer purchase policy, and similar economics were **planned for Villager Commerce** but shelved for:

1. **General appeal** — keep VC approachable for players who don’t want deep Minecraft economics  
2. **Performance** — avoid heavy per-tick / per-purchase work in core VC  

---

## 1. General appeal

**Our take:** Not our problem. Market Coordination is an **optional addon** for a self-selecting pack (Brave New Globe). VC stays simple for everyone else; servers without this mod pay **zero** cost.

---

## 2. Performance — how we mitigate

### We don’t create more shopping — we gate existing attempts

VC already throttles villager shopping (cooldowns, chance rolls, per-tick check/purchase budgets). We add **allow/deny + stall choice** on those attempts. We do **not** spawn extra purchase attempts or per-tick world scans.

### Hot path stays cheap

On each `canPurchase` / purchase-gate call:

| Check | Cost |
|-------|------|
| Budget remaining | O(1) map lookup |
| Category budget | O(1) map lookup |
| Commodity cap | O(1) map lookup |
| Price ceiling | Cached Stock Market reference × multiplier |

Early-exit when budget is exhausted — before enumerating stalls for competitive selection.

Target: ≤ ~50 µs amortized overhead beyond VC baseline when a purchase is allowed (profiled; see `docs/performance.md`).

### Stall work is bounded

- Iterate stalls linked to **one market** (ledger radius / VC max-stalls-considered), not the whole world  
- Prefer per-market **stall cache** (`MarketRegistry`) over radius scan when warm  
- Competitive selection is O(n) over that capped set — slightly more than VC’s first-match, still capped  

### Heavy work is batched (not per purchase)

| Work | When |
|------|------|
| Growth / decay / activity score | Day boundary, **staggered** across up to 32 ticks |
| Stock Market price refresh | Once per day — never per purchase |
| SavedData flush | Dirty flag; day end + server stop |
| Commodity / category daily reset | Day boundary |

### Memory & client

- Fixed-size rolling windows (7-day rings); capped observed-price samples  
- Economy is **server-only**; UI sync on open, max ~1 Hz while open  
- **Zero** client tick handlers when Market Coordination UI is closed  

### Addon isolation

No Market Coordination → no cost. Cost scales with active ledgers / VC shopping volume, not “always on” simulation.

---

## Honest tradeoffs

1. **Competitive selector** costs more than VC first-match when many stalls pass the gate (finish the loop + band + random). Mitigated by stall caps, cache, and budget early-exit.  
2. **Gate runs twice** on a successful trip (selection filter + execution re-check) — intentional for travel-time races; second check is on **one** stall.  
3. **Measurements still pending** — Spark/MSPT baselines in `docs/performance.md` are not fully filled yet; stall cache exists but selection may still fall back to `getNearbyStalls`.  
4. **We ride VC throttling** — if VC shopping rates rise in a future version, our per-attempt cost scales with them.

---

## What we need from VC (vs what we own)

**From BeakBock:** Stable hooks (count-cap flag, purchase attempt event, post-purchase event; optional stall selector). Not VC implementing our economy.

**We own:** Budget/gate math, growth, SM caching, day-cycle stagger, stall cache, persistence, profiling.

---

## One-liner for outreach

> Optional addon: we add O(1) gates and bounded stall choice on VC’s existing shopping pipeline, batch growth and disk I/O at day boundary, and add no extra villager purchase attempts or idle client tick cost. Extra cost vs VC default is mainly scanning eligible stalls for fair selection — capped by VC’s own stall limits and shopping throttles.
