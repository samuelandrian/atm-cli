# ATM CLI Simulation

A clean, extensible, production-grade Command-Line Interface (CLI) simulation of a retail bank ATM, implemented in **Java 21** (fully compatible up to **Java 25**) using **Hexagonal Architecture (Ports & Adapters)** and the **Command Pattern**.

---

## Architecture and Design Decisions

This application is structured as a production backend whose interface happens to be a CLI. It strictly adheres to **SOLID** principles, ensuring that the domain model, business rules, presentation logic, and infrastructure are decoupled.

### 1. Hexagonal Architecture (Ports & Adapters)
- **Domain Layer**:
  - `Customer` is a rich domain model encapsulating account identity, balance, and outstanding debts.
  - `TransferService` (interface) and `TransferServiceImpl` (implementation) handle multi-customer transfer logic, such as offsetting transfer amounts against mutual outstanding debts.
  - `CustomerRepository` (interface) is the port defining how the domain interacts with storage.
- **Infrastructure Layer**:
  - `InMemoryCustomerRepository` is the adapter implementing the `CustomerRepository` interface, storing state in a thread-safe, localized hash map.
- **Application Layer**:
  - Decoupled into two distinct ports (interfaces) following the **Single Responsibility Principle**:
    - `AuthService` (interface) / `AuthServiceImpl` (implementation) manages session authentication state (login/logout tracking via `Session`).
    - `AtmService` (interface) / `AtmServiceImpl` (implementation) coordinates transaction operations (deposit, withdraw, transfer) and queries debt summaries, resolving user context from `AuthService`.
- **Presentation/CLI Layer**:
  - Reads input via `AtmConsoleImpl` (implementing `AtmConsole`) and delegates parsing to `AtmCommandParserImpl` (implementing `AtmCommandParser`) which translates user input into executable `Command` structures, executing against `AuthService` and `AtmService`.

### 2. Extensibility and Separation from Frameworks
- **Plain Java (No DI Frameworks)**: The application has zero startup overhead. We use Lombok `@RequiredArgsConstructor` to automatically generate constructors for final fields at compile time (constructor dependency injection), avoiding manual constructor boilerplate without introducing runtime framework dependency injection overhead (like Spring).
- **Decoupled Architecture**: Migrating this code to Spring Boot or exposing it via a REST API later is as simple as adding annotations and routing HTTP requests to `AuthService` and `AtmService`.

### 3. FIFO Debt-Tracking Domain Design
- Debts are tracked as a `LinkedHashMap<String, BigDecimal>` on each `Customer` mapping the creditor's name to the amount owed, preserving the chronological order of creation (FIFO) for repayment.
- Duplication is avoided by tracking debt *only on the debtor side*. The list of debtors for a logged-in user is dynamically compiled from the repository during login, ensuring a single source of truth.
- Incoming funds (via deposit or transfer) recursively trigger debt repayments in FIFO order. If a creditor also has debts, payments cascade automatically.

### 4. Design Decision: Unified Customer Model vs. Separate BankAccount Model
- **For this challenge**: We chose to combine the customer identity and account balance into a single `Customer` model. Introducing separate database tables or entities for `Customer` and `BankAccount` (or `Account`) would add mapping boilerplate (YAGNI) without serving any requirement of this CLI simulation.
- **Production System Design**: In a real-world production banking environment, these would be separated:
  - **`Customer` table**: Manages identity (name, email, KYC verification, credentials).
  - **`BankAccount` table**: Manages financial data (account number, balance, interest rates, status). This supports one-to-many relationships (a single customer holding multiple savings, checking, or credit accounts).

---

## Project Structure

```text
atm-cli
│   pom.xml
│   start.sh
│   README.md
│
└───src
    ├───main
    │   └───java
    │       └───io
    │           └───github
    │               └───samuelandrian
    │                   │   Main.java
    │                   │
    │                   ├───cli
    │                   │       AtmCommandParser.java (interface)
    │                   │       AtmCommandParserImpl.java (impl)
    │                   │       AtmConsole.java (interface)
    │                   │       AtmConsoleImpl.java (impl)
    │                   │       Command.java
    │                   │       CommandResult.java
    │                   │
    │                   ├───application
    │                   │       AtmService.java (interface)
    │                   │       AtmServiceImpl.java (impl)
    │                   │       AuthService.java (interface)
    │                   │       AuthServiceImpl.java (impl)
    │                   │       Session.java
    │                   │       DebtInfo.java
    │                   │
    │                   ├───domain
    │                   │   ├───model
    │                   │   │       Customer.java
    │                   │   │       Repayment.java
    │                   │   │
    │                   │   ├───repository
    │                   │   │       CustomerRepository.java
    │                   │   │
    │                   │   └───service
    │                   │           TransferResult.java
    │                   │           TransferService.java (interface)
    │                   │           TransferServiceImpl.java (impl)
    │                   │
    │                   ├───infrastructure
    │                   │   └───repository
    │                   │           InMemoryCustomerRepository.java
    │                   │
    │                   └───exception
    │                           AtmException.java
    │                           CustomerNotFoundException.java
    │                           CustomerNotLoggedInException.java
    │                           InsufficientBalanceException.java
    │                           InvalidAmountException.java
```

---

## Setup and Run Instructions

### Prerequisites
- JDK 21 (Supports up to JDK 25)
- Maven (installed in system path)

### Build and Run via `start.sh`
The project provides a `start.sh` script at the root:
```bash
./start.sh
```
This script will build the application using Maven (skipping test cycles for quick launch) and run the resulting executable JAR `target/atm-cli.jar`.

### Running Tests
To execute the suite of unit and integration tests:
```bash
mvn test
```

---

## Testing Strategy
- **Unit Tests**: Coverage for domain models (`CustomerTest`), transfer operations (`TransferServiceTest`), authentication services (`AuthServiceTest`), and transaction services (`AtmServiceTest`).
- **CLI Integration Test**: `CLIIntegrationTest` simulates the exact command sequences described in the requirements' example session and verifies that the output matches the required terminal outputs.
