# 技术债治理迭代文档（Tech Debt Iteration Report）

> 迭代周期：2026-08 · 项目：kiftd-source（Cloudflow 1.3.0，Java 21 + Spring Boot 3.4 + Undertow + MyBatis Plus + H2/MySQL）
>
> 本迭代遵循**闭环方法论**：量化评估 → 优先级计划 → 方案设计 → 实施 → 严格回归 → 监控机制，
> 并配套完整迭代文档（本文档）。所有已修复债务的验证方式已同步登记至 `TECH-DEBT-REGISTER.md`。

---

## 1. 概述与方法论

### 1.1 治理范围

技术债不限于架构与安全，本迭代覆盖 **8 个维度**：

| 维度 | 说明 |
|---|---|
| 安全（SEC） | 凭据存储、输入校验、资源上限兜底 |
| 性能（PERF） | N+1 查询、全表加载、无分页、锁粒度 |
| 代码质量（CODE） | 魔数、直出输出、大方法、重复代码 |
| 架构（ARCH） | 分层依赖、双协议通道、循环依赖 |
| 可维护性（MAIN） | 常量集中、异常处理、命名与注释 |
| 文档（DOC） | 设计文档、接口契约、配置说明 |
| 测试（TEST） | 单元/集成覆盖率、关键路径覆盖 |
| 基础设施（INFRA） | 构建时长、CI 稳定性、测试基建缺陷 |

### 1.2 闭环流程与防"死胡同"策略

1. **量化评估**：建立指标体系并打分（见第 2、3 章）；
2. **优先级计划**：按「债务分 ÷ 修复成本」排序（见第 4 章）；
3. **方案设计**：每项债务给出具体方案，先静态分析确认零行为变更；
4. **实施 + 回归**：实施后用全量门禁验证；对回归嫌疑采用 **Git 基线对照法**
   （`git stash` 保存改动 → 基线上复跑同一命令 → 对照失败模式归属 → 恢复改动），
   从根本上排除"改动引入回归"的误判；
5. **监控机制**：台账 + 静态检查脚本 + 准入规则（见第 7 章）；
6. **迭代文档**：本文档。

> **沙箱/预演验证实践**：集成测试基建缺陷（TEST-INFRA-001）无法靠静态分析确诊时，
> 采用基线对照法验证——基线上同样失败（Folder 2F/3E、File 2F/2E），证明为既有基建缺陷而非本次改动引入；
> 修复后 80 用例全绿，构成完整闭环。

---

## 2. 量化评估指标体系（d1）

### 2.1 单笔债务评分模型

```
债务分 D = 严重度 S(1-5) × 影响面 I(1-5) × 复发概率 P(1-5)     （D ∈ [1,125]）
优先级   = D ÷ 修复成本 C(1-3)
```

| 评分项 | 1 | 3 | 5 |
|---|---|---|---|
| 严重度 S | 无功能影响 | 偶发功能异常/轻微性能 | 数据泄露、数据丢失、核心功能不可用 |
| 影响面 I | 单文件 | 单模块 | 跨模块/全局 |
| 复发概率 P | 一次性 | 特定场景复发 | 持续/高概率新增 |
| 修复成本 C | 小（≤半天） | 中（1 天左右） | 大（跨层重构） |

### 2.2 项目级量化指标

| 指标 | 度量方式 | 当前基线（2026-08） | 门禁阈值 |
|---|---|---|---|
| 单元测试数 | mvn surefire | 897（通过） | ≥890，新增代码须带测试 |
| 集成测试数 | mvn failsafe | 80（通过） | 全绿 |
| 整体行覆盖率 | JaCoCo LINE | ≥25%（达标） | ≥25% |
| server.util 包行覆盖率 | JaCoCo PACKAGE | ≥30%（达标） | ≥30% |
| 构建/全量验证时长 | mvn clean verify | ~60s（含 977 用例） | 无硬性上限 |
| TODO/FIXME/HACK 注释 | 静态扫描 | 2 | ≤50 |
| 核心层 System.out 直出 | 静态扫描（newcore+server） | 0 | =0 |
| printStackTrace 直打 | 静态扫描 | 0 | =0 |
| AJAX 协议码魔数 | 静态扫描 | 0 | =0 |
| 集成测试失败用例 | failsafe 报告 | 0（修复前 9） | =0 |

---

## 3. 基线评估结果（d2）

按第 2 章模型，对已知技术债逐项打分（完整台账见 `TECH-DEBT-REGISTER.md`）：

| 债务ID | 维度 | 描述 | S | I | P | D | C | D/C | 优先级 |
|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | 安全 | 密码明文落盘 | 5 | 5 | 5 | 125 | 2 | 62.5 | **P0** |
| SEC-002 | 安全 | 上传大小无兜底 | 4 | 4 | 4 | 64 | 1 | 64 | **P0** |
| TEST-INFRA-001 | 基建 | MOCK 下集成测试建表缺失 | 4 | 4 | 5 | 80 | 2 | 40 | **P1** |
| PERF-002 | 性能 | confirmMoveFiles N+1 | 3 | 3 | 4 | 36 | 1 | 36 | **P1** |
| PERF-001 | 性能 | 搜索全量加载无 LIMIT | 3 | 4 | 4 | 48 | 2 | 24 | **P1** |
| MAIN-001 | 可维护性 | 协议码魔数散落 | 3 | 4 | 4 | 48 | 2 | 24 | **P2** |
| TEST-002 | 测试 | 覆盖率偏低（门禁边缘） | 3 | 3 | 4 | 36 | 3 | 12 | P2 |
| ARCH-001 | 架构 | 双协议通道并存 | 2 | 3 | 3 | 18 | 3 | 6 | P3 |
| PERF-003 | 性能 | 下载限速锁粒度粗 | 3 | 2 | 3 | 18 | 3 | 6 | P3 |
| CODE-001 | 代码质量 | 启动器/UI 模块 System.out 直出 | 2 | 2 | 3 | 12 | 3 | 4 | P3 |
| DOC-001 | 文档 | 部分模块文档覆盖不足 | 2 | 2 | 3 | 12 | 2 | 6 | P3 |
| PKG-001 | 可维护性 | 拼写错误类名（5 个类） | 3 | 4 | 3 | 36 | 2 | 18 | P2 |
| PKG-002 | 可维护性 | 重名类 Folder/FolderView（双包共存） | 2 | 3 | 3 | 18 | 3 | 6 | P3 |
| PKG-003 | 架构 | 包归属混乱 + 资源位置错误 | 2 | 3 | 3 | 18 | 1 | 18 | P2 |
| PKG-004 | 架构 | mc/ui/printer 旧包与 newcore 并存 | 2 | 3 | 3 | 18 | 3 | 6 | P3 |
| IMP-001 | 代码质量 | 未使用 import（5 处） | 1 | 3 | 3 | 9 | 1 | 9 | P3 |
| IMP-002 | 代码质量 | import 排序无规范 + static 通配符 | 1 | 3 | 4 | 12 | 3 | 4 | P3 |
| NAM-001 | 可维护性 | 方法名拼写错误（Form→From、cannel→cancel） | 2 | 2 | 2 | 8 | 1 | 8 | P3 |
| NAM-002 | 可维护性 | 变量/异常命名风格不佳（缩写、匈牙利后缀） | 1 | 3 | 2 | 6 | 2 | 3 | P3 |
| EXC-001 | 异常处理 | 核心层空 catch 吞异常（3 处静默 + 3 处无注释忽略） | 3 | 3 | 2 | 18 | 1 | 18 | P2 |
| EXC-002 | 异常处理 | 宽泛捕获 catch(Exception) 约 80 处 | 2 | 3 | 2 | 12 | 2 | 6 | P3 |
| EXC-003 | 异常处理 | UI/mc 遗留层空 catch 吞异常（20+ 处） | 2 | 2 | 1 | 4 | 3 | 1.3 | P3 |
| DEAD-001 | 代码质量 | 未使用方法参数：sendResource.fname、addFoldersToZipEntrySourceArray.account（调用方传入后被忽略） | 1 | 2 | 2 | 4 | 1 | 4 | P3 |
| DEAD-002 | 代码质量 | 死分支/恒假条件：FilesTable/FSViewer int 加法溢出致 `> Integer.MAX_VALUE` 恒假（隐藏溢出隐患）；KiftdProperties/FolderUtil `size()<MAX_VALUE` 恒真冗余子条件 | 2 | 2 | 2 | 8 | 1 | 8 | P3 |
| DEAD-003 | 代码质量 | 冗余实现：saveToRecycleBin 双份（FileSystemManager 退化版缺 null 保护/异常记录）；AESCipher 显式空构造器 | 1 | 2 | 2 | 4 | 1 | 4 | P3 |
| DEAD-004 | 代码质量 | 残留 IDE 自动生成 TODO 注释（VerificationCodeFactory/CharsetDetectionObserverImpl 各 1 处） | 1 | 1 | 2 | 2 | 1 | 2 | P3 |
| DEAD-005 | 代码质量 | 未使用类/接口：UploadKeyCertificate（仅测试引用）；FileStorageService/LogService 抽象层（实现为 bean 但无任何消费者） | 2 | 2 | 2 | 8 | 2 | 4 | P3 |
| DEAD-011 | 代码质量 | 恒假/恒真条件与冗余：FileServiceImpl 3 处 `< 0` 死分支（long 计数器恒非负）、getFileNameFormPath 恒真 `paths.length > 0`、packTime 区间冗余下界、2 处冗余 else；FileBlockUtil `else { continue; }`；RangeFileStreamWriter `maxRate < Long.MAX_VALUE` 恒真子条件 | 1 | 3 | 2 | 6 | 1 | 6 | P3 |
| DEAD-012 | 代码质量 | 重复实现/过时注释：FolderUtil/FolderServiceImpl 重复 parseInt；FileBlockUtil zip 改名循环重复（DRY 抽取）；FolderUtil Javadoc 与实现不符 | 1 | 2 | 2 | 4 | 1 | 4 | P3 |
| EXC-004 | 异常处理 | ApiPerformanceFilter 端点统计达上限后 computeIfAbsent 返回 null，finally 中 `.incrementAndGet()` 触发 NPE 破坏请求 | 2 | 3 | 3 | 18 | 1 | 18 | P2 |
| PERF-003 | 性能 | 下载限速锁粒度为全局/单账户，极端并发下有性能瓶颈 | 3 | 2 | 3 | 18 | 3 | 6 | P3 |

