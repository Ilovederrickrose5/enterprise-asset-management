# 企业资产管理系统 - 技术文档

## 1. 项目概述

### 1.1 项目名称
企业资产管理系统 (Enterprise Asset Management)

### 1.2 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 后端 | Java | 21 |
| 框架 | Spring Boot | 3.2.x |
| ORM | Spring Data JPA | 3.2.x |
| 数据库 | MySQL | 8.0+ |
| 前端 | Vue | 3.x |
| 构建工具 | Maven | 3.9+ |

### 1.3 项目结构

```
enterprise-asset-management/
├── frontend/                    # 前端Vue项目
│   ├── src/
│   │   ├── components/          # 公共组件
│   │   ├── config/              # 配置文件
│   │   ├── router/              # 路由配置
│   │   ├── utils/               # 工具函数
│   │   └── views/               # 页面视图
│   └── vite.config.js           # Vite配置
├── src/
│   └── main/
│       ├── java/com/enterprise/asset/enterpriseassetmanagement/
│       │   ├── controller/      # REST API控制器
│       │   ├── service/         # 业务服务层
│       │   ├── repository/      # 数据访问层
│       │   ├── entity/          # JPA实体类
│       │   ├── dto/             # 数据传输对象
│       │   ├── security/        # 安全认证模块
│       │   ├── config/          # 配置类
│       │   └── common/          # 公共组件
│       └── resources/           # 资源文件
└── pom.xml                      # Maven依赖管理
```

---

## 2. 核心业务功能

### 2.1 功能模块

| 模块 | 说明 | 状态 |
|------|------|------|
| 资产管理 | 资产的增删改查、状态管理 | ✅ |
| 资产领用 | 资产领用申请与审批 | ✅ |
| 资产转移 | 资产部门/人员转移 | ✅ |
| 资产维修 | 维修申请与进度追踪 | ✅ |
| 资产报废 | 二级审批报废流程 | ✅ |
| 资产盘点 | 盘点计划与执行 | ✅ |
| 折旧管理 | 自动折旧计算 | ✅ |
| 采购管理 | 采购申请与订单 | ✅ |
| 报表统计 | 资产统计报表 | ✅ |

### 2.2 核心业务流程

#### 2.2.1 资产报废二级审批流程

```
用户提交报废申请 → pending → 资产管理员初审 → pending_leader → 领导审批 → approved → 资产状态更新为scrapped
                                                    ↓
                                              rejected（拒绝）
```

**状态流转说明：**

| 状态 | 说明 | 操作权限 |
|------|------|----------|
| `pending` | 待审批 | 资产管理员可初审 |
| `pending_leader` | 待领导审批 | 领导可审批 |
| `approved` | 已批准 | 系统自动更新资产状态 |
| `rejected` | 已拒绝 | 可重新提交或撤销 |

---

## 3. 数据库设计

### 3.1 核心表结构

#### 3.1.1 asset（资产表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 资产ID |
| asset_no | VARCHAR(255) | UNIQUE, NOT NULL | 资产编号 |
| asset_name | VARCHAR(255) | NOT NULL | 资产名称 |
| category_id | BIGINT | NULL | 分类ID |
| model | VARCHAR(255) | NULL | 型号规格 |
| purchase_price | DECIMAL(38,2) | NULL | 购入价格 |
| net_value | DECIMAL(38,2) | NULL | 净值 |
| original_value | DECIMAL(38,2) | NOT NULL | 原值 |
| supplier_id | BIGINT | NULL | 供应商ID |
| purchase_date | DATE | NULL | 购入日期 |
| useful_life | INT | NOT NULL | 使用年限（月） |
| depreciation_method | VARCHAR(255) | NULL | 折旧方法 |
| status | VARCHAR(255) | NULL | 资产状态 |
| use_status | VARCHAR(255) | NULL | 使用状态 |
| dept_id | BIGINT | NULL | 所属部门ID |
| user_id | BIGINT | NULL | 使用人ID |
| borrow_status | VARCHAR(255) | NULL | 借出状态 |
| borrower_id | BIGINT | NULL | 借用人ID |
| borrow_time | DATETIME(6) | NULL | 借出时间 |
| expected_return_time | DATETIME(6) | NULL | 预计归还时间 |

