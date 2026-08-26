# Performance baseline (v0.1)

Targets from [BUILD_PLAN.md](../BUILD_PLAN.md):

| Area | Target | v0.1 status |
|------|--------|-------------|
| Purchase mixin overhead | ≤ 50 µs when budget allows | Pending VC mixin mapping |
| Daily cycle | Staggered across `day_cycle_stagger_ticks` (default 32) | Implemented |
| Stall cache | Per-market list, no radius scan per purchase | Infrastructure ready |
| SavedData | Dirty batch at day boundary + server stop | Implemented |
| Client UI idle cost | Zero tick handlers when UI closed | UI not yet implemented |
| MSPT under 3 markets × 20 stalls | Unnoticeable vs baseline | Not yet measured |

Record Spark before/after results here when running on Brave New Globe test world.
