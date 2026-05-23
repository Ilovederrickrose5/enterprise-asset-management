package com.enterprise.asset.enterpriseassetmanagement.repository;

import com.enterprise.asset.enterpriseassetmanagement.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByCreatorId(Long creatorId);

    List<PurchaseOrder> findByDepartmentId(Long departmentId);

    Page<PurchaseOrder> findByDepartmentId(Long departmentId, Pageable pageable);

    List<PurchaseOrder> findByStatus(String status);

    Page<PurchaseOrder> findByStatus(String status, Pageable pageable);

    List<PurchaseOrder> findByPurchaseRequestId(Long purchaseRequestId);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByDepartmentIdAndStatus(Long departmentId, String status);

    Page<PurchaseOrder> findByDepartmentIdAndStatus(Long departmentId, String status, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = :status AND po.departmentId IN (SELECT u.deptId FROM User u WHERE u.id = :userId)")
    List<PurchaseOrder> findOrdersForLeader(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = :status AND (po.departmentId IS NULL OR po.departmentId IN (SELECT u.deptId FROM User u WHERE u.id = :userId))")
    List<PurchaseOrder> findOrdersForAdmin(@Param("userId") Long userId, @Param("status") String status);
}