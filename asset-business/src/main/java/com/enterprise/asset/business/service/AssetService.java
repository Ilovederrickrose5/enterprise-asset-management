package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.SysLog;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.repository.SysLogRepository;
import com.enterprise.asset.common.dto.UserDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 资产服务 - 处理资产CRUD操作与权限控制 */
@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final SysLogRepository sysLogRepository;

    public AssetService(AssetRepository assetRepository, SysLogRepository sysLogRepository) {
        this.assetRepository = assetRepository;
        this.sysLogRepository = sysLogRepository;
    }

    /**
     * 获取当前用户的UserDTO信息（从SecurityContext中提取）
     */
    private UserDTO getCurrentUserDTO() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDTO) {
            return (UserDTO) principal;
        }
        return null;
    }

    /**
     * 获取所有资产（根据用户角色返回不同数据）
     * 权限规则：admin查看全部，manager/leader查看本部门，普通用户查看个人资产
     */
    public List<Asset> getAllAssets() {
        List<Asset> allAssets = assetRepository.findAll();

        // 【本次修改点】从SecurityContext直接获取UserDTO，不再查询本地UserRepository
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return List.of();
        }

        List<String> roles = userDTO.getRoleCodes();
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        boolean isLeader = roles != null && roles.contains("LEADER");
        boolean isManager = roles != null && roles.contains("MANAGER");

        if (isAdmin) {
            // 系统管理员：可以看到所有资产
            return allAssets;
        } else if (isLeader || isManager) {
            // 部门领导或部门资产管理员：只能看到本部门的资产
            if (userDTO.getDeptId() != null) {
                return allAssets.stream()
                        .filter(asset -> asset.getDeptId() != null && asset.getDeptId().equals(userDTO.getDeptId()))
                        .toList();
            }
            return List.of();
        } else {
            // 普通员工：只能看到自己的资产
            return allAssets.stream()
                    .filter(asset -> asset.getUserId() != null && asset.getUserId().equals(userDTO.getId()))
                    .toList();
        }
    }

    /**
     * 根据ID获取资产
     */
    public Asset getAssetById(Long id) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset != null) {
            // 获取当前用户信息
            UserDTO userDTO = getCurrentUserDTO();
            if (userDTO != null) {
                List<String> roles = userDTO.getRoleCodes();
                // 检查用户角色
                boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

                // 部门资产管理员只能查看本部门资产
                if (isManager && userDTO.getDeptId() != null) {
                    if (asset.getDeptId() == null
                            || !asset.getDeptId().equals(userDTO.getDeptId())) {
                        return null; // 返回null表示无权限访问
                    }
                }
            }
        }
        return asset;
    }

    /**
     * 创建资产
     */
    @Transactional
    public Asset createAsset(Asset asset) {
        // 获取当前用户信息
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            // 检查用户角色
            boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

            // 检查是否是领导角色
            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            // 领导角色不能创建资产
            if (isLeader) {
                throw new SecurityException("领导角色无权限创建资产");
            }

            // 部门资产管理员只能创建本部门的资产
            if (isManager && userDTO.getDeptId() != null) {
                // 强制设置为用户所在部门
                asset.setDeptId(userDTO.getDeptId());
            }
        }
        return assetRepository.save(asset);
    }

    /**
     * 更新资产
     */
    @Transactional
    public Asset updateAsset(Long id, Asset asset) {
        Asset existingAsset = assetRepository.findById(id).orElse(null);
        if (existingAsset == null) {
            return null;
        }

        // 获取当前用户信息
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            // 检查用户角色
            boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

            // 检查是否是领导角色
            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            // 领导角色不能更新资产
            if (isLeader) {
                throw new SecurityException("领导角色无权限更新资产");
            }

            // 部门资产管理员只能更新本部门的资产
            if (isManager && userDTO.getDeptId() != null) {
                if (existingAsset.getDeptId() == null
                        || !existingAsset.getDeptId().equals(userDTO.getDeptId())) {
                    return null; // 返回null表示无权限访问
                }
                // 部门资产管理员不能修改部门ID
                asset.setDeptId(existingAsset.getDeptId());
            }
        }

        existingAsset.setAssetName(asset.getAssetName());
        existingAsset.setCategoryId(asset.getCategoryId());
        existingAsset.setModel(asset.getModel());
        existingAsset.setUnit(asset.getUnit());
        existingAsset.setPurchasePrice(asset.getPurchasePrice());
        existingAsset.setNetValue(asset.getNetValue());
        existingAsset.setOriginalValue(asset.getOriginalValue());
        existingAsset.setSupplierId(asset.getSupplierId());
        existingAsset.setPurchaseDate(asset.getPurchaseDate());
        existingAsset.setWarrantyPeriod(asset.getWarrantyPeriod());
        existingAsset.setUsefulLife(asset.getUsefulLife());
        existingAsset.setDepreciationMethod(asset.getDepreciationMethod());
        existingAsset.setStatus(asset.getStatus());
        existingAsset.setUseStatus(asset.getUseStatus());
        existingAsset.setCustodianId(asset.getCustodianId());
        existingAsset.setDeptId(asset.getDeptId());
        existingAsset.setLocation(asset.getLocation());
        existingAsset.setRemark(asset.getRemark());
        existingAsset.setUserId(asset.getUserId());

        // 更新借出相关字段
        existingAsset.setBorrowStatus(asset.getBorrowStatus());
        existingAsset.setCurrentLocation(asset.getCurrentLocation());
        existingAsset.setBorrowerId(asset.getBorrowerId());
        existingAsset.setBorrowTime(asset.getBorrowTime());
        existingAsset.setExpectedReturnTime(asset.getExpectedReturnTime());

        return assetRepository.save(existingAsset);
    }

    /**
     * 删除资产
     */
    @Transactional
    public boolean deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return false;
        }

        // 获取当前用户信息
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            // 检查用户角色
            boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

            // 检查是否是领导角色
            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            // 领导角色不能删除资产
            if (isLeader) {
                throw new SecurityException("领导角色无权限删除资产");
            }

            // 部门资产管理员只能删除本部门的资产
            if (isManager && userDTO.getDeptId() != null) {
                if (asset.getDeptId() == null || !asset.getDeptId().equals(userDTO.getDeptId())) {
                    return false; // 返回false表示无权限删除
                }
            }
        }

        assetRepository.deleteById(id);
        return true;
    }

    /**
     * 更新资产状态
     */
    @Transactional
    public Asset updateAssetStatus(Long id, String status) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return null;
        }

        // 权限控制：检查当前用户是否有权限更新此资产状态
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            Long currentUserId = userDTO.getId();

            // 检查是否是领导角色
            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            // 领导角色不能更新资产状态
            if (isLeader) {
                throw new SecurityException("领导角色无权限更新资产状态");
            }

            // 检查是否是管理员
            boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("admin"));

            // 检查是否是部门资产管理员
            boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

            // 部门资产管理员只能更新本部门资产状态
            if (isManager && userDTO.getDeptId() != null) {
                if (asset.getDeptId() == null || !asset.getDeptId().equals(userDTO.getDeptId())) {
                    throw new SecurityException("部门资产管理员只能更新本部门资产状态");
                }
            }

            // 如果不是管理员也不是部门管理员，检查是否是资产的使用者或借用者
            if (!isAdmin && !isManager) {
                boolean isAssetUser = asset.getUserId() != null && asset.getUserId().equals(currentUserId);
                boolean isBorrower = "borrowed".equals(asset.getBorrowStatus()) && asset.getBorrowerId() != null
                        && asset.getBorrowerId().equals(currentUserId);

                if (!isAssetUser && !isBorrower) {
                    throw new SecurityException("无权限更新此资产状态");
                }
            }
        }

        asset.setStatus(status);

        // 当资产状态为在库、闲置或维修中时，清空使用人信息
        if ("in_stock".equals(status) || "idle".equals(status) || "maintenance".equals(status)) {
            asset.setUserId(null);
            asset.setUseStatus("idle");
        }

        Asset savedAsset = assetRepository.save(asset);

        // 添加操作记录
        try {
            if (userDTO != null) {
                SysLog log = new SysLog();
                log.setUserId(userDTO.getId());
                log.setUsername(userDTO.getUsername());
                log.setOperation("资产状态更新：" + asset.getAssetName() + " (" + asset.getAssetNo() + ")");
                log.setLogType("ASSET");
                log.setStatus("success");
                sysLogRepository.save(log);
            }
        } catch (Exception e) {
            // 记录操作失败不影响主流程
            System.err.println("添加操作记录失败: " + e.getMessage());
        }

        return savedAsset;
    }

    /**
     * 更新资产使用状态
     */
    @Transactional
    public Asset updateAssetUseStatus(Long id, String useStatus) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return null;
        }

        // 权限控制：检查当前用户是否有权限更新此资产使用状态
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            Long currentUserId = userDTO.getId();

            // 检查是否是领导角色
            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            // 领导角色不能更新资产使用状态
            if (isLeader) {
                throw new SecurityException("领导角色无权限更新资产使用状态");
            }

            // 检查是否是资产的使用者
            boolean isAssetUser = asset.getUserId() != null && asset.getUserId().equals(currentUserId);

            // 非管理员用户只能更新自己的资产
            if (!isAssetUser) {
                // 检查是否是管理员
                boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("admin"));

                // 检查是否是部门资产管理员
                boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

                if (!isAdmin && !isManager) {
                    throw new SecurityException("无权限更新此资产使用状态");
                }

                // 部门资产管理员只能更新本部门资产使用状态
                if (isManager && userDTO.getDeptId() != null) {
                    if (asset.getDeptId() == null || !asset.getDeptId().equals(userDTO.getDeptId())) {
                        throw new SecurityException("部门资产管理员只能更新本部门资产使用状态");
                    }
                }
            }
        }

        asset.setUseStatus(useStatus);

        // 当使用状态为闲置时，清空使用人信息
        if ("idle".equals(useStatus)) {
            asset.setUserId(null);
            asset.setStatus("idle");
        }

        return assetRepository.save(asset);
    }

    /**
     * 根据用户ID获取资产
     */
    public List<Asset> getAssetsByUser(Long userId) {
        return assetRepository.findAll().stream()
                .filter(asset -> asset.getUserId() != null && asset.getUserId().equals(userId))
                .toList();
    }

    /**
     * 根据状态获取资产
     */
    public List<Asset> getAssetsByStatus(String status) {
        return assetRepository.findAll().stream()
                .filter(asset -> asset.getStatus() != null && asset.getStatus().equals(status))
                .toList();
    }

    /**
     * 根据使用状态获取资产
     */
    public List<Asset> getAssetsByUseStatus(String useStatus) {
        return assetRepository.findAll().stream()
                .filter(asset -> asset.getUseStatus() != null && asset.getUseStatus().equals(useStatus))
                .toList();
    }

    /**
     * 获取资产总数
     */
    public long getAssetCount() {
        try {
            return assetRepository.countAllAssets();
        } catch (Exception e) {
            System.err.println("获取资产总数失败: " + e.getMessage());
            e.printStackTrace();
            // 发生异常时返回0，避免500错误
            return 0;
        }
    }

    /**
     * 获取用户资产数量
     */
    public long getAssetCountByUser(Long userId) {
        try {
            return assetRepository.countAllAssetsByUser(userId);
        } catch (Exception e) {
            System.err.println("获取用户资产数量失败: " + e.getMessage());
            e.printStackTrace();
            // 发生异常时返回0，避免500错误
            return 0;
        }
    }

    /**
     * 获取可领用的资产（未分配给用户的资产）
     * 用于资产领用申请
     */
    public List<Asset> getAvailableAssets() {
        List<Asset> allAssets = assetRepository.findAll();

        // 【本次修改点】使用getCurrentUserDTO获取用户信息
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return List.of();
        }

        // 获取所有未分配的资产（userId为空的资产）
        // 所有角色都可以看到所有未分配的资产用于领用申请
        List<Asset> filteredAssets = allAssets.stream()
                .filter(asset -> asset.getUserId() == null)
                .toList();

        return filteredAssets;
    }

    /**
     * 获取所有地点列表
     */
    public List<String> getAllLocations() {
        return assetRepository.findAll().stream()
                .map(Asset::getLocation)
                .filter(location -> location != null && !location.isEmpty())
                .distinct()
                .toList();
    }
}