#### 3.1.2 asset_application（资产业务申请表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 申请ID |
| asset_id | BIGINT | NULL | 关联资产ID |
| application_type | VARCHAR(255) | NOT NULL | 申请类型：RECEIVE/TRANSFER/MAINTENANCE/DISPOSAL |
| applicant_id | BIGINT | NOT NULL | 申请人ID |
| applicant_name | VARCHAR(255) | NOT NULL | 申请人姓名 |
| department_id | BIGINT | NULL | 部门ID |
| status | VARCHAR(255) | NULL | 申请状态 |
| approver_id | BIGINT | NULL | 审批人ID |
| approval_date | DATETIME | NULL | 审批日期 |
| approval_remark | TEXT | NULL | 审批备注 |
| original_application_id | BIGINT | NULL | 关联原申请ID（用于二级审批） |

#### 3.1.3 user（用户表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(255) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt加密） |
| real_name | VARCHAR(255) | NULL | 真实姓名 |
| dept_id | BIGINT | NULL | 所属部门ID |
| role | VARCHAR(255) | NULL | 角色：admin/leader/manager/user |
| status | INT | NOT NULL | 状态：1-启用，0-禁用 |

### 3.2 索引设计

| 表名 | 索引名称 | 字段 | 类型 |
|------|----------|------|------|
| asset | PRIMARY | id | 主键 |
| asset | asset_no | asset_no | 唯一索引 |
| asset | idx_category_id | category_id | 普通索引 |
| asset | idx_status | status | 普通索引 |
| asset | idx_user_id | user_id | 普通索引 |
| asset | idx_dept_id | dept_id | 普通索引 |
| asset | idx_category_status | category_id, status | 联合索引 |
| asset_application | idx_application_status_dept | status, department_id | 联合索引 |

---

## 4. API接口设计

### 4.1 资产管理接口

| API路径 | HTTP方法 | 功能描述 |
|---------|----------|----------|
| `/api/assets` | GET | 获取资产列表（支持分页） |
| `/api/assets/{id}` | GET | 获取单个资产详情 |
| `/api/assets` | POST | 新增资产 |
| `/api/assets/{id}` | PUT | 更新资产信息 |
| `/api/assets/{id}` | DELETE | 删除资产 |
| `/api/assets/status/{status}` | GET | 按状态查询资产 |
| `/api/assets/department/{deptId}` | GET | 按部门查询资产 |

### 4.2 资产业务申请接口

| API路径 | HTTP方法 | 功能描述 |
|---------|----------|----------|
| `/api/asset-applications` | GET | 获取申请列表 |
| `/api/asset-applications/{id}` | GET | 获取申请详情 |
| `/api/asset-applications` | POST | 创建申请 |
| `/api/asset-applications/{id}/approve` | POST | 批准申请 |
| `/api/asset-applications/{id}/reject` | POST | 拒绝申请 |
| `/api/asset-applications/status/pending_leader` | GET | 获取待领导审批列表 |
| `/api/asset-applications/status/pending_leader/department/{deptId}` | GET | 获取部门待领导审批列表 |

### 4.3 用户认证接口

