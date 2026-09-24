# Memecoin Intelligence Bot — Project Blueprint

> **Estado:** diseño del MVP / paper intelligence  
> **Ejecución inicial:** local  
> **Trading real:** deshabilitado  
> **Core de negocio:** Java + Spring Boot  
> **Integraciones rápidas/puntuales:** TypeScript  
> **Interfaz operativa:** Telegram  
> **Arquitectura:** Monolito modular + DDD ligero + Hexagonal + Event-Driven interno

---

## 1. Visión del producto

Construir un bot local de **inteligencia para memecoins en Solana** que observe múltiples fuentes, normalice los datos y produzca señales explicables por Telegram.

En esta primera etapa el bot **NO compra ni vende**. Su trabajo es responder de forma fiable y con baja latencia:

1. ¿Qué tokens están surgiendo o se están volviendo elegibles?
2. ¿Qué está ocurriendo en el mercado alrededor de esos tokens?
3. ¿Qué wallets relevantes están comprando, vendiendo o moviendo capital?
4. ¿Esas wallets han mostrado históricamente un comportamiento rentable?
5. ¿Qué eventos sociales relevantes están ocurriendo?
6. ¿Coinciden señales de mercado, wallets y social?
7. ¿Qué tan fresca y confiable es cada señal?

La capa futura de decisión (por ejemplo, un modelo de decisión como Jev, reglas cuantitativas u otro modelo) **no forma parte del Core de inteligencia**. Consumirá la información producida por este Core.

---

# 2. Alcance

## 2.1 Incluido en MVP

- Descubrimiento y seguimiento de tokens de Solana.
- Market Intelligence.
- Wallet Intelligence.
- Social Intelligence.
- Registro histórico de eventos y snapshots.
- Sistema de scoring descriptivo para priorizar señales.
- Alertas y consultas mediante Telegram.
- Ejecución local.
- PostgreSQL para persistencia.
- Redis opcional para caché/eventos efímeros.
- Integraciones Solana mediante APIs/RPC/WebSockets.
- TypeScript únicamente cuando el ecosistema Solana o una integración lo justifique.
- Observabilidad básica.
- Replay de eventos para validación.
- Paper intelligence: señales sin ejecución financiera.

## 2.2 Fuera de alcance inicial

- Compra o venta automática.
- Custodia de private keys.
- Firma de transacciones.
- Conexión de una wallet con fondos reales.
- Copy trading automático.
- Sniping automático.
- Microservicios.
- Kubernetes.
- Dashboard web.
- Modelo generativo en el camino crítico.
- Autorización para que un modelo modifique reglas de riesgo.
- Promesas de rentabilidad.

---

# 3. Principios de arquitectura

## 3.1 Monolito modular

El backend Java será una sola aplicación desplegable, pero dividida en módulos con límites claros.

No se utilizarán microservicios en el MVP.

Motivos:

- ejecución local;
- menor complejidad operativa;
- transacciones y debugging más simples;
- menor latencia interna;
- menor costo;
- posibilidad de extraer módulos posteriormente si existe una necesidad real.

## 3.2 DDD ligero

Los módulos representan capacidades del negocio, no capas técnicas arbitrarias.

Bounded Contexts iniciales:

- `Token Discovery`
- `Market Intelligence`
- `Wallet Intelligence`
- `Social Intelligence`
- `Signal Intelligence`
- `Notification & Control`
- `Historical Intelligence`

## 3.3 Arquitectura hexagonal

Cada módulo separará:

- `domain`: reglas y modelos del dominio;
- `application`: casos de uso;
- `ports`: contratos;
- `infrastructure`: implementaciones externas.

El dominio no debe conocer Telegram, Helius, PostgreSQL, HTTP, WebSocket ni SDKs externos.

## 3.4 Event-Driven interno

Los módulos se comunican principalmente mediante eventos internos.

Ejemplos:

