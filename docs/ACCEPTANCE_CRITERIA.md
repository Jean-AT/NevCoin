# Acceptance Criteria

## Foundation and Telegram Milestone

- The project builds with Java 21 using `./mvnw clean package`.
- PostgreSQL starts through local infrastructure, migrations run cleanly, and the application can restart without corrupting state.
- Actuator exposes working health/readiness checks and baseline metrics.
- The Telegram adapter connects through long polling without a public endpoint.
- `/status`, `/health`, and `/help` route through application services and return structured English responses.
- A chat ID outside `TELEGRAM_ALLOWED_CHAT_IDS` cannot execute commands and creates a security log entry.
- Tokens and database credentials are supplied through configuration and do not appear in source code or logs.

## Telegram Milestone

- Long polling is disabled unless `TELEGRAM_ENABLED=true`.
- Enabling Telegram without a bot token or allowlisted chat ID fails fast during startup.
- `/status`, `/health`, and `/help` return structured English responses through application services.
- Commands may include a bot mention, such as `/health@nevcoin_bot`.
- Unauthorized chat IDs receive no response and are security logged.
- Telegram API errors retry with a bounded backoff and never expose the bot token in logs.

## Market Intelligence First Slice

- `MarketDataProvider` hides provider-specific HTTP details from the market domain.
- Market polling is disabled unless `MARKET_ENABLED=true`; Helius ingestion requires `HELIUS_API_KEY` and configured `MARKET_WATCH_MINTS`.
- Each provider response records token address, source and observed timestamps, price, liquidity, aggregate volume, price movement, and freshness.
- PostgreSQL migrations create market ticks, snapshots, signals, and signal evidence tables.
- Configurable momentum, volume-spike, and liquidity-drop rules emit descriptive signals only; no BUY or SELL decision is produced.
- Configured Telegram alert chats receive signal evidence only when `MARKET_ALERTS_ENABLED=true`.
- When `MARKET_STREAM_ENABLED=true`, the Helius trade WebSocket starts for configured watch mints and overview polling is disabled.
- Stream disconnects are logged and retried with the configured bounded delay; disabling the stream keeps the overview polling fallback available.
- Trade-level metrics aggregate a five-minute window with BUY volume, SELL volume, net flow, unique buyers, and unique sellers.
- Stream-derived market data is persisted in ticks and snapshots and remains descriptive intelligence only; no BUY or SELL decision is produced.
- The live stream uses active tokens from the persisted watchlist, accepts additions and removals without restarting the application, and may include optional static `MARKET_WATCH_MINTS` entries for backward compatibility.
- Streaming trade ingestion runs alongside periodic DEX overview snapshots so price/liquidity signal rules remain actionable; an active signal of the same token and type suppresses duplicate persistence and Telegram alerts until its expiry.
- Market signal notifications display persisted token names/symbols instead of raw mint addresses, and Telegram can enable or disable alerts persistently for one watched token or all tokens.
- Paper trading is disabled by default, persists simulated cash, positions, trades, and realized PnL, and can produce only simulated BUY/SELL actions from the rule-based decision engine; Telegram paper decision alerts include token names, risk blocks, and simulated-only disclaimers; max positions, max daily loss, stop-loss, take-profit, and trade-notional limits are configurable; `/paper-reset` clears the simulated portfolio; no private key, transaction signer, or live order endpoint is used.
- `/signals` lists non-expired market signals grouped by token, includes evidence and the latest available price, liquidity, volume, and net-flow context, and clearly states that paper evaluation is scheduled rather than a live trade.
- `/token <mint|symbol>` enriches a persisted watchlist entry with current DEX market data, identifies its source, and degrades safely when no liquid pool is available.
- Live token symbol and name metadata are persisted, and a legacy watchlist entry without metadata can be resolved by its provider symbol.
- When several DEX pools exist for one mint, the overview selects the pool with the greatest reported USD liquidity.

## Intelligence Capabilities

- Token discovery uses configurable thresholds and emits candidates with reasons, scores, timestamps, and expiry.
- The token watchlist is persisted and Telegram supports `/tokens`, `/token`, `/watch-token`, and `/unwatch-token`.
- Wallet activity is accessed through a provider port and Helius standard RPC normalizes recent transactions and token balance changes.
- Wallets are persisted in a registry, swaps require both token balance directions, positions remain observable balances, and profiles expose confidence based on sample size.
- Market data records price, volume, liquidity, pressure, freshness, and source quality without issuing BUY or SELL decisions.
- Wallet analytics distinguish tracked wallets from smart-wallet conclusions and require sufficient sample size.
- Social events record source, publication time, detection time, hashes, related tokens, and confidence.
- Cross-signals include market, wallet, and social evidence, freshness, confidence, priority, and expiry.
- `/social` exposes the last 24 hours of normalized social events and clearly reports when no provider is configured.

## Validation and Definition of Done

- Every external event records available source, received, processed, and alert timestamps.
- Duplicate events are safely ignored and provider failures are recorded for recovery.
- Validation measures post-detection outcomes, latency, false positives, drawdown, and data freshness without using future information.
- Each completed ticket compiles, has relevant tests, preserves module boundaries, updates documentation/configuration when needed, and reports known risks.
