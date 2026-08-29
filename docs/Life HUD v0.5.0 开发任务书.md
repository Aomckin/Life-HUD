# Life HUD v0.5.0 开发任务书

## 0. 版本定位

版本：

```text
v0.5.0
```

主题：

```text
Direction
梦想 / 目标 / 任务 / 仪式 / 「现在。」
```

本版本目标不是继续扩展 Growth，也不是提前开发 Life / Media / Agent。

这一版只回答三个问题：

```text
我想去哪里？
我想怎么生活？
现在的我是什么样？
```

完成后，Life HUD 应当从：

```text
记录我做了什么
```

进一步变成：

```text
记录我正在追逐什么
记录我想以怎样的方式生活
记录此刻的我处于怎样的人生阶段
```

---

# 1. 本轮最高优先级约束

## 1.1 不破坏 v0.4 Growth 架构

v0.4 已确定：

```text
业务模块
↓
产生事实 LifeEvent
↓
Growth Engine
↓
计算 Energy / EXP / Achievement / Title
```

因此 v0.5 禁止重新出现：

```text
TaskService.complete()
→ 直接修改 EXP

RitualService.complete()
→ 直接修改 Energy

DreamMilestone.complete()
→ 直接写 Level
```

必须保持：

```text
Task / Dream / Ritual
只负责业务事实

Growth
负责消费事实并派生成长结果
```

本版本可以新增 Growth 将来需要消费的事件，但不要为了 v0.5 重新大改 Growth 规则。

---

## 1.2 LifeEvent 是跨模块连接点

本轮所有值得进入生活时间线的重要行为，都必须生成 LifeEvent。

至少新增或确认支持：

```text
DREAM_CREATED
DREAM_COMPLETED

GOAL_CREATED
GOAL_COMPLETED

DREAM_MILESTONE_COMPLETED

TASK_COMPLETED

RITUAL_COMPLETED

NOW_SNAPSHOT_CREATED
```

事件必须保留可靠的：

```text
source
sourceId
title
occurredAt
metadata
```

同一业务动作不得因为重复请求产生重复完成事件。

---

# 2. 命名约束

v0.4 Growth 已存在：

```text
Milestone
```

它代表：

> 用户选择保留的人生重要成长节点。

v0.5 梦想体系中的 Milestone 代表：

> 一个 Goal 下的阶段性拆解节点。

因此代码层禁止两个概念都直接命名为 `Milestone`。

统一：

```text
GrowthMilestone / Milestone
→ 保留现有 v0.4 实现，不随意改名

DreamMilestone
→ v0.5 梦想拆解节点
```

前端文案仍可显示：

```text
里程碑
```

---

# 3. 模块 A：Tasks & Dreams

目标结构：

```text
Dream
  ↓
Goal
  ↓
DreamMilestone
  ↓
Task
```

但：

```text
Task
```

必须继续支持完全独立存在。

不得强迫普通任务绑定 Dream。

---

# 4. Dream

## 4.1 数据模型

实现 Dream。

建议核心字段：

```text
id

title
description
meaning

status

createdAt
updatedAt

targetDate

coverImage / coverImagePath

note
```

状态：

```text
ACTIVE
PAUSED
COMPLETED
ARCHIVED
```

要求：

- title 必填
- description 可选
- meaning 用于记录“为什么想做到”
- targetDate 可为空
- coverImage 可为空
- Dream 删除优先采用归档语义
- 已关联大量 Goal / Task 的 Dream 不应被粗暴物理删除

---

# 5. Goal

Goal 是 Dream 下较大的可执行方向。

例如：

```text
Dream
写完自己的小说

Goal
完成第一卷
```

建议：

```text
Goal
├── id
├── dreamId
├── title
├── description
├── status
├── targetDate
├── sortOrder
├── createdAt
└── updatedAt
```

状态可以统一：

```text
ACTIVE
PAUSED
COMPLETED
ARCHIVED
```

要求：

- Goal 必须属于某个 Dream
- Dream 页面可以看到其全部 Goal
- 支持排序
- 支持完成 / 暂停 / 恢复
- Goal 完成时生成事实事件
- 不直接修改 EXP / Energy

---

# 6. DreamMilestone

结构：

```text
Dream
  ↓
Goal
  ↓
DreamMilestone
```

例：

