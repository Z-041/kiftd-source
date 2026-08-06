# newcore 服务层接口契约文档（API Contract）

> 文档版本：1.0 ｜ 关联债务：DOC-001 ｜ 适用范围：`src/main/java/kohgylw/kiftd/newcore/service` 服务层与 `newcore/controller` 端点
> 本契约以**当前代码实现**为准（2026-08 迭代快照），任何协议码/端点变更必须同步更新本文档与前端 `webContext/`。

---

## 1. 双通道概览

kiftd 对外提供两个 HTTP 通道，职责明确分工（详见 `TECHNICAL-DOC.md` 6.4 节）：

| 通道 | 路径前缀 | 认证方式 | 响应格式 | 使用方 |
|---|---|---|---|---|
| AJAX 通道（主） | `/homeController/*.ajax` 等 | Session（`ACCOUNT` 会话属性） | 纯文本：JSON 字符串或协议码（`text/html; charset=utf-8`） | Web UI（`home.js` 等） |
| API 通道（程序化） | `/api/*` | Session + 超级管理员（`ApiAuthFilter`） | `ApiResponse<T>` JSON 包装 | 外部程序/监控脚本 |

### 1.1 API 通道统一响应包装（`ApiResponse<T>`）

```json
{ "success": true, "code": "SUCCESS", "message": null, "data": { ... }, "timestamp": 1786000000000 }
```

- `code` 取值见 `ResultCode` 枚举（`newcore/domain/ResultCode.java`），如 `UNAUTHORIZED`、`FORBIDDEN`、`NOT_FOUND`、`INTERNAL_SERVER_ERROR`。
- 未登录访问 `/api/*` → HTTP **401**（`UNAUTHORIZED`）；已登录非管理员 → HTTP **403**（`FORBIDDEN`）。

---

## 2. AJAX 协议码对照表

> 来源：`newcore/domain/AjaxProtocol.java`（唯一事实来源，MAIN-001 收敛后全部服务/控制器引用常量）。
> **字符串值属于前后端线上契约，修改前必须同步调整前端。**

| 常量名 | 字符串值 | 语义 | 主要产出方 |
|---|---|---|---|
| `SUCCESS` | `SUCCESS` | 操作成功 | 通用 |
| `ERROR` | `ERROR` | 通用失败 | 通用 |
| `NOT_FOUND` | `NOT_FOUND` | 资源不存在 | 通用 |
| `NOT_ACCESS` | `notAccess` | 无访问权限（对目标资源） | 文件夹/文件访问控制 |
| `NO_AUTHORIZED` | `noAuthorized` | 未授权（需登录） | 权限校验 |
| `ERROR_PARAMETER` | `errorParameter` | 参数错误（缺失/非法/约束不符） | 文件夹/文件参数校验 |
| `FOLDERS_TOTAL_OUT_OF_LIMIT` | `foldersTotalOutOfLimit` | 文件夹数量超出单目录上限（10000） | 新建文件夹 |
| `NAME_OCCUPIED` | `nameOccupied` | 名称已被同级占用 | 新建/重命名文件夹 |
| `CREATE_FOLDER_SUCCESS` | `createFolderSuccess` | 创建文件夹成功 | 新建文件夹 |
| `CANNOT_CREATE_FOLDER` | `cannotCreateFolder` | 无法创建文件夹 | 新建文件夹 |
| `DELETE_FOLDER_SUCCESS` | `deleteFolderSuccess` | 删除文件夹成功 | 删除文件夹 |
| `CANNOT_DELETE_FOLDER` | `cannotDeleteFolder` | 无法删除文件夹 | 删除文件夹 |
| `RENAME_FOLDER_SUCCESS` | `renameFolderSuccess` | 重命名文件夹成功 | 重命名文件夹 |
| `DELETE_ERROR` | `deleteError` | 删除出错（参数/权限） | 按名称删除文件夹 |
| `DELETE_SUCCESS` | `deleteSuccess` | 删除成功（通用） | 按名称删除文件夹 |
| `FILES_TOTAL_OUT_OF_LIMIT` | `filesTotalOutOfLimit` | 文件数量超出单目录上限（10000） | 上传校验 |
| `DELETE_FILE_SUCCESS` | `deleteFileSuccess` | 删除文件成功 | 删除文件 |
| `CANNOT_DELETE_FILE` | `cannotDeleteFile` | 无法删除文件 | 删除文件 |
| `RENAME_FILE_SUCCESS` | `renameFileSuccess` | 重命名文件成功 | 重命名文件 |
| `CANNOT_MOVE_FILES` | `cannotMoveFiles` | 无法移动文件（目标重名/越权等） | 移动文件 |

