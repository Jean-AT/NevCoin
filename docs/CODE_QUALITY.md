# Code Quality Standards

## Java and Naming

Target Java 21 and use four spaces for indentation. Use `PascalCase` for types, `camelCase` for methods and fields, and `UPPER_SNAKE_CASE` for constants. Keep packages under `com.trading.nevcoin`. Prefer explicit code and small cohesive classes over speculative abstractions.

## Architecture

Organize code by bounded context: discovery, market, wallet, social, signal, history, notification, and shared. Keep domain rules independent of Spring, Telegram, PostgreSQL, HTTP, WebSocket, and provider SDKs. Hide Helius, Birdeye, Solana, and social integrations behind ports. Do not leak provider DTOs into domain models.

## Reliability and Observability

External processing must support reconnects, exponential backoff, idempotency, deduplication, and recorded failures. Capture source, received, processed, and alert timestamps where available. Expose health/readiness checks and metrics for received, processed, failed, reconnect, signal, alert, latency, and freshness behavior.

## Security

Load secrets from environment variables or configuration. Never commit tokens, keys, private keys, or credentials. Enforce Telegram chat allowlisting and log unauthorized access without exposing sensitive values. No MVP component may sign or transmit financial transactions.

## Testing and Review

Test domain rules with unit tests, application orchestration with service tests, and persistence/adapters with integration tests where valuable. Use Testcontainers for PostgreSQL integration tests. A change is reviewable when it compiles, has relevant tests, preserves module boundaries, documents configuration or migration changes, and reports known limitations.
