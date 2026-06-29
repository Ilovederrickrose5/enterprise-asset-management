package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.common.InventoryStatus;
import com.enterprise.asset.enterpriseassetmanagement.common.LogType;
import com.enterprise.asset.enterpriseassetmanagement.common.UserRole;
import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.AssetInventory;
import com.enterprise.asset.enterpriseassetmanagement.entity.AssetInventoryDetail;
import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import com.enterprise.asset.enterpriseassetmanagement.entity.SysLog;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetInventoryDetailRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetInventoryRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.DepartmentRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.SysLogRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import com.enterprise.asset.enterpriseassetmanagement.service.AssetInventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 资产盘点服务实现 - 处理盘点计划管理与执行（含权限校验、任务分配、结果统计） */
@Service
public class AssetInventoryServiceImpl implements AssetInventoryService {

    private final AssetInventoryRepository assetInventoryRepository;
    private final AssetInventoryDetailRepository assetInventoryDetailRepository;
    private final AssetRepository assetRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final SysLogRepository sysLogRepository;

    public AssetInventoryServiceImpl(AssetInventoryRepository assetInventoryRepository,
            AssetInventoryDetailRepository assetInventoryDetailRepository, AssetRepository assetRepository,
            DepartmentRepository departmentRepository, UserRepository userRepository,
            SysLogRepository sysLogRepository) {
        this.assetInventoryRepository = assetInventoryRepository;
        this.assetInventoryDetailRepository = assetInventoryDetailRepository;
        this.assetRepository = assetRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.sysLogRepository = sysLogRepository;
    }

    @Override
    @Transactional
    public AssetInventory createPlan(AssetInventory plan) {
        // 验证创建权限
        if (!canCreatePlan()) {
            throw new RuntimeException("您没有创建盘点计划的权限");
        }

        // 验证盘点范围
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();

            // 非管理员只能创建本部门的盘点计划
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

        // 自动生成盘点编号，格式：INV + 年份(4位) + 月份(2位) + 序号(4位)
        String inventoryNo = generateInventoryNo();
        plan.setInventoryNo(inventoryNo);

        // 如果planName为空，则使用inventoryName的值
        if (plan.getPlanName() == null || plan.getPlanName().isEmpty()) {
            plan.setPlanName(plan.getInventoryName());
        }

        // 设置部门ID（从inventory_scope获取）
        if (plan.getDeptId() == null && plan.getInventoryScope() != null && !"全部资产".equals(plan.getInventoryScope())) {
            Department dept = departmentRepository.findByDeptName(plan.getInventoryScope()).orElse(null);
            if (dept != null) {
                plan.setDeptId(dept.getId());
            }
        }

        plan.setStatus(InventoryStatus.PENDING.getCode());
        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        // 自动生成盘点明细
        generateInventoryDetails(savedPlan);

        // 记录操作日志（使用保存后的计划名称，确保准确性）
        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();
        saveInventoryLog(savedPlan.getCreatorId(), savedPlan.getCreatorName(), "创建盘点计划",
                "创建了盘点计划: " + planName, LogType.INVENTORY.getCode());

        return savedPlan;
    }