**基线结论**：
- 高优先级（P0/P1）共 5 项，全部在本迭代处理完毕并闭环验证；
- P2 中 MAIN-001、PKG-001、PKG-003、EXC-001、EXC-004 已处理；TEST-002（覆盖率提升）本次闭环（50.42% → 54.86%）；
- P3 中 IMP-001（未使用导入）已清零并通过脚本门禁；NAM-001 已闭环；DEAD-001~005（死代码五路扫描）已全部闭环；DEAD-006~012（三轮死代码扫描）已全部闭环；PKG-002（重名类）、EXC-003（UI/mc 空 catch 补注释）、IMP-002（import 排序规范+脚本门禁）、NAM-002（变量命名）、CODE-001（System.out 审计关闭）、EXC-002（120 处宽泛捕获全量评估，23 处精确化）、ARCH-001（API 通道规范化）、PERF-003（session 粒度限速锁）均已闭环；DOC-001（API-CONTRACT.md）本次闭环；余下 PKG-004（mc/ui/printer 旧包并存）已完成可行性评估：printer 为基础设施、mc CLI 为离线运维必需、ui Swing 收敛属产品功能（需开发 Web 管理端），列入产品路线图独立跟进。

---

## 4. 优先级排序解决计划（d3）

| 优先级 | 判定 | 本迭代处理项 | 状态 |
|---|---|---|---|
| P0 | D/C ≥ 50，安全/数据类，立即处理 | SEC-001 密码哈希接入；SEC-002 上传兜底 | ✅ 已闭环 |
| P1 | 25 ≤ D/C < 50，高影响 | TEST-INFRA-001 建表基建；PERF-002 N+1；PERF-001 搜索 LIMIT | ✅ 已闭环 |
| P2 | 10 ≤ D/C < 25，近期处理 | MAIN-001 协议码常量；TEST-002 覆盖率持续提升；EXC-001 吞异常；EXC-004 NPE 缺陷 | MAIN-001 ✅、EXC-001 ✅、EXC-004 ✅、TEST-002 ✅（54.86%） |
| P3 | D/C < 10，持续优化 | ARCH-001 双通道统一；PERF-003 锁粒度；CODE-001 输出收敛；DOC-001 文档；NAM-001 方法拼写；EXC-002 宽泛捕获；IMP-002/NAM-002 风格规范；EXC-003 UI 吞异常；DEAD-001~012 死代码；PKG-002 重名类 | ARCH-001 ✅、NAM-001 ✅、EXC-002 ✅、IMP-002 ✅、NAM-002 ✅、EXC-003 ✅、CODE-001 ✅（审计关闭）、DEAD-001~012 ✅、PKG-002 ✅、PERF-003 ✅、DOC-001 ✅（API-CONTRACT.md）；PKG-004 ✅ 已评估（UI 收敛列入产品路线图） |

---

## 5. 改进方案与实施记录（d4–d7）

### 5.1 SEC-001 密码哈希接入（安全 · P0）

**问题**：`conf/account.properties` 中账户密码以明文存储，服务器可读写该文件即获得全部口令。

**方案**：
- 以既有 `PasswordUtil`（PBKDF2 + 16 字节随机盐 + 100,000 迭代 + 恒定时间比较）为唯一写入出口；
- `ConfigurationManager` 4 处写入点统一接入哈希：
  - `changePassword`、`createNewAccount`、`resetPassword`、`createDefaultAccountPropertiesFile`；
- `verifyPassword` 双兼容（哈希与历史明文），历史明文账户无需人工迁移，首次改密后自动升级为哈希；
- 顺带将 `resetPassword` 的裸 `FileOutputStream.store` 统一为原子写（`storePropertiesAtomically`）。

**变更文件**：`server/util/ConfigurationManager.java`；`server/util/PasswordUtil.java`（既有，复用）。
**测试**：`ConfigurationManagerAccountTest` 新增哈希写入契约 + 默认 admin 哈希 + 明文迁移兼容用例。

### 5.2 SEC-002 上传大小兜底（安全 · P0）

**问题**：`multipartConfigElement` 若配置缺失，上传大小无上限，可打满磁盘。

**方案**：`WebMvcConfig.multipartConfigElement` 兜底：单文件 ≤50GB、请求 ≤52GB（含 multipart 开销）。

**变更文件**：`newcore/config/WebMvcConfig.java`。

### 5.3 PERF-001 搜索 LIMIT 下推（性能 · P1）

**问题**：全目录搜索对每个可访问目录一次性 `selectByParentFolderIds` 全量加载，超大目录下 OOM 风险高。

**方案**：
- SQL 层下推 LIMIT：`NodeMapper.queryByParentFolderIdsLimit`（`IN (...)` + `LIMIT`），经 `FileNodeRepository.selectByParentFolderIdsLimit` 暴露；
- 搜索上限常量：`SEARCH_RESULT_LIMIT = 500`（结果集）、`SEARCH_FILE_QUERY_LIMIT = 500`（查询下推）；
- 文件夹匹配增加 `size < LIMIT` 守卫。

**变更文件**：`NodeMapper`、`FileNodeRepository(+Impl)`、`FolderViewServiceImpl`。
**测试**：`NodeMapperTest` +3（下推、LIMIT 生效、空列表）；`FolderViewServiceImplTest` mock 更新。

### 5.4 PERF-002 confirmMoveFiles N+1 消除（性能 · P1）

**问题**：移动文件校验循环内重复 `queryByParentFolderId(targetFolderId)`，移动 N 个文件产生 N+1 次查询。

**方案**：校验前一次性加载目标目录的 `targetFolderNodes`/`targetFolders`，循环内改为引用预加载结果（零行为等价——校验期间目标目录不变）。

**变更文件**：`newcore/service/impl/FileServiceImpl.java`。

### 5.5 TEST-INFRA-001 集成测试基建修复（基建 · P1）

**问题**：MOCK 环境下 `@WebListener`（`ServerInitListener`）不触发，`FileNodeUtil.initNodeTableToDataBase()` 从未执行，FOLDER/FILE/PROPERTIES 表缺失，集成测试报 `Table "FOLDER" not found`（9 用例失败）。`TestConfig` 等 4 个基建类存在但未被任何测试引用（死代码）。

**方案**：
- `TestConfig` 构造器注入 `ConfigurationManager`（保证配置与目录初始化在前），`@PostConstruct` 中调用 `FileNodeUtil.initNodeTableToDataBase()` 显式建表；
- 4 个 `@SpringBootTest` 类 `classes` 扩展为 `{ KiftdApplication.class, TestConfig.class }`。

**变更文件**：`TestConfig`、`AuthServiceIntegrationTest`、`FileServiceIntegrationTest`、`FolderServiceIntegrationTest`、`KiftdApplicationIntegrationTest`。

**验证**：基线对照法确认失败为既有基建缺陷；修复后 80 用例全绿。

### 5.6 MAIN-001 AJAX 协议码常量提取（可维护性 · P2）

**问题**：AJAX 通道（`/homeController/*.ajax`）响应协议码以魔数散落 8 个文件约 80 处，易拼写错误导致前端分支失效。

**方案**：
- 新建 `newcore/domain/AjaxProtocol` 常量类（20 个协议码，字符串值与线上契约完全一致）；
- 替换 `FolderServiceImpl`、`FolderViewServiceImpl`、`FileServiceImpl`、`ResourceServiceImpl`、`MediaServiceImpl`、`FileChainServiceImpl`、`ExternalDownloadServiceImpl`、`AuthController` 中的字面量；
- 字符串值零变更 → 前后端契约不受影响。

**审计结论（GlobalExceptionHandler 专项化）**：`GlobalExceptionHandler` 已具备
NoResourceFoundException→404、ResponseStatusException→状态透传、其余→500，并区分 `/api/*`（JSON `ApiResponse`）与 AJAX 通道，
异常计数统计与防 `fbu.checkFileBlocks` 误删数据块防护完备，**专项化目标已达标，无需改动**。

**变更文件**：`newcore/domain/AjaxProtocol.java`（新增）+ 8 个使用方文件。

