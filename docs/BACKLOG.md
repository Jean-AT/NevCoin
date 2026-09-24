# Backlog Tickets

## EPIC 0 — Foundation (P0)

- [x] Upgrade the Maven baseline to Java 21 and establish bounded-context packages.
- [x] Add PostgreSQL configuration with profiles and Flyway migrations.
- [x] Add `.env.example`, Actuator health/readiness, and Micrometer metrics.
- [x] Add base Spring context and Testcontainers PostgreSQL test setup.

## EPIC 1 — Telegram (P0)

- [ ] Add `NotificationPort` and an HTTP Telegram adapter using long polling.
- [ ] Add `TELEGRAM_BOT_TOKEN` and `TELEGRAM_ALLOWED_CHAT_IDS` configuration.
- [ ] Implement `/status`, `/health`, and `/help` through application use cases.
- [ ] Ignore and security-log unauthorized chat IDs.

## EPIC 2 — Market Intelligence (P0)

- [ ] Define `MarketDataProvider` and integrate Birdeye behind the port.
- [ ] Normalize price, volume, liquidity, buyer/seller pressure, and freshness.
- [ ] Persist market ticks and snapshots and emit descriptive signals.
- [ ] Send structured Telegram alerts with evidence and timestamps.

## EPIC 3 — Token Discovery (P0)

- [ ] Define configurable candidate filters and eligibility scoring.
- [ ] Add token watchlist persistence and `/tokens` and `/token` queries.

## EPIC 4 — Wallet Intelligence (P0)

- [ ] Define wallet provider ports and integrate Helius transaction data.
- [ ] Detect swaps, reconstruct observable positions, and calculate sample-aware profiles.
- [ ] Add wallet registry, queries, and alerts.

## EPIC 5 — Social Intelligence (P1)

- [ ] Add configurable social sources, polling/streaming, deduplication, and token association.
- [ ] Persist social events and expose `/social` plus alerts.

## EPIC 6 — Signal Intelligence (P1)

- [ ] Correlate market, wallet, and social evidence by token and time window.
- [ ] Add freshness, confidence, priority, evidence, `/signals`, and cross-signal alerts.

## EPIC 7 — Historical Replay (P1)

- [ ] Store provider events, snapshots, processing failures, and post-signal outcomes.
- [ ] Add replay and dataset export without look-ahead bias.

## EPICS 8–10 — Deferred

Decision engines and paper trading follow Core validation. Real trading remains blocked and requires explicit approval outside the MVP.
