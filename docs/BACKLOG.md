# Backlog Tickets

> Estado de tickets: `COMPLETADO` = implementado y marcado con `[x]`; `INCOMPLETO` = pendiente y marcado con `[ ]`.

## EPIC 0 — Foundation (P0)

- [x] **COMPLETADO** — Upgrade the Maven baseline to Java 21 and establish bounded-context packages.
- [x] **COMPLETADO** — Add PostgreSQL configuration with profiles and Flyway migrations.
- [x] **COMPLETADO** — Add `.env.example`, Actuator health/readiness, and Micrometer metrics.
- [x] **COMPLETADO** — Add base Spring context and Testcontainers PostgreSQL test setup.

## EPIC 1 — Telegram (P0)

- [x] **COMPLETADO** — Add `NotificationPort` and an HTTP Telegram adapter using long polling.
- [x] **COMPLETADO** — Add `TELEGRAM_BOT_TOKEN` and `TELEGRAM_ALLOWED_CHAT_IDS` configuration.
- [x] **COMPLETADO** — Implement `/status`, `/health`, and `/help` through application use cases.
- [x] **COMPLETADO** — Ignore and security-log unauthorized chat IDs.

## EPIC 2 — Market Intelligence (P0)

- [x] **COMPLETADO** — Define `MarketDataProvider` behind the provider port; Helius is the selected market source.
- [x] **COMPLETADO** — Normalize price, aggregate volume, liquidity, price movement, and freshness.
- [x] **COMPLETADO** — Persist market ticks and snapshots and emit descriptive signals.
- [x] **COMPLETADO** — Send structured Telegram alerts with evidence and timestamps.
- [x] **COMPLETADO** — Replace overview polling with streaming/WebSocket ingestion.
- [x] **COMPLETADO** — Add trade-level buyer/seller pressure and unique trader metrics.
- [x] **COMPLETADO** — Enrich `/token` on demand with live DEX price, market cap, liquidity, volume, price change, and five-minute transaction counts; persist provider metadata so symbol lookups remain available.
- [x] **COMPLETADO** — Drive live stream subscriptions dynamically from the persisted token watchlist and emit Telegram alerts without duplicating `MARKET_WATCH_MINTS` configuration.
- [x] **COMPLETADO** — Combine streaming trades with periodic DEX snapshots and suppress duplicate token/type alerts during the active 15-minute signal window.
- [x] **COMPLETADO** — Display token names in market alerts and add persistent `/alerts`, `/alerts-on`, and `/alerts-off` controls for individual or all watched tokens.
- [x] **COMPLETADO** — Add a configurable momentum decision engine and persisted paper portfolio with `/paper-status`, `/paper-on`, `/paper-off`, and `/paper-reset`; paper BUY/SELL decisions notify Telegram, risk limits protect the simulation, and real execution remains disabled.
- [x] **COMPLETADO** — Add `/signals` to group active market signals by token with evidence, market context, and paper-evaluation status.

## EPIC 3 — Token Discovery (P0)

- [x] **COMPLETADO** — Define configurable candidate filters and eligibility scoring.
- [x] **COMPLETADO** — Add token watchlist persistence and `/tokens`, `/token`, `/watch-token`, and `/unwatch-token` commands.

## EPIC 4 — Wallet Intelligence (P0)

- [x] **COMPLETADO** — Define wallet provider ports and integrate Helius transaction data through standard RPC.
- [x] **COMPLETADO** — Detect swaps, reconstruct observable positions, and calculate sample-aware profiles.
- [x] **COMPLETADO** — Add wallet registry and query/management ports; alert polling remains bounded and descriptive.

## EPIC 5 — Social Intelligence (P1)

- [ ] **INCOMPLETO** — Add configurable social sources, polling/streaming, deduplication, and token association.
- [x] **PARCIAL** — Define normalized social events and expose `/social`; provider ingestion and persistence remain pending.

## EPIC 6 — Signal Intelligence (P1)

- [ ] **INCOMPLETO** — Correlate market, wallet, and social evidence by token and time window.
- [ ] **INCOMPLETO** — Add freshness, confidence, priority, evidence, `/signals`, and cross-signal alerts.

## EPIC 7 — Historical Replay (P1)

- [ ] **INCOMPLETO** — Store provider events, snapshots, processing failures, and post-signal outcomes.
- [ ] **INCOMPLETO** — Add replay and dataset export without look-ahead bias.

## EPICS 8–10 — Deferred

Decision engines and paper trading follow Core validation. Real trading remains blocked and requires explicit approval outside the MVP.
