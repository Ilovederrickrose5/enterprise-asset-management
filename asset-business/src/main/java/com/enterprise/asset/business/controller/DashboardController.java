package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.common.dto.DashboardOperationsDTO;
import com.enterprise.asset.common.dto.DashboardStatsDTO;
import com.enterprise.asset.common.dto.RecentOperationDTO;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.business.client.AuthFeignClient;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.service.DashboardService;
import com.enterprise.asset.business.service.RecentOperationService;
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

/**
 * 仪表盘控制器 - 处理首页统计数据和操作记录
 * 
 * Feign改造说明：
 * - 删除了UserRepository本地DAO依赖
 * - 通过AuthFeignClient远程调用asset-auth服务获取用户信息
 * - 使用UserDTO替代User实体进行跨服务数据传输
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AssetRepository assetRepository;
    private final RecentOperationService recentOperationService;
    private final AuthFeignClient authFeignClient;

    public DashboardController(DashboardService dashboardService, 
            AssetRepository assetRepository,
            RecentOperationService recentOperationService,
            AuthFeignClient authFeignClient) {
        this.dashboardService = dashboardService;
        this.assetRepository = assetRepository;
        this.recentOperationService = recentOperationService;
        this.authFeignClient = authFeignClient;
    }

    /**
     * GET /api/dashboard/stats - 获取仪表盘统计数据
     * 权限：admin查看全部统计，manager/leader查看部门统计，普通用户查看个人资产统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Result<DashboardStatsDTO>> getStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Feign远程调用：根据用户名获取用户信息
        Result<UserDTO> userResult = authFeignClient.getUserByUsername(username);
        if (userResult.getCode() != 200 || userResult.getData() == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }

        UserDTO user = userResult.getData();
        DashboardStatsDTO stats;

        boolean isAdmin = user.getRoleCodes() != null && user.getRoleCodes().contains("admin");
        boolean isManager = user.getRoleCodes() != null && user.getRoleCodes().contains("manager");
        boolean isLeader = user.getRoleCodes() != null && user.getRoleCodes().contains("leader");

        if (isAdmin) {
            stats = dashboardService.getStatsForAdmin();
        } else if (isManager || isLeader) {
            stats = dashboardService.getStatsForDepartment(user.getDeptId());
        } else {
            stats = dashboardService.getStatsForUser(user.getId());
        }

        return ResponseEntity.ok(Result.success(stats));
    }

    /**
     * GET /api/dashboard/test-asset-count - 测试接口：查询资产总数
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
     * GET /api/dashboard/recent-operations - 获取最近操作记录
     */
    @GetMapping("/recent-operations")
    public ResponseEntity<Result<List<RecentOperationDTO>>> getRecentOperations(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Feign远程调用：根据用户名获取用户信息
            Result<UserDTO> userResult = authFeignClient.getUserByUsername(username);
            if (userResult.getCode() != 200 || userResult.getData() == null) {
                return ResponseEntity.ok(Result.error(401, "用户未登录"));
            }

            UserDTO user = userResult.getData();
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
     */
    @GetMapping("/operations")
    public ResponseEntity<Result<DashboardOperationsDTO>> getDashboardOperations(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            System.out.println("=== [operations接口] 调试日志: 获取操作数据, 用户: " + username + ", limit: " + limit);

            // Feign远程调用：根据用户名获取用户信息
            Result<UserDTO> userResult = authFeignClient.getUserByUsername(username);
            if (userResult.getCode() != 200 || userResult.getData() == null) {
                System.out.println("=== [operations接口] 用户未找到, username: " + username);
                return ResponseEntity.ok(Result.error(401, "用户未登录"));
            }

            UserDTO user = userResult.getData();
            System.out.println("=== [operations接口] 用户信息, id: " + user.getId() 
                    + ", roleCodes: " + user.getRoleCodes()
                    + ", deptId: " + user.getDeptId());

            DashboardOperationsDTO operations = recentOperationService.getDashboardOperations(limit, user);
            System.out.println("=== [operations接口] 操作数据获取成功, pendingTasks: "
                    + (operations.getPendingTasks() != null ? operations.getPendingTasks().size() : 0)
                    + ", recentActivities: "
                    + (operations.getRecentActivities() != null ? operations.getRecentActivities().size() : 0));

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