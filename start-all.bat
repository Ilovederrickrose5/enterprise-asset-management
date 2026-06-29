@echo off
chcp 65001 > nul
title 固定资产管理系统 - 一键启动前后端

echo ==============================================
echo  固定资产管理系统
echo  Trae 终端启动命令：
echo  .\start-all.bat
echo ==============================================
echo.

:: 清理后端8080端口残留进程
echo [清理] 释放后端8080端口
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do taskkill /F /PID %%a 2>nul

:: 清理前端5173端口残留进程
echo [清理] 释放前端5173端口
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do taskkill /F /PID %%a
echo.

:: 启动后端独立窗口（整行命令不换行，规避语法错误）
echo [启动] SpringBoot后端服务(端口8080)
start "后端服务【关闭窗口即停止服务】" cmd /k "cd /d ""D:\Users\30776\IdeaProjects\enterprise-asset-management"" && mvn spring-boot:run"

:: 延时缓冲
timeout /t 2 /nobreak >nul

:: 启动前端独立窗口
echo [启动] Vite前端服务(端口5173)
start "前端服务【关闭窗口即停止服务】" cmd /k "cd /d ""D:\Users\30776\IdeaProjects\enterprise-asset-management\frontend"" && npm run dev"

echo.
echo ==============================================
echo ✅ 使用说明：
echo 1. 关闭「后端服务」弹窗 → 后端进程直接终止
echo 2. 关闭「前端服务」弹窗 → 前端进程直接终止
echo 3. 本启动控制台可直接关闭，不影响前后端运行
echo ==============================================
pause