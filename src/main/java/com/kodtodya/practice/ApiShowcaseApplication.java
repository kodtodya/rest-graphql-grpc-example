package com.kodtodya.practice;

import com.kodtodya.practice.domain.Product;
import com.kodtodya.practice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootApplication
@Slf4j
public class ApiShowcaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiShowcaseApplication.class, args);
    }

    /**
     * Seed the in-memory H2 database with sample data on startup.
     * This lets you immediately try all three APIs without any setup.
     */
    @Bean
    CommandLineRunner seedData(ProductRepository repo) {
        return args -> {
            if (repo.count() > 0) return; // idempotent

            repo.save(Product.builder()
                    .name("MacBook Pro 16\"")
                    .description("Apple M3 Pro chip, 18GB RAM, 512GB SSD")
                    .price(new BigDecimal("2499.99"))
                    .category("Electronics")
                    .stock(15)
                    .available(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());

            repo.save(Product.builder()
                    .name("Sony WH-1000XM5")
                    .description("Industry-leading noise cancelling wireless headphones")
                    .price(new BigDecimal("349.99"))
                    .category("Electronics")
                    .stock(42)
                    .available(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());

            repo.save(Product.builder()
                    .name("The Pragmatic Programmer")
                    .description("From Journeyman to Master — essential developer reading")
                    .price(new BigDecimal("49.99"))
                    .category("Books")
                    .stock(100)
                    .available(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());

            repo.save(Product.builder()
                    .name("Herman Miller Aeron")
                    .description("Ergonomic office chair — size B, graphite")
                    .price(new BigDecimal("1495.00"))
                    .category("Furniture")
                    .stock(8)
                    .available(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());

            repo.save(Product.builder()
                    .name("Standing Desk 160cm")
                    .description("Electric height-adjustable desk with memory settings")
                    .price(new BigDecimal("699.00"))
                    .category("Furniture")
                    .stock(0)
                    .available(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());

            log.info("✅ Seeded {} products into H2 database", repo.count());
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("  REST    →  http://localhost:8080/api/products");
            log.info("  GraphQL →  http://localhost:8080/graphiql");
            log.info("  gRPC    →  localhost:9090  (use grpcurl or Postman)");
            log.info("  H2 UI   →  http://localhost:8080/h2-console");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        };
    }
}
