# Prompt for Claude: Account Service Generation

Act as an expert Java Enterprise Architect and Senior Spring Cloud Developer.

I need you to generate a comprehensive, production-ready codebase for the `account-service` module of our microservices-based Banking Application. Your entire response must be formatted as a single, well-structured Markdown file (`account-service.md`) containing all the necessary files, code blocks, and configurations.

### **CRITICAL PROJECT CONTEXT**
1. **Standalone Repository:** This `account-service` is housed in its **own separate repository**, not a mono-repo. The `pom.xml` must be a standalone Spring Boot configuration, not a sub-module.
2. **Strictly NO Local DTOs or Exceptions:** All services in this ecosystem share a `common` library. **Do NOT** generate any DTO classes (e.g., `AccountDTO`) or Exception classes (e.g., `AccountNotFoundException`). You must assume these are imported from `com.tnf.common.dto` and `com.tnf.common.exception`.

### Core Specifications & Versions
* **Java Version:** Java 21 (utilize records, pattern matching, switch expressions).
* **Spring Boot Version:** 3.5.16.
* **Spring Cloud Version:** 2025.0.3 (Release Train).
* **Base Package:** `com.tnf.account`
* **Server Port:** 8083
* **Service Discovery:** Configured as a Eureka Client using `@EnableDiscoveryClient`.
* **Configuration:** Configured to fetch remote configurations from a central `config-server` (port 8888).

### Architectural Rules
* **Shared Dependency:** In the `pom.xml`, include a dependency for our shared library (GroupId: `com.tnf`, ArtifactId: `common`, Version: `1.0.0`).
* **Data Persistence:** Use Spring Data JPA. Implement a clean inheritance strategy for `BankAccount`, `SavingsAccount`, and `CurrentAccount` (e.g., `InheritanceType.SINGLE_TABLE` or `JOINED` using a discriminator). Replace any in-memory HashMaps with real repository interfaces.
* **Validation & Resilience:** Include `@Valid` for incoming DTOs and `@Transactional` on all service methods modifying state (credit, debit, transfers).

### Required File Layout
Generate the full, un-abbreviated code for the following file structure:

account-service/
├── pom.xml
└── src/main/
├── resources/
│   └── bootstrap.yml (configured for config-server fetch)
└── java/com/tnf/account/
├── AccountServiceApplication.java
├── model/
│   ├── BankAccount.java (Abstract Base Entity)
│   ├── SavingsAccount.java
│   ├── CurrentAccount.java
│   └── Transaction.java
├── repository/
│   ├── BankAccountRepository.java
│   └── TransactionRepository.java
├── service/
│   ├── AccountService.java (Interface)
│   └── AccountServiceImpl.java (Implementation handling credit, debit, transfers using the shared DTOs/Exceptions)
└── controller/
└── AccountController.java (REST endpoints exposing operations to the api-gateway)

### Final Output Requirements:
* **No placeholders:** Write out complete implementations for the service logic and controllers.
* Do not leave `// TODO` comments for core logic.
* Ensure all imports for DTOs and Exceptions point to `com.tnf.common.*`.

Generate the `account-service.md` markdown file now.