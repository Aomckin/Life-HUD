## Life HUD v0.4 总开发任务书

## 版本名称

# v0.4 · Growth

成长、里程碑与长期生活轨迹

---

# 一、版本定位

Life HUD 已经逐渐从最早的“宅宅能量条”演化为一个完整的个人生活 HUD。

因此 v0.4 不应简单恢复旧项目中的游戏化系统。

旧版存在：

- Energy
- EXP
- Level
- Achievement
- Title
- Shop
- Currency
- Reward

其中：

继续保留并重构：

- Energy
- EXP
- Level
- Achievement
- Title

明确废弃：

- Shop
- Currency / Coin
- Purchase
- Redemption
- 使用 Energy 购买娱乐时间
- RPG 式 Buff
- 游戏经济循环

v0.4 的目标不是：

> “让生活变成游戏。”

而是：

> “让已经发生的生活能够被记录、积累，并逐渐形成一条可以回看的成长轨迹。”

---

# 二、核心目标

本版本一次性完成以下体系：

1. LifeEvent 基础设施完善
2. Growth Engine
3. Energy 重构
4. EXP / Level 重构
5. Growth Snapshot
6. Achievement
7. Milestone
8. Title
9. Growth 页面
10. Today / Dashboard 联动
11. Timeline 联动
12. 成长反馈动画
13. 旧数据迁移
14. 完整测试与文档

完成后形成：

```text
真实生活行为
     ↓
  LifeEvent
     ↓
 Growth Engine
     ↓
┌─────────────────┐
│ Energy          │
│ EXP / Level     │
│ Achievement     │
│ Milestone       │
│ Title           │
└─────────────────┘
     ↓
Growth / Today / Timeline
```

# 三、总体设计原则

## 3.1 不做生活 RPG

禁止引入：

- 金币
- 商店
- 装备
- 抽卡
- 稀有度
- Buff / Debuff
- 战斗力
- 属性面板
- 奖励兑换商城
- “做任务换游戏时间”的经济循环

可以保留一定的游戏化视觉表达：

- Level
- EXP
- Achievement
- Title
- Level Up 动画

但这些东西只用于：

> 记录成长与制造轻量仪式感。

---

## 3.2 事实优先

Life HUD 中的事实来源应尽量唯一。

例如：

```
FocusSession
```

负责记录真实 Focus。

```
LifeEvent
```

负责表达：

> 某一刻发生了什么。

```
Growth Engine
```

负责根据 LifeEvent 推导成长变化。

禁止重复存储大量可以从现有数据计算得到的信息。

---

## 3.3 Growth 是派生层

业务模块不应该直接修改：

```
exp += 10;
energy += 5;
```

例如 Focus 只负责产生：

```
FOCUS_FINISHED
```

Task 只负责产生：

```
TASK_COMPLETED
```

然后：

```
LifeEvent↓Growth Engine↓EXP / Energy / Achievement
```

---

# 四、LifeEvent 完善

v0.3 已经存在或已经开始建立 LifeEvent。

v0.4 首先检查当前实现。

如果已经可用：

直接扩展。

如果仍然只是基础实现：

补全为统一生活事件模型。

建议：

```
LifeEvent
├── id
├── type
├── occurredAt
├── sourceType
├── sourceId
├── title
├── description
├── metadata
├── createdAt
└── version
```

---

## 4.1 sourceType

例如：

```
FOCUS
TASK
SYSTEM
ACHIEVEMENT
MILESTONE
LEVEL
RITUAL
SLEEP
MEAL
MEDIA
DREAM
```

当前不存在的模块可以预留 enum。

不要提前实现其业务。

---

## 4.2 当前至少支持

```
FOCUS_STARTED
FOCUS_PAUSED
FOCUS_RESUMED
FOCUS_FINISHED

TASK_COMPLETED

LEVEL_UP

ACHIEVEMENT_UNLOCKED

MILESTONE_CREATED

TITLE_UNLOCKED
TITLE_EQUIPPED
```

根据当前 Task 模块实际能力调整。

---

# 五、事件幂等

Growth Engine 必须保证：

