@echo off
echo ========================================
echo  Community Platform - Build & Start All
echo ========================================
echo.
echo [1/2] Building all services...
call mvn package -DskipTests -B
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.
echo [2/2] Starting Docker Compose...
docker compose up -d --build
echo.
echo Startup complete! Services:
echo   Gateway:       http://localhost:8080
echo   User Service:  http://localhost:8081
echo   Server Service: http://localhost:8082
echo   Message Svc:   http://localhost:8083
echo   File Service:  http://localhost:8084
echo   WebSocket:     ws://localhost:8091
echo   Nacos Console: http://localhost:8848/nacos
echo   MinIO Console: http://localhost:9005
echo.
pause
