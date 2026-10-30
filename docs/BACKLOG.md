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

- [x] **COMPLETADO** — Add configurable RSS/Atom social sources, polling, deduplication, and watchlist token association.
- [x] **COMPLETADO** — Persist social events and expose `/social` plus token-associated alerts.
- [x] **COMPLETADO** — Generate deterministic, evidence-backed `SocialSignal` records for token-associated events; events without a token and future-dated events do not create token signals.
- [x] **COMPLETADO** — Harden RSS/Atom ingestion with durable raw events, source cursors, versioned normalization, evidence-backed token associations, deterministic social signals, persisted source health, bounded backoff, and durable Telegram delivery.
- [ ] **INCOMPLETO (TRACKING DIFERIDO)** — Add operator-managed social accounts/sources (for example direct X/account tracking or streaming providers) with per-account priorities and commands. The current build intentionally keeps RSS/Atom as its supported social provider.

### Social Intelligence implementation guidance

The current RSS/Atom provider is the supported Social Intelligence source for this milestone. Preserve the existing `social_events` data with additive Flyway migrations; do not rewrite or remove an applied migration. New account/stream providers remain a separate tracking milestone.

Implement the hardening work as reviewable slices, keeping the application buildable after each slice:

1. **Contracts and compatibility:** Define stable meanings for internal source ID, provider, external source/account ID, external event ID, canonical URL, and deduplication key. Specify event update/version semantics, timestamps (`publishedAt`, `receivedAt`, `processedAt`, `alertedAt`), processing states, and algorithm versions. Keep provider DTOs and RSS/XML parsing in infrastructure adapters.
2. **Durable raw ingestion:** Have providers return raw events plus fetch metadata, cursor, and typed failure/retry information. Persist the raw batch before advancing its cursor. Advance only after durable persistence; define overlap/recovery behavior for feeds without stable cursors. Bound response and payload sizes and retain the existing XML external-entity protections.
3. **Normalization and idempotency:** Normalize raw events through an application use case. Enforce primary deduplication with a database unique constraint and atomic insert/upsert semantics, not a check-then-insert alone. Add a deterministic secondary fingerprint for missing provider IDs. Persist processing failures with stage, category, retryability, attempt count, and timestamps. Treat updated posts as revisions or explicit update events, not silently as duplicates.
4. **Sources, accounts, and health:** Persist enabled state, provider identity, priority, and polling interval. Make scheduling honor per-source/account settings and disabled sources; define behavior for concurrent pollers. Record last attempt/success/error, latency, consecutive failures, and backoff state.
5. **Entities and token associations:** Represent association type, mint, confidence, extractor/rule version, and structured evidence. Never assign an ambiguous symbol or narrative to a specific token at high confidence without corroborating evidence. Events may validly have no token association.
6. **Social signals:** Specify deterministic identity, aggregation windows, strength/confidence/priority rules, source-diversity calculation, refresh behavior, and freshness/expiry transitions before implementation. Preserve links to supporting events and deduplicate alerts independently from event ingestion.
7. **Queries and operations:** Extend `/social` and add source-management commands only after the contracts stabilize. Expose ingestion, processing, failure, deduplication, freshness, latency, and alert metrics. Telegram delivery failures must not roll back persisted events or lose retryable alert work.

Replayability is a design constraint from the first slice, not a later-only feature. Persist immutable provider input and available source/receive/process/alert times; version normalizer, classifier, association, and signal rules. Processing an event at time `T` may only use information observed at or before `T`. Define handling for late and out-of-order events and make replay use the same application rules as live processing.

### Social Intelligence acceptance checks

- Replaying or restarting ingestion does not create duplicate events or duplicate alerts; concurrent processing of the same event remains idempotent.
- A cursor never advances past raw data that was not durably stored, and a transient provider failure does not stop other sources from polling.
- Permanent payload/normalization failures are recorded and do not retry indefinitely; transient failures use bounded backoff.
- Legacy rows remain queryable after migration and do not trigger historical alerts during rollout.
- Every token association has reproducible evidence and a version; ambiguous symbol/name matches cannot produce high-confidence associations by themselves.
- Social signals update/expire deterministically, link to evidence, and contain no financial action (`BUY`, `SELL`, `LONG`, `SHORT`, `ENTRY`, or `TAKE_PROFIT`).
- Tests use fixtures and fake providers without real credentials or Internet. PostgreSQL integration tests verify constraints, cursor commits, migration/backfill, and concurrent idempotency.

## EPIC 6 — Signal Intelligence (P1)

- [x] **COMPLETADO** — Correlate Social, Market, and wallet-purchase evidence by token and configurable event-time window. A HIGH cross-domain signal currently requires all three components and three unique wallet transactions.
- [x] **COMPLETADO** — Persist deterministic, expiring, evidence-backed cross-domain signals and expose application queries for recent, per-token, and high-priority signals. Internal publication is a port for a later durable notification outbox.
- [x] **COMPLETADO** — Wire cross-domain signals into Telegram `/cross-signals` and a durable, retryable outbox delivery path with idempotency keys; application notifications now pass through the durable adapter when enabled.

Dependencies and constraints:

- Start after the relevant Market, Wallet, and Social signal contracts are stable; consume normalized signals through application ports/events, never provider adapters or raw social posts.
- Define event-time windows, freshness, confidence, priority, identity/update rules, and expiry deterministically. Reject future evidence relative to the cross-signal detection time.
- Keep `/signals` descriptive and evidence-backed. A single source or signal family must not be presented as multi-source confirmation.
- Make signal persistence and alert deduplication restart-safe; decide which internal events require durable delivery before relying on them for downstream processing.

## EPIC 7 — Historical Replay (P1)

- [x] **COMPLETADO** — Add deterministic Paper Validation over a replay dataset: event-time entries, configurable fees/slippage, explicit take-profit/stop-loss/expiry/period exits, realized/unrealized PnL, equity curve, drawdown, exposure, and per-signal/per-token metrics.
- [x] **COMPLETADO** — Define `HistoricalReplayPort` and replay dataset fixtures without coupling validation to Helius, DexScreener, PostgreSQL, Jev, or live execution.
- [x] **COMPLETADO** — Store provider events, snapshots, processing failures, and paper-validation outcomes needed by the replay bridge.
- [x] **COMPLETADO** — Add deterministic replay and paper-validation dataset projection without look-ahead bias; `/paper-validate [hours]` exposes the operator result.

Replay must reuse versioned production normalization and signal rules, provide an as-of-time view of input data, and record the algorithm versions used for each result. Evaluate outcomes from signal detection time, not token creation time. Define late-event handling and dataset fields before paper-intelligence validation begins.

## EPICS 8–10 — Deferred

Decision engines and paper intelligence follow Cross Signal and Historical Replay validation; they consume an aggregated intelligence snapshot rather than querying providers directly. Existing paper trading remains experimental until replay-based outcomes, mark-to-market, drawdown, and execution-cost assumptions are validated. Real trading remains blocked and requires a separate approved scope; no milestone in this backlog authorizes live orders or key custody.
