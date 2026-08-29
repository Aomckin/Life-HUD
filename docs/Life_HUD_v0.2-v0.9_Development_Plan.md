# Life HUD v0.2 ~ v0.9 开发计划

> 当前目标：今晚正式开工，在 Agent 接入前，用 2~3 天完成 Life HUD 的主体生活业务。
>
> 核心路线：**先让 Life HUD 本体能够记录、组织和反馈生活，再让 Agent 去理解生活。**

---

## 0. 总体产品结构

Life HUD 不再按“一个功能一个模块”无限横向堆积，而采用：

> **大业务域 + 小类别 + 统一 LifeEvent**

顶层大域：

```text
Life HUD
├── Today / Dashboard
├── Focus
├── Tasks & Dreams
├── Ritual
├── Life
├── Media
├── Journal
├── Now / 「现在。」
└── Growth
```

核心数据流：

```text
现实生活
  ↓
各业务域记录
  ↓
LifeEvent
  ↓
Life Timeline
  ↓
Dashboard / Growth / Statistics
  ↓
Agent Context API
  ↓
朝汐 Agent
```

---

# v0.2 —— Shell / 新前端与全局骨架

## 版本目标

彻底结束“Python / FastAPI 时代顺手做的网页前端”。

这一版不追求大量新业务，重点是把 Life HUD 的长期产品骨架搭起来，让后续所有模块都有稳定的容器。

---

## 1. 前端整体重构

### 页面结构

建议路由：

```text
/dashboard

/focus

/tasks
/dreams

/rituals

/life

/media

/journal

/now

/growth

/settings
```

### 整体 Layout

完成：

- 顶部状态区
- 主导航
- 内容区域
- 移动端基本适配
- 统一卡片
- 统一按钮
- 统一表单
- Modal
- Toast
- Loading
- Empty State
- Error State

---

## 2. Dashboard 雏形

首页只做聚合，不继续塞全部 CRUD。

第一版显示：

- 当前能量
- 等级 / EXP
- 当前称号
- 今日任务
- 今日 Focus
- 当前状态
- 最近 LifeEvent
- 快捷操作

---

## 3. 前端工程结构

至少完成：

```text
api/
components/
pages/
modules/
utils/
styles/
```

要求：

- API 请求统一封装
- 页面和业务组件分离
- 不继续把所有逻辑堆进单个 JS
- 后续新增模块不需要大改全局结构

---

## 4. LifeEvent 地基

虽然 Journal 还没正式开发，但从 v0.2 开始就建立统一事件模型。

建议：

```text
LifeEvent
├── id
├── type
├── source
├── title
├── content
├── occurredAt
├── createdAt
├── tags
└── metadata
```

第一批类型：

```text
TASK_COMPLETED
FOCUS_STARTED
FOCUS_FINISHED
RITUAL_COMPLETED
ENERGY_CHANGED
JOURNAL_WRITTEN
MANUAL_EVENT
```

后续继续扩展。

---

## v0.2 完成标准

- 新前端框架可用
- 所有大域都有页面入口
- Dashboard 不再是旧式控制台
- LifeEvent 模型落地
- 后续模块可以独立快速开发

---

# v0.3 —— Focus / 铁幕与人性化番茄钟

## 版本目标

把“铁幕时间”正式产品化。

番茄钟不再是一个孤立计时器，而成为 Focus 的一种工作模式。

---

## 1. FocusSession

统一模型：

```text
FocusSession
├── id
├── type
├── startTime
├── endTime
├── plannedMinutes
├── actualMinutes
├── effectiveMinutes
├── status
├── note
├── interruptions
└── relatedTaskIds
```

类型：

```text
IRON_CURTAIN
POMODORO
FREE
```

---

## 2. 铁幕

支持：

- 开幕
- 暂停
- 恢复
- 落幕
- 中断记录
- 手动补录
- 有效时间统计
- 关联任务
- 落幕总结

示例：

```text
14:10 铁幕开幕

算法        50 min
休息        10 min
Life HUD    80 min
中断        12 min
Java        40 min

18:20 铁幕落幕

总时长：250 min
有效时间：170 min
```

---

## 3. 番茄钟重构

支持：

- 自定义专注时长
- 自定义休息时长
- 延长本轮
- 提前结束
- 暂停 / 恢复
- 跳过休息
- 连续轮次
- 关联任务
- 自动记录有效时间

