package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.dto.AssetStatusDistributionDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DepartmentAssetStatsDTO;
import java.util.List;

public interface ReportService {
    List<DepartmentAssetStatsDTO> getDepartmentAssetStats();
    List<AssetStatusDistributionDTO> getAssetStatusDistribution();
    List<DepartmentAssetStatsDTO> getDepartmentAssetStatsByDepartment(Long departmentId);
    List<AssetStatusDistributionDTO> getAssetStatusDistributionByDepartment(Long departmentId);
}