- `TokenDiscovered`
- `TokenEligibilityChanged`
- `MarketMomentumDetected`
- `LiquidityChanged`
- `WalletSwapDetected`
- `SmartWalletEnteredToken`
- `SmartWalletExitedToken`
- `SocialEventDetected`
- `CrossSignalDetected`
- `AlertRequested`

En el MVP se puede utilizar Spring Application Events o un bus interno propio. No introducir Kafka inicialmente.

## 3.5 Datos antes que modelos

La arquitectura debe funcionar aunque no exista Jev, GPT, ML o cualquier modelo externo.

La secuencia es:

`Sources -> Intelligence Core -> Normalized Signals -> Decision Layer (futuro) -> Risk -> Execution (futuro)`

---

# 4. Arquitectura de alto nivel

```text
                         ┌─────────────────────┐
                         │      TELEGRAM       │
                         │ alerts / commands   │
                         └──────────┬──────────┘
                                    │
                           Telegram Adapter
                                    │
┌───────────────────────────────────▼───────────────────────────────────┐
│                      JAVA / SPRING BOOT CORE                         │
│                                                                       │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────────┐    │
│  │ Token Discovery│  │Market Intel.   │  │ Wallet Intelligence  │    │
│  └───────┬────────┘  └───────┬────────┘  └──────────┬───────────┘    │
│          │                   │                       │                 │
│          └───────────────────┼───────────────────────┘                 │
│                              │                                         │
│                    ┌─────────▼─────────┐                               │
│                    │ Signal Intelligence│                              │
│                    │ normalize/correlate│                             │
│                    └─────────┬─────────┘                               │
│                              │                                         │
│                    ┌─────────▼──────────┐                              │
│                    │ Historical Intel.  │                              │
│                    │ snapshots / replay │                              │
│                    └────────────────────┘                              │
│                                                                       │
│  ┌─────────────────────┐                                              │
│  │ Social Intelligence │──────────────────────────────┐               │
│  └─────────────────────┘                              │               │
└───────────────────────────────────────────────────────┼───────────────┘
                                                        │
                    ┌───────────────────────────────────▼────────────┐
                    │ TypeScript Integration Workers (optional)     │
                    │ Solana parsing / SDK-specific adapters         │
                    └────────────────────────────────────────────────┘

Sources:
Solana RPC/WebSocket | Helius-like provider | market APIs | public social APIs

Storage:
PostgreSQL | optional Redis
```

---

# 5. Bounded Contexts / módulos de negocio

## 5.1 Token Discovery

### Responsabilidad

Descubrir tokens que merezcan ser observados.

No decide comprar.

### Entradas

- nuevos pools;
- tokens recientemente activos;
- volumen;
- liquidez;
- antigüedad;
- número de traders;
- metadata del token;
- actividad reciente.

### Salidas

`TokenDiscovered`

`TokenEligibilityChanged`

### Entidades

#### Token

- `tokenId`
- `mintAddress`
- `symbol`
- `name`
- `decimals`
- `createdAt`
- `firstSeenAt`
- `status`

#### TokenCandidate

- `tokenId`
- `eligibilityScore`
- `reasons`
- `observedAt`
- `expiresAt`

### Reglas iniciales

Los umbrales serán configurables y experimentales.

Ejemplos:

- liquidez mínima;
- volumen mínimo;
- antigüedad;
- actividad mínima;
- slippage estimado;
- disponibilidad de ruta de intercambio.

No codificar valores mágicos directamente en las clases de dominio.

---

# 6. Market Intelligence

## Pregunta de negocio

**¿Qué está haciendo el mercado?**

### Responsabilidades

- recibir precios y operaciones;
- calcular ventanas temporales;
- volumen;
- volatilidad;
- liquidez;
- presión compradora/vendedora;
- traders únicos;
- aceleración del volumen;
- momentum;
- cambios abruptos;
- detectar anomalías.

### Objetos principales

#### MarketSnapshot