> 同一个 LifeEvent 只能被成长系统处理一次。

否则刷新、重试或恢复时可能重复增加 EXP。

建议建立类似：

```
GrowthEventRecord
├── eventId
├── processedAt
├── expDelta
├── energyDelta
├── levelBefore
├── levelAfter
└── result
```

或者使用当前项目中更简单可靠的实现。

要求：

```
eventId
```

必须成为幂等依据。

---

# 六、Growth Engine

新增统一成长计算层。

职责：

```
接收 LifeEvent
↓
匹配 Growth Rule
↓
计算 Growth Change
↓
更新状态
↓
检查 Level
↓
检查 Achievement
↓
产生后续 LifeEvent
```

禁止：

```
FocusService
TaskService
```

直接包含大量 EXP / Level 判断。

---

# 七、Growth Result

建议统一一次成长变化结果。

例如：

```
GrowthResult
├── expDelta
├── energyDelta
├── levelBefore
├── levelAfter
├── levelUp
├── unlockedAchievements
├── unlockedTitles
└── message
```

前端可以直接根据结果展示：

```
有效 Focus 3h 10min

EXP +38
Lv.16 → Lv.17

解锁成就：
「百时」
```

不需要业务模块自己拼奖励提示。

---

# 八、Growth Rule

成长规则禁止大量硬编码散落。

至少建立一个集中规则层。

形式可以是：

- Java Configuration
- JSON
- 当前项目已有规则文件
- Database-backed definition

选择最符合现有架构的方案。

重点是：

> 所有成长规则都有统一入口。

---

## 8.1 Focus Rule

例如：

```
有效 Focus
每一定有效时间获得 EXP
```

不要按照自然跨度计算。

必须使用：

```
effectiveMinutes
```

BREAK / INTERRUPTION / PAUSE：

不产生有效 Focus EXP。

---

## 8.2 Task Rule

例如：

```
TASK_COMPLETED
→ EXP
```

如果 Task 已有 difficulty / reward / type 等字段：

尽量复用。

---

## 8.3 防异常

必须考虑：

- 超长 Focus
- 手动补录
- 修改历史
- 重复事件
- 删除数据
- 测试 Session
- 一次性导入旧数据

避免出现异常刷 EXP。

---

# 九、Energy 重新定义

Energy 不再作为货币。

不要用于：

```
购买奖励
购买游戏时间
购买商品
```

v0.4 中正式定义：

> Energy 是 Life HUD 对近期生活状态的一种抽象表达。

它属于：

```
短周期状态
```

而 EXP 属于：

```
长期累计成长
```

---

# 十、Energy 当前阶段实现

由于：

- Sleep
- Meal
- Exercise
- Mood
- Entertainment

等生活模块尚未完全建立，

v0.4 不要假装 Energy 是科学健康指标。

当前版本先建立：

```
EnergyState
```

与统一计算接口。

允许现有：

- Focus
- Task
- Ritual（若已有）

提供有限影响。

后续生活模块进入后逐步扩展。

---

## 10.1 Energy 模型

建议：

```
EnergyState
├── current
├── min
├── max
├── updatedAt
└── reason
```

保留当前项目已有 0 ~ 180 或其他规则时：

优先兼容现有数据。

不要未经迁移直接改变用户当前 Energy。

---

## 10.2 Energy History

必须能够知道：

> 为什么 Energy 变成了现在这个值。

建议建立：

```
EnergyRecord
├── id
├── delta
├── before
├── after
├── reason
├── sourceEventId
└── occurredAt
```

例如：

```
+5
完成 90min Focus

-3
某状态调整
```

当前没有负向规则也可以先保持只有正向。

但模型应支持正负变化。

---

# 十一、EXP

EXP 表达：

> Life HUD 所记录下来的长期行动积累。

特点：

```
只增不减
长期累计
不可消费
```

除：

- 数据纠错
- 数据迁移
- 管理操作

之外，不应出现 EXP 减少。

---

# 十二、Level

Level 是 EXP 的阶段表达。

不要让 Level 承担大量功能解锁。

它主要用于：

