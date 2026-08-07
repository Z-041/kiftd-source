# 桌面端 UI 问题跟踪与优化记录

> 适用范围：kiftd 桌面端（Java Swing）管理界面，即 `src/main/java/kohgylw/kiftd/{mc,ui}` 包（**非** webContext 网页端）。
> 更新频率：每次迭代修复后更新状态与对比效果。

## 一、问题总览（按优先级）

| ID | 优先级 | 分类 | 问题摘要 | 位置 | 状态 |
|----|--------|------|----------|------|------|
| UI-001 | P0 | 稳定性 | 进度监听线程每 16ms 直接操作 Swing 组件（非 EDT） | FSProgressDialog | 已修复 |
| UI-002 | P0 | 稳定性 | 模态进度对话框在非 EDT 线程中显示 | FSProgressDialog / FSViewer / FileSystemPathViewer | 已修复 |
| UI-003 | P0 | 稳定性 | updateServerStatus() 工作线程直接操作 Swing 组件 | ServerUIModule | 已修复 |
| UI-004 | P0 | 稳定性 | getServerStatus() 工作线程直接操作 Swing 组件 | SettingWindow | 已修复 |
| UI-005 | P0 | 稳定性 | worker 线程中调用按钮启用/禁用与表格刷新 | FSViewer / FilesTable | 已修复 |
| UI-006 | P0 | 稳定性 | 应用设置工作线程直接操作 Swing 组件 | SettingWindow | 已修复 |
| UI-007 | P1 | 正确性 | 日志时间使用 12 小时制（hh），混淆上下午 | ServerUIModule | 已修复 |
| UI-008 | P1 | 正确性 | 退出程序无确认，误点直接关停服务器 | ServerUIModule | 已修复 |
| UI-009 | P1 | 正确性 | 未启动时端口显示 0（应为默认配置端口） | ServerUIModule | 已修复 |
| UI-010 | P1 | 正确性 | 端口/缓存非法输入仅打印到日志，无弹窗提示 | SettingWindow | 已修复 |
| UI-011 | P1 | 交互 | 进度条误导（0/50/100 块级跳变）；取消后监听线程悬挂访问已销毁组件 | FSProgressDialog | 已修复 |
| UI-012 | P2 | 视觉 | 全局字体为宋体，现代系统观感陈旧 | KiftdDynamicWindow | 已修复 |
| UI-013 | P2 | 视觉 | 未启用系统外观（Look&Feel），默认 Metal 主题生硬 | UIRunner | 已修复 |
| UI-014 | P2 | 视觉 | 输出区文字灰色，对比度不足 | ServerUIModule | 已修复 |
| UI-015 | P2 | 视觉 | 按钮文字符号装饰过重（如"开启(Start)>>"） | ServerUIModule / FSViewer | 已修复 |
| UI-016 | P2 | 布局 | 设置窗口各行 FlowLayout 居中，标签/控件不对齐 | SettingWindow | 已修复 |
| UI-017 | P2 | 布局 | 分辨率比例下限 clamp 为 1.0，小屏幕窗口溢出 | KiftdDynamicWindow | 已修复 |
| UI-018 | P2 | 交互 | 托盘菜单"退出"置顶、无隐藏气泡提示 | ServerUIModule | 已修复 |
| UI-019 | P2 | 交互 | 主界面不显示访问地址，用户不知如何访问 | ServerUIModule | 已修复 |
| UI-020 | P3 | 交互 | 文件列表表头排序无方向指示 | FilesTable | 已修复 |
| UI-021 | P3 | 性能 | PathsTable 每次刷新新建线程 | PathsTable | 已修复 |
| UI-022 | P3 | 性能 | 输出区每行插入都做行数检查与清理 | ServerUIModule | 已修复 |
| UI-023 | P3 | 交互 | 主窗口/设置按钮无键盘快捷键 | ServerUIModule / SettingWindow | 已修复 |

## 二、问题详情与修复方案

### P0 稳定性（Swing 线程违规）

