package com.kodtodya.practice.grpc;

import com.kodtodya.practice.domain.Product;
import com.kodtodya.practice.repository.ProductRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────────
 *  gRPC Service Implementation
 * ─────────────────────────────────────────────────────────────────
 *
 *  gRPC Concepts illustrated here:
 *
 *  1. CONTRACT-FIRST: The .proto file is the source of truth.
 *     Java classes are auto-generated at compile time via protobuf plugin.
 *
 *  2. BINARY PROTOCOL: Messages are serialized to binary (protobuf),
 *     not JSON text. Far more compact and faster than REST/GraphQL.
 *
 *  3. HTTP/2: gRPC runs over HTTP/2 — multiplexing, header compression,
 *     streaming all built-in. REST typically uses HTTP/1.1.
 *
 *  4. STREAMOBSERVER PATTERN:
 *     - onNext(value)    → send one message in the stream
 *     - onCompleted()    → signal normal end of stream
 *     - onError(t)       → signal failure with status code
 *
 *  5. GRPC STATUS CODES (similar role to HTTP status codes):
 *     OK, NOT_FOUND, INVALID_ARGUMENT, INTERNAL, UNAVAILABLE, etc.
 *
 *  6. @GrpcService: Spring Boot integration annotation that registers
 *     this class as a gRPC endpoint (no @RestController needed).
 *
 *  Server runs on port 9090 (separate from HTTP 8080).
 * ─────────────────────────────────────────────────────────────────
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductRepository repo;

    // ── UNARY: Get single product ──────────────────────────────────

    /**
     * Unary RPC — one request, one response.
     *
     * Client call (pseudo-code):
     *   ProductResponse r = stub.getProduct(GetProductRequest.newBuilder().setId(1).build());
     */
    @Override
    public void getProduct(GetProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        log.info("[gRPC] GetProduct id={}", request.getId());
        repo.findById(request.getId())
                .ifPresentOrElse(
                        product -> {
                            responseObserver.onNext(toProto(product));
                            responseObserver.onCompleted();
                        },
                        () -> responseObserver.onError(
                                Status.NOT_FOUND
                                        .withDescription("Product not found: " + request.getId())
                                        .asRuntimeException())
                );
    }

    // ── UNARY: List all products ───────────────────────────────────

    /**
     * Returns all products (or filtered by category) in a single response.
     */
    @Override
    public void listProducts(ListProductsRequest request, StreamObserver<ListProductsResponse> responseObserver) {
        log.info("[gRPC] ListProducts category={}", request.getCategory());

        List<Product> products = request.getCategory().isBlank()
                ? repo.findAll()
                : repo.findByCategory(request.getCategory());

        List<ProductResponse> protoList = products.stream().map(this::toProto).toList();

        ListProductsResponse response = ListProductsResponse.newBuilder()
                .addAllProducts(protoList)
                .setTotal(protoList.size())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // ── UNARY: Create product ──────────────────────────────────────

    @Override
    public void createProduct(CreateProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        log.info("[gRPC] CreateProduct name={}", request.getName());
        try {
            Product product = Product.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(BigDecimal.valueOf(request.getPrice()))
                    .category(request.getCategory())
                    .stock(request.getStock())
                    .available(request.getAvailable())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            Product saved = repo.save(product);
            responseObserver.onNext(toProto(saved));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // ── UNARY: Update product ──────────────────────────────────────

    @Override
    public void updateProduct(UpdateProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        log.info("[gRPC] UpdateProduct id={}", request.getId());
        repo.findById(request.getId()).ifPresentOrElse(product -> {
            if (!request.getName().isBlank())     product.setName(request.getName());
            if (!request.getDescription().isBlank()) product.setDescription(request.getDescription());
            if (request.getPrice() > 0)            product.setPrice(BigDecimal.valueOf(request.getPrice()));
            if (!request.getCategory().isBlank())  product.setCategory(request.getCategory());
            if (request.getStock() >= 0)           product.setStock(request.getStock());
            product.setAvailable(request.getAvailable());
            product.setUpdatedAt(LocalDateTime.now());
            responseObserver.onNext(toProto(repo.save(product)));
            responseObserver.onCompleted();
        }, () -> responseObserver.onError(
                Status.NOT_FOUND.withDescription("Product not found: " + request.getId()).asRuntimeException()));
    }

    // ── UNARY: Delete product ──────────────────────────────────────

    @Override
    public void deleteProduct(DeleteProductRequest request, StreamObserver<DeleteProductResponse> responseObserver) {
        log.info("[gRPC] DeleteProduct id={}", request.getId());
        if (repo.existsById(request.getId())) {
            repo.deleteById(request.getId());
            responseObserver.onNext(DeleteProductResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Product deleted successfully")
                    .build());
        } else {
            responseObserver.onNext(DeleteProductResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Product not found: " + request.getId())
                    .build());
        }
        responseObserver.onCompleted();
    }

    // ── SERVER STREAMING: Stream all products one-by-one ──────────

    /**
     * Server Streaming RPC — one request, MANY responses streamed back.
     *
     * Client receives products one at a time as the server sends them.
     * Perfect for large datasets, live feeds, or real-time price updates.
     *
     * Client call (pseudo-code):
     *   Iterator<ProductResponse> iter = stub.streamProducts(request);
     *   while (iter.hasNext()) { handle(iter.next()); }
     */
    @Override
    public void streamProducts(ListProductsRequest request, StreamObserver<ProductResponse> responseObserver) {
        log.info("[gRPC] StreamProducts — server-streaming category={}", request.getCategory());
        List<Product> products = request.getCategory().isBlank()
                ? repo.findAll()
                : repo.findByCategory(request.getCategory());

        for (Product product : products) {
            // Each onNext() call pushes one item to the client stream
            responseObserver.onNext(toProto(product));
            try { Thread.sleep(50); } catch (InterruptedException ignored) {} // simulate real-time flow
        }
        responseObserver.onCompleted(); // signals end of stream
    }

    // ── Mapping helper ─────────────────────────────────────────────

    /**
     * Converts a JPA entity to a protobuf message.
     * Protobuf uses builders — all fields are immutable once built.
     */
    private ProductResponse toProto(Product p) {
        return ProductResponse.newBuilder()
                .setId(p.getId())
                .setName(p.getName())
                .setDescription(p.getDescription() != null ? p.getDescription() : "")
                .setPrice(p.getPrice().doubleValue())
                .setCategory(p.getCategory())
                .setStock(p.getStock())
                .setAvailable(p.getAvailable())
                .setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "")
                .setUpdatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "")
                .build();
    }
}
