# Life HUD v0.5.3 开发任务书
## `/tasks` 行动台重构

> 版本：`v0.5.3`
>
> 主题：**Tasks / 行动台**
>
> 本版本属于 v0.5 Direction 阶段的体验补丁，重点重构 `/tasks` 页面。
>
> 核心目标：
>
> **让任务不再像数据库记录，而像今天真正要去做掉的行动。**

---

# 0. 版本定位

当前 `/tasks` 已经具备：

- 每日任务
- 特殊任务
- 完成状态
- Dream / Goal / DreamMilestone 关联能力
- LifeEvent 联动
- Growth 已与业务模块解耦

但当前 UI 存在明显问题：

```text
一张超宽大 Card
+
一行一个任务
+
已完成 / 未完成文本
+
右侧“独立任务 / 关联”
```

整体更像：

```text
SELECT * FROM tasks
```

经过轻度美化后的展示。

这与 Life HUD 的产品定位不符。

本版本需要把 `/tasks` 从：

```text
任务数据库列表
```

重构为：

```text
行动台 / Action Desk
```

它首先回答：

> **今天真正还有什么要去做？**

其次才回答：

> 这些任务属于什么类型、与哪个 Dream / Goal 有关系。

---

# 1. 产品定义

## 1.1 Tasks

Task 的定义：

> **完成一件具体的事。**

它和 Ritual 的边界继续保持：

```text
Task
= 完成一件事

Ritual
= 进入一种状态
```

因此 `/tasks` 的核心应该是：

```text
行动
完成
推进
```

而不是：

```text
字段
关联
状态文字
```

---

## 1.2 页面定位

`/tasks` 不是：

- 后台管理页
- 数据表浏览页
- Dream 关联配置页
- Jira / Trello 仿制品

它应该是：

> **Life HUD 中面向“行动”的桌面。**

打开页面时，最重要的是一眼知道：

- 今天还有多少任务没完成
- 哪些已经完成
- 哪些是日常
- 哪些是特殊行动
- 哪些正在推进某个 Dream / Goal

---

# 2. 本轮最高优先级

## P0：必须完成

- `/tasks` 从行式列表改为卡片式行动台
- 每个 Task 自己成为一个独立视觉对象
- 未完成任务成为默认视觉重点
- 已完成任务明显弱化
- 完成操作直接出现在 Task Card 上
- 移除当前“独立任务 / 关联”常驻右侧操作
- Dream / Goal / DreamMilestone 关联改为“意义展示”
- 每日任务与特殊任务产生明显不同的视觉节奏
- 页面顶部增加今日完成状态
- 默认首屏更聚焦“今天真正要做的任务”
- 保留现有 Task 数据与后端业务，不大改模型

## P1：尽量完成

- Today / Daily / Special 分区
- Direction chip
- 已完成区域折叠
- Task 快速编辑
- hover 微交互
- 轻量筛选
- 完成时间展示
- 空状态优化

## P2：可以后补

- 拖拽排序
- 自定义任务颜色
- 看板模式
- 日历视图
- 复杂批量操作
- 高级任务搜索
- 完整任务历史页

---

# 3. 本版核心原则

## 3.1 任务是“对象”，不是“行”

禁止继续：

```text
大容器
├── Task row
├── Task row
├── Task row
└── Task row
```

推荐：

```text
Task Card
Task Card
Task Card
Task Card
```

每个 Task 自己拥有：

- 标题
- 类型
- 状态
- Direction
- 操作
- 必要的少量元数据

---

## 3.2 行动优先于配置

页面主操作优先级：

```text
完成任务
>
查看详情
>
编辑任务
>
修改关联
```

不能再让：

```text
关联
```

成为每条任务最显眼的操作之一。

---

## 3.3 Direction 是“意义”，不是“外键”

当前：

```text
独立任务    关联
```

这种文案过于数据库化。

改成：

### 未关联

默认不展示任何显眼内容。

必要时只显示：

```text
独立行动
```

作为极弱标签。

### 已关联

展示实际意义，例如：

```text
✦ Life HUD
```

或：

```text
✦ Life HUD
  完成主体生活业务
```

进一步可展开：

```text
Dream
Life HUD

Goal
完成主体生活业务

Milestone
v0.5 Direction
```

不要显示：

```text
dreamId = 3
goalId = 7
```

也不要持续显示：

```text
关联
```

这个动作只应该出现在编辑 Task 时。

---

# 4. 页面信息架构

推荐：

```text
/tasks

├── Tasks Header
├── Today Summary
├── Today / Active
├── Daily
├── Special
└── Completed
```