```text
token
timestamp
price
liquidityUsd
volume1m
volume5m
volume15m
priceChange1m
priceChange5m
priceChange15m
uniqueBuyers5m
uniqueSellers5m
buyVolume5m
sellVolume5m
netFlow5m
volatility5m
dataFreshnessMs
```

#### MarketSignal

```text
signalId
token
type
strength
observedAt
expiresAt
evidence
sourceQuality
```

### Eventos

- `MomentumDetected`
- `VolumeSpikeDetected`
- `LiquiditySpikeDetected`
- `LiquidityDropDetected`
- `BuyerAccelerationDetected`
- `MarketAnomalyDetected`

### Regla

Market Intelligence describe el comportamiento. No emite `BUY` ni `SELL`.

---

# 7. Wallet Intelligence

## Pregunta de negocio

**¿Qué está haciendo el dinero relevante?**

No se debe confundir `wallet grande` con `wallet inteligente`.

### Responsabilidades

- seguir wallets seleccionadas;
- detectar swaps;
- distinguir swaps de transferencias;
- reconstruir entradas/salidas;
- calcular posiciones observables;
- analizar comportamiento histórico;
- detectar compras coordinadas;
- detectar distribución/salida;
- calcular métricas de wallet.

### Entidades

#### TrackedWallet

```text
walletAddress
label
status
firstSeenAt
trackingReason
confidence
```

#### WalletTrade

```text
wallet
token
side
amountToken
amountUsd
transactionSignature
timestamp
estimatedPrice
```

#### WalletProfile

```text
wallet
closedTrades
winRate
realizedPnl
averageReturn
medianReturn
maxObservedDrawdown
averageHoldingTime
lastCalculatedAt
sampleSize
confidence
```

Las métricas solo se mostrarán cuando exista una muestra suficiente. Una wallet con una operación exitosa no se clasifica automáticamente como smart wallet.

### Eventos

- `WalletSwapDetected`
- `TrackedWalletEnteredToken`
- `TrackedWalletIncreasedPosition`
- `TrackedWalletReducedPosition`
- `TrackedWalletExitedToken`
- `MultipleTrackedWalletsEnteredToken`

---

# 8. Social Intelligence

## Pregunta de negocio

**¿Qué está ocurriendo socialmente que podría ser relevante para este mercado?**

### Alcance inicial

Lista explícita y configurable de fuentes/cuentas públicas.

No intentar monitorear todo Internet.

### Responsabilidades

- obtener publicaciones/eventos;
- deduplicar;
- registrar fuente y timestamp;
- identificar tokens/temas mencionados;
- detectar cambios de actividad;
- correlacionar temporalmente eventos con tokens.

### Entidad SocialEvent

```text
eventId
source
sourceAccount
publishedAt
detectedAt
textHash
entities
relatedTokens
eventType
confidence
sourceUrl
```

### Importante

Una mención de una persona influyente:

- no prueba asociación oficial con un token;
- no es automáticamente una señal de compra;
- no debe generar una operación por sí sola.

### Frecuencia

Preferir streaming/webhooks cuando el proveedor lo permita.

Cuando solo exista polling:

- intervalo configurable;
- inicialmente decenas de segundos, sujeto a límites de la API;
- backoff;
- deduplicación;
- control de cuota.

No fijar la arquitectura a una frecuencia concreta.

---

# 9. Signal Intelligence

Este módulo es el punto donde convergen las tres fuentes.

```text
Market Intelligence ─┐
Wallet Intelligence ─┼─> Signal Intelligence
Social Intelligence ─┘
```

### Responsabilidad

Normalizar, correlacionar y priorizar información.

No toma decisiones financieras.

### Entidad IntelligenceSignal

```text
signalId
token
timestamp
expiresAt

marketContext
walletContext
socialContext

marketStrength
walletStrength
socialStrength

overallPriority
evidence[]
dataFreshness
confidence
```

### Ejemplo

