# rest-graphql-grpc-example

A single Spring Boot application that implements all three modern API paradigms on the same domain model (Products), so you can compare them side-by-side and understand when to use each.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Architecture Overview](#architecture-overview)
3. [REST API](#rest-api)
4. [GraphQL API](#graphql-api)
5. [gRPC API](#grpc-api)
6. [Side-by-Side Comparison](#side-by-side-comparison)
7. [When to Use What](#when-to-use-what)
8. [Running the Tests](#running-the-tests)
9. [Project Structure](#project-structure)

---

## Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.9+ |
| grpcurl (optional) | latest |

```bash
# Clone and run
git clone git@github.com:kodtodya/rest-graphql-grpc-example.git
cd rest-graphql-grpc-example
mvn spring-boot:run
```

The app starts with 5 seed products. Three APIs are live immediately:

| API | URL | Protocol |
|-----|-----|----------|
| REST | http://localhost:8080/api/products | HTTP/1.1 + JSON |
| GraphQL | http://localhost:8080/graphiql | HTTP/1.1 + JSON |
| gRPC | localhost:9090 | HTTP/2 + Protobuf |
| H2 Console | http://localhost:8080/h2-console | — |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot App                             │
│                                                                 │
│  ┌─────────────┐  ┌─────────────────┐  ┌───────────────────┐  │
│  │ REST Layer  │  │  GraphQL Layer  │  │   gRPC Layer      │  │
│  │             │  │                 │  │                   │  │
│  │ @RestCtrl   │  │ @QueryMapping   │  │ @GrpcService      │  │
│  │ HTTP/1.1    │  │ @MutationMapping│  │ HTTP/2 + protobuf │  │
│  │ JSON        │  │ JSON            │  │ Binary            │  │
│  │ Port 8080   │  │ Port 8080       │  │ Port 9090         │  │
│  └──────┬──────┘  └───────┬─────────┘  └────────┬──────────┘  │
│         │                 │                      │             │
│         └─────────────────┴──────────────────────┘             │
│                           │                                     │
│              ┌────────────▼────────────┐                       │
│              │   ProductRepository     │                       │
│              │   (Spring Data JPA)     │                       │
│              └────────────┬────────────┘                       │
│                           │                                     │
│              ┌────────────▼────────────┐                       │
│              │   H2 In-Memory DB       │                       │
│              │   (same data, 3 APIs)   │                       │
│              └─────────────────────────┘                       │
└─────────────────────────────────────────────────────────────────┘
```

All three API layers share the same `Product` JPA entity and `ProductRepository`. The **only** difference is the communication protocol and query style.

---

## REST API

### Core Concept

REST (Representational State Transfer) is an architectural style built on HTTP. Resources are identified by URLs, and operations are expressed via HTTP verbs.

```
Resource  : /api/products
Sub-resource: /api/products/{id}
Action    : GET, POST, PUT, PATCH, DELETE
```

### HTTP Methods Cheat Sheet

| Verb | Idempotent? | Body? | Common Status |
|------|------------|-------|---------------|
| GET | ✅ Yes | No | 200 OK |
| POST | ❌ No | Yes | 201 Created |
| PUT | ✅ Yes | Yes | 200 OK |
| PATCH | ❌ No | Yes | 200 OK |
| DELETE | ✅ Yes | No | 204 No Content |

> **Idempotent** means calling it multiple times produces the same result as calling it once.

### Try It — REST Examples

#### Get all products
```bash
curl http://localhost:8080/api/products
```

#### Get one product
```bash
curl http://localhost:8080/api/products/1
```

#### Search with query params
```bash
curl "http://localhost:8080/api/products/search?category=Electronics"
curl "http://localhost:8080/api/products/search?name=sony"
```

#### Create a product (POST with JSON body)
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dell XPS 15",
    "description": "OLED display laptop",
    "price": 1799.99,
    "category": "Electronics",
    "stock": 20,
    "available": true
  }'
```

Response: `201 Created` with `Location: /api/products/6`

#### Full update (PUT — replaces entire resource)
```bash
curl -X PUT http://localhost:8080/api/products/6 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dell XPS 15 Updated",
    "price": 1699.99,
    "category": "Electronics",
    "stock": 18,
    "available": true
  }'
```

#### Partial update (PATCH — only changes stock)
```bash
curl -X PATCH http://localhost:8080/api/products/6/stock \
  -H "Content-Type: application/json" \
  -d '{"stock": 5}'
```

#### Delete
```bash
curl -X DELETE http://localhost:8080/api/products/6
# Returns: 204 No Content (no body)
```

### REST Status Codes in This App

| Code | Meaning | When |
|------|---------|------|
| 200 OK | Success | GET, PUT, PATCH |
| 201 Created | Resource created | POST |
| 204 No Content | Success, no body | DELETE |
| 400 Bad Request | Validation failed | POST/PUT with bad data |
| 404 Not Found | Resource missing | Any operation on unknown ID |
| 500 Internal Server Error | Unexpected failure | Bug in server |

### REST Error Response (RFC 7807 Problem Details)

```json
{
  "type": "/errors/validation",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "errors": {
    "price": "Price must be positive",
    "name": "Name is required"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Key REST Principles in This Code

1. **Stateless** — no session state between requests; each request is self-contained
2. **Resource URIs** — `/products` is a noun (collection), `/products/1` is a noun (item)
3. **HTTP verb = action** — `GET /products` reads; `POST /products` creates
4. **Uniform interface** — same conventions everywhere
5. **Content negotiation** — `Content-Type: application/json` tells the server what you're sending

---

## GraphQL API

### Core Concept

GraphQL is a **query language for your API**. Instead of many endpoints (one per resource), there's a **single endpoint** (`/graphql`) and clients describe **exactly** the data they need.

```
One endpoint + client-defined query shape = no over/under-fetching
```

### Open GraphiQL

Visit http://localhost:8080/graphiql for an interactive IDE with schema explorer and autocomplete.

### Schema First

GraphQL starts with a schema (SDL — Schema Definition Language). The schema is in `src/main/resources/graphql/products.graphqls`:

```graphql
type Product {
    id: ID!          # ! means non-null
    name: String!
    price: Float!
    category: String!
    stock: Int!
    available: Boolean!
    description: String  # nullable — no !
}

type Query {
    products(category: String): [Product!]!
    product(id: ID!): Product
}

type Mutation {
    createProduct(input: CreateProductInput!): Product!
    updateProduct(id: ID!, input: UpdateProductInput!): Product!
    deleteProduct(id: ID!): Boolean!
}
```

### Try It — GraphQL Examples

All GraphQL operations are `POST /graphql` with a JSON body `{ "query": "..." }`.

In GraphiQL, just paste the operation body:

#### Query: Get all products (request only needed fields)
```graphql
query {
  products {
    id
    name
    price
    category
  }
}
```

Compare to REST: `GET /api/products` always returns ALL fields. GraphQL returns only `id`, `name`, `price`, `category` — description, stock, timestamps are not sent.

#### Query: Get by category
```graphql
query {
  products(category: "Electronics") {
    id
    name
    price
    stock
  }
}
```

#### Query: Get one product
```graphql
query {
  product(id: 1) {
    name
    description
    price
    available
    createdAt
  }
}
```

#### Query: Search by name
```graphql
query {
  searchProducts(name: "Sony") {
    id
    name
    price
  }
}
```

#### Query: Multiple operations in one request
```graphql
query Dashboard {
  allProducts: products {
    id name price
  }
  electronics: products(category: "Electronics") {
    name price stock
  }
  categories
}
```

This is impossible in REST without making 3 separate HTTP calls. GraphQL does it in one.

#### Mutation: Create
```graphql
mutation {
  createProduct(input: {
    name: "AirPods Pro"
    description: "Active noise cancellation"
    price: 249.99
    category: "Electronics"
    stock: 75
    available: true
  }) {
    id
    name
    createdAt
  }
}
```

#### Mutation: Partial update (just change price)
```graphql
mutation {
  updateProduct(id: 1, input: { price: 2299.99 }) {
    id
    name
    price
    updatedAt
  }
}
```

Note: In REST, partial update requires a separate PATCH endpoint. In GraphQL, partial updates are natural — just include the fields you want to change in the input.

#### Mutation: Delete
```graphql
mutation {
  deleteProduct(id: 6)
}
```

### GraphQL vs REST: N+1 Problem

**REST problem**: Fetching 100 products and their categories requires 101 HTTP calls.

**GraphQL solution**: Fetch all in one query:
```graphql
query {
  products {
    name
    category
    price
  }
}
```

### GraphQL Error Format

GraphQL always returns HTTP 200, even on errors. Errors appear in the response body:

```json
{
  "data": { "product": null },
  "errors": [
    {
      "message": "Product not found: 999",
      "locations": [{ "line": 2, "column": 3 }],
      "path": ["product"]
    }
  ]
}
```

### Key GraphQL Concepts in This Code

| Concept | Java Annotation | Purpose |
|---------|----------------|---------|
| Query resolver | `@QueryMapping` | Handles read operations |
| Mutation resolver | `@MutationMapping` | Handles write operations |
| Argument binding | `@Argument` | Maps GraphQL args to Java params |
| Schema | `.graphqls` file | Defines the contract |

---

## gRPC API

### Core Concept

gRPC is a **Remote Procedure Call** framework by Google. Instead of HTTP verbs + URLs, you call methods on a remote service as if they were local function calls. Communication uses:

- **Protocol Buffers (protobuf)** — binary serialization format (smaller, faster than JSON)
- **HTTP/2** — multiplexing, streaming, header compression
- **Strongly-typed** — schema defined in `.proto` file

### Proto File = Contract

The `.proto` file in `src/main/proto/product.proto` defines everything:

```protobuf
service ProductService {
    // Unary: 1 request → 1 response
    rpc GetProduct (GetProductRequest) returns (ProductResponse);
    rpc CreateProduct (CreateProductRequest) returns (ProductResponse);

    // Server streaming: 1 request → stream of responses
    rpc StreamProducts (ListProductsRequest) returns (stream ProductResponse);
}

message ProductResponse {
    int64  id       = 1;   // Field number (tag) — used in binary encoding
    string name     = 2;   // NOT field name — the number is what's serialized
    double price    = 4;
    string category = 5;
    int32  stock    = 6;
    bool   available = 7;
}
```

Maven compiles `.proto` → Java classes automatically (`mvn compile`).

### gRPC Communication Patterns

| Pattern | Direction | Use Case |
|---------|-----------|----------|
| **Unary** | 1 req → 1 res | Standard CRUD |
| **Server Streaming** | 1 req → many res | Large datasets, live updates |
| **Client Streaming** | many req → 1 res | Batch uploads, file upload |
| **Bidirectional** | many req ↔ many res | Chat, real-time collaboration |

This app implements **Unary** (all CRUD) and **Server Streaming** (`StreamProducts`).

### Try It — gRPC with grpcurl

Install grpcurl: https://github.com/fullstorydev/grpcurl

```bash
# List available services
grpcurl -plaintext localhost:9090 list

# List methods in ProductService
grpcurl -plaintext localhost:9090 list product.ProductService

# Unary: Get a product
grpcurl -plaintext \
  -d '{"id": 1}' \
  localhost:9090 product.ProductService/GetProduct

# Unary: List all products
grpcurl -plaintext \
  -d '{}' \
  localhost:9090 product.ProductService/ListProducts

# Unary: List by category
grpcurl -plaintext \
  -d '{"category": "Electronics"}' \
  localhost:9090 product.ProductService/ListProducts

# Unary: Create a product
grpcurl -plaintext \
  -d '{
    "name": "Google Pixel 9",
    "description": "AI-powered Android phone",
    "price": 799.99,
    "category": "Electronics",
    "stock": 30,
    "available": true
  }' \
  localhost:9090 product.ProductService/CreateProduct

# Unary: Update a product
grpcurl -plaintext \
  -d '{"id": 1, "price": 2299.99}' \
  localhost:9090 product.ProductService/UpdateProduct

# Unary: Delete a product
grpcurl -plaintext \
  -d '{"id": 6}' \
  localhost:9090 product.ProductService/DeleteProduct

# Server Streaming: products arrive one-by-one
grpcurl -plaintext \
  -d '{}' \
  localhost:9090 product.ProductService/StreamProducts
```

### gRPC Status Codes

gRPC uses its own status codes (not HTTP status codes):

| gRPC Status | HTTP Equivalent | Meaning |
|-------------|----------------|---------|
| `OK` | 200 | Success |
| `NOT_FOUND` | 404 | Resource missing |
| `INVALID_ARGUMENT` | 400 | Bad input |
| `UNAUTHENTICATED` | 401 | No credentials |
| `PERMISSION_DENIED` | 403 | Not authorized |
| `INTERNAL` | 500 | Server error |
| `UNAVAILABLE` | 503 | Service down |

In code (see `ProductGrpcService.java`):
```java
responseObserver.onError(
    Status.NOT_FOUND
        .withDescription("Product not found: " + id)
        .asRuntimeException()
);
```

### StreamObserver Pattern

Every gRPC method receives a `StreamObserver<T>` to send the response back:

```java
public void getProduct(GetProductRequest req, StreamObserver<ProductResponse> observer) {
    // 1. Send response
    observer.onNext(productResponse);

    // 2. Signal success (end of stream)
    observer.onCompleted();

    // OR: signal failure
    // observer.onError(Status.NOT_FOUND.asRuntimeException());
}
```

For streaming, call `onNext()` multiple times before `onCompleted()`:
```java
public void streamProducts(Request req, StreamObserver<ProductResponse> observer) {
    for (Product p : repo.findAll()) {
        observer.onNext(toProto(p));  // sends each product as it's ready
    }
    observer.onCompleted();           // signals end of stream
}
```

### Why Binary (Protobuf) is Faster

Same product as JSON vs Protobuf:

```json
{"id":1,"name":"MacBook Pro 16\"","price":2499.99,"category":"Electronics","stock":15}
```
JSON: ~85 bytes (human-readable text)

Protobuf: ~40 bytes (binary, not human-readable, ~50% smaller)

At scale, this difference compounds significantly.

---

## Side-by-Side Comparison

### The Same Operation in All Three APIs

**Goal**: Get product with ID=1, returning only name and price.

#### REST
```bash
GET /api/products/1
# Returns ALL fields including description, stock, timestamps, etc.
# Cannot request fewer fields — server decides the response shape
```

#### GraphQL
```graphql
query {
  product(id: 1) {
    name
    price
  }
}
# Returns ONLY name and price
# Client decides the response shape
```

#### gRPC
```bash
grpcurl -d '{"id": 1}' localhost:9090 product.ProductService/GetProduct
# Returns all fields defined in ProductResponse proto message
# Schema is fixed by the .proto contract
```

---

### Full Comparison Table

| Feature | REST | GraphQL | gRPC |
|---------|------|---------|------|
| **Protocol** | HTTP/1.1 | HTTP/1.1 | HTTP/2 |
| **Format** | JSON (text) | JSON (text) | Protobuf (binary) |
| **Endpoints** | Many (`/products`, `/products/{id}`, ...) | One (`/graphql`) | Methods (like function calls) |
| **Query flexibility** | Fixed response shape | Client defines fields | Fixed response shape |
| **Type safety** | No (runtime) | Yes (schema) | Yes (proto) |
| **Streaming** | SSE / WebSocket (workarounds) | Subscriptions | Built-in (4 patterns) |
| **Code generation** | OpenAPI/Swagger | GraphQL codegen | `protoc` compiler |
| **Browser native** | ✅ Yes | ✅ Yes (POST) | ❌ No (needs grpc-web) |
| **Human readable** | ✅ Yes | ✅ Yes | ❌ No (binary) |
| **Caching** | ✅ HTTP cache (GET) | ⚠️ Complex | ❌ Manual |
| **Learning curve** | Low | Medium | High |
| **Tooling** | Excellent | Good | Moderate |
| **Best for** | Public APIs, CRUD | Flexible data needs | Microservices, performance |

---

## When to Use What

### Choose REST when:
- Building a **public API** that external developers will consume
- Your API is **simple CRUD** operations
- You want maximum **tooling support** (Swagger, curl, Postman)
- **Browser caching** of GET requests matters
- Your team is familiar with HTTP conventions
- Building a **mobile or web backend** serving JSON to UIs

**Examples**: Twitter API, GitHub API, Stripe API, any public-facing API

### Choose GraphQL when:
- You have **multiple client types** (web, iOS, Android) needing different data shapes
- Clients suffer from **over-fetching** (getting too much data) or **under-fetching** (needing multiple requests)
- Your data model is a **graph** (entities with many relationships)
- You want a **self-documenting API** with schema exploration
- Rapid frontend iteration where query shapes change often

**Examples**: GitHub GraphQL API v4, Shopify Storefront API, Facebook API

### Choose gRPC when:
- Building **microservice-to-microservice** communication (internal APIs)
- **Performance is critical** — latency, bandwidth, throughput all matter
- You need **real-time streaming** (telemetry, chat, live data)
- You have **polyglot services** that need to communicate (Java talking to Go, Python, Rust)
- Contract-first development with **strict schema enforcement**
- **Mobile clients** in low-bandwidth environments

**Examples**: Google internal APIs, Netflix inter-service calls, Kubernetes, etcd

---

## Running the Tests

```bash
# Run all tests
mvn test

# Run only REST tests
mvn test -Dtest=RestApiTest

# Run only GraphQL tests
mvn test -Dtest=GraphQLApiTest
```

Tests use an H2 in-memory database — no external setup needed.

---

## Project Structure

```
est-graphql-grpc-example/
├── pom.xml                          # Maven config with all 3 API dependencies
│
├── src/main/
│   ├── java/com/kodtodya/practice/
│   │   ├── ApiShowcaseApplication.java     # Entry point + data seeder
│   │   ├── model/
│   │   │   └── Product.java               # JPA entity (shared by all 3 APIs)
│   │   ├── repository/
│   │   │   └── ProductRepository.java     # Spring Data JPA queries
│   │   ├── rest/
│   │   │   ├── ProductRestController.java  # @RestController — REST endpoints
│   │   │   └── RestExceptionHandler.java  # Global error handler (RFC 7807)
│   │   ├── graphql/
│   │   │   └── ProductGraphQLController.java # @QueryMapping @MutationMapping
│   │   └── grpc/
│   │       └── ProductGrpcService.java    # @GrpcService — gRPC server
│   │
│   ├── proto/
│   │   └── product.proto              # gRPC service + message definitions
│   │
│   └── resources/
│       ├── application.yml            # Config for all 3 APIs
│       └── graphql/
│           └── products.graphqls      # GraphQL schema (SDL)
│
└── src/test/java/com/kodtodya/practice/
    ├── RestApiTest.java               # REST integration tests
    └── GraphQLApiTest.java            # GraphQL integration tests
```

### Key Files to Study

| File | What it teaches |
|------|----------------|
| `ProductRestController.java` | HTTP verbs, status codes, path/query params, request body |
| `products.graphqls` | GraphQL SDL, types, queries, mutations, input types |
| `ProductGraphQLController.java` | `@QueryMapping`, `@MutationMapping`, `@Argument` |
| `product.proto` | protobuf syntax, message types, service definition, streaming |
| `ProductGrpcService.java` | `StreamObserver`, gRPC status codes, unary vs streaming |
| `application.yml` | Configuring all three APIs (ports, paths, options) |

---

## Learning Path

**Day 1: REST**
1. Start the app, open http://localhost:8080/api/products
2. Run all the `curl` examples in the REST section
3. Read `ProductRestController.java` top to bottom — all methods are documented
4. Try to break it — send invalid data, use wrong IDs, see the error responses

**Day 2: GraphQL**
1. Open http://localhost:8080/graphiql
2. Click "Docs" to explore the auto-generated schema documentation
3. Run each query/mutation from the GraphQL examples section
4. Try the "multiple operations in one request" example — really feel the difference
5. Read `products.graphqls` then `ProductGraphQLController.java`

**Day 3: gRPC**
1. Install grpcurl
2. Run `grpcurl -plaintext localhost:9090 list` to see the service
3. Run each grpcurl command from the gRPC examples section
4. Read `product.proto` — understand field numbers, `repeated`, `stream`
5. Read `ProductGrpcService.java` — focus on StreamObserver pattern
6. Compare the streaming call to how you'd do it in REST (you can't, easily)

**Day 4: Compare**
1. Do the same operation (create a product) in all three APIs
2. Use Wireshark or proxy to see the actual bytes on the wire — REST/GraphQL are text, gRPC is binary
3. Read the "When to Use What" section and discuss with your team

---

## Additional Resources

- [REST — Roy Fielding's dissertation](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)
- [GraphQL official docs](https://graphql.org/learn/)
- [gRPC official docs](https://grpc.io/docs/)
- [Spring for GraphQL](https://docs.spring.io/spring-graphql/reference/)
- [grpc-spring-boot-starter](https://github.com/grpc-ecosystem/grpc-spring)
- [Protocol Buffers Language Guide](https://protobuf.dev/programming-guides/proto3/)