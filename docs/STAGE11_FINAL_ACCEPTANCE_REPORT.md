# 阶段11 项目最终验收与交付封装报告

验收日期：2026-09-08  
项目目录：`D:\Desktop\works-dep`  
结论：**达到最终可交付状态**。

## 1. 最终技术栈

- Java 8 字节码（本机 JDK 25.0.2 使用 Maven `--release 8` 构建）
- Spring Framework / Spring MVC 5.3.39
- MyBatis 3.5.19、MyBatis-Spring 2.1.2
- JSP、JSTL 1.2
- Bootstrap 5.3.8 本地资源、HTML5、CSS3、JavaScript、Fetch/AJAX
- MySQL 8.4.8（满足 8.0.16+）、InnoDB、utf8mb4
- Apache Tomcat 9.0.121
- Maven 3.9.13

源码和文档未宣称 Spring Boot、Vue、React、Tomcat 10、Jakarta Servlet 或 H2 正式数据库。

## 2. 项目目录结构

```text
pom.xml
README.md
src/
  applicationContext.xml
  spring-mvc.xml
  mybatis-config.xml
  jdbc.properties
  com/course/aftersales/
    controller/  filter/  mapper/  model/  service/  util/
web/
  assets/
    vendor/bootstrap/  images/  app.css  app.js
  WEB-INF/
    views/admin/  views/customer/  views/engineer/  views/warehouse/
    web.xml
database/
  after_sales.sql
db/
  schema-h2.sql  seed-h2.sql
scripts/
  build.ps1  check.ps1  run.ps1  stop.ps1
tests/probes/
docs/
  PUBLIC_DEPLOY.md
```

统计：8 个 Controller、8 个 Service、20 个 Mapper 接口、20 个 Mapper XML、19 个 JSP/JSPF、7 个探针。保留既有 `src/`、`web/` 布局，未做 Maven 目录迁移。

## 3. 生产代码静态扫描

生产源码扫描结果：

- `@WebServlet`：0
- `extends HttpServlet`：0
- Controller 内 `new XxxService(...)`：0
- `Database` 生产引用：0
- `DriverManager` / `Statement` / `PreparedStatement` / `ResultSet` 生产引用：0
- `openSession()`、手动 commit/rollback：0
- shared mapper/session factory/旧兼容桥：0
- H2 生产初始化或生产 JDBC URL：0
- `jakarta.servlet`：0

`DriverManagerDataSource` 是 Spring 管理的数据源类型，不属于手写 JDBC 业务代码。

## 4. Spring 架构核验

实际调用链为 Controller → Service → Mapper → MySQL。全部 Controller 是 Spring MVC Bean并通过构造器注入 Service；全部业务 Service 是 Spring Bean。多步骤写操作使用 `@Transactional(rollbackFor = Exception.class)`。根上下文只存在一套 `dataSource`、`sqlSessionFactory`、`transactionManager`，未发现第二套隐藏事务体系。

## 5. MySQL 脚本核验

正式入口唯一为 `database/after_sales.sql`。脚本包含 29 张表、53 个外键、12 个 UNIQUE、5 个 CHECK；29 张表全部使用 InnoDB/utf8mb4。正式种子仅包含 warehouse、admin、基础设备/故障/区域/时段/配件/库存/系统配置，无测试业务数据和数据库连接密码。

`schema-mysql.sql`、`seed-mysql.sql` 不存在。`db/schema-h2.sql` 与 `db/seed-h2.sql` 已在首行明确标注为 legacy/dev reference，且不参与正式构建。

## 6. 临时空库初始化结果

在 `target/stage11-mysql-data` 创建一次性独立 MySQL 8.4.8 实例，监听 127.0.0.1:3307；直接执行原始 `database/after_sales.sql` 成功。初始校验为 29 表、2 个种子账号、0 报修、0 通知。Spring 上下文及 Stage9 MySQL 事务探针连接该实例成功。临时实例正常关闭，3307 释放；后续 `mvn clean` 已删除整个临时数据目录。

