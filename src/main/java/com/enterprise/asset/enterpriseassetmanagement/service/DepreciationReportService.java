package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.dto.AssetNetValueDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DepreciationReportDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DepreciationSummaryDTO;

import java.time.LocalDate;
import java.util.List;

public interface DepreciationReportService {
    
    List<DepreciationReportDTO> generateDepreciationDetailReport(LocalDate startDate, LocalDate endDate);
    
    List<DepreciationReportDTO> generateDepreciationDetailReportByCategory(Long categoryId, LocalDate startDate, LocalDate endDate);
    
    List<DepreciationReportDTO> generateDepreciationDetailReportByDepartment(Long departmentId, LocalDate startDate, LocalDate endDate);
    
    List<DepreciationSummaryDTO> generateDepreciationSummaryReport(LocalDate startDate, LocalDate endDate);
    
    List<DepreciationSummaryDTO> generateDepreciationSummaryReportByCategory(LocalDate startDate, LocalDate endDate);
    
    List<DepreciationSummaryDTO> generateDepreciationSummaryReportByDepartment(LocalDate startDate, LocalDate endDate);
    
    List<AssetNetValueDTO> generateAssetNetValueReport(LocalDate asOfDate);
    
    List<AssetNetValueDTO> generateAssetNetValueReportByCategory(Long categoryId, LocalDate asOfDate);
    
    List<AssetNetValueDTO> generateAssetNetValueReportByDepartment(Long departmentId, LocalDate asOfDate);
    
    byte[] exportToExcel(List<DepreciationReportDTO> data, String reportType) throws Exception;
    
    byte[] exportToPDF(List<DepreciationReportDTO> data, String reportType) throws Exception;
}