不要求一次全部复杂化。

最小可用版本建议：

```text
Tasks Header
↓
Today Summary
↓
Daily
↓
Special
↓
Completed
```

---

# 5. 顶部 Header

保留：

```text
任务 · Tasks
```

增加一句轻量状态：

```text
今天还有 5 件事
```

或：

```text
3 / 8 已完成
```

不需要做复杂统计页。

建议：

```text
任务 · Tasks

今天还有 5 件事
3 / 8 已完成
━━━━━━━━━━━━━━░░░░░
```

Progress Bar 要克制。

它是：

> 今日行动状态

不是：

> KPI 面板

---

# 6. Today Summary

数据来源：

```text
今日相关 Task
```

至少统计：

```text
total
completed
remaining
```

如果当前 Task 模型没有严格“今日任务”概念，可以先按：

- Daily
- 当前激活的 Special
- 有 today / deadline = today 的 Task

组成。

不要为了这个模块重构整套任务调度系统。

---

# 7. Daily / 每日任务

## 7.1 定位

Daily 是：

> 高频、短小、每天重复出现的行动。

例如：

```text
学习 Python
健身
阅读 30 分钟
散步 20 分钟
整理桌面
喝够 2000ml 水
早于 11 点起床
无短视频日
```

这些任务通常不需要占一整行 1400px。

---

## 7.2 布局

推荐：

```text
2 ~ 4 列紧凑卡片
```

根据窗口宽度响应式。

示意：

```text
┌─────────────────┐  ┌─────────────────┐
│ ○ 学习 Python    │  │ ✓ 整理桌面       │
│ 每日             │  │ 今日 18:32       │
│                  │  │                  │
│      [ 完成 ]     │  │       完成       │
└─────────────────┘  └─────────────────┘
```

卡片不要过高。

重点：

```text
标题
完成状态
操作
```

其他信息尽量轻。

---

# 8. Special / 特殊任务

## 8.1 定位

Special 通常更接近：

> 值得专门去做的一次生活行动。

例如：

```text
进入心流状态 1 小时
连续编程 90 分钟
读书两章节
去没去过的地方散步
骑行 10km
尝试从未吃过的店
主动和朋友聊 30 分钟
```

这些任务比 Daily 更有“事件感”。

---

## 8.2 布局

Special Card 可以：

- 更宽
- 信息更多
- 允许显示 Direction
- 允许显示一句描述
- 允许显示 deadline

例如：

```text
┌────────────────────────────────────┐
│ ◇ 去没去过的地方散步               │
│                                    │
│ 特别行动                           │
│ ✦ 梦想 · 活得热烈                  │
│                                    │
│                         [ 完成 ]    │
└────────────────────────────────────┘
```

推荐：

```text
1 ~ 2 列
```

不要和 Daily 使用完全相同的卡片密度。

---

# 9. Task 类型视觉标记

不要使用巨大的分类标签。

建议使用轻量符号：

```text
○ DAILY
◇ SPECIAL
✦ DREAM / DIRECTION
```

示例：

```text
○ 阅读 30 分钟

◇ 骑行 10km

✦ 完成 v0.5 验收
```

图标 / 符号可以继续复用 Life HUD 当前视觉语言。

---

# 10. Task Card

建议基础结构：

```text
TaskCard
├── Status Control
├── Title
├── Meta
├── Direction
└── Actions
```

---

## 10.1 未完成 Task

默认视觉重点。

例如：

```text
○ 学习 Python
每日

[完成]
```

要求：

- 标题清晰
- 状态明显
- 完成按钮易点
- 不要堆大量次要字段

---

## 10.2 已完成 Task

完成后：

- Card opacity 降低
- 标题轻微弱化
- 状态使用 ✓
- 可显示完成时间
- 不再显示大型“完成”按钮

例如：

```text
✓ 学习 Python
今日 18:32
```

不要只在下面写：

```text
已完成
```

---

# 11. 完成操作

Task 的核心动作：

```text
完成
```

必须在 Card 上直接操作。

建议：

```text
○ → 点击
↓
✓
```

或：

```text
[完成]
```

完成后立即：

- 更新 Card
- 更新 Today Summary
- 更新 LifeEvent
- Growth 继续由 Growth Engine 消费
- 不整页刷新

如果后端已有完成 API，直接复用。

不要重写完成业务。

---

# 12. 已完成任务区域

默认页面应该优先展示：

```text
还没完成什么
```

所以已完成任务不能继续与未完成任务同等占满主区域。

建议：

```text
今天完成了 · 3
```

可：