- 表达长期成长
- 提供阶段感
- 提供历史刻度
- Level Up 仪式反馈

---

## 12.1 等级曲线

重新检查当前旧版：

```
EXP 34 / 50
Lv.16
```

的等级公式。

如果旧规则合理：

优先复用。

如果旧规则过于简单或已经不适合长期使用：

重新设计等级曲线。

要求：

- 前期升级不要太慢
- 后期不会一天十几级
- 使用几年仍然可继续累计
- 无硬等级上限或上限足够高

---

## 12.2 Level Up

当 EXP 跨越等级边界：

产生：

```
LEVEL_UP
```

LifeEvent。

记录：

```
oldLevel
newLevel
occurredAt
```

---

# 十三、Level Up 动画

Level Up 应有独立视觉反馈。

保持 Life HUD 风格：

- 清透
- 轻盈
- 克制
- 有仪式感

例如：

```
LEVEL UP
Lv.16
↓
Lv.17
又往前走了一点。
```

允许：

- fade
- glow
- 小范围 scale
- 数字滚动

禁止：

- 大量粒子
- 金光爆炸
- 游戏结算音效
- 页游式升级特效

---

# 十四、Achievement

Achievement 从“游戏成就”重新定义为：

> 系统自动识别出的生活里程碑。

例如：

```
铁幕初启
第一次完成铁幕

百时
累计有效 Focus 100 小时

长夜
完成一次跨午夜 Focus

长期主义
累计产生 1000 个 LifeEvent
```

以后可以扩展：

- Sleep
- Meal
- Exercise
- Dream
- Media
- Ritual
- Project
- Travel

---

# 十五、Achievement Definition

建议：

```
AchievementDefinition
├── id
├── name
├── description
├── icon
├── category
├── hidden
├── conditionType
├── target
├── metadata
└── relatedTitleId
```

用户状态：

```
AchievementProgress
├── achievementId
├── current
├── target
├── unlocked
└── unlockedAt
```

---

# 十六、Achievement 条件类型

第一版至少支持：

```
COUNT
TOTAL_DURATION
SINGLE_DURATION
STREAK
LEVEL
VALUE_THRESHOLD
CUSTOM
```

如果某些类型 v0.4 暂时没有数据来源：

可以保留定义能力，不必硬造对应成就。

---

# 十七、Achievement 分类

建议：

```
FOCUS
TASK
GROWTH
LIFE
SPECIAL
HIDDEN
```

未来可以追加：

```
SLEEP
FOOD
MEDIA
DREAM
RITUAL
TRAVEL
```

---

# 十八、隐藏成就

支持：

```
hidden = true
```

未解锁前：

```
???
```

或：

```
隐藏成就
```

解锁后展示真实内容。

不要做很多。

只需要建立能力，并准备少量有趣例子。

---

# 十九、Achievement Unlock

解锁成就时：

产生：

```
ACHIEVEMENT_UNLOCKED
```

LifeEvent。

并显示轻量动画。

例如：

```
Achievement Unlocked
「长夜」
完成一次跨越午夜的 Focus。
```

不要显示：

```
奖励 100金币
```

因为不存在奖励经济系统。

---

# 二十、Milestone

新增：

# Milestone / 人生里程碑

Achievement 是：

> 系统根据规则识别出的节点。

Milestone 是：

> 用户认为值得留下来的生活节点。

二者必须区分。

---

# 二十一、Milestone 模型

建议：

```
Milestone
├── id
├── title
├── description
├── occurredAt
├── category
├── source
├── relatedEventIds
├── relatedModule
├── media
├── pinned
├── createdAt
└── updatedAt
```

---

## 21.1 Milestone 来源

```
MANUAL
SYSTEM
ACHIEVEMENT
IMPORT
```

---

## 21.2 用户可以手动创建

例如：

```
Life HUD 完成 Java 化

第一次完整用 Life HUD 记录铁幕

某项目发布 v1.0

某个梦想完成
```

Milestone 应允许：

- 标题
- 日期
- 描述
- 分类
- 关联模块
- 可选图片 / 媒体引用（如果当前文件体系容易支持）
- Pin

