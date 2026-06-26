@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0.."

echo ========================================
echo  Community Platform - One-Click Startup
echo ========================================
echo.

REM ============================================================
REM  Step 0: Prerequisites check
REM ============================================================
echo [0/5] Checking prerequisites...

REM --- JAVA_HOME ---
if not defined JAVA_HOME (
    for %%d in (
        "C:\Program Files\Java\jdk-21"
        "E:\Learn_zone\Environment\Java\Java\jdk-21.0.1"
    ) do (
        if exist %%d (
            set "JAVA_HOME=%%~d"
            echo   JAVA_HOME auto-detected: !JAVA_HOME!
            goto :java_ok
        )
    )
    echo   ERROR: JAVA_HOME not set! Set it or edit this script's search paths.
    pause
    exit /b 1
)
:java_ok
set "JAVA_HOME=%JAVA_HOME:"=%"

REM --- Docker daemon ---
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo   ERROR: Docker is not running. Start Docker Desktop first!
    pause
    exit /b 1
)
echo   Docker daemon: OK

REM ============================================================
REM  Step 1: Cleanup stale instances
REM ============================================================
echo.
echo [1/5] Cleaning up old instances...

docker compose down 2>nul
echo   Docker containers cleaned.

set "PORT_CONFLICT=0"
for %%p in (8080 8081 8082 8083 8084 8091 3308 6381 8848 9200 9876) do (
    netstat -ano 2>nul | findstr ":%%p " | findstr "LISTENING" >nul
    if !ERRORLEVEL! EQU 0 (
        echo   WARNING: Port %%p occupied! Free it before starting.
        set "PORT_CONFLICT=1"
    )
)
if !PORT_CONFLICT! EQU 1 (
    echo.
    echo   Find the process: netstat -ano ^| findstr "LISTENING" ^| findstr ":8080"
    echo   Kill it: taskkill //F //PID ^<PID^>
    pause
    exit /b 1
)

REM ============================================================
REM  Step 2: Build application JARs
REM ============================================================
echo.
echo [2/5] Building application JARs (mvn package -DskipTests)...

call mvn package -DskipTests -B -q
if %ERRORLEVEL% NEQ 0 (
    echo   ERROR: Maven build failed! Run without -q to see details:
    echo          mvn package -DskipTests
    pause
    exit /b %ERRORLEVEL%
)
echo   Build: SUCCESS

REM ============================================================
REM  Step 3: Build Docker images (all at once, only if needed)
REM ============================================================
echo.
echo [3/5] Building Docker images (skipped if cached)...

docker compose build 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo   ============================================================
    echo   ERROR: Docker build failed!
    echo.
    echo   Common causes:
    echo   1. Elasticsearch IK plugin download failed (China network):
    echo      ^> Check: elasticsearch.Dockerfile has '^^^|^| echo WARNING' fallback
    echo   2. Docker out of memory:
    echo      ^> Fix: Docker Desktop Settings ^> Resources ^> Memory ^> 8GB+
    echo   ============================================================
    pause
    exit /b %ERRORLEVEL%
)
echo   Images: READY

REM ============================================================
REM  Step 4: Staged startup (avoids disk I/O spike from 13 simultaneous starts)
REM ============================================================
echo.
echo [4/5] Starting services in waves (to avoid disk I/O spike)...

REM --- Wave A: Infrastructure (6 containers) ---
echo   Wave A: Infrastructure (MySQL Redis Nacos MinIO ES RocketMQ)...
docker compose up -d --no-build mysql redis nacos minio elasticsearch rocketmq-namesrv rocketmq-broker
if %ERRORLEVEL% NEQ 0 (
    echo   ERROR: Infrastructure startup failed!
    pause
    exit /b %ERRORLEVEL%
)

REM Wait for core infra health
echo   Waiting for infrastructure to stabilize (30s)...
timeout /t 30 /nobreak >nul

REM --- Wave B: Nacos config init (publishes shared config) ---
echo   Wave B: Publishing Nacos shared config...
docker compose up -d --no-build nacos-init
REM nacos-init exits after publishing, that's normal

REM --- Wave C: Application services (6 containers) ---
echo   Wave C: Application services (Gateway ^& 5 microservices ^& WebSocket)...
docker compose up -d --no-build
if %ERRORLEVEL% NEQ 0 (
    echo   ERROR: Application startup failed!
    pause
    exit /b %ERRORLEVEL%
)

REM ============================================================
REM  Step 5: Wait and verify
REM ============================================================
echo.
echo [5/5] Waiting for all services to be healthy (approx 60s)...
timeout /t 15 /nobreak >nul

echo   Checking Gateway health...
curl -s -o nul -w "%%{http_code}" http://localhost:8080/actuator/health 2>nul | findstr "200" >nul
if %ERRORLEVEL% EQU 0 (
    echo   Gateway: HEALTHY
) else (
    echo   Gateway: NOT READY YET (run: docker compose logs gateway --tail 30)
)

echo.
echo ========================================
echo  Startup complete! Access points:
echo    Swagger UI:    http://localhost:8080/swagger-ui.html
echo    Gateway:       http://localhost:8080
echo    Nacos Console: http://localhost:8848/nacos  (no auth)
echo    MinIO Console: http://localhost:9005  (minioadmin / minioadmin)
echo    WebSocket:     ws://localhost:8091
echo ========================================
echo.
echo  Troubleshooting:
echo    docker compose ps                     # Check all services
echo    docker compose logs -f [service]     # Follow logs for one service
echo    bash scripts/smoke-test.sh           # Smoke test (15+ assertions)
echo.
echo  Disk I/O diagnosis (if system is sluggish):
echo    1. Win+R ^> resmon ^> Disk tab ^> sort by "Total (B/sec)"
echo    2. Suspects: Redis AOF fsync, MySQL InnoDB, ES segment merge
echo    3. Quick fix: change Redis to "--appendonly no" in docker-compose.yml
echo       (AOF is safe to disable for local dev; data survives in RDB snapshots)
echo.
pause
