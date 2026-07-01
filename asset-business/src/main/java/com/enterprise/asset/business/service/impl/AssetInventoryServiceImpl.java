package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.common.enums.InventoryStatus;
import com.enterprise.asset.common.enums.LogType;
import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.AssetInventory;
import com.enterprise.asset.business.entity.AssetInventoryDetail;
import com.enterprise.asset.business.entity.SysLog;
import com.enterprise.asset.business.repository.AssetInventoryDetailRepository;
import com.enterprise.asset.business.repository.AssetInventoryRepository;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.repository.SysLogRepository;
import com.enterprise.asset.business.service.AssetInventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 资产盘点服务实现 - 处理盘点计划管理与执行(含权限校验、任务分配、结果统计) */
@Service
public class AssetInventoryServiceImpl implements AssetInventoryService {

    private final AssetInventoryRepository assetInventoryRepository;
    private final AssetInventoryDetailRepository assetInventoryDetailRepository;
    private final AssetRepository assetRepository;
    private final SysLogRepository sysLogRepository;

    public AssetInventoryServiceImpl(AssetInventoryRepository assetInventoryRepository,
            AssetInventoryDetailRepository assetInventoryDetailRepository, AssetRepository assetRepository,
            SysLogRepository sysLogRepository) {
        this.assetInventoryRepository = assetInventoryRepository;
        this.assetInventoryDetailRepository = assetInventoryDetailRepository;
        this.assetRepository = assetRepository;
        this.sysLogRepository = sysLogRepository;
    }

    @Override
    @Transactional
    public AssetInventory createPlan(AssetInventory plan) {
        if (!canCreatePlan()) {
            throw new RuntimeException("您没有创建盘点计划的权限");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();

            if (!isAdmin(username)) {
                String inventoryScope = plan.getInventoryScope();
                String userDept = getUserDepartment(username);
                if (userDept != null && !userDept.isEmpty() && !"全部资产".equals(inventoryScope)) {
                    if (!userDept.equals(inventoryScope)) {
                        throw new RuntimeException("您只能创建本部门的盘点计划");
                    }
                } else if ("全部资产".equals(inventoryScope)) {
                    throw new RuntimeException("您不能创建全部资产的盘点计划");
                }
            }
        }

        String inventoryNo = generateInventoryNo();
        plan.setInventoryNo(inventoryNo);

        if (plan.getPlanName() == null || plan.getPlanName().isEmpty()) {
            plan.setPlanName(plan.getInventoryName());
        }

        plan.setStatus(InventoryStatus.PENDING.getCode());
        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        generateInventoryDetails(savedPlan);

        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();
        saveInventoryLog(savedPlan.getCreatorId(), savedPlan.getCreatorName(), "创建盘点计划",
                "创建了盘点计划: " + planName, LogType.INVENTORY.getCode());

        return savedPlan;
    }

    private void generateInventoryDetails(AssetInventory plan) {
        List<Asset> assets = new ArrayList<>();
        String scope = plan.getInventoryScope();

        if ("全部资产".equals(scope)) {
            assets = assetRepository.findAll();
        }

        for (Asset asset : assets) {
            AssetInventoryDetail detail = new AssetInventoryDetail();
            detail.setInventoryId(plan.getId());
            detail.setAssetId(asset.getId());
            detail.setAssetNo(asset.getAssetNo());
            detail.setAssetName(asset.getAssetName());
            detail.setSystemQuantity(1);
            detail.setActualQuantity(0);
            detail.setDifferenceQuantity(0);
            detail.setStatus(InventoryStatus.PENDING.getCode());
            detail.setCreateTime(LocalDateTime.now());
            if (plan.getCreatorId() != null) {
                detail.setInventoryPersonId(plan.getCreatorId());
                detail.setInventoryPersonName(plan.getCreatorName());
            }
            assetInventoryDetailRepository.save(detail);
        }
    }

