package com.kodtodya.practice.service;

import com.kodtodya.practice.domain.Product;
import com.kodtodya.practice.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business logic layer — shared by REST, GraphQL, and gRPC.
 *
 * This is the correct layered architecture:
 *
 *   REST Controller   ─┐
 *   GraphQL Controller ─┼──▶  ProductService  ──▶  ProductRepository  ──▶  DB
 *   gRPC Service      ─┘
 *
 * Benefits:
 *  - Business rules live in ONE place (no duplication across 3 API layers)
 *  - @Transactional is managed here (not scattered in controllers)
 *  - Easy to unit-test without HTTP/gRPC infrastructure
 *
 * NOTE: The controllers in this demo call the repository directly for brevity.
 *       In production, they should all go through this service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository repo;

    public List<Product> findAll() {
        return repo.findAll();
    }

    public List<Product> findByCategory(String category) {
        return repo.findByCategory(category);
    }

    public List<Product> findAvailable() {
        return repo.findByAvailableTrue();
    }

    public List<Product> search(String name) {
        return repo.findByNameContainingIgnoreCase(name);
    }

    public List<Product> findByPriceRange(BigDecimal min, BigDecimal max) {
        return repo.findByPriceRange(min, max);
    }

    public List<String> findAllCategories() {
        return repo.findAllCategories();
    }

    public Optional<Product> findById(Long id) {
        return repo.findById(id);
    }

    public Product create(Product product) {
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        Product saved = repo.save(product);
        log.info("Created product id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    public Product update(Long id, Product incoming) {
        Product existing = repo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setPrice(incoming.getPrice());
        existing.setCategory(incoming.getCategory());
        existing.setStock(incoming.getStock());
        existing.setAvailable(incoming.getAvailable());
        existing.setUpdatedAt(LocalDateTime.now());

        return repo.save(existing);
    }

    public Product updateStock(Long id, int stock) {
        Product product = repo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setStock(stock);
        product.setUpdatedAt(LocalDateTime.now());
        return repo.save(product);
    }

    public boolean delete(Long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        log.info("Deleted product id={}", id);
        return true;
    }

    public boolean exists(Long id) {
        return repo.existsById(id);
    }

    // ── Domain exception ──────────────────────────────────────────

    public static class ProductNotFoundException extends RuntimeException {
        public final Long id;
        public ProductNotFoundException(Long id) {
            super("Product not found: " + id);
            this.id = id;
        }
    }
}