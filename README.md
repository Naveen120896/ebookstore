# E-Bookstore

Full stack e-commerce capstone: Spring Boot + PostgreSQL backend and Angular frontend.

## Structure
- backend/ : Spring Boot API (Java 17, Maven, PostgreSQL, JWT)
- backend/db/ : schema.sql and seed.sql
- backend/docs/openapi.yaml : API specification
- ui/ : Angular app

## How to run
1. Database: create a database named ebookstore in PostgreSQL, then run backend/db/schema.sql and backend/db/seed.sql
2. Backend (in backend folder): set the DB_PASSWORD environment variable, then run mvn spring-boot:run
3. API docs (Swagger UI): http://localhost:8080/swagger-ui/index.html
4. UI (in ui folder): npm install, then npm start. Open http://localhost:4200
5. Backend tests: mvn test (in backend folder)

## Features
- Register and login (JWT), guest browsing
- Browse by category and brand, search, related books, recommendations
- Cart, delivery address, gift points redemption
- Card payment (simulated gateway), order confirmation
- Order history, buy it again, cancel within 48 hours
- Dark and light theme, loading states
