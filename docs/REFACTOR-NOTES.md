# kiftd（青阳网络文件系统）重构说明文档

> 文档版本：1.0 ｜ 归档日期：2026-08-05 ｜ 适用范围：本项目 v1.3.0 的架构梳理与重构工作

---

## 目录

- [1. 重构目标与范围](#1-重构目标与范围)
- [2. 架构调整前后对比](#2-架构调整前后对比)
- [3. 类导入规范说明](#3-类导入规范说明)
- [4. 工作台优化内容](#4-工作台优化内容)
- [5. 重构过程中发现并修复的问题](#5-重构过程中发现并修复的问题)
- [6. 验证与回归](#6-验证与回归)
- [7. 已知限制与后续建议](#7-已知限制与后续建议)

---

## 1. 重构目标与范围

本次重构围绕以下四个目标展开：

1. **分析现有架构的问题点和不合理之处**——对依赖方向、包归属、导入方式进行全面梳理；
2. **设计并实施合理的项目目录结构**——消除 server→newcore 反向依赖，形成单向分层；
3. **统一类的导入路径和方式**——消除循环依赖与冗余导入（通配符、无用导入清零）；
4. **优化整个开发工作台的配置和使用体验**——编码规范、忽略规则、测试组织、构建质量门禁。

同时，本次重构修复了在审查与全量验证中发现的所有遗留问题（详见第 5 章）。

---

## 2. 架构调整前后对比

### 2.1 重构前的问题点

| 问题 | 说明 | 影响 |
| --- | --- | --- |
| server→newcore 反向依赖 | `ConfigurationManager`（配置管理核心类）归属 `newcore.config` 包，却被 `server` 层多达 14 处引用 | 破坏分层方向，newcore 无法独立演进；IDE 依赖分析呈环状 |
| 包归属错位 | 全局配置管理属于「服务端基础设施」，却放在 `newcore.config` 下 | 语义不清，导致跨层引用 |
| 导入方式混乱 | 全项目存在 56 处通配符导入（`java.util.*`、`javax.swing.*` 等）、21 处无用导入 | 降低可读性，隐藏真实依赖，增加编译耦合 |
| 测试包错位 | `ConfigurationManagerAccountTest` 位于 `newcore.config`，随主类迁移后才归位 | 测试与源码包结构不一致 |
| 冗余死分支 | `ContentTypeMap` 中 13 个 case 分支显式返回默认值 `application/octet-stream` | 死代码，误导阅读者 |
| 安全豁免过宽 | `CsrfFilter` 对 `doLogin.ajax`/`doSigUp.ajax` 无条件放行 | 存在「登录 CSRF」风险（攻击者可借受害者 Cookie 跨站提交登录/注册表单） |

### 2.2 重构后

**依赖方向（单向分层）：**

```
server（基础设施：util / filter / mapper / model / listener）
    │  仅单向依赖
    ▼
newcore（领域层：service / controller / repository / infrastructure）
    │
    ▼
Spring Boot 运行时
```

- `ConfigurationManager` 由 `newcore.config` 迁移至 `server.util`，`newcore→server` 反向依赖 14 处全部清零；
- 三个配置类 `DataSourceConfig` / `UndertowServerConfig` / `WebMvcConfig` 随迁移补充显式 import；
- `server` 包与 `newcore` 包之间不再存在反向引用，分层方向恢复单向。

**过滤器链（保持并确认）：**

```
SecurityHeadersFilter → IPFilter / CsrfFilter(@Order(1)) → MastLoginFilter → ProtectedURLFilter
```

- `CsrfFilter` 采用 Double-Submit Cookie 模式：响应下发 `XSRF-TOKEN` Cookie，非安全方法要求 `X-XSRF-TOKEN` 请求头一致；
- 重构后豁免范围收紧为仅两个只读预登录端点（详见 5.3）。

---

## 3. 类导入规范说明

### 3.1 导入规范（项目编码规范）

1. **禁止通配符导入**：`import java.util.*;` 等形式一律展开为显式导入；
2. **无无用导入**：未使用的 import 一律删除，编译期不产生 unused 警告；
3. **同包免导入**：同包类不再冗余 `import` 自身包路径（如 `server.util` 包内互相引用）；
4. **分组顺序**：`static` 导入 → `java/jakarta` → 第三方 → 项目内部（`kohgylw.*`）；
5. **缩进风格**：沿用项目原有 Tab（4 列）缩进，不因整理导入而改动代码风格（由 `.editorconfig` 固化）。

### 3.2 执行结果

| 项目 | 数量 |
| --- | --- |
| 展开的通配符导入 | 56 处 |
| 删除的无用导入 | 21 处 |
| 调整/新增的显式导入（含 3 个配置类补 import） | 56 个文件 |
| 迁移的类 | `ConfigurationManager`、`ConfigurationManagerAccountTest` |
| 清理的 server→newcore 反向依赖 | 14 处 |

**典型示例**：

- `FileBlockUtil`：`org.springframework.stereotype.*`、`server.mapper.*`、`jakarta.annotation.*`、`org.springframework.web.multipart.*`、`java.io.*`、`java.util.*`、`org.zeroturnaround.zip.*` 全部展开为具体类；
- `FolderMapper` / `NodeMapper`：`server.model.*` → `model.Folder` / `model.Node`；`org.apache.ibatis.annotations.*` → `@Param` / `@Select`；
- UI 类（`ServerUIModule` / `SettingWindow`）：`javax.swing.*` 等展开，并在 `mvn clean compile` 全量编译下补齐缺失导入（`BorderFactory`、`BoxLayout`、`WindowConstants` 等）。

> 注：增量编译可能掩盖缺失导入，因此所有导入调整均以 `mvn clean compile`（全量重编译）作为验收标准。

---

## 4. 工作台优化内容

### 4.1 新增 `.editorconfig`（编码规范固化）

- 统一 UTF-8 编码、CRLF 行尾、末尾换行；
- Java 使用 Tab 缩进（4 列），XML/properties/JS/CSS/HTML 使用 2 空格；
- `*.min.js` 豁免格式校验（压缩文件不做行尾处理）。

### 4.2 修正 `.gitignore`

- 删除 `src/main/resources/` 忽略规则——该规则误伤 `application.properties`（Spring Boot 核心配置），修复后配置可被纳入版本管理。

### 4.3 测试组织

- 测试类随源码包结构归位：`ConfigurationManagerAccountTest` 从 `newcore.config` 迁移至 `server.util`；
- 新增 `CircuitBreakerTest`、`RetryUtilTest`，覆盖此前零覆盖的熔断器与重试工具类；
- 新增 `ContentTypeMapTest#testAllDeclaredSuffixesResolveToSpecificContentType` 映射表一致性校验（见 5.2）。

### 4.4 构建质量门禁（维持并达标）

- `mvn clean package` 全量通过（含单元测试 + JaCoCo 覆盖率检查）：
  - 全项目行覆盖率 ≥ 0.25；
  - `server.util` 包行覆盖率 ≥ 0.30（此前因 `ConfigurationManager` 迁入而跌破 0.30，本次通过新增测试恢复到 0.30 以上）；
- 测试规模：890 个用例全部通过，0 失败 0 错误。

---

## 5. 重构过程中发现并修复的问题

### 5.1 测试断言未同步（3 处）

安全改造（注册白名单返回码、转码接口鉴权）落地后，相关测试未同步更新，`mvn clean package` 全量验证暴露：

| 测试 | 问题 | 修复 |
| --- | --- | --- |
| `AuthServiceImplTest#testSignUp_IllegalAccount` | 期望 `illegalaccount`，实现实际返回 `invalidaccount` | 断言同步为 `invalidaccount`（前端 signup.js 已同时兼容两种返回码） |
| `ResourceServiceImplTest#testGetVideoTranscodeStatus_Success` | 缺登录态/鉴权 mock，返回 `ERROR` 而非 `50%` | 补 Node/ACCOUNT/authorized/accessFolder mock |
| `ResourceServiceImplTest#testGetVideoTranscodeStatus_Exception` | 缺鉴权 mock，未触发异常路径 | 同上，补全 mock 后验证 `logUtil.writeException` 被调用 |

### 5.2 ContentTypeMap 冗余分支清理（13 处）

新增的「映射表一致性」测试发现 13 个 case 分支显式返回 `application/octet-stream`，与 `default` 行为完全一致，属冗余死分支：

```
.bin  .bpk  .deploy  .dist  .distz  .dms  .dump  .elc  .exe  .lrf  .mar  .pkg  .so
```

删除后运行时行为零变化（走 default），源码精简 13 个分支；一致性测试确保未来新增后缀不会误映射为默认类型。

### 5.3 CSRF 登录豁免收紧

- **问题**：`CsrfFilter` 对 `doLogin.ajax` / `doSigUp.ajax` 无条件放行。但登录/注册页面加载时本过滤器已下发 `XSRF-TOKEN` Cookie，前端 `jQuery.ajaxSetup` 钩子（login.html / signup.html / home.html）会自动附加 `X-XSRF-TOKEN` 请求头，因此豁免并不必要，且引入「登录 CSRF」风险。
- **修复**：豁免范围收紧为仅两个只读预登录端点（`getPublicKey.ajax`、`askForAllowSignUpOrNot.ajax`）。
- **验证**：
  - 无 `X-XSRF-TOKEN` 头的 `doLogin.ajax` POST → **403** 拦截；
  - 带正确 `X-XSRF-TOKEN` 头的登录请求 → 正常放行（200）；
  - 真实浏览器端到端登录成功（见第 6 章）。

---

## 6. 验证与回归

### 6.1 构建验证

```
mvn clean package        # 编译 + 890 个测试 + JaCoCo 覆盖率检查 → BUILD SUCCESS
```

### 6.2 运行时冒烟（真实浏览器端到端）

启动 `target/Cloudflow-1.3.0.jar`（端口 8080），使用浏览器自动化完成全链路：

| 步骤 | 结果 |
| --- | --- |
| 页面导航（首页自动跳转 home.html） | ✅ |
| RSA 加密 + CSRF 双提交令牌登录（admin/admin123） | ✅ |
| 文件列表加载 | ✅ |
| 新建文件夹 `smoke-test-20260805` | ✅ |
| 上传文件 `smoke-upload.txt`（98B） | ✅ |
| 下载文件（响应体与本地源文件逐字节一致） | ✅ |
| 注销登录 | ✅ |

### 6.3 HTTP 级验证

- GET `/` 下发 `XSRF-TOKEN` Cookie ✅
- POST 无 CSRF 头 → 403 ✅（CSRF 收紧生效）
- POST 带正确 CSRF 头 → 放行 ✅
- `getPublicKey.ajax` 返回 RSA 公钥 JSON ✅

---

## 7. 已知限制与后续建议

| 项目 | 现状 | 建议 |
| --- | --- | --- |
| 配置键/协议码常量提取 | `ConfigurationManager` 中的配置键（如 `port`、`VC.level`）与协议返回码（如 `"permitlogin"`）仍以字符串字面量散落使用 | 后续可提取为 `Constants` 常量类，避免拼写漂移 |
| 双套加密体系 | `server.util.AESCipher`（文件块加密）与 `newcore.infrastructure.crypto.CryptoServiceImpl`（AES-GCM，链式密钥）并存且均为活代码 | 二者服务不同场景（文件块 vs 密钥链），当前不合并；如需统一可做专项重构 |
| UI 与 web 双入口 | Swing UI（`kiftd.ui`）与浏览器前端（`webContext`）并存 | 长期可将配置/监控能力收敛至 web 端，UI 仅保留引导与启动控制 |
| `RangeFileStreamWriter` 覆盖 | 断点续传核心工具暂无单元测试（依赖 Servlet 容器） | 建议补充 `writeRangeFileStream`/`writeRangeFileHead` 的 MockMvc 级测试 |
