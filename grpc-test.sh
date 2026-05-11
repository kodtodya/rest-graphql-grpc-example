#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────
#  gRPC Test Script — grpcurl commands for all service methods
#  Prerequisites: grpcurl (https://github.com/fullstorydev/grpcurl)
#  Install: brew install grpcurl  OR  go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest
#
#  Usage: ./grpc-test.sh          (runs all tests)
#         ./grpc-test.sh list     (list services)
#         ./grpc-test.sh get 1    (get product id=1)
# ─────────────────────────────────────────────────────────────────

HOST="localhost:9090"
SERVICE="product.ProductService"

check_grpcurl() {
  if ! command -v grpcurl &> /dev/null; then
    echo "❌ grpcurl not found. Install: brew install grpcurl"
    exit 1
  fi
}

separator() { echo -e "\n$(printf '─%.0s' {1..60})"; }

list_services() {
  separator
  echo "📋 List all gRPC services:"
  grpcurl -plaintext "$HOST" list
  separator
  echo "📋 List methods in $SERVICE:"
  grpcurl -plaintext "$HOST" list "$SERVICE"
}

list_products() {
  separator
  echo "📦 ListProducts (all):"
  grpcurl -plaintext -d '{}' "$HOST" "$SERVICE/ListProducts"

  separator
  echo "📦 ListProducts (Electronics only):"
  grpcurl -plaintext -d '{"category": "Electronics"}' "$HOST" "$SERVICE/ListProducts"
}

get_product() {
  local id=${1:-1}
  separator
  echo "🔍 GetProduct id=$id:"
  grpcurl -plaintext -d "{\"id\": $id}" "$HOST" "$SERVICE/GetProduct"
}

create_product() {
  separator
  echo "➕ CreateProduct:"
  grpcurl -plaintext \
    -d '{
      "name": "grpcurl Test Product",
      "description": "Created via grpcurl script",
      "price": 99.99,
      "category": "Testing",
      "stock": 5,
      "available": true
    }' \
    "$HOST" "$SERVICE/CreateProduct"
}

update_product() {
  local id=${1:-1}
  separator
  echo "✏️  UpdateProduct id=$id (price only):"
  grpcurl -plaintext \
    -d "{\"id\": $id, \"price\": 1999.99}" \
    "$HOST" "$SERVICE/UpdateProduct"
}

delete_product() {
  local id=${1:-99}
  separator
  echo "🗑️  DeleteProduct id=$id:"
  grpcurl -plaintext -d "{\"id\": $id}" "$HOST" "$SERVICE/DeleteProduct"
}

stream_products() {
  separator
  echo "🌊 StreamProducts (server-streaming — items arrive one-by-one):"
  grpcurl -plaintext -d '{}' "$HOST" "$SERVICE/StreamProducts"
}

get_product_not_found() {
  separator
  echo "❓ GetProduct id=9999 (expect NOT_FOUND):"
  grpcurl -plaintext -d '{"id": 9999}' "$HOST" "$SERVICE/GetProduct" || true
}

# ── Main ──────────────────────────────────────────────────────────
check_grpcurl

case "${1:-all}" in
  list)    list_services ;;
  get)     get_product "$2" ;;
  list-products) list_products ;;
  create)  create_product ;;
  update)  update_product "$2" ;;
  delete)  delete_product "$2" ;;
  stream)  stream_products ;;
  all)
    echo "🚀 Running all gRPC tests against $HOST"
    list_services
    list_products
    get_product 1
    get_product_not_found
    create_product
    update_product 1
    stream_products
    separator
    echo "✅ All gRPC tests complete"
    ;;
  *)
    echo "Usage: $0 [all|list|get <id>|list-products|create|update <id>|delete <id>|stream]"
    ;;
esac