### 5.7 其他（文档/基础设施）

- `.gitignore` 补充 `logs/`（LogUtil 运行时按日日志目录），防止日志文件误提交。

### 5.8 PKG-001 拼写错误类名重命名（可维护性 · P2）

**问题**：5 个公共类名存在拼写错误并跨主代码与测试传播，长期维护易继续扩散错误命名：
`Propertie`（应为 Property）、`SreachView`（应为 SearchView）、`CheckUploadFilesRespons`/`CheckImportFolderRespons`/`CreateNewFolderByNameRespons`（应为 Response）。

**方案**：
- `server.model.Propertie` → `Property`：字段同步重命名 `propertieKey→propertyKey`、`propertieValue→propertyValue`；
  MyBatis Plus 表映射依赖 `@TableName("PROPERTIES")`/`@TableId`/`@TableField` 显式注解，类与字段名变更对 ORM 零影响；
- `server.pojo.SreachView` → `SearchView`；
- 3 个 `*Respons` → `*Response`；
- 对应测试类同步重命名（`PropertyTest`、`SearchViewTest`、`CheckUploadFilesResponseTest` 等）；
- 替换过程避开 `PropertiesMapper`/`PropertiesRepository` 等含 "Properties" 前缀的合法标识符（精确替换而非全局子串替换）。

**变更文件**：`server/model/Property.java`（新）、`server/pojo/SearchView.java`（新）、3 个 `*Response.java`（新）；引用方 `SystemServiceImpl`、`FileChainServiceImpl`、`PropertiesRepository(+Impl)`、`PropertiesMapper`、`FolderViewServiceImpl`、`FileServiceImpl`、`FolderServiceImpl` 及 7 个测试文件。

### 5.9 PKG-003 包归属与资源位置修复（架构 · P2）

**问题**：`SizeFormatUtil` 孤悬在顶层 `kohgylw.kiftd.util` 包（与 `server.util` 并列，包层级混乱）；`licenses` 许可文本置于 java 源码目录 `kohgylw/kiftd/util/licenses/`（非资源位置）。

**方案**：
- `SizeFormatUtil` 迁移至 `kohgylw.kiftd.server.util`（与其余服务器工具类同包），更新 4 个引用方 import，并移除 `ConfigurationManager` 中因同包而产生的冗余 import；
- `licenses/` 移至 `src/main/resources/licenses/`（标准资源位置，随 jar 打包）。

**变更文件**：`server/util/SizeFormatUtil.java`（新）、`util/SizeFormatUtil.java`（删）、`FileServiceImpl`、`ConfigurationManager`、`FileSystemCommandHandler`、`AccountCommandHandler`、`resources/licenses/*`。

### 5.10 IMP-001 未使用 import 清理（代码质量 · P3）

**问题**：5 处未使用 import（主代码 1 处 `KiftdDynamicWindow` 的 `java.awt.Point`；测试 4 处 `BeforeAll`、`QueryWrapper`、`ArrayList`、`PictureViewList`），多为复制粘贴遗留，易随代码复制继续扩散。

**方案**：
- 编写启发式扫描脚本 `scripts/find-unused-imports.ps1`：对每个 java 文件，取"去掉 import 行后的正文"，检查每个普通 import 的简单类名是否出现；未出现即报告（保守设计——注释/字符串中的同名会漏报，方向安全）；
- 排除 static 通配符导入（JUnit `Assertions.*` / Mockito `Mockito.*` 属测试惯例）；
- 逐文件人工核对后删除 5 处未使用 import。

**验证**：脚本普通未使用 import 候选 = 0；`mvn clean verify` 全绿。

**遗留规范项（IMP-002）**：import 分组排序（java → jakarta → org/com → kohgylw）无统一规范、测试普遍使用 static 通配符——属纯风格项，登记台账待排期。

### 5.11 NAM-001 方法名拼写错误修正（可维护性 · P3）

**问题**：4 个方法名存在拼写错误，`Form` 应为 `From`、`cannel/canncel` 应为 `cancel`：
- `FileSystemManager.getFileFormBlocks(Node)`（private，类内 4 处调用 + 测试反射名）；
- `ServerTimeUtil.getLastModifiedFormBlock(File)`（public static，跨 3 个类 7 处调用）；
- `FileSystemManager.cannel()` 与 `FSProgressDialog.canncel()`（各 1 处调用）。

**方案**：
- `getFileFormBlocks` → `getFileFromBlocks`；`getLastModifiedFormBlock` → `getLastModifiedFromBlock`；
- `cannel`/`canncel` → `cancel`（`FSProgressDialog` 私有方法同步重命名）；
- 同步更新全部调用方与测试（`FileSystemManagerTest` 反射名、`ResourceServiceImplTest` 的 mockStatic 引用）。

**验证**：残留扫描 `\b(getFileFormBlocks|getLastModifiedFormBlock|cannel|canncel)\b` = 0；`mvn clean verify` 全绿。

**遗留规范项（NAM-002）**：`settingp`/`ifc`/`mfname`/`namelistObj`/`e1`/`e2` 等缩写与匈牙利后缀变量命名——属风格规范，登记台账交 checkstyle 治理。

### 5.12 EXC-001/EXC-002 异常处理收敛（异常处理 · P2/P3）

**EXC-001 问题**：核心层空 catch 吞异常：
- `ResourceServiceImpl` 3 处 `catch (IOException e) {}` 静默丢弃（sendError(500/404) 失败、公告响应写入失败），排查时无任何痕迹；
- `LogUtil` 2 处、`ConfigurationManager` 1 处为有意忽略（`ignored`），但无注释说明意图。

**方案**：
- `ResourceServiceImpl` 3 处补 `logUtil.writeException(e)`，失败可追踪；
- `LogUtil`/`ConfigurationManager` 3 处补充意图注释（关闭日志系统时无法再记录、探测目录失败回退 user.dir），保留忽略语义但可读。

**EXC-002 问题**：`catch (Exception)` 宽泛捕获约 80 处，其中参数校验 `Integer.parseInt` 场景完全可精确化。

**方案**：确认每处 catch 体仅含 `Integer.parseInt` 后，6 处精确化为 `catch (NumberFormatException)`（`FolderServiceImpl` 5 处 + `FileServiceImpl` 1 处），行为完全等价（parseInt 仅抛 NFE），异常边界更清晰；其余复合业务块（数据库操作/JSON 解析/文件树递归，可能抛多种异常）保留宽泛捕获，登记待排期。

**验证**：`mvn clean verify` 全绿。

**遗留项**：EXC-002 其余约 70 处宽泛捕获待逐块评估；EXC-003（UI/mc 层 20+ 处空 catch）并入 ARCH-001 UI 改造。

### 5.13 DEAD-001~005 死代码清理（代码质量 · P3）

**排查方法（三路扫描）**：
1. **未使用成员**：扫描全部 private 方法/字段/常量/方法参数，逐一核对调用点；排除 Spring 自动装配（@Resource/@Autowired）、反射（`getDeclaredMethod`）、JSON 绑定（Gson 反序列化字段）等存活场景；
2. **空方法/死分支/冗余**：扫描空 catch、恒真/恒假条件、`Integer.MAX_VALUE` 溢出边界、重复实现、IDE 自动生成残留；
3. **未使用类/接口**：全库引用扫描（含测试），排除 @Controller/@Service/@Component 自动装配与反射引用。

**DEAD-001 未使用方法参数**：
- `ResourceServiceImpl.sendResource(File, String fname, ...)`：`fname` 从未参与响应（文件流式输出不依赖文件名），移除参数并同步 2 处调用点；
- `FileBlockUtil.addFoldersToZipEntrySourceArray(..., String account)`：`account` 在递归体内未使用，移除参数并同步 3 处调用点。

**DEAD-002 死分支/恒假条件**：
- `FilesTable` 165-168 行与 `FSViewer` 493 行：`folders.size() + files.size()` 为 int 加法，溢出后赋给 long，导致 `> MAX_LIST_LIMIT` 防御分支**恒假**（防御逻辑从未激活的隐藏溢出隐患）。修复为 `(long) folders.size() + files.size()` 先行强转，激活防御；
- `KiftdProperties` 141 行与 `FolderUtil` 50 行：`while (cond && list.size() < Integer.MAX_VALUE)` 中 `size()` 返回 int，恒 < MAX_VALUE，子条件恒真冗余，移除。

**DEAD-003 冗余实现**：
- `FileSystemManager.saveToRecycleBin` 与 `FileBlockUtil.saveToRecycleBin` 双份实现：前者为退化版（缺 `dateDir.list()` null 保护、异常静默），对齐健壮版（null 保护 + `lastDotIndex` 预计算 + 异常记录）；
- `AESCipher` 显式空构造器 `public AESCipher() {}`（类无其他构造器）删除。

**DEAD-004 残留 IDE 自动生成注释**：`VerificationCodeFactory`、`CharsetDetectionObserverImpl` 中 `// TODO 自动生成...` 存根删除。

**DEAD-005 未使用类/接口**（6 文件删除）：
- `server/pojo/UploadKeyCertificate.java`（仅测试引用，无 JSON 注解/主代码消费者）+ 其测试类；
- `newcore/infrastructure/storage/FileStorageService(+Impl)`、`logging/LogService(+Impl)`：实现虽注册为 bean 但**无任何消费者**（newcore 业务直接使用 FileBlockUtil/LogUtil），抽象层空转，整体删除并清理空目录。

