# 自动化测试覆盖表

更新日期：2026-09-17

## 1. 测试目标与范围

本表对应课程设计中“单元测试方法、集成测试方法、覆盖所有功能点并撰写测试用例”的要求。测试分为四层：

- 单元测试：Mockito 隔离 Mapper 或下游 Service，验证业务分支、输入校验和角色规则。
- Web 层测试：使用 Spring MockMvc 和 Servlet Mock 对象验证路由、模型、Session、登录入口和权限拦截。
- H2 集成测试：加载真实 Spring、MyBatis 和事务配置，在隔离数据库中验证四角色核心业务、提交和回滚。
- MySQL 集成测试：在专用 MySQL 测试库验证连接、事务、并发和 MySQL 方言行为，默认不执行。

## 2. 执行命令

默认自动化测试：

```powershell
mvn test
```

包含 MySQL 集成测试：

```powershell
$env:APP_DB_URL='jdbc:mysql://127.0.0.1:3306/after_sales_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:APP_DB_USER='测试库账号'
$env:APP_DB_PASSWORD='测试库密码'
$env:APP_DB_DRIVER='com.mysql.cj.jdbc.Driver'
$env:RUN_MYSQL_INTEGRATION_TESTS='true'
mvn test
```

MySQL 测试会写入并清理探针数据，只能连接专用测试库。

最近一次默认验证结果：`mvn clean package` 构建成功；共发现 30 项测试，28 项通过，0 项失败，0 项错误，2 项 MySQL 条件测试按设计跳过；WAR 生成于 `target/after-sales.war`。

## 3. 功能覆盖矩阵

状态说明：`自动通过`表示纳入默认 `mvn test`；`条件执行`表示需要专用 MySQL；`人工证据`表示当前有验收记录或截图，但尚未自动化。

