package com.enterprise.asset.enterpriseassetmanagement.repository;

import com.enterprise.asset.enterpriseassetmanagement.entity.PurchaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    List<PurchaseRequest> findByApplicantId(Long applicantId);
    
    List<PurchaseRequest> findByDepartmentId(Long departmentId);
    
    List<PurchaseRequest> findByStatus(String status);
    
    List<PurchaseRequest> findByApplicantIdAndStatus(Long applicantId, String status);

    List<PurchaseRequest> findByDepartmentIdAndStatus(Long departmentId, String status);

    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.status = :status AND pr.departmentId IN (SELECT u.deptId FROM User u WHERE u.id = :userId)")
    List<PurchaseRequest> findPendingRequestsForLeader(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.status = :status AND (pr.departmentId IS NULL OR pr.departmentId IN (SELECT u.deptId FROM User u WHERE u.id = :userId))")
    List<PurchaseRequest> findPendingRequestsForAdmin(@Param("userId") Long userId, @Param("status") String status);
    
    // 分页查询
    Page<PurchaseRequest> findAll(Pageable pageable);
    
    Page<PurchaseRequest> findByApplicantId(Long applicantId, Pageable pageable);
    
    Page<PurchaseRequest> findByDepartmentId(Long departmentId, Pageable pageable);
    
    // 按状态分页查询
    @Query("SELECT pr FROM PurchaseRequest pr WHERE (:status IS NULL OR :status = '' OR pr.status = :status)")
    Page<PurchaseRequest> findByStatus(@Param("status") String status, Pageable pageable);
    
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.applicantId = :applicantId AND (:status IS NULL OR :status = '' OR pr.status = :status)")
    Page<PurchaseRequest> findByApplicantIdAndStatus(@Param("applicantId") Long applicantId, @Param("status") String status, Pageable pageable);
    
    // 根据审批人ID查询审批记录
    List<PurchaseRequest> findByApproverIdOrderByApprovalDateDesc(Long approverId);
    
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.departmentId = :departmentId AND (:status IS NULL OR :status = '' OR pr.status = :status)")
    Page<PurchaseRequest> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId, @Param("status") String status, Pageable pageable);
    
    // 按申请人ID查询并按更新时间倒序
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.applicantId = :applicantId ORDER BY pr.updateTime DESC")
    List<PurchaseRequest> findByApplicantIdOrderByUpdateTimeDesc(@Param("applicantId") Long applicantId);
}