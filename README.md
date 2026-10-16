# Create: Stock Market

**Minecraft 1.20.1 · Fabric (backport) · v1.1.1**

Turn your server's player economy into a real-time stock exchange.  
Place the **Market Terminal**, right-click it — and get a live dashboard of every shop, price trend, and top trader on the server.

---

## 🔧 Dependencies

| Mod | Required? |
|-----|-----------|
| [Fabric API](https://modrinth.com/mod/fabric-api) | ✅ Required |
| [Create Fabric](https://modrinth.com/mod/create-fabric) 6.0.8+ | ✅ Required |
| [Create: Numismatics](https://modrinth.com/mod/numismatics) 1.1.0+ (Fabric 1.20.1) | ⚠️ Optional |
| [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) | ⚠️ Optional |

> **Without Numismatics** — coin prices and Vendor shops won't appear, but TableCloth barter shops still work.

---

## 📦 Craft the Market Terminal

```
G
C
P

G = Framed Glass   C = Brass Casing   P = Precision Mechanism
```

Place it anywhere, right-click to open the market screen.  
The **Market Remote** (Iron Nugget ×6, Redstone, Ender Pearl, Market Terminal) opens it from anywhere.

---

## 🖥️ The Market Screen

The screen is fully responsive — it scales with your GUI size and fits any resolution. It has three tabs:

---

### 🛍️ Shops Tab

A live list of every active shop on the server — both **Numismatics Vendor** and **Create TableCloth** shops.

**Columns:** ★ Favourite · Item · Price · Owner · Mode (SELL / BUY) · Dimension

**Features:**
- Search by item name or shop owner
- Click any column header to sort; click again to reverse
- ★ Favourite toggle — pinned shops always appear at the top (persists per session)
- Free (0-price) shops always sort to the bottom
- **Player tabs** — All / ★ Fav / one tab per unique owner

**Sidebar filters (6 collapsible sections):**
- **Mode** — SELL / BUY
- **Type** — Vendor / TableCloth
- **Currency** — by coin denomination (Spur → Bevel → Sprocket → Cog → Crown → Sun) or barter item
- **Items** — filter by what's being sold
- **Owner** — filter by shop owner
- **Dimension** — filter by world dimension

Each filter section shows a count badge when active. Hover any row for a detailed tooltip with item, price, owner, mode, exact coordinates, and dimension.

**Xaero's Minimap integration** — right-click a shop row (requires Xaero's Minimap installed) to instantly add a waypoint at the shop's location.

---

### 📈 Market Tab

A **stock-exchange-style** view of the server economy, aggregated by item.

**Columns:** Item · Avg Price · Change (%) · Volume · Sparkline Chart · Trend

**Features:**
- **Sparkline charts** — inline price history visualization per item
- **% price change** — green `+5.2%` / red `-3.1%` based on recorded history
- **Trend indicator** — ▲ Rising / ▼ Falling / — Stable
- 🔥 **High activity** highlight — rows with ≥ 5 shops get a gold stripe
- Hover tooltip: avg price, min price, 24h change, sell/buy volume, full history bar chart
- Toolbar shows time since last refresh and a ↺ refresh button
- Click column headers to sort (Chart column is not sortable)

**Two sections in one scrollable list:**
- **Coin prices** (top) — Numismatics Vendor shops with sortable coin prices
- **Barter** (bottom) — Create TableCloth shops showing payment item and sell/buy count

**Price history engine:**
- Server takes a snapshot every **10 minutes**
- Stores up to **144 snapshots** per item (**24 hours** of history)
- History persists across server restarts (saved to world data)

---

### 🏆 Top Sellers Tab

A leaderboard of the most active traders on the server.

- Ranked by **actual sales count** (server-side trade statistics)
- Falls back to **shop listing count** if no sales data is recorded yet
- Top 3 get 🥇🥈🥉 medals with gold/silver/bronze name colors
- Each row shows a proportional bar chart
- Footer indicates whether data is live or an estimate

---

## ⚙️ Technical

- Shop indexing is **automatic** — block entities loading and blocks being broken trigger re-indexing, plus a periodic rescan
- Data is sent to the client **on demand** (right-click the block, or use the remote) — no polling overhead
- No commands required — everything is block-based
- This branch is the **Fabric 1.20.1** backport (single module, Mojang mappings); the NeoForge 1.21.1 version is a separate codebase

