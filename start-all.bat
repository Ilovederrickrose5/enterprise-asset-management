@echo off
chcp 65001 > nul
title 企业资产管理系统 - 微服务一键启动

echo ==============================================
echo  企业资产管理系统（微服务架构）
echo  启动顺序：Nacos → Auth → Business → Frontend
echo ==============================================
echo.

:: 提示用户根据本地Nacos安装路径修改
echo [提示] 请确保已修改脚本内Nacos路径配置！
echo [提示] Nacos默认路径：C:\nacos\bin\startup.cmd
echo.

:: 清理端口残留进程
echo [清理] 释放端口资源
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8848"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8082"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do taskkill /F /PID %%a 2>nul
echo.

:: 第一步：启动Nacos服务发现
echo [启动] Nacos服务发现中心(端口8848)
echo [注意] 请根据您的Nacos安装路径修改下方命令！
start "Nacos服务【关闭窗口即停止】" cmd /k "cd /d ""D:\Users\307776\Downloads\nacos-server-3.2.2\nacos\bin"" && startup.cmd -m standalone"
timeout /t 10 /nobreak >nul

:: 第二步：启动asset-auth认证服务
echo [启动] Asset-Auth认证服务(端口8081)
start "Auth服务【关闭窗口即停止】" cmd /k "cd /d ""D:\Users\30776\IdeaProjects\enterprise-asset-management"" && mvn -pl asset-auth spring-boot:run"
timeout /t 15 /nobreak >nul

:: 第三步：启动asset-business业务服务
echo [启动] Asset-Business业务服务(端口8082)
start "Business服务【关闭窗口即停止】" cmd /k "cd /d ""D:\Users\30776\IdeaProjects\enterprise-asset-management"" && mvn -pl asset-business spring-boot:run"
timeout /t 10 /nobreak >nul

:: 第四步：启动前端服务
echo [启动] Vite前端服务(端口5173)
start "前端服务【关闭窗口即停止】" cmd /k "cd /d ""D:\Users\30776\IdeaProjects\enterprise-asset-management\frontend"" && npm.cmd run dev"

echo.
echo ==============================================
echo ✅ 微服务启动完成：
echo 1. Nacos控制台：http://localhost:8848/nacos
echo 2. Auth服务健康检查：http://localhost:8081/api/auth/login
echo 3. Business服务健康检查：http://localhost:8082/api/assets
echo 4. 前端访问地址：http://localhost:5173
echo.
echo 服务关闭说明：
echo - 关闭对应窗口即可停止服务
echo - Nacos关闭会触发服务注销
echo - 本控制台可直接关闭，不影响服务运行
echo ==============================================
pause