## 7. 环境变量验证

正式连接读取 `APP_DB_URL`、`APP_DB_USER`、`APP_DB_PASSWORD`、`APP_DB_DRIVER`。不设置密码时，Tomcat和登录页可以启动，但首次数据库操作明确返回 MySQL连接失败；未创建或连接 H2。设置正确变量后，Spring连接和完整 HTTP 业务链均成功。运行及日志未打印数据库密码。

## 8. Maven结果

`mvn clean package`：BUILD SUCCESS。最终产物唯一为 `target/after-sales.war`，大小 17,830,921 字节，SHA-256：`FE5B14A7104F599FACA0749CD43B869637FBA9E536124E415FF9786ED35FACA1`。

## 9. build.ps1

脚本内部调用 `mvn clean package`，验收结果 BUILD SUCCESS，并校验 `target/after-sales.war` 存在。未生成 `build/after-sales.war`。

## 10. run.ps1

从 8080 空闲状态执行成功。识别 Tomcat 9.0.121，设置 `CATALINA_BASE=runtime/tomcat`，部署 `target/after-sales.war`，连接 MySQL并启动；客户登录页返回 HTTP 200。

## 11. stop.ps1

使用该项目的 `CATALINA_HOME`/`CATALINA_BASE` 调用 Tomcat shutdown。Tomcat正常退出，8080释放；未按进程名批量终止 Java，验收时未误杀其他 Java 进程。

## 12. 启动脚本

项目通过 `build.ps1`、`run.ps1` 和 `stop.ps1` 完成构建、启动与停止，不需要额外的外部服务配置。

## 13. WAR依赖

WAR包含 Spring 5.3.39、MyBatis/MyBatis-Spring、MySQL Connector/J 8.4.0、JSTL、本地Bootstrap 5.3.8、19个JSP/JSPF、20个Mapper XML、Spring XML及40个业务class。WAR不含 H2 JAR/H2 SQL、`javax.servlet-api`、测试探针、测试截图、历史数据库文件或数据库密码。

## 14. 默认演示账号

| 角色 | 用户名 | 默认密码 |
| --- | --- | --- |
| 仓库管理员 | `warehouse` | `123456` |
| 系统管理员 | `admin` | `123456` |

客户通过注册创建；工程师通过客户注册、提交申请、管理员审核后获得角色。以上仅为项目公开演示账号，不包含 MySQL 或外部服务凭据。

## 15. 客户最终链

HTTP验收通过：注册、登录、报修、工程师候选、预约、预约列表、工单详情、验收、评价、通知及全部已读。最终工单为 COMPLETED，预约为 FULFILLED，验收与评价各1条。

## 16. 工程师最终链

HTTP验收通过：客户身份申请、管理员审核、重新登录成为工程师、档案、技能、服务区域、排班、查看工单、START、维修记录、配件申请、FINISH、查看评价关联数据。

## 17. Warehouse最终链

HTTP验收通过：申请列表、库存页、审核、出库、退库、完成、补充入库及反向调整恢复种子值。库存回滚探针和库存预警数据查询正常。

## 18. Admin最终链

HTTP验收通过：Admin首页、用户停用/启用、工程师申请审核、资质状态、系统配置、SLA、超时改约；基础数据由种子查询和Service/Mapper路径核验，未为验收永久新增基础数据。

## 19. 事务最终抽查

`Stage9MySqlTransactionProbe` 在正式MySQL和独立临时MySQL均通过：生成键、预约失败回滚、改约失败回滚、管理员审核回滚、仓库审核回滚、仓库出库回滚、Warehouse complete rollback、正常完成、库存并发、预约并发、时区、Boolean、SLA、超时改约全部OK。

首次在历史正式库执行时因探针使用全局通知计数而被旧数据干扰；恢复种子状态后通过。另发现探针清理遗漏发给种子仓库账号的测试通知，已仅在 `tests/probes/Stage9MySqlTransactionProbe.java` 补充清理语句。

