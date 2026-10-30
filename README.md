# RestockRadar

AI-Based Inventory and Reorder Management System — a mobile-first web app for small shop, pharmacy, and grocery owners to track stock health and get automatic reorder recommendations.

Built with: **Spring Boot 3 · Spring MVC · Spring Data JPA · Spring Security · Thymeleaf · MySQL**

---

## 1. What's in this project

A single Spring Boot application (backend + server-rendered frontend together — no separate frontend build step needed):

```
restockradar/
├── pom.xml
├── database/
│   └── schema.sql              ← MySQL schema (matches your Physical Design doc)
├── src/main/java/com/restockradar/
│   ├── RestockRadarApplication.java
│   ├── config/                 ← Spring Security setup
│   ├── model/                  ← 8 JPA entities (Users, Categories, Products, Sales,
│   │                              Inventory, ReorderRecommendation, StockAlert, Report)
│   ├── repository/             ← Spring Data JPA repositories
│   ├── service/                ← Business logic: stock health classification,
│   │                              reorder recommendation engine, alerts, reports
│   ├── controller/              ← MVC controllers (pages + form handling)
│   └── dto/                    ← Form-backing objects
└── src/main/resources/
    ├── application.properties  ← DB connection config
    ├── templates/              ← Thymeleaf HTML pages
    └── static/css/style.css    ← Styling
```

## 2. Features implemented

- **Owner registration & login** (Spring Security, BCrypt-hashed passwords)
- **Product catalog** — one-time setup: name, category, cost price, selling price, stock, minimum stock
- **Categories** management
- **Daily sales logging** — logging a sale automatically reduces stock and re-runs the stock/reorder calculations
- **Dashboard** — color-coded stock health (Low / Healthy / Overstock) with live counts
- **Reorder Recommendation Engine** — pure math, no external ML:
  - Average daily sales = units sold in the last 30 days ÷ 30
  - Days to stockout = current stock ÷ average daily sales
  - Suggested quantity = enough stock for 14 days, minus what you already have
  - Reorder date = today + (days to stockout − 3-day supplier lead time)
  - Also refreshes automatically every night via a scheduled job
- **Stock Alerts** — automatically raised/resolved when a product goes Low or Overstock
- **Reports** — revenue per product, top seller, dead stock (no sales in 30 days)

## 3. Prerequisites

- **Java 17** or newer (Java 21 also works)
- **Maven** (or use the Maven wrapper built into STS)
- **MySQL 8.x** running locally (or update `application.properties` to point elsewhere)
- **Spring Tool Suite (STS)** 4.x

## 4. Import into STS

1. Unzip this project anywhere on disk.
2. Open STS → `File` → `Import...` → `Maven` → `Existing Maven Projects`.
3. Browse to the unzipped `restockradar` folder → select it → `Finish`.
4. STS will download all dependencies automatically (needs internet access the first time).

## 5. Set up the database

Option A — let Hibernate create the tables automatically (easiest):
1. In MySQL, just create an empty database: `CREATE DATABASE restockradar_db;`
2. Leave `spring.jpa.hibernate.ddl-auto=update` in `application.properties` (already set).
3. Run the app — tables are created on first startup.

Option B — run the provided schema yourself for full control over indexes:
```sql
SOURCE /path/to/restockradar/database/schema.sql;
```

Then update credentials in `src/main/resources/application.properties` if your MySQL username/password differ from the defaults (`root` / `root`):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/restockradar_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
```

## 6. Run the application

In STS: right-click the project → `Run As` → `Spring Boot App`.

Or from a terminal in the project folder:
```bash
mvn spring-boot:run
```

Then open: **http://localhost:8080**

You'll land on the login page → click "Create a shop account" to register your first shop owner login → you'll be taken to the dashboard.

## 7. Typical first-time flow

1. Register a shop account.
2. Go to **Categories** → add a couple of categories (optional).
3. Go to **Products** → add your products with cost price, selling price, current stock, and minimum stock.
4. Go to **Daily Sales** → log a sale — watch stock drop and the dashboard update.
5. Go to **Reorders** → click "Refresh recommendations" to run the reorder engine.
6. Go to **Alerts** → see any Low/Overstock alerts raised automatically.
7. Go to **Reports** → click "Generate report" to see revenue, top sellers, and dead stock.

## 8. Notes

- Passwords are hashed with BCrypt — never stored in plain text.
- Each shop owner only ever sees their own products, sales, alerts, and reports (enforced in the service/repository layer).
- The reorder engine's thresholds (30-day sales window, 14-day target stock, 3-day lead time) are defined as constants at the top of `InventoryService.java` if you want to tune them.
- `spring.jpa.hibernate.ddl-auto=update` is convenient for development. For a production deployment, switch to `validate` and manage schema changes via the provided `schema.sql` / a migration tool instead.
