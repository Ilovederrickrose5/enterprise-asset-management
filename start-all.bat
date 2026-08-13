@echo off
title EAM Startup

echo ==============================================
echo  Enterprise Asset Management System
echo ==============================================
echo.

set "NACOS_DIR=D:\Users\30776\Downloads\nacos-server-3.2.2\nacos\bin"
set "REDIS_DIR=D:\Program Files\Redis"
set "PROJECT_DIR=D:\Users\30776\IdeaProjects\enterprise-asset-management"

echo [INFO] Cleaning ports...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8848"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8082"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do taskkill /F /PID %%a 2>nul
echo [OK] Ports cleaned.
echo.

echo [INFO] Starting Redis...
start "Redis" cmd /k "cd /d ""%REDIS_DIR%"" & .\redis-server.exe"
echo [OK] Redis started.
echo.

echo [INFO] Starting Nacos...
start "Nacos" cmd /k "cd /d ""%NACOS_DIR%"" & startup.cmd -m standalone"
echo [OK] Nacos started.
echo.

echo [INFO] Building project...
cd /d "%PROJECT_DIR%"
mvn clean install -DskipTests -q
echo [OK] Build done.
echo.

echo [INFO] Starting Auth Service...
start "Auth" cmd /k "cd /d ""%PROJECT_DIR%"" & mvn -pl asset-auth spring-boot:run"
echo [OK] Auth Service starting...
echo.

echo [INFO] Starting Business Service...
start "Business" cmd /k "cd /d ""%PROJECT_DIR%"" & mvn -pl asset-business spring-boot:run"
echo [OK] Business Service starting...
echo.

echo [INFO] Starting Frontend...
start "Frontend" cmd /k "cd /d ""%PROJECT_DIR%\frontend"" & npm.cmd run dev"
echo [OK] Frontend starting...
echo.

echo ==============================================
echo [SUCCESS] All services started.
echo.
echo Access:
echo   Frontend: http://localhost:5173
echo   Nacos: http://localhost:8848/nacos
echo   Auth: http://localhost:8081
echo   Business: http://localhost:8082
echo ==============================================
pause