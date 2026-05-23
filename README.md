# 企业固定资产管理系统

## 1. 项目概述

企业固定资产管理系统是一个基于 Spring Boot + Vue 3 的企业级资产全生命周期管理平台，实现了对固定资产的采购入库、日常管理、折旧计算、盘点管理、维修报废等全流程管理。

### 1.1 技术栈

**后端技术栈：**
- **框架**：Spring Boot 4.0.6
- **持久层**：Spring Data JPA
- **数据库**：MySQL 8.0
- **安全**：Spring Security + JWT
- **构建工具**：Maven
- **Java版本**：JDK 17

**前端技术栈：**
- **框架**：Vue 3
- **UI组件库**：Element Plus
- **状态管理**：Pinia
- **HTTP客户端**：Axios
- **图表**：ECharts
- **构建工具**：Vite

---

## 2. 系统功能模块

### 2.1 功能模块总览

```
├── 基础数据管理
│   ├── 部门管理
│   ├── 资产分类管理
│   ├── 员工信息管理
│   └── 供应商信息管理
│
├── 资产管理
│   ├── 资产登记
│   ├── 资产查询
│   ├── 资产编辑
│   └── 资产删除
│
├── 资产折旧
│   ├── 单资产折旧计算
│   ├── 批量折旧计算
│   ├── 折旧方法支持（直线法、双倍余额递减法、工作量法）
│   └── 折旧记录查询
│
├── 资产盘点
│   ├── 创建盘点计划
│   ├── 分配盘点任务
│   ├── 执行盘点
│   └── 盘点结果统计
│
├── 资产业务流程
│   ├── 资产领用申请
│   ├── 资产转移申请
│   ├── 资产维修申请
│   └── 资产报废申请（二级审批）
│
├── 采购管理
│   ├── 采购申请
│   ├── 采购审批
│   └── 采购订单
│
├── 报表统计
│   ├── 资产统计报表
│   ├── 部门资产分布
│   └── 资产状态分布
│
└── 系统管理
    ├── 角色管理
    ├── 用户管理
    └── 操作日志
```

### 2.2 角色权限体系

系统采用基于角色的访问控制（RBAC），包含四种角色：

| 角色 | 角色代码 | 权限说明 |
|------|---------|---------|
| 系统管理员 | admin | 拥有系统所有权限 |
| 部门领导 | leader | 查看统计报表、审批报废申请 |
| 部门资产管理员 | manager | 管理本部门资产 |
| 普通员工 | user | 发起各类资产申请 |

---

## 3. 数据库设计

### 3.1 数据库配置

```properties
# 数据库连接信息
spring.datasource.url=jdbc:mysql://localhost:3306/asset_management
spring.datasource.username=root
spring.datasource.password=123456
```

### 3.2 核心数据表

#### 3.2.1 资产表（asset）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| asset_no | VARCHAR | 资产编号（唯一） |
| asset_name | VARCHAR | 资产名称 |
| category_id | BIGINT | 资产分类ID |
| model | VARCHAR | 规格型号 |
| purchase_price | DECIMAL | 购入价格 |
| original_value | DECIMAL | 原值 |
| net_value | DECIMAL | 当前净值 |
| supplier_id | BIGINT | 供应商ID |
| purchase_date | DATE | 购入日期 |
| useful_life | INT | 使用年限（月） |
| depreciation_method | VARCHAR | 折旧方法 |
| status | VARCHAR | 资产状态 |
| use_status | VARCHAR | 使用状态 |
| dept_id | BIGINT | 所属部门ID |
| location | VARCHAR | 存放地点 |
| custodian_id | BIGINT | 保管人ID |

#### 3.2.2 资产分类表（asset_category）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| category_name | VARCHAR | 分类名称 |
| category_code | VARCHAR | 分类编码（唯一） |
| parent_id | BIGINT | 上级分类ID |
| description | VARCHAR | 分类描述 |
| useful_life | INT | 默认使用年限 |
| depreciation_method | VARCHAR | 默认折旧方法 |