```text
Dream：写完小说

Goal：完成第一卷

DreamMilestone：完成第一章
```

建议字段：

```text
id
goalId

title
description

status

targetDate
completedAt

sortOrder

createdAt
updatedAt
```

至少状态：

```text
PENDING
COMPLETED
ARCHIVED
```

完成 DreamMilestone：

```text
1. 修改业务状态
2. 记录 completedAt
3. 生成 DREAM_MILESTONE_COMPLETED
4. 后续由 Growth Engine 决定是否消费
```

不要：

```text
DreamMilestone.complete()
→ exp += 500
```

---

# 7. Task 改造

基于当前已有 Task 实现增量升级。

不要推翻现有 Task 系统重新做一套。

## 7.1 保留现有任务能力

至少保留：

```text
普通任务
每日任务
特殊任务

截止时间
优先级
状态
完成时间
已有备注 / 描述能力
```

如果当前项目已经有其他可靠字段，也不要无理由删除。

---

## 7.2 增加 Direction 关联

Task 可以：

```text
完全独立
```

也可以关联：

```text
Dream
Goal
DreamMilestone
```

建议优先保证：

```text
dreamId
goalId
dreamMilestoneId
```

均可为空。

需要做关联一致性校验。

例如：

如果：

```text
dreamMilestoneId = 10
```

则该 milestone 所属 Goal / Dream 与 Task 指定的 goalId / dreamId 不得互相冲突。

可以由 Service 自动向上推导关联，避免用户重复选择三层。

推荐交互：

```text
关联到：完成第一章

Life HUD 自动知道：
完成第一章
→ Goal：完成第一卷
→ Dream：写完小说
```

---

## 7.3 Task 完成行为

继续生成：

```text
TASK_COMPLETED
```

并让 metadata 可以携带：

```text
dreamId
goalId
dreamMilestoneId
priority
taskType
```

本模块不直接结算 Growth。

---

# 8. Dreams 前端

路由：

```text
/dreams
```

不要做成传统后台管理表格。

它应该更像：

> 我当前正在追逐什么。

---

## 8.1 Dreams 首页

至少提供：

```text
Active Dreams
Paused Dreams
Completed Dreams
Archived Dreams
```

默认首先展示 ACTIVE。

Dream 卡片建议展示：

```text
封面
标题
一句 meaning
目标日期
Goal 完成情况
最近推进情况
状态
```

不需要做复杂百分比算法。

可以使用最简单的：

```text
已完成 DreamMilestone / 总 DreamMilestone
```

作为参考。

没有 Milestone 时不要显示假的 0%。

---

## 8.2 Dream Detail

进入 Dream 后展示：

```text
Dream 标题
意义 meaning
描述
状态
目标日期
封面

Goals
  ├── Goal
  │    ├── DreamMilestone
  │    ├── DreamMilestone
  │    └── Related Tasks
  │
  └── Goal

未直接归属 Goal 的关联 Task
```

支持：

```text
新增 Goal
新增 DreamMilestone
新增关联 Task

编辑
暂停
完成
归档
```

---

# 9. Tasks 前端升级

路由继续使用：

```text
/tasks
```

任务列表不要被 Dream 体系绑架。

普通任务依然是第一公民。

增加：

```text
关联方向
```

展示，例如：

```text
今晚写 1000 字

写完小说
› 第一卷
› 第一章
```

任务编辑 Modal / Form 增加可选关联选择器。

建议采用逐级选择：

```text
Dream
↓
Goal
↓
DreamMilestone
```

也允许全部留空。

---

# 10. 模块 B：Ritual

## 10.1 Ritual 的产品定义

必须坚持：

```text
Task
= 完成一件事

Ritual
= 进入一种状态
```

Ritual 不是另一套 Todo。

例如：

```text
晨光协议
黄昏仪式
睡前收尾
创作仪式
沉浸宅宅日
周总结
```

---

# 11. Ritual 数据结构

不要只保存“仪式模板”。

因为同一个晨光协议会被执行很多次。

推荐拆为：

```text
Ritual
+
RitualStep
+
RitualExecution
```

---

## 11.1 Ritual

```text
id
name
description
category

triggerTime

enabled

createdAt
updatedAt
```

可以保留：

```text
reward
```

这类旧兼容字段，但不得由 Ritual 模块直接修改 Growth。

