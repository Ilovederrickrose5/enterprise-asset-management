package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.dto.AssetStatusDistributionDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DepartmentAssetStatsDTO;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.service.ReportService;
import com.enterprise.asset.enterpriseassetmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @GetMapping("/department-stats")
    public ResponseEntity<Result<List<DepartmentAssetStatsDTO>>> getDepartmentAssetStats() {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    // 检查用户角色
                    boolean isAdmin = "admin".equals(user.getRole());
                    boolean isLeader = "leader".equals(user.getRole());
                    boolean isManager = "manager".equals(user.getRole());

                    // 普通员工只能查看自己的资产数据
                    if (!isAdmin && !isLeader && !isManager) {
                        return ResponseEntity.ok(Result.error(403, "无权限访问部门资产统计"));
                    }

                    // 部门资产管理员和领导只能查看本部门数据
                    if ((isManager || isLeader) && user.getDeptId() != null) {
                        List<DepartmentAssetStatsDTO> stats = reportService
                                .getDepartmentAssetStatsByDepartment(user.getDeptId());
                        return ResponseEntity.ok(Result.success(stats));
                    }
                }
            }

            // 只有系统管理员可以查看所有部门数据
            List<DepartmentAssetStatsDTO> stats = reportService.getDepartmentAssetStats();
            return ResponseEntity.ok(Result.success(stats));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "获取部门资产统计失败"));
        }
    }

    @GetMapping("/department-stats/{departmentId}")
    public ResponseEntity<Result<List<DepartmentAssetStatsDTO>>> getDepartmentAssetStatsByDepartment(
            @PathVariable Long departmentId) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    // 检查用户角色
                    boolean isAdmin = "admin".equals(user.getRole());

                    // 只有系统管理员可以查看任意部门数据
                    // 领导和部门资产管理员只能查看本部门数据
                    if (!isAdmin && (user.getDeptId() == null || !user.getDeptId().equals(departmentId))) {
                        return ResponseEntity.ok(Result.error(403, "无权限访问该部门资产统计"));
                    }
                }
            }

            List<DepartmentAssetStatsDTO> stats = reportService.getDepartmentAssetStatsByDepartment(departmentId);
            return ResponseEntity.ok(Result.success(stats));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "获取部门资产统计失败"));
        }
    }

    @GetMapping("/status-distribution")
    public ResponseEntity<Result<List<AssetStatusDistributionDTO>>> getAssetStatusDistribution() {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    // 检查用户角色
                    boolean isAdmin = "admin".equals(user.getRole());
                    boolean isLeader = "leader".equals(user.getRole());
                    boolean isManager = "manager".equals(user.getRole());

                    // 普通员工只能查看自己的资产数据
                    if (!isAdmin && !isLeader && !isManager) {
                        return ResponseEntity.ok(Result.error(403, "无权限访问资产状态分布"));
                    }

                    // 部门资产管理员和领导只能查看本部门数据
                    if ((isManager || isLeader) && user.getDeptId() != null) {
                        List<AssetStatusDistributionDTO> distribution = reportService
                                .getAssetStatusDistributionByDepartment(user.getDeptId());
                        return ResponseEntity.ok(Result.success(distribution));
                    }
                }
            }

            // 只有系统管理员可以查看所有资产数据
            List<AssetStatusDistributionDTO> distribution = reportService.getAssetStatusDistribution();
            return ResponseEntity.ok(Result.success(distribution));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "获取资产状态分布失败"));
        }
    }
}
