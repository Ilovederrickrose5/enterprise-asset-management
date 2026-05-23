package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.DepreciationRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DoubleDecliningBalanceDepreciationCalculatorTest {
    
    private DoubleDecliningBalanceDepreciationCalculator calculator;
    private Asset testAsset;
    
    @BeforeEach
    void setUp() {
        calculator = new DoubleDecliningBalanceDepreciationCalculator();
        testAsset = new Asset();
        testAsset.setId(1L);
        testAsset.setAssetNo("TEST001");
        testAsset.setAssetName("测试资产");
        testAsset.setCategoryId(1L);
        testAsset.setDeptId(1L);
        testAsset.setOriginalValue(new BigDecimal("12000"));
        testAsset.setUsefulLife(60);
    }
    
    @Test
    void testCalculateDepreciation_NormalCase() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, startDate, endDate, BigDecimal.ZERO, 0
        );
        
        assertNotNull(record);
        assertEquals("DOUBLE_DECLINING_BALANCE", record.getDepreciationMethod());
        assertEquals(testAsset.getAssetNo(), record.getAssetNo());
        assertEquals(testAsset.getAssetName(), record.getAssetName());
        assertEquals(testAsset.getOriginalValue(), record.getOriginalValue());
        
        BigDecimal doubleRate = new BigDecimal("2").divide(new BigDecimal("60"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal annualDepreciation = testAsset.getOriginalValue().multiply(doubleRate).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal monthlyDepreciation = annualDepreciation.divide(new BigDecimal("12"), 2, java.math.RoundingMode.HALF_UP);
        
        assertEquals(0, monthlyDepreciation.compareTo(record.getDepreciationAmount()));
        assertTrue(record.getCurrentNetValue().compareTo(BigDecimal.ZERO) >= 0);
        assertEquals(1, record.getUsedMonths());
        assertEquals("COMPLETED", record.getStatus());
    }
    
    @Test
    void testCalculateDepreciation_WithAccumulatedDepreciation() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        
        BigDecimal accumulatedDepreciation = new BigDecimal("2000");
        Integer usedMonths = 12;
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, startDate, endDate, accumulatedDepreciation, usedMonths
        );
        
        assertNotNull(record);
        assertEquals(13, record.getUsedMonths());
        
        BigDecimal currentNetValue = testAsset.getOriginalValue().subtract(accumulatedDepreciation);
        BigDecimal doubleRate = new BigDecimal("2").divide(new BigDecimal("60"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal annualDepreciation = currentNetValue.multiply(doubleRate).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal monthlyDepreciation = annualDepreciation.divide(new BigDecimal("12"), 2, java.math.RoundingMode.HALF_UP);
        
        assertEquals(0, monthlyDepreciation.compareTo(record.getDepreciationAmount()));
    }
    
    @Test
    void testCalculateDepreciation_ZeroOriginalValue() {
        testAsset.setOriginalValue(BigDecimal.ZERO);
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, LocalDate.now(), LocalDate.now(), BigDecimal.ZERO, 0
        );
        
        assertNotNull(record);
        assertEquals(0, record.getDepreciationAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, record.getCurrentNetValue().compareTo(BigDecimal.ZERO));
    }
    
    @Test
    void testCalculateDepreciation_ZeroUsefulLife() {
        testAsset.setUsefulLife(0);
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, LocalDate.now(), LocalDate.now(), BigDecimal.ZERO, 0
        );
        
        assertNotNull(record);
        assertEquals(0, record.getDepreciationAmount().compareTo(BigDecimal.ZERO));
    }
    
    @Test
    void testCalculateDepreciation_NegativeNetValue() {
        BigDecimal accumulatedDepreciation = new BigDecimal("11500");
        Integer usedMonths = 55;
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, LocalDate.now(), LocalDate.now(), accumulatedDepreciation, usedMonths
        );
        
        assertNotNull(record);
        assertTrue(record.getCurrentNetValue().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(record.getAccumulatedDepreciation().compareTo(testAsset.getOriginalValue()) <= 0);
    }
    
    @Test
    void testCalculateNetValue() {
        BigDecimal originalValue = new BigDecimal("10000");
        BigDecimal accumulatedDepreciation = new BigDecimal("3000");
        
        BigDecimal netValue = calculator.calculateNetValue(originalValue, accumulatedDepreciation);
        
        assertEquals(0, netValue.compareTo(new BigDecimal("7000")));
    }
    
    @Test
    void testCalculateNetValue_NegativeResult() {
        BigDecimal originalValue = new BigDecimal("10000");
        BigDecimal accumulatedDepreciation = new BigDecimal("12000");
        
        BigDecimal netValue = calculator.calculateNetValue(originalValue, accumulatedDepreciation);
        
        assertEquals(0, netValue.compareTo(BigDecimal.ZERO));
    }
    
    @Test
    void testGetDepreciationMethod() {
        assertEquals("DOUBLE_DECLINING_BALANCE", calculator.getDepreciationMethod());
    }
}