package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.common.ApplicationStatus;
import com.enterprise.asset.enterpriseassetmanagement.dto.DashboardStatsDTO;
import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetApplicationRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.DepartmentRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** 仪表盘服务 - 处理首页统计数据查询（根据角色返回不同数据） */
@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final AssetRepository assetRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AssetApplicationRepository assetApplicationRepository;

    public DashboardService(AssetRepository assetRepository, DepartmentRepository departmentRepository,
            UserRepository userRepository, AssetApplicationRepository assetApplicationRepository) {
        this.assetRepository = assetRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.assetApplicationRepository = assetApplicationRepository;
    }

    /**
     * 获取管理员仪表盘统计数据
     * 统计内容：资产总数、待审批数量、部门数、用户数
     */
    // 如果Controller中判断为系统管理员，调用该方法
    public DashboardStatsDTO getStatsForAdmin() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        // 查询资产总数
        long assetCount = assetRepository.countAllAssets();
        stats.setAssetCount(assetCount);

        long totalPending = assetApplicationRepository.countByStatus(ApplicationStatus.PENDING.getCode());
        stats.setPendingCount(totalPending);
        stats.setDepartmentCount(departmentRepository.countByStatus(1));
        stats.setUserCount(userRepository.count());
        stats.setDepartmentName("公司");

        logger.info("系统管理员统计数据 - 资产总数: {}, 待审批总数: {}, 部门数: {}, 用户数: {}",
                assetCount, totalPending, stats.getDepartmentCount(), stats.getUserCount());

        return stats;
    }

    /**
     * 获取部门仪表盘统计数据
     * 统计内容：部门资产数、部门用户数
     */
    // 部门数据查询
    // 如果Controller中判断为部门领导或部门资产管理员，调用该方法
    public DashboardStatsDTO getStatsForDepartment(Long deptId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        long assetCount = assetRepository.countAllAssetsByDepartment(deptId);
        stats.setAssetCount(assetCount);

        stats.setPendingCount(0L);
        stats.setDepartmentCount(null);
        stats.setUserCount(userRepository.countByDeptId(deptId));

        Optional<Department> dept = departmentRepository.findById(deptId);
        stats.setDepartmentName(dept.map(Department::getDeptName).orElse("部门"));

        logger.info("部门统计数据 - 部门ID: {}, 资产数: {}, 用户数: {}",
                deptId, assetCount, stats.getUserCount());

        return stats;
    }

    /**
     * 获取个人仪表盘统计数据
     * 统计内容：个人资产数
     */
    // 个人数据查询
    // 如果Controller中判断为普通用户，调用该方法
    public DashboardStatsDTO getStatsForUser(Long userId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        // 查询个人资产数
        long assetCount = assetRepository.countAllAssetsByUser(userId);
        stats.setAssetCount(assetCount);

        stats.setPendingCount(0L);
        stats.setDepartmentCount(null);
        stats.setUserCount(1L);
        stats.setDepartmentName("个人");

        logger.info("个人统计数据 - 用户ID: {}, 资产数: {}", userId, assetCount);

        return stats;
    }
}
