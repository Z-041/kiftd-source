# kiftd 桌面端 UI 设计规范文档

## 1. 设计原则

### 1.1 核心设计理念
- **简洁实用**：以门户功能为核心，避免过度装饰和复杂视觉效果
- **功能完整**：确保所有原有功能模块正常运行，功能完整无缺
- **响应迅速**：界面响应速度不低于原有水平
- **用户习惯**：交互逻辑符合用户使用习惯

### 1.2 设计风格
- 现代扁平化设计风格
- 卡片式布局结构
- 清晰的视觉层次
- 统一的交互反馈

---

## 2. 设计令牌系统 (Design Tokens)

### 2.1 色彩系统

#### 2.1.1 品牌色 (Brand Colors)
| 名称 | 色值 | 用途 |
|------|------|------|
| BRAND_50 | `#eef4ff` | 品牌色最浅 |
| BRAND_100 | `#d6e4ff` | 品牌色极浅 |
| BRAND_200 | `#adc8ff` | 品牌色很浅 |
| BRAND_300 | `#85a9ff` | 品牌色较浅 |
| BRAND_400 | `#5c8aff` | 品牌色略浅 |
| BRAND_500 | `#3366ff` | 品牌色标准 |
| BRAND_600 | `#1a53ff` | 主色调/PRIMARY |
| BRAND_700 | `#0d42e6` | 品牌色较深 |
| BRAND_800 | `#0a33b4` | 品牌色很深 |
| BRAND_900 | `#072382` | 品牌色最深 |

#### 2.1.2 功能色 (Functional Colors)
| 名称 | 色值 | 用途 |
|------|------|------|
| PRIMARY | `#1a53ff` | 主要操作、主按钮 |
| PRIMARY_HOVER | `#3366ff` | 主按钮悬停 |
| PRIMARY_ACTIVE | `#0d42e6` | 主按钮激活 |
| PRIMARY_LIGHT | `#eef4ff` | 主色浅色背景 |
| SUCCESS | `#00b42a` | 成功状态 |
| SUCCESS_HOVER | `#23c343` | 成功悬停 |
| SUCCESS_LIGHT | `#e8ffea` | 成功浅色背景 |
| WARNING | `#ff7d00` | 警告状态 |
| WARNING_HOVER | `#ff9a2e` | 警告悬停 |
| WARNING_LIGHT | `#fff3e8` | 警告浅色背景 |
| DANGER | `#f53f3f` | 危险/错误状态 |
| DANGER_HOVER | `#f76560` | 危险悬停 |
| DANGER_LIGHT | `#ffece8` | 危险浅色背景 |
| INFO | `#3491fa` | 信息状态 |
| INFO_LIGHT | `#e8f3ff` | 信息浅色背景 |

#### 2.1.3 文字色 (Text Colors)
| 名称 | 色值 | 用途 |
|------|------|------|
| TEXT_PRIMARY | `#1d2129` | 主要文字 |
| TEXT_SECONDARY | `#4e5969` | 次要文字 |
| TEXT_TERTIARY | `#86909c` | 三级文字 |
| TEXT_QUATERNARY | `#c9cdd4` | 四级文字 |
| TEXT_DISABLED | `#c9cdd4` | 禁用文字 |
| TEXT_PLACEHOLDER | `#a9aeb6` | 占位符文字 |
| TEXT_ON_PRIMARY | `#ffffff` | 主色背景上的文字 |

#### 2.1.4 背景色 (Background Colors)
| 名称 | 色值 | 用途 |
|------|------|------|
| BG_BODY / BG_PAGE | `#f7f8fa` | 页面背景 |
| BG_WHITE | `#ffffff` | 白色背景 |
| BG_CARD | `#ffffff` | 卡片背景 |
| BG_GRAY | `#f7f8fa` | 灰色背景 |
| BG_BUTTON | `#f7f8fa` | 按钮背景 |
| BG_BUTTON_HOVER | `#ffffff` | 按钮悬停背景 |
| BG_INPUT | `#ffffff` | 输入框背景 |
| BG_DISABLED | `#f2f3f5` | 禁用背景 |
| BG_TOOLTIP | `#4e5969` | 提示框背景 |

#### 2.1.5 边框色 (Border Colors)
| 名称 | 色值 | 用途 |
|------|------|------|
| BORDER | `#e5e6eb` | 默认边框 |
| BORDER_HOVER | `#c9cdd4` | 悬停边框 |
| BORDER_ACTIVE | `#1a53ff` | 激活边框 |
| BORDER_DISABLED | `#e5e6eb` | 禁用边框 |
| BORDER_DIVIDER | `#f2f3f5` | 分割线 |