## 20. 前端最终扫描

Bootstrap bundle本地资源存在并标识5.3.8；JSP无CDN依赖。`.app-btn`、`.app-card`、`.app-badge`、`.status`、`.metric`、`.table-card`、`.form-grid`、`.detail-layout`、`.inline-form` 业务class引用均为0。

建议提交前人工登录浏览器进行375/768/1366三档目检；阶段10E未生成登录后真实headless截图不记为缺陷，也未新增自动登录截图系统。

## 21. 敏感信息扫描

全项目复扫未发现私钥、`sk-`密钥、静态 Bearer Token 或源码/文档数据库密码。发现 `.claude/settings.local.json` 两条历史命令曾嵌入数据库密码，已替换为 `$APP_DB_PASSWORD` 引用，并将 `.claude/` 加入 `.gitignore`；未在本报告输出该值。

## 22. 测试数据清理

正式 `after_sales` 已使用正式SQL恢复纯种子状态：29表、2账号；报修、预约、工单、工程师申请、配件申请、通知、操作日志均为0。五项种子库存分别恢复为20/8/15/10/20，available等于total，locked与issued均为0。admin与warehouse保留且ACTIVE。

## 23. 历史文件处理

已删除旧 `build/`、旧WAR、runtime展开应用、runtime日志/临时文件和 `runtime/tomcat/data/after_sales.mv.db.stage9-bak`。当前项目只有 `target/after-sales.war` 一个WAR，无H2数据库文件、截图或日志。保留 `runtime/tomcat/conf` 等运行骨架；保留并明确标注的 `db/` legacy/dev参考SQL。

## 24. README

已逐项核对项目简介、技术栈、环境要求、MySQL初始化、四个 APP_DB 变量、Maven构建、Tomcat运行/停止、默认演示账号和项目结构；与源码和脚本一致，无过时栈声明。

## 25. PUBLIC_DEPLOY

已核对环境、SQL、环境变量、build、run、访问和 stop。修正应用账号示例为 `after_sales_app`，并补充 Windows/Linux 停止 Tomcat 命令。新接手者无需依赖开发者隐含知识即可部署。

## 26. 最终交付文件清单

- `pom.xml`
- `src/`
- `web/`
- `database/after_sales.sql`
- `scripts/`
- `README.md`
- `docs/PUBLIC_DEPLOY.md`
- `docs/STAGE11_FINAL_ACCEPTANCE_REPORT.md`
- `tests/probes/`
- 构建产物：`target/after-sales.war`

课程提交必须包含源码和上述文档，不应只提交WAR。`runtime/`、`target/`和IDE本地文件可按课程提交规则排除；需要提交现成可部署包时附带最终WAR。

## 27. 当前仍存在的已知风险

- 当前目录没有 `.git`，无法用提交历史证明阶段9B后并发相关Service/Mapper是否未修改；本次已在MySQL重新执行两项并发探针并通过，功能证据有效。
- JDK 25构建Java 8字节码时输出“source/target 8已过时”警告，不影响BUILD SUCCESS；在README要求的JDK 8+环境仍需按目标环境常规复验。
- Tomcat停止时MySQL Connector/J abandoned-connection-cleanup线程会输出一次类加载器清理警告，但进程正常退出、端口释放，未观察到持续进程泄漏。
- 最终提交前仍建议进行375/768/1366三档人工浏览器目检。

以上均不是当前交付阻断项。

## 28. 最终状态

MySQL空库初始化可复现、Maven构建成功、Tomcat运行/停止成功、四角色核心业务成功、事务与并发抽查成功、Bootstrap资源正常、正式数据库已恢复纯种子、敏感信息已清理、README与部署文档正确、交付资产完整。

**项目达到“最终可交付”状态。前端冻结、后端冻结；阶段11完成后停止主动修改。**
