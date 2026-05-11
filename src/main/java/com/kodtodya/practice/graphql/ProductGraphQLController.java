package com.kodtodya.practice.graphql;

import com.kodtodya.practice.domain.Product;
import com.kodtodya.practice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ─────────────────────────────────────────────────────────────────
 *  GraphQL API  —  POST /graphql
 * ─────────────────────────────────────────────────────────────────
 *
 *  GraphQL Concepts illustrated here:
 *
 *  1. SINGLE ENDPOINT: All operations hit POST /graphql
 *     (contrast with REST's many endpoints)
 *
 *  2. CLIENT-DRIVEN QUERIES: Clients request only the fields they need.
 *     e.g. { product(id: 1) { name price } }  — no description, no stock
 *
 *  3. OPERATIONS:
 *     - @QueryMapping  → resolves a field in `type Query`
 *     - @MutationMapping → resolves a field in `type Mutation`
 *     - @Argument      → binds a GraphQL argument to a method param
 *
 *  4. STRONG TYPING: Schema defines exact shape; server enforces it
 *
 *  5. NO OVER/UNDER-FETCHING: request exactly what you need,
 *     unlike REST where the server decides the response shape
 *
 *  Try in GraphiQL at http://localhost:8080/graphiql
 * ─────────────────────────────────────────────────────────────────
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductGraphQLController {

    private final ProductRepository repo;

    // ── QUERIES ───────────────────────────────────────────────────

    /**
     * Query: products(category: String): [Product!]!
     *
     * Example:
     *   query {
     *     products(category: "Electronics") {
     *       id name price stock
     *     }
     *   }
     */
    @QueryMapping
    public List<Product> products(@Argument String category) {
        log.info("[GraphQL] Query: products category={}", category);
        return category != null ? repo.findByCategory(category) : repo.findAll();
    }

    /**
     * Query: product(id: ID!): Product
     *
     * Example:
     *   query {
     *     product(id: "1") {
     *       name description price available
     *     }
     *   }
     */
    @QueryMapping
    public Optional<Product> product(@Argument Long id) {
        log.info("[GraphQL] Query: product id={}", id);
        return repo.findById(id);
    }

    /**
     * Query: availableProducts: [Product!]!
     */
    @QueryMapping
    public List<Product> availableProducts() {
        log.info("[GraphQL] Query: availableProducts");
        return repo.findByAvailableTrue();
    }

    /**
     * Query: categories: [String!]!
     */
    @QueryMapping
    public List<String> categories() {
        log.info("[GraphQL] Query: categories");
        return repo.findAllCategories();
    }

    /**
     * Query: searchProducts(name: String!): [Product!]!
     *
     * Example:
     *   query {
     *     searchProducts(name: "laptop") {
     *       id name price
     *     }
     *   }
     */
    @QueryMapping
    public List<Product> searchProducts(@Argument String name) {
        log.info("[GraphQL] Query: searchProducts name={}", name);
        return repo.findByNameContainingIgnoreCase(name);
    }

    // ── MUTATIONS ─────────────────────────────────────────────────

    /**
     * Mutation: createProduct(input: CreateProductInput!): Product!
     *
     * Example:
     *   mutation {
     *     createProduct(input: {
     *       name: "iPhone 16"
     *       price: 999.99
     *       category: "Electronics"
     *       stock: 50
     *     }) {
     *       id name price createdAt
     *     }
     *   }
     */
    @MutationMapping
    public Product createProduct(@Argument Map<String, Object> input) {
        log.info("[GraphQL] Mutation: createProduct name={}", input.get("name"));
        Product product = Product.builder()
                .name((String) input.get("name"))
                .description((String) input.get("description"))
                .price(new BigDecimal(input.get("price").toString()))
                .category((String) input.get("category"))
                .stock(input.get("stock") != null ? (Integer) input.get("stock") : 0)
                .available(input.get("available") == null || (Boolean) input.get("available"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return repo.save(product);
    }

    /**
     * Mutation: updateProduct(id: ID!, input: UpdateProductInput!): Product!
     *
     * NOTE: GraphQL partial update is natural — only provided fields change.
     * Contrast with REST PUT (full replacement) vs PATCH (partial).
     *
     * Example:
     *   mutation {
     *     updateProduct(id: "1", input: { price: 899.99 }) {
     *       id name price updatedAt
     *     }
     *   }
     */
    @MutationMapping
    public Product updateProduct(@Argument Long id, @Argument Map<String, Object> input) {
        log.info("[GraphQL] Mutation: updateProduct id={}", id);
        Product product = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (input.containsKey("name"))        product.setName((String) input.get("name"));
        if (input.containsKey("description")) product.setDescription((String) input.get("description"));
        if (input.containsKey("price"))       product.setPrice(new BigDecimal(input.get("price").toString()));
        if (input.containsKey("category"))    product.setCategory((String) input.get("category"));
        if (input.containsKey("stock"))       product.setStock((Integer) input.get("stock"));
        if (input.containsKey("available"))   product.setAvailable((Boolean) input.get("available"));
        product.setUpdatedAt(LocalDateTime.now());

        return repo.save(product);
    }

    /**
     * Mutation: updateStock(id: ID!, stock: Int!): Product!
     */
    @MutationMapping
    public Product updateStock(@Argument Long id, @Argument Integer stock) {
        log.info("[GraphQL] Mutation: updateStock id={} stock={}", id, stock);
        Product product = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        product.setStock(stock);
        product.setUpdatedAt(LocalDateTime.now());
        return repo.save(product);
    }

    /**
     * Mutation: deleteProduct(id: ID!): Boolean!
     *
     * Example:
     *   mutation {
     *     deleteProduct(id: "1")
     *   }
     */
    @MutationMapping
    public boolean deleteProduct(@Argument Long id) {
        log.info("[GraphQL] Mutation: deleteProduct id={}", id);
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }
}
