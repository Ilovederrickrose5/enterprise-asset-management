@echo off
title EAM Microservice Startup

echo ==============================================
echo  Enterprise Asset Management System
echo  Startup: Nacos -> Auth -> Business -> Frontend
echo ==============================================
echo.

set "NACOS_DIR=D:\Users\30776\Downloads\nacos-server-3.2.2\nacos\bin"
set "PROJECT_DIR=D:\Users\30776\IdeaProjects\enterprise-asset-management"

echo [INFO] Nacos Path: %NACOS_DIR%
echo.

echo [INFO] Cleaning port resources...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8848"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8082"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do taskkill /F /PID %%a 2>nul
echo.

echo [STEP 1] Starting Nacos Service Discovery...
if not exist "%NACOS_DIR%\startup.cmd" (
    echo [ERROR] Nacos not found at: %NACOS_DIR%
    pause
    exit /b 1
)
start "Nacos" cmd /k ""%NACOS_DIR%\startup.cmd" -m standalone"
echo [INFO] Waiting 15 seconds for Nacos...
timeout /t 15 /nobreak >nul
echo.

echo [STEP 2] Building Maven modules...
cd /d "%PROJECT_DIR%"
mvn clean install -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed!
    pause
    exit /b 1
)
echo [INFO] Maven build completed
echo.

echo [STEP 3] Starting Asset-Auth Service...
start "Auth" cmd /k "cd /d ""%PROJECT_DIR%"" & mvn -pl asset-auth spring-boot:run"
echo [INFO] Waiting 15 seconds for Auth service...
timeout /t 15 /nobreak >nul
echo.

echo [STEP 4] Starting Asset-Business Service...
start "Business" cmd /k "cd /d ""%PROJECT_DIR%"" & mvn -pl asset-business spring-boot:run"
echo [INFO] Waiting 10 seconds for Business service...
timeout /t 10 /nobreak >nul
echo.

echo [STEP 5] Starting Frontend Service...
start "Frontend" cmd /k "cd /d ""%PROJECT_DIR%\frontend"" & npm.cmd run dev"
echo.

echo ==============================================
echo [SUCCESS] All services started:
echo 1. Nacos: http://localhost:8848/nacos
echo 2. Auth Service: http://localhost:8081
echo 3. Business Service: http://localhost:8082
echo 4. Frontend: http://localhost:5173
echo ==============================================
echo [INFO] Close windows to stop services.
pause