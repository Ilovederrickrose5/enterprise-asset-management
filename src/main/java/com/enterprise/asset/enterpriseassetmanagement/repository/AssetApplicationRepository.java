package com.enterprise.asset.enterpriseassetmanagement.repository;

import com.enterprise.asset.enterpriseassetmanagement.entity.AssetApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetApplicationRepository extends JpaRepository<AssetApplication, Long> {

    List<AssetApplication> findByApplicantId(Long applicantId);

    List<AssetApplication> findByStatus(String status);

    List<AssetApplication> findByStatusOrderByApplicationDateDesc(String status);

    long countByStatus(String status);

    List<AssetApplication> findByDepartmentId(Long departmentId);

    List<AssetApplication> findByStatusAndDepartmentId(String status, Long departmentId);
    
    // 分页查询所有申请
    Page<AssetApplication> findAll(Pageable pageable);
    
    // 按申请人ID分页查询
    Page<AssetApplication> findByApplicantId(Long applicantId, Pageable pageable);
    
    // 按部门ID分页查询
    Page<AssetApplication> findByDepartmentId(Long departmentId, Pageable pageable);
    
    // 按类型和状态分页查询所有申请
    @Query("SELECT a FROM AssetApplication a WHERE " +
           "(:type IS NULL OR :type = '' OR a.applicationType = :type) AND " +
           "(:status IS NULL OR :status = '' OR a.status = :status)")
    Page<AssetApplication> findByTypeAndStatus(@Param("type") String type, @Param("status") String status, Pageable pageable);
    
    // 按申请人ID、类型和状态分页查询
    @Query("SELECT a FROM AssetApplication a WHERE a.applicantId = :applicantId AND " +
           "(:type IS NULL OR :type = '' OR a.applicationType = :type) AND " +
           "(:status IS NULL OR :status = '' OR a.status = :status)")
    Page<AssetApplication> findByApplicantIdAndTypeAndStatus(
            @Param("applicantId") Long applicantId, 
            @Param("type") String type, 
            @Param("status") String status, 
            Pageable pageable);
    
    // 按部门ID、类型和状态分页查询
    @Query("SELECT a FROM AssetApplication a WHERE a.departmentId = :departmentId AND " +
           "(:type IS NULL OR :type = '' OR a.applicationType = :type) AND " +
           "(:status IS NULL OR :status = '' OR a.status = :status)")
    Page<AssetApplication> findByDepartmentIdAndTypeAndStatus(
            @Param("departmentId") Long departmentId, 
            @Param("type") String type, 
            @Param("status") String status, 
            Pageable pageable);
    
    // 按申请人ID查询并按更新时间倒序
    @Query("SELECT a FROM AssetApplication a WHERE a.applicantId = :applicantId ORDER BY a.updateTime DESC")
    List<AssetApplication> findByApplicantIdOrderByUpdateTimeDesc(@Param("applicantId") Long applicantId);
}