    /**
     * 根据盘点范围自动生成盘点明细
     */
    private void generateInventoryDetails(AssetInventory plan) {
        List<Asset> assets = new ArrayList<>();
        String scope = plan.getInventoryScope();

        if ("全部资产".equals(scope)) {
            assets = assetRepository.findAll();
        } else {
            // 根据部门名称查找部门，再查找该部门的资产
            Department dept = departmentRepository.findByDeptName(scope).orElse(null);
            if (dept != null) {
                assets = assetRepository.findByDeptId(dept.getId());
            }
        }

        // 生成盘点明细
        for (Asset asset : assets) {
            AssetInventoryDetail detail = new AssetInventoryDetail();
            detail.setInventoryId(plan.getId());
            detail.setAssetId(asset.getId());
            detail.setAssetNo(asset.getAssetNo());
            detail.setAssetName(asset.getAssetName());
            detail.setSystemQuantity(1); // 默认系统数量为1
            detail.setActualQuantity(0); // 初始实际数量为0
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

    /**
     * 生成盘点编号，格式：INV + 年份(4位) + 月份(2位) + 序号(5位)，共14位
     */
    private String generateInventoryNo() {
        LocalDateTime now = LocalDateTime.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String prefix = "INV" + year + month;

        // 查询本月最大编号
        List<AssetInventory> thisMonthPlans = assetInventoryRepository
                .findByInventoryNoStartingWith(prefix);
        int maxSeq = 0;
        for (AssetInventory plan : thisMonthPlans) {
            String no = plan.getInventoryNo();
            if (no != null && no.length() >= 13 && no.startsWith(prefix)) {
                // 处理13位或更长的编号，截取最后5位作为序号
                String seqStr = no.substring(no.length() - 5);
                try {
                    int seq = Integer.parseInt(seqStr);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    // 忽略格式错误的编号
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
        // 查询用户信息获取部门名称
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getDeptId() != null && user.getDeptId() > 0) {
            // 获取部门名称
            Department dept = departmentRepository.findById(user.getDeptId()).orElse(null);
            if (dept != null && dept.getDeptName() != null) {
                // 获取用户角色
                UserRole role = UserRole.fromCode(user.getRole());
                // 只有管理员、部门领导、资产管理员可以查看本部门的任务
                if (role != null && (role.isAdmin() || role.isLeader() || role.isManager())) {
                    // 查询我创建的、分配给我的、或本部门用户创建且盘点范围是本部门的任务
                    // 这样确保只能看到本部门用户创建的、盘点范围也是本部门的任务
                    return assetInventoryRepository.findMyTasksOrDeptTasks(userId, dept.getDeptName(), dept.getId());
                }
            }
        }
        // 普通用户只能看到自己创建或被分配的任务
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

            // 获取当前用户信息用于日志记录
            String username = getCurrentUsername();
            Long userId = getCurrentUserId();

            // 记录操作日志
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

        // 验证分配权限（包括分配者权限和被分配人权限范围）
        if (!canAssignTask(plan, assigneeId)) {
            throw new RuntimeException("您没有分配该盘点任务的权限");
        }

        plan.setAssigneeId(assigneeId);
        plan.setAssigneeName(assigneeName);
        plan.setInventoryArea(inventoryArea);
        plan.setStatus(InventoryStatus.IN_PROGRESS.getCode());
        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        // 获取当前操作人信息
        String currentUsername = getCurrentUsername();
        Long currentUserId = getCurrentUserId();

        // 记录操作日志（使用inventoryName显示更准确的计划名称）
        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();

        // 记录分配人的日志
        saveInventoryLog(currentUserId, currentUsername, "分配盘点任务",
                "将盘点计划'" + planName + "'分配给: " + assigneeName, LogType.INVENTORY.getCode());

        // 记录被分配人的日志（让被分配人能看到"谁分配了任务给我"）
        saveInventoryLog(assigneeId, assigneeName, "收到盘点任务",
                currentUsername + " 分配给您盘点任务: " + planName, LogType.INVENTORY.getCode());

        return savedPlan;
    }

    /**
     * 保存盘点操作日志
     */
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
            // 日志保存失败不影响主流程
            // 可以添加日志记录失败的日志
        }
    }

    /**
     * 检查是否有分配任务的权限（包括分配者权限和被分配人权限范围）
     */
    private boolean canAssignTask(AssetInventory plan, Long assigneeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return false;
        }

        UserDetails userDetails = (UserDetails) principal;
        String username = userDetails.getUsername();

        // 获取当前用户ID（优先从数据库查询）
        Long currentUserId = null;
        User currentUser = null;

        // 尝试从数据库获取用户信息
        try {
            // 先尝试作为ID查询
            Long userId = Long.parseLong(username);
            currentUser = userRepository.findById(userId).orElse(null);
            if (currentUser != null) {
                currentUserId = currentUser.getId();
            }
        } catch (NumberFormatException e) {
            // 作为用户名查询
            currentUser = userRepository.findByUsername(username).orElse(null);
            if (currentUser != null) {
                currentUserId = currentUser.getId();
            }
        }

        if (currentUserId == null || currentUser == null) {
            return false;
        }

        // 获取当前用户角色（从数据库查询）
        UserRole currentUserRole = UserRole.fromCode(currentUser.getRole());

        // 获取创建者信息
        User creator = userRepository.findById(plan.getCreatorId()).orElse(null);
        UserRole creatorRole = creator != null ? UserRole.fromCode(creator.getRole()) : UserRole.USER;
        Long creatorDeptId = creator != null ? creator.getDeptId() : null;

        // 获取被分配人信息
        User assignee = userRepository.findById(assigneeId).orElse(null);
        UserRole assigneeRole = assignee != null ? UserRole.fromCode(assignee.getRole()) : UserRole.USER;
        Long assigneeDeptId = assignee != null ? assignee.getDeptId() : null;

        // 权限验证逻辑
        // 1. 系统管理员可以分配所有任务（不受限制）
        if (currentUserRole != null && currentUserRole.isAdmin()) {
            // 但被分配人必须在盘点范围内
            return isAssigneeInScope(plan, assigneeDeptId, assigneeRole);
        }

        // 2. 创建者可以分配自己创建的任务
        if (currentUserId.equals(plan.getCreatorId())) {
            // 根据创建者角色判断被分配人权限范围
            return checkAssigneePermission(creatorRole, creatorDeptId, assigneeRole, assigneeDeptId);
        }

        // 3. 部门领导可以分配本部门的任务（包括他人创建的）
        if (currentUserRole != null && currentUserRole.isLeader()) {
            Long userDeptId = getUserDeptId(username);
            if (userDeptId != null) {
                // 获取盘点计划所属部门ID（优先使用dept_id，否则从inventory_scope获取）
                Long planDeptId = plan.getDeptId();
                if (planDeptId == null && plan.getInventoryScope() != null) {
                    Department scopeDept = departmentRepository.findByDeptName(plan.getInventoryScope()).orElse(null);
                    if (scopeDept != null) {
                        planDeptId = scopeDept.getId();
                    }
                }

                if (userDeptId.equals(planDeptId)) {
                    // 部门领导分配时，被分配人必须在本部门内
                    return assigneeDeptId != null && assigneeDeptId.equals(userDeptId) &&
                            assigneeRole != null && (assigneeRole.isManager() || assigneeRole.isUser());
                }
            }
        }

        return false;
    }

    /**
     * 检查被分配人是否在盘点范围内
     */
    private boolean isAssigneeInScope(AssetInventory plan, Long assigneeDeptId, UserRole assigneeRole) {
        if (assigneeDeptId == null) {
            return false;
        }

        // 获取盘点范围对应的部门ID
        String scope = plan.getInventoryScope();
        Department scopeDept = departmentRepository.findByDeptName(scope).orElse(null);

        if (scopeDept == null) {
            return false;
        }

        // 被分配人必须在盘点范围内
        if (!scopeDept.getId().equals(assigneeDeptId)) {
            return false;
        }

        // 可以分配给资产管理员、部门领导和普通用户
        return assigneeRole != null && (assigneeRole.isManager() || assigneeRole.isLeader() || assigneeRole.isUser());
    }

    /**
     * 根据创建者角色检查被分配人权限
     */
    private boolean checkAssigneePermission(UserRole creatorRole, Long creatorDeptId,
            UserRole assigneeRole, Long assigneeDeptId) {
        if (assigneeDeptId == null || creatorDeptId == null || assigneeRole == null || creatorRole == null) {
            return false;
        }

        // 被分配人必须在创建者所在部门
        if (!creatorDeptId.equals(assigneeDeptId)) {
            return false;
        }

        // 根据创建者角色判断
        if (creatorRole.isAdmin()) {
            return assigneeRole.isManager() || assigneeRole.isLeader() || assigneeRole.isUser();
        } else if (creatorRole.isLeader()) {
            return assigneeRole.isManager() || assigneeRole.isUser();
        } else if (creatorRole.isManager()) {
            return assigneeRole.isUser();
        } else {
            return false;
        }
    }

    @Override
    @Transactional
    public AssetInventory startTask(Long id) {
        AssetInventory plan = assetInventoryRepository.findById(id).orElse(null);
        if (plan == null) {
            return null;
        }

        // 验证操作权限
        if (!hasInventoryPermission(id)) {
            throw new RuntimeException("您没有执行该盘点任务的权限");
        }

        plan.setStartTime(LocalDateTime.now());
        plan.setStatus(InventoryStatus.IN_PROGRESS.getCode());
        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        // 记录操作日志
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

        // 验证操作权限
        if (!hasInventoryPermission(id)) {
            throw new RuntimeException("您没有完成该盘点任务的权限");
        }

        plan.setCompleteTime(LocalDateTime.now());
        plan.setStatus(InventoryStatus.COMPLETED.getCode());
        plan.setActualCompletionTime(LocalDateTime.now());

        // 自动统计盘点结果
        calculateInventoryResult(plan);

        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        // 记录操作日志
        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();
        saveInventoryLog(savedPlan.getAssigneeId(), savedPlan.getAssigneeName(), "完成盘点",
                "完成盘点任务: " + planName + " (盘盈: " + savedPlan.getSurplusCount() + ", 盘亏: " + savedPlan.getShortageCount()
                        + ")",
                LogType.INVENTORY.getCode());

        return savedPlan;
    }

    /**
     * 计算盘点结果统计
     */
    private void calculateInventoryResult(AssetInventory plan) {
        List<AssetInventoryDetail> details = assetInventoryDetailRepository.findByInventoryId(plan.getId());

        int totalCount = details.size();// 资产总数
        int actualCount = 0;// 实际盘点数
        int surplusCount = 0; // 盘盈（实际数量 > 系统数量）
        int shortageCount = 0; // 盘亏（实际数量 < 系统数量）
        // 遍历盘点明细，判断盘盈/盘亏
        for (AssetInventoryDetail detail : details) {
            // 计算差异：实际数量 - 系统数量
            int diff = detail.getActualQuantity() - detail.getSystemQuantity();
            detail.setDifferenceQuantity(diff);

            // 根据差异判断状态
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
        // 如果 assetName 为空，从资产表查询并填充
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

        // 检查权限：只有admin或负责人/被分配人可以修改
        if (!hasInventoryPermission(existingDetail.getInventoryId())) {
            return null;
        }

        existingDetail.setActualQuantity(detail.getActualQuantity());
        existingDetail.setRemark(detail.getRemark());
        existingDetail.setStatus(detail.getStatus());
        return assetInventoryDetailRepository.save(existingDetail);
    }

    /**
     * 检查是否有盘点权限
     */
    private boolean hasInventoryPermission(Long inventoryId) {
        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return false;
        }

        UserDetails userDetails = (UserDetails) principal;
        String username = userDetails.getUsername();

        // 通过用户名查询用户信息
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        // 管理员有所有权限（直接使用数据库role字段）
        UserRole role = UserRole.fromCode(user.getRole());
        if (role != null && role.isAdmin()) {
            return true;
        }

        // 获取盘点计划信息
        AssetInventory plan = assetInventoryRepository.findById(inventoryId).orElse(null);
        if (plan == null) {
            return false;
        }

        Long currentUserId = user.getId();

        // 负责人或被分配人有修改权限
        return currentUserId.equals(plan.getCreatorId()) || currentUserId.equals(plan.getAssigneeId());
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

    /**
     * 检查是否有创建盘点计划的权限
     */
    private boolean canCreatePlan() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return false;
        }

        UserDetails userDetails = (UserDetails) principal;
        String username = userDetails.getUsername();
        UserRole role = UserRole.fromCode(getUserRole(username));

        // admin、leader、manager 可以创建盘点计划
        return role != null && (role.isAdmin() || role.isLeader() || role.isManager());
    }

