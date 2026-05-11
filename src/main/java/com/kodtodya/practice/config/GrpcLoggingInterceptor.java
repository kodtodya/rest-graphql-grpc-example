package com.kodtodya.practice.config;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

/**
 * gRPC Server Interceptor — runs for every incoming gRPC call.
 *
 * Interceptors in gRPC are like Servlet Filters or Spring HandlerInterceptors
 * for HTTP — they wrap the call and can add cross-cutting concerns:
 *   - Logging
 *   - Authentication / JWT validation
 *   - Metrics / tracing
 *   - Rate limiting
 *
 * @GrpcGlobalServerInterceptor registers this for ALL gRPC services automatically.
 */
@GrpcGlobalServerInterceptor
@Slf4j
public class GrpcLoggingInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String method = call.getMethodDescriptor().getFullMethodName();
        long start = System.currentTimeMillis();
        log.info("[gRPC] ▶ {}", method);

        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                long elapsed = System.currentTimeMillis() - start;
                if (status.isOk()) {
                    log.info("[gRPC] ✓ {} completed in {}ms", method, elapsed);
                } else {
                    log.warn("[gRPC] ✗ {} failed in {}ms — {} {}",
                            method, elapsed, status.getCode(), status.getDescription());
                }
                super.close(status, trailers);
            }
        };

        return next.startCall(wrappedCall, headers);
    }
}