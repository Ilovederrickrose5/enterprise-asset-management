package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.dto.AssetStatusDistributionDTO;
import com.enterprise.asset.enterpriseassetmanagement.dto.DepartmentAssetStatsDTO;
import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class ReportServiceImplTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDepartmentAssetStats() {
        // 准备测试数据
        Department dept1 = new Department();
        dept1.setId(1L);
        dept1.setDeptName("技术部");

        Department dept2 = new Department();
        dept2.setId(2L);
        dept2.setDeptName("人事部");

        List<Department> departments = List.of(dept1, dept2);

        Asset asset1 = new Asset();
        asset1.setId(1L);
        asset1.setDeptId(1L);
        asset1.setStatus("using");
        asset1.setPurchasePrice(new java.math.BigDecimal("5000.0"));

        Asset asset2 = new Asset();
        asset2.setId(2L);
        asset2.setDeptId(1L);
        asset2.setStatus("maintenance");
        asset2.setPurchasePrice(new java.math.BigDecimal("3000.0"));

        Asset asset3 = new Asset();
        asset3.setId(3L);
        asset3.setDeptId(2L);
        asset3.setStatus("in_stock");
        asset3.setPurchasePrice(new java.math.BigDecimal("2000.0"));

        List<Asset> assets = List.of(asset1, asset2, asset3);

        // 模拟方法调用
        when(departmentRepository.findAll()).thenReturn(departments);
        when(assetRepository.findAll()).thenReturn(assets);

        // 执行测试
        List<DepartmentAssetStatsDTO> stats = reportService.getDepartmentAssetStats();

        // 验证结果
        assertNotNull(stats);
        assertEquals(2, stats.size());

        DepartmentAssetStatsDTO dept1Stats = stats.stream()
                .filter(s -> s.getDepartmentId().equals(1L))
                .findFirst()
                .orElse(null);

        assertNotNull(dept1Stats);
        assertEquals("技术部", dept1Stats.getDepartmentName());
        assertEquals(2, dept1Stats.getAssetCount());
        assertEquals(8000.0, dept1Stats.getTotalValue());
        assertEquals(4000.0, dept1Stats.getAverageValue());
        assertEquals(1, dept1Stats.getInUseCount());
        assertEquals(0, dept1Stats.getIdleCount());
        assertEquals(1, dept1Stats.getMaintenanceCount());
        assertEquals(0, dept1Stats.getScrappedCount());
    }

    @Test
    void getAssetStatusDistribution() {
        // 准备测试数据
        Asset asset1 = new Asset();
        asset1.setId(1L);
        asset1.setStatus("using");
        asset1.setPurchasePrice(new java.math.BigDecimal("5000.0"));

        Asset asset2 = new Asset();
        asset2.setId(2L);
        asset2.setStatus("maintenance");
        asset2.setPurchasePrice(new java.math.BigDecimal("3000.0"));

        Asset asset3 = new Asset();
        asset3.setId(3L);
        asset3.setStatus("using");
        asset3.setPurchasePrice(new java.math.BigDecimal("2000.0"));

        List<Asset> assets = List.of(asset1, asset2, asset3);

        // 模拟方法调用
        when(assetRepository.findAll()).thenReturn(assets);

        // 执行测试
        List<AssetStatusDistributionDTO> distribution = reportService.getAssetStatusDistribution();

        // 验证结果
        assertNotNull(distribution);
        assertEquals(2, distribution.size());

        AssetStatusDistributionDTO usingStatus = distribution.stream()
                .filter(d -> "using".equals(d.getStatus()))
                .findFirst()
                .orElse(null);

        assertNotNull(usingStatus);
        assertEquals("使用中", usingStatus.getStatusName());
        assertEquals(2, usingStatus.getCount());
        assertEquals(66.67, usingStatus.getPercentage(), 0.01);
        assertEquals(7000.0, usingStatus.getTotalValue());
    }

    @Test
    void getDepartmentAssetStatsByDepartment() {
        // 准备测试数据
        Department dept1 = new Department();
        dept1.setId(1L);
        dept1.setDeptName("技术部");

        Asset asset1 = new Asset();
        asset1.setId(1L);
        asset1.setDeptId(1L);
        asset1.setStatus("using");
        asset1.setPurchasePrice(new java.math.BigDecimal("5000.0"));

        Asset asset2 = new Asset();
        asset2.setId(2L);
        asset2.setDeptId(1L);
        asset2.setStatus("maintenance");
        asset2.setPurchasePrice(new java.math.BigDecimal("3000.0"));

        List<Asset> assets = List.of(asset1, asset2);

        // 模拟方法调用
        when(departmentRepository.findById(1L)).thenReturn(java.util.Optional.of(dept1));
        when(assetRepository.findAll()).thenReturn(assets);

        // 执行测试
        List<DepartmentAssetStatsDTO> stats = reportService.getDepartmentAssetStatsByDepartment(1L);

        // 验证结果
        assertNotNull(stats);
        assertEquals(1, stats.size());

        DepartmentAssetStatsDTO dept1Stats = stats.get(0);
        assertEquals("技术部", dept1Stats.getDepartmentName());
        assertEquals(2, dept1Stats.getAssetCount());
        assertEquals(8000.0, dept1Stats.getTotalValue());
        assertEquals(4000.0, dept1Stats.getAverageValue());
    }
}
