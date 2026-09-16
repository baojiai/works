# 公网部署说明

本项目是标准 Servlet/JSP/Tomcat 应用。公网运行需要服务器、JDK、Tomcat 9 和 MySQL 8；GitHub 仅用于保存代码，不能直接运行该应用。

## 推荐部署形态

```text
用户浏览器
   ↓
域名或公网 IP
   ↓
Nginx / 防火墙转发
   ↓
Tomcat 9
   ↓
after-sales.war
   ↓
MySQL 8
```

## 1. 构建应用

```powershell
mvn clean package
```

或使用包装脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

将生成的 `target\after-sales.war` 上传到 Tomcat 的 `webapps` 目录。

## 2. 初始化 MySQL

```bash
mysql -u <管理员用户> -p < database/after_sales.sql
```

建议创建仅能访问 `after_sales` 库的应用账号：

```sql
CREATE USER 'after_sales_app'@'localhost' IDENTIFIED BY '<强密码>';
GRANT ALL PRIVILEGES ON after_sales.* TO 'after_sales_app'@'localhost';
FLUSH PRIVILEGES;
```

## 3. 配置数据库环境变量

Windows Server：

```powershell
$env:APP_DB_URL='jdbc:mysql://127.0.0.1:3306/after_sales?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:APP_DB_USER='after_sales_app'
$env:APP_DB_PASSWORD='你的数据库密码'
$env:APP_DB_DRIVER='com.mysql.cj.jdbc.Driver'
```

Linux Server：

```bash
export APP_DB_URL='jdbc:mysql://127.0.0.1:3306/after_sales?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
export APP_DB_USER='after_sales_app'
export APP_DB_PASSWORD='你的数据库密码'
export APP_DB_DRIVER='com.mysql.cj.jdbc.Driver'
```

不要把数据库密码写入 Java、JSP、脚本或公开仓库。

## 4. 启动与停止 Tomcat

Windows：

```powershell
.\bin\startup.bat
.\bin\shutdown.bat
```

Linux：

```bash
./bin/startup.sh
./bin/shutdown.sh
```

访问入口：

```text
http://服务器地址:8080/after-sales/client/login
http://服务器地址:8080/after-sales/warehouse/login
http://服务器地址:8080/after-sales/admin/login
```

## 5. 上线前检查

- 修改默认管理员和仓库账号密码
- 使用专用、最小权限的 MySQL 账号
- 防火墙只开放必要端口
- 对公网域名配置 HTTPS
- 不使用测试手机号作为正式账号
- 备份 MySQL 数据库并验证恢复流程

临时局域网演示可使用本机局域网 IP 加 `8080` 端口访问；长期公网使用建议配置域名、HTTPS 和反向代理。