> 另有少量**非协议码**响应不经过 `AjaxProtocol`（仍为线上契约）：登录/注册/改密返回 `permitlogin`/`accountpwderror`/`needsubmitvercode`/`accountnotfound`/`error` 等（见 `AuthServiceImpl`）；`getServerOS.ajax` 返回 OS 名文本；`getPackTime.ajax` 返回耗时数值；`ping.ajax` 返回 `pong`。

---

## 3. 服务层接口契约

> 服务实现类位于 `newcore/service/impl/`，均标注 `@Service`（`FolderServiceImpl`/`CryptoServiceImpl` 为 `@Primary` 默认实现）。
> 所有方法均同步操作 H2/MySQL（MyBatis Plus）与文件系统文件块，事务边界见各实现 `@Transactional`。

### 3.1 AuthService（认证）

| 方法 | 返回 | 语义与契约 |
|---|---|---|
| `login(request, session)` | `OperationResult` | RSA 密文登录；成功 `success`，失败 `failure(code)`（`accountpwderror`/`needsubmitvercode`/`accountnotfound`/`error` 等）。成功后会话写入 `ACCOUNT`。 |
| `logout(session)` | `void` | 清除 `ACCOUNT` 会话属性。 |
| `getPublicKeyJson()` | `String` | 返回 `{publicKey, timestamp}` JSON（RSA 公钥用于前端加密）。 |
| `getVerificationCode(request, response, session)` | `void` | 输出验证码图片流并写入会话。 |
| `changePassword(request)` | `OperationResult` | RSA 密文改密；`PASSWORD_CHANGE_NOT_ALLOWED`/`PASSWORD_TOO_WEAK` 等。 |
| `doPong(request)` | `String` | 心跳：已登录返回 `pong`。 |
| `isAllowSignUp()` | `boolean` | 是否开放注册（`authSignup` 配置）。 |
| `signUp(request)` | `OperationResult` | RSA 密文注册；成功自动登录。 |

### 3.2 FolderService（文件夹）

| 方法 | 返回 | 语义与契约 |
|---|---|---|
| `newFolder(request)` | `String` | 参数 `parentId`/`folderName`/`folderConstraint`；协议码 `CREATE_FOLDER_SUCCESS`/`ERROR_PARAMETER`/`NO_AUTHORIZED`/`NAME_OCCUPIED`/`FOLDERS_TOTAL_OUT_OF_LIMIT`/`CANNOT_CREATE_FOLDER`。 |
| `deleteFolder(request)` | `String` | 参数 `folderId`（禁删 `root`）；`DELETE_FOLDER_SUCCESS`/`CANNOT_DELETE_FOLDER`/`NO_AUTHORIZED`/`ERROR_PARAMETER`；级联删除子文件夹与文件块。 |
| `renameFolder(request)` | `String` | 参数 `folderId`/`newName`/`folderConstraint`；`RENAME_FOLDER_SUCCESS`/`NAME_OCCUPIED`/`ERROR_PARAMETER`/`NO_AUTHORIZED`；约束不得低于父级。 |
| `deleteFolderByName(request)` | `String` | 参数 `parentId`/`folderName`；`DELETE_SUCCESS`/`DELETE_ERROR`；删除全部同名文件夹。 |
| `createNewFolderByName(request)` | `String` | 参数同上；返回 `CreateNewFolderByNameResponse` JSON（`result` 字段：`success`/`error`/`foldersTotalOutOfLimit`）。 |
| `getFolderCountResult(request)` | `String` | 参数 `fid`；返回 `FolderCountResult` JSON（`folders`/`files` 计数）。 |

### 3.3 FileService（文件）