```text
Token: EXAMPLE/SOL

MARKET
Volume 5m: 4.1x baseline
Price 5m: +8.2%
Unique buyers: +143

WALLETS
3 tracked wallets entered
Net tracked-wallet flow: +$18,400

SOCIAL
Relevant event detected 2m ago

Priority: HIGH
Freshness: 1.2s
```

`HIGH` significa prioridad de análisis/alerta, **no recomendación de inversión**.

---

# 10. Historical Intelligence

Es crítico para validar el proyecto posteriormente.

### Responsabilidades

Guardar:

- eventos originales;
- snapshots;
- señales;
- decisiones futuras;
- timestamps;
- latencias;
- resultados posteriores.

Debe permitir responder:

> ¿Qué información conocía realmente el sistema a las 14:03:21?

Esto evita look-ahead bias en backtesting.

### Capacidades futuras

- event replay;
- backtesting;
- comparación Quant vs modelo de decisión;
- evaluación de señales;
- análisis de falsos positivos.

---

# 11. Capa futura de decisión

No implementar en la primera entrega.

Se define desde ahora mediante un puerto:

```java
public interface DecisionEngine {
    Decision evaluate(IntelligenceContext context);
}
```

Implementaciones futuras:

```text
RuleBasedDecisionEngine
QuantDecisionEngine
JevDecisionEngine
ExperimentalLlmDecisionEngine
```

De esta manera el Core nunca dependerá de un proveedor/modelo específico.

---

# 12. Java vs TypeScript

## Java + Spring Boot

Java es dueño de:

- dominio;
- casos de uso;
- scoring;
- correlación;
- persistencia;
- Telegram;
- configuración;
- scheduler;
- reglas;
- histórico;
- observabilidad;
- futuros risk controls.

## TypeScript

TypeScript se utiliza únicamente cuando aporta una ventaja concreta:

- SDK de Solana mejor soportado;
- parsing especializado;
- subscriptions específicas;
- integración que resulte considerablemente más simple con librerías JS/TS.

TypeScript **no contiene decisiones de negocio**.

Contrato recomendado:

```text
Java Core
   |
HTTP/gRPC/local process
   |
Solana Integration Worker (TypeScript)
```

Para el MVP puede eliminarse el worker si Java cubre correctamente la integración requerida.

---

# 13. Fuentes de datos

## On-chain

Preferencia:

1. WebSocket / streaming.
2. API incremental.
3. Polling como último recurso.

El Core no dependerá de Axiom u otra interfaz gráfica.

Axiom puede utilizarse manualmente para contrastar información durante desarrollo, pero no será una dependencia arquitectónica.

Crear puertos:

```text
BlockchainEventProvider
MarketDataProvider
WalletActivityProvider
SocialEventProvider
```

Esto permite cambiar proveedores sin modificar dominio.

---

# 14. Telegram

Telegram es una interfaz del sistema, no parte del dominio.

## 14.1 Conexión local

Para el MVP usar:

**Long Polling**

Ventajas:

- no requiere IP pública;
- no requiere dominio;
- no requiere HTTPS local;
- ideal para desarrollo.

En despliegue futuro se podrá cambiar a webhook.

## 14.2 Flujo de entrada

```text
Telegram
   ↓
Telegram API
   ↓
TelegramPollingAdapter
   ↓
CommandRouter
   ↓
Application Use Case
   ↓
Domain
```

## 14.3 Flujo de salida

```text
Domain Event
   ↓
AlertPolicy
   ↓
NotificationRequested
   ↓
TelegramNotificationAdapter
   ↓
Telegram API
   ↓
User
```

## 14.4 Seguridad

Configurar:

```env
TELEGRAM_BOT_TOKEN=
TELEGRAM_ALLOWED_CHAT_IDS=
```

Nunca incluir el token en Git.

Todo comando debe comprobar `chatId`.

Usuarios no autorizados:

```text
ignore + security log
```

## 14.5 Comandos MVP

```text
/status
/tokens
/token <symbol|mint>
/wallets
/wallet <address|alias>
/signals
/social
/watch-token <mint>
/watch-wallet <address>
/unwatch-wallet <address>
/pause-alerts
/resume-alerts
/health
/help
```

