package com.enterprise.asset.enterpriseassetmanagement.repository;

import com.enterprise.asset.enterpriseassetmanagement.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

    List<AssetCategory> findByStatus(int status);

    long countByStatus(int status);

    Optional<AssetCategory> findByCategoryCode(String categoryCode);

    boolean existsByCategoryCode(String categoryCode);

    List<AssetCategory> findByParentId(Long parentId);
}