| API路径 | HTTP方法 | 功能描述 |
|---------|----------|----------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/auth/me` | GET | 获取当前用户信息 |

### 4.4 报表接口

| API路径 | HTTP方法 | 功能描述 |
|---------|----------|----------|
| `/api/reports/asset-status` | GET | 资产状态分布报表 |
| `/api/reports/department-asset` | GET | 部门资产统计报表 |
| `/api/reports/depreciation-summary` | GET | 折旧汇总报表 |

---

## 5. 安全与权限

### 5.1 角色定义

| 角色 | 权限描述 |
|------|----------|
| `admin` | 系统管理员，拥有所有权限 |
| `leader` | 部门领导，可审批报废申请 |
| `manager` | 资产管理员，可管理资产、处理申请 |
| `user` | 普通用户，可提交申请、查看自己的资产 |

### 5.2 权限控制策略

- **基于角色的访问控制（RBAC）**
- **JWT Token认证**
- **密码使用BCrypt加密存储**
- **API请求需要携带有效Token**

---

## 6. 核心业务逻辑

### 6.1 资产报废二级审批

```java
// 审批逻辑（简化版）
if ("DISPOSAL".equals(applicationType)) {
    if ("ROLE_admin".equals(userRole) || "ROLE_leader".equals(userRole)) {
        // 领导审批（一级）
        if ("pending_leader".equals(status)) {
            // 更新为leader_approved或直接approved
        }
    } else {
        // 资产管理员审批（二级）
        if ("leader_approved".equals(status)) {
            // 最终批准，更新资产状态为scrapped
        }
    }
}
```

### 6.2 折旧计算策略

系统支持多种折旧方法：

| 方法 | 类名 | 说明 |
|------|------|------|
| 直线法 | `StraightLineDepreciationCalculator` | 平均年限折旧 |
| 双倍余额递减法 | `DoubleDecliningBalanceDepreciationCalculator` | 加速折旧 |
| 工作量法 | `WorkUnitDepreciationCalculator` | 按工作量折旧 |

---

## 7. 部署与运行

### 7.1 环境要求

- JDK 21+
- MySQL 8.0+
- Maven 3.9+

### 7.2 启动方式

```bash
# 后端启动
cd enterprise-asset-management
mvn spring-boot:run

# 前端启动（开发模式）
cd frontend
npm install
npm run dev
```

### 7.3 配置说明

主要配置文件：`application.properties`

```properties
# 服务器端口
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/asset_management?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=admin
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## 8. 代码质量保证

### 8.1 测试覆盖

| 测试类型 | 路径 | 说明 |
|----------|------|------|
| 单元测试 | `src/test/java/...` | 业务逻辑单元测试 |
| 集成测试 | `src/test/java/...` | 服务层集成测试 |

### 8.2 测试示例

```java
// 折旧计算器测试
@Test
void testStraightLineDepreciation() {
    BigDecimal assetValue = new BigDecimal("10000");
    int usefulLife = 60; // 5年 = 60个月
    
    BigDecimal depreciation = calculator.calculate(assetValue, usefulLife, 1);
    assertEquals(new BigDecimal("166.67"), depreciation.setScale(2, RoundingMode.HALF_UP));
}
```

---

## 9. 维护与扩展

### 9.1 日志管理

系统日志记录到 `sys_log` 表：

| 字段 | 说明 |
|------|------|
| user_id | 操作用户ID |
| operation | 操作描述 |
| log_type | 日志类型 |
| status | 操作状态 |

### 9.2 性能优化建议

1. **索引优化**：根据查询频率合理创建索引
2. **查询优化**：避免SELECT *，使用覆盖索引
3. **分页优化**：使用LIMIT分页，避免大数据量一次性加载
4. **缓存策略**：对热点数据进行缓存

---

## 附录：ER图关系

```
user 1 ─── * asset_application (申请人)
user 1 ─── * asset (使用人)
department 1 ─── * asset (所属部门)
department 1 ─── * user (部门用户)
asset_category 1 ─── * asset (资产分类)
supplier 1 ─── * asset (供应商)
asset 1 ─── * asset_application (关联申请)
asset 1 ─── * asset_depreciation (折旧记录)
asset_inventory 1 ─── * asset_inventory_detail (盘点明细)
```
