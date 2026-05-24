package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.dto.LoginRequest;
import com.enterprise.asset.enterpriseassetmanagement.dto.LoginResponse;
import com.enterprise.asset.enterpriseassetmanagement.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/** 认证控制器 - 处理用户登录/登出 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    /**
     * POST /api/auth/login - 用户登录
     * @param loginRequest 登录请求体 {"username": "...", "password": "..."}
     * @return 登录响应（包含用户信息和权限）
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(Result.success(loginResponse));
    }
    
    /**
     * POST /api/auth/logout - 用户登出
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<Result<String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Result.success("登出成功"));
    }
}