- 折叠
- 默认展开但弱化
- 放在每个分区底部

推荐优先：

```text
折叠 / 弱化
```

避免已完成的 8 条 Daily 把仍未完成的 2 条任务挤出首屏。

---

# 13. Direction 展示

## 13.1 没有关联

不要再显示：

```text
独立任务    关联
```

默认：

```text
不显示
```

如果需要识别，可以极弱显示：

```text
独立行动
```

---

## 13.2 已有关联

推荐：

```text
✦ Life HUD
```

hover / click 后显示完整路径：

```text
Dream
Life HUD

Goal
完成主体生活业务

Milestone
v0.5 Direction
```

也可以压成：

```text
✦ Life HUD › v0.5 Direction
```

视当前宽度决定。

---

## 13.3 编辑关联

关联操作只存在于：

```text
Task Edit Modal / Drawer
```

例如：

```text
关联方向（可选）

Dream
Goal
DreamMilestone
```

不要在主列表常驻：

```text
关联
```

---

# 14. Task 编辑

建议点击：

```text
⋯
```

或卡片编辑按钮打开：

```text
TaskEditorModal
```

支持：

```text
title
description
type
priority
deadline
Dream / Goal / DreamMilestone
```

以当前已有字段为准。

不要为了 UI 重构新增大量 Task 业务字段。

---

# 15. Task 创建

页面顶部提供：

```text
+ 新建任务
```

可以：

- Header 右侧
- Today 区顶部
- Floating Button

建议保持 Life HUD 当前按钮样式。

创建流程继续复用现有 API。

---

# 16. 任务分区建议

第一版至少：

```text
每日任务 · Daily

特别行动 · Special
```

如果工程量可控，可以增加：

```text
今天 · Today
```

其中 Today 聚合：

- 今日活跃 Daily
- 用户主动选择到今天的 Special
- deadline today
- Direction Task

但本版不要为了 Today 重构后端日程系统。

---

# 17. 空状态

Daily 空：

```text
今天还没有固定日常。

[添加每日任务]
```

Special 空：

```text
还没有特别行动。

去做一点和平常不一样的事吧。

[添加特别行动]
```

Completed 空：

```text
今天还没有完成任务。
```

不要：

```text
No data
```

---

# 18. 响应式

桌面端：

```text
Daily
2~4 列

Special
1~2 列
```

平板：

```text
2 列
```

手机：

```text
1 列
```

要求：

- 不横向溢出
- 按钮不挤压
- Direction chip 可换行
- 卡片高度自适应

---

# 19. 前端组件建议

推荐：

```text
TasksPage
│
├── TasksHeader
├── TodayTaskSummary
│
├── DailyTaskSection
│   └── TaskCard
│
├── SpecialTaskSection
│   └── TaskCard
│
├── CompletedTaskSection
│   └── TaskCard
│
└── TaskEditorModal / Drawer
```

如果当前项目已有通用 Card：

```text
复用
```

不要重新造第二套 Design System。

---

# 20. 后端改动原则

本版本：

> **尽量前端重构，后端最小修改。**

现有：

```text
Task
Task completion
Task Direction
LifeEvent
Growth
```

原则上全部复用。

只有当前 API 无法支撑展示时才增加 DTO / 查询字段。

禁止：

```text
重写 Task Service
重建 Task 表
改 Growth 逻辑
重做 LifeEvent
```

---

# 21. DTO 建议

如果当前 Task DTO 暴露方式不方便前端，可以增加聚合字段：

```text
direction
```

例如：

```json
{
  "dreamId": 1,
  "dreamTitle": "Life HUD",
  "goalId": 2,
  "goalTitle": "完成主体生活业务",
  "milestoneId": 3,
  "milestoneTitle": "v0.5 Direction"
}
```

前端不要：

```text
根据 ID 再发 3 次请求拼名称
```

如果当前 API 已有类似 `DirectionInfo`，优先复用。

---

# 22. 完成状态更新

确保完成操作保持幂等。

同一 Task：

```text
complete
complete
```

不能：

```text
重复 TASK_COMPLETED
重复 Growth
```

继续遵循 v0.4 / v0.5 已建立的事件边界。

---

# 23. LifeEvent

本版不新增复杂事件类型。

继续使用：

```text
TASK_COMPLETED
```

UI 重构不得改变：

```text
Task
↓
LifeEvent
↓
Growth Engine
```

关系。

不要在前端 / Task Service 直接修改 EXP 或 Energy。

---

# 24. 视觉语言

继续使用 Life HUD 当前：