    private String generateInventoryNo() {
        LocalDateTime now = LocalDateTime.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String prefix = "INV" + year + month;

        List<AssetInventory> thisMonthPlans = assetInventoryRepository
                .findByInventoryNoStartingWith(prefix);
        int maxSeq = 0;
        for (AssetInventory plan : thisMonthPlans) {
            String no = plan.getInventoryNo();
            if (no != null && no.length() >= 13 && no.startsWith(prefix)) {
                String seqStr = no.substring(no.length() - 5);
                try {
                    int seq = Integer.parseInt(seqStr);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        int nextSeq = maxSeq + 1;
        return prefix + String.format("%05d", nextSeq);
    }

    @Override
    public AssetInventory getPlanById(Long id) {
        return assetInventoryRepository.findById(id).orElse(null);
    }

    @Override
    public List<AssetInventory> getAllPlans() {
        return assetInventoryRepository.findAll();
    }

    @Override
    public List<AssetInventory> getPlansByCreatorOrAssignee(Long userId) {
        return assetInventoryRepository.findByCreatorIdOrAssigneeId(userId, userId);
    }

    @Override
    public List<AssetInventory> getPlansByUserAndDept(Long userId, String deptName) {
        return assetInventoryRepository.findByCreatorIdOrAssigneeIdOrInventoryScope(userId, userId, deptName);
    }

    public List<AssetInventory> getPlansByStatus(String status) {
        return assetInventoryRepository.findByStatus(status);
    }

    @Override
    public List<AssetInventory> getPlansByCreatorId(Long creatorId) {
        return assetInventoryRepository.findByCreatorId(creatorId);
    }

    @Override
    @Transactional
    public AssetInventory updatePlan(Long id, AssetInventory plan) {
        AssetInventory existingPlan = assetInventoryRepository.findById(id).orElse(null);
        if (existingPlan == null) {
            return null;
        }
        existingPlan.setPlanName(plan.getPlanName());
        existingPlan.setInventoryScope(plan.getInventoryScope());
        existingPlan.setExpectedCompletionTime(plan.getExpectedCompletionTime());
        existingPlan.setRemark(plan.getRemark());
        return assetInventoryRepository.save(existingPlan);
    }

    @Override
    @Transactional
    public AssetInventory updatePlanStatus(Long id, String status) {
        AssetInventory existingPlan = assetInventoryRepository.findById(id).orElse(null);
        if (existingPlan == null) {
            return null;
        }
        existingPlan.setStatus(status);
        if (InventoryStatus.COMPLETED.getCode().equals(status)) {
            existingPlan.setActualCompletionTime(LocalDateTime.now());
        }
        return assetInventoryRepository.save(existingPlan);
    }

    @Override
    @Transactional
    public boolean deletePlan(Long id) {
        Optional<AssetInventory> planOpt = assetInventoryRepository.findById(id);
        if (planOpt.isPresent()) {
            AssetInventory plan = planOpt.get();

            String username = getCurrentUsername();
            Long userId = getCurrentUserId();

            String planName = plan.getInventoryName() != null ? plan.getInventoryName() : plan.getPlanName();
            saveInventoryLog(userId, username, "删除盘点计划",
                    "删除了盘点计划: " + planName, LogType.INVENTORY.getCode());

            assetInventoryDetailRepository.deleteByInventoryId(id);
            assetInventoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public AssetInventory assignTask(Long id, Long assigneeId, String assigneeName, String inventoryArea) {
        AssetInventory plan = assetInventoryRepository.findById(id).orElse(null);
        if (plan == null) {
            return null;
        }

        if (!canAssignTask(plan, assigneeId)) {
            throw new RuntimeException("您没有分配该盘点任务的权限");
        }

        plan.setAssigneeId(assigneeId);
        plan.setAssigneeName(assigneeName);
        plan.setInventoryArea(inventoryArea);
        plan.setStatus(InventoryStatus.IN_PROGRESS.getCode());
        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        String currentUsername = getCurrentUsername();
        Long currentUserId = getCurrentUserId();

        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();

        saveInventoryLog(currentUserId, currentUsername, "分配盘点任务",
                "将盘点计划'" + planName + "'分配给: " + assigneeName, LogType.INVENTORY.getCode());

        saveInventoryLog(assigneeId, assigneeName, "收到盘点任务",
                currentUsername + " 分配给您盘点任务: " + planName, LogType.INVENTORY.getCode());

        return savedPlan;
    }

    private void saveInventoryLog(Long userId, String username, String operation, String params, String logType) {
        try {
            SysLog log = new SysLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setOperation(operation);
            log.setParams(params);
            log.setLogType(logType);
            log.setStatus("success");
            sysLogRepository.save(log);
        } catch (Exception e) {
        }
    }

    private boolean canAssignTask(AssetInventory plan, Long assigneeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return false;
        }

        return false;
    }

    @Override
    @Transactional
    public AssetInventory startTask(Long id) {
        AssetInventory plan = assetInventoryRepository.findById(id).orElse(null);
        if (plan == null) {
            return null;
        }

        if (!hasInventoryPermission(id)) {
            throw new RuntimeException("您没有执行该盘点任务的权限");
        }

        plan.setStartTime(LocalDateTime.now());
        plan.setStatus(InventoryStatus.IN_PROGRESS.getCode());
        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();
        saveInventoryLog(savedPlan.getAssigneeId(), savedPlan.getAssigneeName(), "开始盘点",
                "开始执行盘点任务: " + planName, LogType.INVENTORY.getCode());

        return savedPlan;
    }

    @Override
    @Transactional
    public AssetInventory completeTask(Long id) {
        AssetInventory plan = assetInventoryRepository.findById(id).orElse(null);
        if (plan == null) {
            return null;
        }

        if (!hasInventoryPermission(id)) {
            throw new RuntimeException("您没有完成该盘点任务的权限");
        }

        plan.setCompleteTime(LocalDateTime.now());
        plan.setStatus(InventoryStatus.COMPLETED.getCode());
        plan.setActualCompletionTime(LocalDateTime.now());

        calculateInventoryResult(plan);

        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();
        saveInventoryLog(savedPlan.getAssigneeId(), savedPlan.getAssigneeName(), "完成盘点",
                "完成盘点任务: " + planName + " (盘盈: " + savedPlan.getSurplusCount() + ", 盘亏: " + savedPlan.getShortageCount()
                        + ")",
                LogType.INVENTORY.getCode());

        return savedPlan;
    }

    private void calculateInventoryResult(AssetInventory plan) {
        List<AssetInventoryDetail> details = assetInventoryDetailRepository.findByInventoryId(plan.getId());

        int totalCount = details.size();
        int actualCount = 0;
        int surplusCount = 0;
        int shortageCount = 0;
        for (AssetInventoryDetail detail : details) {
            int diff = detail.getActualQuantity() - detail.getSystemQuantity();
            detail.setDifferenceQuantity(diff);

            if (detail.getActualQuantity() > detail.getSystemQuantity()) {
                detail.setStatus(InventoryStatus.SURPLUS.getCode());
                surplusCount++;
                actualCount += detail.getActualQuantity();
            } else if (detail.getActualQuantity() < detail.getSystemQuantity()) {
                detail.setStatus(InventoryStatus.SHORTAGE.getCode());
                shortageCount++;
                actualCount += detail.getActualQuantity();
            } else if (detail.getActualQuantity() > 0) {
                detail.setStatus(InventoryStatus.NORMAL.getCode());
                actualCount += detail.getActualQuantity();
            } else {
                detail.setStatus(InventoryStatus.PENDING.getCode());
            }

            assetInventoryDetailRepository.save(detail);
        }

        plan.setTotalCount(totalCount);
        plan.setActualCount(actualCount);
        plan.setSurplusCount(surplusCount);
        plan.setShortageCount(shortageCount);
    }

    @Override
    @Transactional
    public AssetInventoryDetail addInventoryDetail(Long inventoryId, AssetInventoryDetail detail) {
        detail.setInventoryId(inventoryId);
        return assetInventoryDetailRepository.save(detail);
    }

    @Override
    public List<AssetInventoryDetail> getInventoryDetails(Long inventoryId) {
        List<AssetInventoryDetail> details = assetInventoryDetailRepository.findByInventoryId(inventoryId);
        for (AssetInventoryDetail detail : details) {
            if (detail.getAssetName() == null || detail.getAssetName().isEmpty()) {
                Asset asset = assetRepository.findById(detail.getAssetId()).orElse(null);
                if (asset != null) {
                    detail.setAssetName(asset.getAssetName());
                }
            }
        }
        return details;
    }

    @Override
    @Transactional
    public AssetInventoryDetail updateInventoryDetail(Long detailId, AssetInventoryDetail detail) {
        AssetInventoryDetail existingDetail = assetInventoryDetailRepository.findById(detailId).orElse(null);
        if (existingDetail == null) {
            return null;
        }

        if (!hasInventoryPermission(existingDetail.getInventoryId())) {
            return null;
        }

        existingDetail.setActualQuantity(detail.getActualQuantity());
        existingDetail.setRemark(detail.getRemark());
        existingDetail.setStatus(detail.getStatus());
        return assetInventoryDetailRepository.save(existingDetail);
    }

    private boolean hasInventoryPermission(Long inventoryId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return false;
        }

        AssetInventory plan = assetInventoryRepository.findById(inventoryId).orElse(null);
        if (plan == null) {
            return false;
        }

        return false;
    }

    @Override
    @Transactional
    public boolean deleteInventoryDetail(Long detailId) {
        if (assetInventoryDetailRepository.existsById(detailId)) {
            assetInventoryDetailRepository.deleteById(detailId);
            return true;
        }
        return false;
    }

    private boolean canCreatePlan() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return false;
        }

        return true;
    }

    private boolean isAdmin(String username) {
        if (username == null) {
            return false;
        }
        return false;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal != null ? principal.toString() : "system";
    }

    private String getUserDepartment(String username) {
        return null;
    }

    private Long getCurrentUserId() {
        return null;
    }
}