No implementar:

```text
/buy
/sell
/withdraw
```

## 14.6 Alertas

### Token emergente

```text
🟡 TOKEN ACTIVITY

TOKEN/SOL

Price 5m: +8.4%
Volume: 3.7x
Liquidity: $184K
Unique buyers 5m: 127

Reason:
Volume acceleration + buyer growth

Observed: 21:42:13
Data age: 820ms
```

### Wallet

```text
🐋 WALLET ACTIVITY

Wallet: alpha-07
Action: BUY
Token: TOKEN
Estimated size: $4,250

Historical sample: 48 closed trades
Observed win rate: 62.5%

Tx: ...
```

### Correlación

```text
⚡ CROSS-SIGNAL

TOKEN/SOL

Market: STRONG
Tracked wallets: 3 entered
Social: relevant event detected

Priority: HIGH

This is an intelligence alert, not an executed trade.
```

---

# 15. Persistencia

PostgreSQL.

Tablas iniciales:

```text
tokens
token_candidates
market_ticks
market_snapshots
market_signals

tracked_wallets
wallet_transactions
wallet_trades
wallet_positions
wallet_profiles

social_sources
social_events

intelligence_signals
signal_evidence

provider_events
processing_failures
system_metrics
```

Evitar guardar cada dato indefinidamente sin estrategia.

Definir retención y agregación para ticks de alta frecuencia.

---

# 16. Estructura del repositorio

```text
memecoin-intelligence/
│
├── README.md
├── PROJECT.md
├── HARNESS.md
├── VALIDATION.md
├── BACKLOG.md
├── docker-compose.yml
├── .env.example
│
├── core/
│   ├── pom.xml
│   └── src/main/java/com/project/memecoin/
│       │
│       ├── discovery/
│       │   ├── domain/
│       │   ├── application/
│       │   ├── ports/
│       │   └── infrastructure/
│       │
│       ├── market/
│       ├── wallet/
│       ├── social/
│       ├── signal/
│       ├── history/
│       ├── notification/
│       └── shared/
│
├── integrations/
│   └── solana-worker/
│       ├── package.json
│       └── src/
│
├── infrastructure/
│   ├── postgres/
│   └── scripts/
│
└── docs/
    ├── architecture/
    ├── adr/
    └── domain/
```

---

# 17. Requerimientos funcionales

## RF-01 — Descubrimiento

El sistema debe detectar tokens candidatos utilizando proveedores configurables.

## RF-02 — Market Intelligence

Debe mantener métricas recientes de precio, volumen, liquidez y actividad.

## RF-03 — Wallet Tracking

Debe permitir registrar wallets y observar actividad relevante.

## RF-04 — Wallet Analytics

Debe calcular métricas históricas observables sin asumir rentabilidad a partir de muestras insuficientes.

## RF-05 — Social Tracking

Debe observar fuentes sociales configuradas y registrar eventos nuevos.

## RF-06 — Correlación

Debe correlacionar señales de mercado, wallets y social por token y ventana temporal.

## RF-07 — Telegram Alerts

Debe notificar señales configuradas.

## RF-08 — Telegram Queries

Debe permitir consultar el estado del sistema.

## RF-09 — Histórico

Debe conservar suficiente información para reproducir eventos.

## RF-10 — Trading Disabled

Ningún componente del MVP debe poder firmar o transmitir una operación financiera.

---

# 18. Requerimientos no funcionales

## Latencia

Registrar para cada evento:

```text
sourceTimestamp
receivedTimestamp
processedTimestamp
alertTimestamp
```

No prometer una latencia concreta hasta medir proveedores reales.

## Resiliencia

- reconnect automático;
- exponential backoff;
- circuit breaker donde corresponda;
- deduplicación;
- idempotencia;
- dead-letter/error table para eventos fallidos.

## Seguridad