不强制传统 25 + 5。

---

## 4. LifeEvent 联动

自动生成：

```text
FOCUS_STARTED
FOCUS_PAUSED
FOCUS_RESUMED
FOCUS_FINISHED
```

Dashboard 同步今日 Focus 数据。

---

## v0.3 完成标准

- 能完整记录一次真实铁幕
- 番茄钟可日常使用
- FocusSession 可持久化
- 能统计今日有效专注时间
- Focus 自动进入 Timeline 数据源

---

# v0.4 —— Growth / 成长、里程碑与长期生活轨迹

## 版本目标

以 [Life HUD v0.4 总开发任务书](Life%20HUD%20v0.4%20任务书.md) 为唯一开发基准，把 Growth 从旧游戏经济中拆出，改造成由 LifeEvent 驱动的生活成长派生层。

核心范围：

```text
LifeEvent
  ↓ eventId 幂等
Growth Engine
  ├── Energy（近期状态）
  ├── EXP / Level（长期累计）
  ├── Achievement（系统识别节点）
  ├── Milestone（用户选择节点）
  ├── Title（身份标签，无 Buff）
  └── Growth Snapshot
```

同时完成 Growth 一级页面、Today / Focus / Task / Timeline 联动、旧成长数据兼容迁移，并停用 Shop、Coin、Purchase、Redemption 与称号 Buff 主流程。

## v0.4 完成标准

- Focus / Task 只产生事实 LifeEvent，由 Growth Engine 统一计算
- 同一事件不会重复增加 EXP / Energy 或重复解锁
- Achievement、Milestone、Title、Snapshot 与 Growth 页面完整可用
- Today 与 Timeline 能展示真实成长状态和重要节点
- 旧 EXP、Energy、Achievement、Title 得到兼容保留
- Shop / Coin 不再进入 UI 与命令主流程
- 版本号为 v0.4.0，专项与回归测试通过

---

# v0.5 —— Direction / 梦想、任务、仪式与「现在。」

## 版本目标

给 Life HUD 加上“方向”和“生活气质”。

这一版回答三个问题：

```text
我想去哪里？
我想怎么生活？
现在的我是什么样？
```

---

# A. Tasks & Dreams

## 1. 层级

```text
Dream
  ↓
Goal
  ↓
Milestone
  ↓
Task
```

但普通 Task 不强制关联 Dream。

---

## 2. Dream

建议字段：

```text
Dream
├── title
├── description
├── meaning
├── status
├── createdAt
├── targetDate
├── coverImage
└── note
```

状态：

```text
ACTIVE
PAUSED
COMPLETED
ARCHIVED
```

---

## 3. Goal / Milestone

用于拆解梦想。

示例：

```text
Dream
写完自己的小说

Goal
完成第一卷

Milestone
完成第一章

Task
今晚写 1000 字
```

---

## 4. Task

保留：

- 普通任务
- 每日任务
- 特殊任务
- 截止时间
- 优先级
- 完成奖励
- Dream / Goal / Milestone 关联

完成任务生成：

```text
TASK_COMPLETED
```

---

# B. Ritual

## 1. 定位

Task：

> 完成一件事。

Ritual：

> 进入一种生活状态。

两者不混在一起。

---

## 2. Ritual 类型

例如：

```text
晨光协议
黄昏仪式
铁幕开幕
铁幕落幕
睡前收尾
沉浸宅宅日
创作仪式
周总结
```

---

## 3. Ritual 结构

```text
Ritual
├── name
├── description
├── category
├── triggerTime
├── enabled
├── reward
└── steps
```

Step 支持：

```text
TEXT
CHECK
TIMER
LINK
MUSIC_HINT
NOTE
```

完成生成：

```text
RITUAL_COMPLETED
```

---

# C. Now / 「现在。」

## 定位

记录阶段快照，而不是每天的流水账。

建议内容：

- 现在最喜欢的十首歌
- 当前游戏
- 当前番剧
- 当前书
- 当前梦想
- 当前目标
- 最近喜欢的一句话
- 当前阶段主题
- 图片
- 自由文字

支持：

```text
Create Snapshot
```

形成历史：

```text
2026 盛夏
2026 秋招
2026 深秋
2027 春
...
```

