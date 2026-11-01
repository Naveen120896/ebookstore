# E-Bookstore UI (Angular 21)

Talks to the Spring Boot backend at `http://localhost:8080/api/v1`
(change it in `src/app/api.config.ts`).

## Run
1. Start the backend first (`mvn spring-boot:run` in the backend folder).
2. In this folder:
   ```
   npm install
   npm start
   ```
3. Open http://localhost:4200

## Screens
| Route | What it does |
|-------|--------------|
| `/login`, `/register` | Auth (new users get 100 gift points) |
| `/products` | Search, filter by category/brand, pagination, recommendations |
| `/products/:id` | Details, tentative delivery date, related books, add to cart |
| `/cart` | Change quantity, remove, subtotal |
| `/checkout` | Pick/add address, redeem gift points, order summary |
| `/payment/:orderId` | Card payment (card ending 0000 is declined in the demo) |
| `/confirmation/:orderId` | "Your purchase is complete" + delivery dates |
| `/orders` | History, Buy it again, Pay now, Cancel (within 48 hours) |

## Structure
```
src/app/
  api.config.ts  models.ts  util.ts
  services/   auth, api, cart
  core/       auth.interceptor (adds JWT), auth.guard (protects pages)
  components/ product-card
  pages/      login, register, products, product-detail, cart, checkout, payment, confirmation, orders
```
