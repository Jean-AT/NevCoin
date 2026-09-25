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

## Intelligence Capabilities

- Token discovery uses configurable thresholds and emits candidates with reasons and timestamps.
- Market data records price, volume, liquidity, pressure, freshness, and source quality without issuing BUY or SELL decisions.
- Wallet analytics distinguish tracked wallets from smart-wallet conclusions and require sufficient sample size.
- Social events record source, publication time, detection time, hashes, related tokens, and confidence.
- Cross-signals include market, wallet, and social evidence, freshness, confidence, priority, and expiry.

## Validation and Definition of Done

- Every external event records available source, received, processed, and alert timestamps.
- Duplicate events are safely ignored and provider failures are recorded for recovery.
- Validation measures post-detection outcomes, latency, false positives, drawdown, and data freshness without using future information.
- Each completed ticket compiles, has relevant tests, preserves module boundaries, updates documentation/configuration when needed, and reports known risks.
