package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.AssetCategory;

import java.util.List;

public interface AssetCategoryService {
    
    List<AssetCategory> getAllCategories();
    
    List<AssetCategory> getActiveCategories();
    
    AssetCategory getCategoryById(Long id);
    
    AssetCategory getCategoryByCode(String categoryCode);
    
    AssetCategory createCategory(AssetCategory category);
    
    AssetCategory updateCategory(Long id, AssetCategory category);
    
    boolean deleteCategory(Long id);
    
    long getActiveCategoryCount();
    
    String generateCategoryCode(Long parentId);
}
