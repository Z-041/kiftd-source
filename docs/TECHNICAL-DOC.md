# kiftd（青阳网络文件系统）v1.3.0 技术文档

> 文档版本：1.3.0 ｜ 归档日期：2026-08-04 ｜ 适用范围：本项目全部源代码、配置与部署

---

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 架构设计](#2-架构设计)
- [3. 功能模块说明](#3-功能模块说明)
- [4. 优化内容与实施步骤](#4-优化内容与实施步骤)
- [5. 关键技术点](#5-关键技术点)
- [6. API 文档](#6-api-文档)
- [7. 部署指南](#7-部署指南)
- [8. 使用说明](#8-使用说明)
- [9. 已知限制与后续建议](#9-已知限制与后续建议)
- [10. 附录：配置项速查表](#10-附录配置项速查表)

---

## 1. 项目概述

kiftd 是一款便捷、开源、功能完善的个人/团队网盘服务器系统（青阳网络文件系统）。它支持文件上传下载、文件夹管理、在线预览（图片/文本/视频）、打包下载、用户权限控制、扩展存储区、HTTPS、视频转码与缩略图生成等能力，同时提供图形界面（Swing）与命令行两种操作模式。

### 1.1 基本信息

| 项目 | 内容 |
| --- | --- |
| 名称 | Cloudflow（kiftd / 青阳网络文件系统） |
| 当前版本 | 1.3.0 |
| 语言/运行时 | Java 21（JDK 21） |
| 构建工具 | Maven（`mvn package`） |
| Web 框架 | Spring Boot 3.4.3 + Undertow |
| 持久层 | MyBatis Plus 3.5.9 + Spring JDBC + HikariCP |
| 数据库 | H2（内嵌，默认）/ MySQL 8（可选） |
| 打包产物 | `target/Cloudflow-1.3.0.jar`（依赖位于 `target/libs/`） |
| 入口类 | `kohgylw.kiftd.mc.MC` |

### 1.2 版本变更记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| 1.2.3-SNAPSHOT | 2026-07 | 历史版本（对应 README 旧描述） |
| 1.3.0 | 2026-08-04 | 系统性审查与修复：修复视频播放阻断、多类明确 Bug、安全加固、性能优化与质量体验改进（详见第 4 章） |

---

## 2. 架构设计

### 2.1 整体分层

项目采用「传统 MVC + 领域化分层」的混合结构，源码位于 `src/main/java/kohgylw/kiftd/` 下：

```
┌────────────────────────────────────────────────────────────┐
│  表现层  webContext/（HTML/CSS/JS）+ Swing UI（kiftd.ui）     │
├────────────────────────────────────────────────────────────┤
│  控制器层  newcore/controller + server.controller 的映射入口  │
├────────────────────────────────────────────────────────────┤
│  过滤器链  server/filter（安全头 → CSRF → IP → 登录）          │
├────────────────────────────────────────────────────────────┤
│  服务层    newcore/service + server/service（业务逻辑）        │
├────────────────────────────────────────────────────────────┤
│  基础设施   repository（仓储）、infrastructure（日志/加密/存储）│
├────────────────────────────────────────────────────────────┤
│  数据层    server/mapper + H2/MySQL + 文件块存储 filesystem/   │
└────────────────────────────────────────────────────────────┘
```

### 2.2 启动机制

- `mvn package` 生成 `target/Cloudflow-1.3.0.jar`（**非** Spring Boot fat jar），依赖通过 `maven-dependency-plugin` 拷贝至 `target/libs/`，Manifest 以 `libs/` 作为 classpath 前缀。
- 入口 `MC.main()`：无参数 → Swing 图形界面；`-console` → 控制台交互模式；`-start` → 直接启动服务器引擎。
- 配置目录 `conf/`：`server.properties`（服务器配置）、`account.properties`（账户与 IP 规则）、`rsa.key`（RSA 密钥对）。首次运行自动生成并创建默认管理员 `admin`（初始密码 `000000`，登录后应立即修改）。
- 文件存储目录 `filesystem/`：`fileblocks/`（文件块）、`filenodes/`（文件节点元数据 H2 库）、`temporaryfiles/`（临时转码/打包空间）。

### 2.3 请求处理链路（过滤器链）

| 顺序 | 过滤器 | 职责 |
| --- | --- | --- |
| @Order(0) | `SecurityHeadersFilter` | 为所有响应注入安全响应头（`X-Frame-Options`、`X-Content-Type-Options`、`Referrer-Policy`） |
| @Order(1) | `CsrfFilter` | Double-Submit Cookie 模式 CSRF 防护，写入 `XSRF-TOKEN` Cookie 并校验 `X-XSRF-TOKEN` 请求头 |
| @Order(1) | `IPFilter` | 基于 `account.properties` 中 `IP.allow`/`IP.banned` 的 IP 访问规则（可选 XFF 解析） |
| @Order(2) | `MastLoginFilter` | `mustLogin=N` 时强制登录校验 |
| — | `ProtectedURLFilter` | 受保护 URL（下载、管理接口等）的访问控制 |

> 注意：`IPFilter` 与 `CsrfFilter` 同为 `@Order(1)`，Spring Boot 对同顺序 Filter 按注册顺序执行；由于两者职责互不重叠，当前无实际冲突（详见第 9 章建议）。

### 2.4 异常处理

`GlobalExceptionHandler` 统一处理控制器异常，并对 `ResponseStatusException` 保留原始 HTTP 状态码（如 403），API 请求返回 JSON 错误体，避免被兜底逻辑降级为 500。

---

## 3. 功能模块说明

### 3.1 认证与账户模块（AuthService / ConfigurationManager）

- RSA 公钥下发 + 前端 RSA 加密提交登录/注册/改密数据，服务端私钥解密。
- 会话以 `ACCOUNT` 会话属性标识登录账户；支持验证码（`VCLevel`：STANDARD/SIMP/CLOSE）、失败次数聚焦（focusAccount）、修改密码、自助注册（需配置 `authSignup`/`groupSignup`）。
- 账户存储在 `account.properties`，**密码以明文存储**（纯文件控制，登录后不再自动加密；`PasswordUtil.verifyPassword` 同时兼容历史 `PBKDF2$` 哈希条目）。
- 权限字符：`c`新建文件夹、`u`上传、`d`删除、`r`重命名、`m`移动、`l`下载；`admin` 的 `privilege=S` 为超级管理员。
- 账户配置文件被修改时，通过 `WatchService` 守护线程热加载。

### 3.2 文件管理模块（FileService / FolderService / FileSystemManager）

- 文件块（分块）存储与文件节点（元数据）分离；支持主存储区与最多 255 个扩展存储区（`FS.extend.N`）。
- 文件夹树、文件夹约束（0=公开、1=同组可见、2=仅创建者）、删除留档（`recyclebin`）。
- 文件导入/导出、文件夹导入、移动/复制、重命名、批量删除、打包下载（ZIP，zt-zip）。

### 3.3 预览与媒体模块（MediaService / ResourceService）

- 图片预览与缩略图（`showCondensedPicture.do`，Thumbnailator）。
- 文本在线预览与编码探测（jchardet）、字体渲染。
- 视频播放：kplayer 前端 + 后端 `playVideo.ajax`，支持 FFmpeg（JAVE）在线转码（`VideoTranscodeUtil`）与 Range 流式分段（`RangeFileStreamWriter`）。

### 3.4 系统管理与监控模块（SystemInfoController / SystemHealthService）

- `GET /api/system/info|stats|health|metrics` 提供运行信息、内存/线程/负载等指标，**仅超级管理员可访问**（v1.3.0 新增鉴权）。

### 3.5 外链与分享模块（ExternalDownloadService / FileChainService）

- 分享下载：`getDownloadKey.ajax` 生成临时下载 Key，`downloadFileByKey/{fileName}` 免登录下载。
- 永久资源链：`chain/{fileName}`（`openFileChain=OPEN` 时启用）。

### 3.6 图形界面模块（kiftd.ui）

Swing 主界面（`ServerUIModule`）提供服务器启停、状态显示、配置窗口（端口/日志/验证码/文件系统路径等）与文件系统浏览视图。

---

## 4. 优化内容与实施步骤

本章按优先级（P0–P4）列出 v1.3.0 的全部改动，标注「问题 → 根因 → 方案」。

### 4.1 P0：修复视频播放功能阻断

| 文件 | 变更 |
| --- | --- |
| `webContext/quickview/video.html` | ① 将已不存在的 `js/jquery-3.3.1.min.js` 引用改为实际存在的 `js/jquery.min.js`；② 新增 CSRF 支持脚本：读取 `XSRF-TOKEN` Cookie，通过 `jQuery.ajaxSetup` 为所有 AJAX 请求自动附加 `X-XSRF-TOKEN` 请求头并启用 `withCredentials` |

- **问题**：视频页面无法播放，控制台报 jQuery 404 且 kplayer 的 POST 请求（ping/playVideo/getVideoTranscodeStatus）被 403 拦截。
- **根因**：旧版本引用了不存在的高版本 jQuery；且新增 CSRF 过滤器后，页面未携带 CSRF Token。
- **验证**：页面 200；jQuery 资源实际返回 87,533 字节；`playVideo.ajax` 携带有效 Token 后返回 200/ERROR。

### 4.2 P1：明确 Bug 修复

| 文件 | 变更 |
| --- | --- |
| `server/util/RSADecryptUtil.java` | `dncryption(...)` 增加 `context`/`privateKey` 空串校验，避免 `Base64.decode(null)` 抛 NPE（修复登录接口空密文导致的异常日志刷屏） |
| `newcore/service/impl/AuthServiceImpl.java` | 解密结果为 `null` 时直接返回失败，不再对 null 执行 `gson.fromJson` |
| `server/util/RangeFileStreamWriter.java` | Range 带结束偏移分支中，`raf.read(buf)` 返回 `-1`（源文件截断/提前结束）时 `break`，修复 `readLength` 不变导致的无限循环 |
| `server/util/VideoTranscodeUtil.java` | 重构为三段式并发模型（详见 4.4）；删除失效记录时校验 `get(fId) == vtt` 防误删新任务 |
| `server/pojo/VideoTranscodeThread.java` | 字段改为 `volatile`（`md5`/`progress`/`outputFileName`），消除转码子线程与轮询线程的数据竞争；转码异常时 `progress = "ERROR"`（原为悬置状态导致前端永久轮询） |
| `util/SizeFormatUtil.java` | `parseSizeWithUnit` 重写：支持空串/单字符/带 `B` 后缀（如 `2B`、`2KB`）正确解析，新增 `T`（TB）单位，修复原 `Long.parseLong(in.trim())` 对 `"2B"` 抛异常的缺陷 |
| `util/file_system_manager/FileSystemManager.java` | `getNativePath` 增加 `Set<String> visited` 防环检测，数据库父级出现环引用时 break，避免死循环 |
| `server/util/LogUtil.java` | `writeToLog` 声明为 `synchronized`，防止事件日志线程池与异常调用线程并发写入导致的日志内容错乱 |
| `newcore/service/impl/MediaServiceImpl.java` | 图片预览列表遍历时 `getFileFromBlocks(n)` 返回 null（文件块缺失）则 `continue` 跳过，避免 NPE |
| `newcore/service/impl/FolderServiceImpl.java` | 新建文件夹时父文件夹查询为 null 则返回 `"errorParameter"` |
| `server/util/FileBlockUtil.java` | `createZip` 权限校验增加 `fo`/`n`/`parent` 判空，避免 NPE |
| `server/filter/MastLoginFilter.java` | 所有 `session.getAttribute(...)` 前增加 `session != null` 判空 |

### 4.3 P2：安全加固

| 文件 | 变更 |
| --- | --- |
| `server/filter/SecurityHeadersFilter.java`（新增） | `@Order(0)` 为所有响应添加 `X-Frame-Options: SAMEORIGIN`、`X-Content-Type-Options: nosniff`、`Referrer-Policy: strict-origin-when-cross-origin` |
| `newcore/config/ConfigurationManager.java` | `IP.xff` 未配置时默认 `ipXFFAnalysis = false`（原默认为 true，存在伪造 `X-Forwarded-For` 头绕过 IP 规则的隐患）；`dbPwd` 支持 `server.properties` 的 `db.pwd` 覆盖（默认 `301537gY` 兼容历史数据）；删除死代码 `upgradePasswordHash(...)`；首次生成 account.properties 时提示默认管理员账号及"立即修改密码"警告 |
| `newcore/controller/SystemInfoController.java` | `/api/system/info|stats|health|metrics` 全部要求管理员会话（`ACCOUNT` 会话属性 + `isSuperAdmin`），否则抛 `ResponseStatusException(403)` |
| `server/filter/CsrfFilter.java` | ① `addCsrfCookie` 增加 `req.isSecure()` 参数 → `cookie.setSecure(secure)` 并设置 `SameSite=Lax`；② CSRF 拒绝时弃用 `resp.sendError(403)`（会触发 ERROR dispatch 二次进入过滤器链，被资源处理器接管变为 500），改为 `setStatus(403)` + 手写 JSON 响应体 |
| `newcore/controller/GlobalExceptionHandler.java` | 新增 `ResponseStatusException` 处理分支，保留其 HTTP 状态码（如 403），API 请求输出 JSON 错误体 |
| `newcore/config/WebMvcConfig.java` | ① Session Cookie 增加 `setSecure(cm.isHttpsEnabled())`；② 注册 `CookieSameSiteSupplier.ofLax()` Bean（import 修正为 `org.springframework.boot.web.servlet.server.CookieSameSiteSupplier`，适配 Spring Boot 3.4） |

### 4.4 P3：性能优化

| 文件 | 变更 |
| --- | --- |
| `server/mapper/NodeMapper.java` / `FolderMapper.java` | 新增 `@Select("SELECT COUNT(*) ...")` 聚合统计：`countByParentFolderId(...)` / `countByParentId(...)` |
| `newcore/repository/impl/FileNodeRepositoryImpl.java` / `FolderRepositoryImpl.java` | `countByParentFolderId` / `countByParentId` 改用 COUNT 聚合查询，替代原"SELECT 全列表再 `.size()`"的逐行读取 |
| `server/util/VideoTranscodeUtil.java` | 转码 MD5 校验移出全局锁：锁内仅快速返回非完成进度并短暂取引用，**锁外**校验 FIN 的 MD5/输出文件，再回到锁内做并发检查后启动新转码，显著降低长耗时 MD5 计算对转码队列的阻塞 |
| `server/util/FileBlockUtil.java` | `createZip` 增加 `Map<String, List<String>> ancestorCache`，用 `computeIfAbsent(folderId, fu::getAllFoldersId)` 缓存祖先链查询，避免逐项重复查库 |

### 4.5 P4：质量与体验

| 文件 | 变更 |
| --- | --- |
| `mc/MC.java`、`mc/UIRunner.java`、`printer/Printer.java` | 删除 `[STARTUP]` 系列调试输出残留 |
| `ui/module/ServerUIModule.java` | ① 无系统托盘时 `setDefaultCloseOperation(EXIT_ON_CLOSE)`（原 `HIDE_ON_CLOSE` 导致进程无法退出）；② `changedUpdate` 去除 `selectAll` + `requestFocus` 焦点抢占；③ 输出区更新统一 `SwingUtilities.invokeLater` 上 EDT |
| `newcore/controller/SystemInfoController.java`、`mc/MC.java` | 版本号由硬编码 `"1.2.3-SNAPSHOT"` 统一为 `"1.3.0"` |
| `newcore/infrastructure/logging/ApiPerformanceFilter.java` | 补齐缺失常量 `MAX_ENDPOINT_ENTRIES = 100`（修复编译错误） |

---

## 5. 关键技术点

### 5.1 Double-Submit Cookie CSRF 防护

- 服务端在响应中下发 `XSRF-TOKEN` Cookie（HttpOnly 关闭，供 JS 读取；`Secure` 随 HTTPS 启用；`SameSite=Lax`）。
- 前端（`video.html` 等页面）通过 `jQuery.ajaxSetup` 自动读取该 Cookie 并附加 `X-XSRF-TOKEN` 请求头。
- 过滤器对存在修改语义的请求校验请求头与 Cookie 的一致性。

### 5.2 ERROR dispatch 陷阱

- 在 Filter/Controller 中调用 `resp.sendError(403)` 会触发 Spring Boot 错误转发（ERROR dispatch），请求**二次进入过滤器链**，被 `/**` 的 `ResourceHttpRequestHandler` 接管后产生 `HttpRequestMethodNotSupportedException`，最终 403 被降级为 500。
- **处理原则**：需要返回错误状态码时，优先 `response.setStatus(code)` + 直接写出响应体，或抛出 `ResponseStatusException` 由 `GlobalExceptionHandler` 统一处理。

### 5.3 视频转码三段式并发模型

1. **锁内快速路径**：`progress` 为非完成状态时立即返回，不做任何 IO。
2. **锁外校验**（仅在短暂锁内取引用）：对 FIN 状态的转码任务计算 MD5、校验输出文件存在性。
3. **锁内并发检查后启动新转码**：确认无并发任务后才创建新转码线程。

配合 `VideoTranscodeThread` 的 `volatile` 字段，既消除了数据竞争，又将 MD5 计算等长耗时操作移出全局锁，避免阻塞其他文件的转码请求。

### 5.4 Spring Boot 3.4 适配注意点

- `CookieSameSiteSupplier` 已从 `org.springframework.boot.web.server` 移至 `org.springframework.boot.web.servlet.server`，导入包需随之调整。
- Undertow 作为 Web 服务器（排除默认 Tomcat）。

### 5.5 COUNT 聚合查询

文件夹内容计数由「全量 SELECT + 内存 `.size()`」改为数据库端 `SELECT COUNT(*)`，对海量文件的目录计数接口是显著的数量级优化。

---

## 6. API 文档

基础前缀：`homeController`、`resourceController`、`externalLinksController`、`api/system`。除标注外，以下接口均为 `.ajax` 后缀、返回 `text/html; charset=utf-8` 文本（JSON 字符串或状态码）。

### 6.1 认证接口 `homeController`

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET/POST | `/getPublicKey.ajax` | — | 获取 RSA 公钥与时间戳（JSON） |
| POST | `/doLogin.ajax` | `encrypted`(RSA密文)、`vercode`(可选) | 登录，返回状态码（`permitlogin`/`accountpwderror`/`needsubmitvercode`/`accountnotfound`/`error`） |
| GET | `/getNewVerCode.do` | — | 获取验证码图片 |
| POST | `/doLogout.ajax` | — | 注销 |
| POST | `/doChangePassword.ajax` | `encrypted`(RSA密文) | 修改密码 |
| GET/POST | `/ping.ajax` | — | 心跳，登录态返回 `pong` |
| GET | `/askForAllowSignUpOrNot.ajax` | — | 是否开放注册（`true`/`false`） |
| POST | `/doSigUp.ajax` | `encrypted`(RSA密文)、`vercode` | 注册 |

### 6.2 文件与文件夹接口 `homeController`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/douploadFile.ajax` | 上传文件（multipart） |
| POST | `/checkUploadFile.ajax` | 校验上传文件（大小/权限） |
| POST | `/checkImportFolder.ajax` | 校验文件夹导入 |
| POST | `/doImportFolder.ajax` | 导入文件夹（multipart） |
| POST | `/createNewFolderByName.ajax` / `/newFolder.ajax` | 新建文件夹 |
| POST | `/deleteFolder.ajax` / `/deleteFolderByName.ajax` | 删除文件夹 |
| POST | `/renameFolder.ajax` | 重命名文件夹 |
| GET | `/getFolderView.ajax` | 获取目录视图（参数 `fid`） |
| GET | `/getRemainingFolderView.ajax` | 获取其余目录（移动/复制目标选择） |
| GET | `/countFolderContent.ajax` | 目录内容计数 |
| GET | `/downloadFile.do` | 下载文件（支持 Range） |
| POST | `/deleteFile.ajax` / `/deleteCheckedFiles.ajax` | 删除文件/批量删除 |
| POST | `/renameFile.ajax` | 重命名文件 |
| GET | `/getPackTime.ajax` | 获取打包耗时 |
| POST | `/downloadCheckedFiles.ajax` | 校验批量下载 |
| GET | `/downloadCheckedFilesZip.do` | 打包下载 ZIP |
| POST | `/confirmMoveFiles.ajax` / `/moveCheckedFiles.ajax` | 移动文件（批量） |
| GET | `/sreachInCompletePath.ajax` | 全路径搜索 |

### 6.3 预览与媒体接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `homeController/playVideo.ajax` | 视频播放信息（含转码状态） |
| GET | `homeController/getPrePicture.ajax` | 图片预览列表（JSON） |
| GET | `homeController/showCondensedPicture.do` | 缩略图输出 |
| GET | `resourceController/getResource/{fileId}` | 资源流（文件下载/播放） |
| GET | `resourceController/getLRContext/{fileId}` | 文本文件内容（UTF-8 探测） |
| GET | `resourceController/getVideoTranscodeStatus.ajax` | 视频转码状态轮询 |
| GET | `resourceController/getNoticeMD5.ajax` | 公告 MD5 |
| GET | `resourceController/getNoticeContext.do` | 公告内容 |

### 6.4 外链与分享接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `externalLinksController/getDownloadKey.ajax` | 生成分享下载 Key |
| GET | `externalLinksController/downloadFileByKey/{fileName}` | 按 Key 免登录下载 |
| GET | `externalLinksController/chain/{fileName}` | 永久资源链下载（需 `openFileChain=OPEN`） |

### 6.5 系统管理接口 `api/system`（v1.3.0 起需超级管理员会话）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/system/info` | 应用名/版本/运行时长/端口/HTTPS/文件系统路径 |
| GET | `/api/system/stats` | 堆与非堆内存/OS/线程数/系统负载 |
| GET | `/api/system/health` | 健康状态（文件系统/数据库等） |
| GET | `/api/system/metrics` | 内存/线程等运行时指标 |

> 安全说明：以上 4 个接口在 v1.3.0 前可匿名访问，存在敏感信息（文件系统路径、内存、磁盘）泄露风险；现要求 `ACCOUNT` 会话属性且账户 `privilege=S`，否则返回 `403`。

### 6.6 其余接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `homeController/getServerOS.ajax` | 服务器操作系统名 |
| GET | `homeController/getFileChainKey.ajax` | 生成永久资源链 Key |
| GET | `/` | 重定向至 `home.html` |

---

## 7. 部署指南

### 7.1 环境要求

- JDK 21（运行时与编译均需 21+）
- Maven 3.8+（构建）
- 可选：MySQL 8（若使用自定义数据库）；FFmpeg 可执行文件（视频在线转码，JAVE 自动探测）

### 7.2 构建

```powershell
# 全量构建（含测试与 JaCoCo 覆盖率门禁：整体行覆盖率≥25%，server.util 包≥30%）
mvn clean package

# 跳过测试快速打包
mvn clean package -DskipTests
```

产物：`target/Cloudflow-1.3.0.jar` + `target/libs/`（第三方依赖）。`mvn dependency:copy-dependencies` 已在 package 阶段自动执行。

### 7.3 启动

```powershell
# 图形界面模式（无参数）
java -jar target\Cloudflow-1.3.0.jar

# 控制台交互模式
java -jar target\Cloudflow-1.3.0.jar -console

# 直接启动服务器引擎（推荐服务器环境）
java -jar target\Cloudflow-1.3.0.jar -start
```

> 部署时需将 `Cloudflow-1.3.0.jar` 与 `libs/` 保持同目录；首次启动会生成 `conf/`、`filesystem/`、`logs/` 目录。

### 7.4 命令行工具命令

| 命令 | 说明 |
| --- | --- |
| `-import <源> <目标> [-C覆盖|-B保留]` | 导入文件/文件夹到文件系统 |
| `-export <源> <目标> [-C覆盖|-B保留]` | 导出文件系统内容到本地 |
| `-transfer <扩展区编号> <目标路径>` | 移出扩展存储区数据 |
| `-account list/add/del/chpwd/chauth/info` | 账户管理 |
| `-resetpwd <账户> <新密码>` | 重置密码 |
| `-v / -version`、`-h / -help`、`-about` | 版本/帮助/关于 |

### 7.5 冒烟验证

```powershell
# 1) 健康检查（需登录后携带会话；匿名应返回 403）
Invoke-WebRequest http://localhost:8080/ -UseBasicParsing
Invoke-WebRequest http://localhost:8080/api/system/health -UseBasicParsing   # 期望 403

# 2) 首页安全响应头
(Invoke-WebRequest http://localhost:8080/home.html -UseBasicParsing).Headers

# 3) CSRF 验证
#    无/错误 Token 的 POST → 403；携带 Cookie XSRF-TOKEN + 头 X-XSRF-TOKEN → 正常进入业务
```

### 7.6 HTTPS 启用

- 在主目录放置 `https.p12`（PKCS12）或 `https.jks`（JKS）证书。
- `server.properties` 配置：

```properties
https.enable=true
https.port=443
https.keypass=你的证书密码
```

---

## 8. 使用说明

### 8.1 首次使用

1. 启动服务器，访问 `http://localhost:8080/`。
2. 使用默认管理员账户 `admin` / 密码 `000000` 登录。
3. **立即修改默认密码**（首页 → 修改密码）。

### 8.2 界面功能

- **文件管理**：新建/重命名/删除/移动/复制文件夹与文件，批量操作，拖拽上传。
- **在线预览**：图片（缩略图/大图）、文本、视频（kplayer，支持转码后播放）。
- **下载**：单文件下载（支持断点续传 Range）、打包下载 ZIP。
- **分享**：生成分享链接（临时 Key）或永久资源链。
- **账户**：注册（若开放）、修改密码、退出。

### 8.3 配置管理

- 图形界面「设置」窗口可修改端口、日志等级、验证码等级、文件系统路径、扩展存储区、修改密码开关、永久资源链开关等（对应 `conf/server.properties`）。
- 账户权限与 IP 规则手动编辑 `conf/account.properties` 后会自动热加载。

### 8.4 默认账户权限字符

| 字符 | 权限 |
| --- | --- |
| c | 新建文件夹 |
| u | 上传文件 |
| d | 删除文件/文件夹 |
| r | 重命名 |
| m | 移动 |
| l | 下载 |

### 8.5 用户忘记密码怎么办（纯文件控制）

账户体系**完全由 `conf/account.properties` 一个文件控制**，与 H2/MySQL 数据库无关（数据库仅存放文件节点元数据）。用户忘记密码时，管理员只需操作该文件即可恢复，无需数据库、无需重启：

**方式一：直接编辑文件（最直观，无需任何命令）**

用文本编辑器打开 `conf/account.properties`，将忘记密码账户的 `xxx.pwd=` 值直接改为明文新密码并保存：

```properties
user1.pwd=123456
```

系统内建"明文兼容"机制（`PasswordUtil.verifyPassword` 对非 `PBKDF2$` 前缀的值按明文直接比较），该用户即可用 `123456` 登录；**登录后密码保持明文，不会再被自动加密**（该行为在 v1.3.0 已移除）。服务器运行期间修改文件也会被热加载生效。

**方式二：命令行重置（同样只写该文件）**

```powershell
java -jar Cloudflow-1.3.0.jar -resetpwd <账户名> <新密码>
java -jar Cloudflow-1.3.0.jar -account chpwd <账户名> <新密码>
java -jar Cloudflow-1.3.0.jar -account list      # 查看账户列表
```

- 重置后告知用户使用新密码登录，并建议登录后自行修改密码。
- 密码以**明文**存储于 `account.properties`，新建账户、修改密码、重置密码写入的均为明文，登录后保持明文不变。
- 兼容性：历史 `PBKDF2$` 哈希条目仍可正常登录（不会自动改写）。如需将旧条目转明文，直接编辑文件替换为明文即可。
- 安全提醒：明文存储意味着任何能读取配置文件的人都能直接看到全部账户（含管理员）的密码，请务必做好 `conf/` 目录的访问控制。

---

## 9. 已知限制与后续建议

1. **过滤器顺序隐患**：`IPFilter` 与 `CsrfFilter` 同声明 `@Order(1)`，当前无实际冲突，但建议为 `IPFilter` 显式指定唯一顺序（如 `@Order(3)`）以避免后续扩展时的隐性依赖。
2. **README 同步**：`README.md` 已在本轮更新为 v1.3.0/JDK 21（历史说明请以本文档为准）；其中"程序基本结构说明"描述的 `mybatisResource/`、`fonts/`（源码级）目录已不存在，若重新整理 README 可一并修正。
3. **视频转码**：依赖 JAVE 自动探测 FFmpeg，若目标机未安装 FFmpeg，视频转码功能将不可用（不影响普通播放已转码格式）。
4. **CSRF 与多端页面**：本项目采用手工注入方式为 video 页启用 CSRF 头；新增自定义页面时需同步注入，避免 403。
5. **`db.pwd` 默认值**：为兼容历史 H2 数据保留默认口令 `301537gY`，生产环境建议在 `server.properties` 配置 `db.pwd` 覆盖。
6. **JaCoCo 门禁**：整体行覆盖率 ≥ 25%、`kohgylw.kiftd.server.util` 包 ≥ 30%；新增核心逻辑时建议同步补充单元测试。

---

## 10. 附录：配置项速查表

### 10.1 server.properties

| 键 | 取值 | 默认 | 说明 |
| --- | --- | --- | --- |
| `port` | 1–65535 | 8080 | HTTP 端口 |
| `mustLogin` | O/N | O | 是否强制登录 |
| `log` | N/R/E | E | 日志等级（无/异常/事件） |
| `VC.level` | STANDARD/SIMP/CLOSE | STANDARD | 验证码等级 |
| `FS.path` | 路径或 DEFAULT | DEFAULT | 主文件系统路径 |
| `FS.extend.N` | 路径 | — | 扩展存储区（N=1..255） |
| `buff.size` | 正整数 | 1048576 | IO 缓冲字节数 |
| `password.change` | Y/N | N | 是否允许修改密码 |
| `openFileChain` | OPEN/CLOSE | CLOSE | 永久资源链 |
| `download.zip` | enable/disable | enable | 打包下载 |
| `video.ffmpeg` | enable/disable | enable | 视频在线转码 |
| `IP.xff` | enable/disable | **disable（v1.3.0 起）** | XFF 解析（置于可信反向代理后时再开启） |
| `mysql.enable` | true/false | false | 启用 MySQL |
| `mysql.url` | host/dbname | 127.0.0.1/kift | MySQL 地址 |
| `mysql.user` / `mysql.password` | 字符串 | root / 空 | MySQL 账号 |
| `mysql.timezone` | 时区 | — | MySQL serverTimezone |
| `db.pwd` | 字符串 | 301537gY | H2 数据库口令（v1.3.0 新增覆盖项） |
| `https.enable` / `https.port` / `https.keypass` | — | false/443/空 | HTTPS（需主目录证书） |
| `https.redirect.host` | 域名 | — | HTTPS 跳转域名 |
| `cors.allowedOrigins` | 逗号分隔 | 空 | 跨域白名单 |
| `recyclebin` | 路径 | — | 删除留档目录 |

### 10.2 account.properties

| 键 | 说明 |
| --- | --- |
| `<账户>.pwd` | 密码（明文存储，兼容历史 `PBKDF2$` 哈希） |
| `<账户>.auth` | 账户级权限字符 |
| `<账户>.auth.<文件夹ID>` | 指定文件夹权限 |
| `<账户>.group` | 账户分组 |
| `<账户>.privilege` | `S` 表示超级管理员 |
| `<账户>.maxSize` / `<账户>.maxRate` | 账户级上传大小/下载限速 |
| `defaultMaxSize` / `defaultMaxRate` | 全局默认 |
| `authOverall` | 匿名（未登录）全局权限 |
| `authSignup` / `groupSignup` | 开放注册时新用户权限/分组 |
| `IP.allow` / `IP.banned` | 分号分隔的 IP 名单（配置即启用规则） |
| `import.account` | 系统导入账户（默认 SYS_IN） |

---

*文档维护：kiftd 项目组。任何代码变更后请同步更新本文档并归档至 `docs/`。*
