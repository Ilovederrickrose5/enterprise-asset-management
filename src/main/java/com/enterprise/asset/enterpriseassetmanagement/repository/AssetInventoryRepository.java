package com.enterprise.asset.enterpriseassetmanagement.repository;

import com.enterprise.asset.enterpriseassetmanagement.entity.AssetInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetInventoryRepository extends JpaRepository<AssetInventory, Long> {
    List<AssetInventory> findByStatus(String status);

    List<AssetInventory> findByCreatorId(Long creatorId);

    List<AssetInventory> findByInventoryNoStartingWith(String prefix);

    List<AssetInventory> findByInventoryScope(String inventoryScope);

    // 查询我创建的或分配给我的任务
    List<AssetInventory> findByCreatorIdOrAssigneeId(Long creatorId, Long assigneeId);

    // 查询我创建的、分配给我的、或我部门的任务
    List<AssetInventory> findByCreatorIdOrAssigneeIdOrInventoryScope(Long creatorId, Long assigneeId,
            String inventoryScope);

    // 查询我创建的、分配给我的、或本部门用户创建且盘点范围是本部门的任务
    @Query("SELECT ai FROM AssetInventory ai WHERE " +
           "ai.creatorId = :userId OR " +
           "ai.assigneeId = :userId OR " +
           "(ai.inventoryScope = :deptName AND ai.deptId = :deptId)")
    List<AssetInventory> findMyTasksOrDeptTasks(@Param("userId") Long userId,
                                                 @Param("deptName") String deptName,
                                                 @Param("deptId") Long deptId);
    
    // 按被分配人ID查询并按创建时间倒序
    @Query("SELECT ai FROM AssetInventory ai WHERE ai.assigneeId = :assigneeId ORDER BY ai.createTime DESC")
    List<AssetInventory> findByAssigneeIdOrderByCreateTimeDesc(@Param("assigneeId") Long assigneeId);
}