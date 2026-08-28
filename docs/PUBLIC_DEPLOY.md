# 公网部署说明

本项目本地地址 `http://localhost:8080/after-sales/` 只能在当前电脑访问。想让其他人也能使用，需要把应用部署到一台公网服务器上。

## 推荐部署形态

```text
用户浏览器
   ↓
公网域名 / 公网 IP
   ↓
Nginx 或服务器防火墙转发
   ↓
Tomcat 9
   ↓
after-sales.war
   ↓
MySQL 8
```

课程设计演示可以直接开放 `8080` 端口；如果想更像正式应用，建议使用 Nginx 代理到 Tomcat，并配置 HTTPS。

## 服务器需要准备

- JDK 8
- Tomcat 9
- MySQL 8
- 一个公网 IP，或已经解析到服务器的域名
- 已放行的端口：
  - `8080`：直接访问 Tomcat 时使用
  - `80` / `443`：使用 Nginx 和 HTTPS 时使用

## 1. 在本机打包

在项目目录执行：

```powershell
cd E:\软件课程设计\after-sales-repair-system
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

打包成功后会生成：

```text
E:\软件课程设计\after-sales-repair-system\build\after-sales.war
```

把这个文件上传到服务器的 Tomcat：

```text
{Tomcat目录}/webapps/after-sales.war
```

Tomcat 启动后会自动解压为：

```text
{Tomcat目录}/webapps/after-sales/
```

## 2. 初始化 MySQL 数据库

在服务器 MySQL 中创建数据库：

```sql
CREATE DATABASE after_sales DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后依次执行项目中的脚本：

```text
db/schema-mysql.sql
db/seed-mysql.sql
```

注意：公网多人使用时不建议继续用本地 H2 文件数据库。H2 更适合课程设计单机演示；MySQL 更适合多人同时访问。

## 3. 配置服务器环境变量

启动 Tomcat 前，需要配置数据库和 DeepSeek：

### Windows Server 示例

```powershell
$env:APP_DB_URL='jdbc:mysql://服务器内网或本机地址:3306/after_sales?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:APP_DB_USER='after_sales_user'
$env:APP_DB_PASSWORD='你的数据库密码'
$env:APP_DB_DRIVER='com.mysql.cj.jdbc.Driver'
$env:DEEPSEEK_API_KEY='你的 DeepSeek API Key'
$env:DEEPSEEK_MODEL='deepseek-v4-flash'
```

### Linux Server 示例

```bash
export APP_DB_URL='jdbc:mysql://127.0.0.1:3306/after_sales?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
export APP_DB_USER='after_sales_user'
export APP_DB_PASSWORD='你的数据库密码'
export APP_DB_DRIVER='com.mysql.cj.jdbc.Driver'
export DEEPSEEK_API_KEY='你的 DeepSeek API Key'
export DEEPSEEK_MODEL='deepseek-v4-flash'
```

不要把 `DEEPSEEK_API_KEY` 写进 Java、JSP、JS 或公开仓库。

## 4. 启动 Tomcat

Windows：

```powershell
.\bin\startup.bat
```

Linux：

```bash
./bin/startup.sh
```

访问地址：

```text
http://服务器公网IP:8080/after-sales/client/login
http://服务器公网IP:8080/after-sales/warehouse/login
http://服务器公网IP:8080/after-sales/admin/login
```

如果配置了域名和反向代理，可以访问：

```text
https://你的域名/after-sales/client/login
```

## 5. 上线前必须修改

上线给别人使用前，至少完成这些安全设置：

- 修改默认管理员密码
- 修改默认仓库账号密码
- 不要使用测试手机号作为正式账号
- 使用 MySQL，不要用本机 H2 数据库
- 服务器安全组/防火墙只开放必要端口
- DeepSeek API Key 只放在服务器环境变量
- 如果给校外用户访问，建议配置 HTTPS

## 6. 临时给别人看：局域网访问

如果只是让同一 Wi-Fi / 同一局域网里的同学访问，可以不买服务器。

在你的电脑上运行项目后，查看本机局域网 IP：

```powershell
ipconfig
```

假设你的 IPv4 是：

```text
192.168.1.23
```

同一网络下的其他人可以访问：

```text
http://192.168.1.23:8080/after-sales/client/login
```

如果访问不了，需要检查 Windows 防火墙是否允许 Java/Tomcat 使用 8080 端口。

## 7. 临时公网演示

如果只是短时间演示，可以使用内网穿透工具，把你电脑上的 `8080` 暂时映射到公网地址。

这种方式方便，但不适合作为正式部署：

- 你的电脑必须一直开机
- 地址可能会变化
- 稳定性和安全性不如云服务器
- 不建议长期暴露管理端

正式让更多人使用，仍建议部署到公网服务器。
