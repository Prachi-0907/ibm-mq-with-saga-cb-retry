# ibm-mq-with-saga-cb-retry
# IBM MQ Saga Pattern POC
 
## Overview
 
This project demonstrates a **Saga Pattern** implementation using **Spring Boot Microservices**, **IBM MQ**, **PostgreSQL**, **Resilience4j (Circuit Breaker + Retry)**, and the **Outbox Pattern**.
 
The system consists of four independent microservices, each owning its own database:
 
1. Order Producer Service
2. Inventory Service
3. Payment Service
4. Notification Service
---
 
## Architecture
 
```text
Client
   |
   v
Order Producer Service
   |
   | IBM MQ (DEV.QUEUE.1)
   v
Inventory Service
   |
   | REST + Circuit Breaker + Retry
   v
Payment Service
   |
   | REST + Circuit Breaker + Retry
   v
Notification Service
   |
   v
PostgreSQL (per-service database)
```
 
### Failure / Compensation Flow
 
```text
Inventory Failure (e.g. invalid quantity / out of stock)
        |
        v
   DEV.ORDER.FAILED  (JMS Queue)
        |
        +---> Inventory Service:  RESERVED -> RELEASED   (compensation_log)
        |
        +---> Payment Service:    SUCCESS  -> CANCELLED  (payment_events)
```
 
---
 
## Technologies Used
 
- Java 17
- Spring Boot 3
- IBM MQ (JMS)
- PostgreSQL
- Spring Data JPA / Hibernate
- Resilience4j (Circuit Breaker + Retry)
- Saga Pattern
- Outbox Pattern
- Docker
- Maven
---
 
## Services & Ports
 
| Service | Port | Responsibility |
|---|---|---|
| order-producer-service | 8080 | Accepts orders, writes to Outbox, publishes to MQ |
| order-inventory-service | 8081 | Reserves inventory, calls Payment, handles compensation |
| order-payment-service | 8082 | Processes payment, calls Notification, handles compensation |
| order-notification-service | 8083 | Sends final notification |
 
---
 
## Databases (One database per service)
 
### `order_db` — Producer Service
| Table | Purpose |
|---|---|
| `orders` | Business record of each order and its status (`PENDING` / `SUCCESS` / `FAILED`) |
| `outbox_events` | Outbox pattern staging table, relayed to MQ every 10s |
 
### `inventory_db` — Inventory Service
| Table | Purpose |
|---|---|
| `inventory_records` | Reserved inventory per order |
| `compensation_log` | Saga compensation actions (e.g. stock released) |
 
### `payment_db` — Payment Service
| Table | Purpose |
|---|---|
| `payment_records` | Payment transactions |
| `payment_events` | Payment lifecycle events, including Saga compensation (cancellations) |
 
### `notification_db` — Notification Service
| Table | Purpose |
|---|---|
| `notification_records` | Notifications sent to customers |
| `notification_events` | Notification lifecycle events |
 
---
 
## Running the Project
 
### Step 1 — Start PostgreSQL
 
Create the four databases:
 
```sql
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;
CREATE DATABASE payment_db;
CREATE DATABASE notification_db;
```
 
> Tables are **not** created manually — each service has `spring.jpa.hibernate.ddl-auto=update`, so Hibernate creates them automatically on startup based on the entities.
 
### Step 2 — Start IBM MQ (Docker)
 
```bash
docker run -d ^
  --name ibmmq ^
  -e LICENSE=accept ^
  -e MQ_QMGR_NAME=QM1 ^
  -e MQ_APP_PASSWORD=passw0rd ^
  -p 1414:1414 ^
  -p 9443:9443 ^
  icr.io/ibm-messaging/mq:latest
```
 
Verify the container is running:
 
```bash
docker ps
```
 
### Step 3 — Start the Services (in order)
 
1. `order-producer-service` → port `8080`
2. `order-inventory-service` → port `8081`
3. `order-payment-service` → port `8082`
4. `order-notification-service` → port `8083`
Each service exposes a health check:
 
```http
GET /api/producer/health
GET /api/inventory/health
GET /api/payment/health
GET /api/notification/health
```
 
---
 
## Order Processing Flow (Happy Path)
 
**1. Client places an order**
```http
POST http://localhost:8080/api/producer/send?product=Laptop&quantity=2
```
 
