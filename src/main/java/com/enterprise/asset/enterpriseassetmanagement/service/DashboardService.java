package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.dto.DashboardStatsDTO;
import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetApplicationRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.DepartmentRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** 仪表盘服务 - 处理首页统计数据查询（根据角色返回不同数据） */
@Service
public class DashboardService {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetApplicationRepository assetApplicationRepository;

    /**
     * 获取管理员仪表盘统计数据
     * 统计内容：资产总数、待审批数量、部门数、用户数
     */
    public DashboardStatsDTO getStatsForAdmin() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        long assetCount = assetRepository.countAllAssets();
        stats.setAssetCount(assetCount);

        long totalPending = assetApplicationRepository.countByStatus("pending");
        stats.setPendingCount(totalPending);
        stats.setDepartmentCount(departmentRepository.countByStatus(1));
        stats.setUserCount(userRepository.count());
        stats.setDepartmentName("公司");

        System.out.println("=== 系统管理员统计数据 ===");
        System.out.println("资产总数: " + assetCount);
        System.out.println("待审批总数: " + totalPending);
        System.out.println("部门数: " + stats.getDepartmentCount());
        System.out.println("用户数: " + stats.getUserCount());

        return stats;
    }

    /**
     * 获取部门仪表盘统计数据
     * 统计内容：部门资产数、部门用户数
     */
    public DashboardStatsDTO getStatsForDepartment(Long deptId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        long assetCount = assetRepository.countAllAssetsByDepartment(deptId);
        stats.setAssetCount(assetCount);

        stats.setPendingCount(0L);
        stats.setDepartmentCount(null);
        stats.setUserCount(userRepository.countByDeptId(deptId));

        Optional<Department> dept = departmentRepository.findById(deptId);
        stats.setDepartmentName(dept.map(Department::getDeptName).orElse("部门"));

        System.out.println("=== 部门统计数据 ===");
        System.out.println("部门ID: " + deptId);
        System.out.println("资产数: " + assetCount);
        System.out.println("用户数: " + stats.getUserCount());

        return stats;
    }

    /**
     * 获取个人仪表盘统计数据
     * 统计内容：个人资产数
     */
    public DashboardStatsDTO getStatsForUser(Long userId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        long assetCount = assetRepository.countAllAssetsByUser(userId);
        stats.setAssetCount(assetCount);

        stats.setPendingCount(0L);
        stats.setDepartmentCount(null);
        stats.setUserCount(1L);
        stats.setDepartmentName("个人");

        System.out.println("=== 个人统计数据 ===");
        System.out.println("用户ID: " + userId);
        System.out.println("资产数: " + assetCount);

        return stats;
    }
}
