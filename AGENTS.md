# Repository Guidelines

## Project Structure & Module Organization

NevCoin is a Java 21 Spring Boot application. Production code lives under
`src/main/java/com/trading/nevcoin`, with `NevCoinApplication` as the entry
point. Runtime configuration is in `src/main/resources` (currently
`application.properties`). Tests mirror the Java package under
`src/test/java`. `compose.yaml` provides local PostgreSQL and Redis services.
Project documentation is centralized in `docs/`: start with
`docs/PROJECT_IDEA.md`, then consult `docs/HARNESS.md`,
`docs/CODE_QUALITY.md`, `docs/BACKLOG.md`, and
`docs/ACCEPTANCE_CRITERIA.md`. Keep new code organized around the bounded
contexts and restrictions described there.

## Build, Test, and Development Commands

Use the Maven Wrapper so contributors share the project’s Maven setup:

```bash
./mvnw clean package       # compile and package the application
./mvnw test                # run the test suite
./mvnw spring-boot:run     # start the application locally
docker compose up -d       # start PostgreSQL and Redis dependencies
docker compose down        # stop local dependencies
```

The application targets Java 21. Configure local credentials and connection
settings through environment-specific properties or environment variables;
do not commit secrets. PostgreSQL is required for persistence; Redis remains
optional until a concrete cache or event use case is implemented.

## Coding Style & Naming Conventions

Use four spaces for Java indentation and standard Spring conventions. Classes
and interfaces use `PascalCase`; methods, fields, and variables use
`camelCase`; constants use `UPPER_SNAKE_CASE`. Keep domain logic independent of
Spring and external adapters. Use `BigDecimal` for financial values and
`Instant` for event timestamps. Follow the detailed rules in
`docs/CODE_QUALITY.md`.

## Testing Guidelines

Tests use JUnit 5 through Spring Boot’s test starters. Add focused unit tests
for domain rules, application tests for orchestration, and Testcontainers
integration tests for PostgreSQL and adapters where valuable. Run
`./mvnw test` before opening a pull request and verify the relevant criteria in
`docs/ACCEPTANCE_CRITERIA.md`.

## Commit & Pull Request Guidelines

Use short, imperative commit subjects such as `Add token discovery model` or
`Fix Redis session configuration`. Keep commits focused. Pull requests should
explain the behavior change, mention configuration or database impacts, link
the relevant issue when one exists, include test commands/results, and update
the relevant document in `docs/` when behavior or constraints change. Add
screenshots only when a user-facing interface is introduced.