---

## v0.5 完成标准

- Dream 可创建、修改、归档
- Dream 可拆 Goal / Milestone
- Task 可关联上层目标
- Ritual 可创建并执行
- 「现在。」可编辑
- 可保存阶段快照

---

# v0.6 —— Life / 生活输入与万能时间线

## 版本目标

开始真正记录“生活本身”。

这一版是 Life HUD 从生产力工具变成生活状态系统的关键版本。

---

# A. Life 大域

原则：

> **简单记录走通用模型，复杂业务升格独立模型。**

---

## 1. Sleep

第一版手动填写。

```text
SleepRecord
├── sleepTime
├── wakeTime
├── duration
├── quality
├── type
└── note
```

类型：

```text
NIGHT
NAP
OTHER
```

支持：

- 入睡时间
- 起床时间
- 自动计算时长
- 主观睡眠质量
- 午睡
- 备注

生成：

```text
SLEEP_RECORDED
```

---

## 2. Meal

支持照片上传。

```text
MealRecord
├── mealType
├── time
├── images[]
├── description
├── satisfaction
└── note
```

类型：

```text
BREAKFAST
LUNCH
DINNER
SNACK
OTHER
```

第一版不做：

- 卡路里
- 蛋白质
- 脂肪
- 碳水
- AI 菜品识别

只做：

> 时间 + 照片 + 一句话

生成：

```text
MEAL_RECORDED
```

---

## 3. Exercise

支持：

```text
力量训练
散步
骑行
舞萌
跑步
其他
```

结构：

```text
ExerciseRecord
├── type
├── startTime
├── duration
├── intensity
└── note
```

生成：

```text
EXERCISE_RECORDED
```

---

## 4. Check-in

快速主观状态记录。

建议：

```text
CheckIn
├── energy
├── mood
├── focusDesire
├── fatigue
├── time
└── note
```

可以全部使用 1~10。

目标是让未来 Agent 能理解：

> “做了什么”之前，处在什么状态。

---

## 5. LifeRecord

零碎生活信息统一使用通用记录。

```text
LifeRecord
├── type
├── value
├── unit
├── time
├── note
└── metadata
```

小类别：

```text
WATER
CAFFEINE
ALCOHOL
SUNLIGHT
SOCIAL
BODY_STATUS
OUTDOOR
CUSTOM
```

以后新增小类别时：

> 优先增加 Type，不新增 Controller / Service / Table。

---

# B. Journal / Life Timeline

## 定位

万能日记不是一个普通富文本编辑器。

它是：

> **所有生活事件的统一时间线。**

---

## 1. Life Timeline

聚合：

- Focus
- Task
- Dream
- Ritual
- Sleep
- Meal
- Exercise
- Check-in
- LifeRecord
- Media
- Growth
- 手动日记

示例：

```text
2026-08-26

08:12
Sleep
睡眠 7h42min

08:40
Meal
早餐
[照片]

10:10
Check-in
能量 8 / 10

14:10
Focus
铁幕开幕

17:30
Task
完成 Java 学习

18:22
Exercise
散步 31min

21:03
Game
游玩 86min

00:10
Journal
今天很喜欢……
```

---

## 2. 手动日记

允许：

- 长文本
- 一句话
- 图片
- 标签
- 自由记录

手写日记本质上也生成：

```text
JOURNAL_WRITTEN
```

---

## v0.6 完成标准

- 睡眠可手动记录
- 饮食可上传照片
- 运动可记录
- Check-in 可快速填写
- 零碎 LifeRecord 可扩展
- Timeline 能聚合多个模块
- 手动日记可进入 Timeline

---

# v0.7 —— Media / 宅宅生活档案

## 版本目标

建立真正属于生活的一部分：

> 番、游戏、书、电影，以及它们在什么时候陪伴过自己。

---

## 1. Media 顶层结构

```text
Media
├── Anime
├── Game
├── Book
├── Manga
├── Movie
└── Other
```

核心思想：

```text
作品档案
+
消费 Session
```

---

# A. Anime

```text
Anime
├── title
├── totalEpisodes
├── currentEpisode
├── status
├── score
├── startedAt
├── finishedAt
└── note
```

状态：

```text
PLANNED
WATCHING
PAUSED
COMPLETED
DROPPED
```

观看记录：

