# ─────────────────────────────────────────────────────────────────
#  Spring API Showcase — Makefile
#  Usage: make <target>
# ─────────────────────────────────────────────────────────────────

.PHONY: run build test clean proto help

## Run the application (starts all 3 APIs)
run:
	./mvnw spring-boot:run

## Build the JAR (skips tests)
build:
	./mvnw package -DskipTests

## Build the JAR with tests
build-all:
	./mvnw package

## Run all tests
test:
	./mvnw test

## Run only REST tests
test-rest:
	./mvnw test -Dtest=RestApiTest,ProductRestControllerMockMvcTest

## Run only GraphQL tests
test-graphql:
	./mvnw test -Dtest=GraphQLApiTest

## Run only gRPC tests
test-grpc:
	./mvnw test -Dtest=ProductGrpcServiceTest

## Run unit tests only (fast)
test-unit:
	./mvnw test -Dtest=ProductServiceTest

## Clean build artifacts
clean:
	./mvnw clean

## Compile and generate protobuf Java classes
proto:
	./mvnw generate-sources

## Run the built JAR directly
run-jar:
	java -jar target/spring-api-showcase-1.0.0.jar

## Test REST with curl (app must be running)
curl-test:
	@echo "\n=== GET all products ==="
	curl -s http://localhost:8080/api/products | python3 -m json.tool
	@echo "\n=== GET product 1 ==="
	curl -s http://localhost:8080/api/products/1 | python3 -m json.tool
	@echo "\n=== GET categories ==="
	curl -s http://localhost:8080/api/products/categories | python3 -m json.tool

## Test GraphQL with curl (app must be running)
graphql-test:
	@echo "\n=== GraphQL: list all products ==="
	curl -s -X POST http://localhost:8080/graphql \
	  -H "Content-Type: application/json" \
	  -d '{"query":"{ products { id name price category } }"}' | python3 -m json.tool

## Test gRPC with grpcurl (app must be running, grpcurl must be installed)
grpc-test:
	@echo "\n=== gRPC: list products ==="
	grpcurl -plaintext -d '{}' localhost:9090 product.ProductService/ListProducts
	@echo "\n=== gRPC: get product 1 ==="
	grpcurl -plaintext -d '{"id":1}' localhost:9090 product.ProductService/GetProduct

help:
	@grep -E '^##' Makefile | sed 's/## /  /'