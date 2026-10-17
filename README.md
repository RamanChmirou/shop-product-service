# Product Service

Product catalog microservice of the online shop. Stores products with configurable options
(e.g. CPU/RAM for computers, color/battery/accessories for smartphones) and calculates
the unit price for a chosen configuration.

In the final system this service is called only by other services (cart-service and
user-interaction-service). The whole sales process goes through user-interaction-service.

## Tech stack

Java 21, Spring Boot 4.1, Spring Data JPA (Hibernate), PostgreSQL (JSONB for attributes),
Liquibase, MapStruct, Lombok, springdoc-openapi, JUnit 5, Mockito, Testcontainers.

## Domain model

- **Product** – name, description, type (`COMPUTER`, `SMARTPHONE`, `ELECTRONICS`), base price,
  free-form attributes (JSONB), active flag.
- **OptionGroup** – configurable group of a product (e.g. `PROCESSOR`, `RAM`, `COLOR`),
  `SINGLE` or `MULTI` selection, required or optional.
- **OptionValue** – one choice in a group with a price surcharge (`priceDelta`).
- **Price** – gross amount (price as in the shop), currency, VAT rate (`VAT_23`, `VAT_8`,
  `VAT_5`, `VAT_0`). Net amount and tax are derived.

`ELECTRONICS` products cannot have option groups.

## API

| Method | Path | Description |
|---|---|---|
| GET | `/products?page=&size=&sort=` | Paginated list of active products |
| GET | `/products?type=COMPUTER` | Paginated list filtered by type |
| GET | `/products/{id}` | Product details |
| GET | `/products/{id}/configuration` | Option groups and values |
| POST | `/products/{id}/price-quote` | Unit price for selected options |
| POST | `/products` | Create product (201) |
| PUT | `/products/{id}` | Update product and its option groups |
| DELETE | `/products/{id}` | Delete product (204) |

Errors are returned as RFC 9457 `ProblemDetail` (400 for invalid input or option selection,
404 when the product does not exist).

Swagger UI: `http://localhost:8081/swagger-ui.html`, OpenAPI JSON: `http://localhost:8081/v3/api-docs`.

## Running

The whole project (all services, database, Kafka, Keycloak) is started from the
`shop-infra` repository with a single command:

```
docker compose up --build
```

To run only this service from the IDE, start PostgreSQL from `shop-infra`
(`docker compose up -d postgres`) and run `ShopProductServiceApplication`.
It listens on port `8081` and uses database `product_db`
(user and password: `shop`). Liquibase creates the schema and loads sample products on startup.

## Tests

```
./mvnw test
```

- unit tests (Mockito): service, price quote calculator, validators, `Price`
- `@WebMvcTest`: controller (status codes, validation, error format)
- integration test: Testcontainers with PostgreSQL (Docker must be running)