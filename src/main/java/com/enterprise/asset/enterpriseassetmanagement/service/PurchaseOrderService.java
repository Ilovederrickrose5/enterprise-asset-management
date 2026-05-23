package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.PurchaseOrder;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PurchaseOrderService {
    
    List<PurchaseOrder> getAllOrders();
    
    Page<PurchaseOrder> getOrdersByPage(int page, int size, String status);
    
    PurchaseOrder getOrderById(Long id);
    
    List<PurchaseOrder> getOrdersByCreatorId(Long creatorId);
    
    List<PurchaseOrder> getOrdersByDepartmentId(Long departmentId);
    
    List<PurchaseOrder> getOrdersByStatus(String status);
    
    List<PurchaseOrder> getOrdersByPurchaseRequestId(Long purchaseRequestId);
    
    PurchaseOrder createOrder(PurchaseOrder order);
    
    PurchaseOrder updateOrder(Long id, PurchaseOrder order);
    
    boolean deleteOrder(Long id);
    
    PurchaseOrder updateOrderStatus(Long id, String status);
    
    PurchaseOrder completeOrder(Long id);
}