package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.dto.AssetNetValueDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DepreciationReportDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DepreciationSummaryDTO;
import com.enterprise.asset.enterpriseassetmanagement.service.DepreciationReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/depreciation/reports")
public class DepreciationReportController {
    
    @Autowired
    private DepreciationReportService depreciationReportService;
    
    @GetMapping("/detail")
    public ResponseEntity<Result<List<DepreciationReportDTO>>> generateDepreciationDetailReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationReportDTO> report = depreciationReportService.generateDepreciationDetailReport(startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
    
    @GetMapping("/detail/category/{categoryId}")
    public ResponseEntity<Result<List<DepreciationReportDTO>>> generateDepreciationDetailReportByCategory(
            @PathVariable Long categoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationReportDTO> report = depreciationReportService.generateDepreciationDetailReportByCategory(categoryId, startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
    
    @GetMapping("/detail/department/{departmentId}")
    public ResponseEntity<Result<List<DepreciationReportDTO>>> generateDepreciationDetailReportByDepartment(
            @PathVariable Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationReportDTO> report = depreciationReportService.generateDepreciationDetailReportByDepartment(departmentId, startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
    
    @GetMapping("/summary")
    public ResponseEntity<Result<List<DepreciationSummaryDTO>>> generateDepreciationSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationSummaryDTO> report = depreciationReportService.generateDepreciationSummaryReport(startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
    
    @GetMapping("/summary/category")
    public ResponseEntity<Result<List<DepreciationSummaryDTO>>> generateDepreciationSummaryReportByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationSummaryDTO> report = depreciationReportService.generateDepreciationSummaryReportByCategory(startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
    
    @GetMapping("/summary/department")
    public ResponseEntity<Result<List<DepreciationSummaryDTO>>> generateDepreciationSummaryReportByDepartment(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationSummaryDTO> report = depreciationReportService.generateDepreciationSummaryReportByDepartment(startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
    
    @GetMapping("/net-value")
    public ResponseEntity<Result<List<AssetNetValueDTO>>> generateAssetNetValueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        try {
            List<AssetNetValueDTO> report = depreciationReportService.generateAssetNetValueReport(asOfDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
    
    @GetMapping("/net-value/category/{categoryId}")
    public ResponseEntity<Result<List<AssetNetValueDTO>>> generateAssetNetValueReportByCategory(
            @PathVariable Long categoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        try {
            List<AssetNetValueDTO> report = depreciationReportService.generateAssetNetValueReportByCategory(categoryId, asOfDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
    
    @GetMapping("/net-value/department/{departmentId}")
    public ResponseEntity<Result<List<AssetNetValueDTO>>> generateAssetNetValueReportByDepartment(
            @PathVariable Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        try {
            List<AssetNetValueDTO> report = depreciationReportService.generateAssetNetValueReportByDepartment(departmentId, asOfDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
    
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reportType) {
        try {
            List<DepreciationReportDTO> data = depreciationReportService.generateDepreciationDetailReport(startDate, endDate);
            byte[] excelBytes = depreciationReportService.exportToExcel(data, reportType);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", reportType + ".xlsx");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()).toString().getBytes());
        }
    }
    
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reportType) {
        try {
            List<DepreciationReportDTO> data = depreciationReportService.generateDepreciationDetailReport(startDate, endDate);
            byte[] pdfBytes = depreciationReportService.exportToPDF(data, reportType);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", reportType + ".pdf");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()).toString().getBytes());
        }
    }
}