```text
AnimeWatchSession
├── animeId
├── episodeStart
├── episodeEnd
├── watchedAt
├── duration
└── note
```

---

# B. Game

```text
Game
├── title
├── platform
├── status
├── totalPlayTime
├── startedAt
├── finishedAt
├── score
└── note
```

Session：

```text
GameSession
├── gameId
├── startTime
├── endTime
├── duration
├── progress
└── note
```

---

# C. Book / Manga / Movie

第一版只做简单档案。

统一思路：

```text
MediaItem
+
MediaSession
```

暂时不追求复杂业务。

---

## 2. LifeEvent 联动

例如：

```text
GAME_PLAYED
ANIME_WATCHED
BOOK_READ
MOVIE_WATCHED
```

一条 GameSession：

1. 更新游戏累计时长
2. 保存 Session
3. 生成 LifeEvent
4. 出现在 Journal
5. 未来供 Agent 分析

---

## 3. Music

暂时不做完整音乐活动统计。

当前音乐重点放在：

```text
「现在。」
└── 十首歌
```

以后需要时再接 Last.fm / Spotify 等数据源。

---

## v0.7 完成标准

- Anime 可维护
- Game 可维护
- Anime / Game Session 可记录
- Book / Movie 至少有基础档案
- Media 活动进入 Timeline

---

# v0.8 —— Growth 扩展与 Agent 前置

## 版本目标

让 v0.4 已建立的 Growth 派生层消费 v0.5 ~ v0.7 新增的真实生活行为，并准备 Agent 读取接口。

此时 Growth 不再是业务本体，而是：

> **现实生活行为的游戏化反馈层。**

---

# A. Growth

```text
Growth
├── Energy
├── EXP
├── Level
├── Achievement
├── Milestone
└── Title
```

---

## 1. Energy

逐渐减少纯手工：

```text
学习 +10
游戏 -20
```

这种旧式操作。

改为真实行为驱动：

```text
完成铁幕
→ Energy / EXP

完成 Ritual
→ Energy / EXP

完成 Dream Milestone
→ 大量 EXP

连续记录睡眠
→ Achievement

完成一部番
→ LifeEvent / Achievement
```

保留快捷行动作为：

> 手动修正和特殊情况。

---

## 2. EXP / Level

EXP 来源：

- Focus
- Task
- Goal
- Dream Milestone
- Ritual
- 特殊行为

---

## 3. Achievement

基于后续模块追加：

```text
连续专注 7 天
完成 10 次晨光协议
第一次创建梦想
完成第一个 Dream Milestone
看完第一部番
通关第一款记录游戏
连续记录 30 天
```

---

## 4. Title

来源：

- Achievement
- Level
- 特殊事件

支持手动装备。

Title 始终只作为身份标签，不引入 Buff。

---

# B. Dashboard 聚合升级

到 v0.8 首页应该能够显示：

- 当前 Energy
- Level / EXP
- 今日 Focus
- 今日 Task
- 昨夜睡眠
- 最近一餐
- 当前 Check-in
- 当前 Dream
- 最近 Media
- 今日 Timeline
- Ritual
- Achievement

---

# C. Agent Context API

v0.8 不开发 Agent。

只准备干净的读取接口。

建议：

```text
GET /api/agent/context/today

GET /api/agent/context/recent

GET /api/agent/context/status

GET /api/agent/context/focus

GET /api/agent/context/tasks

GET /api/agent/context/dreams

GET /api/agent/context/life

GET /api/agent/context/journal

GET /api/agent/context/media

GET /api/agent/context/growth
```

原则：

```text
Agent
  ↓
REST API
  ↓
Life HUD
  ↓
Database
```

禁止：

```text
Agent
  ↓
直接读 Life HUD 数据库
```

---

## v0.8 完成标准

到这一版结束，Life HUD 应该能够知道：

- 今天做了什么
- 今天专注了多久
- 完成了什么任务
- 正在追逐什么梦想
- 执行了哪些仪式
- 昨晚睡了多久
- 吃了什么
- 有没有运动
- 当前主观状态如何
- 看了什么番
- 玩了什么游戏
- 当前能量 / 等级 / EXP
- 最近一段时间生活发生了什么

此时：

> **Life HUD 已经拥有生活记忆，只是还没有理解这些记忆的大脑。**

---

