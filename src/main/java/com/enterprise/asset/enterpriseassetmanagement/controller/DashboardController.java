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

    @GetMapping("/stats")
    public ResponseEntity<Result<DashboardStatsDTO>> getStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }

        User user = userOpt.get();
        DashboardStatsDTO stats;

        // 根据用户角色返回不同的统计数据
        boolean isAdmin = "admin".equals(user.getRole());
        boolean isManager = "manager".equals(user.getRole());
        boolean isLeader = "leader".equals(user.getRole());

        if (isAdmin) {
            stats = dashboardService.getStatsForAdmin();
        } else if (isManager || isLeader) {
            stats = dashboardService.getStatsForDepartment(user.getDeptId());
        } else {
            stats = dashboardService.getStatsForUser(user.getId());
        }

        return ResponseEntity.ok(Result.success(stats));
    }

    // 测试接口，直接查询资产表
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

    // 获取最近操作记录（旧版本，保持兼容性）
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

    // 获取仪表盘操作数据（新版，包含待处理任务和最近动态）
    @GetMapping("/operations")
    public ResponseEntity<Result<DashboardOperationsDTO>> getDashboardOperations(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(Result.error(401, "用户未登录"));
            }

            User user = userOpt.get();
            DashboardOperationsDTO operations = recentOperationService.getDashboardOperations(limit, user);
            return ResponseEntity.ok(Result.success(operations));
        } catch (Exception e) {
            System.err.println("获取仪表盘操作数据失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "获取仪表盘操作数据失败"));
        }
    }
}