**2. Producer Service**
- Saves the order (`PENDING`) into `orders`
- Saves an event into `outbox_events` (Outbox Pattern — no direct MQ call inside the transaction)
**3. Outbox Relay**
- A scheduled job (`OutboxRelayService`, every 10s) reads `PENDING` events
- Publishes the message to IBM MQ queue `DEV.QUEUE.1`
- Marks the outbox event as `SENT`
**4. Inventory Service**
- Consumes the message from `DEV.QUEUE.1`
- Validates quantity and reserves inventory → saved to `inventory_records`
- Calls **Payment Service** over REST (protected by Circuit Breaker + Retry)
**5. Payment Service**
- Processes the payment → saved to `payment_records`
- Records a `PAYMENT_SUCCESS` event in `payment_events`
- Calls **Notification Service** over REST (protected by Circuit Breaker + Retry)
**6. Notification Service**
- Sends the notification → saved to `notification_records`
- Records a `NOTIFICATION_SENT` event in `notification_events`
---
 
## Failure / Saga Compensation Flow
 
**Example: bad order (invalid quantity)**
```http
POST http://localhost:8080/api/producer/send-bad
```
 
1. Inventory Service fails validation (`quantity <= 0`)
2. Message is published to `DEV.ORDER.FAILED`
3. **Inventory Service** (`SagaCompensationConsumer`) — releases the reservation:
   `RESERVED → RELEASED`, logged in `compensation_log`
4. **Payment Service** (`PaymentCompensationConsumer`) — cancels any related payment:
   `SUCCESS → CANCELLED`, logged in `payment_events`
This way, compensation is not isolated to a single service — Inventory *and* Payment both participate in undoing their part of the transaction, which is the core idea of the Saga Pattern.
 
---
 
## Circuit Breaker & Retry
 
Every service protects its one external dependency with Resilience4j:
 
| Service | Protected call | CB name |
|---|---|---|
| Producer | MQ send (via Outbox Relay) | `mqProducerService` |
| Inventory | REST call to Payment | `paymentService` |
| Payment | REST call to Notification | `notificationService` |
| Notification | Database write | `notificationDb` |
 
**Demo: simulate Notification service outage**
 
1. Stop `order-notification-service`
2. Send 4–5 orders through the pipeline
3. Observe in Payment logs:
```
   Retry attempt 1 -> failed
   Retry attempt 2 -> failed
   Retry attempt 3 -> failed
   Circuit Breaker OPEN
   Fallback triggered
```
4. Check circuit state:
```http
   GET http://localhost:8082/api/payment/circuit-status
```
```json
   { "service": "notificationService", "state": "OPEN" }
```
 
---
 
## API Reference
 
### Producer (`:8080`)
```http
POST /api/producer/send?product=Laptop&quantity=2
POST /api/producer/send-bad
GET  /api/producer/health
```
 
### Inventory (`:8081`)
```http
GET /api/inventory/records
GET /api/inventory/stats
GET /api/inventory/health
GET /api/inventory/circuit-status
```
 
### Payment (`:8082`)
```http
POST /api/payment/process
GET  /api/payment/records
GET  /api/payment/health
GET  /api/payment/circuit-status
```
 
### Notification (`:8083`)
```http
POST /api/notification/send
GET  /api/notification/records
GET  /api/notification/health
GET  /api/notification/circuit-status
```
 
---
 
## Design Patterns Implemented
 
- **Saga Pattern** — choreography-based, via MQ failure queue (`DEV.ORDER.FAILED`)
- **Outbox Pattern** — Producer writes to an outbox table instead of calling MQ inside the business transaction
- **Circuit Breaker Pattern** — Resilience4j, one breaker per service's external dependency
- **Retry Pattern** — Resilience4j, retries before the breaker opens
- **Database-per-Service Pattern** — each service owns an isolated PostgreSQL database
- **Event-Driven Architecture** — services communicate asynchronously via IBM MQ, and synchronously via REST for the payment/notification hops
---
 
## Project Structure
 
```text
four-diff-services/
 ├── order-producer-service/       (port 8080)
 ├── order-inventory-service/      (port 8081)
 ├── order-payment-service/        (port 8082)
 └── order-notification-service/   (port 8083)
```