#### 3.2.3 部门表（department）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| dept_name | VARCHAR | 部门名称 |
| dept_code | VARCHAR | 部门编码 |
| parent_id | BIGINT | 上级部门ID |
| leader | BIGINT | 部门负责人ID |
| description | VARCHAR | 部门描述 |

#### 3.2.4 用户表（user）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| username | VARCHAR | 用户名（唯一） |
| password | VARCHAR | 密码 |
| real_name | VARCHAR | 真实姓名 |
| employee_no | VARCHAR | 员工工号 |
| dept_id | BIGINT | 部门ID |
| position | VARCHAR | 职位 |
| role | VARCHAR | 角色代码 |
| status | INT | 账号状态 |

#### 3.2.5 供应商表（supplier）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| supplier_name | VARCHAR | 供应商名称 |
| contact_person | VARCHAR | 联系人 |
| phone | VARCHAR | 联系电话 |
| address | VARCHAR | 地址 |
| status | INT | 状态 |

#### 3.2.6 资产盘点表（asset_inventory）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| inventory_no | VARCHAR | 盘点编号 |
| inventory_name | VARCHAR | 盘点名称 |
| plan_name | VARCHAR | 计划名称 |
| inventory_scope | VARCHAR | 盘点范围 |
| inventory_area | VARCHAR | 盘点区域 |
| dept_id | BIGINT | 盘点部门ID |
| status | VARCHAR | 状态（pending/in_progress/completed/cancelled） |
| assignee_id | BIGINT | 被分配人ID |
| assignee_name | VARCHAR | 被分配人姓名 |
| creator_id | BIGINT | 创建人ID |
| total_count | INT | 资产总数 |
| actual_count | INT | 实际盘点数 |
| surplus_count | INT | 盘盈数量 |
| shortage_count | INT | 盘亏数量 |

#### 3.2.7 资产折旧记录表（asset_depreciation）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| asset_id | BIGINT | 资产ID |
| depreciation_method | VARCHAR | 折旧方法 |
| original_value | DECIMAL | 原值 |
| current_net_value | DECIMAL | 折旧前净值 |
| depreciation_amount | DECIMAL | 本期折旧额 |
| accumulated_depreciation | DECIMAL | 累计折旧 |
| net_value | DECIMAL | 折旧后净值 |
| depreciation_month | VARCHAR | 折旧月份 |
| useful_life | INT | 使用年限 |
| used_months | INT | 已使用月数 |

#### 3.2.8 资产业务申请表（asset_application）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| asset_id | BIGINT | 资产ID |
| application_type | VARCHAR | 申请类型（领用/转移/维修/报废） |
| applicant_id | BIGINT | 申请人ID |
| applicant_name | VARCHAR | 申请人姓名 |
| department_id | BIGINT | 部门ID |
| status | VARCHAR | 状态 |
| approver_id | BIGINT | 审批人ID |
| approver_name | VARCHAR | 审批人姓名 |
| approval_date | DATETIME | 审批时间 |
| approval_remark | TEXT | 审批备注 |

#### 3.2.9 采购申请表（purchase_request）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| item_name | VARCHAR | 物品名称 |
| specification | VARCHAR | 规格型号 |
| quantity | INT | 数量 |
| unit | VARCHAR | 单位 |
| estimated_unit_price | DECIMAL | 预估单价 |
| total_amount | DECIMAL | 总金额 |
| purchase_reason | TEXT | 采购原因 |
| department_id | BIGINT | 部门ID |
| applicant_id | BIGINT | 申请人ID |
| status | VARCHAR | 状态 |

#### 3.2.10 采购订单表（purchase_order）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| order_number | VARCHAR | 订单编号 |
| purchase_request_id | BIGINT | 关联采购申请ID |
| item_name | VARCHAR | 物品名称 |
| quantity | INT | 数量 |
| unit_price | DECIMAL | 单价 |
| total_amount | DECIMAL | 总金额 |
| supplier_id | BIGINT | 供应商ID |
| status | VARCHAR | 订单状态 |
| payment_status | VARCHAR | 付款状态 |