#### 2.1.6 阴影色 (Shadow Colors)
| 名称 | 色值 | 用途 |
|------|------|------|
| SHADOW_1 | `rgba(0,0,0,8)` | 轻微阴影 |
| SHADOW_2 | `rgba(0,0,0,12)` | 中等阴影 |
| SHADOW_3 | `rgba(0,0,0,16)` | 较深阴影 |

### 2.2 字体系统 (Typography)

#### 2.2.1 字号 (Font Sizes)
| 名称 | 字号 | 用途 |
|------|------|------|
| FONT_SIZE_XS | 10px | 辅助文字、标签 |
| FONT_SIZE_SM | 12px | 次要文字、说明 |
| FONT_SIZE_BASE | 13px | 正文基础 |
| FONT_SIZE_MD | 14px | 正文、按钮 |
| FONT_SIZE_LG | 16px | 小标题 |
| FONT_SIZE_XL | 18px | 标题 |
| FONT_SIZE_2XL | 20px | 大标题 |
| FONT_SIZE_3XL | 24px | 页面标题 |
| FONT_SIZE_4XL | 28px | 特大标题 |
| FONT_SIZE_5XL | 32px | 超大标题 |

#### 2.2.2 字重 (Font Weights)
| 名称 | 值 | 用途 |
|------|----|------|
| FONT_WEIGHT_NORMAL | PLAIN | 常规 |
| FONT_WEIGHT_MEDIUM | BOLD | 中等 |
| FONT_WEIGHT_BOLD | BOLD | 粗体 |

### 2.3 间距系统 (Spacing)
| 名称 | 值 | 用途 |
|------|----|------|
| SPACING_0 | 0px | 无间距 |
| SPACING_1 | 2px | 极小间距 |
| SPACING_2 | 4px | 很小间距 |
| SPACING_3 | 6px | 小间距 |
| SPACING_4 | 8px | 较小间距 |
| SPACING_5 | 10px | 中小间距 |
| SPACING_6 | 12px | 中等间距 |
| SPACING_7 | 14px | 中大型间距 |
| SPACING_8 | 16px | 标准间距 |
| SPACING_9 | 18px | 较大间距 |
| SPACING_10 | 20px | 大间距 |
| SPACING_12 | 24px | 很大间距 |
| SPACING_14 | 28px | 特大间距 |
| SPACING_16 | 32px | 超大间距 |
| SPACING_20 | 40px | 巨大间距 |
| SPACING_24 | 48px | 特大型间距 |
| SPACING_32 | 64px | 最大间距 |

### 2.4 圆角系统 (Border Radius)
| 名称 | 值 | 用途 |
|------|----|------|
| RADIUS_NONE | 0px | 无圆角 |
| RADIUS_SM | 2px | 小圆角 |
| RADIUS_MD | 4px | 中圆角（默认） |
| RADIUS_LG | 6px | 大圆角 |
| RADIUS_XL | 8px | 较大圆角 |
| RADIUS_2XL | 12px | 很大圆角 |
| RADIUS_3XL | 16px | 特大圆角 |
| RADIUS_FULL | 999px | 完全圆角 |

### 2.5 边框宽度 (Border Width)
| 名称 | 值 | 用途 |
|------|----|------|
| BORDER_WIDTH_THIN | 1px | 细边框 |
| BORDER_WIDTH_DEFAULT | 1px | 默认边框 |
| BORDER_WIDTH_THICK | 2px | 粗边框 |

### 2.6 动效时长 (Duration)
| 名称 | 值 | 用途 |
|------|----|------|
| DURATION_FAST | 100ms | 快速动效 |
| DURATION_BASE | 200ms | 基础动效 |
| DURATION_SLOW | 300ms | 慢速动效 |
| DURATION_SLOWER | 500ms | 更慢动效 |

### 2.7 层级 (Z-Index)
| 名称 | 值 | 用途 |
|------|----|------|
| Z_INDEX_HIDDEN | -1 | 隐藏层级 |
| Z_INDEX_AUTO | 0 | 自动层级 |
| Z_INDEX_DROPDOWN | 1000 | 下拉菜单 |
| Z_INDEX_STICKY | 1020 | 粘性定位 |
| Z_INDEX_FIXED | 1030 | 固定定位 |
| Z_INDEX_MODAL_BACKDROP | 1040 | 模态框背景 |
| Z_INDEX_MODAL | 1050 | 模态框 |
| Z_INDEX_POPOVER | 1060 | 弹出框 |
| Z_INDEX_TOOLTIP | 1070 | 提示框 |
| Z_INDEX_TOAST | 1080 | 消息提示 |

---

## 3. 组件库规范 (Component Library)

### 3.1 按钮 (Buttons)

