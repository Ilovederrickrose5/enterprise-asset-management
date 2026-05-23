package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.entity.Supplier;
import com.enterprise.asset.enterpriseassetmanagement.service.SupplierService;
import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public ResponseEntity<Result<List<Supplier>>> getAllSuppliers() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(Result.success(suppliers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Supplier>> getSupplierById(@PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        return supplier != null ? ResponseEntity.ok(Result.success(supplier))
                : ResponseEntity.ok(Result.error(404, "供应商不存在"));
    }

    @PostMapping
    public ResponseEntity<Result<Supplier>> createSupplier(@RequestBody Supplier supplier) {
        Supplier createdSupplier = supplierService.createSupplier(supplier);
        return ResponseEntity.ok(Result.success(createdSupplier));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<Supplier>> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        Supplier updatedSupplier = supplierService.updateSupplier(id, supplier);
        return updatedSupplier != null ? ResponseEntity.ok(Result.success(updatedSupplier))
                : ResponseEntity.ok(Result.error(404, "供应商不存在"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(Result.success("删除成功"));
    }
}