- secretos solo mediante variables de entorno;
- logs sin credenciales;
- allowlist de Telegram;
- dependencias escaneables;
- trading real inexistente en MVP.

## Observabilidad

Métricas mínimas:

```text
events_received_total
events_processed_total
events_failed_total
provider_reconnects_total
signal_count
telegram_alerts_total
event_processing_latency
data_freshness
```

---

# 19. Backlog

## EPIC 0 — Foundation

### P0
- [ ] Crear repositorio.
- [ ] Java 21 + Spring Boot.
- [ ] PostgreSQL en Docker Compose.
- [ ] Flyway.
- [ ] Configuración por profiles.
- [ ] `.env.example`.
- [ ] health endpoint.
- [ ] arquitectura modular inicial.
- [ ] pruebas unitarias base.

## EPIC 1 — Telegram

### P0
- [ ] Crear Telegram adapter.
- [ ] Long polling.
- [ ] Allowlist de chat IDs.
- [ ] `/status`.
- [ ] `/health`.
- [ ] `/help`.
- [ ] Notification port.
- [ ] mensajes estructurados.

## EPIC 2 — Market Intelligence

### P0
- [ ] Definir `MarketDataProvider`.
- [ ] Conectar primer proveedor.
- [ ] ingestión WebSocket.
- [ ] normalización.
- [ ] snapshots 1m/5m/15m.
- [ ] volumen relativo.
- [ ] buyer/seller pressure.
- [ ] liquidez.
- [ ] señales descriptivas.
- [ ] Telegram alerts.

## EPIC 3 — Token Discovery

### P0
- [ ] detectar candidatos;
- [ ] filtros configurables;
- [ ] watchlist;
- [ ] `/tokens`;
- [ ] `/token`.

## EPIC 4 — Wallet Intelligence

### P0
- [ ] registry de wallets;
- [ ] seguimiento on-chain;
- [ ] detectar swaps;
- [ ] reconstruir posición observable;
- [ ] métricas históricas;
- [ ] `/wallets`;
- [ ] `/wallet`;
- [ ] alertas.

## EPIC 5 — Social Intelligence

### P1
- [ ] `SocialEventProvider`;
- [ ] sources configurables;
- [ ] polling/streaming;
- [ ] deduplicación;
- [ ] entity/token association;
- [ ] `/social`;
- [ ] alertas.

## EPIC 6 — Signal Intelligence

### P1
- [ ] modelo unificado;
- [ ] correlación temporal;
- [ ] freshness;
- [ ] evidence;
- [ ] prioridad;
- [ ] `/signals`;
- [ ] cross-signal alerts.

## EPIC 7 — Historical / Replay

### P1
- [ ] event store lógico;
- [ ] snapshots históricos;
- [ ] replay;
- [ ] dataset export;
- [ ] métricas posteriores a señal.

## EPIC 8 — Decision Engine

### P2 — NO implementar hasta validar Core
- [ ] definir `DecisionEngine`;
- [ ] baseline rule engine;
- [ ] evaluar Jev;
- [ ] benchmark latencia;
- [ ] benchmark calidad;
- [ ] shadow mode.

## EPIC 9 — Paper Trading

### P2
- [ ] simulador;
- [ ] slippage;
- [ ] fees;
- [ ] latency simulation;
- [ ] positions;
- [ ] PnL.

## EPIC 10 — Real Trading

### FUTURE / BLOCKED
No iniciar sin aprobación explícita y validación previa.

---

# 20. Definition of Done

Una historia está terminada cuando:

- compila;
- tiene tests relevantes;
- no rompe límites de módulos;
- errores están controlados;
- secretos no aparecen en código/logs;
- métricas necesarias existen;
- documentación se actualizó;
- comportamiento puede reproducirse;
- el happy path y al menos los fallos críticos fueron probados.

---

# 21. HARNESS.md — instrucciones para la IA que programe

Copiar esta sección a `HARNESS.md`.