**变更文件**：见第 9 章变更清单（DEAD 系列新增/修改/删除）。
**验证**：`scripts/find-unused-imports.ps1` 候选=0；`scripts/tech-debt-check.ps1` TODO 计数 2→0 全 PASS；`mvn clean verify` 全绿。

**已排除误报**：WelcomeController/ApiPerformanceFilter 等 @Controller/@WebFilter 自动装配存活；`FileSystemManager >= MAX_FOLDERS_OR_FILES_LIMIT`（long 返回值可比）与 FileBlockUtil `createNewBlock` 的 `appendIndex` 分支当时判定为有效防御。
> **后续修订（2026-08 第二轮 DEAD-009/DEAD-008）**：本轮重新审视后确认——`LIMIT 0,2147483647` 拼接为恒等冗余、`MAX_FOLDERS_OR_FILES_LIMIT=Integer.MAX_VALUE` 防御值形同虚设（真实业务上限为 10000），已对齐 `FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER`；`createNewBlock` 的 else 换 UUID 重试分支因 `appendIndex < Integer.MAX_VALUE` 恒真而不可达，已删除。详见 5.16 节。

### 5.14 PKG-002/EXC-003/NAM-002/IMP-002/CODE-001/EXC-002 收尾治理（可维护性 · 代码质量 · 异常处理）

**PKG-002 重名类重命名**（`util.file_system_manager.pojo` 包）：
- 原 `Folder`/`FolderView` 与 `server.model.Folder`/`server.pojo.FolderView` 同名，import 时易混淆；
- 更名 `Folder → FolderTreeNode`（`extends server.model.Folder`，覆写 `toString()` 返回文件夹名）、`FolderView → FileSystemFolderView`，删除旧类文件；
- 引用方 4 个文件同步更新（`FileSystemManager` 27 处、`ui/module/FSViewer`、`ui/util/FilesTable`、`mc/FileSystemCommandHandler`）。

**EXC-003 UI/mc 层空 catch 补注释**：
- `PathsTable` 刷新失败保留旧布局；`FileSystemCommandHandler` 路径解析/序号参数异常视为未找到（×3）；`ServerUIModule` 图标缺失/不可读保留默认、行偏移失败 end 保持 0（×2）；`KiftdDynamicWindow` 缩放设置文件损坏时回退程序计算比例。
- 修复期间发现上轮注释插入导致 `PathsTable` 匿名 Runnable 缺右括号（增量编译掩盖），`mvn clean verify` 全量编译暴露后补齐修复。

**NAM-002 变量命名规范**：
- `settingp→settingProps`（`KiftdDynamicWindow`）、`ifc→constraintValue`（`FolderUtil` ×3、`FolderServiceImpl` ×7、`FileServiceImpl` ×1）、`mfname→maxFileName`、`namelistObj→parsedNameList`（`FileServiceImpl`）；全部为局部变量，批量替换行为等价。

**IMP-002 import 排序规范**：
- 制定规范：`java.* → jakarta./javax.* → org./com.* → kohgylw.* → 其他`，组内 Ordinal 排序；static 通配符（测试惯例 112 处）豁免；
- 新增 `scripts/check-import-order.ps1`（门禁，94 个含 import 文件 0 违规）与 `scripts/reorder-imports.ps1`（自动重排 45 个主代码文件，UTF-8 no BOM 读写保中文注释）。

**CODE-001 System.out 审计**（审计关闭，无代码改动）：
- 全库 93 处 System.out 逐一审计：核心层（newcore+server）已清零；其余全部位于 mc/UI 控制台交互合理场景（Printer 输出中枢、MC 横幅/命令提示符/帮助、UI 启动提示），与 7.2 章监控脚本豁免声明一致。

**EXC-002 全量 120 处宽泛捕获评估**（本轮收尾）：
- **精确化 23 处**（行为等价或更窄，异常边界清晰）：
  - 参数解析类（→`NumberFormatException`）：`FileServiceImpl.checkUploadFile`（`NumberFormatException | IndexOutOfBoundsException`，try 收窄至两个解析语句并对 `parsedNameList==null` 显式防护）、`checkImportFolder`、`FolderUtil` ×2、`ConfigurationManager` ×3（端口/缓冲/https 端口）；`SizeFormatUtil` ×2（→`IllegalArgumentException`，覆盖显式抛出与 NumberFormatException）；
  - IO 类（→`IOException`）：`FileBlockUtil.saveToRecycleBin`、`FileSystemManager.saveToRecycleBin`、`ConfigurationManager` ×5（原子写 ×2、WatchService.close、FileOutputStream ×2）、`SystemHealthService` 文件系统读写探测、`StartupHealthChecker` 文件系统探测、`ResourceServiceImpl.sendError`；
  - 其他（→精确类型）：`SystemHealthService` DB 连接→`SQLException`；`PasswordUtil`→`GeneralSecurityException`；`IpAddrGetter`→`UnknownHostException`；`ConfigurationManager` DB 连通测试→`ReflectiveOperationException | SQLException`。
- **保留宽泛 97 处**（逐块评估登记理由，代表性清单）：
  - 复合业务块（DB+JSON+文件树+日志，多异常类型）：FileServiceImpl 移动/删除/打包、FolderServiceImpl 重试插入、FileBlockUtil 校验/打包、AuthServiceImpl 登录/改密/注册、FileChainServiceImpl、MediaServiceImpl 转码探测、SystemServiceImpl、FileSystemManager 导入；
  - 防御/兜底型（失败降级返回，捕获一切合理）：ResourceServiceImpl 流式响应、ConfigurationManager 配置加载、NoticeUtil/LogUtil/FileNodeUtil/RSA 系列、RetryUtil 重试包装、CircuitBreaker 记录重抛、KiftdApplication 启动/关闭钩子；
  - CLI/UI 层（打印错误提示）：AccountCommandHandler、FileSystemCommandHandler、FSViewer、FileSystemPathViewer、FilesTable、SettingWindow、ServerUIModule、UIRunner/ConsoleRunner/MC、Printer。
- **精确化原则**：仅当 try 内异常类型单一（纯 parse 或单类 IO/网络）才精确化；复合块保留宽泛并在台账注明合理。

**验证**：`mvn clean verify` 全绿（单测 895 + 集成 80）；`tech-debt-check.ps1` / `find-unused-imports.ps1` / `check-import-order.ps1` 三脚本全 PASS。

### 5.15 ARCH-001 API 通道规范化（架构 · P3）

**问题**：`/homeController/*.ajax`（Web UI 主通道）与 `/api/*`（程序化通道）双协议并存：
- AJAX 通道 7 个控制器 30+ 端点，Session + 字符串协议码（`AjaxProtocol`），过滤链完整；
- `/api/*` 仅 4 个端点且"有名无实"：正常响应输出**裸 Map**，未用 `ApiResponse` 统一包装；
  `ApiResponse`/`ResultCode` 成为仅 `GlobalExceptionHandler` 异常路径使用的半成品框架；
- 管理员认证手写在 `SystemInfoController.requireAdmin` 内，未走统一过滤链，与 AJAX 通道认证机制不一致。

**方案**（双通道明确分工，前端零改动）：
- AJAX 通道保持 Web UI 专用（Session + 协议码线上契约不变）；
- API 通道规范化：
  1. `SystemInfoController` 4 个端点（`/info`、`/stats`、`/health`、`/metrics`）统一返回 `ApiResponse<T>` 包装，消除裸 Map；
  2. 新增 `newcore/infrastructure/security/ApiAuthFilter`（`@WebFilter("/api/*")`、`@Order(1)` 置于 MastLoginFilter 之前）：
     未登录 → 401 JSON、非管理员 → 403 JSON、管理员放行；移除控制器内嵌 `requireAdmin`，
     未登录时 API 客户端收到结构化 401 JSON 而非面向浏览器的 HTML 重定向；
  3. `WebMvcConfig` 的 `@ServletComponentScan` 补充扫描 `newcore.infrastructure` 包——修复此前该包下的
     `@WebFilter`（既有 `ApiPerformanceFilter` 与新增 `ApiAuthFilter`）未被注册进 Servlet 容器的隐患；
  4. 顺带修正 `SystemHealthService.getHealthStatus` 中过期版本号残留 `"1.2.3-SNAPSHOT"` → `"1.3.0"`；
  5. 修复"内置 admin 无法识别为超级管理员"缺陷：`ConfigurationManager.hasSuperAuth` 将内置 `admin` 账户
     识别为超级管理员（`.privilege=S` 仍作为显式提权手段）。此前 `/api/system/*` 在标准部署
     （无 `.privilege=S`）下对真实管理员同样返回 403，属产品级缺陷，由新增集成测试暴露。

**变更文件**：`newcore/infrastructure/security/ApiAuthFilter.java`（新增）、`newcore/controller/SystemInfoController.java`、
`newcore/service/SystemHealthService.java`、`newcore/config/WebMvcConfig.java`（@ServletComponentScan 补包）、
`server/util/ConfigurationManager.java`（hasSuperAuth 内置 admin 识别）。

