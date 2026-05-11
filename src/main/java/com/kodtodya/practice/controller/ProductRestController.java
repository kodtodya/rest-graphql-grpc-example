package com.kodtodya.practice.controller;

import com.kodtodya.practice.domain.Product;
import com.kodtodya.practice.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────────
 *  REST API  —  /api/products
 * ─────────────────────────────────────────────────────────────────
 *
 *  REST Concepts illustrated here:
 *  ┌──────────────┬────────────────────────────┬──────────────────┐
 *  │ HTTP Method  │ Endpoint                   │ Action           │
 *  ├──────────────┼────────────────────────────┼──────────────────┤
 *  │ GET          │ /api/products              │ List all         │
 *  │ GET          │ /api/products/{id}         │ Get one          │
 *  │ GET          │ /api/products/search       │ Filter/search    │
 *  │ POST         │ /api/products              │ Create           │
 *  │ PUT          │ /api/products/{id}         │ Full update      │
 *  │ PATCH        │ /api/products/{id}/stock   │ Partial update   │
 *  │ DELETE       │ /api/products/{id}         │ Delete           │
 *  └──────────────┴────────────────────────────┴──────────────────┘
 *
 *  Key REST Principles:
 *  - Stateless: every request contains all info the server needs
 *  - Resource-based URIs: nouns, not verbs (/products not /getProducts)
 *  - HTTP status codes convey meaning (200, 201, 204, 404, 400…)
 *  - JSON request/response bodies (Content-Type: application/json)
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductRestController {

    private final ProductRepository repo;

    // ── READ ──────────────────────────────────────────────────────

    /**
     * GET /api/products
     * Returns the full product catalog (200 OK).
     */
    @GetMapping
    public List<Product> getAll() {
        log.info("[REST] GET /api/products");
        return repo.findAll();
    }

    /**
     * GET /api/products/{id}
     * Returns one product or 404 Not Found.
     */
    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        log.info("[REST] GET /api/products/{}", id);
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    /**
     * GET /api/products/search?name=&category=&minPrice=&maxPrice=
     * Flexible search with optional query parameters.
     */
    @GetMapping("/search")
    public List<Product> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        log.info("[REST] GET /api/products/search name={} category={}", name, category);

        if (name != null) return repo.findByNameContainingIgnoreCase(name);
        if (category != null) return repo.findByCategory(category);
        if (minPrice != null && maxPrice != null) return repo.findByPriceRange(minPrice, maxPrice);
        return repo.findByAvailableTrue();
    }

    /**
     * GET /api/products/categories
     * Returns distinct category list — demonstrates sub-resource.
     */
    @GetMapping("/categories")
    public List<String> getCategories() {
        return repo.findAllCategories();
    }

    // ── CREATE ────────────────────────────────────────────────────

    /**
     * POST /api/products
     * Creates a new product. Returns 201 Created with Location header.
     * Body is validated with Bean Validation (@Valid).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        log.info("[REST] POST /api/products name={}", product.getName());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        Product saved = repo.save(product);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/products/" + saved.getId())
                .body(saved);
    }

    // ── FULL UPDATE ───────────────────────────────────────────────

    /**
     * PUT /api/products/{id}
     * Replaces the entire product resource (idempotent).
     */
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody Product incoming) {
        log.info("[REST] PUT /api/products/{}", id);
        Product existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));

        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setPrice(incoming.getPrice());
        existing.setCategory(incoming.getCategory());
        existing.setStock(incoming.getStock());
        existing.setAvailable(incoming.getAvailable());
        existing.setUpdatedAt(LocalDateTime.now());
        return repo.save(existing);
    }

    // ── PARTIAL UPDATE ────────────────────────────────────────────

    /**
     * PATCH /api/products/{id}/stock
     * Updates only the stock field — demonstrates partial update pattern.
     */
    @PatchMapping("/{id}/stock")
    public Product updateStock(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        log.info("[REST] PATCH /api/products/{}/stock", id);
        Product product = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
        product.setStock(body.get("stock"));
        product.setUpdatedAt(LocalDateTime.now());
        return repo.save(product);
    }

    // ── DELETE ────────────────────────────────────────────────────

    /**
     * DELETE /api/products/{id}
     * Returns 204 No Content on success (body-less response).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        log.info("[REST] DELETE /api/products/{}", id);
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        repo.deleteById(id);
    }
}
