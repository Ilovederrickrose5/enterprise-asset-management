package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.dto.DashboardOperationsDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DashboardStatsDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.RecentOperationDTO;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import com.enterprise.asset.enterpriseassetmanagement.service.DashboardService;
import com.enterprise.asset.enterpriseassetmanagement.service.RecentOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 仪表盘控制器 - 处理首页统计数据和操作记录（根据角色返回不同数据） */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private RecentOperationService recentOperationService;

    /**
     * GET /api/dashboard/stats - 获取仪表盘统计数据
     * 权限：admin查看全部统计，manager/leader查看部门统计，普通用户查看个人资产统计
     * 
     * @return 统计数据
     */
    // 获取系统概览统计数据
    @GetMapping("/stats")
    public ResponseEntity<Result<DashboardStatsDTO>> getStats() {
        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }

        User user = userOpt.get();
        DashboardStatsDTO stats;
        // 根据角色判断调用哪个Service方法
        boolean isAdmin = "admin".equals(user.getRole());
        boolean isManager = "manager".equals(user.getRole());
        boolean isLeader = "leader".equals(user.getRole());

        if (isAdmin) {
            // 全公司数据
            stats = dashboardService.getStatsForAdmin();
        } else if (isManager || isLeader) {
            // 部门数据
            stats = dashboardService.getStatsForDepartment(user.getDeptId());
        } else {
            // 个人数据
            stats = dashboardService.getStatsForUser(user.getId());
        }

        return ResponseEntity.ok(Result.success(stats));
    }

    /**
     * GET /api/dashboard/test-asset-count - 测试接口：查询资产总数
     * 
     * @return 资产统计结果
     */
    @GetMapping("/test-asset-count")
    public ResponseEntity<Result<Map<String, Object>>> testAssetCount() {
        Map<String, Object> result = new HashMap<>();
        try {
            long count = assetRepository.countAllAssets();
            result.put("assetCount", count);
            result.put("message", "查询成功");
            System.out.println("测试接口 - 资产总数: " + count);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("message", "查询失败");
            System.err.println("测试接口 - 查询失败: " + e.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * GET /api/dashboard/recent-operations - 获取最近操作记录（旧版本，保持兼容性）
     * 
     * @param limit 返回数量限制，默认10
     * @return 最近操作记录列表
     */
    @GetMapping("/recent-operations")
    public ResponseEntity<Result<List<RecentOperationDTO>>> getRecentOperations(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(Result.error(401, "用户未登录"));
            }

            User user = userOpt.get();
            List<RecentOperationDTO> operations = recentOperationService.getRecentOperations(limit, user);
            return ResponseEntity.ok(Result.success(operations));
        } catch (Exception e) {
            System.err.println("获取最近操作失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "获取最近操作失败"));
        }
    }

    /**
     * GET /api/dashboard/operations - 获取仪表盘操作数据（新版）
     * 包含待处理任务和最近动态
     * 
     * @param limit 返回数量限制，默认10
     * @return 仪表盘操作数据
     */
    @GetMapping("/operations")
    public ResponseEntity<Result<DashboardOperationsDTO>> getDashboardOperations(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            System.out.println("=== [operations接口] 调试日志: 获取操作数据, 用户: " + username + ", limit: " + limit);

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("=== [operations接口] 用户未找到, username: " + username);
                return ResponseEntity.ok(Result.error(401, "用户未登录"));
            }

            User user = userOpt.get();
            System.out.println("=== [operations接口] 用户信息, id: " + user.getId() + ", role: " + user.getRole() + ", deptId: " + user.getDeptId());
            
            DashboardOperationsDTO operations = recentOperationService.getDashboardOperations(limit, user);
            System.out.println("=== [operations接口] 操作数据获取成功, pendingTasks: " + (operations.getPendingTasks() != null ? operations.getPendingTasks().size() : 0) + ", recentActivities: " + (operations.getRecentActivities() != null ? operations.getRecentActivities().size() : 0));
            
            return ResponseEntity.ok(Result.success(operations));
        } catch (Exception e) {
            System.err.println("=== [operations接口] 获取仪表盘操作数据失败 ===");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常消息: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "获取仪表盘操作数据失败: " + e.getMessage()));
        }
    }
}
