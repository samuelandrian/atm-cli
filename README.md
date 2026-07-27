# ATM CLI Simulation

A clean, extensible, production-grade Command-Line Interface (CLI) simulation of a retail bank ATM, implemented in **Java 21** (fully compatible up to **Java 25**) using a **Layered Architecture** and the **Command Pattern**.

---

## Requirements

To build and run this application, you need the following software installed on your system:
- **Java Development Kit (JDK)**: Version 21 or newer (fully tested up to JDK 25.0.2).
- **Apache Maven**: Build automation tool installed in your environment path.

---

## Installation

1. **Extract or Clone the Repository**:
   Navigate to the root directory where the source code is located (`atm-cli`).

2. **Clean and Compile the Code**:
   Build the executable JAR file and verify the compilation:
   ```bash
   mvn clean package -DskipTests
   ```
   This compiles the project, runs Lombok annotation processing, and outputs the executable fat JAR to `target/atm-cli.jar`.

---

## Assumptions & Design Choices

To resolve ambiguities in the problem specifications, the following design decisions and assumptions were made:

1. **User Identity (Case-Insensitivity)**: Usernames are treated as case-insensitive (e.g. `login Alice` and `login alice` access the same customer record). This prevents duplicate accounts being created due to accidental capitalization typos.
2. **Self-Transactions**: Customers are not allowed to perform transactions to themselves (e.g. `transfer Alice 10` when logged in as Alice is rejected with a validation error).
3. **Transaction Inputs**: All transactional values (deposits, withdrawals, transfers) must be strictly positive numbers. Negative values, zero, or non-numeric strings are rejected gracefully.
4. **Circular Debt Resolution**: In complex debt loops (e.g., A owes B, B owes C, C owes A), recursive repayment cascading handles this gracefully. When money cycles back to the original payer, it is deposited directly into their balance to resolve the cycle and prevent infinite recursion.
5. **Thread-Safe Memory**: Although the CLI operates as a single-threaded local terminal, the underlying data repository (`InMemoryCustomerRepository`) utilizes thread-safe collections (`ConcurrentHashMap`). This ensures the storage adapter is ready to be reused in multi-threaded REST/Web environments.
6. **Transient Storage**: As requested by the test guidelines, the database is in-memory and resets on every fresh launch of the ATM CLI.

---

## How to Run the Application

You can start the ATM interactive terminal in one of two ways:

### Option A: Using the Startup Script (Recommended)
The project provides a startup shell script `start.sh` at the root. Run it from Git Bash or a terminal:
```bash
./start.sh
```
*Note: This script automatically detects and exports your `JAVA_HOME` from the path if it is not inherited, packages the code, and launches the app.*

### Option B: Running the JAR Executable Directly
You can run the compiled package using `java -jar`:
```bash
java -jar target/atm-cli.jar
```

---

## Supported Commands

The ATM interactive terminal supports the following commands. All commands are case-insensitive.

### 1. `login [name]`
* **Description**: Logs in a customer by their name. If the customer does not exist in the bank repository, they are automatically created with a starting balance of `$0`. Only one user can be logged in at a time.
* **Example**:
  ```text
  login Alice
  ```
* **Output**:
  ```text
  Hello, Alice!
  Your balance is $0
  ```

### 2. `deposit [amount]`
* **Description**: Deposits the specified amount of money into the logged-in customer's account. If the customer owes money to other users (creditors), the deposited money is automatically used to repay those outstanding debts in **FIFO (First-In, First-Out)** order.
* **Example**:
  ```text
  deposit 100
  ```
* **Output**:
  ```text
  Your balance is $100
  ```

### 3. `withdraw [amount]`
* **Description**: Withdraws the specified amount of money from the logged-in customer's balance. Throws an error if the customer does not have enough funds or if no user is currently logged in.
* **Example**:
  ```text
  withdraw 30
  ```
* **Output**:
  ```text
  Your balance is $70
  ```

### 4. `transfer [target] [amount]`
* **Description**: Transfers the specified amount of money to the `target` customer.
  - If the receiver (target) owes money to the sender, the transfer first offsets that debt.
  - If the sender has enough balance, the money is transferred.
  - If the sender has **insufficient balance**, the transfer still succeeds: the sender's balance is reduced to `$0`, all available cash is transferred, and the remaining unpaid amount is recorded as a **debt owed to the target**.
* **Example**:
  ```text
  transfer Bob 80
  ```
* **Output**:
  ```text
  Transferred $70 to Bob
  Your balance is $0
  Owed $10 to Bob
  ```

### 5. `logout`
* **Description**: Logs out the currently active customer and ends the session.
* **Example**:
  ```text
  logout
  ```
* **Output**:
  ```text
  Goodbye, Alice!
  ```

### 6. `exit` or `quit`
* **Description**: Exits the ATM interactive CLI terminal loop.

---

## Interactive Simulation Session

Here is an example walk-through simulating a sequential multi-customer session:

```text
$ ./start.sh

login Alice
Hello, Alice!
Your balance is $0

deposit 100
Your balance is $100

logout
Goodbye, Alice!

login Bob
Hello, Bob!
Your balance is $0

deposit 80
Your balance is $80

transfer Alice 50
Transferred $50 to Alice
Your balance is $30

transfer Alice 100
Transferred $30 to Alice
Your balance is $0
Owed $70 to Alice

deposit 30
Transferred $30 to Alice
Your balance is $0
Owed $40 to Alice

logout
Goodbye, Bob!

login Alice
Hello, Alice!
Your balance is $210
Owed $40 from Bob

transfer Bob 30
Your balance is $210
Owed $10 from Bob

logout
Goodbye, Alice!

login Bob
Hello, Bob!
Your balance is $0
Owed $10 to Alice

deposit 100
Transferred $10 to Alice
Your balance is $90

logout
Goodbye, Bob!
```

