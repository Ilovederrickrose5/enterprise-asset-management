package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.dto.LoginRequest;
import com.enterprise.asset.enterpriseassetmanagement.dto.LoginResponse;
import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import com.enterprise.asset.enterpriseassetmanagement.repository.DepartmentRepository;
import com.enterprise.asset.enterpriseassetmanagement.security.JwtUtil;
import com.enterprise.asset.enterpriseassetmanagement.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** 认证服务 - 处理用户登录认证与JWT生成 */
@Service
public class AuthService {

        private final AuthenticationManager authenticationManager;
        private final JwtUtil jwtUtil;
        private final DepartmentRepository departmentRepository;

        public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                DepartmentRepository departmentRepository) {
            this.authenticationManager = authenticationManager;
            this.jwtUtil = jwtUtil;
            this.departmentRepository = departmentRepository;
        }

        /**
         * 用户登录认证
         * 业务流程：用户名密码验证 → 获取用户详情 → 生成JWT令牌 → 查询部门名称 → 返回登录响应
         * 外部调用：AuthenticationManager认证、JwtUtil生成令牌、DepartmentRepository查询部门
         * @param loginRequest 登录请求（用户名、密码）
         * @return 登录响应（包含JWT、用户信息、角色列表）
         */
        public LoginResponse login(LoginRequest loginRequest) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                                                loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

                String jwt = jwtUtil.generateToken(userDetails);

                List<String> roles = userDetails.getAuthorities().stream()
                                .map(item -> item.getAuthority())
                                .collect(Collectors.toList());

                String departmentName = null;
                if (userDetails.getDepartmentId() != null) {
                        Department department = departmentRepository.findById(userDetails.getDepartmentId())
                                        .orElse(null);
                        if (department != null) {
                                departmentName = department.getDeptName();
                        }
                }

                return new LoginResponse(
                                jwt,
                                userDetails.getId(),
                                userDetails.getUsername(),
                                userDetails.getEmail(),
                                userDetails.getRealName(),
                                userDetails.getDepartmentId(),
                                departmentName,
                                roles);
        }
}