**测试**：`SystemInfoControllerTest`（+5，ApiResponse 包装契约）、`ApiAuthFilterTest`（+3，401/403/管理员放行）、
`SystemApiIntegrationTest`（+4，真实容器验证过滤器注册与 401/403/200 行为）。

**验证**：`mvn clean verify` BUILD SUCCESS（单测 897 + 集成 84）；三脚本全 PASS。前端零改动（webContext 无任何 `api/` 调用，契约不变）。

### 5.16 DEAD-006~010 第二轮死代码清理（代码质量 · 架构 · P3）

**排查方法**：延续 5.13 节三路扫描（未使用成员 / 死分支冗余 / 未使用类接口），本轮重点复查上轮"已排除误报"清单与旧架构过渡期残留。

**DEAD-006 未使用公共方法 7 处删除**（无任何生产调用方，测试独用引用随测试同步清理）：
- `ServerTimeUtil.getTimeFromDateAccurateToDay`（含 import 清理）；
- `ApiPerformanceFilter.resetStats`（性能统计自复位入口，无调用方）；
- `VideoTranscodeThread.abort`（转码线程无外部取消路径）；
- `RangeFileStreamWriter.writeRangeFileHead`（7 参重载，主入口为 8 参 `writeRangeFileStream`）；
- `RSAKeyUtil.getKeySize` / `TextFormateUtil.hasEscapes`（仅测试引用）；
- `ConfigurationManager.reTestServerPropertiesAndEffect`（仅转调 `revalidate()` 的孤儿包装）。

**DEAD-007 重复方法 / 冗余重载**：
- `ConfigurationManager.getPropertiesStatus` 与 `getStatus()` 逻辑完全一致（重复实现），删除后 `ServerUIModule` 2 处调用改为 `getStatus()`；
- `TxtCharsetGetter.getTxtCharset(byte[], int, int)` 重载（仅测试引用，主入口为 InputStream 版）删除，顺带清理 `import java.util.Arrays`；
- `RetryUtil` 两个重载删除：`executeWithRetry(Runnable, String)`、2 参泛型 `executeWithRetry(RetryableOperation<T>, String)`（无入口，仅留 5 参完整版）；测试改调 5 参版。
- **保留**：`ui/callback/GetServerStatus.getPropertiesStatus()` 为 UI 回调接口签名，由 `mc/UIRunner` 实现并转调 `ConfigurationManager.getStatus()`，属 Swing 回调存活代码，非 ConfigurationManager 重复方法。

**DEAD-008 不可达/恒假分支**：
- `FileBlockUtil.createNewBlock`：else 换 UUID 重试分支因 `appendIndex >= 0 && appendIndex < Integer.MAX_VALUE` 恒真不可达（含 `retryNum` 兜底 return null），删除 else 分支后保留递增序号兜底；
- `FileSystemCommandHandler.showCurrentFolder`：`Math.max(...) > Integer.MAX_VALUE` 恒假 if 块删除；
- `AuthServiceImpl.doPong` 冗余 else 简化为无 else return。

**DEAD-009 FileSystemManager 冗余上限防御**（上轮排除项翻转，见 5.13 修订注）：
- 4 处 SQL `LIMIT 0,2147483647` 恒等拼接删除（`selectNodesByPathExcludeById` / `selectNodesByFolderId` / `selectFoldersByParentFolderId` / `selectNodesByExtendStoreIndex`）；
- `MAX_FOLDERS_OR_FILES_LIMIT = Integer.MAX_VALUE` 常量删除，2 处上限判断（文件/文件夹导入）对齐全局业务上限 `FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER(10000)`，与 FolderUtil / FileServiceImpl / FolderServiceImpl 一致。

**DEAD-010 双份实现冗余**（架构过渡期残留）：
- `FileChainService.getChainKeyByFid`（旧数据层 PropertiesMapper+AESCipher）与 `SystemServiceImpl.getFileChainKey`（新数据层）业务重复，前端实际调用走新路径；删除接口方法与实现（含 AccountAuth/FolderMapper/Folder/FolderUtil 依赖裁剪），构造器 9 参精简为 7 参（NodeMapper/FileBlockUtil/ContentTypeMap/LogUtil/AESCipher/ChainKeyMaster/PropertiesMapper）。

**变更文件**：见第 9 章变更清单（DEAD-006~010 新增/修改/删除）。

**教训（防误判）**：`SizeFormatUtil.formatFileSize(String)` 首轮被误判为死代码删除，`mvn clean verify` 编译期暴露 `mc/FileSystemCommandHandler` 真实调用（`Node.getFileSize()` 返回 String），已恢复（`Long.parseLong` + 非法值原样返回）。**判定公共方法死活必须以全库调用点扫描为准（含 mc/UI 层），且以全量编译作为最终裁决**。

**验证**：`mvn clean verify` BUILD SUCCESS（全量编译，非增量）；`scripts/find-unused-imports.ps1` 候选=0；`scripts/tech-debt-check.ps1` / `scripts/check-import-order.ps1` 全 PASS；残留扫描（`MAX_FOLDERS_OR_FILES_LIMIT`/`getChainKeyByFid`/`getPropertiesStatus`(ConfigurationManager) 等）= 0。

### 5.17 DEAD-011/DEAD-012/EXC-004/PERF-003 第三轮死代码清理与缺陷修复（代码质量 · 异常处理 · 性能 · P3）

**排查方法**：延续三路扫描（未使用成员 / 死分支冗余 / 未使用类接口），本轮结果：
1. **未使用 private 成员**：无候选（所有 private 成员均有有效引用或测试反射引用）；
2. **未使用类/接口**：无候选（所有顶层类型均可达：Spring 组件扫描 / Servlet 注解 / 入口链）；
3. **死分支与冗余**：12 处候选全部修复（见 DEAD-011/012），另发现 1 处真实 NPE 缺陷（EXC-004）。

**DEAD-011 恒假/恒真条件与冗余**（FileServiceImpl 6 处 + FileBlockUtil 1 处 + RangeFileStreamWriter 1 处）：
- `FileServiceImpl` 3 处 `< 0` 死分支：`estimatedTotal`（`countByParentFolderId` 返回 long 恒 ≥0 + 名单长度差 ≥0）、`estimateFilesTotal`/`estimateFoldersTotal`（long 计数器恒非负）——防御分支从未激活，删除；
- `FileServiceImpl.getFileNameFormPath`：`if (paths.length > 0)` 恒真（`String.split` 对非空字符串至少返回 1 个元素），直接 `return paths[paths.length - 1]`；
- `FileServiceImpl` packTime 区间判断：删除恒真下界（`packTime >= 4L` 等），改为递增 `<` 条件序列，末分支直接 return；
- `FileServiceImpl` 2 处冗余 else（`if (newNode != null) { ...; return; } else { ... }`）简化为无 else；
- `FileBlockUtil.saveToFileBlocks`：循环体末尾 `else { continue; }` 无跳转效果，删除；
- `RangeFileStreamWriter`：`maxRate < Long.MAX_VALUE` 恒真子条件删除（所有调用方传参来自 `ConfigurationManager.getDownloadMaxRate`，经 `parseRateString` 只返回 -1/0/正有限值），三元表达式简化为 `maxRate > 0 && session != null`。

**DEAD-012 重复实现/过时注释**：
- `FolderUtil.createNewFolder` 与 `FolderServiceImpl.newFolder` 对同一 `folderConstraint` 重复 `Integer.parseInt` 两次 → 提升外层解析一次，循环内直接复用 `constraintValue`（校验语义各自保持：FolderUtil `> 0` / FolderServiceImpl `!= 0`，行为等价）；
- `FileBlockUtil` zip 打包顶层 for 与递归层 for 的同名文件夹改名 while 循环逐字重复 → 抽取私有方法 `deduplicateFoldersAndAddToZip`（DRY）；
- `FolderUtil.getParentList` Javadoc 描述与实现不符（"如果上级层数超过了 Integer.MAX_VALUE……" 实际实现无此逻辑）→ 修正为"沿 parent 链向上迭代，遇到不存在或成环的节点时停止"。

**EXC-004 ApiPerformanceFilter NPE 缺陷**（真实缺陷，非死代码）：
- `endpointRequestCount.computeIfAbsent(endpoint, k -> ...)` 在统计条目数达 `MAX_ENDPOINT_ENTRIES(100)` 时返回 null，原代码对 null 直接 `.incrementAndGet()`，在 finally 块中触发 NPE 破坏请求处理；
- 修复：条目达上限后不再新增统计条目（`counter == null` 跳过自增），仅更新已有条目计数。

**PERF-003 下载限速锁粒度核验**（台账关闭，无新改动）：
- 主链路已由上轮实现为 **session 粒度限速锁**：`VariableSpeedBufferedOutputStream` 以 `HttpSession` 为监视器，预算耗尽时 `session.wait(remain)` 释放监视器 + `notifyAll()` 唤醒，避免慢速任务长时间独占锁；
- 配套并发测试 `testConcurrentWritesSharedSessionCompleteAllData` 验证同会话多任务无死锁且完整写出；
- 外链下载 `synchronized(downloadKeyMap)` 仅为密钥表并发保护，非限速锁；无 session 时降级不限速属合理边界 → 台账置「已修复」。

