# Ejecución y despliegue

NevCoin se ejecuta como un proceso Spring Boot con PostgreSQL. Redis queda
opcional mientras no se habilite un caso de uso que lo requiera.

## Local

1. Copia `.env.example` a `.env` y completa las credenciales. No subas `.env`.
2. Inicia PostgreSQL: `docker compose up -d postgres`.
3. Arranca el bot: `./mvnw spring-boot:run`.

Flyway aplica las migraciones automáticamente. Comprueba
`http://localhost:8080/actuator/health` y prueba Telegram con `/help`.

## Docker

```bash
docker build -t nevcoin .
docker run --env-file .env -p 8080:8080 nevcoin
```

El contenedor necesita PostgreSQL accesible mediante `DATABASE_URL`,
`DATABASE_USERNAME` y `DATABASE_PASSWORD`.

## Render, Railway o Fly.io

Puedes desplegar esta imagen como un Web Service/worker y usar PostgreSQL
administrado. Configura como mínimo:

```text
TELEGRAM_ENABLED=true
TELEGRAM_BOT_TOKEN=...
TELEGRAM_ALLOWED_CHAT_IDS=...
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
```

Añade Helius y los flags Market/Social/Wallet solo cuando actives esos
módulos. Telegram usa long polling, así que no requiere dominio público ni
webhook; el servicio debe permanecer encendido. En Render selecciona
`Dockerfile`, expón `8080` y usa `/actuator/health` como health check.

El build actual incluye Market, Social, Wallet, señales cross-domain, paper
trading, replay y outbox durable de Telegram. La wallet real y las órdenes
BUY/SELL reales con Jupiter siguen fuera de alcance: no hay claves privadas ni
transacciones de mainnet.
