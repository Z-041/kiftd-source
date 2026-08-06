# 技术债登记台账（Tech Debt Register）

> 本台账是技术债治理的**唯一事实来源**。所有已知技术债必须在此登记、打分、跟踪状态。
> 新增债务时：登记 ID → 按量化模型打分（见 `TECH-DEBT-ITERATION.md` 第 2 章）→ 指定优先级与责任人 → 定期复查。
> 关闭债务时：标注验证方式（回归测试 / 门禁结果）后置为「已修复」。

| 债务ID | 维度 | 描述 | 严重度S | 影响面I | 复发概率P | 债务分D=S×I×P | 修复成本C | 优先级D/C | 状态 | 迭代/日期 |
|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | 安全 | 账户密码明文落盘（conf/account.properties） | 5 | 5 | 5 | 125 | 2 | 62.5 | ✅ 已修复 | 2026-08 |
| SEC-002 | 安全 | 上传大小无兜底，配置缺失时可无限上传打满磁盘 | 4 | 4 | 4 | 64 | 1 | 64 | ✅ 已修复 | 2026-08 |
| PERF-001 | 性能 | 全目录搜索一次性加载全部文件节点（无 LIMIT） | 3 | 4 | 4 | 48 | 2 | 24 | ✅ 已修复 | 2026-08 |
| PERF-002 | 性能 | confirmMoveFiles 循环内重复查询目标目录（N+1） | 3 | 3 | 4 | 36 | 1 | 36 | ✅ 已修复 | 2026-08 |
| TEST-INFRA-001 | 测试/基建 | MOCK 环境下 @WebListener 不触发导致集成测试建表缺失 | 4 | 4 | 5 | 80 | 2 | 40 | ✅ 已修复 | 2026-08 |
| MAIN-001 | 可维护性 | AJAX 协议码以魔数散落 8 个文件约 80 处 | 3 | 4 | 4 | 48 | 2 | 24 | ✅ 已修复 | 2026-08 |
| TEST-002 | 测试 | 整体行覆盖率仅略高于门禁（≥25%），server.util 之外包覆盖偏低 | 3 | 3 | 4 | 36 | 3 | 12 | ✅ 已修复（整体行覆盖率 50.42% → 54.86%，新增 7 测试类 44 用例） | 2026-08 |
| ARCH-001 | 架构 | "/homeController/*.ajax" 与 "/api/*" 双协议通道并存，需统一 | 2 | 3 | 3 | 18 | 3 | 6 | ✅ 已修复（API 通道规范化） | 2026-08 |
| PERF-003 | 性能 | 下载限速锁粒度为全局/单账户，极端并发下有性能瓶颈 | 3 | 2 | 3 | 18 | 3 | 6 | ✅ 已修复（session 粒度 + wait/notifyAll 释放监视器） | 2026-08 |
| CODE-001 | 代码质量 | 启动器(mc/ui)与 UI 模块大量 System.out 直出（核心层已清零） | 2 | 2 | 3 | 12 | 3 | 4 | ✅ 已修复（审计关闭：CLI/UI 控制台交互豁免） | 2026-08 |
| DOC-001 | 文档 | 部分模块缺设计文档/接口契约文档（README/TECHNICAL-DOC 外） | 2 | 2 | 3 | 12 | 2 | 6 | ✅ 已修复（新增 docs/API-CONTRACT.md：双通道契约 + 20 协议码对照表 + 9 服务接口契约 + 40+ 端点映射） | 2026-08 |
| PKG-001 | 可维护性 | 拼写错误类名：Propertie/SreachView/Check*Respons 等 5 个类，跨主代码与测试传播 | 3 | 4 | 3 | 36 | 2 | 18 | ✅ 已修复 | 2026-08 |
| PKG-002 | 可维护性 | 重名类：util.file_system_manager.pojo.Folder/FolderView 与 server.model.Folder/server.pojo.FolderView 同名，易 import 混淆 | 2 | 3 | 3 | 18 | 3 | 6 | ✅ 已修复 | 2026-08 |
| PKG-003 | 架构 | 包归属混乱：SizeFormatUtil 孤悬顶层 kohgylw.kiftd.util；licenses 许可文本置于 java 源码目录 | 2 | 3 | 3 | 18 | 1 | 18 | ✅ 已修复 | 2026-08 |
| PKG-004 | 架构 | mc/ui/printer 旧架构包与 newcore 分层并存 | 2 | 3 | 3 | 18 | 3 | 6 | ✅ 已评估（printer 为基础设施保留；mc CLI 为离线运维必需；ui Swing 收敛需 Web 管理端，列入产品路线图） | 2026-08 |
| IMP-001 | 代码质量 | 未使用 import（5 处，主代码 1 + 测试 4），复制粘贴易扩散 | 1 | 3 | 3 | 9 | 1 | 9 | ✅ 已修复 | 2026-08 |
| IMP-002 | 代码质量 | import 排序无统一规范（java/org/com/kohgylw 混排）；测试普遍使用 static 通配符导入 | 1 | 3 | 4 | 12 | 3 | 4 | ✅ 已修复（规范+脚本门禁） | 2026-08 |
| NAM-001 | 可维护性 | 方法名拼写错误：`getFileFormBlocks`/`getLastModifiedFormBlock`（Form→From）、`cannel`/`canncel`→`cancel` | 2 | 2 | 2 | 8 | 1 | 8 | ✅ 已修复 | 2026-08 |
| NAM-002 | 可维护性 | 变量/异常参数命名风格不佳：`settingp`/`ifc`/`mfname`/`namelistObj`/`e1`/`e2`/`exc` 等缩写与匈牙利后缀 | 1 | 3 | 2 | 6 | 2 | 3 | ✅ 已修复 | 2026-08 |
| EXC-001 | 异常处理 | 核心层空 catch 吞异常：ResourceServiceImpl 3 处响应失败静默丢弃；LogUtil/ConfigurationManager 有意忽略无注释说明 | 3 | 3 | 2 | 18 | 1 | 18 | ✅ 已修复 | 2026-08 |
| EXC-002 | 异常处理 | 宽泛捕获 `catch (Exception)` 约 80 处：参数校验 parseInt 场景可精确化；复合业务块捕获过宽 | 2 | 3 | 2 | 12 | 2 | 6 | ✅ 已修复（120 处全量评估：23 处精确化 + 97 处保留登记理由） | 2026-08 |
| EXC-003 | 异常处理 | UI/mc 遗留层空 catch 吞异常（PathsTable/KiftdDynamicWindow/ServerUIModule/FSViewer 等 20+ 处），静默失败难排查 | 2 | 2 | 1 | 4 | 3 | 1.3 | ✅ 已修复（8 处补意图注释） | 2026-08 |
| DEAD-001 | 代码质量 | 未使用方法参数：sendResource.fname、addFoldersToZipEntrySourceArray.account（调用方传入后被忽略） | 1 | 2 | 2 | 4 | 1 | 4 | ✅ 已修复 | 2026-08 |
| DEAD-002 | 代码质量 | 死分支/恒假条件：FilesTable/FSViewer int 加法溢出致 `> Integer.MAX_VALUE` 恒假（隐藏溢出隐患）；KiftdProperties/FolderUtil `size()<MAX_VALUE` 恒真冗余子条件 | 2 | 2 | 2 | 8 | 1 | 8 | ✅ 已修复 | 2026-08 |
| DEAD-003 | 代码质量 | 冗余实现：saveToRecycleBin 双份（FileSystemManager 退化版缺 null 保护/异常记录）；AESCipher 显式空构造器 | 1 | 2 | 2 | 4 | 1 | 4 | ✅ 已修复 | 2026-08 |
| DEAD-004 | 代码质量 | 残留 IDE 自动生成 TODO 注释（VerificationCodeFactory/CharsetDetectionObserverImpl 各 1 处） | 1 | 1 | 2 | 2 | 1 | 2 | ✅ 已修复 | 2026-08 |
| DEAD-005 | 代码质量 | 未使用类/接口：UploadKeyCertificate（仅测试引用）；FileStorageService/LogService 抽象层（实现为 bean 但无任何消费者） | 2 | 2 | 2 | 8 | 2 | 4 | ✅ 已修复 | 2026-08 |
| DEAD-006 | 代码质量 | 未使用公共方法 7 处（无任何调用方）：ServerTimeUtil.getTimeFromDateAccurateToDay、ApiPerformanceFilter.resetStats、VideoTranscodeThread.abort、RangeFileStreamWriter.writeRangeFileHead、RSAKeyUtil.getKeySize、TextFormateUtil.hasEscapes、ConfigurationManager.reTestServerPropertiesAndEffect | 1 | 2 | 2 | 4 | 1 | 4 | ✅ 已修复 | 2026-08 |
| DEAD-007 | 代码质量 | 重复方法/孤儿包装：ConfigurationManager.getPropertiesStatus 与 getStatus 逻辑完全一致（重复）；重载冗余：TxtCharsetGetter.getTxtCharset(byte[],int,int)、RetryUtil 2 参泛型与 Runnable 重载均无生产调用方 | 1 | 2 | 2 | 4 | 1 | 4 | ✅ 已修复 | 2026-08 |
| DEAD-008 | 代码质量 | 不可达/恒假分支：FileBlockUtil.createNewBlock 的 else 换 UUID 重试分支（appendIndex 恒真不可达）；FileSystemCommandHandler.showCurrentFolder `Math.max(...) > Integer.MAX_VALUE` 恒假；AuthServiceImpl.doPong 冗余 else | 2 | 2 | 2 | 8 | 1 | 8 | ✅ 已修复 | 2026-08 |
| DEAD-009 | 代码质量 | FileSystemManager 冗余上限防御：`LIMIT 0,2147483647` 恒等拼接（4 处 SQL）与 `MAX_FOLDERS_OR_FILES_LIMIT=Integer.MAX_VALUE` 防御值形同虚设；对齐为全局业务上限 FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER(10000) | 2 | 2 | 2 | 8 | 1 | 8 | ✅ 已修复 | 2026-08 |
| DEAD-010 | 架构 | 双份实现冗余：FileChainService.getChainKeyByFid（旧数据层 PropertiesMapper+AESCipher）与 SystemServiceImpl.getFileChainKey（新数据层）业务重复，前端实际走新路径，删除旧副本 | 2 | 2 | 2 | 8 | 1 | 8 | ✅ 已修复 | 2026-08 |
| DEAD-011 | 代码质量 | 恒假/恒真条件与冗余：FileServiceImpl 3 处 `< 0` 死分支（estimatedTotal/estimateFilesTotal/estimateFoldersTotal 恒非负）、getFileNameFormPath 恒真 `paths.length > 0`、packTime 区间判断冗余下界、2 处冗余 else；FileBlockUtil `else { continue; }` 无效果；RangeFileStreamWriter `maxRate < Long.MAX_VALUE` 恒真子条件 | 1 | 3 | 2 | 6 | 1 | 6 | ✅ 已修复 | 2026-08 |
| DEAD-012 | 代码质量 | 重复实现/过时注释：FolderUtil/FolderServiceImpl 同一 folderConstraint 重复 Integer.parseInt 两次；FileBlockUtil zip 内同名文件夹改名循环逐字重复两处（已抽取私有方法）；FolderUtil.getParentList Javadoc 描述与实现不符 | 1 | 2 | 2 | 4 | 1 | 4 | ✅ 已修复 | 2026-08 |
| EXC-004 | 异常处理 | ApiPerformanceFilter 端点统计达 MAX_ENDPOINT_ENTRIES(100) 后 computeIfAbsent 返回 null，`.incrementAndGet()` 在 finally 块中触发 NPE 破坏请求 | 2 | 3 | 3 | 18 | 1 | 18 | ✅ 已修复（判空后自增） | 2026-08 |