**变更文件**：见第 9 章变更清单（DEAD-011/012/EXC-004 新增/修改）。
**验证**：`mvn clean verify` BUILD SUCCESS；三脚本全 PASS；残留恒假模式扫描（`< 0` 死分支/恒真下界）= 0。

### 5.18 TEST-002/DOC-001 修复与 PKG-004 收敛可行性评估（测试 · 文档 · 架构 · P2/P3）

**TEST-002 覆盖率提升**（测试 · P2，本次闭环）：
- 上轮基线：整体行覆盖率 50.42%（远高于 25% 门禁），但 `newcore` 多个关键类为 **0 覆盖**；
- 本次补齐 7 个测试类 **44 个用例**（`mvn clean verify` 后单测 876 → **920**）：
  - `SystemHealthServiceTest`（+5）：健康状态全组件 UP / 数据库不可用 / 文件系统路径缺失 / metrics 分区完整性 / 磁盘缺失分区为空；
  - `StartupHealthCheckerTest`（+5）：全通过 / 数据库不可用 / 文件系统缺失 / context 为 null / DataSource bean 缺失；
  - `CryptoServiceImplTest`（+6）：AES-GCM 加解密往返 / 错钥不往返 / 256-bit 密钥 / 随机性 / RSA 解密往返 / 非法密钥返回 null；
  - `ApiPerformanceFilterTest`（+4）：端点统计 / 异常时 finally 仍统计（EXC-004 路径）/ 慢请求计数 / **端点达上限不新增不 NPE**；
  - `FileNodeRepositoryImplTest`（+12）、`FolderRepositoryImplTest`（+8）、`PropertiesRepositoryImplTest`（+4）：三层仓储委托契约；
- 效果：整体行覆盖率 **50.42% → 54.86%**（远超 35% 目标），server.util 保持 59.8%；
- 零覆盖类收敛：CryptoServiceImpl、ApiPerformanceFilter、SystemHealthService、StartupHealthChecker、三个 RepositoryImpl 全部清零（KiftdApplication 88 行启动类除外——由 failsafe 集成测试覆盖，不进 surefire 统计）。

**DOC-001 契约文档**（文档 · P3，本次闭环）：
- 新增 `docs/API-CONTRACT.md`：双通道概览（AJAX + `/api/*`）与 `ApiResponse<T>` 包装结构；
- **AJAX 协议码对照表**：`AjaxProtocol` 全部 20 个协议码（常量名 → 字符串值 → 语义 → 主要产出方），并注明登录/注册等非协议码响应契约；
- 9 个服务接口（Auth/Folder/File/FolderView/Media/Resource/ExternalDownload/FileChain/System）方法级契约（参数、返回、协议码）；
- 控制器端点 → 服务方法映射表（8 个控制器 40+ 端点）；
- 契约变更流程（协议码修改 → 前端同步 → 脚本门禁 → 全量回归）。

**PKG-004 旧包收敛可行性评估**（架构 · P3，评估闭环）：
- **现状盘点**：`mc`（5 文件 1435 行：MC 入口 + ConsoleRunner + UIRunner + 2 个命令处理器）、`ui`（14 文件 2723 行：Swing 主窗口/设置/状态/文件系统浏览/进度）、`printer`（2 文件 117 行：输出中枢 + MessageOutput 接口）；
- **不可收敛部分**：
  - `printer`：为 newcore/server/util 全局输出中枢（`Printer.instance` 被 30+ 核心类依赖），已在上轮完成 UI 解耦（`setMessageOutput` 注入式输出接收器），**属基础设施而非旧架构残留**；
  - `mc` CLI（`-import/-export/-transfer/-account/-resetpwd`）与 `-console`：离线运维入口，必须在服务器启动前可用（Web 无法替代）；`-start` 为推荐服务器启动模式；
  - `mc` 启动器（MC/UIRunner/ConsoleRunner）：多模式启动的产品特性（无参数=GUI / `-console` / `-start`），收敛后仍须保留。
- **可收敛部分（长期路线）**：`ui` Swing 管理界面（SettingWindow/ServerUIModule/FSViewer 等，约 2000 行）承担的服务器设置编辑、运行状态监控、账户管理、文件系统导入导出能力——**当前 Web 前端（webContext）仅提供文件操作与账户登录，无任何管理页面**，故收敛等价于**开发一套 Web 管理端**（设置/监控/账户管理页面 + 对应 `/api/*` 端点），属产品功能开发而非技术债清理；
- **评估结论**：PKG-004 **不建议在本迭代以"删除/合并代码"方式闭环**——它不是死代码，而是多模式启动 + 离线运维 + Swing 管理的产品特性。合理路线：
  1. 短期（已完成）：printer 与 UI 解耦、Swing 层不反向依赖 newcore（分层单向）；
  2. 中期：若需 Web 管理端，以新增 `/api/*` 管理端点 + webContext 管理页面方式增量建设，Swing 界面与 Web 并存过渡；
  3. 长期：Swing 仅保留启动引导，管理能力完全收敛至 Web。
- **台账状态**：置「已评估」——保留现状（分层单向依赖已达标），收敛列入产品路线图，不再作为待排期技术债阻塞项。

---

## 6. 回归测试验证结果（d7）

### 6.1 全量门禁

```
mvn clean verify
```

| 阶段 | 结果 |
|---|---|
| 单元测试（surefire） | **920 用例，0 失败 0 错误**（上轮 876；本轮 TEST-002 新增 7 测试类 44 用例：SystemHealthServiceTest 5 + StartupHealthCheckerTest 5 + CryptoServiceImplTest 6 + ApiPerformanceFilterTest 4 + FileNodeRepositoryImplTest 12 + FolderRepositoryImplTest 8 + PropertiesRepositoryImplTest 4） |
| JaCoCo 覆盖率门禁 | 整体行覆盖 ≥25%、server.util ≥30%：**全部达标**；整体行覆盖 50.42% → **54.86%**（covered 3886 / total 7083） |
| 集成测试（failsafe） | **84 用例，0 失败 0 错误** |
| 打包（jar + copy-libs） | 成功 |
| 监控脚本门禁 | `tech-debt-check.ps1`、`find-unused-imports.ps1`、`check-import-order.ps1` 三脚本全 PASS（import 候选 0 / 95 文件 0 违规） |
| 最终结论 | **BUILD SUCCESS** |

### 6.2 回归归属判定（防死胡同）

| 疑点 | 判定方法 | 结论 |
|---|---|---|
| 集成测试失败是否由本轮改动引入？ | `git stash` 保存改动 → 基线 `mvn clean verify` → 对照 → `git stash pop` | 基线同样失败（Folder 2F/3E、File 2F/2E）→ **既有基建缺陷，与改动无关** |
| 协议码重构是否破坏前端契约？ | 常量值与字面量逐项比对 + 80 集成/单测 | 字符串零变更，契约不变 |
| 类名/包名重命名是否破坏 ORM 与依赖？ | `Propertie` 依赖 `@TableName("PROPERTIES")` 等显式注解（类/字段名与表/列名解耦）+ 全量 verify | `mvn clean verify` BUILD SUCCESS（单测 895 + 集成 80）；`\b(Propertie|SreachView|Respons)\b` 残留扫描 = 0 |
| EXC-003 注释插入是否破坏语法结构？ | `mvn clean verify` 全量编译（非增量） | `PathsTable` 匿名 Runnable 缺右括号被暴露并修复；增量编译会掩盖该问题，故回归一律使用 `clean` |

---

## 7. 技术债监控机制（d8）

### 7.1 登记台账（唯一事实来源）

`docs/TECH-DEBT-REGISTER.md`：所有债务必须登记、打分（S×I×P）、标注优先级与状态。
新增债务流程：登记 → 打分 → 定优先级 → 排期 → 修复后回填验证方式并关闭。

### 7.2 静态检查脚本（可执行门禁）

`scripts/tech-debt-check.ps1`，用法：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/tech-debt-check.ps1
```

| 规则 | 阈值 | 说明 |
|---|---|---|
| TODO/FIXME/HACK/XXX 注释 | ≤50 | 债务注释总量 |
| 核心层（newcore+server）System.out/err 直出 | =0 | 输出必须走 Printer/LogUtil；启动器/UI 合法控制台输出豁免 |
| printStackTrace 直打 | =0 | 异常必须记录日志 |
| AJAX 协议码魔数（`return "ERROR"` 等） | =0 | 必须引用 `AjaxProtocol` 常量 |

`scripts/find-unused-imports.ps1`（新增，IMP-001 门禁），用法：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/find-unused-imports.ps1
```

| 规则 | 阈值 | 说明 |
|---|---|---|
| 普通 import 的简单类名未在正文出现 | =0 | 启发式扫描（跳过 static 通配符，如 JUnit/Mockito 惯例）；保守设计，注释/字符串同名会漏报，方向安全 |