```markdown
# Coding Harness

## Mission

Implement the Memecoin Intelligence Bot incrementally.

The current product is an intelligence/monitoring system.
DO NOT implement real trading.

## Non-negotiable architecture

- Java 21 + Spring Boot owns all business logic.
- Modular monolith.
- Lightweight DDD.
- Hexagonal architecture.
- Internal event-driven communication.
- PostgreSQL persistence.
- Telegram is an adapter.
- TypeScript is allowed only for integration-specific functionality where it materially improves Solana/tooling support.
- No business decision logic in TypeScript.
- No microservices.
- No Kafka unless explicitly approved.
- No LLM dependency in the Core.
- No wallet private keys.
- No transaction signing.
- No real BUY/SELL endpoints or Telegram commands.

## Module rule

Each bounded context uses:

domain/
application/
ports/
infrastructure/

Domain packages MUST NOT import infrastructure packages.

## Provider rule

External providers MUST be hidden behind ports.

Examples:

MarketDataProvider
BlockchainEventProvider
WalletActivityProvider
SocialEventProvider
NotificationPort

Do not leak provider-specific DTOs into domain models.

## Money/numeric rule

Use BigDecimal for financial decimal calculations.
Never use float/double for money.

## Time rule

Use Instant for event timestamps.
Persist source, received and processed timestamps where available.

## Event rule

Events must be immutable and idempotently processable.

Every external event needs a deterministic deduplication strategy.

## Configuration rule

No API keys or secrets in source code.

Use environment variables/configuration properties.

## Telegram rule

Telegram cannot call repositories or infrastructure directly.

Telegram command:
adapter -> application use case -> domain

Telegram notification:
domain/application event -> notification port -> Telegram adapter

Always validate allowed chat IDs.

## Testing

For every business rule:
1. unit test domain behavior;
2. test application orchestration;
3. use integration tests for adapters where valuable.

Provider integrations must be mockable.

## Development workflow

Before coding a backlog item:

1. Read PROJECT.md.
2. Identify bounded context.
3. State affected modules/files.
4. State assumptions.
5. Implement smallest coherent vertical slice.
6. Add tests.
7. Run test suite.
8. Report changed files.
9. Report remaining risks/TODOs.

Do not refactor unrelated modules.

## Safety switch

REAL_TRADING_ENABLED must not exist in the MVP.

If a requested change introduces transaction signing, private-key custody,
automatic BUY/SELL, or fund transfer, stop implementation and flag that
the request belongs to the future Real Trading epic.

## Quality

Prefer explicit code over unnecessary abstractions.
Do not introduce an interface unless it protects a domain boundary,
provider boundary, or testing seam.

Do not create speculative microservices.

## Definition of completion

Never claim a task is complete unless:
- project builds;
- relevant tests pass;
- migration changes are included;
- config examples are updated;
- no secrets are committed;
- documentation matches behavior.
```

---

# 22. Validación

La validación se divide en cuatro dimensiones.

## 22.1 Validación técnica

### Objetivo

Demostrar que el Core captura correctamente la realidad observable.

### Pruebas

- comparar eventos recibidos con exploradores/fuentes independientes;
- comprobar swaps;
- comprobar timestamps;
- comprobar balances;
- comprobar duplicados;
- desconectar Internet/proveedor y validar recuperación;
- reiniciar aplicación y comprobar consistencia;
- medir pérdida de eventos;
- medir latencia.

### Criterios

No fijar números arbitrarios inicialmente.

Crear un benchmark durante la primera semana y establecer SLOs después de observar la infraestructura real.

---

## 22.2 Validación de Market Intelligence

Para cada señal almacenar el comportamiento posterior:

```text
return_1m
return_5m
return_15m
return_30m
max_favorable_excursion
max_adverse_excursion
liquidity_change
```

Esto permite responder:

> ¿Una señal de volumen/momentum realmente anticipó algo o simplemente describió algo que ya ocurrió?

---

## 22.3 Validación de Wallet Intelligence