    /**
     * 判断是否是管理员（直接使用数据库role字段）
     */
    private boolean isAdmin(String username) {
        if (username == null) {
            return false;
        }

        // 尝试作为用户ID查询
        try {
            Long userId = Long.parseLong(username);
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && "admin".equals(user.getRole())) {
                return true;
            }
        } catch (NumberFormatException e) {
            // 作为用户名查询
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && "admin".equals(user.getRole())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取用户角色（支持用户名或用户ID，直接使用数据库role字段）
     */
    private String getUserRole(String identifier) {
        if (identifier == null) {
            return "user";
        }

        User user = null;

        // 尝试作为用户ID查询
        try {
            Long userId = Long.parseLong(identifier);
            user = userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            // 作为用户名查询
            user = userRepository.findByUsername(identifier).orElse(null);
        }

        // 直接返回数据库中的role字段
        if (user != null && user.getRole() != null) {
            return user.getRole();
        }

        return "user";
    }

    /**
     * 获取用户所在部门ID（支持用户名或用户ID）
     */
    private Long getUserDeptId(String identifier) {
        if (identifier == null) {
            return null;
        }

        User user = null;

        // 尝试作为用户ID查询
        try {
            Long userId = Long.parseLong(identifier);
            user = userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            // identifier 不是数字，尝试作为用户名查询
            user = userRepository.findByUsername(identifier).orElse(null);
        }

        if (user != null) {
            return user.getDeptId();
        }

        return null;
    }

    /**
     * 获取用户所在部门名称
     */
    private String getUserDepartment(String username) {
        // 先尝试作为用户ID查询
        try {
            Long userId = Long.parseLong(username);
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getDeptId() != null) {
                Department dept = departmentRepository.findById(user.getDeptId()).orElse(null);
                return dept != null ? dept.getDeptName() : null;
            }
        } catch (NumberFormatException e) {
            // 尝试作为用户名查询
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && user.getDeptId() != null) {
                Department dept = departmentRepository.findById(user.getDeptId()).orElse(null);
                return dept != null ? dept.getDeptName() : null;
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户名
     */
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

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        String username = getCurrentUsername();
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            User user = userRepository.findByUsername(username).orElse(null);
            return user != null ? user.getId() : null;
        }
    }
}