# 售后维修预约与配件协同管理系统

这是一个 Java Web 课程设计项目，基于 Spring MVC、MyBatis、JSP/JSTL、MySQL 8、Tomcat 9 和 Maven 实现。系统面向售后维修场景，包含客户报修、AI 辅助诊断、工程师认证与接单、预约管理、维修工单、配件申请、仓库审核、客户验收评价和平台管理等功能。

## 技术栈

- Java 8（字节码兼容）
- Spring Framework 5.3 / Spring MVC
- MyBatis 3.5 + mybatis-spring（Spring 声明式事务）
- JSP / JSTL
- Bootstrap 5.3.8（本地化资源，无 CDN 依赖）+ HTML5 / CSS3 / 原生 JavaScript / Fetch
- MySQL 8.0.16+（正式数据库，InnoDB / utf8mb4）
- Tomcat 9
- Maven
- DeepSeek API（AI 辅助诊断，可选）

## 前置要求

- JDK 8+（项目以 `--release 8` 生成 Java 8 字节码）
- Maven 3.6+
- Tomcat 9
- MySQL 8.0.16+（本机或可连接实例）
- PowerShell

## 数据库初始化（一次性）

1. 在 MySQL 中执行建库脚本（独立完整，utf8mb4/InnoDB，含种子数据）：

```bash
mysql -u <用户> -p < database/after_sales.sql
```

2. 创建应用账号（或使用已有账号），授予 after_sales 库权限。
3. 设置连接环境变量（密码不要写入任何文件）：

```powershell
$env:APP_DB_URL='jdbc:mysql://localhost:3306/after_sales?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:APP_DB_USER='你的用户名'
$env:APP_DB_PASSWORD='你的密码'
$env:APP_DB_DRIVER='com.mysql.cj.jdbc.Driver'
```

未设置环境变量时，应用使用 `src/jdbc.properties` 中的默认值（默认密码为空，连接会失败并明确报错，不会静默回退其它数据库）。

## 系统版本入口

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

## 构建

```powershell
mvn clean package
```

或使用包装脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

构建产物：

```text
target\after-sales.war
```

## 运行

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

脚本会：检查/构建 WAR → 部署到项目运行目录 `runtime\tomcat\webapps` → 启动 Tomcat 9（`runtime\tomcat` 为隔离的 CATALINA_BASE，日志在 `runtime\tomcat\logs`）。控制台按 Ctrl+C 停止。

停止服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1
```

Tomcat 路径优先取环境变量 `CATALINA_HOME`，未设置时自动检测 `D:\Program Files\Tomcat\apache-tomcat-9.0.121`。

## DeepSeek AI 配置

项目支持接入 DeepSeek API。不要把 API Key 写进代码或 GitHub。

推荐使用安全启动脚本（Key 以安全方式输入，只存在于当前进程环境变量，不会写入文件）：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-with-deepseek.ps1
```

也可以手动设置环境变量：

```powershell
$env:DEEPSEEK_API_KEY='你的 DeepSeek API Key'
$env:DEEPSEEK_MODEL='deepseek-v4-flash'
```

未配置 Key 时，AI 诊断接口会返回明确的"未配置"提示，不影响其他功能。

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

正式数据库为 **MySQL 8.0.16+**，建库脚本与种子数据见：

```text
database/after_sales.sql
```

历史开发阶段使用的 H2 脚本（`db/schema-h2.sql`、`db/seed-h2.sql`）仅作开发调试参考，正式运行不再自动初始化 H2。

## 项目结构

```text
pom.xml              Maven 构建与依赖配置
src/                 Java 后端代码（Spring MVC Controller、Service、MyBatis Mapper）
web/                 JSP 页面、静态资源、WEB-INF 配置
database/            MySQL 8 正式建库脚本（after_sales.sql）
db/                  H2 历史脚本（开发调试参考）
scripts/             Maven/Tomcat 包装脚本（build/run/stop/run-with-deepseek）
docs/                部署说明文档
tests/probes/        Stage7/9 事务与 MySQL 回归探针（事务回滚、并发、方言验证）
README.md            项目说明、构建与本地运行入口
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