如果当前没有历史兼容需要，优先不新增直接奖励字段。

---

## 11.2 RitualStep

```text
id
ritualId

type
title
content

durationSeconds
url

sortOrder

required
```

类型：

```text
TEXT
CHECK
TIMER
LINK
MUSIC_HINT
NOTE
```

语义：

### TEXT

纯提示。

例如：

```text
拉开窗帘，让自然光进来。
```

### CHECK

需要手动确认完成。

### TIMER

倒计时步骤。

例如：

```text
闭眼坐 3 分钟。
```

### LINK

可以打开链接。

### MUSIC_HINT

歌曲 / 歌单提示。

v0.5 暂不接完整音乐服务。

### NOTE

允许执行过程中填写文字。

---

# 12. RitualExecution

用于保存每一次真实执行。

建议：

```text
id
ritualId

startedAt
completedAt

status

note

stepResults / metadata
```

状态：

```text
RUNNING
COMPLETED
CANCELLED
```

如果当前持久层适合关系模型，可拆：

```text
RitualExecutionStep
```

如果项目现状更适合 JSON metadata，也允许把执行结果保存为结构化 JSON。

优先遵循当前项目已有持久化风格，不为了一个小功能引进新框架。

---

# 13. Ritual 执行流程

用户点击：

```text
开始仪式
```

进入执行界面。

依次呈现 Step。

支持：

```text
下一步
勾选
计时
打开链接
填写 Note
跳过非 required 步骤
取消仪式
完成仪式
```

完成后：

```text
RitualExecution.status = COMPLETED

生成：
RITUAL_COMPLETED
```

LifeEvent metadata 至少包含：

```text
ritualId
executionId
category
duration
```

不要直接加 EXP。

---

# 14. Ritual 前端

路由：

```text
/rituals
```

主页分成：

```text
当前可执行
全部仪式
已停用
最近执行
```

卡片重点展示：

```text
名字
一句描述
类别
预计步骤
触发时间
最近一次完成时间
```

主要操作：

```text
开始
编辑
停用
查看历史
```

---

# 15. 仪式执行 UI

执行时不要继续显示后台 CRUD 风格。

建议使用：

```text
较大的居中执行卡片
当前步骤
步骤进度
当前 Step 内容
操作按钮
```

例如：

```text
晨光协议

2 / 5

♫ 播放今天想听的第一首歌

[完成这一步]
```

重点是：

> 有一点“进入状态”的感觉。

但本版本禁止为了仪式页面花大量时间开发复杂粒子动画、转场或音效系统。

先把交互体验做完整。

---

# 16. Focus 与 Ritual 边界

目前系统已经拥有：

```text
IRON_CURTAIN
FocusSession
```

因此：

```text
铁幕开幕
铁幕落幕
```

如果作为 Ritual 存在，只能承担：

```text
进入专注状态的仪式流程
```

不要重复实现 FocusSession 计时。

例如：

```text
铁幕开幕 Ritual
├── 拉窗帘
├── 切换壁纸
├── 整理桌面
└── 准备开始
```

仪式完成后可以提供：

```text
开始 Focus
```

快捷入口。

但：

```text
RitualExecution
≠ FocusSession
```

两个领域保持独立。

---

# 17. 模块 C：「现在。」

路由：

```text
/now
```

产品定位：

```text
不是日记。
不是每日打卡。
不是 Dashboard。
```

它记录：

> 某个人生阶段，此刻的我是什么样。

---

# 18. Now 当前状态

需要存在一份：

```text
当前 Now
```

用户可以不断编辑。

建议逻辑模型：

```text
NowState
```

包含：

```text
stageTitle

theme

favoriteSongs[]
currentGames[]
currentAnime[]
currentBooks[]

currentDreamIds[]
currentGoalIds[]

favoriteQuote

images[]

content

updatedAt
```

例如：

```text
阶段：
2026 盛夏

主题：
热烈、开发、独居、秋招

最近最喜欢的十首歌：
...

当前游戏：
...

当前番剧：
...

当前梦想：
...

最近喜欢的一句话：
...

自由文字：
...
```

---

# 19. v0.5 不要提前绑定 Media

Media 正式开发在后续版本。

所以当前：

```text
currentGames
currentAnime
currentBooks
favoriteSongs
```