#### 3.2.11 系统日志表（sys_log）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| user_id | BIGINT | 用户ID |
| username | VARCHAR | 用户名 |
| operation | VARCHAR | 操作类型 |
| method | VARCHAR | 方法名 |
| params | VARCHAR | 参数 |
| ip | VARCHAR | IP地址 |
| status | VARCHAR | 状态 |
| log_type | VARCHAR | 日志类型 |

#### 3.2.12 角色表（role）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| name | VARCHAR | 角色名称 |
| code | VARCHAR | 角色代码（唯一） |
| description | VARCHAR | 角色描述 |

---

## 4. API接口文档

### 4.1 认证接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/logout` | POST | 用户登出 |

### 4.2 资产管理接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/assets` | GET | 获取资产列表 |
| `/api/assets/{id}` | GET | 获取资产详情 |
| `/api/assets` | POST | 创建资产 |
| `/api/assets/{id}` | PUT | 更新资产 |
| `/api/assets/{id}` | DELETE | 删除资产 |
| `/api/assets/stats` | GET | 获取资产统计 |
| `/api/assets/status-distribution` | GET | 资产状态分布 |

### 4.3 资产分类接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/asset-categories` | GET | 获取分类列表 |
| `/api/asset-categories/{id}` | GET | 获取分类详情 |
| `/api/asset-categories` | POST | 创建分类 |
| `/api/asset-categories/{id}` | PUT | 更新分类 |
| `/api/asset-categories/{id}` | DELETE | 删除分类 |

### 4.4 部门管理接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/departments` | GET | 获取部门列表 |
| `/api/departments/{id}` | GET | 获取部门详情 |
| `/api/departments` | POST | 创建部门 |
| `/api/departments/{id}` | PUT | 更新部门 |
| `/api/departments/{id}` | DELETE | 删除部门 |

### 4.5 员工管理接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/users` | GET | 获取员工列表 |
| `/api/users/{id}` | GET | 获取员工详情 |
| `/api/users` | POST | 创建员工 |
| `/api/users/{id}` | PUT | 更新员工 |
| `/api/users/{id}` | DELETE | 删除员工 |

### 4.6 供应商管理接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/suppliers` | GET | 获取供应商列表 |
| `/api/suppliers/{id}` | GET | 获取供应商详情 |
| `/api/suppliers` | POST | 创建供应商 |
| `/api/suppliers/{id}` | PUT | 更新供应商 |
| `/api/suppliers/{id}` | DELETE | 删除供应商 |

### 4.7 资产折旧接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/depreciation/calculate` | POST | 计算折旧 |
| `/api/depreciation/batch-calculate` | POST | 批量计算折旧 |
| `/api/depreciation/records` | GET | 获取折旧记录 |
| `/api/depreciation/summary` | GET | 获取折旧汇总 |
| `/api/depreciation/report` | GET | 获取折旧报表 |

### 4.8 资产盘点接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/asset-inventory/plans` | GET | 获取盘点计划列表 |
| `/api/asset-inventory/plans/{id}` | GET | 获取盘点计划详情 |
| `/api/asset-inventory/plans` | POST | 创建盘点计划 |
| `/api/asset-inventory/plans/{id}` | PUT | 更新盘点计划 |
| `/api/asset-inventory/plans/{id}` | DELETE | 删除盘点计划 |
| `/api/asset-inventory/plans/{id}/assign` | POST | 分配盘点任务 |
| `/api/asset-inventory/plans/{id}/start` | POST | 开始盘点 |
| `/api/asset-inventory/plans/{id}/complete` | POST | 完成盘点 |
| `/api/asset-inventory/details` | GET | 获取盘点明细 |