本轮如果媒体上传成本明显较高：

先保留字段与 UI 占位，不强制实现上传。

---

# 二十二、Milestone 与 Timeline

Timeline 中：

普通 LifeEvent：

轻量展示。

Milestone：

明显提升视觉层级。

例如：

```
2026-08-29

◆ Life HUD v0.4

Progression 系统完成。
```

未来长时间使用后：

Milestone 应成为 Timeline 中最值得回看的节点。

---

# 二十三、Title

Title 不再提供 RPG Buff。

Title 定义：

> 从生活经历中获得并选择展示的身份标签。

例如：

```
信息绝缘体
铁幕行者
夜航者
百时专注
夏日施工队
梦想收藏家
```

---

# 二十四、Title 模型

建议：

```
TitleDefinition
├── id
├── name
├── description
├── sourceType
├── sourceId
├── hidden
└── style
```

用户状态：

```
UserTitle
├── titleId
├── unlockedAt
└── equipped
```

---

# 二十五、Title 来源

允许：

```
ACHIEVEMENT
LEVEL
MILESTONE
SYSTEM
MANUAL
AI
```

其中：

```
AI
```

只预留。

未来朝汐可以根据长期生活记录生成特殊称号。

v0.4 不实现 AI 生成。

---

# 二十六、Title 解锁

某些 Achievement 可以关联称号。

例如：

```
Achievement
百时

↓ unlock

Title
百时专注
```

但：

不是所有 Achievement 都必须给称号。

---

# 二十七、Title Equip

只能同时装备一个当前称号。

首页：

```
Lv.17
称号：信息绝缘体
```

点击称号区域：

进入 Title 管理。

允许：

```
查看已解锁
查看未解锁
装备
取消装备
```

---

# 二十八、自定义 Title

如果旧系统已有自定义称号：

尽量保留兼容。

如果没有：

可以允许用户创建个人称号。

但自定义称号必须与系统解锁称号视觉上可区分。

例如：

```
自定义
```

标签。

---

# 二十九、Growth Snapshot

新增：

# GrowthSnapshot

这是 v0.4 非常重要的新能力。

目的：

> 不只知道现在是什么样，还能知道以前是什么样。

---

## 29.1 Snapshot 内容

建议每天保存：

```
GrowthSnapshot
├── date
├── level
├── totalExp
├── energy
├── totalFocusMinutes
├── totalEffectiveFocusMinutes
├── totalTaskCompleted
├── lifeEventCount
├── achievementCount
├── milestoneCount
└── createdAt
```

未来其他模块进入后可以扩展：

```
sleep
exercise
meals
media
dreams
```

---

## 29.2 Snapshot 创建

可以：

- 每天第一次访问时补前一天
- 每日结束时生成
- 查询时按需补齐

根据当前项目运行方式选择最可靠方案。

不要依赖必须一直运行的后台定时器。

---

## 29.3 Snapshot 幂等

同一天：

只能存在一个正式 Snapshot。

如果当天数据仍在变化：

允许更新当天 Snapshot。

历史日期应稳定。

---

# 三十、Growth Trend

Growth 页面允许基于 Snapshot 展示轻量趋势。

v0.4 不需要复杂数据分析平台。

可以先展示：

```
过去 7 天

有效 Focus
EXP 增长
完成任务
Energy
```

如果需要图表：

保持简单。

不要出现企业 BI Dashboard 风格。

---

# 三十一、新建 Growth 页面

新增一级页面：

```
Growth
```

建议侧边导航：

```
Today
Tasks
Focus
Growth
...
```

具体顺序根据当前 UI 调整。

---

# 三十二、Growth 页面总体布局

建议：

```
Growth

Lv.17
████████────
EXP 126 / 180

Energy
142 / 180

当前称号
信息绝缘体


最近成长
────────────
铁幕落幕        EXP +18
任务完成        EXP +5
Level Up        16 → 17


成长轨迹
────────────
过去 7 天 ...


Achievement
27 / 80

Milestone
12

Titles
8
```

页面只需要表达：

