package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.common.AssetStatus;
import com.enterprise.asset.enterpriseassetmanagement.dto.AssetStatusDistributionDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DepartmentAssetStatsDTO;
import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.DepartmentRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import com.enterprise.asset.enterpriseassetmanagement.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 报表服务实现 - 处理资产统计报表查询（权限控制：admin可查看全部，leader/manager查看本部门） */
@Service
public class ReportServiceImpl implements ReportService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public ReportServiceImpl(AssetRepository assetRepository, UserRepository userRepository,
            DepartmentRepository departmentRepository) {
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    /** 状态处理顺序 */
    private static final List<AssetStatus> STATUS_ORDER = List.of(
            AssetStatus.SCRAPPED, AssetStatus.USING, AssetStatus.IN_STOCK, AssetStatus.MAINTENANCE);

    /**
     * 获取所有部门资产统计
     * 业务逻辑：按部门分组资产→统计资产数量、总价值、平均价值→统计各状态资产数量
     */
    @Override
    // 部门资产统计业务逻辑
    public List<DepartmentAssetStatsDTO> getDepartmentAssetStats() {
        List<Asset> assets = assetRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Department> departments = departmentRepository.findAll();

        Map<Long, Long> userToDeptMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getDeptId));
        Map<Long, String> deptIdToNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        // 按部门分组
        Map<Long, List<Asset>> assetsByDepartment = assets.stream()
                .collect(Collectors.groupingBy(asset -> getAssetDepartmentId(asset, userToDeptMap)));

        // 构建统计结果
        List<DepartmentAssetStatsDTO> statsList = new ArrayList<>();
        for (Map.Entry<Long, List<Asset>> entry : assetsByDepartment.entrySet()) {
            statsList.add(buildDepartmentStats(entry.getKey(), entry.getValue(), deptIdToNameMap));
        }

        return statsList;
    }

    @Override
    // 资产状态分布业务逻辑
    public List<AssetStatusDistributionDTO> getAssetStatusDistribution() {
        List<Asset> assets = assetRepository.findAll();
        return buildStatusDistribution(assets);
    }

    @Override
    public List<DepartmentAssetStatsDTO> getDepartmentAssetStatsByDepartment(Long departmentId) {
        List<Asset> assets = assetRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Department> departments = departmentRepository.findAll();

        Map<Long, Long> userToDeptMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getDeptId));
        Map<Long, String> deptIdToNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        // 过滤出指定部门的资产
        List<Asset> departmentAssets = assets.stream()
                .filter(asset -> getAssetDepartmentId(asset, userToDeptMap).equals(departmentId))
                .collect(Collectors.toList());

        List<DepartmentAssetStatsDTO> statsList = new ArrayList<>();
        if (!departmentAssets.isEmpty()) {
            statsList.add(buildDepartmentStats(departmentId, departmentAssets, deptIdToNameMap));
        } else {
            // 如果指定部门没有资产，添加一个空的部门统计
            DepartmentAssetStatsDTO stats = new DepartmentAssetStatsDTO();
            stats.setDepartmentId(departmentId);
            stats.setDepartmentName(deptIdToNameMap.getOrDefault(departmentId, "部门" + departmentId));
            stats.setAssetCount(0);
            stats.setTotalValue(0.0);
            stats.setAverageValue(0.0);
            stats.setInUseCount(0);
            stats.setIdleCount(0);
            stats.setMaintenanceCount(0);
            stats.setScrappedCount(0);
            statsList.add(stats);
        }

        return statsList;
    }

    @Override
    public List<AssetStatusDistributionDTO> getAssetStatusDistributionByDepartment(Long departmentId) {
        List<Asset> assets = assetRepository.findAll();
        List<User> users = userRepository.findAll();

        Map<Long, Long> userToDeptMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getDeptId));

        // 过滤出指定部门的资产
        List<Asset> departmentAssets = assets.stream()
                .filter(asset -> getAssetDepartmentId(asset, userToDeptMap).equals(departmentId))
                .collect(Collectors.toList());

        return buildStatusDistribution(departmentAssets);
    }

    // ==================== 私有工具方法（抽取公共逻辑） ====================

    /**
     * 计算资产所属部门ID
     * 优先使用资产的deptId，否则使用用户的deptId，最后返回0L表示公共资产
     */
    private Long getAssetDepartmentId(Asset asset, Map<Long, Long> userToDeptMap) {
        if (asset.getDeptId() != null) {
            return asset.getDeptId();
        } else if (asset.getUserId() != null && userToDeptMap.containsKey(asset.getUserId())) {
            return userToDeptMap.get(asset.getUserId());
        } else {
            return 0L;
        }
    }

    /**
     * 计算资产列表总价值
     */
    private double calculateTotalValue(List<Asset> assets) {
        return assets.stream()
                .map(Asset::getPurchasePrice)
                .filter(price -> price != null)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();
    }

    /**
     * 统计资产列表中各状态的数量
     */
    private Map<String, Long> countAssetsByStatus(List<Asset> assets) {
        return assets.stream()
                .collect(Collectors.groupingBy(Asset::getStatus, Collectors.counting()));
    }

    /**
     * 构建部门资产统计对象
     */
    private DepartmentAssetStatsDTO buildDepartmentStats(Long departmentId, List<Asset> departmentAssets,
            Map<Long, String> deptIdToNameMap) {
        DepartmentAssetStatsDTO stats = new DepartmentAssetStatsDTO();
        stats.setDepartmentId(departmentId);
        stats.setDepartmentName(deptIdToNameMap.getOrDefault(departmentId,
                departmentId != 0L ? "部门" + departmentId : "公共资产"));
        stats.setAssetCount(departmentAssets.size());

        double totalValue = calculateTotalValue(departmentAssets);
        stats.setTotalValue(totalValue);

        if (!departmentAssets.isEmpty()) {
            stats.setAverageValue(totalValue / departmentAssets.size());
        }

        Map<String, Long> statusCounts = countAssetsByStatus(departmentAssets);
        stats.setInUseCount(statusCounts.getOrDefault(AssetStatus.USING.getCode(), 0L));
        stats.setIdleCount(statusCounts.getOrDefault(AssetStatus.IN_STOCK.getCode(), 0L));
        stats.setMaintenanceCount(statusCounts.getOrDefault(AssetStatus.MAINTENANCE.getCode(), 0L));
        stats.setScrappedCount(statusCounts.getOrDefault(AssetStatus.SCRAPPED.getCode(), 0L)
                + statusCounts.getOrDefault(AssetStatus.DISPOSED.getCode(), 0L));

        return stats;
    }

    /**
     * 构建资产状态分布对象（通用方法）
     */
    private List<AssetStatusDistributionDTO> buildStatusDistribution(List<Asset> assets) {
        long totalCount = assets.size();
        Map<String, List<Asset>> assetsByStatus = assets.stream()
                .collect(Collectors.groupingBy(Asset::getStatus));

        List<AssetStatusDistributionDTO> distributionList = new ArrayList<>();

        // 先处理已报废状态（合并scrapped和disposed）
        List<Asset> scrappedAssets = assetsByStatus.getOrDefault(AssetStatus.SCRAPPED.getCode(), new ArrayList<>());
        List<Asset> disposedAssets = assetsByStatus.getOrDefault(AssetStatus.DISPOSED.getCode(), new ArrayList<>());

        int scrappedCount = scrappedAssets.size() + disposedAssets.size();
        double scrappedValue = calculateTotalValue(scrappedAssets) + calculateTotalValue(disposedAssets);

        AssetStatusDistributionDTO scrappedDistribution = new AssetStatusDistributionDTO();
        scrappedDistribution.setStatus(AssetStatus.SCRAPPED.getCode());
        scrappedDistribution.setStatusName(AssetStatus.SCRAPPED.getName());
        scrappedDistribution.setCount(scrappedCount);
        scrappedDistribution.setPercentage(totalCount > 0 ? (double) scrappedCount / totalCount * 100 : 0.0);
        scrappedDistribution.setTotalValue(scrappedValue);
        distributionList.add(scrappedDistribution);

        // 处理其他状态
        for (AssetStatus status : STATUS_ORDER) {
            if (status == AssetStatus.SCRAPPED) {
                continue;
            }

            List<Asset> statusAssets = assetsByStatus.getOrDefault(status.getCode(), new ArrayList<>());
            AssetStatusDistributionDTO distribution = new AssetStatusDistributionDTO();
            distribution.setStatus(status.getCode());
            distribution.setStatusName(status.getName());
            distribution.setCount(statusAssets.size());
            distribution.setPercentage(totalCount > 0 ? (double) statusAssets.size() / totalCount * 100 : 0.0);
            distribution.setTotalValue(calculateTotalValue(statusAssets));
            distributionList.add(distribution);
        }

        return distributionList;
    }
}
