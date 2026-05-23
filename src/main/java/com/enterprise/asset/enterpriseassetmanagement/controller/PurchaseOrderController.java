package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.PurchaseOrder;
import com.enterprise.asset.enterpriseassetmanagement.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<Result<Map<String, Object>>> getAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<PurchaseOrder> orderPage = purchaseOrderService.getOrdersByPage(page - 1, size, status);
        Map<String, Object> result = new HashMap<>();
        result.put("records", orderPage.getContent());
        result.put("total", orderPage.getTotalElements());
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<PurchaseOrder>> getOrderById(@PathVariable Long id) {
        PurchaseOrder order = purchaseOrderService.getOrderById(id);
        if (order == null) {
            return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
        }
        return ResponseEntity.ok(Result.success(order));
    }

    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<Result<List<PurchaseOrder>>> getOrdersByCreatorId(@PathVariable Long creatorId) {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByCreatorId(creatorId);
        return ResponseEntity.ok(Result.success(orders));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<List<PurchaseOrder>>> getOrdersByDepartmentId(@PathVariable Long departmentId) {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByDepartmentId(departmentId);
        return ResponseEntity.ok(Result.success(orders));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<PurchaseOrder>>> getOrdersByStatus(@PathVariable String status) {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByStatus(status);
        return ResponseEntity.ok(Result.success(orders));
    }

    @GetMapping("/request/{purchaseRequestId}")
    public ResponseEntity<Result<List<PurchaseOrder>>> getOrdersByPurchaseRequestId(@PathVariable Long purchaseRequestId) {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByPurchaseRequestId(purchaseRequestId);
        return ResponseEntity.ok(Result.success(orders));
    }

    @PostMapping
    public ResponseEntity<Result<PurchaseOrder>> createOrder(@RequestBody PurchaseOrder order) {
        try {
            PurchaseOrder createdOrder = purchaseOrderService.createOrder(order);
            return ResponseEntity.ok(Result.success(createdOrder));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "创建采购订单失败：" + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<PurchaseOrder>> updateOrder(
            @PathVariable Long id,
            @RequestBody PurchaseOrder order) {
        try {
            PurchaseOrder updatedOrder = purchaseOrderService.updateOrder(id, order);
            if (updatedOrder == null) {
                return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
            }
            return ResponseEntity.ok(Result.success(updatedOrder));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "更新采购订单失败：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteOrder(@PathVariable Long id) {
        try {
            boolean deleted = purchaseOrderService.deleteOrder(id);
            if (!deleted) {
                return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
            }
            return ResponseEntity.ok(Result.success("删除成功"));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "删除采购订单失败：" + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Result<PurchaseOrder>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest statusUpdate) {
        try {
            PurchaseOrder order = purchaseOrderService.updateOrderStatus(id, statusUpdate.getStatus());
            if (order == null) {
                return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
            }
            return ResponseEntity.ok(Result.success(order));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "更新订单状态失败：" + e.getMessage()));
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Result<PurchaseOrder>> completeOrder(@PathVariable Long id) {
        try {
            PurchaseOrder order = purchaseOrderService.completeOrder(id);
            if (order == null) {
                return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
            }
            return ResponseEntity.ok(Result.success(order));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "完成订单失败：" + e.getMessage()));
        }
    }

    static class StatusUpdateRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}