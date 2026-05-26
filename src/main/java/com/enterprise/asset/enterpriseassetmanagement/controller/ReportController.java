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

/** 报表控制器 - 处理资产统计报表查询（权限控制：admin可查看全部，leader/manager查看本部门） */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    /**
     * GET /api/reports/department-stats - 获取部门资产统计
     * 权限：admin查看全部，leader/manager查看本部门，普通用户无权限
     * 
     * @return 部门资产统计列表
     */
    @GetMapping("/department-stats")
    public ResponseEntity<Result<List<DepartmentAssetStatsDTO>>> getDepartmentAssetStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    boolean isAdmin = "admin".equals(user.getRole());
                    boolean isLeader = "leader".equals(user.getRole());
                    boolean isManager = "manager".equals(user.getRole());
                    // 普通用户无权限访问部门资产统计
                    if (!isAdmin && !isLeader && !isManager) {
                        return ResponseEntity.ok(Result.error(403, "无权限访问部门资产统计"));
                    }
                    // 如果是系统管理员，可以查看全部部门资产统计
                    if ((isManager || isLeader) && user.getDeptId() != null) {
                        List<DepartmentAssetStatsDTO> stats = reportService
                                .getDepartmentAssetStatsByDepartment(user.getDeptId());
                        return ResponseEntity.ok(Result.success(stats));
                    }
                }
            }

            List<DepartmentAssetStatsDTO> stats = reportService.getDepartmentAssetStats();
            return ResponseEntity.ok(Result.success(stats));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "获取部门资产统计失败"));
        }
    }

    /**
     * GET /api/reports/department-stats/{departmentId} - 获取指定部门资产统计
     * 权限：admin可查看任意部门，leader/manager只能查看本部门
     * 
     * @param departmentId 部门ID
     * @return 部门资产统计列表
     */
    @GetMapping("/department-stats/{departmentId}")
    public ResponseEntity<Result<List<DepartmentAssetStatsDTO>>> getDepartmentAssetStatsByDepartment(
            @PathVariable Long departmentId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    boolean isAdmin = "admin".equals(user.getRole());

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

    /**
     * GET /api/reports/status-distribution - 获取资产状态分布
     * 权限：admin查看全部，leader/manager查看本部门，普通用户无权限
     * 
     * @return 资产状态分布列表
     */
    @GetMapping("/status-distribution")
    public ResponseEntity<Result<List<AssetStatusDistributionDTO>>> getAssetStatusDistribution() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    boolean isAdmin = "admin".equals(user.getRole());
                    boolean isLeader = "leader".equals(user.getRole());
                    boolean isManager = "manager".equals(user.getRole());

                    if (!isAdmin && !isLeader && !isManager) {
                        return ResponseEntity.ok(Result.error(403, "无权限访问资产状态分布"));
                    }

                    if ((isManager || isLeader) && user.getDeptId() != null) {
                        List<AssetStatusDistributionDTO> distribution = reportService
                                .getAssetStatusDistributionByDepartment(user.getDeptId());
                        return ResponseEntity.ok(Result.success(distribution));
                    }
                }
            }

            List<AssetStatusDistributionDTO> distribution = reportService.getAssetStatusDistribution();
            return ResponseEntity.ok(Result.success(distribution));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "获取资产状态分布失败"));
        }
    }
}