#### 3.1.1 按钮类型
| 类型 | 类名 | 用途 |
|------|------|------|
| 主按钮 | `createPrimaryButton` | 主要操作，如"确认"、"应用" |
| 次按钮 | `createSecondaryButton` / `createButton` | 次要操作，如"取消"、"返回" |
| 危险按钮 | `createDangerButton` | 危险操作，如"删除" |
| 成功按钮 | `createSuccessButton` | 成功操作 |
| 警告按钮 | `createWarningButton` | 警告操作 |
| 文字按钮 | `createTextButton` | 无背景文字按钮 |
| 链接按钮 | `createLinkButton` | 链接样式按钮 |

#### 3.1.2 按钮状态
- 默认 (Default)
- 悬停 (Hover)
- 激活 (Active)
- 禁用 (Disabled)

#### 3.1.3 按钮规格
- 圆角：4px (RADIUS_MD)
- 内边距：上下 4px，左右 8px
- 字号：13px (FONT_SIZE_BASE)

### 3.2 卡片 (Cards)

#### 3.2.1 卡片类型
| 类型 | 类名 | 用途 |
|------|------|------|
| 默认卡片 | `createCardPanel()` | 通用容器 |
| 内边距卡片 | `createCardPanel(padding)` | 带内边距的容器 |
| 悬停卡片 | `createHoverCardPanel()` | 带悬停效果的卡片 |
| 统计卡片 | `createStatCard()` | 数据统计展示 |

#### 3.2.2 卡片规格
- 背景：白色 (BG_CARD)
- 边框：1px 灰色 (BORDER)
- 圆角：6px (RADIUS_LG)

### 3.3 标签 (Labels)

#### 3.3.1 标签类型
| 类型 | 类名 | 用途 |
|------|------|------|
| 默认标签 | `createLabel()` | 普通文字 |
| 标题标签 | `createTitleLabel()` | 页面大标题 (24px) |
| 副标题标签 | `createHeadingLabel()` | 页面标题 (20px) |
| 次标题标签 | `createSubtitleLabel()` | 副标题说明 (12px) |
| 次要标签 | `createSecondaryLabel()` | 次要文字 |
| 三级标签 | `createTertiaryLabel()` | 三级文字 |
| 徽章 | `createBadge()` | 状态标记 |

### 3.4 输入组件 (Input Components)

#### 3.4.1 文本框
- 类名：`createTextField()`
- 边框：1px 圆角
- 聚焦状态：边框变为主色
- 内边距：上下 4px，左右 6px

#### 3.4.2 密码框
- 类名：`createPasswordField()`
- 回显字符：•
- 其他同文本框

#### 3.4.3 文本域
- 类名：`createTextArea()`
- 自动换行
- 其他同文本框

#### 3.4.4 下拉选择框
- 类名：`createComboBox()`
- 圆角边框
- 统一高度

#### 3.4.5 复选框
- 类名：`createCheckBox()`
- 无焦点框
- 透明背景

### 3.5 进度条 (Progress Bar)
- 类名：`createProgressBar()`
- 类型：primary / success / warning / danger
- 高度：4px (SPACING_2)
- 背景：灰色 (BG_GRAY)

### 3.6 其他组件

#### 3.6.1 分割线
- 水平分割线：`createHorizontalDivider()`
- 垂直分割线：`createVerticalDivider()`
- 颜色：BORDER_DIVIDER

#### 3.6.2 空状态
- 类名：`createEmptyState()`
- 图标 + 文字
- 灰色背景

#### 3.6.3 边框
- 圆角边框：`createRoundedBorder()`
- 空边框：`createEmptyBorder()`
- 标题边框：`createTitledBorder()`

---

## 4. 布局规范 (Layout)

### 4.1 页面结构
所有页面遵循统一的三段式布局结构：

```
┌─────────────────────────────┐
│          Header             │  标题区（白色背景）
├─────────────────────────────┤
│                             │
│          Content            │  内容区（灰色背景）
│                             │
├─────────────────────────────┤
│          Footer             │  底部操作区（白色背景）
└─────────────────────────────┘
```

### 4.2 页面间距
- 页面左右内边距：20px (SPACING_5)
- 页面上下内边距：20px (SPACING_5)
- 卡片间距：16px (SPACING_4)

### 4.3 卡片布局
- 内容区使用卡片承载
- 卡片圆角：6px (RADIUS_LG)
- 卡片内边距：16px (SPACING_4 或 SPACING_8)

---

## 5. 模块设计规范

### 5.1 主控制台 (ServerUIModule)
- 功能：服务器控制、状态监控、日志显示
- 布局：2×2 统计卡片 + 操作按钮区 + 日志区 + 底部状态栏
- 特点：门户式设计，信息一目了然

