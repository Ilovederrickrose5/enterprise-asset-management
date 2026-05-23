package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.AssetInventory;
import com.enterprise.asset.enterpriseassetmanagement.entity.AssetInventoryDetail;

import java.util.List;

public interface AssetInventoryService {
    AssetInventory createPlan(AssetInventory plan);

    AssetInventory getPlanById(Long id);

    List<AssetInventory> getAllPlans();

    List<AssetInventory> getPlansByStatus(String status);

    List<AssetInventory> getPlansByCreatorId(Long creatorId);

    List<AssetInventory> getPlansByCreatorOrAssignee(Long userId);

    List<AssetInventory> getPlansByUserAndDept(Long userId, String deptName);

    AssetInventory updatePlan(Long id, AssetInventory plan);

    AssetInventory updatePlanStatus(Long id, String status);

    boolean deletePlan(Long id);

    AssetInventory assignTask(Long id, Long assigneeId, String assigneeName, String inventoryArea);

    AssetInventory startTask(Long id);
    AssetInventory completeTask(Long id);

    AssetInventoryDetail addInventoryDetail(Long inventoryId, AssetInventoryDetail detail);

    List<AssetInventoryDetail> getInventoryDetails(Long inventoryId);

    AssetInventoryDetail updateInventoryDetail(Long detailId, AssetInventoryDetail detail);

    boolean deleteInventoryDetail(Long detailId);
}