> 我现在在哪里，以及一路是怎么走过来的。

---

# 三十三、Growth 页面子区域

可以使用：

```
Overview
Achievements
Milestones
Titles
```

Tab。

不要为四个系统建立四套巨大独立页面，除非当前前端结构明显更适合路由拆分。

---

# 三十四、Overview

至少显示：

- Level
- EXP
- Energy
- 当前称号
- 最近 Growth Event
- 最近 Milestone
- Achievement 数量
- 7 天基础趋势

---

# 三十五、Achievements UI

至少支持：

```
全部已解锁未解锁隐藏
```

卡片显示：

```
图标名称描述进度解锁日期
```

例如：

```
百时
累计有效 
Focus
83h / 100h
████████░░
```

---

# 三十六、Milestones UI

推荐 Timeline / Journal 风格。

例如：

```
2026

Aug 29
Life HUD Growth 系统建立

Aug 28
第一次完整记录铁幕
```

允许：

- 新建
- 编辑
- 删除
- Pin

删除要求确认。

---

# 三十七、Titles UI

展示：

```
当前称号
可装备称号
未解锁称号
自定义称号
```

Equip 操作必须简单。

---

# 三十八、Today 页面联动

当前 Today 已有：

- Energy
- Level
- EXP
- Title

这些不能再是摆设。

v0.4 完成后：

点击：

```
Energy
EXP
Level
Title
```

可以进入 Growth 对应位置或展示详情。

---

## 38.1 Energy

Today 显示：

```
Energy
142 / 180

今日 +12
```

如果没有变化：

不强行显示 `+0`。

---

## 38.2 EXP

例如：

```
EXP
126 / 180

今日 +32
```

---

## 38.3 Level

例如：

```
Lv.17
距 Lv.18
54 EXP
```

保持简洁。

---

## 38.4 Title

显示：

```
信息绝缘体
```

并允许快速进入称号页面。

---

# 三十九、Focus 联动

v0.3 Focus 已经产生有效时间。

v0.4 必须正式消费：

```
FOCUS_FINISHED
```

尤其：

```
effectiveMinutes
```

作为成长计算依据。

例如：

```
铁幕
actual 4h10m
effective 3h10m

Growth 只根据有效时间计算。
```

---

# 四十、Task 联动

检查当前 Task 完成逻辑。

Task 完成后：

产生：

```
TASK_COMPLETED
```

并进入 Growth。

禁止：

Task Service 直接加 EXP。

---

# 四十一、Timeline 联动

Timeline 应能够展示重要成长节点。

至少：

```
LEVEL_UP
ACHIEVEMENT_UNLOCKED
MILESTONE_CREATED
TITLE_UNLOCKED
```

普通 EXP +5 不需要全部塞进 Timeline。

否则 Timeline 会被成长噪音淹没。

---

# 四十二、成长记录 / Growth Log

Growth 页面可以展示轻量日志：

```
08:42
完成 Focus
EXP +8

10:15
任务完成
EXP +4

14:30
Achievement
「铁幕初启」

14:30
Level Up
Lv.16 → Lv.17
```

这个日志主要来自：

```
LifeEvent + GrowthEventRecord
```

---

# 四十三、视觉语言

Growth 页面必须继承 Life HUD v0.2 / v0.3 的视觉体系。

保持：

- 白色 / 浅蓝
- 清透
- 壁纸
- 半透明卡片
- 柔和阴影
- 圆角
- 大量留白
- 清晰信息层级

---

# 四十四、避免 RPG UI

禁止：

- 金色边框满屏
- 游戏式属性面板
- 稀有度颜色
- SSR / S / A / B
- 宝箱
- 商店图标
- 金币 UI
- 大量奖励弹窗

Growth 应更接近：

> 长期生活档案 + 一点点游戏化仪式。

---

# 四十五、动画体系

v0.4 允许增加：

### EXP 增长

EXP bar 平滑推进。

### Level Up

独立轻量动画。

### Achievement Unlock

短暂卡片提示。

### Title Equip

轻微状态变化。

### Milestone Created