- **UI-001 / UI-011 FSProgressDialog 监听线程**：`while(listen){pBar.setValue(...);message.setText(...);sleep(16)}` 高频直改组件。
  方案：改用 `javax.swing.Timer`（EDT 线程执行，间隔 100ms），仅当值变化时更新；进度条改为 indeterminate 模式；`cancel()` 时同步置 `listen=false` 并停止定时器，避免悬挂线程访问已销毁组件。
- **UI-002 模态对话框非 EDT 显示**：`new Thread(() -> fsd.show()).start()` 在非 EDT 线程显示模态窗口，违反 Swing 规则。
  方案：全部改为 `SwingUtilities.invokeLater(fsd::show)`。
- **UI-003 updateServerStatus**：工作线程内直接 `setText/setEnabled`。
  方案：UI 更新整体用 `SwingUtilities.invokeLater` 包裹。
- **UI-004 SettingWindow.getServerStatus**：同上。
  方案：同上。
- **UI-005 FSViewer worker 内操作按钮/表格**：`doImport/export/delete/双击` 等在工作线程中直接启用禁用按钮、`FilesTable.updateValues`、`window.setTitle`。
  方案：`disableAllButtons/enableAllButtons` 与 `FilesTable.updateValues` 内部做 EDT 检测；`window.setTitle` 用 invokeLater。
- **UI-006 应用设置线程**：`window.setVisible(false)` 非 EDT。
  方案：invokeLater。

### P1 正确性 / 交互

- **UI-007 时间 12 小时制**：`SimpleDateFormat("yyyy-MM-dd hh:mm:ss")` → 改为 `HH`。
- **UI-008 退出无确认**：`exit()` 增加确认对话框；服务器运行中时给出明确警告。
- **UI-009 端口显示 0**：未启动时 `getPort()` 可能为 0，回退显示 `getInitProt()`。
- **UI-010 非法输入静默失败**：端口/缓存解析异常时弹窗提示并保持窗口打开。
- **UI-011 见上**。

### P2 视觉 / 布局 / 交互

- **UI-012 字体**：Windows 下改为"微软雅黑"，其余系统回退宋体。
- **UI-013 外观**：启动时 `UIManager.setLookAndFeel(系统外观)`，跟随 Windows 原生风格。
- **UI-014 输出区灰字**：改用深灰 `#37474F`。
- **UI-015 按钮文字**：去除 `>> || ~> [*] [/] [X]` 等符号装饰。
- **UI-016 设置窗口对齐**：由 `GridLayout+FlowLayout(居中)` 改为 `GridBagLayout` 表单布局（标签右对齐、控件左对齐）。
- **UI-017 比例下限**：`clamp(0.8)`，允许小屏按比例缩小。
- **UI-018 托盘菜单**：调整顺序（显示→文件→退出），隐藏窗口时气泡提示。
- **UI-019 访问地址**：端口行展示 `http://localhost:端口`。

### P3 后续迭代

- UI-020 表头排序方向指示（已实现）、UI-021 PathsTable 线程（已实现）、UI-022 输出区清理策略（已实现）、UI-023 快捷键（已实现）。

## 三、修复前后对比

| 维度 | 修复前 | 修复后 |
|------|--------|--------|
| 线程模型 | 多处非 EDT 操作 Swing，存在随机崩溃/卡顿风险 | 全部 UI 更新收敛到 EDT |
| 字体 | 宋体（渲染陈旧） | Windows 下微软雅黑 |
| 主题 | Metal 默认主题 | 系统外观（Windows 原生） |
| 布局 | 设置窗口控件参差居中 | 表单式左对齐 |
| 反馈 | 非法输入静默、退出无确认 | 弹窗提示 + 退出确认 |
| 进度 | 块级跳变、16ms 高频刷新 | indeterminate + 100ms EDT 刷新 |
| 排序 | 点击表头排序无方向提示 | 表头显示 ▲/▼ 方向指示 |
| 日志区 | 每行插入都全文拷贝定位 | 按批次滚动、文档长度定位 |
| 操作方式 | 仅鼠标操作 | 支持 Alt+字母 快捷键 |

## 四、用户反馈记录（待补充）

- 请在实际使用中反馈：布局、字号、配色、交互习惯等。