### 4.9 资产业务申请接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/asset-applications` | GET | 获取申请列表 |
| `/api/asset-applications/{id}` | GET | 获取申请详情 |
| `/api/asset-applications` | POST | 创建申请 |
| `/api/asset-applications/{id}/approve` | POST | 审批通过 |
| `/api/asset-applications/{id}/reject` | POST | 审批拒绝 |
| `/api/asset-applications/{id}/start-maintenance` | POST | 开始维修 |
| `/api/asset-applications/{id}/complete-maintenance` | POST | 完成维修 |

### 4.10 采购管理接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/purchase-requests` | GET | 获取采购申请列表 |
| `/api/purchase-requests` | POST | 创建采购申请 |
| `/api/purchase-requests/{id}/approve` | POST | 审批采购申请 |
| `/api/purchase-requests/{id}/reject` | POST | 拒绝采购申请 |
| `/api/purchase-orders` | GET | 获取采购订单列表 |
| `/api/purchase-orders` | POST | 创建采购订单 |

### 4.11 报表统计接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/dashboard/stats` | GET | 获取仪表盘统计 |
| `/api/dashboard/recent-operations` | GET | 获取最近操作 |
| `/api/reports/assets` | GET | 资产统计报表 |
| `/api/reports/departments` | GET | 部门资产报表 |
| `/api/depreciation/report` | GET | 折旧报表 |

### 4.12 角色管理接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/roles` | GET | 获取角色列表 |
| `/api/roles/{id}` | GET | 获取角色详情 |

---

## 5. 核心业务流程

### 5.1 资产折旧流程

系统支持三种折旧方法：

1. **直线法**：每期折旧额 =（原值 - 残值）/ 使用年限
2. **双倍余额递减法**：年折旧率 = 2 / 使用年限 × 100%
3. **工作量法**：单位工作量折旧 =（原值 - 残值）/ 总工作量

### 5.2 资产盘点流程

```
创建盘点计划 → 分配盘点任务 → 开始盘点 → 执行盘点 → 完成盘点
     │              │              │            │            │
     ▼              ▼              ▼            ▼            ▼
   生成明细      记录日志      记录日志     更新明细     统计结果
```

### 5.3 资产业务流程

#### 资产领用/转移
```
普通员工申请 → 部门资产管理员审批 → 完成
```

#### 资产维修
```
普通员工申请 → 部门资产管理员批准 → 普通员工开始维修 → 普通员工完成维修
```

#### 资产报废（二级审批）
```
部门资产管理员申请 → 部门领导终审 → 资产报废
```

### 5.4 采购流程

```
采购申请 → 部门领导审批 → 生成采购订单 → 采购入库
```

---

## 6. 安全机制

### 6.1 认证机制

系统采用 JWT（JSON Web Token）进行身份认证：

1. 用户登录成功后，服务器生成 JWT Token
2. 客户端在后续请求中携带 Token
3. 服务器验证 Token 有效性后处理请求

### 6.2 权限控制

基于 Spring Security 的方法级权限控制：

| 接口前缀 | 允许访问角色 |
|---------|-------------|
| `/api/admin/**` | admin |
| `/api/leader/**` | leader, admin |
| `/api/manager/**` | manager, leader, admin |
| `/api/assets/**` | user, manager, leader, admin |
| `/api/suppliers/**` | user, manager, leader, admin |

### 6.3 跨域配置

系统配置了 CORS，允许前端应用跨域访问：

```java
configuration.setAllowedOriginPatterns(Arrays.asList("*"));
configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(Arrays.asList("*"));
configuration.setAllowCredentials(true);
```

---

## 7. 项目结构

### 7.1 后端项目结构

