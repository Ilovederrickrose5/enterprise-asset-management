package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.SysLog;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.repository.SysLogRepository;
import com.enterprise.asset.common.dto.UserDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final SysLogRepository sysLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final int CACHE_EXPIRE_MINUTES = 5;

    public AssetService(AssetRepository assetRepository, SysLogRepository sysLogRepository,
                        RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.assetRepository = assetRepository;
        this.sysLogRepository = sysLogRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

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

    public List<Asset> getAllAssets() {
        List<Asset> allAssets = assetRepository.findAll();

        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return List.of();
        }

        List<String> roles = userDTO.getRoleCodes();
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        boolean isLeader = roles != null && roles.contains("LEADER");
        boolean isManager = roles != null && roles.contains("MANAGER");

        if (isAdmin) {
            return allAssets;
        } else if (isLeader || isManager) {
            if (userDTO.getDeptId() != null) {
                return allAssets.stream()
                        .filter(asset -> asset.getDeptId() != null && asset.getDeptId().equals(userDTO.getDeptId()))
                        .toList();
            }
            return List.of();
        } else {
            return allAssets.stream()
                    .filter(asset -> asset.getUserId() != null && asset.getUserId().equals(userDTO.getId()))
                    .toList();
        }
    }

    public Page<Asset> getAssetsWithPagination(int page, int size, String status, String sortBy, String sortDir) {
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return Page.empty();
        }

        List<String> roles = userDTO.getRoleCodes();
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        boolean isLeaderOrManager = roles != null && (roles.contains("LEADER") || roles.contains("MANAGER"));

        String cacheKey = buildCacheKey(page, size, status, sortBy, sortDir, userDTO.getId(), isAdmin, isLeaderOrManager, userDTO.getDeptId());

        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null) {
            try {
                String cachedData = objectMapper.writeValueAsString(cachedObj);
                return objectMapper.readValue(cachedData, new TypeReference<Page<Asset>>() {});
            } catch (JsonProcessingException e) {
                System.err.println("Redis cache deserialization error: " + e.getMessage());
            }
        }

        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Asset> result;
        if (isAdmin) {
            if (status != null && !status.isEmpty()) {
                result = assetRepository.findByStatus(status, pageable);
            } else {
                result = assetRepository.findAll(pageable);
            }
        } else if (isLeaderOrManager) {
            Long deptId = userDTO.getDeptId();
            if (deptId != null) {
                if (status != null && !status.isEmpty()) {
                    result = assetRepository.findByDeptIdAndStatus(deptId, status, pageable);
                } else {
                    result = assetRepository.findByDeptId(deptId, pageable);
                }
            } else {
                result = Page.empty();
            }
        } else {
            result = assetRepository.findByUserId(userDTO.getId(), pageable);
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("Redis cache set error: " + e.getMessage());
        }

        return result;
    }

    private String buildCacheKey(int page, int size, String status, String sortBy, String sortDir,
                                  Long userId, boolean isAdmin, boolean isLeaderOrManager, Long deptId) {
        return String.format("asset_list:user_%d:admin_%s:leader_%s:dept_%s:page_%d:size_%d:status_%s:sort_%s_%s",
                userId, isAdmin, isLeaderOrManager, deptId != null ? deptId : "null",
                page, size, status != null ? status : "all", sortBy, sortDir);
    }

    private void clearAssetCache() {
        try {
            redisTemplate.delete(redisTemplate.keys("asset_list:*"));
        } catch (Exception e) {
            System.err.println("Clear asset cache error: " + e.getMessage());
        }
    }

    public Asset getAssetById(Long id) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset != null) {
            UserDTO userDTO = getCurrentUserDTO();
            if (userDTO != null) {
                List<String> roles = userDTO.getRoleCodes();
                boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

                if (isManager && userDTO.getDeptId() != null) {
                    if (asset.getDeptId() == null
                            || !asset.getDeptId().equals(userDTO.getDeptId())) {
                        return null;
                    }
                }
            }
        }
        return asset;
    }

    @Transactional
    public Asset createAsset(Asset asset) {
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));
            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            if (isLeader) {
                throw new SecurityException("领导角色无权限创建资产");
            }

            if (isManager && userDTO.getDeptId() != null) {
                asset.setDeptId(userDTO.getDeptId());
            }
        }
        Asset savedAsset = assetRepository.save(asset);
        clearAssetCache();
        return savedAsset;
    }

    @Transactional
    public Asset updateAsset(Long id, Asset asset) {
        Asset existingAsset = assetRepository.findById(id).orElse(null);
        if (existingAsset == null) {
            return null;
        }

        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));
            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            if (isLeader) {
                throw new SecurityException("领导角色无权限更新资产");
            }

            if (isManager && userDTO.getDeptId() != null) {
                if (existingAsset.getDeptId() == null
                        || !existingAsset.getDeptId().equals(userDTO.getDeptId())) {
                    return null;
                }
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

        existingAsset.setBorrowStatus(asset.getBorrowStatus());
        existingAsset.setCurrentLocation(asset.getCurrentLocation());
        existingAsset.setBorrowerId(asset.getBorrowerId());
        existingAsset.setBorrowTime(asset.getBorrowTime());
        existingAsset.setExpectedReturnTime(asset.getExpectedReturnTime());

        Asset savedAsset = assetRepository.save(existingAsset);
        clearAssetCache();
        return savedAsset;
    }

    @Transactional
    public boolean deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return false;
        }

        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));
            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            if (isLeader) {
                throw new SecurityException("领导角色无权限删除资产");
            }

            if (isManager && userDTO.getDeptId() != null) {
                if (asset.getDeptId() == null || !asset.getDeptId().equals(userDTO.getDeptId())) {
                    return false;
                }
            }
        }

        assetRepository.deleteById(id);
        clearAssetCache();
        return true;
    }

    @Transactional
    public Asset updateAssetStatus(Long id, String status) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return null;
        }

        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            Long currentUserId = userDTO.getId();

            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            if (isLeader) {
                throw new SecurityException("领导角色无权限更新资产状态");
            }

            boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("admin"));
            boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

            if (isManager && userDTO.getDeptId() != null) {
                if (asset.getDeptId() == null || !asset.getDeptId().equals(userDTO.getDeptId())) {
                    throw new SecurityException("部门资产管理员只能更新本部门资产状态");
                }
            }

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

        if ("in_stock".equals(status) || "idle".equals(status) || "maintenance".equals(status)) {
            asset.setUserId(null);
            asset.setUseStatus("idle");
        }

        Asset savedAsset = assetRepository.save(asset);
        clearAssetCache();

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
            System.err.println("添加操作记录失败: " + e.getMessage());
        }

        return savedAsset;
    }

    @Transactional
    public Asset updateAssetUseStatus(Long id, String useStatus) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return null;
        }

        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO != null) {
            List<String> roles = userDTO.getRoleCodes();
            Long currentUserId = userDTO.getId();

            boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("leader"));

            if (isLeader) {
                throw new SecurityException("领导角色无权限更新资产使用状态");
            }

            boolean isAssetUser = asset.getUserId() != null && asset.getUserId().equals(currentUserId);

            if (!isAssetUser) {
                boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("admin"));
                boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

                if (!isAdmin && !isManager) {
                    throw new SecurityException("无权限更新此资产使用状态");
                }

                if (isManager && userDTO.getDeptId() != null) {
                    if (asset.getDeptId() == null || !asset.getDeptId().equals(userDTO.getDeptId())) {
                        throw new SecurityException("部门资产管理员只能更新本部门资产使用状态");
                    }
                }
            }
        }

        asset.setUseStatus(useStatus);

        if ("idle".equals(useStatus)) {
            asset.setUserId(null);
            asset.setStatus("idle");
        }

        Asset savedAsset = assetRepository.save(asset);
        clearAssetCache();
        return savedAsset;
    }

    public List<Asset> getAssetsByUser(Long userId) {
        return assetRepository.findAll().stream()
                .filter(asset -> asset.getUserId() != null && asset.getUserId().equals(userId))
                .toList();
    }

    public List<Asset> getAssetsByStatus(String status) {
        return assetRepository.findAll().stream()
                .filter(asset -> asset.getStatus() != null && asset.getStatus().equals(status))
                .toList();
    }

    public List<Asset> getAssetsByUseStatus(String useStatus) {
        return assetRepository.findAll().stream()
                .filter(asset -> asset.getUseStatus() != null && asset.getUseStatus().equals(useStatus))
                .toList();
    }

    public long getAssetCount() {
        try {
            return assetRepository.countAllAssets();
        } catch (Exception e) {
            System.err.println("获取资产总数失败: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public long getAssetCountByUser(Long userId) {
        try {
            return assetRepository.countAllAssetsByUser(userId);
        } catch (Exception e) {
            System.err.println("获取用户资产数量失败: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public List<Asset> getAvailableAssets() {
        List<Asset> allAssets = assetRepository.findAll();

        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return List.of();
        }

        List<Asset> filteredAssets = allAssets.stream()
                .filter(asset -> asset.getUserId() == null)
                .toList();

        return filteredAssets;
    }

    public List<String> getAllLocations() {
        return assetRepository.findAll().stream()
                .map(Asset::getLocation)
                .filter(location -> location != null && !location.isEmpty())
                .distinct()
                .toList();
    }
}