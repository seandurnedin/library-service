# Library App — Backend

Two Spring Boot microservices for the Library App:

| Service             | Port | Role                                                                                                                                                         |
|---------------------|------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **library-service** | 8080 | Frontend-facing API gateway/orchestrator. JWT-secured. Contains `@RestController` and business logic, including sending and broadcasting live notifications. |
| **domain-service**  | 8081 | Data layer service. Exposes CRUD endpoints using `@RepositoryRestResource` and custom aggregate queries. Owns the MySQL database.                            |

**Technology Stack**

* Java 25
* Spring Boot 4.1
* Spring MVC (running each request on a virtual thread)
* Spring Data REST
* Spring Data JPA
* MySQL
* Lombok
* JWT (jjwt)
* springdoc-openapi
* JUnit 5 & Mockito

---

# Why Is the Data Layer a Separate Service?

`library-service` is the **only** service the frontend ever talks to. It handles login, business rules (borrow
limits, late fees, waitlists), and orchestration. `domain-service` just stores and serves data (users, books, loans,
payments) — it has no business rules of its own.

Keeping them separate this way means:

* **One API for the frontend.** It only ever needs to know about library-service and its JWT — domain-service doesn't
  exist as far as the frontend is concerned.
* **Data storage stays independent of business logic.** Changing a business rule doesn't touch the schema, and changing
  the schema doesn't touch business rules.
* **Smaller attack surface.** domain-service can sit on a private network with no public access, since only
  library-service ever calls it.
* **Each service can evolve on its own.** They can be changed, tested, and (if needed) scaled independently of each
  other.

---

# Assumptions

A few assumptions were made while building this to keep scope reasonable:

* **Payments are mocked.** No real payment gateway is called — a payment is marked `PAID` immediately.
* **Notifications are logged, not really sent.** No email/SMS/push provider is wired up yet; "sending" a notification
  just logs it and pushes it live over SSE.
* **library-service and domain-service trust each other completely.** There's no authentication between them — they're
  assumed to run on the same private network. Only the frontend ↔ library-service hop is secured with a JWT.
* **The live notification stream (SSE) assumes a single library-service instance.** It's held in memory, not shared, so
  it won't work correctly if you scale library-service to multiple instances.
* **`ddl-auto=update` manages the schema.** Fine for local development; a real migration tool (e.g. Flyway) would be
  needed before production.
* **Book genres are free text**, not a separate, structured entity.
* **A user has exactly one role** (`USER`, `MANAGER`, or `ADMIN`), and that role decides which tabs the frontend shows —
  there's no multi-role support.
* **The frontend both subscribes to the live SSE stream and calls `GET /api/notifications/mine` once on login** — the
  SSE stream covers anything that happens while connected, and `/mine` catches up on whatever fired while the user was
  offline.
* **The catalogue list view only needs summary fields** (title, author, genre, status) per book; full details are
  fetched on demand via `GET /api/books/{id}` only when a book is opened.
* **A user can cancel their own waitlisted reservation at any time** from a self-service menu, independent of any staff
  action.
* Duplicate usernames/ISBNs aren't a concern since both are presumably unique
* **A user can reserve any book at any time, whether or not it's currently available.** If a copy is available, it's
  taken out of general availability and held for them immediately (no need to wait in line); if not, they join a FIFO
  waitlist and are notified once a copy is held for them. Either way, "having a copy held for you" and "being notified
  a copy is ready" are the same state.
* **A manager loaning a book by username + ISBN fulfills that user's held reservation for it, if one exists**, instead
  of taking another copy out of general availability — the copy was already set aside when the hold was placed.
* **Cancelling a reservation that's actively holding a copy releases it back to general availability** and immediately
  offers it to the next person on that book's waitlist, if any.

---

# Getting Started

## Prerequisites

Install the following software before running the application:

| Software        | Version        | Notes                                                   |
|-----------------|----------------|---------------------------------------------------------|
| Java            | 25             | Required to run both Spring Boot services.              |
| Apache Maven    | 3.9.9 or later | Used to build and run the project.                      |
| MySQL Server    | Latest         | Database used by the application.                       |
| MySQL Workbench | Latest         | Recommended for managing the MySQL server.              |
| DBeaver         | Latest         | Optional database client for viewing and querying data. |
| IntelliJ IDEA   | Latest         | Recommended IDE for opening both backend projects.      |

---

## 1. Install Java 25

Download and install Java 25.

After installation, verify that Java is correctly configured by opening a terminal and running:

```bash
java -version
javac -version
```

Ensure the `JAVA_HOME` environment variable is configured and that Java has been added to your system `PATH`.

---

## 2. Install Maven

Install Apache Maven **3.9.9 or later**.

Verify the installation:

```bash
mvn -version
```

The output should display both the Maven version and Java 25.

---

## 3. Install MySQL

Install:

* MySQL Server
* MySQL Workbench

During installation, create a MySQL administrator account (for example `admin`) and remember the password.

Create a database named:

```
library_app
```

Alternatively, the application can automatically create the database if the configured user has sufficient privileges.

---

## 4. Configure the Database

Open the following file inside **domain-service**:

```
src/main/resources/application.properties
```

Update the database credentials to match your local MySQL installation:

```properties
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

Example:

```properties
spring.datasource.username=admin
spring.datasource.password=password123
```

---

## 5. Enable Data Seeding

Ensure the following property is enabled inside `application.properties`:

```properties
app.data-seed.enabled=${DATA_SEED_ENABLED:true}
```

When enabled, the application will automatically populate the database with sample data on startup.

---

## 6. Import the Projects

Open IntelliJ IDEA.

Import both projects as **Maven Projects**:

* library-service
* domain-service

Allow IntelliJ to download all Maven dependencies.

---

## 7. Start the Backend Services

Start the services in the following order:

### Step 1

Run:

```
DomainServiceApplication
```

This starts the data service on **http://localhost:8081**.

### Step 2

Run:

```
LibraryServiceApplication
```

This starts the API service on **http://localhost:8080**.

Once it's up, browse the full API and try out endpoints directly from the Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

(Raw OpenAPI spec, if you need it: `http://localhost:8080/v3/api-docs`.)

---

## 8. Run the Angular Frontend

Open the Angular project and install dependencies:

```bash
npm install
```

Start the application:

```bash
ng serve
```

The application will be available at:

```
http://localhost:4200
```

---

# Default Login Accounts

When data seeding is enabled, the following accounts are created automatically.

| Role          | Username  | Password       |
|---------------|-----------|----------------|
| Administrator | `admin`   | `ChangeMe123!` |
| Manager       | `manager` | `ChangeMe123!` |

---

# Verify the Installation

Once all services are running:

* Backend API: http://localhost:8080
* Domain Service: http://localhost:8081
* Angular UI: http://localhost:4200

The application should load with the seeded books and the default administrator and manager accounts ready for use.
