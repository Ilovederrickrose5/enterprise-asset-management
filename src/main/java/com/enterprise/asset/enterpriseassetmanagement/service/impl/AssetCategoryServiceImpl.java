package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.entity.AssetCategory;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetCategoryRepository;
import com.enterprise.asset.enterpriseassetmanagement.service.AssetCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetCategoryServiceImpl implements AssetCategoryService {

    @Autowired
    private AssetCategoryRepository assetCategoryRepository;

    @Override
    public List<AssetCategory> getAllCategories() {
        return assetCategoryRepository.findAll();
    }

    @Override
    public List<AssetCategory> getActiveCategories() {
        return assetCategoryRepository.findByStatus(1);
    }

    @Override
    public AssetCategory getCategoryById(Long id) {
        Optional<AssetCategory> optional = assetCategoryRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public AssetCategory getCategoryByCode(String categoryCode) {
        return assetCategoryRepository.findByCategoryCode(categoryCode).orElse(null);
    }

    @Override
    public AssetCategory createCategory(AssetCategory category) {
        // 生成分类编码
        String generatedCode = generateCategoryCode(category);
        category.setCategoryCode(generatedCode);
        return assetCategoryRepository.save(category);
    }

    @Override
    public AssetCategory updateCategory(Long id, AssetCategory category) {
        Optional<AssetCategory> optional = assetCategoryRepository.findById(id);
        if (optional.isPresent()) {
            AssetCategory existingCategory = optional.get();
            existingCategory.setCategoryName(category.getCategoryName());
            existingCategory.setParentId(category.getParentId());
            existingCategory.setDescription(category.getDescription());
            existingCategory.setStatus(category.getStatus());
            existingCategory.setUsefulLife(category.getUsefulLife());
            existingCategory.setDepreciationMethod(category.getDepreciationMethod());
            return assetCategoryRepository.save(existingCategory);
        }
        return null;
    }

    @Override
    public boolean deleteCategory(Long id) {
        if (assetCategoryRepository.existsById(id)) {
            assetCategoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public long getActiveCategoryCount() {
        return assetCategoryRepository.countByStatus(1);
    }

    @Override
    public String generateCategoryCode(Long parentId) {
        // 顶级分类
        if (parentId == null || parentId == 0) {
            // 生成顶级分类编码：3位字母前缀
            List<AssetCategory> topCategories = assetCategoryRepository.findByParentId(0L);
            String[] prefixes = { "EQU", "OFF", "TRA", "BUI", "MCH", "FUR", "SPT", "MED", "SEC", "OTH" };

            for (String prefix : prefixes) {
                if (!assetCategoryRepository.existsByCategoryCode(prefix)) {
                    return prefix;
                }
            }

            // 如果所有前缀都已使用，生成新的前缀
            return "CAT" + String.format("%02d", topCategories.size() + 1);
        }

        // 子分类
        AssetCategory parentCategory = assetCategoryRepository.findById(parentId).orElse(null);
        if (parentCategory == null) {
            throw new IllegalArgumentException("父分类不存在");
        }

        String parentCode = parentCategory.getCategoryCode();
        List<AssetCategory> childCategories = assetCategoryRepository.findByParentId(parentId);

        // 提取最大编号
        int maxCode = 0;

        for (AssetCategory child : childCategories) {
            String childCode = child.getCategoryCode();
            if (childCode.startsWith(parentCode + "-")) {
                // 提取最后一部分数字
                String[] parts = childCode.split("-");
                if (parts.length > 0) {
                    String lastPart = parts[parts.length - 1];
                    try {
                        int code = Integer.parseInt(lastPart);
                        if (code > maxCode) {
                            maxCode = code;
                        }
                    } catch (NumberFormatException e) {
                        // 忽略非数字结尾的编码
                    }
                }
            }
        }

        // 生成新编码
        return parentCode + "-" + String.format("%02d", maxCode + 1);
    }

    // 生成分类编码
    private String generateCategoryCode(AssetCategory category) {
        return generateCategoryCode(category.getParentId());
    }
}