| 方法 | 返回 | 语义与契约 |
|---|---|---|
| `checkUploadFile(request, response)` | `String` | 参数 `parentFolderId`/`fileSize`/`fileNameList`；校验上限（10000）与权限；`FILES_TOTAL_OUT_OF_LIMIT`/`ERROR_PARAMETER`/`NO_AUTHORIZED`。 |
| `doUploadFile(request, response, file)` | `String` | multipart 上传；`SUCCESS`/`ERROR`/`FILES_TOTAL_OUT_OF_LIMIT`/`NO_AUTHORIZED`；写入文件块 + 节点。 |
| `deleteFile(request)` | `String` | 参数 `fileId`；`DELETE_FILE_SUCCESS`/`CANNOT_DELETE_FILE`。 |
| `doDownloadFile(request, response)` | `void` | 参数 `fileId`；支持 Range 断点续传；权限校验失败写错误流。 |
| `doRenameFile(request)` | `String` | 参数 `fileId`/`newName`；`RENAME_FILE_SUCCESS`/`ERROR_PARAMETER`。 |
| `deleteCheckedFiles(request)` | `String` | 批量删除 `fileIdList`；`DELETE_FILE_SUCCESS`/`CANNOT_DELETE_FILE`。 |
| `getPackTime(request)` | `String` | 参数 `fileIdList`；返回预计打包耗时。 |
| `downloadCheckedFiles(request)` | `String` | 批量下载校验；`SUCCESS`/`ERROR`/`NO_AUTHORIZED`。 |
| `downloadCheckedFilesZip(request, response)` | `void` | ZIP 打包下载（zt-zip，断点续传）。 |
| `confirmMoveFiles(request)` | `String` | 移动预检（N+1 已消除）；`SUCCESS`/`CANNOT_MOVE_FILES`。 |
| `doMoveFiles(request)` | `String` | 执行移动；`SUCCESS`/`CANNOT_MOVE_FILES`。 |
| `checkImportFolder(request)` | `String` | 导入校验；`CheckImportFolderResponse` JSON。 |
| `doImportFolder(request, file)` | `String` | multipart 导入文件夹树；`SUCCESS`/`ERROR`。 |

### 3.4 FolderViewService（目录视图）

| 方法 | 返回 | 语义与契约 |
|---|---|---|
| `getFolderViewJson(folderId, session, request)` | `String` | 目录视图 JSON（`folderView` + 文件夹/文件列表，含权限标记）。 |
| `getRemainingFolderViewJson(request)` | `String` | 移动/复制目标选择视图（排除自身子树）。 |
| `getSearchViewJson(request)` | `String` | 全路径搜索（LIMIT 500 下推）；`SearchView` JSON。 |

### 3.5 MediaService（预览/媒体）

| 方法 | 返回 | 语义与契约 |
|---|---|---|
| `getPlayVideoJson(request)` | `String` | 视频播放信息 JSON（含转码状态 `VideoInfo`）。 |
| `getPreviewPictureJson(request)` | `String` | 图片预览列表 JSON（`PictureViewList`）。 |
| `getCondensedPicture(request, response)` | `void` | 缩略图流输出（Thumbnailator）。 |

### 3.6 ResourceService（资源/文本/公告）

| 方法 | 返回 | 语义与契约 |
|---|---|---|
| `getResource(fid, request, response)` | `void` | 文件资源流（下载/播放，Range 支持；无权限 403）。 |
| `getVideoTranscodeStatus(request)` | `String` | 转码状态轮询（`waiting`/`converting`/`finished` 等）。 |
| `getLRContextByUTF8(fileId, request, response)` | `void` | 文本内容（jchardet 编码探测）。 |
| `getNoticeMD5()` | `String` | 公告 MD5（前端轮询变更）。 |
| `getNoticeContext(request, response)` | `void` | 公告内容输出。 |

### 3.7 ExternalDownloadService（外链下载）

| 方法 | 返回 | 语义与契约 |
|---|---|---|
| `getDownloadKey(request)` | `String` | 生成临时分享 Key（参数 `fileId`）。 |
| `downloadFileByKey(request, response)` | `void` | 按 Key 免登录下载；Key 无效/过期返回错误。 |

### 3.8 FileChainService（永久资源链）

