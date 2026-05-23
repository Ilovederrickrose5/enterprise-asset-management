package com.enterprise.asset.enterpriseassetmanagement.repository;

import com.enterprise.asset.enterpriseassetmanagement.entity.AssetInventoryDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetInventoryDetailRepository extends JpaRepository<AssetInventoryDetail, Long> {
    List<AssetInventoryDetail> findByInventoryId(Long inventoryId);

    List<AssetInventoryDetail> findByAssetId(Long assetId);

    List<AssetInventoryDetail> findByStatus(String status);

    void deleteByInventoryId(Long inventoryId);
}
