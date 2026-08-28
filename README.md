# 售后维修预约与配件协同管理系统

这是一个 Java Web 课程设计项目，基于 Servlet、JSP/JSTL、JDBC、Tomcat 9 和数据库实现。系统面向售后维修场景，包含客户报修、AI 辅助诊断、工程师认证与接单、预约管理、维修工单、配件申请、仓库审核、客户验收评价和平台管理等功能。

## 系统版本入口

项目按真实平台思路拆分为三个主要发行入口：

| 版本 | 入口 |
| --- | --- |
| 客户版 | `http://localhost:8080/after-sales/client/login` |
| 区域仓库版 | `http://localhost:8080/after-sales/warehouse/login` |
| 平台管理端 | `http://localhost:8080/after-sales/admin/login` |

兼容入口：

```text
http://localhost:8080/after-sales/login
```

## 主要功能

- 手机号注册和客户登录
- 客户搜索问题并使用 AI 辅助诊断
- 根据故障、区域、时间筛选可接单工程师
- 类似外卖平台的工程师市场选择模式
- 客户提交工程师认证申请
- 管理员审核工程师认证资料
- 工程师维护服务档案和可约时段
- 客户预约、取消、改约、验收和评价
- 工程师维修记录、完工提交、异常取消
- 工程师向仓库申请配件
- 仓库审核配件申请、锁定库存、出库、退回和盘点
- 管理端维护账号、资质、基础数据、业务规则和 SLA

## 本地运行环境

需要：

- JDK 8
- Tomcat 9
- PowerShell

默认 Tomcat 路径：

```text
E:\Tomcat\apache-tomcat-9.0.115
```

如果你的 Tomcat 不在这个路径，可以在运行前设置 `CATALINA_HOME`。

## 构建和启动

```powershell
cd E:\软件课程设计\after-sales-repair-system
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

停止服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1
```

检查构建：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1
```

## DeepSeek AI 配置

项目支持接入 DeepSeek API。不要把 API Key 写进代码或 GitHub。

推荐使用安全启动脚本：

```powershell
cd E:\软件课程设计\after-sales-repair-system
powershell -ExecutionPolicy Bypass -File .\scripts\run-with-deepseek.ps1
```

脚本会要求输入 DeepSeek API Key，只在当前 Tomcat 进程中生效，不会保存到文件。

也可以手动设置环境变量：

```powershell
$env:DEEPSEEK_API_KEY='你的 DeepSeek API Key'
$env:DEEPSEEK_MODEL='deepseek-v4-flash'
```

## 默认账号

初始内部账号密码均为：

```text
123456
```

| 角色 | 账号 |
| --- | --- |
| 区域仓库 | `warehouse` |
| 平台管理员 | `admin` |

客户账号请在客户版登录页通过手机号注册。

## 数据库说明

默认使用项目自带 H2 文件数据库，适合本地课程设计演示。数据保存在 Tomcat runtime 的 `data/after_sales.*` 文件中。

如果要多人公网使用，建议切换 MySQL 8。

MySQL 环境变量示例：

```powershell
$env:APP_DB_URL='jdbc:mysql://localhost:3306/after_sales?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:APP_DB_USER='root'
$env:APP_DB_PASSWORD='你的数据库密码'
$env:APP_DB_DRIVER='com.mysql.cj.jdbc.Driver'
```

初始化脚本：

- `db/schema-mysql.sql`
- `db/seed-mysql.sql`

## 项目结构

```text
src/                 Java 后端代码
web/                 JSP 页面、静态资源、WEB-INF 配置
db/                  H2 / MySQL 建表和初始化脚本
scripts/             构建、运行、停止、检查脚本
docs/                部署说明文档
tests/               测试相关文件
```

## 公网部署

GitHub 只负责保存代码，不能直接运行 Servlet/JSP/Tomcat 项目。

如果要让其他人通过公网访问，需要部署到服务器：

```text
公网服务器 + JDK 8 + Tomcat 9 + MySQL 8 + DeepSeek 环境变量
```

详细说明见：

```text
docs/PUBLIC_DEPLOY.md
```