| 方法 | 返回 | 语义与契约 |
|---|---|---|
| `getResourceByChainKey(request, response)` | `void` | 永久链下载（`chain/{fileName}`，需 `openFileChain=OPEN`，AES+主密钥校验）。 |

### 3.9 SystemService / SystemHealthService / StartupHealthChecker（系统）

| 方法 | 返回 | 语义与契约 |
|---|---|---|
| `SystemService.getOSName()` | `String` | `os.name` 系统属性。 |
| `SystemService.getFileChainKey(request)` | `String` | 生成永久资源链 Key（`openFileChain=OPEN` 时；否则提示）。 |
| `SystemHealthService.getHealthStatus()` | `Map` | 数据库/文件系统/内存/磁盘/运行时长检查；`status` = `UP`/`DOWN`。 |
| `SystemHealthService.getMetrics()` | `Map` | 堆/非堆内存、线程数、磁盘、请求统计（`ApiPerformanceFilter`）、异常统计（`GlobalExceptionHandler`）。 |
| `StartupHealthChecker.performHealthCheck(context)` | `boolean` | 启动期 DB 连接 + 文件系统读写自检。 |

---

## 4. 控制器端点 → 服务方法映射

| 控制器 | 端点 | 服务方法 |
|---|---|---|
| `AuthController` (`/homeController`) | `getPublicKey.ajax` / `doLogin.ajax` / `getNewVerCode.do` / `doLogout.ajax` / `doChangePassword.ajax` / `ping.ajax` / `askForAllowSignUpOrNot.ajax` / `doSigUp.ajax` | `AuthService.*` |
| `FolderController` (`/homeController`) | `getFolderView.ajax` / `getRemainingFolderView.ajax` / `newFolder.ajax` / `deleteFolder.ajax` / `renameFolder.ajax` / `countFolderContent.ajax` / `createNewFolderByName.ajax` / `deleteFolderByName.ajax` | `FolderViewService` / `FolderService` |
| `FileController` (`/homeController`) | `douploadFile.ajax` / `checkUploadFile.ajax` / `deleteFile.ajax` / `downloadFile.do` / `renameFile.ajax` / `deleteCheckedFiles.ajax` / `getPackTime.ajax` / `downloadCheckedFiles.ajax` / `downloadCheckedFilesZip.do` / `confirmMoveFiles.ajax` / `moveCheckedFiles.ajax` / `sreachInCompletePath.ajax` / `checkImportFolder.ajax` / `doImportFolder.ajax` | `FileService` / `FolderViewService` |
| `MediaController` (`/homeController`) | `playVideo.ajax` / `getPrePicture.ajax` / `showCondensedPicture.do` | `MediaService` |
| `ResourceController` (`/resourceController`) | `getResource/{fileId}` / `getLRContext/{fileId}` / `getVideoTranscodeStatus.ajax` / `getNoticeMD5.ajax` / `getNoticeContext.do` | `ResourceService` |
| `ExternalLinksController` (`/externalLinksController`) | `getDownloadKey.ajax` / `downloadFileByKey/{fileName}` / `chain/{fileName}` | `ExternalDownloadService` / `FileChainService` |
| `AdminController` (`/homeController`) | `getServerOS.ajax` / `getFileChainKey.ajax` | `SystemService` |
| `SystemInfoController` (`/api/system`) | `info` / `stats` / `health` / `metrics` | `SystemHealthService`（`ApiResponse` 包装） |
| `WelcomeController` | `/` | 重定向 `home.html` |

---

## 5. 契约变更流程

1. 修改 `AjaxProtocol` / `ResultCode` 常量值 → 必须同步更新前端 `webContext/` 对应分支与本文档第 2 章；
2. 新增/修改端点 → 更新本文档第 3、4 章与服务实现 Javadoc；
3. 运行 `scripts/tech-debt-check.ps1`（协议码魔数 = 0）确认服务层未引入裸字面量；
4. 运行 `mvn clean verify` 全量回归（协议码相关测试断言与契约一致）。

---

*本文档与 `TECHNICAL-DOC.md`（第 6 章 API 文档）配套使用；第 6 章偏部署/使用视角，本文档偏服务层实现契约视角。*
