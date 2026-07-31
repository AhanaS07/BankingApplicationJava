# Banking Application (Java / Spring Cloud Microservices)

Java assessment reimagined as a Spring Cloud microservices system.

This README is the shared reference for the team — follow it when creating new services so everything stays uniform.

> The original monolith code has been archived under [`Previous_Code/`](./Previous_Code) for reference.

## Recommended flow to test on Postman (after deploying all services)

All requests go through the **API Gateway** at `http://localhost:8080` — do **not** call the services on their own ports directly. The gateway validates the JWT at the edge, then injects the authenticated customer's id as the `X-Auth-Customer-Id` header before forwarding to the downstream service. Every business endpoint (customers/accounts/wallets) relies on that header, so a request that skips the gateway is rejected.

**Bring the platform up in this order** (each waits on the previous):

1. `config-server` (8888) → 2. `eureka-server` (8761) → 3. `api-gateway` (8080) → 4. `auth-service` (8081), `customer-service` (8082), `account-service` (8083), `wallet-service` (8084).

Give Eureka ~30s so the services register before sending traffic. You can confirm registration at `http://localhost:8761`.

**Step-by-step in Postman:**

1. **Register a user** — `POST http://localhost:8080/api/auth/register` with the registration body (see [Auth Service](#auth-service-port-8081)). This creates the login **and** provisions the matching Customer in customer-service in one call. The response returns `accessToken`, `refreshToken` and the linked `customerId`.
2. **Save the tokens & customerId** — copy `accessToken`, `refreshToken` and `customerId` from the response. In Postman, store them as environment/collection variables (e.g. `{{accessToken}}`, `{{customerId}}`) so later requests reuse them.
   - Tip: add a **Tests** script on the register/login request — `pm.environment.set("accessToken", pm.response.json().accessToken)` — to capture them automatically.
3. **Authenticate every following request** — on the collection (or each request) add an **Authorization** header: `Authorization: Bearer {{accessToken}}`. Using a collection-level Bearer token auth is easiest.
4. **(Optional) Log in again** — `POST /api/auth/login` if you need a fresh token pair; `POST /api/auth/refresh` to rotate an expired access token using the refresh token.
5. **Create a bank account** — `POST /api/accounts` using `{{customerId}}` as `customerId` in the body. Grab the returned `accountNumber` for the money-movement calls.
6. **Move money on the account** — deposit, withdraw, transfer, and read transaction history using the `accountNumber` in the URL path.
7. **Create a wallet** — `POST /api/wallets` using `{{customerId}}`. Grab the returned `walletId`, then add money / pay bills / transfer between wallets.
8. **Log out when done** — `POST /api/auth/logout` with the refresh token to revoke it.

**Key rules to remember while testing:**

- You can only act on **your own** resources. `customerId` in a body must equal your authenticated id, and you can only read/modify accounts and wallets you own (someone else's id returns `404`, never a `403`, so existence isn't leaked).
- The one exception is user registration (`POST /api/auth/register`), which is unauthenticated. The downstream customer-provisioning endpoint (`POST /api/customers`) is internal-only (guarded by a shared API key the gateway strips from inbound traffic) — you cannot call it directly from Postman.
- Every successful business response is wrapped in a common envelope: `{ "success": true, "message": "...", "data": { ... } }`. Auth responses return the token/user objects directly.

---


## Team conventions (read before creating a service)

- **Create a `common` (CommonDTO) library and an Exception library**, and pull them in **as dependencies** wherever a service needs the shared DTOs / error model. Don't re-declare DTOs or exception classes per service — depend on the shared jar.
- **Switch each service to Spring Boot `3.5.16` right after generating it** from Spring Initializr (Initializr no longer lists 3.5.x — generate on a shown version, then pin the parent to `3.5.16`).
- **Keep the whole app on one Spring Cloud release train.** Spring Boot `3.5.16` pairs with Spring Cloud **`2025.0.3`** (those target Spring Boot 4.0 and will fail at runtime with relocated-class errors such as `ServerProperties`).
- **Java 21** across all modules.

## Repository layout

```
BankingApplicationJava/               
│
├── config-server/                      ← start here (port 8888)
├── eureka-server/                      ← service discovery (port 8761)
├── api-gateway/                        ← single entry point (port 8080)
│
├── auth-service/                       ← login / JWT issuing (port 8081)
├── customer-service/                   ← Customer + Address       (port 8082)
├── account-service/                    ← Bank accounts + Transactions (port 8083)
├── wallet-service/                     ← Paytm/PhonePe wallets     (port 8084)
│
├── config-repo/                        ← the .properties/.yml files the config-server serves
│   ├── application.yml                 ← shared config for ALL services
│   ├── customer-service.yml
│   ├── account-service.yml
│   └── ...
│
└──common/ (optional)                  ← shared DTOs, exceptions, error model (a plain jar module)
```

## Service module layout

Each service module looks like the current layout, just narrower in scope:

```
account-service/
├── pom.xml
└── src/main/java/com/tnf/account/
    ├── AccountServiceApplication.java   ← @SpringBootApplication + @EnableDiscoveryClient
    ├── controller/                       ← REST endpoints (new — the web layer)
    ├── service/                          ← your BankingService logic, sliced to accounts
    ├── model/                            ← BankAccount, SavingsAccount, CurrentAccount, Transaction
    ├── repository/                       ← replaces the in-memory HashMaps
    └── exception/                        ← account-relevant exceptions
```

## Unit tests & coverage (wallet-service)

`wallet-service` has 98 unit tests with JaCoCo coverage and SonarQube analysis.
Nothing needs to be running — no Mongo, no Eureka, no config-server.

### Step 1 — run the tests

```bash
cd wallet-service
./mvnw clean verify
```

Expected output near the end:

```
Tests run: 98, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

`verify` fails the build if line coverage falls below 90% or branch coverage below 85%.
Use `./mvnw test` if you only want the tests without the coverage report.

### Step 2 — check the coverage report

```bash
open target/site/jacoco/index.html
```

Currently 100% line and branch coverage. Click a class to see line-by-line
green/red highlighting.

### Step 3 — send it to SonarQube (optional)

Needs a SonarQube server running. To start one with Docker:

```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community
```

Wait for it to boot, then open http://localhost:9000, log in with `admin` / `admin`,
set a new password, and generate a token under **My Account → Security**. Then:

```bash
./mvnw clean verify sonar:sonar -Dsonar.token=<your-token>
```

Results appear at http://localhost:9000. The host URL, project key and JaCoCo report
path are already configured in `wallet-service/pom.xml` — add `-Dsonar.host.url=...`
only if your server is somewhere else.

### What the tests cover

| Test class | What it checks |
|---|---|
| `service/WalletServiceTest` | The money rules — max balance 50,000, daily spend limit 20,000, the daily counter resetting on a new day, and transfers rolling back the debit when the credit fails |
| `controller/WalletControllerTest` | That every endpoint checks ownership, and that a wallet belonging to someone else returns 404 (not 403) so wallet IDs can't be guessed |
| `exception/GlobalExceptionHandlerTest` | Each exception maps to the right HTTP status (404 / 403 / 422 / 503 / 409 / 500) |
| `config/FeignClientConfigTest` | The caller's identity header is forwarded to customer-service, and never faked when it's missing |
| `entity/WalletTest` | The `Wallet` constructor, `toString()`, and that `WalletType` still matches the values the API accepts |
| `WalletServiceApplicationTests` | The Spring context starts and all beans wire up |

## Ports at a glance

| Service            | Port |
|--------------------|------|
| config-server      | 8888 |
| eureka-server      | 8761 |
| api-gateway        | 8080 |
| auth-service       | 8081 |
| customer-service   | 8082 |
| account-service    | 8083 |
| wallet-service     | 8084 |

## API Endpoints

All paths below are reached **through the gateway** at `http://localhost:8080`. Unless noted otherwise, every endpoint requires an `Authorization: Bearer <accessToken>` header, and the gateway injects `X-Auth-Customer-Id` for the downstream service automatically — you never set it yourself.

Business responses (customer/account/wallet) are wrapped in the shared envelope:

```json
{ "success": true, "message": "Account created successfully", "data": { ... } }
```

### Auth Service (port 8081)

Base path `/api/auth`. Registration, login and the JWT lifecycle. These endpoints are **public** (no bearer token needed) except `/profile` and `/validate`.

| Method & Path | Description | Auth | Request body | Returns |
|---|---|---|---|---|
| `POST /api/auth/register` | Registers a new user and provisions the matching Customer in customer-service in one call. | Public | `RegisterRequest` | `201` `JwtResponse` — access + refresh token pair and the linked `customerId`. `409` if username/email exists, `400` on validation failure. |
| `POST /api/auth/login` | Authenticates credentials and issues a token pair. | Public | `LoginRequest` | `200` `JwtResponse`. `401` on invalid credentials. |
| `POST /api/auth/refresh` | Rotates the refresh token and issues a new access token. | Public | `RefreshTokenRequest` | `200` `RefreshTokenResponse` (new `accessToken` + `refreshToken`). `401` if expired/revoked, `404` if not found. |
| `POST /api/auth/logout` | Revokes the supplied refresh token. | Public | `RefreshTokenRequest` | `204` No Content. |
| `GET /api/auth/profile` | Returns the authenticated user's profile. | Bearer | — | `200` `UserResponse`. `401` if token missing/invalid. |
| `GET /api/auth/validate` | Validates the access token and returns the resolved principal; used by other services. | Bearer | — | `200` `UserResponse`. `401` if token missing/invalid. |

**`RegisterRequest`**

```json
{
  "username": "jdoe",
  "email": "jdoe@example.com",
  "password": "P@ssw0rd!",
  "roles": ["ROLE_USER"],
  "firstName": "John",
  "lastName": "Doe",
  "phone": "9876543210",
  "address": {
    "line1": "12 Main St", "line2": "Apt 4", "city": "Bengaluru",
    "state": "KA", "zip": "560001", "country": "India"
  }
}
```

- `username` 3–50 chars, letters/digits/`. _ -` only. `email` a valid address. `password` ≥ 8 chars with an uppercase, lowercase, digit and special char. `phone` exactly 10 digits. `roles` and `address` optional (defaults to `ROLE_USER`).

**`LoginRequest`** — `{ "username": "jdoe", "password": "P@ssw0rd!" }`

**`RefreshTokenRequest`** — `{ "refreshToken": "<token>" }` (used by both refresh and logout)

**`JwtResponse`** — `{ "accessToken", "refreshToken", "tokenType": "Bearer", "username", "customerId", "roles": [...] }`

### Customer Service (port 8082)

Base path `/api/customers`. Owns the Customer profile + Address. A caller may only touch **their own** profile (`id` must equal the authenticated customer id).

| Method & Path | Description | Auth | Request body | Returns |
|---|---|---|---|---|
| `POST /api/customers` | **Internal only.** Provisions a Customer; called by auth-service during registration with the shared internal API key. Not callable from Postman (the gateway strips the key). | Internal key | `CustomerDto` | `201` `ApiResponse<CustomerDto>`. `401`/`403` without the internal key. |
| `GET /api/customers/{id}` | Fetches a customer by id (only your own). | Bearer | — | `200` `ApiResponse<CustomerDto>`. |
| `GET /api/customers` | Returns the caller's own profile (scoped to the caller, not all customers). | Bearer | — | `200` `ApiResponse<List<CustomerDto>>` (single element). |
| `PUT /api/customers/{id}` | Updates a customer (only your own). | Bearer | `CustomerDto` | `200` `ApiResponse<CustomerDto>`. |
| `DELETE /api/customers/{id}` | Deletes a customer (only your own). | Bearer | — | `200` `ApiResponse<Void>`. |

**`CustomerDto`**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "jdoe@example.com",
  "phone": "9876543210",
  "address": {
    "line1": "12 Main St", "line2": "Apt 4", "city": "Bengaluru",
    "state": "KA", "zip": "560001", "country": "India"
  }
}
```

- All name/email/phone fields required (`phone` exactly 10 digits). `address` fields `line1`, `city`, `state`, `zip`, `country` required; `line2` optional. `id` is server-assigned (omit on create).

### Account Service (port 8083)

Base path `/api/accounts`. Bank accounts (SAVINGS / CURRENT) and transactions. A caller may only act on accounts belonging to their own customer id; an account owned by someone else returns `404` (not `403`).

| Method & Path | Description | Auth | Request body | Returns |
|---|---|---|---|---|
| `POST /api/accounts` | Opens a SAVINGS or CURRENT account for the caller's own customer. | Bearer | `CreateAccountRequest` | `201` `ApiResponse<BankAccountDto>` — includes the generated `accountNumber`. |
| `GET /api/accounts/{accountNumber}` | Fetches one account you own. | Bearer | — | `200` `ApiResponse<BankAccountDto>`. `404` if missing/not yours. |
| `GET /api/accounts/customer/{customerId}` | Lists all accounts for your own customer id. | Bearer | — | `200` `ApiResponse<List<BankAccountDto>>`. |
| `POST /api/accounts/{accountNumber}/deposit` | Credits the account. | Bearer | `AmountRequest` | `200` `ApiResponse<BankAccountDto>` (updated balance). |
| `POST /api/accounts/{accountNumber}/withdraw` | Debits the account (respects minimum-balance / overdraft rules). | Bearer | `AmountRequest` | `200` `ApiResponse<BankAccountDto>`. |
| `POST /api/accounts/{accountNumber}/transfer` | Transfers from your account (path) to a target account. Only the source must be owned by you; the target may belong to anyone. | Bearer | `AccountTransferRequest` | `200` `ApiResponse<BankAccountDto>` (source account). |
| `GET /api/accounts/{accountNumber}/transactions` | Returns the account's transaction history. | Bearer | — | `200` `ApiResponse<List<TransactionDto>>`. |

**`CreateAccountRequest`**

```json
{
  "customerId": "<your customerId>",
  "accountType": "SAVINGS",
  "initialDeposit": 5000,
  "minimumBalance": 1000,
  "interestRate": 0.035,
  "overdraftLimit": 0
}
```

- `customerId` and `accountType` (`SAVINGS`|`CURRENT`) required. `minimumBalance`/`interestRate` apply to SAVINGS; `overdraftLimit` applies to CURRENT. Amounts must be ≥ 0; omitted type-specific fields take sensible defaults.

**`AmountRequest`** (deposit / withdraw) — `{ "amount": 250.00 }` (must be positive)

**`AccountTransferRequest`** — `{ "targetAccountNumber": "ACC-1002", "amount": 500.00 }`

**`BankAccountDto`** (response) — `{ "id", "accountNumber", "customerId", "accountType", "balance", "minimumBalance", "interestRate", "overdraftLimit" }`

**`TransactionDto`** — `{ "id", "accountId", "targetAccountId", "amount", "transactionType": "DEPOSIT|WITHDRAWAL|TRANSFER", "timestamp" }`

### Wallet Service (port 8084)

Base path `/api/wallets`. Paytm / PhonePe wallets. Same ownership model as accounts — a wallet owned by someone else returns `404`.

| Method & Path | Description | Auth | Request body | Returns |
|---|---|---|---|---|
| `POST /api/wallets` | Creates a PAYTM or PHONEPE wallet for the caller's own customer. | Bearer | `CreateWalletRequest` | `201` `ApiResponse<WalletDTO>` — includes the generated `walletId`. |
| `GET /api/wallets` | Lists the caller's own wallets (scoped to the caller). | Bearer | — | `200` `ApiResponse<List<WalletDTO>>`. |
| `GET /api/wallets/{walletId}` | Fetches one wallet you own. | Bearer | — | `200` `ApiResponse<WalletDTO>`. `404` if missing/not yours. |
| `GET /api/wallets/customer/{customerId}` | Lists all wallets for your own customer id. | Bearer | — | `200` `ApiResponse<List<WalletDTO>>`. |
| `POST /api/wallets/{walletId}/add-money` | Tops up the wallet. | Bearer | `AddMoneyRequest` | `200` `ApiResponse<WalletDTO>` (updated balance). |
| `POST /api/wallets/{walletId}/pay-bill` | Pays a bill from the wallet. | Bearer | `PayBillRequest` | `200` `ApiResponse<WalletDTO>`. |
| `POST /api/wallets/{walletId}/transfer` | Transfers from your wallet (path) to a target wallet. Only the source must be owned by you. | Bearer | `TransferRequest` | `200` `ApiResponse<WalletDTO>` (source wallet). |

**`CreateWalletRequest`**

```json
{ "customerId": "<your customerId>", "walletType": "PAYTM", "openingBalance": 1000 }
```

- `customerId` and `walletType` (`PAYTM`|`PHONEPE`) required. `openingBalance` ≥ 0.

**`AddMoneyRequest`** / **`PayBillRequest`** — `{ "amount": 200.00 }` (must be positive)

**`TransferRequest`** — `{ "targetWalletId": "<walletId>", "amount": 150.00 }`

**`WalletDTO`** (response) — `{ "walletId", "customerId", "walletType", "balance" }`

### API Gateway (port 8080)

The single entry point. It routes `/api/auth/**`, `/api/customers/**`, `/api/accounts/**`, `/api/wallets/**` to the matching service, validates JWTs at the edge, and applies circuit breakers. When a downstream service is unavailable the breaker forwards to a fallback that returns `503`:

| Path | Description | Returns |
|---|---|---|
| `GET /fallback/wallets` | Circuit-breaker fallback for wallet-service. | `503` `{ "message", "status" }`. |
| `GET /fallback/accounts` | Circuit-breaker fallback for account-service. | `503` `{ "message", "status" }`. |
| `GET /fallback/customers` | Circuit-breaker fallback for customer-service. | `503` `{ "message", "status" }`. |

Aggregated Swagger UI is available at `http://localhost:8080/swagger-ui.html` (per-service docs under `/api/<service>/v3/api-docs`).