优先使用可编辑的轻量文本结构。

例如：

```text
NowItem
├── title
├── subtitle
└── note
```

或结构化 JSON。

不要为了：

```text
当前番剧
```

提前开发完整 Anime Entity。

Dream 已存在，可以允许：

```text
currentDreamIds
currentGoalIds
```

使用真实关联。

---

# 20. Now Snapshot

核心功能：

```text
Create Snapshot
```

当前 Now 可以不断修改。

当用户认为：

> 这一刻值得保存。

点击：

```text
保存阶段快照
```

生成不可随当前 Now 后续修改而变化的：

```text
NowSnapshot
```

---

## 20.1 Snapshot 字段

至少保存：

```text
id

stageTitle
theme

favoriteSongs
currentGames
currentAnime
currentBooks

currentDreams
currentGoals

favoriteQuote
images
content

createdAt
```

重点：

> Snapshot 必须保存当时数据的快照。

不能只是引用当前 NowState。

否则几年后 Dream 改名、Now 被编辑，旧快照也跟着变化，就失去了历史意义。

Dream / Goal 可以同时保存：

```text
id
+
snapshotTitle
```

---

# 21. Now 历史页面

在 `/now` 中同时提供：

```text
现在
历史快照
```

历史建议采用纵向时间轴或卡片。

例如：

```text
2026 盛夏
2026-08-29

2026 秋招
2026-10-12

2026 深秋
2026-11-20
```

点击可以阅读当时完整内容。

历史 Snapshot 默认：

```text
只读
```

允许删除前需要确认。

可以后续再增加编辑能力。

---

# 22. 图片支持

Dream Cover 和 Now Image 如果当前项目已经存在统一上传能力：

```text
复用
```

如果没有，则实现一个简单统一图片上传接口。

禁止：

```text
DreamUploadController
NowUploadController
RitualUploadController
```

各自复制三套上传代码。

优先：

```text
统一 File / Image Storage Service
```

并做好：

```text
文件类型限制
路径安全
随机文件名 / UUID
不存在文件处理
```

本版不要做图库管理系统。

---

# 23. LifeEvent 联动

v0.5 完成后，Timeline 至少能够理解：

```text
创建了一个梦想
完成了一个 Goal
完成了 Dream Milestone
完成了 Task
完成了一次 Ritual
保存了一份「现在。」快照
```

建议事件：

```text
DREAM_CREATED
DREAM_COMPLETED
GOAL_COMPLETED
DREAM_MILESTONE_COMPLETED
TASK_COMPLETED
RITUAL_COMPLETED
NOW_SNAPSHOT_CREATED
```

不要为每一次普通编辑制造 Timeline 噪音。

例如：

```text
改了 Dream 描述一个标点
```

不值得产生事件。

---

# 24. Timeline 文案

LifeEvent 的 title/content 应面向人类阅读。

例如：

```text
完成梦想里程碑「第一章」
Dream：写完自己的小说
Goal：完成第一卷
```

而不是：

```text
DREAM_MILESTONE_COMPLETED id=17
```

Timeline 的事件是生活记录，不是数据库日志。

---

# 25. Dashboard 轻联动

v0.5 不进行新一轮 Dashboard 大重构。

仅做必要聚合。

建议增加：

```text
当前主要 Dream
今日 / 待执行 Ritual
最近保存的 Now Snapshot
```

如果 Dashboard 当前结构不适合，优先只做：

```text
快捷入口
```

不要拖慢主版本。

---

# 26. API

具体 URL 可以根据当前项目已有 REST 命名风格调整。

至少覆盖：

## Dreams

```text
GET    /api/dreams
POST   /api/dreams
GET    /api/dreams/{id}
PUT    /api/dreams/{id}
DELETE / ARCHIVE /api/dreams/{id}

POST   /api/dreams/{id}/complete
POST   /api/dreams/{id}/pause
POST   /api/dreams/{id}/resume
```

## Goals

```text
POST   /api/dreams/{dreamId}/goals
PUT    /api/goals/{id}
POST   /api/goals/{id}/complete
POST   /api/goals/{id}/pause
```

## DreamMilestones

```text
POST   /api/goals/{goalId}/milestones
PUT    /api/dream-milestones/{id}
POST   /api/dream-milestones/{id}/complete
```

## Task Direction