### 5.2 设置界面 (SettingWindow)
- 功能：服务器参数配置、安全选项
- 布局：标题区 + 滚动内容区（分组卡片）+ 底部操作按钮
- 特点：分组清晰，对齐整齐

### 5.3 文件管理器 (FSViewer)
- 功能：文件浏览、导入、导出、删除
- 布局：顶部工具栏 + 面包屑路径 + 文件列表表格 + 状态栏
- 特点：操作分组，路径清晰

### 5.4 路径管理 (FileSystemPathViewer)
- 功能：主文件系统路径、扩展存储区管理
- 布局：标题区 + 工具栏卡片 + 表格卡片 + 提示区
- 特点：层次分明，操作便捷

### 5.5 进度对话框 (FSProgressDialog)
- 功能：显示文件操作进度
- 布局：标题 + 进度信息 + 进度条 + 取消按钮
- 特点：简洁明了，反馈及时

---

## 6. 交互规范 (Interaction)

### 6.1 按钮交互
- 悬停：背景色变化
- 按下：背景色加深
- 禁用：灰色半透明，不可点击
- 光标：手型指针 (HAND_CURSOR)

### 6.2 输入框交互
- 默认：灰色边框
- 聚焦：主色边框
- 禁用：灰色背景

### 6.3 表格交互
- 选中行：浅蓝色背景 (PRIMARY_LIGHT)
- 表头：灰色背景，可点击排序
- 行高：28px

### 6.4 对话框交互
- 模态对话框阻塞操作
- 右上角关闭按钮
- 底部操作按钮右对齐

---

## 7. 响应式设计

### 7.1 缩放机制
- 基于 `proportion` 比例因子
- 支持 1.0 ~ 10.0 倍缩放
- 所有尺寸统一通过 `UITheme.scale()` 计算

### 7.2 字体缩放
- 所有字号通过 `UITheme.getFont(size)` 获取
- 自动应用缩放比例

### 7.3 间距缩放
- 所有间距通过 `UITheme.getSpacing(spacing)` 获取
- 自动应用缩放比例

---

## 8. 实现规范

### 8.1 代码结构
```
ui/
├── theme/
│   ├── UITheme.java              # 设计令牌
│   └── UIComponentFactory.java   # 组件工厂
├── module/
│   ├── KiftdDynamicWindow.java   # 窗口基类
│   ├── ServerUIModule.java       # 主控制台
│   ├── SettingWindow.java        # 设置界面
│   ├── FSViewer.java             # 文件管理器
│   ├── FileSystemPathViewer.java # 路径管理
│   └── FSProgressDialog.java     # 进度对话框
├── util/
│   ├── FilesTable.java           # 文件表格
│   └── PathsTable.java           # 路径表格
├── callback/                      # 回调接口
└── pojo/                          # 数据对象
```

### 8.2 开发规范
1. **统一使用组件工厂**：所有 UI 组件通过 `UIComponentFactory` 创建
2. **统一使用设计令牌**：所有颜色、字体、间距通过 `UITheme` 常量引用
3. **统一缩放处理**：所有尺寸通过 `UITheme.scale()` 计算
4. **保持功能完整**：UI 重构不改变业务逻辑
5. **线程安全**：UI 更新必须在 EDT 线程中执行

---

## 9. 设计文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| UITheme.java | `src/main/java/kohgylw/kiftd/ui/theme/UITheme.java` | 设计令牌系统 |
| UIComponentFactory.java | `src/main/java/kohgylw/kiftd/ui/theme/UIComponentFactory.java` | 组件工厂 |
| KiftdDynamicWindow.java | `src/main/java/kohgylw/kiftd/ui/module/KiftdDynamicWindow.java` | 窗口基类 |
| ServerUIModule.java | `src/main/java/kohgylw/kiftd/ui/module/ServerUIModule.java` | 主控制台 |
| SettingWindow.java | `src/main/java/kohgylw/kiftd/ui/module/SettingWindow.java` | 设置界面 |
| FSViewer.java | `src/main/java/kohgylw/kiftd/ui/module/FSViewer.java` | 文件管理器 |
| FileSystemPathViewer.java | `src/main/java/kohgylw/kiftd/ui/module/FileSystemPathViewer.java` | 路径管理 |
| FSProgressDialog.java | `src/main/java/kohgylw/kiftd/ui/module/FSProgressDialog.java` | 进度对话框 |

---

## 10. 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 2.0.0 | 2026-07-19 | 全面重构，全新现代化设计 |

---

*本文档为 kiftd 桌面端 UI 设计规范，所有界面开发均需遵循此规范。*
