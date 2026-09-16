# 售后维修预约与配件协同管理系统

这是一个基于 Spring MVC、MyBatis、JSP/JSTL、MySQL 8、Tomcat 9 和 Maven 的 Java Web 课程设计项目。系统覆盖客户报修、工程师认证与接单、预约管理、维修工单、配件申请、仓库审核、客户验收评价、通知和平台管理。

## 技术栈

- Java 8（字节码兼容）
- Spring Framework 5.3 / Spring MVC
- MyBatis 3.5 / MyBatis-Spring
- JSP / JSTL
- Bootstrap 5.3.8（本地资源，无 CDN）
- HTML5 / CSS3 / 原生 JavaScript
- MySQL 8.0.16+（InnoDB / utf8mb4）
- Tomcat 9
- Maven

## 前置要求

- JDK 8+
- Maven 3.6+
- Tomcat 9
- MySQL 8.0.16+
- PowerShell（使用 Windows 包装脚本时）

## 数据库初始化

执行正式建库脚本：

```bash
mysql -u <管理员用户> -p < database/after_sales.sql
```

为应用创建数据库账号后，设置连接环境变量：

```powershell
$env:APP_DB_URL='jdbc:mysql://localhost:3306/after_sales?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:APP_DB_USER='你的用户名'
$env:APP_DB_PASSWORD='你的密码'
$env:APP_DB_DRIVER='com.mysql.cj.jdbc.Driver'
```

未设置环境变量时，应用读取 `src/jdbc.properties`；其中默认密码为空，运行前必须提供有效密码。

## 系统入口

| 版本 | 地址 |
| --- | --- |
| 客户版 | `http://localhost:8080/after-sales/client/login` |
| 区域仓库版 | `http://localhost:8080/after-sales/warehouse/login` |
| 平台管理端 | `http://localhost:8080/after-sales/admin/login` |

兼容登录入口：`http://localhost:8080/after-sales/login`

## 主要功能

- 客户注册、登录和手动提交报修
- 根据设备故障、区域和时间筛选可接单工程师
- 客户预约、取消、改约、验收和评价
- 工程师认证、服务档案、可约时段和维修工单
- 工程师申请配件，仓库审核、锁定、出库、退回和盘点
- 管理员维护账号、资质、基础数据、业务规则和 SLA
- 多角色消息通知

## 构建与运行

```powershell
mvn clean package
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

构建产物为 `target\after-sales.war`。运行脚本将其部署到隔离的 `runtime\tomcat`，并使用本机 Tomcat 9。

停止服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1
```

## 默认内部账号

初始密码均为 `123456`。

| 角色 | 账号 |
| --- | --- |
| 区域仓库 | `warehouse` |
| 平台管理员 | `admin` |

客户账号请在客户版登录页注册。

## 项目结构

```text
pom.xml              Maven 构建与依赖配置
src/                 Spring MVC、Service 与 MyBatis 源码
web/                 JSP 页面与本地静态资源
database/            MySQL 8 正式建库脚本
db/                  历史开发阶段参考脚本
scripts/             build、run、stop 等包装脚本
docs/                部署说明
tests/probes/        事务与 MySQL 回归探针
```

公网部署说明见 `docs/PUBLIC_DEPLOY.md`。