现有 Task API 扩展 Direction 关联，不平行复制新 Task API。

## Ritual

```text
GET    /api/rituals
POST   /api/rituals
GET    /api/rituals/{id}
PUT    /api/rituals/{id}

POST   /api/rituals/{id}/start

GET    /api/ritual-executions/{id}
POST   /api/ritual-executions/{id}/complete
POST   /api/ritual-executions/{id}/cancel
```

## Now

```text
GET /api/now
PUT /api/now

POST /api/now/snapshots
GET  /api/now/snapshots
GET  /api/now/snapshots/{id}
DELETE /api/now/snapshots/{id}
```

---

# 27. 后端工程要求

继续遵循项目当前 Java / Spring Boot 架构。

禁止重新出现明显的 Python 转 Java 风格：

```text
超大 Controller
Map<String, Object> 到处传
Controller 写业务
Service 几百行万能函数
无意义 static 工具堆积
```

优先：

```text
Controller
↓
DTO
↓
Application / Service
↓
Domain / Entity
↓
Repository
```

合理使用：

```text
record DTO
enum
Optional
Bean Validation
事务边界
异常统一处理
```

但不要为了“Java 味”过度设计。

---

# 28. 数据一致性

必须处理以下情况：

### 删除 / 归档 Dream

不得让 Task 外键直接崩坏。

优先：

```text
ARCHIVED
```

而非物理删除。

### Goal 完成

不自动强制完成所有 Task。

### DreamMilestone 完成

也不自动完成相关 Task。

事实可能是：

```text
用户认为这个阶段已经完成
但下面仍存在一些可选 Task
```

因此不要做隐式级联业务。

### Dream 完成

不要自动修改所有 Goal / Milestone。

可以 UI 提示存在未完成项。

最终决定由用户完成。

---

# 29. 重复完成保护

以下操作必须幂等：

```text
complete Task
complete Goal
complete DreamMilestone
complete Dream
complete RitualExecution
create Now Snapshot 请求重试
```

尤其避免：

```text
前端双击
网络重试
↓
产生两条 LifeEvent
↓
Growth Engine 消费两次
```

业务完成动作必须拥有可靠的重复保护。

---

# 30. 数据库迁移

必须：

```text
保留 v0.4 数据
```

禁止为了开发方便：

```text
drop database
重新初始化
```

需要新增表 / 字段时使用当前项目统一迁移方式。

重点检查：

```text
旧 Task
旧 LifeEvent
Growth processed event
Achievement
Milestone
Title
Energy
EXP
```

不得因 v0.5 schema 改动损坏。

---

# 31. 前端统一要求

继续复用 v0.2 已建立的视觉体系：

```text
Layout
Card
Button
Modal
Form
Toast
Loading
Empty State
Error State
```

不要每个新页面自己造一套 CSS。

v0.5 主要页面：

```text
/tasks
/dreams
/rituals
/now
```

必须至少完成桌面端。

移动端保持：

```text
不溢出
不严重错位
基础操作可用
```

本版本不追求单独设计完整移动 UI。

---

# 32. 空状态

这些页面很可能第一次打开什么都没有。

空状态必须认真处理。

例如 Dreams：

```text
还没有写下想追逐的东西。

[创建第一个梦想]
```

Ritual：

```text
还没有属于你的仪式。

[创建仪式]
```

Now：

```text
「现在。」还没有留下内容。

从这一刻开始记录。
```

不要直接显示空白页面或：

```text
No data.
```

---

# 33. 测试

至少增加：

## Dream

```text
创建 Dream
修改 Dream
暂停 / 恢复
完成 Dream
归档
Goal 关联
DreamMilestone 关联
```

## Task

```text
独立 Task 正常工作
Task 关联 Dream
Task 关联 Goal
Task 关联 DreamMilestone
错误跨 Dream 关联被拒绝
TASK_COMPLETED 只生成一次
```

## Ritual

```text
创建 Ritual
Step 排序
开始 execution
完成 execution
取消 execution
重复 complete 不重复产生 LifeEvent
```

## Now

```text
读取当前状态
修改当前状态
创建 Snapshot
修改当前状态后旧 Snapshot 不变化
历史 Snapshot 可读取
```

## 回归

```text
v0.3 Focus
v0.4 Growth
Task 原有功能
LifeEvent
Dashboard
```

