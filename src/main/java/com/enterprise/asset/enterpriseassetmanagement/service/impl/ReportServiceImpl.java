package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.dto.AssetStatusDistributionDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DepartmentAssetStatsDTO;
import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.DepartmentRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import com.enterprise.asset.enterpriseassetmanagement.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public List<DepartmentAssetStatsDTO> getDepartmentAssetStats() {
        List<Asset> assets = assetRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Department> departments = departmentRepository.findAll();

        // 创建用户ID到部门ID的映射
        Map<Long, Long> userToDeptMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getDeptId));

        // 创建部门ID到部门名称的映射
        Map<Long, String> deptIdToNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        // 按部门分组，优先使用资产的deptId，否则使用用户的deptId
        Map<Long, List<Asset>> assetsByDepartment = assets.stream()
                .collect(Collectors.groupingBy(asset -> {
                    if (asset.getDeptId() != null) {
                        return asset.getDeptId();
                    } else if (asset.getUserId() != null && userToDeptMap.containsKey(asset.getUserId())) {
                        return userToDeptMap.get(asset.getUserId());
                    } else {
                        return 0L; // 公共资产
                    }
                }));

        List<DepartmentAssetStatsDTO> statsList = new ArrayList<>();

        // 处理每个部门的数据
        for (Map.Entry<Long, List<Asset>> entry : assetsByDepartment.entrySet()) {
            Long departmentId = entry.getKey();
            
            List<Asset> departmentAssets = entry.getValue();

            DepartmentAssetStatsDTO stats = new DepartmentAssetStatsDTO();
            stats.setDepartmentId(departmentId);
            // 使用实际的部门名称
            stats.setDepartmentName(deptIdToNameMap.getOrDefault(departmentId, departmentId != 0L ? "部门" + departmentId : "公共资产"));
            stats.setAssetCount(departmentAssets.size());

            // 计算总价值
            double totalValue = departmentAssets.stream()
                    .map(Asset::getPurchasePrice)
                    .filter(price -> price != null)
                    .mapToDouble(java.math.BigDecimal::doubleValue)
                    .sum();
            stats.setTotalValue(totalValue);

            // 计算平均价值
            if (!departmentAssets.isEmpty()) {
                stats.setAverageValue(totalValue / departmentAssets.size());
            }

            // 统计各状态资产数量
            stats.setInUseCount(departmentAssets.stream().filter(a -> "using".equals(a.getStatus())).count());
            stats.setIdleCount(departmentAssets.stream().filter(a -> "in_stock".equals(a.getStatus())).count());
            stats.setMaintenanceCount(
                    departmentAssets.stream().filter(a -> "maintenance".equals(a.getStatus())).count());
            stats.setScrappedCount(departmentAssets.stream()
                    .filter(a -> "scrapped".equals(a.getStatus()) || "disposed".equals(a.getStatus())).count());

            statsList.add(stats);
        }

        return statsList;
    }

    @Override
    public List<AssetStatusDistributionDTO> getAssetStatusDistribution() {
        List<Asset> assets = assetRepository.findAll();
        long totalCount = assets.size();

        // 按状态分组
        Map<String, List<Asset>> assetsByStatus = assets.stream()
                .collect(Collectors.groupingBy(Asset::getStatus));

        List<AssetStatusDistributionDTO> distributionList = new ArrayList<>();

        // 定义所有可能的状态及其名称，按顺序排列
        List<String> statusOrder = List.of("scrapped", "using", "in_stock", "maintenance");
        Map<String, String> statusNameMap = Map.of(
                "in_stock", "在库",
                "using", "使用中",
                "maintenance", "维修中",
                "scrapped", "已报废",
                "disposed", "已报废");

        // 先处理已报废状态（合并scrapped和disposed）
        int scrappedCount = 0;
        double scrappedValue = 0.0;

        List<Asset> scrappedAssets = assetsByStatus.getOrDefault("scrapped", new ArrayList<>());
        List<Asset> disposedAssets = assetsByStatus.getOrDefault("disposed", new ArrayList<>());

        scrappedCount = scrappedAssets.size() + disposedAssets.size();

        // 计算已报废资产总价值
        double scrappedAssetsValue = scrappedAssets.stream()
                .map(Asset::getPurchasePrice)
                .filter(price -> price != null)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();

        double disposedAssetsValue = disposedAssets.stream()
                .map(Asset::getPurchasePrice)
                .filter(price -> price != null)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();

        scrappedValue = scrappedAssetsValue + disposedAssetsValue;

        // 创建已报废状态的分布对象
        AssetStatusDistributionDTO scrappedDistribution = new AssetStatusDistributionDTO();
        scrappedDistribution.setStatus("scrapped");
        scrappedDistribution.setStatusName("已报废");
        scrappedDistribution.setCount(scrappedCount);

        // 计算占比
        if (totalCount > 0) {
            scrappedDistribution.setPercentage((double) scrappedCount / totalCount * 100);
        } else {
            scrappedDistribution.setPercentage(0.0);
        }

        scrappedDistribution.setTotalValue(scrappedValue);

        // 添加已报废状态到结果列表
        distributionList.add(scrappedDistribution);

        // 处理其他状态
        for (String status : statusOrder) {
            // 跳过已报废状态，因为已经处理过了
            if ("scrapped".equals(status)) {
                continue;
            }

            String statusName = statusNameMap.get(status);
            List<Asset> statusAssets = assetsByStatus.getOrDefault(status, new ArrayList<>());

            AssetStatusDistributionDTO distribution = new AssetStatusDistributionDTO();
            distribution.setStatus(status);
            distribution.setStatusName(statusName);
            distribution.setCount(statusAssets.size());

            // 计算占比
            if (totalCount > 0) {
                distribution.setPercentage((double) statusAssets.size() / totalCount * 100);
            } else {
                distribution.setPercentage(0.0);
            }

            // 计算该状态资产总价值
            double statusValue = statusAssets.stream()
                    .map(Asset::getPurchasePrice)
                    .filter(price -> price != null)
                    .mapToDouble(java.math.BigDecimal::doubleValue)
                    .sum();
            distribution.setTotalValue(statusValue);

            // 添加到结果列表
            distributionList.add(distribution);
        }

        return distributionList;
    }

    @Override
    public List<DepartmentAssetStatsDTO> getDepartmentAssetStatsByDepartment(Long departmentId) {
        List<Asset> assets = assetRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Department> departments = departmentRepository.findAll();

        // 创建用户ID到部门ID的映射
        Map<Long, Long> userToDeptMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getDeptId));

        // 创建部门ID到部门名称的映射
        Map<Long, String> deptIdToNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        // 过滤出指定部门的资产，优先使用资产的deptId，否则使用用户的deptId
        List<Asset> departmentAssets = assets.stream()
                .filter(asset -> {
                    Long assetDeptId;
                    if (asset.getDeptId() != null) {
                        assetDeptId = asset.getDeptId();
                    } else if (asset.getUserId() != null && userToDeptMap.containsKey(asset.getUserId())) {
                        assetDeptId = userToDeptMap.get(asset.getUserId());
                    } else {
                        assetDeptId = 0L; // 公共资产
                    }
                    return assetDeptId.equals(departmentId);
                })
                .collect(Collectors.toList());

        List<DepartmentAssetStatsDTO> statsList = new ArrayList<>();

        // 处理指定部门的数据
        if (!departmentAssets.isEmpty()) {
            DepartmentAssetStatsDTO stats = new DepartmentAssetStatsDTO();
            stats.setDepartmentId(departmentId);
            // 使用实际的部门名称
            stats.setDepartmentName(deptIdToNameMap.getOrDefault(departmentId, "部门" + departmentId));
            stats.setAssetCount(departmentAssets.size());

            // 计算总价值
            double totalValue = departmentAssets.stream()
                    .map(Asset::getPurchasePrice)
                    .filter(price -> price != null)
                    .mapToDouble(java.math.BigDecimal::doubleValue)
                    .sum();
            stats.setTotalValue(totalValue);

            // 计算平均价值
            if (!departmentAssets.isEmpty()) {
                stats.setAverageValue(totalValue / departmentAssets.size());
            }

            // 统计各状态资产数量
            stats.setInUseCount(departmentAssets.stream().filter(a -> "using".equals(a.getStatus())).count());
            stats.setIdleCount(departmentAssets.stream().filter(a -> "in_stock".equals(a.getStatus())).count());
            stats.setMaintenanceCount(
                    departmentAssets.stream().filter(a -> "maintenance".equals(a.getStatus())).count());
            stats.setScrappedCount(departmentAssets.stream()
                    .filter(a -> "scrapped".equals(a.getStatus()) || "disposed".equals(a.getStatus())).count());

            statsList.add(stats);
        } else {
            // 如果指定部门没有资产，添加一个空的部门统计
            DepartmentAssetStatsDTO stats = new DepartmentAssetStatsDTO();
            stats.setDepartmentId(departmentId);
            // 使用实际的部门名称
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

        // 创建用户ID到部门ID的映射
        Map<Long, Long> userToDeptMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getDeptId));

        // 过滤出指定部门的资产，优先使用资产的deptId，否则使用用户的deptId
        List<Asset> departmentAssets = assets.stream()
                .filter(asset -> {
                    Long assetDeptId;
                    if (asset.getDeptId() != null) {
                        assetDeptId = asset.getDeptId();
                    } else if (asset.getUserId() != null && userToDeptMap.containsKey(asset.getUserId())) {
                        assetDeptId = userToDeptMap.get(asset.getUserId());
                    } else {
                        assetDeptId = 0L; // 公共资产
                    }
                    return assetDeptId.equals(departmentId);
                })
                .collect(Collectors.toList());

        long totalCount = departmentAssets.size();

        // 按状态分组
        Map<String, List<Asset>> assetsByStatus = departmentAssets.stream()
                .collect(Collectors.groupingBy(Asset::getStatus));

        List<AssetStatusDistributionDTO> distributionList = new ArrayList<>();

        // 定义所有可能的状态及其名称，按顺序排列
        List<String> statusOrder = List.of("scrapped", "using", "in_stock", "maintenance");
        Map<String, String> statusNameMap = Map.of(
                "in_stock", "在库",
                "using", "使用中",
                "maintenance", "维修中",
                "scrapped", "已报废",
                "disposed", "已报废");

        // 先处理已报废状态（合并scrapped和disposed）
        int scrappedCount = 0;
        double scrappedValue = 0.0;

        List<Asset> scrappedAssets = assetsByStatus.getOrDefault("scrapped", new ArrayList<>());
        List<Asset> disposedAssets = assetsByStatus.getOrDefault("disposed", new ArrayList<>());

        scrappedCount = scrappedAssets.size() + disposedAssets.size();

        // 计算已报废资产总价值
        double scrappedAssetsValue = scrappedAssets.stream()
                .map(Asset::getPurchasePrice)
                .filter(price -> price != null)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();

        double disposedAssetsValue = disposedAssets.stream()
                .map(Asset::getPurchasePrice)
                .filter(price -> price != null)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();

        scrappedValue = scrappedAssetsValue + disposedAssetsValue;

        // 创建已报废状态的分布对象
        AssetStatusDistributionDTO scrappedDistribution = new AssetStatusDistributionDTO();
        scrappedDistribution.setStatus("scrapped");
        scrappedDistribution.setStatusName("已报废");
        scrappedDistribution.setCount(scrappedCount);

        // 计算占比
        if (totalCount > 0) {
            scrappedDistribution.setPercentage((double) scrappedCount / totalCount * 100);
        } else {
            scrappedDistribution.setPercentage(0.0);
        }

        scrappedDistribution.setTotalValue(scrappedValue);

        // 添加已报废状态到结果列表
        distributionList.add(scrappedDistribution);

        // 处理其他状态
        for (String status : statusOrder) {
            // 跳过已报废状态，因为已经处理过了
            if ("scrapped".equals(status)) {
                continue;
            }

            String statusName = statusNameMap.get(status);
            List<Asset> statusAssets = assetsByStatus.getOrDefault(status, new ArrayList<>());

            AssetStatusDistributionDTO distribution = new AssetStatusDistributionDTO();
            distribution.setStatus(status);
            distribution.setStatusName(statusName);
            distribution.setCount(statusAssets.size());

            // 计算占比
            if (totalCount > 0) {
                distribution.setPercentage((double) statusAssets.size() / totalCount * 100);
            } else {
                distribution.setPercentage(0.0);
            }

            // 计算该状态资产总价值
            double statusValue = statusAssets.stream()
                    .map(Asset::getPurchasePrice)
                    .filter(price -> price != null)
                    .mapToDouble(java.math.BigDecimal::doubleValue)
                    .sum();
            distribution.setTotalValue(statusValue);

            // 添加到结果列表
            distributionList.add(distribution);
        }

        return distributionList;
    }
}
