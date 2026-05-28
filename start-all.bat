@echo off
chcp 65001 > nul
title 固定资产管理系统 - 一键启动前后端

echo ==============================================
echo  固定资产管理系统
echo  Trae 终端启动命令：
echo  .\start-all.bat
echo ==============================================
echo.

echo ========== 启动后端 ==========
start "后端服务" cmd /k "cd /d ""D:\Users\30776\IdeaProjects\enterprise-asset-management"" && mvn spring-boot:run"

echo ========== 启动前端 ==========
start "前端服务" cmd /k "cd /d ""D:\Users\30776\IdeaProjects\enterprise-asset-management\frontend"" && npm run dev"

echo.
echo 前后端启动中，请等待窗口弹出...
echo 关闭窗口即可停止服务
pause