# v0.9 —— 朝汐 Agent

> 不属于本轮 2~3 天开发目标。

Agent 接入后主要负责：

- 自然语言查询 Life HUD
- 今日总结
- 日评
- 周报
- 状态趋势
- 行为关联分析
- 梦想推进分析
- Focus 分析
- 睡眠 / 状态关联
- 娱乐 / 学习平衡
- 主动建议
- 调用 Life HUD API 执行业务

架构：

```text
Life HUD
Java 21 / Spring Boot
负责真实业务与事实

        ↓ REST API

朝汐 Agent
Python
负责理解、分析与自然语言交互
```

---

# 2~3 天施工计划

## 今晚：v0.2

主攻：

```text
前端 Shell
导航
Dashboard
页面骨架
LifeEvent
```

今晚不要陷入像素级审美打磨。

目标是：

> 把旧房子的承重墙和水电彻底换掉。

---

## Day 1：v0.3 + v0.5

上午 / 下午：

```text
Focus
铁幕
番茄钟
```

后半：

```text
Dream
Goal
Milestone
Task 关联
Ritual
「现在。」
```

---

## Day 2：v0.6

整天优先给：

```text
Life
+
Journal
```

重点：

```text
Sleep
Meal + 图片上传
Exercise
Check-in
LifeRecord
Life Timeline
```

这是 Agent 前最重要的一版。

---

## Day 3：v0.7 + v0.8

前半：

```text
Anime
Game
Media Session
```

后半：

```text
Growth
Achievement
Dashboard 聚合
Agent Context API
```

最后统一：

- 修 Bug
- 清技术债
- 检查数据库迁移
- 检查 API
- 检查移动端
- 检查 Timeline
- 做一次完整真实生活录入测试

---

# 优先级

## P0：绝对不能砍

```text
新前端骨架
Focus
Dream
Sleep
Meal
Check-in
LifeEvent
Life Timeline
```

---

## P1：尽量完成

```text
Ritual
「现在。」
Exercise
Anime
Game
Growth 整合
Agent Context API
```

---

## P2：可以后补

```text
饮水
咖啡因
酒精
日晒
社交记录
Book / Manga 复杂功能
Shop 深化
Title Buff
复杂统计
高级动画
AI 图片识别
自动设备数据
```

---

# 架构约束

## 1. 大域稳定，小类别可扩展

不要：

```text
WaterController
WaterService

SunlightController
SunlightService

CoffeeController
CoffeeService

AlcoholController
AlcoholService
```

优先：

```text
LifeRecord
  ↓
LifeRecordType

WATER
SUNLIGHT
CAFFEINE
ALCOHOL
SOCIAL
...
```

---

## 2. 简单记录通用化

规则：

> **简单记录走通用模型，复杂业务升格独立模型。**

独立模型：

```text
SleepRecord
MealRecord
FocusSession
Dream
Game
Anime
```

通用模型：

```text
LifeRecord
```

---

## 3. 所有重要行为最终进入 LifeEvent

每个模块都应该问一句：

> 这个行为是否值得成为 Life Timeline 中的一条生活记录？

如果值得，就生成 LifeEvent。

---

## 4. 原始记录是事实，AI 输出是解释

未来 Agent 可以生成：

- AI 日评
- AI 周报
- 行为分析
- 趋势总结

但：

> AI 生成内容不能覆盖原始 LifeEvent。

---

## 5. Agent 只能通过 API 使用 Life HUD

保持：

```text
Life HUD = 业务事实层
Agent = 理解与交互层
```

---

# v0.8 后的 Life HUD

最终结构：

```text
                 Life HUD

          ┌──── Today ────┐
          │               │
       Focus             Life
       Tasks             Media
       Dreams            Ritual
          │               │
          └── LifeEvent ──┘
                  │
               Journal
                  │
                Growth
                  │
           Agent Context API
                  │
                  ▼
              朝汐 Agent
```

到这里，Life HUD 不再只是：

> 任务管理 + 能量条。

而应该已经成为：

> **一套记录暗苟如何生活、如何行动、如何休息、如何娱乐、如何成长、正在追逐什么，以及这一切如何互相影响的个人 Life HUD。**

Agent 只是最后坐进驾驶舱的“副驾驶”。

Life HUD 本身，必须先成为一辆完整的车。
