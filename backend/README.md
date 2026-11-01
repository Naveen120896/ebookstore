# E-Bookstore Backend (Spring Boot + PostgreSQL)

Java 17 · Maven · Spring Boot 3.5 · PostgreSQL · JWT security

## 1. Database (once)
1. In pgAdmin create database `ebookstore`.
2. Run `db/schema.sql`, then `db/seed.sql` (Query Tool on the **ebookstore** database).

## 2. Configure
Edit `src/main/resources/application.properties` and set `spring.datasource.password`
to your PostgreSQL password.

## 3. Run
```
mvn clean install
mvn spring-boot:run
```
API base URL: `http://localhost:8080/api/v1`

## 4. Test in Insomnia / browser
| # | Request | Notes |
|---|---------|-------|
| 1 | `POST /auth/register` `{"email":"a@b.com","password":"password1","fullName":"Naveen"}` | new users get 100 gift points |
| 2 | `POST /auth/login` same email/password | copy `token`; send as `Authorization: Bearer <token>` |
| 3 | `GET /categories`, `GET /brands?categoryId=1`, `GET /products?categoryId=1&q=gatsby` | public |
| 4 | `GET /products/{id}` and `/products/{id}/related` | includes `tentativeDeliveryDate` |
| 5 | `POST /cart/items` `{"productId":1,"quantity":2}` then `GET /cart` | needs token |
| 6 | `POST /addresses` `{"line1":"12 Main St","city":"Chennai","postalCode":"600001","country":"India"}` | |
| 7 | `POST /orders` `{"addressId":1,"giftPointsToRedeem":50}` | creates order, empties cart |
| 8 | `POST /orders/{id}/payment` `{"method":"CREDIT_CARD","cardNumber":"4111111111111111","cardHolder":"Naveen","expiry":"12/28","cvv":"123"}` | card ending `0000` is declined (demo) |
| 9 | `GET /orders`, `GET /products/recommendations` | order history + recommendations |
| 10 | `POST /orders/{id}/cancel` | only within 48 h |

## Business rules
- 1 gift point = 1 currency unit; redeemed at checkout, refunded on cancel.
- Stock is reduced at checkout and restored on cancel.
- `order_items.unit_price` snapshots the price at purchase.
- Only the last 4 card digits are stored.

## Git check-in
```
git init
git checkout -b feature/api-implementation
git add .
git commit -m "Implement Spring Boot API for e-commerce"
git remote add origin <your-repo-url>
git push origin feature/api-implementation
```
