# Banking Application (Java / Spring Cloud Microservices)

Java assessment reimagined as a Spring Cloud microservices system.

This README is the shared reference for the team — follow it when creating new services so everything stays uniform.

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
├── common/ (optional)                  ← shared DTOs, exceptions, error model (a plain jar module)
│
├── docker-compose.yml                  ← Prometheus, Grafana, ELK, Zipkin/Tempo 
└── observability/
    ├── prometheus.yml
    ├── logstash.conf
    └── grafana/…
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