## 已修复债务验证方式

| 债务ID | 验证方式 | 结果 |
|---|---|---|
| SEC-001 | 单元测试 `ConfigurationManagerAccountTest` 新增哈希写入契约 + 明文迁移兼容用例；`PasswordUtil` PBKDF2(100k)+恒定时间比较 | 通过 |
| SEC-002 | `WebMvcConfig.multipartConfigElement` 兜底 50GB/52GB 绝对上限 | 通过 |
| PERF-001 | 单测 `FolderViewServiceImplTest` 校验 mock 调用 `selectByParentFolderIdsLimit`；`NodeMapperTest` 3 个 LIMIT 用例 | 通过 |
| PERF-002 | 单测 `FileServiceImplTest`（行为等价重构） | 通过 |
| TEST-INFRA-001 | `mvn failsafe:integration-test` 80 用例全绿（修复前 9 失败） | 通过 |
| MAIN-001 | `scripts/tech-debt-check.ps1` 的 Ajax-protocol-literal 规则为 0；`mvn clean verify` 全绿 | 通过 |
| PKG-001 | 类名重命名后 `mvn clean verify` 全绿（单测 895 + 集成 80）；残留扫描 `\b(Propertie|SreachView|Respons)\b` = 0 | 通过 |
| PKG-003 | SizeFormatUtil 迁移至 server.util 后 4 个引用方编译通过；licenses 移至 resources/licenses/ | 通过 |
| IMP-001 | `scripts/find-unused-imports.ps1` 普通未使用 import 候选 = 0；`mvn clean verify` 全绿 | 通过 |
| NAM-001 | 方法重命名后残留扫描 `\b(getFileFormBlocks|getLastModifiedFormBlock|cannel|canncel)\b` = 0；`mvn clean verify` 全绿 | 通过 |
| EXC-001 | 核心层空 catch 全部补日志/注释；`mvn clean verify` 全绿 | 通过 |
| DEAD-001 | 参数移除后 2 处调用点同步更新；`mvn clean verify` 全绿 | 通过 |
| DEAD-002 | 死分支修正（long 加法激活防御逻辑）后 `mvn clean verify` 全绿；残留恒真子条件扫描 = 0 | 通过 |
| DEAD-003 | saveToRecycleBin 对齐健壮实现；AESCipher 空构造器删除；`mvn clean verify` 全绿 | 通过 |
| DEAD-004 | TODO 注释删除后 `tech-debt-check.ps1` TODO 计数降至 0 | 通过 |
| DEAD-005 | 删除 6 个文件（4 主代码 + 1 测试 + 1 关联）后全库无引用；`mvn clean verify` 全绿 | 通过 |
| DEAD-006 | 7 处方法删除后残留扫描 = 0（getTimeFromDateAccurateToDay/resetStats/abort/writeRangeFileHead/getKeySize/hasEscapes/reTestServerPropertiesAndEffect）；`mvn clean verify` 全绿 | 通过 |
| DEAD-007 | getPropertiesStatus 删除后 UI 回调接口（GetServerStatus 由 UIRunner 实现）保留存活；重载删除后调用点同步；残留扫描 = 0；`mvn clean verify` 全绿 | 通过 |
| DEAD-008 | 分支简化后 `mvn clean verify` 全绿；残留恒假模式扫描 = 0 | 通过 |
| DEAD-009 | LIMIT 拼接移除（4 处 SQL）+ 上限对齐 10000 后 `mvn clean verify` 全绿；残留 `MAX_FOLDERS_OR_FILES_LIMIT` 扫描 = 0 | 通过 |
| DEAD-010 | getChainKeyByFid 删除后残留扫描 = 0；FileChainServiceImpl 构造器 9→7 参数调用点（测试）同步；`mvn clean verify` 全绿 | 通过 |
| DEAD-011 | 分支简化后 `mvn clean verify` 全绿；残留恒假模式扫描（`< 0` 死分支/恒真下界）= 0 | 通过 |
| DEAD-012 | 重复解析合并 + DRY 抽取后 `mvn clean verify` 全绿；FolderUtil/FolderServiceImpl 行为等价（原有 校验语义 `> 0`/`!= 0` 不变） | 通过 |
| EXC-004 | 判空后自增修复 + `mvn clean verify` 全绿；ApiPerformanceFilter 端点超限场景不再 NPE | 通过 |
| PERF-003 | `VariableSpeedBufferedOutputStream` session 粒度锁 + wait/notifyAll（预算耗尽时释放监视器）；并发测试 `testConcurrentWritesSharedSessionCompleteAllData` 验证同会话多任务无死锁且完整写出 | 通过 |
| PKG-002 | 重名类 Folder→FolderTreeNode、FolderView→FileSystemFolderView 重命名后 `mvn clean verify` 全绿；残留扫描 = 0 | 通过 |
| IMP-002 | `scripts/check-import-order.ps1` 违规 = 0（java→jakarta/javax→org/com→kohgylw 分组 + 组内 Ordinal 排序）；`mvn clean verify` 全绿 | 通过 |
| NAM-002 | 变量/异常参数命名规范化（全名替代缩写、去匈牙利后缀）后 `mvn clean verify` 全绿 | 通过 |
| EXC-002 | 120 处宽泛捕获全量评估：23 处精确化（NFE/IOE/SQLException/GeneralSecurityException/UnknownHostException 等）+ 97 处保留理由登记（迭代文档 5.14 节）；`mvn clean verify` 全绿 | 通过 |
| EXC-003 | UI/mc 层 8 处空 catch 补意图注释（PathsTable/FSViewer 等）；`mvn clean verify` 全绿 | 通过 |
| CODE-001 | 审计关闭：newcore+server 核心层 System.out 静态扫描 = 0；mc/ui CLI 控制台交互场景豁免（Printer 输出中枢 + 启动提示符） | 通过 |
| ARCH-001 | 新增 `ApiAuthFilter`（/api/* 统一管理员认证：401/403 JSON）；SystemInfoController 4 端点统一 ApiResponse 包装；`@ServletComponentScan` 补扫 `newcore.infrastructure`；hasSuperAuth 内置 admin 识别为超级管理员；`SystemInfoControllerTest`(+5) + `ApiAuthFilterTest`(+3) + `SystemApiIntegrationTest`(+4)；`mvn clean verify` BUILD SUCCESS（单测 897 + 集成 84）；三脚本全 PASS；前端零改动 | 通过 |
| TEST-002 | 新增 7 测试类 44 用例（SystemHealthServiceTest 5 / StartupHealthCheckerTest 5 / CryptoServiceImplTest 6 / ApiPerformanceFilterTest 4 / FileNodeRepositoryImplTest 12 / FolderRepositoryImplTest 8 / PropertiesRepositoryImplTest 4）；`mvn clean verify` 920 单测全绿；整体行覆盖率 50.42% → 54.86% | 通过 |
| DOC-001 | 新增 `docs/API-CONTRACT.md`：双通道架构概览 + ApiResponse<T> 结构 + 全部 20 个 AJAX 协议码对照表 + 9 个服务接口方法级契约 + 8 控制器 40+ 端点映射表 + 契约变更流程；与源码逐项核对一致 | 通过 |
| PKG-004 | 盘点 mc(5 文件 1435 行)/ui(14 文件 2723 行)/printer(2 文件 117 行)；结论：printer 为基础设施、mc CLI 为离线运维必需、ui Swing 收敛属产品功能（需开发 Web 管理端）→ 置「已评估」，UI 收敛列入产品路线图 | 通过 |

## 使用说明

- **评分模型**：D = 严重度(1-5) × 影响面(1-5) × 复发概率(1-5)；优先级 = D ÷ 修复成本(1-3)。
  - P0：D/C ≥ 50（安全/数据类，立即处理）
  - P1：25 ≤ D/C < 50（高影响，本迭代处理）
  - P2：10 ≤ D/C < 25（中影响，近期处理）
  - P3：D/C < 10（低影响，持续优化）
- **监控**：每次提交前执行 `powershell -ExecutionPolicy Bypass -File scripts/tech-debt-check.ps1`；详见迭代文档第 7 章。