`scripts/check-import-order.ps1`（新增，IMP-002 门禁）+ `scripts/reorder-imports.ps1`（自动重排），用法：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-import-order.ps1
```

| 规则 | 阈值 | 说明 |
|---|---|---|
| import 分组与组内排序（java → jakarta/javax → org/com → kohgylw → 其他） | =0 违规 | 主代码 94 个含 import 文件；static 通配符导入（JUnit/Mockito 测试惯例）豁免 |

退出码：0=通过；1=超阈值（应阻断提交/CI）；2=脚本错误。当前基线 **PASS**。

### 7.3 准入规则（防止新增债务）

1. 新增业务代码必须走 `Printer`/`LogUtil`，禁止 `System.out`/`printStackTrace`；
2. AJAX 响应协议码必须引用 `AjaxProtocol` 常量，禁止裸字符串；
3. 新增数据库查询必须确认 LIMIT/分页策略（拒绝无界查询）；
4. 新增功能必须附带单元测试；改动公共接口必须跑通集成测试；
5. 提交前执行 `tech-debt-check.ps1`，超阈值不得合入；
6. 建议后续接入 CI（GitHub Actions/Jenkins）：`mvn clean verify` + `tech-debt-check.ps1` 双门禁。

---

## 8. 后续迭代建议与风险

### 8.1 遗留债务建议排期

> 本次迭代后台账内全部登记债务均已闭环（✅ 已修复 / ✅ 已评估）。下表仅剩产品路线图项与持续优化建议。

| 债务ID | 建议 |
|---|---|
| PKG-004 旧包并存 | mc/ui/printer 与 newcore 分层并存（已完成可行性评估）：printer 为基础设施保留、mc CLI 为离线运维必需；ui Swing 管理界面收敛需开发 Web 管理端，**列入产品路线图**独立跟进 |
| TEST-002 覆盖率（持续优化） | 已达标 54.86%（远超 35% 目标）；后续仍可优先补齐 `newcore/service/impl` 关键路径（文件移动/批量删除）与 `server/util` 边界类 |

### 8.2 风险与注意事项

- **AJAX 协议码属于前后端线上契约**：修改 `AjaxProtocol` 常量值前必须同步调整前端 `home.js` 等；
- **密码迁移**：`verifyPassword` 兼容明文仅为过渡，建议在 `account.properties` 中标注 `pwd.hash=PBKDF2` 版本字段并定期迁移；
- **集成测试共享文件库**：当前 IT 直接使用项目 `filesystem/filenodes/kift` 文件库，测试数据与本地运行数据共享，CI 环境建议注入隔离的 `user.dir`（`AbstractIntegrationTest`/`TestEnvironmentSetup` 已预留，后续接线可进一步隔离）；
- **H2 文件库并发**：多进程同时运行测试可能锁库，CI 与本地开发避免并行执行。

---

## 9. 附录：变更文件清单

### 新增
| 文件 | 说明 |
|---|---|
| `src/main/java/kohgylw/kiftd/newcore/domain/AjaxProtocol.java` | AJAX 协议码常量（20 个） |
| `docs/TECH-DEBT-REGISTER.md` | 技术债登记台账 |
| `docs/TECH-DEBT-ITERATION.md` | 本文档 |
| `scripts/tech-debt-check.ps1` | 技术债静态检查脚本 |
| `scripts/find-unused-imports.ps1` | 未使用 import 启发式扫描脚本（IMP-001 门禁） |
| `server/model/Property.java` | 原 Propertie 更名（@TableName("PROPERTIES") 不变，字段更名） |
| `server/pojo/SearchView.java` | 原 SreachView 更名 |
| `server/pojo/CheckUploadFilesResponse.java`、`CheckImportFolderResponse.java`、`CreateNewFolderByNameResponse.java` | 原 *Respons 更名 |
| `server/util/SizeFormatUtil.java` | 由顶层 kohgylw.kiftd.util 迁移归位 |
| `src/main/resources/licenses/` | 许可文本由 java 源码目录迁移至标准资源位置 |
| `newcore/infrastructure/security/ApiAuthFilter.java` | ARCH-001：API 通道管理员认证过滤器（未登录 401 / 非管理员 403 / 放行） |
| `src/test/java/kohgylw/kiftd/newcore/controller/SystemInfoControllerTest.java` | ARCH-001：ApiResponse 包装契约测试（+5） |
| `src/test/java/kohgylw/kiftd/newcore/infrastructure/security/ApiAuthFilterTest.java` | ARCH-001：过滤器 401/403/放行测试（+3） |
| `src/test/java/kohgylw/kiftd/integration/SystemApiIntegrationTest.java` | ARCH-001：API 通道集成验证（+4，真实容器 401/403/200） |
| `docs/API-CONTRACT.md` | DOC-001：双通道契约文档（AJAX 协议码对照表 20 个 + 9 服务接口契约 + 8 控制器 40+ 端点映射 + 契约变更流程） |
| `src/test/java/kohgylw/kiftd/newcore/service/SystemHealthServiceTest.java` | TEST-002：健康状态 5 用例（@TempDir 模拟文件系统） |
| `src/test/java/kohgylw/kiftd/newcore/service/StartupHealthCheckerTest.java` | TEST-002：启动健康检查 5 用例（数据库不可用/文件系统缺失/context 为 null/DataSource 缺失） |
| `src/test/java/kohgylw/kiftd/newcore/infrastructure/crypto/impl/CryptoServiceImplTest.java` | TEST-002：AES-GCM/RSA 加解密 6 用例 |
| `src/test/java/kohgylw/kiftd/newcore/infrastructure/logging/ApiPerformanceFilterTest.java` | TEST-002：性能统计 Filter 4 用例（含 EXC-004 端点超限不 NPE 回归） |
| `src/test/java/kohgylw/kiftd/newcore/repository/impl/FileNodeRepositoryImplTest.java` | TEST-002：仓库委托契约 12 用例 |
| `src/test/java/kohgylw/kiftd/newcore/repository/impl/FolderRepositoryImplTest.java` | TEST-002：仓库委托契约 8 用例 |
| `src/test/java/kohgylw/kiftd/newcore/repository/impl/PropertiesRepositoryImplTest.java` | TEST-002：仓库委托契约 4 用例 |

### 修改（主代码）
| 文件 | 改动 |
|---|---|
| `server/util/ConfigurationManager.java` | 密码写入 4 处接入 PBKDF2 哈希；resetPassword 原子写 |
| `newcore/config/WebMvcConfig.java` | multipart 兜底 50GB/52GB |
| `server/mapper/NodeMapper.java` | 新增 `queryByParentFolderIdsLimit` |
| `newcore/repository/FileNodeRepository.java` + Impl | 新增 `selectByParentFolderIdsLimit` |
| `newcore/service/impl/FolderViewServiceImpl.java` | 搜索 LIMIT 下推（500） |
| `newcore/service/impl/FileServiceImpl.java` | confirmMoveFiles N+1 消除 + 协议码常量 |
| `newcore/service/impl/FolderServiceImpl.java` | 协议码常量（41 处） |
| `newcore/service/impl/ResourceServiceImpl.java`、`MediaServiceImpl.java`、`FileChainServiceImpl.java`、`ExternalDownloadServiceImpl.java` | 协议码常量 |
| `newcore/controller/AuthController.java` | 协议码常量 |
| `.gitignore` | 补充 `logs/` |
| `SystemServiceImpl.java`、`FileChainServiceImpl.java`、`PropertiesRepository(+Impl)`、`PropertiesMapper.java` | Propertie → Property（含 getter/setter 更名） |
| `FolderViewServiceImpl.java` | SreachView → SearchView |
| `FileServiceImpl.java`、`FolderServiceImpl.java` | *Respons → *Response；SizeFormatUtil import 更新 |
| `ConfigurationManager.java` | SizeFormatUtil import 更新 + 移除同包冗余 import |
| `mc/FileSystemCommandHandler.java`、`mc/AccountCommandHandler.java` | SizeFormatUtil import 更新 |
| `ui/module/KiftdDynamicWindow.java` | 移除未使用 import `java.awt.Point`（IMP-001） |
| `newcore/service/impl/ResourceServiceImpl.java` | `sendResource` 移除未使用参数 `fname` + 2 处调用点（DEAD-001）；sendError 3 处补异常日志（EXC-001） |
| `server/util/FileBlockUtil.java` | `addFoldersToZipEntrySourceArray` 移除未使用参数 `account` + 3 处调用点（DEAD-001） |
| `ui/util/FilesTable.java`、`ui/module/FSViewer.java` | int 加法溢出死分支修复：`(long)` 先行强转激活 `> MAX_LIST_LIMIT` 防御（DEAD-002） |
| `server/util/KiftdProperties.java`、`server/util/FolderUtil.java` | 移除 `size()<Integer.MAX_VALUE` 恒真冗余子条件（DEAD-002） |
| `util/file_system_manager/FileSystemManager.java` | `saveToRecycleBin` 对齐健壮实现：null 保护 + 重名去重 + 异常记录（DEAD-003） |
| `server/util/AESCipher.java` | 删除冗余显式空构造器（DEAD-003） |
| `server/util/VerificationCodeFactory.java`、`server/util/CharsetDetectionObserverImpl.java` | 删除残留 IDE 自动生成 TODO 存根（DEAD-004） |
| `newcore/KiftdApplication.java`、`newcore/controller/GlobalExceptionHandler.java`、`server/util/ConfigurationManager.java` | 有意忽略的 catch 补充意图注释（EXC-003 专项化审计收尾） |
| `util/file_system_manager/pojo/FolderTreeNode.java`、`FileSystemFolderView.java` | PKG-002：原 Folder/FolderView 更名（消除与 server 包重名） |
| `util/file_system_manager/FileSystemManager.java` | PKG-002 引用类型 27 处更新；EXC-002 留档 Files.copy/move → `IOException` |
| `ui/module/FSViewer.java`、`ui/util/FilesTable.java`、`mc/FileSystemCommandHandler.java` | PKG-002 引用类型更新；EXC-003 空 catch 补意图注释 |
| `ui/module/KiftdDynamicWindow.java` | NAM-002 `settingp→settingProps`；EXC-003 缩放回退注释 |
| `newcore/service/impl/FolderServiceImpl.java`、`server/util/FolderUtil.java` | NAM-002 `ifc→constraintValue`；EXC-002 `parseInt` → `NumberFormatException` |
| `server/util/SizeFormatUtil.java` | EXC-002 解析回退 → `IllegalArgumentException`（×2） |
| `server/util/PasswordUtil.java` | EXC-002 PBKDF2 → `GeneralSecurityException` |
| `server/util/IpAddrGetter.java` | EXC-002 `InetAddress.getByName` → `UnknownHostException` |
| `newcore/service/SystemHealthService.java`、`newcore/service/StartupHealthChecker.java` | EXC-002 DB 连接 → `SQLException`；文件系统探测 → `IOException` |
| `server/util/ConfigurationManager.java` | EXC-002 精确化 8 处（端口/缓冲/https 解析 → NFE；原子写/WatchService/FileOutputStream → IOException；DB 连通 → `ReflectiveOperationException \| SQLException`） |
| `ui/util/PathsTable.java` | EXC-003 注释补全；修复上轮注释插入导致的匿名 Runnable 缺右括号 |
| `newcore/controller/SystemInfoController.java` | ARCH-001：4 个端点统一 ApiResponse 包装；移除内嵌 requireAdmin（认证移交 ApiAuthFilter） |
| `newcore/service/SystemHealthService.java` | ARCH-001：过期版本号残留 `"1.2.3-SNAPSHOT"` → `"1.3.0"` |
| `newcore/config/WebMvcConfig.java` | ARCH-001：@ServletComponentScan 补充扫描 `newcore.infrastructure`（修复 ApiAuthFilter/ApiPerformanceFilter 未注册） |
| `server/util/ConfigurationManager.java` | ARCH-001：hasSuperAuth 将内置 admin 账户识别为超级管理员（修复 `/api/*` 标准部署下对真实管理员 403 的缺陷） |
| `server/util/ServerTimeUtil.java` | DEAD-006：删除 `getTimeFromDateAccurateToDay`（含 import 清理） |
| `newcore/infrastructure/logging/ApiPerformanceFilter.java` | DEAD-006：删除 `resetStats` |
| `server/pojo/VideoTranscodeThread.java` | DEAD-006：删除 `abort` |
| `server/util/RangeFileStreamWriter.java` | DEAD-006：删除 `writeRangeFileHead`（7 参重载） |
| `server/util/RSAKeyUtil.java`、`server/util/TextFormateUtil.java` | DEAD-006：删除 `getKeySize` / `hasEscapes`（仅测试引用） |
| `server/util/ConfigurationManager.java` | DEAD-006/007：删除 `reTestServerPropertiesAndEffect`（孤儿包装）、`getPropertiesStatus`（与 getStatus 重复） |
| `ui/module/ServerUIModule.java` | DEAD-007：`getPropertiesStatus()` → `getStatus()` ×2 |
| `server/util/TxtCharsetGetter.java` | DEAD-007：删除 `getTxtCharset(byte[],int,int)` 重载 + `import java.util.Arrays` |
| `server/util/RetryUtil.java` | DEAD-007：删除 2 参泛型 / Runnable 重载（仅留 5 参完整版） |
| `server/util/FileBlockUtil.java` | DEAD-008：`createNewBlock` 不可达 else 换 UUID 重试分支删除 |
| `mc/FileSystemCommandHandler.java` | DEAD-008：`showCurrentFolder` 恒假 if（`Math.max(...) > Integer.MAX_VALUE`）删除 |
| `newcore/service/impl/AuthServiceImpl.java` | DEAD-008：`doPong` 冗余 else 简化为无 else return |
| `util/file_system_manager/FileSystemManager.java` | DEAD-009：删除 `MAX_FOLDERS_OR_FILES_LIMIT` 常量 + 4 处 `LIMIT 0,2147483647` 拼接 + 2 处上限对齐 `FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER(10000)` |
| `newcore/service/FileChainService.java`、`newcore/service/impl/FileChainServiceImpl.java` | DEAD-010：删除 `getChainKeyByFid`（接口+实现）+ 构造器 9→7 参数 + import 裁剪 |
| `server/util/SizeFormatUtil.java` | 5.16 教训：`formatFileSize(String)` 误删后恢复（Long.parseLong + 非法值原样返回） |
| `newcore/service/impl/FileServiceImpl.java` | DEAD-011：3 处 `< 0` 死分支删除（estimatedTotal/estimateFilesTotal/estimateFoldersTotal 恒非负）、getFileNameFormPath 恒真 `paths.length > 0` 简化、packTime 区间冗余下界删除、2 处冗余 else 简化 |
| `newcore/infrastructure/logging/ApiPerformanceFilter.java` | EXC-004：端点统计达上限时 computeIfAbsent 返回 null，判空后自增（修复 finally 块 NPE） |
| `server/util/FolderUtil.java` | DEAD-012：folderConstraint 重复 parseInt 合并为一次 + 循环内复用；getParentList Javadoc 修正为与实现一致 |
| `newcore/service/impl/FolderServiceImpl.java` | DEAD-012：folderConstraint 重复 parseInt 合并为一次（校验语义 `!= 0` 不变） |
| `server/util/FileBlockUtil.java` | DEAD-011/012：`else { continue; }` 无效果语句删除；zip 同名文件夹改名循环抽取私有方法 `deduplicateFoldersAndAddToZip`（顶层+递归层 DRY） |
| `server/util/RangeFileStreamWriter.java` | DEAD-011：`maxRate < Long.MAX_VALUE` 恒真子条件删除，三元表达式简化为 `maxRate > 0 && session != null` |

### 修改（测试）
| 文件 | 改动 |
|---|---|
| `integration/TestConfig.java` | 接入 ConfigurationManager 依赖 + 显式建表 |
| `integration/*IntegrationTest.java`（4 个） | `classes` 扩展 `TestConfig.class` |
| `server/mapper/NodeMapperTest.java` | +3 LIMIT 用例 |
| `server/util/ConfigurationManagerAccountTest.java` | +2 哈希契约用例 |
| `newcore/service/impl/FolderViewServiceImplTest.java` | mock 更新 `selectByParentFolderIdsLimit` |
| `server/model/PropertyTest.java` 等 4 个 POJO 测试 | 随类名重命名（PropertyTest/SearchViewTest/*ResponseTest） |
| `SystemServiceImplTest.java`、`FileChainServiceImplTest.java`、`PropertiesMapperTest.java`、`FolderServiceImplTest.java` | 引用类型/方法更名 |
| `integration/TestEnvironmentSetup.java`、`FolderServiceIntegrationTest.java` | 移除未使用 import（BeforeAll/QueryWrapper）（IMP-001） |
| `newcore/service/impl/FileServiceImplTest.java`、`MediaServiceImplTest.java` | 移除未使用 import（ArrayList/PictureViewList）（IMP-001） |
| `util/file_system_manager/FileSystemManagerTest.java` | 反射名/方法名随 `getFileFromBlocks` 重命名（NAM-001） |
| `newcore/service/impl/ResourceServiceImplTest.java` | mockStatic 引用 `getLastModifiedFromBlock`（NAM-001） |
| `newcore/service/impl/FileChainServiceImplTest.java` | DEAD-010：删除 12 个 `testGetChainKeyByFid_*`；构造器 9→7 参数 + import 清理 |
| `server/util/TxtCharsetGetterTest.java` | DEAD-007：删除 8 个 `getTxtCharset(byte[],int,int)` 测试（保留 InputStream 版） |
| `server/util/TextFormateUtilTest.java` | DEAD-006：删除 3 个 `testHasEscapes*` |
| `server/util/RSAKeyUtilTest.java` | DEAD-006：删除 `testGetKeySizeIs2048` |
| `server/util/RetryUtilTest.java` | DEAD-007：删除 `testRunnableOverload`；`testSuccessOnFirstAttempt` 改调 5 参版 |

### 删除
| 文件 | 说明 |
|---|---|
| `server/pojo/UploadKeyCertificate.java` | 未使用类：仅测试引用，无主代码消费者（DEAD-005） |
| `test/.../server/pojo/UploadKeyCertificateTest.java` | 随类删除（DEAD-005） |
| `newcore/infrastructure/storage/FileStorageService.java` + `impl/FileStorageServiceImpl.java` | 未使用抽象层：接口无任何消费者（DEAD-005） |
| `newcore/infrastructure/logging/LogService.java` + `impl/LogServiceImpl.java` | 未使用抽象层：接口无任何消费者（DEAD-005） |

---

*本文档与 `TECH-DEBT-REGISTER.md`、`scripts/tech-debt-check.ps1`、`scripts/find-unused-imports.ps1` 配套使用，作为后续迭代的基线依据。*