可以有轻微 Timeline 落点动画。

---

## 45.1 动画原则

继续遵循：

```
普通交互
120 - 200ms

状态变化
180 - 300ms

重要成长节点
300 - 600ms
```

支持：

```
prefers-reduced-motion
```

---

# 四十六、不要弹窗轰炸用户

如果一次大型数据迁移触发：

```
3 个 Level Up
12 个 Achievement
5 个 Title
```

禁止：

弹 20 次动画。

应该合并：

```
成长记录已同步

Lv.14 → Lv.17
解锁 12 个 Achievement
获得 5 个 Title
```

---

# 四十七、旧数据迁移

这是本版本重点。

当前项目已经存在旧：

- Energy
- EXP
- Level
- Title
- Achievement

以及早期宅宅能量条迁移遗留。

要求：

> 优先保留用户已有成长数据。

---

# 四十八、迁移原则

禁止：

```
升级 v0.4
↓
Lv.16 变 Lv.1
EXP 清零
称号消失
```

除非旧数据完全不可恢复。

---

## 48.1 EXP / Level

如果旧数据模型与新模型兼容：

直接迁移。

如果等级曲线变化：

优先保留：

```
totalExp
```

然后重新计算 Level。

但如果这样会造成非常大的等级变化：

需要考虑 migration compatibility。

---

## 48.2 Energy

保留当前值。

如果 maxEnergy 模型变化：

按比例或原值迁移。

避免直接满血或清零。

---

## 48.3 Achievement

旧成就：

如果仍然有意义：

迁入新的 Definition / Progress。

如果已经属于旧商店 / 旧游戏经济：

可以废弃。

记录 migration。

---

## 48.4 Title

现有：

```
信息绝缘体
```

等用户当前拥有 / 装备的称号必须保留。

---

# 四十九、删除旧 Shop 遗留

如果 Java 项目里仍存在：

- Shop
- ShopItem
- Currency
- Coin
- Purchase
- Redemption
- shop.json
- shop API
- shop UI
- 未使用的商城 CSS / JS

进行检查。

如果确认已经没有业务依赖：

删除。

如果删除风险较高：

标记 deprecated 并从 UI / API 主流程断开。

不要让死功能继续污染 Growth 架构。

---

# 五十、规则内容

v0.4 可以准备一批基础 Achievement 和 Title。

数量不需要非常多。

建议：

```
10 - 30 个 Achievement
5 - 15 个 Title
```

用于验证系统。

优先围绕当前真实已有模块：

- Focus
- Iron Curtain
- Pomodoro
- Task
- Level
- LifeEvent
- Milestone

不要为了凑数量给不存在的 Sleep / Meal 创建假成就。

---

# 五十一、建议基础成就

可根据最终实现调整文案。

例如：

```
铁幕初启
完成第一次 Iron Curtain

幕布之后
完成 10 次 Iron Curtain

百时
累计有效 Focus 100h

专注者
累计完成 100 次 Focus

长夜
完成一次跨越午夜的 Focus

深潜
单次有效 Focus ≥ 180min

番茄熟了
完成第一次 Pomodoro

小小一步
完成第一个 Task

百件
累计完成 100 个 Task

轨迹开始
产生第 100 个 LifeEvent

生活档案
产生第 1000 个 LifeEvent

拾起一刻
创建第一个 Milestone
```

---

# 五十二、建议基础称号

例如：

```
铁幕行者
夜航者
百时专注
生活记录者
里程碑收藏家
```

保留现有：

```
信息绝缘体
```

---

# 五十三、Achievement / Title 文案

文案不要：

```
恭喜玩家获得...
```

统一使用 Life HUD 自身语言。

例如：

```
Achievement Unlocked

百时

一百小时，已经真正留在这里。
```

或更简洁。

---

# 五十四、API

按当前 REST 风格设计。

可以考虑：

```
GET /api/growth
GET /api/growth/history
GET /api/growth/snapshots

GET /api/achievements
GET /api/achievements/{id}

GET /api/milestones
POST /api/milestones
PATCH /api/milestones/{id}
DELETE /api/milestones/{id}

GET /api/titles
POST /api/titles/{id}/equip
```

