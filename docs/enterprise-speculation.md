# Enterprise Speculation — moved

Bank, enterprises, equity, and derivatives are **not** implemented in this repo.

Living plan and future code:

**`../Create- Enterprise Speculation/BUILD_PLAN.md`**

That mod (`enterprisespeculation`) hard-depends on Create + Numismatics and optionally hooks Stock Market, this mod, and Villager Commerce. This repo must **never** depend on it.

Later, this repo only adds a thin surface if ES needs it:

- `MarketPurchaseEvent` from `VcIntegration.onPurchaseRecorded`
- `SellerIdentityLookup` SPI for bound-stall → enterprise id
