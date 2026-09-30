# Telegram local setup

1. Create a bot with `@BotFather` and copy its token into `.env`:

   ```env
   TELEGRAM_ENABLED=true
   TELEGRAM_BOT_TOKEN=replace-with-your-bot-token
   TELEGRAM_ALLOWED_CHAT_IDS=123456789
   ```

2. Start the local dependencies and the application:

   ```bash
   docker compose up -d postgres
   ./mvnw spring-boot:run
   ```

   On Windows, use `mvnw.cmd` when the Maven wrapper is available, or the
   project Maven command configured by the local environment.

3. Send `/help` to the bot. The allowlist is strict: only the numeric chat
   IDs in `TELEGRAM_ALLOWED_CHAT_IDS` receive responses.

Available commands include `/status`, `/health`, `/help`, `/tokens`,
`/token <mint|symbol>`, `/wallets`, and `/wallet <address>`.

Never commit `.env` or paste `TELEGRAM_BOT_TOKEN` into source code, logs, or
chat. Telegram uses long polling, so no public webhook URL is required.
