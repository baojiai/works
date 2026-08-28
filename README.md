# 售后维修预约与配件协同管理系统

本项目依据 `1-4设计说明书修改版(1).docx` 实现，采用 Java 8、Servlet/JSP/JSTL、JDBC 和 MVC 分层。系统按真实平台模式拆分为客户版、工程师版、仓库版与管理端：客户通过手机号注册和登录，用户可在客户端提交工程师认证，管理员审核通过后升级为工程师，区域仓库账号负责配件审核、出库、退回和盘点。

## 一键构建与运行

环境要求：JDK 8、项目同级已有 Tomcat 9（默认路径 `E:\Tomcat\apache-tomcat-9.0.115`）。

```powershell
cd E:\软件课程设计\after-sales-repair-system
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

浏览器访问：

| 发行版本 | 入口 |
| --- | --- |
| 客户版 | `http://localhost:8080/after-sales/client/login` |
| 区域仓库版 | `http://localhost:8080/after-sales/warehouse/login` |
| 平台管理端 | `http://localhost:8080/after-sales/admin/login` |

兼容入口 `http://localhost:8080/after-sales/login` 默认进入客户版。

停止服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1
```

默认使用项目自带 H2 文件数据库，首次启动自动建表并写入系统初始化数据，无需联网。数据保存在 Tomcat 的 `data/after_sales.*` 文件中。

初始化内部账号（密码均为 `123456`）：

| 角色 | 账号 |
| --- | --- |
| 仓库管理员 | warehouse |
| 系统管理员 | admin |

客户账号请在登录页点击“手机号注册客户版”创建。需要成为工程师时，客户登录后进入“工程师认证”，提交身份、技能、区域和材料说明；管理员在“系统管理 → 工程师认证审核”中通过后，该账号重新登录即可进入工程师版。

## 切换 MySQL 8

1. 在 MySQL 中依次执行 `db/schema-mysql.sql` 和 `db/seed-mysql.sql`。
2. 启动 Tomcat 前设置环境变量：

```powershell
$env:APP_DB_URL='jdbc:mysql://localhost:3306/after_sales?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:APP_DB_USER='root'
$env:APP_DB_PASSWORD='你的密码'
$env:APP_DB_DRIVER='com.mysql.cj.jdbc.Driver'
```

## 项目结构

- `src/`：Controller、Service、Repository、Filter、Model
- `web/WEB-INF/views/`：JSP/JSTL 页面
- `web/assets/`：完全本地化的响应式样式和脚本
- `db/`：H2/MySQL 建表与测试数据脚本
- `scripts/`：无 Maven 构建、运行和检查脚本
- `tests/`：数据库业务规则集成检查

## 核心约束

- 同一工程师、日期和标准时段唯一；创建预约时用条件更新二次抢占。
- 时段占用、预约创建和工单创建位于同一事务。
- 工单仅允许按 `待上门 → 维修中 ↔ 等待配件 → 待验收 → 已完成/返修中` 合法流转。
- 配件审核锁定、出库、释放和退回全部以事务维护库存，且写入流水。
- 后端基于角色和资源归属双重校验，密码仅保存 SHA-256 摘要。