---

## How to Run the Unit Tests & Coverage

The project contains a comprehensive test suite covering 100% of all code branches and statements.

### 1. Execute Unit & Integration Tests
To compile and execute all tests:
```bash
mvn test
```

### 2. Run Tests with JaCoCo Coverage Verification
To compile, run tests, generate a code coverage report, and enforce the strict **100% coverage threshold**:
```bash
mvn clean test
```
*If a single code path or branch is not covered, the Maven build will fail.*

### 3. Viewing the Coverage Report
After running the tests, open the generated HTML coverage report in your browser to inspect statements and branches:
```text
target/site/jacoco/index.html
```

## Software Design & Layered Architecture

This application is structured as a clean, production-grade backend divided into **4 logical layers** to ensure high maintainability, separation of concerns, and ease of testing.

### 1. Presentation Layer (CLI)
* **What it does**: The "face" of the application. It is responsible for reading user keystrokes from the terminal, translating text inputs into Command objects, and printing formatted success/error messages back to the screen.
* **Core Classes**: [AtmConsoleImpl](file:///C:/Users/samuel/Documents/repository/personal/dkatalis/atm-cli/src/main/java/io/github/samuelandrian/cli/AtmConsoleImpl.java), [AtmCommandParserImpl](file:///C:/Users/samuel/Documents/repository/personal/dkatalis/atm-cli/src/main/java/io/github/samuelandrian/cli/AtmCommandParserImpl.java).

### 2. Application Layer (Workflows & Session Orchestration)
* **What it does**: The "coordinator". It coordinates user flows (login, logout, deposits, withdrawals, transfers). It maintains the active log-in state (`Session`) and loads/saves data without knowing the core mathematical business rules or console details.
* **Core Classes**: [AuthServiceImpl](file:///C:/Users/samuel/Documents/repository/personal/dkatalis/atm-cli/src/main/java/io/github/samuelandrian/application/AuthServiceImpl.java), [AtmServiceImpl](file:///C:/Users/samuel/Documents/repository/personal/dkatalis/atm-cli/src/main/java/io/github/samuelandrian/application/AtmServiceImpl.java).

### 3. Domain Layer (Core Business Rules)
* **What it does**: The "brain" of the bank. It contains pure business calculations (deposits logic, withdrawing logic, debt chronological cascading, and transfer offset balances). This layer has **no dependencies** on terminal readers, console outputs, databases, or frameworks. It is pure Java logic.
* **Core Classes**: [Customer](file:///C:/Users/samuel/Documents/repository/personal/dkatalis/atm-cli/src/main/java/io/github/samuelandrian/domain/model/Customer.java), [TransferServiceImpl](file:///C:/Users/samuel/Documents/repository/personal/dkatalis/atm-cli/src/main/java/io/github/samuelandrian/domain/service/TransferServiceImpl.java).

### 4. Storage / Infrastructure Layer (Data Memory)
* **What it does**: The "memory". It manages saving and loading customer records. Currently, it stores them in-memory since the application starts fresh on every launch.
* **Core Classes**: [InMemoryCustomerRepository](file:///C:/Users/samuel/Documents/repository/personal/dkatalis/atm-cli/src/main/java/io/github/samuelandrian/infrastructure/repository/InMemoryCustomerRepository.java).

### 2. Production Database Schema Design (SQL)
In a real-world production banking environment, we separate core customer identity from financial accounts and loans (debts). Below is the recommended relational database schema design (PostgreSQL dialect) to represent this system:

```sql
-- 1. Customers Table (Identity Management)
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Bank Accounts Table (Asset/Balance Management)
-- Relates 1-to-many with customers, enabling a user to hold multiple accounts (e.g. Savings, Checking).
CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    balance NUMERIC(15, 2) DEFAULT 0.00 NOT NULL CHECK (balance >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. Debts/Loans Table (Liability/Debt Management)
-- Tracks chronological debt relations between users.
CREATE TABLE debts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debtor_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    creditor_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_no_self_debt CHECK (debtor_id <> creditor_id)
);
```

#### How this Schema Maps to Domain Entities:

* **Customer Identity**: Handled by the `customers` table. A user logging in queries this table.
* **Balance**: Stored as the `balance` column in the `bank_accounts` table. Transactions (withdrawals, deposits) modify this row. A `CHECK (balance >= 0)` constraint prevents the account from going below zero, enforcing domain rules at the database level.
* **Loan / Debt Tracking**: Handled by the `debts` table:
  - When Alice transfers money to Bob with insufficient balance, the cash transferred matches her current balance (which goes to $0 in `bank_accounts`). The deficit amount is inserted as a new row in the `debts` table where `debtor_id` is Alice and `creditor_id` is Bob.
  - **FIFO Resolution**: When Alice deposits money, the application queries unresolved debts:
    ```sql
    SELECT * FROM debts 
    WHERE debtor_id = 'alice-uuid' 
    ORDER BY created_at ASC;
    ```
    This returns outstanding debts in the exact chronological order they were created (**FIFO**). The application cascades payments through these rows, reducing the `amount` or deleting the row once it is fully repaid.
