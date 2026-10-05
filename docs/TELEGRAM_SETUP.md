# Telegram local setup

## 1. Rotate an exposed token

Anyone who can read a bot token can control that bot. If a token was pasted in
a chat, issue tracker, terminal output, or committed file, revoke it before
configuring NevCoin:

1. Open the official `@BotFather` chat.
2. Send `/revoke`, select the bot, and confirm the revocation.
3. Send `/token`, select the bot, and copy the newly generated token.

Never reuse the revoked token.

## 2. Obtain the numeric chat ID

1. Open the bot chat and send `/start` (or any text message).
2. Make sure NevCoin and any other long-polling process for this bot are
   stopped. Telegram delivers each update to only one `getUpdates` consumer.
3. In a terminal, enter the **new** token without adding it to shell history:

   ```bash
   read -rsp "Telegram bot token: " TELEGRAM_BOT_TOKEN; echo
   export TELEGRAM_BOT_TOKEN
   curl --silent "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getUpdates"
   unset TELEGRAM_BOT_TOKEN
   ```

4. Find `result[].message.chat.id` in the JSON response. That integer is the
   chat ID. A private chat normally has a positive ID; a group or supergroup
   can have a negative ID. If `result` is empty, send another message to the
   bot and repeat the request.

## 3. Configure NevCoin

Create the local configuration file and edit it:

   ```bash
   cp .env.example .env
   ```

Set these values in `.env`:

   ```env
   TELEGRAM_ENABLED=true
   TELEGRAM_BOT_TOKEN=replace-with-the-new-token
   TELEGRAM_ALLOWED_CHAT_IDS=replace-with-the-numeric-chat-id
   ```

For several authorized chats, separate their IDs with commas. If market or
wallet alerts are enabled, configure their destinations separately with
`MARKET_ALERT_CHAT_IDS` and `WALLET_ALERT_CHAT_IDS`.

## 4. Start and verify

Start the local dependencies and the application:

   ```bash
   docker compose up -d postgres
   ./mvnw spring-boot:run
   ```

   On Windows, use `mvnw.cmd` when the Maven wrapper is available, or the
   project Maven command configured by the local environment.

Send `/help` to the bot. The allowlist is strict: only the numeric chat IDs in
`TELEGRAM_ALLOWED_CHAT_IDS` receive responses.

Available commands include `/status`, `/health`, `/help`, `/tokens`,
`/token <mint|symbol>`, `/alerts`, `/alerts-on <mint|symbol|all>`,
`/alerts-off <mint|symbol|all>`, `/signals`, `/paper-status`, `/paper-on`,
`/paper-off`, `/paper-reset`, `/wallets`, and `/wallet <address>`. Paper
trading sends simulated decision alerts and enforces configured risk limits,
but it never signs or sends a blockchain transaction.
`/social` shows normalized social events from the last 24 hours; until a
provider is configured it reports an empty state.

Never commit `.env` or paste `TELEGRAM_BOT_TOKEN` into source code, logs, or a
chat. Telegram uses long polling, so no public webhook URL is required.