不得明显回归。

---

# 34. 真实验收流程

开发完成后必须自己跑一遍完整业务流。

## Flow A：Dream

创建：

```text
Dream
做出真正属于自己的生活 Agent

Meaning
让生活记录、理解与交互不再依赖手动维护。
```

创建：

```text
Goal
完成 Life HUD 主体
```

创建：

```text
DreamMilestone
完成 v0.5 Direction
```

再创建 Task：

```text
完成 v0.5 验收
```

关联该 DreamMilestone。

完成 Task。

确认：

```text
Task 状态正确
LifeEvent 正确
Growth 不被 Task Service 直接修改
```

---

## Flow B：Ritual

建立：

```text
晨光协议
```

Steps：

```text
TEXT
拉开窗帘

MUSIC_HINT
播放今天的第一首歌

CHECK
整理床铺

NOTE
写下一句今天想做的事
```

真实执行一次。

确认：

```text
Execution 保存
Step 数据保存
RITUAL_COMPLETED 生成
Timeline 可读
```

---

## Flow C：「现在。」

填写：

```text
阶段：
2026 盛夏

主题：
Life HUD / 独居 / 开发

十首歌
当前游戏
当前番剧
当前梦想
一句话
自由文字
```

创建 Snapshot。

随后修改当前 Now。

确认：

```text
当前 Now 已变化
旧 Snapshot 完全保持原样
```

---

# 35. 明确禁止项

v0.5 不开发：

```text
Sleep
Meal
Exercise
Check-in
LifeRecord
完整 Journal
完整 Life Timeline UI 重构

Anime Model
Game Model
Book Model
Movie Model

Agent API
朝汐 Agent

复杂 Growth 新经济规则
新 Buff
Coin
Shop
Purchase
Redemption

复杂统计
AI 自动分析
Spotify / Last.fm
自动推荐音乐
动画编辑器
Ritual 脚本语言
复杂工作流引擎
```

不要提前施工后续版本。

---

# 36. 允许顺手修复

如果开发过程中遇到：

```text
明显空指针
DTO 混乱
重复 API 请求代码
CSS 共用组件缺失
命名错误
小型技术债
```

可以顺手修。

但禁止借 v0.5：

```text
再次全项目重构
重写 v0.4 Growth
更换前端技术栈
更换数据库
更换 Spring Boot 主架构
```

---

# 37. 完成标准

v0.5.0 只有满足以下条件才算完成：

## Dreams

- Dream 可 CRUD / 暂停 / 完成 / 归档
- Dream 可拆 Goal
- Goal 可拆 DreamMilestone
- Task 可以独立存在
- Task 可以关联 Direction 层级
- 关联关系正确

## Ritual

- Ritual 可创建 / 编辑 / 停用
- 支持多种 Step
- Ritual 可以真正执行
- Execution 可以保存
- 完成产生 LifeEvent

## 「现在。」

- 当前状态可以持续编辑
- 可以保存阶段 Snapshot
- 可以查看历史 Snapshot
- 修改当前内容不会污染旧 Snapshot

## LifeEvent

- Direction 重要行为进入 LifeEvent
- 重复请求不会制造重复事实
- Timeline 文案可读

## Growth

- v0.4 Growth 正常运行
- v0.5 业务模块不直接修改 Growth
- 事实事件已经为未来 v0.8 Growth 扩展做好准备

## 工程

- 数据库迁移安全
- 后端测试通过
- 前端构建通过
- 主要页面无明显错误
- 桌面端完整可用
- 移动端无灾难级布局问题
- 版本更新为 v0.5.0

---

# 38. 最终提交

开发完成后输出：

```text
1. v0.5 实际完成内容
2. 新增 / 修改的数据模型
3. 新增 LifeEvent 类型
4. 新增 / 修改 API
5. 数据库迁移说明
6. 前端新增页面与组件
7. 测试结果
8. 发现但未处理的问题
9. 与本任务书存在的偏差及原因
```

不要只回复：

```text
v0.5 已完成
```

需要给出可供验收的具体结果。

---

# 39. 一句话版本目标

```text
v0.4 让 Life HUD 知道“我在成长”。

v0.5 要让 Life HUD 开始知道：

“我为什么往前走，
我想怎样度过自己的日子，
以及此刻的我究竟是什么样。”
```