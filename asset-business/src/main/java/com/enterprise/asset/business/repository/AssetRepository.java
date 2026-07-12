package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 资产数据访问接口
 * 关联表: asset(资产表)
 * 主要操作: 资产的增删改查、统计查询
 */
public interface AssetRepository extends JpaRepository<Asset, Long> {

    /** 查询所有资产总数(包含所有状态) */
    @Query(value = "SELECT COUNT(*) FROM asset", nativeQuery = true)
    long countAllAssets();

    /** 根据部门ID查询资产总数 */
    @Query(value = "SELECT COUNT(*) FROM asset WHERE dept_id = ?1", nativeQuery = true)
    long countAllAssetsByDepartment(Long departmentId);

    /** 根据用户ID查询资产总数 */
    @Query(value = "SELECT COUNT(*) FROM asset WHERE user_id = ?1", nativeQuery = true)
    long countAllAssetsByUser(Long userId);

    /** 根据部门ID查询资产列表 */
    List<Asset> findByDeptId(Long deptId);

    /** 分页查询所有资产 */
    Page<Asset> findAll(Pageable pageable);

    /** 根据部门ID分页查询资产 */
    Page<Asset> findByDeptId(Long deptId, Pageable pageable);

    /** 根据用户ID分页查询资产 */
    Page<Asset> findByUserId(Long userId, Pageable pageable);

    /** 根据状态分页查询资产 */
    Page<Asset> findByStatus(String status, Pageable pageable);

    /** 根据部门ID和状态分页查询资产 */
    Page<Asset> findByDeptIdAndStatus(Long deptId, String status, Pageable pageable);
}