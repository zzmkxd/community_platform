#!/bin/bash
set -e

cd "$(dirname "$0")/.."

echo "========================================"
echo " Community Platform - One-Click Startup"
echo "========================================"
echo ""

# ---- Prerequisites check ----
echo "[0/5] Checking prerequisites..."

if ! command -v docker &>/dev/null; then
    echo "  ERROR: Docker not found! Install Docker first."
    exit 1
fi

if ! docker info &>/dev/null; then
    echo "  ERROR: Docker daemon not running. Start Docker first."
    exit 1
fi
echo "  Docker daemon: OK"

# ---- Cleanup stale instances ----
echo ""
echo "[1/5] Cleaning up old instances..."
docker compose down 2>/dev/null || true
echo "  Docker containers cleaned."

# Check for port conflicts
PORT_CONFLICT=0
for port in 8080 8081 8082 8083 8084 8091 3308 6381 8848 9200 9876; do
    if ss -tlnp 2>/dev/null | grep -q ":$port " || netstat -tlnp 2>/dev/null | grep -q ":$port "; then
        echo "  WARNING: Port $port occupied! Free it before starting."
        PORT_CONFLICT=1
    fi
done
if [ "$PORT_CONFLICT" -eq 1 ]; then
    echo "  Run: lsof -i :8080  (or ss -tlnp | grep :8080) to find the process"
    exit 1
fi

# ---- Build ----
echo ""
echo "[2/5] Building application JARs..."
mvn package -DskipTests -B -q
echo "  Build: SUCCESS"

# ---- Build Docker images ----
echo ""
echo "[3/5] Building Docker images (skipped if cached)..."
if ! docker compose build 2>&1; then
    echo ""
    echo "  ============================================================"
    echo "  ERROR: Docker build failed!"
    echo "  Common causes:"
    echo "  1. Elasticsearch IK plugin download failed → check elasticsearch.Dockerfile"
    echo "  2. Docker out of memory → increase Docker memory to 8GB+"
    echo "  ============================================================"
    exit 1
fi
echo "  Images: READY"

# ---- Staged startup (avoids disk I/O spike) ----
echo ""
echo "[4/5] Starting services in waves (to avoid disk I/O spike)..."

# Wave A: Infrastructure
echo "  Wave A: Infrastructure (MySQL Redis Nacos MinIO ES RocketMQ)..."
docker compose up -d --no-build mysql redis nacos minio elasticsearch rocketmq-namesrv rocketmq-broker
echo "  Waiting 30s for infrastructure to stabilize..."
sleep 30

# Wave B: Nacos config init
echo "  Wave B: Publishing Nacos shared config..."
docker compose up -d --no-build nacos-init

# Wave C: Application services
echo "  Wave C: Application services (Gateway & 5 microservices & WebSocket)..."
docker compose up -d --no-build

# ---- Health check ----
echo ""
echo "[5/5] Waiting for services to be healthy..."
sleep 15
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null | grep -q "200"; then
    echo "  Gateway: HEALTHY"
else
    echo "  Gateway: NOT READY YET (check: docker compose logs gateway --tail 30)"
fi

echo ""
echo "========================================"
echo " Startup complete! Access points:"
echo "   Swagger UI:    http://localhost:8080/swagger-ui.html"
echo "   Gateway:       http://localhost:8080"
echo "   Nacos Console: http://localhost:8848/nacos  (no auth)"
echo "   MinIO Console: http://localhost:9005  (minioadmin / minioadmin)"
echo "   WebSocket:     ws://localhost:8091"
echo "========================================"
echo ""
echo " Troubleshooting:"
echo "   docker compose ps                     # Check all services"
echo "   docker compose logs -f [service]     # Follow logs"
echo "   bash scripts/smoke-test.sh           # Smoke test"
echo ""
echo " Disk I/O diagnosis (if system is sluggish):"
echo "   1. iostat -x 1              # Watch disk utilization"
echo "   2. iotop -o                 # Which process is writing"
echo "   3. Suspects: Redis AOF fsync, MySQL InnoDB, ES segment merge"
echo "   4. Quick fix: change Redis to '--appendonly no' in docker-compose.yml"
echo ""