No evaluar wallets únicamente por win rate.

Medir:

```text
sample size
realized return
median return
average return
max drawdown
holding period
entry timing
exit timing
liquidity at entry
performance after detection delay
```

La métrica clave para nuestro bot será:

> ¿Qué rendimiento habría sido observable DESPUÉS de que nosotros detectamos la operación?

No utilizar el precio original de entrada de la wallet como si nosotros pudiéramos haberlo obtenido.

---

## 22.4 Validación de Social Intelligence

Registrar:

```text
publishedAt
detectedAt
marketReactionStart
relatedToken
confidence
```

Evaluar:

- falsos positivos;
- asociaciones incorrectas de tokens;
- tiempo de detección;
- movimiento antes de detectar;
- movimiento después de detectar;
- duración del efecto.

---

# 23. Validación futura del Decision Engine

Cuando el Core tenga datos suficientes:

```text
                 SAME INPUT
                    │
          ┌─────────┴─────────┐
          │                   │
    Baseline Rules      Decision Model
          │                   │
          └─────────┬─────────┘
                    │
               Evaluation
```

El modelo nuevo debe competir contra un baseline simple.

Medir:

- precision de señales;
- recall cuando corresponda;
- retorno simulado neto;
- drawdown;
- false positives;
- latencia;
- costo por decisión;
- estabilidad.

Primero `shadow mode`.

El modelo genera decisiones pero no afecta el sistema.

Después, paper trading.

Trading real queda fuera del MVP.

---

# 24. Roadmap de implementación

## Fase 0 — Foundation

Spring Boot + PostgreSQL + migraciones + arquitectura + tests.

## Fase 1 — Telegram

Conseguir que:

```text
/status
/health
/help
```

funcionen localmente.

## Fase 2 — Market vertical slice

Una fuente real -> Java -> normalización -> PostgreSQL -> señal -> Telegram.

Este es el primer milestone importante.

## Fase 3 — Token Discovery

Crear watchlist dinámica.

## Fase 4 — Wallet Intelligence

Una wallet -> evento real -> clasificación -> Telegram.

Después escalar a múltiples wallets.

## Fase 5 — Social Intelligence

Una fuente -> evento -> deduplicación -> asociación -> Telegram.

## Fase 6 — Cross Intelligence

Correlacionar Market + Wallet + Social.

## Fase 7 — Historical Replay

Demostrar que podemos reconstruir qué sabía el bot en un instante pasado.

## Fase 8 — Dataset

Ejecutar el sistema durante un periodo suficiente para generar datos propios.

## Fase 9 — Decision Layer

Recién aquí evaluar formalmente Jev u otra tecnología.

## Fase 10 — Paper Trading

Comparar decisiones utilizando costos y latencia realistas.

---

# 25. Primer objetivo ejecutable

La primera versión que consideraremos útil debe poder hacer esto:

```text
1. Arranco PostgreSQL.
2. Arranco Spring Boot.
3. El bot se conecta a Telegram.
4. Se conecta a una fuente de datos de Solana.
5. Recibe información real.
6. Normaliza y persiste.
7. Detecta una condición de interés.
8. Me manda una alerta por Telegram.
9. /token me explica los datos actuales.
10. Puedo apagar y reiniciar el sistema sin corromper estado.
```

Todavía no existe IA ni trading.

Ese milestone demuestra que tenemos un **Core de inteligencia funcional**.

---

# 26. Principio rector

El proyecto se construirá en este orden:

```text
OBSERVAR
   ↓
ENTENDER
   ↓
VALIDAR
   ↓
DECIDIR
   ↓
SIMULAR
   ↓
y solo mucho después:
EJECUTAR
```

No saltar directamente a un modelo de decisión antes de demostrar que las entradas del sistema son fiables.

El activo principal del proyecto no será un prompt ni un modelo concreto.

Será la pipeline capaz de convertir datos de mercado, wallets y eventos sociales en información estructurada, histórica, reproducible y medible.
