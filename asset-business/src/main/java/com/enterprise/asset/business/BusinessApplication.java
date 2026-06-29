package com.enterprise.asset.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 资产业务模块启动类
 * 包含资产管理、采购、折旧、审批、盘点、报表等业务功能
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
    "com.enterprise.asset.business.client",  // 本模块的Feign客户端
    "com.enterprise.asset.auth.client"       // auth模块的Feign客户端（用于调用User、Department等服务）
})
public class BusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }
}