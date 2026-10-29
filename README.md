# SecurePay — Spring Boot

Java Spring Boot conversion of the SecurePay fintech backend. Behaviour-identical replacement of
the legacy PHP service: same MySQL database, same endpoints, same response envelopes.

Built from the specifications in `../securePaybackend/docs-springboot` (docs 00–10) and ported
directly from the legacy source under `../securePaybackend/app/Modules/*`.

## Stack

- Java 21, Spring Boot 3.3.x, Maven (via the bundled wrapper `./mvnw`)
- Spring Web MVC (blocking), Spring Security (opaque token), Spring Data JPA + Hibernate
- MySQL (`ddl-auto=none` — never alters the schema), HikariCP
- Bucket4j (rate limiting), Jakarta Bean Validation, Jackson

## Build & run

```bash
./mvnw clean package          # compile + run tests + build the jar
./mvnw spring-boot:run        # run locally (needs the env vars below + MySQL)
java -jar target/securepay-springboot-0.0.1-SNAPSHOT.jar
```

The Maven wrapper downloads Maven 3.9.9 on first run (no system Maven required).

## Local MySQL (Docker)

A local MySQL 8 instance runs in Docker for development. The `local` Spring profile points at it.

```bash
# Start MySQL 8 with the securepay database + user
docker run -d --name securepay-mysql \
  -e MYSQL_ROOT_PASSWORD=securepay_root \
  -e MYSQL_DATABASE=securepay \
  -e MYSQL_USER=securepay \
  -e MYSQL_PASSWORD=securepay_pass \
  -p 3306:3306 \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

# Run the app against it (profile "local" uses ddl-auto=update to bootstrap the schema)
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

# Verify
curl http://127.0.0.1:8080/actuator/health        # {"status":"UP", db: MySQL ...}
docker exec securepay-mysql mysql -u securepay -psecurepay_pass -D securepay -e "SHOW TABLES;"
```

Container lifecycle: `docker start securepay-mysql` / `docker stop securepay-mysql`.

> ⚠️ The `local` profile uses `ddl-auto=update` to create tables from the JPA entities in the fresh
> dev database. This is **development only**. Against the real legacy database, the same-DB parity
> rule applies: `prod` keeps `ddl-auto=validate` and `default` keeps `none` — the app never alters
> the production schema.

Connection: `jdbc:mysql://127.0.0.1:3306/securepay`, user `securepay` / `securepay_pass`.

### Required environment (doc 07 §7.6)

| Var | Purpose |
|-----|---------|
| `DB_HOST` `DB_PORT` `DB_DATABASE` `DB_USERNAME` `DB_PASSWORD` | MySQL connection (same DB as the legacy app) |
| `APP_KEY` | Wallet column encryption (AES-256-CBC), `base64:` form |
| `USER_ENCRYPTDATA_KEY` `USER_ENCRYPTDATA_IV` | Request/response AES-128-CBC + HMAC |
| `PASS_KEY` `OTP_KEY` `PIN_KEY` | Password / OTP / MPIN peppers |
| `GURU_HKEY` (+ other provider keys) | Provider HMAC signing |

Profiles: `dev` (verbose, swagger on), `prod` (`ddl-auto=validate`). Timezone is fixed to
`Asia/Kolkata`.

## Architecture (doc 04)

Modular monolith. One deployable app, packages mirror the legacy module boundaries.

```
com.securepay
├── SecurePayApplication
├── common/
│   ├── crypto/      AesCbcCipher, HmacUtil, LegacyCrypt (wallet AES-256-CBC), PayloadCrypto,
│   │                EncryptedPaiseConverter (JPA @Convert for w1/w2/w3/lien)
│   ├── response/    ResponseFactory — the four envelope shapes (success/error/jsonResponse/prepaidResponse)
│   ├── security/    TokenAuthenticationFilter, SecurityConfig, CurrentUser, PasswordService, TokenGenerator
│   ├── web/         PayloadDecryptionFilter ({data,hash}), BaseController, interceptors
│   ├── exception/   GlobalExceptionHandler, EnvelopeErrorController
│   ├── ratelimit/   @RateLimit + RateLimitInterceptor (throttle:x,y)
│   ├── audit/       AuditLogService
│   └── money/       Money (integer-paise convention, HALF_UP rounding)
├── config/          SecurePayProperties, Jackson/Cors/WebMvc config
└── modules/         one package per module (auth, user, wallet, commission, dmt, aeps, payout, …)
                     each: controller / service / entity / repository / dto
```

## Request/response pipeline (parity)

1. CORS (permissive, like the legacy global middleware).
2. `PayloadDecryptionFilter` — verify HMAC-SHA256 over base64 `data`, AES-128-CBC decrypt, expose
   plaintext so controllers bind normal DTOs (GET + POST).
3. `TokenAuthenticationFilter` — resolve user by `Authorization: Bearer` → `api_token` → `qr_token`.
4. `RateLimitInterceptor` — per-endpoint throttle.
5. Interceptors — active-user, IP whitelist (role `api`).
6. Controller → service → repository / provider client.
7. `ResponseFactory` — builds the exact envelope incl. `encrypted_data` / `encrypted_errors`.

## Status

- **Foundation (doc 09 Phases 1–2):** complete and tested — crypto, envelope, token auth, decryption
  filter, rate limiting, interceptors, exception handling, audit.
- **Core domain (Phase 3):** `User` (encrypted wallets + hierarchy), the W1/W2/W3 wallet ledger
  services (verbatim money logic incl. source quirks), the commission engine (`CommRule` /
  `AcommRule`), and the Auth module (signIn / secureLogin / QR).
- **DMT money split (Phase 4 start):** `DmtService.create` ports the canonical CCF/GST/TDS/commission
  split verbatim (incl. the di_tds/md_tds = 0 source quirk).
- **Transaction modules (Phase 4):** DMT, AEPS, Payout, Recharge are fully wired end-to-end
  (commission → split → wallet debit → provider call → refund-on-fail) using the provider clients
  under `integration/` (ImpsGuru, Paysprint, iPay, FingPay, Soliteck, Utkal, Abhayam, Enkash,
  Paypoint, Recharge operators, Indicore, Mplan).
- **CRUD modules (Phase 5):** News, Notification, Complains, Plan, Role, Template, Kyc, Risk
  (white/black-listed accounts), Ipay, UserDevice, Layout (Colour/SideNav), Signup (State/District)
  have real entity + repository + service + wired controllers (list/create/update/delete against
  MySQL).
- **All 44 modules (Phase 4–6):** every endpoint from doc 05 is mapped at its exact path/method and
  returns the correct envelope. The `EndpointMappingTest` asserts the full inventory (≥355 mappings)
  boots and registers. Remaining report/schema endpoints return the envelope and are filled in
  incrementally against the legacy source following the phased plan.

Source-quirk fidelity is intentional: known legacy quirks (W3 ledger reading `w2`,
`decrementW1` allowing negative, flat-type di/md re-percentaging, double-assigned transfer `gst`)
are reproduced and flagged with `// NOTE: mirrors legacy quirk` comments. Confirm with product
before "fixing".

## Tests

```bash
./mvnw test
```

- `PayloadCryptoRoundTripTest` — AES-128-CBC round-trip + HMAC hex verify.
- `LegacyCryptRoundTripTest` — wallet column `{iv,value,mac}` round-trip.
- `EndpointMappingTest` — boots the full context and asserts the endpoint inventory.