- 白 / 浅蓝
- 半透明 Card
- 毛玻璃
- Summer Sky 背景
- 柔和阴影
- 中低对比边框
- 现有圆角体系

本版关键词：

```text
清晰
轻量
行动感
有层级
不像后台
```

不需要像 `/now` 歌单那样高度沉浸。

Tasks 是行动页，应该比 Now 更明确、更利落。

---

# 25. 明确禁止

本版不要开发：

```text
Kanban
Trello
拖拽排序
完整日历
Gantt
任务依赖图
复杂 recurring rule
多人协作
提醒系统
Push Notification
任务聊天
AI 自动拆任务
Agent
复杂统计
积分奖励
Coin
Shop
Buff
```

不要把 `/tasks` 做成项目管理平台。

---

# 26. 测试要求

## Daily

- Daily 正常展示
- 未完成状态正确
- 完成后 UI 即时变化
- 已完成任务弱化
- 刷新后状态保持

## Special

- Special 正常展示
- Direction 正常显示
- 独立 Special 不显示奇怪“关联”
- 完成正常

## Direction

- 无关联 Task 正常
- Dream 关联正常
- Goal 关联正常
- DreamMilestone 关联正常
- Direction 名称正确
- 编辑关联成功
- 主页面不再常驻“关联”按钮

## Summary

- total 正确
- completed 正确
- remaining 正确
- 完成任务后即时更新

## Regression

确认：

```text
Dream
Goal
DreamMilestone
Ritual
Now
Growth
LifeEvent
```

无明显回归。

---

# 27. 真实验收流程

## Flow A：Daily

准备：

```text
学习 Python
健身
阅读 30 分钟
整理桌面
```

其中：

```text
学习 Python → 未完成
健身 → 未完成
阅读 30 分钟 → 已完成
整理桌面 → 已完成
```

检查：

- 未完成任务优先可见
- 完成任务明显弱化
- 页面能一眼扫清今天状态
- 不再表现为四行数据库记录

---

## Flow B：Special

准备：

```text
骑行 10km
去没去过的地方散步
尝试从未吃过的店
```

要求：

- 卡片比 Daily 更舒展
- 有明显“特别行动”感
- 完成按钮清晰

---

## Flow C：Direction

创建：

```text
Dream
Life HUD

Goal
完成主体生活业务

DreamMilestone
完成 v0.5 Direction

Task
完成 v0.5 验收
```

绑定后检查主页面。

应该看到：

```text
✦ Life HUD
```

或：

```text
✦ Life HUD › v0.5 Direction
```

而不是：

```text
关联
```

---

## Flow D：完成任务

点击：

```text
完成 v0.5 验收
```

完成。

确认：

```text
Card 立即变为完成状态
Summary 更新
TASK_COMPLETED 只生成一次
Growth 无重复消费
```

---

# 28. 视觉验收

完成后必须满足：

- 首屏不再是一整张超长表格
- 每个 Task 有独立视觉边界
- Daily 更紧凑
- Special 更舒展
- 未完成比已完成更醒目
- “完成”是主要操作
- Direction 是信息，不是后台按钮
- 页面能快速看出今天剩余任务
- 大面积无意义横向空白明显减少
- 整体依旧属于 Life HUD

---

# 29. 版本完成标准

v0.5.3 只有满足以下条件才算完成：

- `/tasks` 已从行列表重构为行动台
- Task 使用独立 Card
- Daily / Special 有不同布局节奏
- 已完成 / 未完成视觉层级清晰
- 完成操作可直接在 Card 上执行
- Today Summary 可正确统计
- “独立任务 / 关联”常驻操作已移除
- Direction 改为真实 Dream / Goal / Milestone 信息展示
- 编辑 Task 时仍可以修改 Direction
- 页面响应式基本正常
- Task 业务数据不丢失
- LifeEvent / Growth 边界不被破坏
- 后端测试通过
- 前端无明显控制台错误
- 版本号更新为 `v0.5.3`

---

# 30. 最终提交要求

开发完成后输出：

```text
1. v0.5.3 实际完成内容
2. Tasks 页面新信息架构
3. TaskCard 结构
4. Daily / Special 差异
5. Direction 展示与编辑方式
6. 后端 / DTO 是否有调整
7. 完成操作与 LifeEvent 验证
8. 测试结果
9. 已知问题
10. 与任务书的偏差及原因
```

不要只输出：

```text
v0.5.3 已完成
```

需要留下完整可验收报告。

---

# 31. 一句话版本目标

> **Tasks 不是一张任务表。**
>
> **它应该是一张打开就知道“今天还要去做什么”的行动台。**