```
src/main/java/com/enterprise/asset/enterpriseassetmanagement/
├── common/              # 公共类
│   └── Result.java       # 统一响应结果
├── config/               # 配置类
│   ├── SecurityConfig.java
│   ├── DataInitializer.java
│   └── GlobalExceptionHandler.java
├── controller/           # 控制器层
│   ├── AssetController.java
│   ├── AssetCategoryController.java
│   ├── AssetInventoryController.java
│   ├── AssetApplicationController.java
│   ├── AuthController.java
│   ├── DepartmentController.java
│   ├── DepreciationController.java
│   ├── PurchaseRequestController.java
│   ├── PurchaseOrderController.java
│   ├── ReportController.java
│   ├── SupplierController.java
│   ├── UserController.java
│   └── RoleController.java
├── dto/                  # 数据传输对象
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── ...
├── entity/               # 实体类
│   ├── Asset.java
│   ├── AssetCategory.java
│   ├── AssetApplication.java
│   ├── AssetInventory.java
│   ├── DepreciationRecord.java
│   ├── Department.java
│   ├── PurchaseRequest.java
│   ├── PurchaseOrder.java
│   ├── Supplier.java
│   ├── SysLog.java
│   ├── User.java
│   └── Role.java
├── repository/           # 数据访问层
│   └── *Repository.java
├── security/             # 安全相关
│   ├── JwtRequestFilter.java
│   ├── JwtUtil.java
│   ├── UserDetailsImpl.java
│   └── UserDetailsServiceImpl.java
└── service/              # 服务层
    ├── impl/
    │   └── *ServiceImpl.java
    └── DepreciationCalculator.java
```

### 7.2 前端项目结构

```
fronted/src/
├── views/                # 页面组件
│   ├── Home.vue
│   ├── Login.vue
│   ├── Asset.vue
│   ├── AssetCategory.vue
│   ├── AssetInventory.vue
│   ├── AssetApply.vue
│   ├── Depreciation.vue
│   ├── Department.vue
│   ├── Supplier.vue
│   └── ...
├── components/           # 公共组件
│   └── business/
├── App.vue
└── main.js
```

---

## 8. 配置说明

### 8.1 数据库配置

配置文件路径：`src/main/resources/application.properties`

```properties
# 数据库连接
spring.datasource.url=jdbc:mysql://localhost:3306/asset_management
spring.datasource.username=root
spring.datasource.password=123456

# JPA配置
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
```

### 8.2 服务器配置

```properties
server.port=8080
spring.application.name=enterprise-asset-management
```

---

## 9. 快速开始

### 9.1 环境要求

- JDK 17+
- MySQL 8.0+
- Node.js 16+
- Maven 3.8+

### 9.2 数据库初始化

1. 创建数据库：
```sql
CREATE DATABASE asset_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 使用系统提供的 SQL 脚本初始化表结构

### 9.3 启动后端服务

```bash
# 进入项目目录
cd enterprise-asset-management

# 编译项目
mvn clean install

# 启动服务
mvn spring-boot:run
```

### 9.4 启动前端服务

```bash
# 进入前端目录
cd fronted

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

### 9.5 默认账号

系统初始化时创建默认角色，用户需手动创建或通过数据初始化脚本创建：

| 角色 | 角色代码 |
|------|---------|
| 系统管理员 | admin |
| 部门领导 | leader |
| 部门资产管理员 | manager |
| 普通员工 | user |

---

## 10. 系统特性

### 10.1 核心特性

- **全流程管理**：覆盖资产从采购到报废的完整生命周期
- **多维度折旧**：支持直线法、双倍余额递减法、工作量法
- **智能盘点**：计划制定、任务分配、执行跟踪、结果统计
- **灵活审批**：支持多种审批流程配置
- **实时统计**：多维度的资产数据统计与分析
- **日志追踪**：完整的操作记录与审计追踪

### 10.2 安全特性

- JWT Token 认证
- 基于角色的访问控制
- 操作日志记录
- 跨域访问控制

### 10.3 性能特性

- RESTful API 设计
- 数据库索引优化
- 分页查询支持
- 批量操作支持

---

## 11. 版本信息

- **项目版本**：0.0.1-SNAPSHOT
- **Spring Boot 版本**：4.0.6
- **Vue 版本**：3.5.25
- **Element Plus 版本**：2.13.3

---

## 12. 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件至项目维护者

---

## 13. 许可证

本项目采用 MIT 许可证
