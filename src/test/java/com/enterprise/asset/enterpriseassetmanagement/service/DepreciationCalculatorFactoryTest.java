package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.service.impl.DoubleDecliningBalanceDepreciationCalculator;
import com.enterprise.asset.enterpriseassetmanagement.service.impl.StraightLineDepreciationCalculator;
import com.enterprise.asset.enterpriseassetmanagement.service.impl.WorkUnitDepreciationCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepreciationCalculatorFactoryTest {
    
    private DepreciationCalculatorFactory factory;
    
    @BeforeEach
    void setUp() {
        factory = new DepreciationCalculatorFactory();
    }
    
    @Test
    void testGetCalculator_StraightLine() {
        DepreciationCalculator calculator = factory.getCalculator("STRAIGHT_LINE");
        
        assertNotNull(calculator);
        assertTrue(calculator instanceof StraightLineDepreciationCalculator);
        assertEquals("STRAIGHT_LINE", calculator.getDepreciationMethod());
    }
    
    @Test
    void testGetCalculator_WorkUnit() {
        DepreciationCalculator calculator = factory.getCalculator("WORK_UNIT");
        
        assertNotNull(calculator);
        assertTrue(calculator instanceof WorkUnitDepreciationCalculator);
        assertEquals("WORK_UNIT", calculator.getDepreciationMethod());
    }
    
    @Test
    void testGetCalculator_DoubleDecliningBalance() {
        DepreciationCalculator calculator = factory.getCalculator("DOUBLE_DECLINING_BALANCE");
        
        assertNotNull(calculator);
        assertTrue(calculator instanceof DoubleDecliningBalanceDepreciationCalculator);
        assertEquals("DOUBLE_DECLINING_BALANCE", calculator.getDepreciationMethod());
    }
    
    @Test
    void testGetCalculator_InvalidMethod() {
        DepreciationCalculator calculator = factory.getCalculator("INVALID_METHOD");
        
        assertNotNull(calculator);
        assertTrue(calculator instanceof StraightLineDepreciationCalculator);
        assertEquals("STRAIGHT_LINE", calculator.getDepreciationMethod());
    }
    
    @Test
    void testGetCalculator_NullMethod() {
        DepreciationCalculator calculator = factory.getCalculator(null);
        
        assertNotNull(calculator);
        assertTrue(calculator instanceof StraightLineDepreciationCalculator);
    }
    
    @Test
    void testIsValidMethod_StraightLine() {
        assertTrue(factory.isValidMethod("STRAIGHT_LINE"));
    }
    
    @Test
    void testIsValidMethod_WorkUnit() {
        assertTrue(factory.isValidMethod("WORK_UNIT"));
    }
    
    @Test
    void testIsValidMethod_DoubleDecliningBalance() {
        assertTrue(factory.isValidMethod("DOUBLE_DECLINING_BALANCE"));
    }
    
    @Test
    void testIsValidMethod_InvalidMethod() {
        assertFalse(factory.isValidMethod("INVALID_METHOD"));
    }
    
    @Test
    void testIsValidMethod_NullMethod() {
        assertFalse(factory.isValidMethod(null));
    }
}