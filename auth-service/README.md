# Auth Service

Authentication and JWT-issuing microservice for the **Banking Application** (Spring Cloud microservices system).
Handles user registration, login, token refresh/rotation, logout and token validation, and is the identity
authority the other services (customer, account, wallet) trust.

- **Port:** `8081`
- **Registers with Eureka as:** `AUTH-SERVICE`
- **Base path:** `/api/auth`

---

## Tech stack

| Area | Choice |
|------|--------|
| Language / build | Java 21, Maven |
| Framework | Spring Boot 3.5.16, Spring Cloud 2025.0.3 |
| Security | Spring Security 6, JWT (`io.jsonwebtoken` / jjwt 0.12.6), BCrypt |
| Persistence | MongoDB (Spring Data MongoDB, auditing + indexes) |
| Discovery / config | Eureka client, Spring Cloud Config client (`optional`) |
| Observability | Actuator, Micrometer + Prometheus, Zipkin (b3) tracing, JSON logging (Logback) |
| Docs | springdoc-openapi / Swagger UI |
| Shared DTOs | `com.tnf:common-dto` (auth DTOs + shared `ErrorResponse`) |
| Tests | JUnit 5, Mockito, `@WebMvcTest`, `@DataMongoTest` (embedded Mongo) |

## Architecture

Clean, layered, constructor-injection only — no business logic in controllers:

```
controller ─▶ service (interface + impl) ─▶ repository ─▶ MongoDB
```

Entities: `User`, `Role`, `RefreshToken` (unique indexes, `@CreatedDate`/`@LastModifiedDate` auditing,
TTL index on refresh tokens). Refresh tokens are **persisted and opaque** (not stateless JWTs) so they can be
individually **revoked** and **rotated**.

## API

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | public | Create a user, return an access + refresh token pair |
| POST | `/api/auth/login` | public | Authenticate, return a token pair |
| POST | `/api/auth/refresh` | public | Rotate refresh token, issue a new access token |
| POST | `/api/auth/logout` | public | Revoke the supplied refresh token |
| GET  | `/api/auth/profile` | Bearer | Current authenticated user's profile |
| GET  | `/api/auth/validate` | Bearer | Validate an access token (used by other services) |

### Example

```bash
# Register
curl -X POST http://localhost:8081/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","email":"demo@tnf.com","password":"Str0ng@Pass1"}'

# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","password":"Str0ng@Pass1"}'

# Profile (use the accessToken from the response above)
curl http://localhost:8081/api/auth/profile -H "Authorization: Bearer <accessToken>"
```

## Configuration

JWT and Mongo settings are externalised (`security.jwt.*`, `spring.data.mongodb.*`). Key environment
variables (see `application-prod.yml`):

| Variable | Meaning |
|----------|---------|
| `JWT_SECRET` | HMAC signing secret (≥ 32 bytes / 256 bits). **Required in prod.** |
| `MONGODB_URI` | MongoDB connection string |
| `EUREKA_URI` | Eureka default zone |
| `ZIPKIN_ENDPOINT` | Zipkin spans endpoint |

Access-token lifetime defaults to 15 min, refresh-token to 7 days.

## Running locally

Prerequisites: **MongoDB** on `27017` and (optionally) a **Eureka server** on `8761`.

```bash
# 1. Ensure the shared DTO library is installed first
#    (run inside the common-dto project)
mvn clean install

# 2. Build and test the auth-service
./mvnw clean package

# 3. Run
./mvnw spring-boot:run
#    or: java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

Swagger UI: <http://localhost:8081/api/auth/swagger-ui.html>
OpenAPI JSON: <http://localhost:8081/api/auth/v3/api-docs>
Health: <http://localhost:8081/actuator/health>

## Docker

```bash
docker build -t tnf/auth-service:latest .
docker run -p 8081:8081 \
  -e MONGODB_URI="mongodb://host.docker.internal:27017/authdb" \
  -e JWT_SECRET="<a-256-bit-secret>" \
  -e EUREKA_URI="http://host.docker.internal:8761/eureka/" \
  -e SPRING_PROFILES_ACTIVE=prod \
  tnf/auth-service:latest
```

## Tests

```bash
./mvnw test
```

32 tests: service-layer (Mockito), `JwtService`, refresh-token lifecycle, controller (`@WebMvcTest`),
JWT filter, and repository slice (`@DataMongoTest` with embedded MongoDB).

## Gateway integration

Behind the API gateway (port 8080) this service is reached at `/api/auth/**`; its OpenAPI docs are aggregated
into the gateway Swagger UI as **Auth Service** (`/api/auth/v3/api-docs`).
