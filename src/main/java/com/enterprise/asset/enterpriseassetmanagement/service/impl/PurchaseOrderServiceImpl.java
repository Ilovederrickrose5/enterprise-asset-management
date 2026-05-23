package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.AssetCategory;
import com.enterprise.asset.enterpriseassetmanagement.entity.PurchaseOrder;
import com.enterprise.asset.enterpriseassetmanagement.entity.PurchaseRequest;
import com.enterprise.asset.enterpriseassetmanagement.entity.Supplier;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetCategoryRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.PurchaseOrderRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.PurchaseRequestRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.SupplierRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import com.enterprise.asset.enterpriseassetmanagement.service.PurchaseOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetCategoryRepository assetCategoryRepository;

    @Override
    public List<PurchaseOrder> getAllOrders() {
        return purchaseOrderRepository.findAll();
    }

    @Override
    public Page<PurchaseOrder> getOrdersByPage(int page, int size, String status) {
        // 获取当前登录用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未登录");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new SecurityException("用户不存在");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate"));
        Long departmentId = user.getDeptId();

        // 管理员可以看到所有订单
        boolean isAdmin = "admin".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole());

        if (isAdmin) {
            // 管理员：查看所有订单
            if (status != null && !status.isEmpty()) {
                return purchaseOrderRepository.findByStatus(status.toLowerCase(), pageable);
            }
            return purchaseOrderRepository.findAll(pageable);
        } else {
            // 普通用户：只能查看本部门订单
            if (departmentId != null) {
                if (status != null && !status.isEmpty()) {
                    return purchaseOrderRepository.findByDepartmentIdAndStatus(departmentId, status.toLowerCase(),
                            pageable);
                }
                return purchaseOrderRepository.findByDepartmentId(departmentId, pageable);
            } else {
                // 如果没有部门信息，返回空列表
                return Page.empty(pageable);
            }
        }
    }

    @Override
    public PurchaseOrder getOrderById(Long id) {
        return purchaseOrderRepository.findById(id).orElse(null);
    }

    @Override
    public List<PurchaseOrder> getOrdersByCreatorId(Long creatorId) {
        return purchaseOrderRepository.findByCreatorId(creatorId);
    }

    @Override
    public List<PurchaseOrder> getOrdersByDepartmentId(Long departmentId) {
        return purchaseOrderRepository.findByDepartmentId(departmentId);
    }

    @Override
    public List<PurchaseOrder> getOrdersByStatus(String status) {
        return purchaseOrderRepository.findByStatus(status);
    }

    @Override
    public List<PurchaseOrder> getOrdersByPurchaseRequestId(Long purchaseRequestId) {
        return purchaseOrderRepository.findByPurchaseRequestId(purchaseRequestId);
    }

    @Override
    public PurchaseOrder createOrder(PurchaseOrder order) {
        logger.info("========== 开始创建采购订单 ==========");

        // 1. 用户认证检查
        logger.info("[步骤1/6] 检查用户认证状态");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("用户未登录，无法创建订单");
            throw new SecurityException("用户未登录");
        }

        String username = authentication.getName();
        logger.info("当前登录用户: {}", username);

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            logger.error("用户不存在: {}", username);
            throw new SecurityException("用户不存在");
        }
        logger.info("用户信息: id={}, name={}, deptId={}, role={}",
                user.getId(), user.getRealName(), user.getDeptId(), user.getRole());

        // 2. 权限检查
        logger.info("[步骤2/6] 检查创建权限");
        boolean isManager = "manager".equals(user.getRole());
        boolean isAdmin = "admin".equals(user.getRole());
        logger.info("用户角色检查: isManager={}, isAdmin={}", isManager, isAdmin);

        if (!isManager && !isAdmin) {
            logger.error("权限不足: 只有部门资产管理员或管理员才能创建采购订单");
            throw new SecurityException("只有部门资产管理员或管理员才能创建采购订单");
        }

        // 3. 采购需求申请处理
        logger.info("[步骤3/6] 处理采购需求申请关联");
        if (order.getPurchaseRequestId() != null) {
            logger.info("关联采购需求申请ID: {}", order.getPurchaseRequestId());
            PurchaseRequest request = purchaseRequestRepository.findById(order.getPurchaseRequestId()).orElse(null);
            if (request == null) {
                logger.error("采购需求申请不存在: id={}", order.getPurchaseRequestId());
                throw new IllegalArgumentException("采购需求申请不存在");
            }
            logger.info("采购需求申请信息: id={}, itemName={}, status={}",
                    request.getId(), request.getItemName(), request.getStatus());

            if (!"approved".equals(request.getStatus())) {
                logger.error("采购需求申请状态未批准: currentStatus={}", request.getStatus());
                throw new SecurityException("只能基于已批准的采购需求创建订单");
            }

            order.setItemName(request.getItemName());
            order.setSpecification(request.getSpecification());
            order.setQuantity(request.getQuantity());
            order.setUnit(request.getUnit());
            order.setDepartmentId(request.getDepartmentId());
            order.setDepartmentName(request.getDepartmentName());
            logger.info("从采购需求申请获取数据: itemName={}, quantity={}, departmentId={}",
                    request.getItemName(), request.getQuantity(), request.getDepartmentId());

            // 创建订单后更新采购需求状态为"已下单"
            request.setStatus("ordered");
            purchaseRequestRepository.save(request);
            logger.info("采购需求申请状态已更新为: ordered");
        } else {
            logger.info("未关联采购需求申请，直接创建订单");
        }

        // 4. 供应商信息处理
        logger.info("[步骤4/6] 处理供应商信息");
        if (order.getSupplierId() != null) {
            logger.info("供应商ID: {}", order.getSupplierId());
            Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
            if (supplier != null) {
                // 只有当前端没有传入 supplierName 时，才从数据库查询
                if (order.getSupplierName() == null || order.getSupplierName().isEmpty()) {
                    order.setSupplierName(supplier.getSupplierName());
                }
                logger.info("供应商名称: {}", supplier.getSupplierName());
            } else {
                logger.warn("供应商不存在: id={}", order.getSupplierId());
            }
        } else {
            logger.warn("供应商ID为空");
        }

        // 5. 资产分类处理
        logger.info("[步骤5/6] 处理资产分类");
        if (order.getCategoryId() != null) {
            AssetCategory category = assetCategoryRepository.findById(order.getCategoryId()).orElse(null);
            if (category != null) {
                order.setCategoryName(category.getCategoryName());
                logger.info("资产分类: id={}, name={}", order.getCategoryId(), category.getCategoryName());
            } else {
                logger.warn("资产分类不存在: id={}", order.getCategoryId());
                order.setCategoryId(10L); // 默认使用"其他资产"
                order.setCategoryName("其他资产");
            }
        } else {
            logger.info("未指定资产分类，使用默认值: 其他资产(id=10)");
            order.setCategoryId(10L);
            order.setCategoryName("其他资产");
        }

        // 6. 计算总金额和保存订单
        logger.info("[步骤6/6] 计算总金额并保存订单");
        if (order.getUnitPrice() != null && order.getQuantity() != null) {
            order.setTotalAmount(order.getUnitPrice().multiply(new BigDecimal(order.getQuantity())));
            logger.info("计算总金额: unitPrice={} * quantity={} = totalAmount={}",
                    order.getUnitPrice(), order.getQuantity(), order.getTotalAmount());
        } else {
            logger.warn("单价或数量为空，无法计算总金额: unitPrice={}, quantity={}",
                    order.getUnitPrice(), order.getQuantity());
        }

        // 设置订单基本信息
        String orderNumber = generateOrderNumber();
        order.setOrderNumber(orderNumber);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("pending");
        order.setPaymentStatus("unpaid");
        order.setCreatorId(user.getId());
        order.setCreatorName(user.getRealName());

        logger.info("订单基本信息: orderNumber={}, status={}, creatorId={}, departmentId={}",
                orderNumber, "pending", user.getId(), order.getDepartmentId());

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        logger.info("采购订单创建成功: id={}, orderNumber={}", savedOrder.getId(), savedOrder.getOrderNumber());
        logger.info("========== 采购订单创建完成 ==========");

        return savedOrder;
    }

    @Override
    public PurchaseOrder updateOrder(Long id, PurchaseOrder order) {
        PurchaseOrder existingOrder = purchaseOrderRepository.findById(id).orElse(null);
        if (existingOrder == null) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未登录");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new SecurityException("用户不存在");
        }

        boolean isAdmin = "admin".equals(user.getRole());

        if (!isAdmin && !existingOrder.getCreatorId().equals(user.getId())) {
            throw new SecurityException("无权修改此采购订单");
        }

        if ("completed".equals(existingOrder.getStatus())) {
            throw new SecurityException("已完成的订单不能修改");
        }

        if (order.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
            if (supplier != null) {
                existingOrder.setSupplierId(order.getSupplierId());
                existingOrder.setSupplierName(supplier.getSupplierName());
            }
        }

        // 更新资产分类
        if (order.getCategoryId() != null) {
            AssetCategory category = assetCategoryRepository.findById(order.getCategoryId()).orElse(null);
            if (category != null) {
                existingOrder.setCategoryId(order.getCategoryId());
                existingOrder.setCategoryName(category.getCategoryName());
            }
        }

        existingOrder.setUnitPrice(order.getUnitPrice());
        existingOrder.setExpectedDeliveryDate(order.getExpectedDeliveryDate());
        existingOrder.setRemark(order.getRemark());

        if (order.getUnitPrice() != null && existingOrder.getQuantity() != null) {
            existingOrder.setTotalAmount(order.getUnitPrice().multiply(new BigDecimal(existingOrder.getQuantity())));
        }

        return purchaseOrderRepository.save(existingOrder);
    }

    @Override
    public boolean deleteOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id).orElse(null);
        if (order == null) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未登录");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new SecurityException("用户不存在");
        }

        boolean isAdmin = "admin".equals(user.getRole());

        if (!isAdmin && !order.getCreatorId().equals(user.getId())) {
            throw new SecurityException("无权删除此采购订单");
        }

        if (!"pending".equals(order.getStatus())) {
            throw new SecurityException("只能删除待处理的采购订单");
        }

        purchaseOrderRepository.deleteById(id);
        return true;
    }

    @Override
    public PurchaseOrder updateOrderStatus(Long id, String status) {
        PurchaseOrder order = purchaseOrderRepository.findById(id).orElse(null);
        if (order == null) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未登录");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new SecurityException("用户不存在");
        }

        boolean isManager = "manager".equals(user.getRole());

        boolean isAdmin = "admin".equals(user.getRole());

        if (!isManager && !isAdmin) {
            throw new SecurityException("只有部门资产管理员或管理员才能更新订单状态");
        }

        order.setStatus(status);
        return purchaseOrderRepository.save(order);
    }

    @Override
    public PurchaseOrder completeOrder(Long id) {
        logger.info("========== 开始完成采购订单（资产入库）==========");
        logger.info("订单ID: {}", id);

        // 1. 查询订单
        logger.info("[步骤1/5] 查询采购订单");
        PurchaseOrder order = purchaseOrderRepository.findById(id).orElse(null);
        if (order == null) {
            logger.error("采购订单不存在: id={}", id);
            return null;
        }
        logger.info("订单信息: id={}, orderNumber={}, itemName={}, quantity={}, status={}",
                order.getId(), order.getOrderNumber(), order.getItemName(), order.getQuantity(), order.getStatus());

        // 2. 用户认证检查
        logger.info("[步骤2/5] 检查用户认证状态");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("用户未登录，无法完成订单");
            throw new SecurityException("用户未登录");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            logger.error("用户不存在: {}", username);
            throw new SecurityException("用户不存在");
        }
        logger.info("当前用户: id={}, name={}, role={}, deptId={}",
                user.getId(), user.getRealName(), user.getRole(), user.getDeptId());

        // 3. 权限检查
        logger.info("[步骤3/5] 检查完成权限");
        boolean isManager = "manager".equals(user.getRole());
        boolean isAdmin = "admin".equals(user.getRole());
        logger.info("用户角色: isManager={}, isAdmin={}", isManager, isAdmin);

        if (!isManager && !isAdmin) {
            logger.error("权限不足: 只有部门资产管理员或管理员才能完成订单");
            throw new SecurityException("只有部门资产管理员或管理员才能完成订单");
        }

        // 部门资产管理员只能完成本部门的订单
        if (isManager && !isAdmin) {
            if (user.getDeptId() == null) {
                logger.error("部门资产管理员没有部门信息");
                throw new SecurityException("部门资产管理员没有部门信息");
            }
            if (order.getDepartmentId() == null || !order.getDepartmentId().equals(user.getDeptId())) {
                logger.error("部门资产管理员只能完成本部门的订单: userDeptId={}, orderDeptId={}",
                        user.getDeptId(), order.getDepartmentId());
                throw new SecurityException("部门资产管理员只能完成本部门的订单");
            }
        }

        // 4. 订单状态检查
        logger.info("[步骤4/5] 检查订单状态");
        if ("completed".equals(order.getStatus())) {
            logger.error("订单已完成，无法重复完成: orderId={}", id);
            throw new SecurityException("订单已完成");
        }

        // 5. 更新订单状态并创建资产
        logger.info("[步骤5/5] 更新订单状态并创建资产");
        order.setStatus("completed");
        order.setActualDeliveryDate(LocalDateTime.now());
        order.setPaymentStatus("paid");

        // 保存订单
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        logger.info("订单状态已更新: status=completed, paymentStatus=paid");

        // 自动创建资产（根据订单数量创建多个资产）
        createAssetsFromOrder(savedOrder, user);

        logger.info("========== 采购订单完成（资产入库）结束 ==========");
        return savedOrder;
    }

    /**
     * 根据采购订单自动创建资产
     */
    private void createAssetsFromOrder(PurchaseOrder order, User currentUser) {
        logger.info("-------- 开始根据订单创建资产 --------");
        logger.info("订单信息: orderId={}, orderNumber={}, itemName={}, quantity={}, categoryId={}",
                order.getId(), order.getOrderNumber(), order.getItemName(), order.getQuantity(), order.getCategoryId());

        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            logger.warn("订单数量无效: quantity={}", order.getQuantity());
            return;
        }

        // 获取分类名称
        String categoryName = "其他资产";
        if (order.getCategoryId() != null) {
            AssetCategory category = assetCategoryRepository.findById(order.getCategoryId()).orElse(null);
            if (category != null) {
                categoryName = category.getCategoryName();
            }
        }

        for (int i = 0; i < order.getQuantity(); i++) {
            logger.info("创建资产 {}/{}", i + 1, order.getQuantity());

            Asset asset = new Asset();

            // 生成资产编号
            String assetNo = generateAssetNo();
            asset.setAssetNo(assetNo);
            logger.info("  资产编号: {}", assetNo);

            // 资产名称 = 物品名称 + 规格型号（如果有）
            String assetName = order.getItemName();
            if (order.getSpecification() != null && !order.getSpecification().isEmpty()) {
                assetName += " " + order.getSpecification();
            }
            asset.setAssetName(assetName);
            logger.info("  资产名称: {}", assetName);

            // 设置类别
            Long categoryId = order.getCategoryId() != null ? order.getCategoryId() : 10L;
            asset.setCategoryId(categoryId);
            logger.info("  资产分类: id={}, name={}", categoryId, categoryName);

            // 设置型号
            asset.setModel(order.getSpecification());
            logger.info("  规格型号: {}", order.getSpecification());

            // 设置单位
            asset.setUnit(order.getUnit());
            logger.info("  单位: {}", order.getUnit());

            // 设置采购价格
            asset.setPurchasePrice(order.getUnitPrice());
            asset.setOriginalValue(order.getUnitPrice());
            asset.setNetValue(order.getUnitPrice());
            logger.info("  采购价格: {}", order.getUnitPrice());

            // 设置供应商
            asset.setSupplierId(order.getSupplierId());
            logger.info("  供应商ID: {}", order.getSupplierId());

            // 设置采购日期（使用订单日期）
            if (order.getOrderDate() != null) {
                asset.setPurchaseDate(LocalDate.from(order.getOrderDate()));
                logger.info("  采购日期: {}", order.getOrderDate());
            }

            // 设置使用年限（默认5年=60个月）
            asset.setUsefulLife(60);
            logger.info("  使用年限: 60个月");

            // 设置部门
            asset.setDeptId(order.getDepartmentId());
            logger.info("  部门ID: {}", order.getDepartmentId());

            // 设置状态为"在库"
            asset.setStatus("in_stock");
            asset.setUseStatus("idle");
            logger.info("  状态: in_stock, 使用状态: idle");

            // 设置创建人信息
            if (currentUser != null) {
                asset.setCustodianId(currentUser.getId());
                logger.info("  保管人ID: {}", currentUser.getId());
            }

            // 保存资产
            Asset savedAsset = assetRepository.save(asset);
            logger.info("  资产创建成功: assetId={}, assetNo={}", savedAsset.getId(), savedAsset.getAssetNo());
        }

        logger.info("-------- 根据订单创建资产完成，共创建 {} 个资产 --------", order.getQuantity());
    }

    /**
     * 生成资产编号
     */
    private String generateAssetNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String random = String.format("%03d", (int) (Math.random() * 1000));
        return "AST" + timestamp + random;
    }

    private String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "PO" + LocalDateTime.now().format(formatter);
    }
}