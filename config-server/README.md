# Config Server

Centralized configuration service for the Banking microservices system (Spring Cloud Config Server).
It serves configuration to every other service from a single Git repository, so config lives in one
place instead of being copy-pasted into each service.

## Tech stack

| | |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring Cloud | 2025.0.3 |
| Port | `8888` |
| Config backend | Git → [moktiksalgotra/config-files](https://github.com/moktiksalgotra/config-files) (`main`) |
| Service discovery | Registers with Eureka (`8761`) |

## How it works

1. On startup the server clones the `config-files` Git repo.
2. When a service asks for its config, the server returns the matching file **plus** the shared
   `application.properties`, merged with service-specific values taking precedence.
3. A service is matched by its `spring.application.name` → `<name>.properties` in the repo.

```
Service (account-service)  ──►  Config Server (8888)  ──►  Git repo (config-files)
        asks for its config          resolves + merges         account-service.properties
                                                                + application.properties
```

## Config repository layout

The [config-files](https://github.com/moktiksalgotra/config-files) repo holds one file per service:

| File | Serves | Port | Database |
|---|---|---|---|
| `application.properties` | **shared by all** (Eureka, actuator) | — | — |
| `account-service.properties` | account-service | 8081 | MongoDB `accountdb` |
| `auth-service.properties` | auth-service | 8083 | MongoDB `authdb` (+ JWT) |
| `customer-service.properties` | customer-service | 8082 | MongoDB `customerdb` |
| `wallet-service.properties` | wallet-service | 8084 | MongoDB `walletdb` |
| `api-gateway.properties` | api-gateway | 8080 | — (Eureka routing) |

## Running

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8888`. If a Eureka server is running on `8761` it will
register automatically; if not, it still starts (with harmless connection-retry warnings).

## Verifying

Fetch any service's config using the pattern `/{service-name}/{profile}`:

```bash
curl http://localhost:8888/account-service/default
```

You should get JSON containing `server.port=8081`, the MongoDB URI, and the shared Eureka settings.

## Connecting a service to this server

Each service needs `spring-cloud-starter-config` on its classpath and just two lines in its own
`application.properties`:

```properties
spring.application.name=account-service
spring.config.import=optional:configserver:http://localhost:8888
```

- `spring.application.name` **must** match the filename in the config repo.
- `optional:` lets the service still boot if the config server is down (useful during development).

---

## Guide for teammates (service owners)

This section is for whoever owns **account-service, auth-service, customer-service, wallet-service,
and api-gateway**. Follow it to make your service read its config from this server.

### 1. Add the required dependencies to your `pom.xml`

Every service needs the **same Spring Boot / Spring Cloud versions** as this repo, or the config
client will not work:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
</parent>

<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2025.0.3</spring-cloud.version>
</properties>

<dependencies>
    <!-- Reads config from the config server -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>

    <!-- Registers with Eureka -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <!-- We use MongoDB everywhere (NOT spring-boot-starter-data-jpa) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> The **api-gateway** service replaces `spring-boot-starter-data-mongodb` with
> `spring-cloud-starter-gateway`.

### 2. Keep your local `application.properties` tiny

Everything else (port, DB URI, etc.) comes from the config server, so your local file is just:

```properties
spring.application.name=account-service
spring.config.import=optional:configserver:http://localhost:8888
```

**Do not** put `server.port`, the Mongo URI, or Eureka settings here — they already live in the
config repo and would only cause drift if duplicated locally.

### 3. Name matters

Your `spring.application.name` **must exactly match** the filename in the config repo:

| Service | `spring.application.name` | Config file |
|---|---|---|
| Account | `account-service` | `account-service.properties` |
| Auth | `auth-service` | `auth-service.properties` |
| Customer | `customer-service` | `customer-service.properties` |
| Wallet | `wallet-service` | `wallet-service.properties` |
| Gateway | `api-gateway` | `api-gateway.properties` |

If the names don't match, your service gets **only** the shared `application.properties` and none of
its own settings (wrong port, no DB — it will fail to start correctly).

### 4. Verify your service picked up the config

On startup you should see a log line like:
`Fetching config from server at : http://localhost:8888`.
The value of your `server.port` in the logs should match the config repo, not `8080`.

### 5. Need a config change?

Don't hard-code it in your service. Ask the config owner (or open a PR on the
[config-files](https://github.com/moktiksalgotra/config-files) repo) to change the value there.
That keeps every environment consistent.

## System startup order

1. **Eureka server** — `8761`
2. **Config server** — `8888` (this app)
3. **Microservices** — account / auth / customer / wallet / api-gateway
