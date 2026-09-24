# AI Coding Harness

## Mission

Build the Memecoin Intelligence Bot incrementally as a local paper-intelligence system. The system observes Solana market, wallet, and social data, normalizes it, stores evidence, and sends explainable Telegram alerts. It must not execute trades.

## Non-negotiable Rules

- Java 21 and Spring Boot own all business logic.
- Keep a modular monolith with lightweight DDD, hexagonal boundaries, and internal events.
- Use PostgreSQL for persistence. Redis is optional and requires a concrete use case.
- Keep Telegram and external providers behind ports and adapters.
- Use TypeScript only for integration-specific Solana functionality; never put business decisions there.
- Do not add microservices, Kafka, Kubernetes, LLM dependencies in the core, private-key custody, transaction signing, or BUY/SELL commands.
- Never add a `REAL_TRADING_ENABLED` switch to the MVP.

## Module and Data Rules

Each bounded context should use `domain`, `application`, `ports`, and `infrastructure` packages. Domain code must not import infrastructure or provider DTOs. Use `BigDecimal` for financial values and `Instant` for event timestamps. External events must be immutable, idempotently processable, and deterministically deduplicated.

## Telegram and Configuration

Route commands as adapter → application use case → domain. Route alerts through application events → `NotificationPort` → Telegram adapter. Validate `TELEGRAM_ALLOWED_CHAT_IDS` for every command. Keep credentials in environment variables and out of source code and logs.

## Development Workflow

Before coding, read `PROJECT_IDEA.md`, identify the bounded context, state affected modules and assumptions, implement the smallest coherent vertical slice, add tests, run the relevant Maven checks, and report changed files and remaining risks. Do not refactor unrelated modules.