| 编号 | 角色/模块 | 功能与验证点 | 测试层级 | 自动化证据 | 状态 |
| --- | --- | --- | --- | --- | --- |
| AUTH-01 | 认证 | 正常登录、错误密码、停用账号、登录日志 | 单元、H2 集成 | `AuthServiceTest`、`Stage7AdminTransactionProbe` | 自动通过 |
| AUTH-02 | 认证 | 注册手机号、密码、确认密码、重复账号、默认昵称、用户资料和日志 | 单元、H2 集成 | `AuthServiceTest`、四个 Stage7 探针 | 自动通过 |
| AUTH-03 | 登录入口 | 客户、仓库、管理员入口模型及角色版本限制 | MockMvc | `AuthControllerWebTest` | 自动通过 |
| AUTH-04 | 会话 | 登录写入 Session、退出、未登录跳转 | MockMvc、Servlet Mock | `AuthControllerWebTest`、`AuthFilterTest` | 自动通过 |
| AUTH-05 | 权限 | 客户不能访问管理员、仓库、工程师页面 | MockMvc | `RoleControllerWebTest` | 自动通过 |
| ROLE-01 | 角色模型 | 工程师兼具客户能力，角色名称映射 | 单元 | `SessionUserTest` | 自动通过 |
| DASH-01 | 工作台 | 客户、工程师、仓库、管理员指标分支 | 单元、MockMvc | `DashboardServiceTest`、`AuthControllerWebTest` | 自动通过 |
| CUST-01 | 客户 | 报修表单数据和新增报修 | MockMvc、H2 集成 | `RoleControllerWebTest`、`Stage7CustomerTransactionProbe` | 自动通过 |
| CUST-02 | 客户 | 工程师候选筛选、详情读取 | H2 集成 | `Stage7CustomerTransactionProbe` | 自动通过 |
| CUST-03 | 客户 | 预约成功、失败回滚、时段占用和工单生成 | H2 集成 | `Stage7CustomerTransactionProbe` | 自动通过 |
| CUST-04 | 客户 | 改约成功、失败回滚、原预约和时段恢复 | H2 集成 | `Stage7CustomerTransactionProbe` | 自动通过 |
| CUST-05 | 客户 | 取消预约、释放时段、取消工单和操作日志 | H2 集成 | `Stage7CustomerTransactionProbe` | 自动通过 |
| CUST-06 | 客户 | 验收通过、验收失败回滚、预约履约 | H2 集成 | `Stage7CustomerTransactionProbe` | 自动通过 |
| CUST-07 | 客户 | 服务评价、统计更新、失败回滚 | H2 集成 | `Stage7CustomerTransactionProbe` | 自动通过 |
| CUST-08 | 客户 | 预约列表、工单列表和工单详情查询 | H2 集成 | `Stage7CustomerTransactionProbe` | 自动通过 |
| ENG-01 | 工程师认证 | 申请、审核、角色转换及失败回滚 | H2 集成 | `Stage7EngineerTransactionProbe`、`Stage7AdminTransactionProbe` | 自动通过 |
| ENG-02 | 工程师 | 档案、技能、服务区域更新及失败回滚 | H2 集成 | `Stage7EngineerTransactionProbe` | 自动通过 |
| ENG-03 | 工程师 | 新增排班、重复排班回滚、关闭时段 | H2 集成 | `Stage7EngineerTransactionProbe` | 自动通过 |
| ENG-04 | 工程师 | 开始维修、状态转换及非法转换回滚 | H2 集成 | `Stage7EngineerTransactionProbe` | 自动通过 |
| ENG-05 | 工程师 | 保存维修记录及失败回滚 | H2 集成 | `Stage7EngineerTransactionProbe` | 自动通过 |
| WH-01 | 仓库 | 配件申请审核、库存锁定及失败回滚 | H2 集成 | `Stage7WarehouseTransactionProbe` | 自动通过 |
| WH-02 | 仓库 | 配件出库、退回、释放、完成及失败回滚 | H2 集成 | `Stage7WarehouseTransactionProbe` | 自动通过 |
| WH-03 | 仓库 | 补充入库、盘点调整、库存流水和数量守恒 | H2 集成 | `Stage7WarehouseTransactionProbe` | 自动通过 |
| ADMIN-01 | 管理员 | 用户停用、启用和登录状态联动 | H2 集成 | `Stage7AdminTransactionProbe` | 自动通过 |
| ADMIN-02 | 管理员 | 系统配置新增、修改和失败回滚 | H2 集成 | `Stage7AdminTransactionProbe` | 自动通过 |
| ADMIN-03 | 管理员 | 设备、故障、区域和标准时段基础数据 | H2 集成 | `Stage7AdminTransactionProbe` | 自动通过 |
| ADMIN-04 | 管理员 | SLA 提醒生成、去重及失败回滚 | H2 集成 | `Stage7AdminTransactionProbe` | 自动通过 |
| ADMIN-05 | 管理员 | 超时改约处理、通知、日志及失败回滚 | H2 集成 | `Stage7AdminTransactionProbe` | 自动通过 |
| MSG-01 | 通知 | 当前用户通知页面和未读数量 | 单元、MockMvc | `DashboardServiceTest`、`RoleControllerWebTest` | 自动通过 |
| DB-01 | 数据库 | 正式 MySQL 连接、版本和当前数据库 | MySQL 集成 | `Stage9MySqlConnectionProbe` | 条件执行 |
| DB-02 | 数据库 | MySQL 事务、生成键、时区、Boolean 映射 | MySQL 集成 | `Stage9MySqlTransactionProbe` | 条件执行 |
| DB-03 | 并发 | 库存并发和预约并发一致性 | MySQL 集成 | `Stage9MySqlTransactionProbe` | 条件执行 |
| UI-01 | PC 端 | 主要角色业务页面和 1920×1080 页面效果 | 人工浏览器 | `figure/图4-1` 至 `图4-12` | 人工证据 |
| UI-02 | 移动端 | 375px 和小米 14 工作台、抽屉导航、纵向报修表单 | 人工浏览器 | `figure/图4-13`、`figure/图4-14` | 人工证据 |

## 4. 测试类清单

| 测试类 | 用途 | 默认执行 |
| --- | --- | --- |
| `AuthServiceTest` | 登录与注册业务单元测试 | 是 |
| `DashboardServiceTest` | 四角色工作台分支单元测试 | 是 |
| `SessionUserTest` | 角色模型与工程师兼容客户角色 | 是 |
| `AuthControllerWebTest` | 登录、注册、退出、Session 和工作台 MockMvc 测试 | 是 |
| `RoleControllerWebTest` | 四角色主页面及跨角色越权访问 MockMvc 测试 | 是 |
| `AuthFilterTest` | 公共路径、静态资源和登录拦截测试 | 是 |
| `TransactionProbeTest` | 客户、工程师、仓库、管理员 H2 事务测试 | 是 |
| `MySqlProbeTest` | MySQL 连接、事务与并发测试 | 条件执行 |

## 5. 尚未自动化的范围

- JSP 在真实 Tomcat 中的浏览器端点击、表单提交和视觉回归，目前由人工验收和截图覆盖。
- PC 与移动端的布局、文字截断和交互体验仍需在演示前人工复查。
- 性能测试尚未建立固定并发量、响应时间和吞吐量基线。
- MySQL 探针不会在默认 `mvn test` 中运行，验收前需在专用测试库执行并保留 Surefire 报告。

以上项目不会被标记为默认自动通过，避免把人工证据或条件测试误写成全自动覆盖。