具体路径根据现有 Controller 风格调整。

---

# 五十五、后端结构建议

根据项目规模合理拆分：

```
growth/
├── GrowthService
├── GrowthEngine
├── GrowthRule
├── GrowthSnapshotService
└── ...

achievement/
├── AchievementService
└── ...

milestone/
├── MilestoneService
└── ...

title/
├── TitleService
└── ...
```

不要机械为了架构图制造空壳类。

保持 Java / Spring Boot 自然风格。

---

# 五十六、状态计算

需要明确：

```
Level
EXP
Energy
Achievement
```

哪些是：

- 持久化状态
- 可重新计算状态
- 缓存
- 派生数据

避免多个 JSON 文件分别成为“事实”。

---

# 五十七、数据存储

继续遵循当前项目持久化方案。

如果当前仍使用 JSON：

v0.4 不要求因为 Growth 强行迁移数据库。

但：

- 写入必须原子
- 避免并发损坏
- 避免大量重复 IO
- 数据模型留出未来迁移空间

---

# 五十八、可靠性

必须处理：

- 应用重启
- 页面刷新
- 重复请求
- LifeEvent 重放
- 历史数据修改
- 多次点击按钮
- Achievement 重复解锁
- Title 重复 unlock
- Level Up 跨多级
- Snapshot 重复生成

---

# 五十九、多级 Level Up

例如一次导入大量旧数据：

```
Lv.10
↓
Lv.15
```

必须正确：

- 计算最终 Level
- 记录 Level 变化
- 不产生错误中间状态

是否产生 5 条 LEVEL_UP Event：

根据当前事件设计决定。

UI 最终可以合并显示。

---

# 六十、Achievement 重新计算

Achievement 必须支持：

```
recalculate
```

场景：

- 迁移旧数据
- 修复规则
- 用户已有 200h Focus，但系统今天才新增“百时”

系统应该能够识别：

```
条件已经满足
```

并正确解锁。

但重算不能重复产生无限 Unlock Event。

---

# 六十一、Milestone 删除

Milestone 属于用户主动记录。

必须支持删除。

删除时：

- 确认
- 不删除关联原始 LifeEvent
- Timeline 正确同步

---

# 六十二、Title 删除

系统 Title：

禁止删除。

用户自定义 Title：

可以删除。

如果删除的是当前装备称号：

自动取消装备。

---

# 六十三、响应式

测试：

```
1920+
1440
1280
1024
768
390
```

Growth 页面尤其检查：

- EXP Bar
- Achievement Grid
- Milestone Timeline
- Title Grid
- Growth Summary
- Tab
- 长文字
- Level 大数字

不得横向溢出。

---

# 六十四、空状态

必须准备：

### 无 Achievement

```
成长还没有被写成成就。
```

### 无 Milestone

```
还没有留下里程碑。
```

### 无 Title

```
还没有获得新的称号。
```

### 无 Snapshot

不要显示破碎图表。

---

# 六十五、测试

至少建立或扩充自动测试。

---

## Case A：Focus Growth

```
Focus Finished
effective = 120min
```

Growth Rule 正确生成 EXP。

BREAK / INTERRUPTION 不参与。

---

## Case B：重复 Event

同一个：

```
FOCUS_FINISHED #123
```

处理两次。

最终 EXP：

只能增加一次。

---

## Case C：Level Up

EXP：

```
49 / 50
```

获得：

```
+5
```

正确升级。

---

## Case D：跨多级

一次迁移大量 EXP。

最终 Level 正确。

---

## Case E：Achievement

累计 Focus 达到条件。

Achievement 解锁一次。

重复查询：

不得重复解锁。

---

## Case F：Title

Achievement 解锁关联 Title。

Title 正确获得。

Equip 后：

Today 正确显示。

---

## Case G：Milestone

创建。

编辑。

删除。

Timeline 正确同步。

---

## Case H：Snapshot

同一天多次生成。

只能保留一个有效 Snapshot。

---

## Case I：旧数据

读取 v0.3 用户数据。

必须：

- Energy 保留
- EXP 保留
- Level 正常
- Title 保留
- Focus History 保留
- Task 保留

---

## Case J：Shop 删除

旧 Shop 相关逻辑不会导致：

- 启动失败
- API 报错
- 页面 JS 报错

---

# 六十六、性能

Achievement 检查不能每个页面刷新都：

```
扫描所有 Focus
扫描所有 Task
扫描所有 LifeEvent
扫描所有历史
```

优先使用：

- 统计结果
- Event-driven update
- 缓存 / 聚合
- 必要时 Recalculate

避免长期数据增加后性能持续恶化。

---

# 六十七、文档

更新：

```
README.md
docs/ARCHITECTURE.md
docs/CODEBASE_STATUS.md
```

记录：

```
Growth
LifeEvent
Growth Engine
Energy
EXP
Level
Achievement
Milestone
Title
GrowthSnapshot
```

并明确：

```
Shop / Currency 已不属于 Life HUD 产品方向。
```

---

# 六十八、版本

统一更新：

```
v0.4.0
```

无需人为拆：

```
v0.4.1
v0.4.2
```

Codex 可以一次完成全部开发。

如果实现过程中发现高度相关的后续基础设施：

允许合理提前实现。

要求：

- 不破坏已有系统
- 不留下半成品
- 不进行与当前目标无关的大规模重构

---

# 六十九、开发优先级

虽然本任务允许一次性完成，但内部建议按以下顺序实施：

```
1. 检查当前数据与旧 Growth 代码
2. LifeEvent 完善
3. Growth Engine
4. EXP / Level
5. Energy
6. Achievement
7. Title
8. Milestone
9. Growth Snapshot
10. Growth UI
11. Today / Timeline 联动
12. 动画
13. 数据迁移
14. 测试
15. 文档
```

如果 Codex 判断其他顺序更符合当前代码结构：

允许调整。

---

# 七十、代码要求

继续保持 Java 化重构方向：

- 使用明确类型
- 使用 enum
- 避免 Map<String, Object> 承担核心模型
- Controller 不写核心业务
- Service 边界清晰
- 避免超长方法
- 避免字符串状态判断
- 避免 Python 转 Java 风格
- 避免重复工具函数
- 新代码与现有 Java 21 / Spring Boot 风格一致

---

# 七十一、最终体验

完成 v0.4 后：

用户打开 Life HUD。

Today 不再只是显示：

```
Energy 180 / 180
Lv.16
EXP 34 / 50
称号：信息绝缘体
```

这些孤立数字。

而是可以真正回答：

```
今天发生了什么？
最近做了多少事情？
这一周状态如何？
从开始使用 Life HUD 到现在走了多远？
有哪些值得记住的节点？
什么时候获得了某个成就？
为什么 Level 变成了现在这样？
过去的自己是什么样？
```

---

# 七十二、Definition of Done

v0.4 完成必须满足：

- LifeEvent 成为 Growth 的统一输入
- Growth Event 幂等
- Focus / Task 不直接操作 EXP
- Energy 有明确语义
- Energy 不再是货币
- EXP 可长期累计
- Level 正确计算
- Level Up 有记录和反馈
- Achievement 可计算进度
- Achievement 可自动解锁
- Hidden Achievement 可用
- Title 可解锁
- Title 可装备
- 现有称号得到保留
- Milestone 可创建 / 编辑 / 删除
- Milestone 进入 Timeline
- GrowthSnapshot 正常生成
- Growth 页面完整可用
- Today 数据真正联动
- Timeline 能看到重要成长事件
- Focus effectiveMinutes 正确进入 Growth
- 旧数据成功迁移
- Shop / Coin 不再进入产品主流程
- 无重复奖励 / 重复解锁
- 刷新 / 重启无异常
- 响应式正常
- Console 无明显错误
- 后端测试通过
- 文档更新
- 版本号更新为 v0.4.0

---

# v0.4 一句话

> Life HUD v0.4 · Growth

生活不是为了升级。

等级、成就与里程碑，只是为了让已经走过的路能够被看见。
