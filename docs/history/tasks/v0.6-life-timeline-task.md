# Life HUD v0.6 总开发任务书

> 版本：v0.6.0  
> 主题：**Life / 生活输入与万能时间线**
>
> 核心目标：
>
> **让 Life HUD 开始真正记录“人是怎么生活的”。**
>
> v0.6 不追求复杂健康分析，不追求 AI，也不追求精密量化。
>
> 这一版首先完成：
>
> ```text
> 生活发生
>     ↓
> 原始记录
>     ↓
> LifeEvent
>     ↓
> Life Timeline
> ```
>
> 为 v0.7 Media、v0.8 Dashboard / Agent Context、v0.9 朝汐提供统一生活事实层。

---

# 0. 本轮范围

v0.6 主要包含两块：

```text
Life
├── Sleep
├── Meal
├── Exercise
├── Check-in
└── LifeRecord

Journal
├── Life Timeline
└── Manual Journal
```

其中：

- `Life` 负责记录生活
- `Journal / Timeline` 负责重新看见生活
- `LifeEvent` 负责连接所有业务域

---

# 1. 核心原则

## 1.1 原始记录是真相

SleepRecord、MealRecord、ExerciseRecord、CheckIn、LifeRecord 等业务记录是事实源。

LifeEvent 是它们进入统一生活时间线后的事件表达。

不要反过来只保存 LifeEvent。

正确结构：

```text
SleepRecord
    ↓
SLEEP_RECORDED LifeEvent
```

而不是：

```text
只有一条 JSON LifeEvent
里面塞完所有睡眠数据
```

复杂业务必须保留自己的结构化数据。

---

## 1.2 简单记录通用化，复杂业务独立化

独立模型：

```text
SleepRecord
MealRecord
ExerciseRecord
CheckIn
JournalEntry
```

通用模型：

```text
LifeRecord
```

LifeRecord 用来承载：

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

禁止继续扩散：

```text
WaterController
CoffeeController
AlcoholController
SunlightController
...
```

---

## 1.3 Life Timeline 不是数据库 Dump

Timeline 不应简单把 LifeEvent 原始字段全部打印出来。

Timeline 是：

> 面向人的生活时间线。

每一种事件应该有：

- 图标 / 类型
- 时间
- 标题
- 简短摘要
- 必要的关键数据
- 来源
- 可展开详情

不要把：

```json
metadata
sourceId
createdAt
updatedAt
```

直接堆到 UI。

---

# 2. LifeEvent 扩展

在现有 LifeEvent 基础上新增事件类型：

```text
SLEEP_RECORDED
MEAL_RECORDED
EXERCISE_RECORDED
CHECK_IN_RECORDED
LIFE_RECORDED
JOURNAL_WRITTEN
```

如果现有 LifeEvent 已存在：

```text
TASK_COMPLETED
FOCUS_STARTED
FOCUS_FINISHED
RITUAL_COMPLETED
ENERGY_CHANGED
...
```

必须继续兼容。

---

## 2.1 推荐事件关联字段

如果现有模型尚未具备，建议补充：

```text
LifeEvent
├── id
├── type
├── source
├── sourceId
├── title
├── content
├── occurredAt
├── createdAt
├── tags
└── metadata
```

其中：

```text
source
```

表示业务来源，例如：

```text
SLEEP
MEAL
EXERCISE
CHECK_IN
LIFE_RECORD
JOURNAL
FOCUS
TASK
RITUAL
GROWTH
```

`sourceId` 用于定位原始业务记录。

---

## 2.2 事件时间语义

必须区分：

```text
occurredAt
createdAt
```

例如：

```text
23:30 吃了夜宵
00:20 才补录
```

Timeline 应显示：

```text
23:30
夜宵
```

而不是：

```text
00:20
夜宵
```

Timeline 默认按照 `occurredAt` 排序。

---

# 3. Sleep / 睡眠

## 3.1 数据模型

```text
SleepRecord
├── id
├── sleepTime
├── wakeTime
├── durationMinutes
├── quality
├── type
├── note
├── createdAt
└── updatedAt
```

类型：

```text
NIGHT
NAP
OTHER
```

---

## 3.2 基础能力

支持：

- 新建睡眠记录
- 编辑
- 删除
- 查看历史
- 入睡时间
- 起床时间
- 自动计算睡眠时长
- 睡眠质量
- 类型
- 备注

睡眠质量：

```text
1 ~ 5
```

或现有项目更适合的统一等级。

不要做复杂睡眠阶段。

---

## 3.3 跨日处理

睡眠最容易出现日期 Bug。

例如：

```text
2026-08-29 02:10 入睡
2026-08-29 09:40 起床
```

以及：

```text
2026-08-28 23:40 入睡
2026-08-29 07:30 起床
```

都必须正确计算。

必须检查：

- 跨午夜
- 午睡
- 手动补录过去日期
- wakeTime <= sleepTime 的非法情况
- 超长睡眠异常值

不要默认“睡眠属于创建记录那一天”。

---

## 3.4 LifeEvent

保存成功后生成：

```text
SLEEP_RECORDED
```

示例：

```text
07:42
睡眠

睡了 7h 36min
质量 4 / 5
```

事件 `occurredAt` 推荐使用：

```text
wakeTime
```

因为睡眠在醒来时成为完整事实。

---

# 4. Meal / 饮食

## 4.1 数据模型

```text
MealRecord
├── id
├── mealType
├── time
├── description
├── satisfaction
├── note
├── images
├── createdAt
└── updatedAt
```

类型：

```text
BREAKFAST
LUNCH
DINNER
SNACK
OTHER
```

---

# 4.2 本版定位

只记录：

```text
什么时候
+
吃了什么
+
照片
+
一句感受
```

禁止在 v0.6 顺手扩张：

- 卡路里
- 蛋白质
- 脂肪
- 碳水
- 营养目标
- AI 菜品识别
- OCR
- 自动健康建议

这些全部以后再说。

---

# 4.3 图片上传

支持：

- 单张图片
- 多张图片
- 图片预览
- 删除已上传图片
- 编辑 Meal 时保留已有图片

图片必须通过统一文件存储方案处理。

禁止：

```text
直接把 base64 图片塞数据库
```

数据库只保存：

```text
路径 / 文件标识 / URL
```

---

## 4.4 文件安全

最低要求：

- 限制图片类型
- 限制文件大小
- 生成安全文件名
- 禁止路径穿越
- 删除 Meal 时处理关联图片
- 图片不存在时 UI 不崩溃

本版本优先支持：

```text
jpg
jpeg
png
webp
```

---

## 4.5 LifeEvent

生成：

```text
MEAL_RECORDED
```

示例：

```text
18:42
晚餐

辣椒炒肉 + 米饭

[图片]
满意度 4 / 5
```

Timeline 可显示第一张图作为缩略图。

---

# 5. Exercise / 运动

## 5.1 数据模型

```text
ExerciseRecord
├── id
├── type
├── startTime
├── durationMinutes
├── intensity
├── note
├── createdAt
└── updatedAt
```

首批类型：

```text
STRENGTH
WALK
CYCLING
MAIMAI
RUNNING
OTHER
```

UI 显示中文。

---

## 5.2 强度

第一版保持简单：

```text
LOW
MEDIUM
HIGH
```

或者：

```text
1 ~ 5
```

选择项目现有交互风格中更统一的一套。

不要加入：

- 心率
- 卡路里
- 配速分析
- GPS
- 路线
- 运动计划

---

## 5.3 LifeEvent

生成：

```text
EXERCISE_RECORDED
```

示例：

```text
20:16
舞萌

86 min
中等强度
```

---

# 6. Check-in / 当前状态

这是 v0.6 的重点之一。

其意义不是：

> 又增加四个数字。

而是帮助未来朝汐理解：

> 人在做一件事情之前和之后是什么状态。

---

## 6.1 数据模型

```text
CheckIn
├── id
├── energy
├── mood
├── focusDesire
├── fatigue
├── time
├── note
├── createdAt
└── updatedAt
```

全部：

```text
1 ~ 10
```

---

## 6.2 UI

Check-in 必须做到：

> 10 秒左右可以完成一次记录。

不要做复杂表单。

建议采用四个横向滑杆 / 分段按钮：

```text
能量       1 ─────── 10
心情       1 ─────── 10
专注意愿   1 ─────── 10
疲劳       1 ─────── 10
```

备注可选。

提供：

```text
快速记录
```

入口。

---

## 6.3 最近状态

Life 页面显示最近一次 Check-in。

例如：

```text
当前状态

能量      7
心情      8
专注意愿  5
疲劳      3

22:14 更新
```

不要假装这是实时状态。

必须明确显示：

```text
最近记录时间
```

---

## 6.4 LifeEvent

生成：

```text
CHECK_IN_RECORDED
```

Timeline 默认可以压缩显示：

```text
22:14
状态记录

能量 7 · 心情 8 · 专注 5 · 疲劳 3
```

---

# 7. LifeRecord / 通用生活记录

## 7.1 数据模型

```text
LifeRecord
├── id
├── type
├── value
├── unit
├── time
├── note
├── metadata
├── createdAt
└── updatedAt
```

首批类型：

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

---

## 7.2 不同类型只定义表现，不拆业务层

例如：

```text
WATER
value = 500
unit = ml
```

```text
CAFFEINE
value = 1
unit = cup
```

```text
ALCOHOL
value = 30
unit = ml
```

```text
SUNLIGHT
value = 25
unit = min
```

仍然全部经过：

```text
LifeRecordController
LifeRecordService
LifeRecordRepository
```

不要拆 Controller。

---

## 7.3 CUSTOM

允许：

```text
type = CUSTOM
```

并在 metadata 或附加字段记录自定义类别名称。

这是未来小众生活记录的逃生舱。

不要因为用户想记一个新东西就创建新表。

---

# 8. Life 页面

建议 `/life` 不做成传统后台 CRUD 表格。

它应该更接近：

> 今天生活的输入面板。

---

## 8.1 页面结构建议

```text
Life
│
├── 最近状态
│
├── 快速记录
│   ├── 睡眠
│   ├── 饮食
│   ├── Check-in
│   ├── 运动
│   └── 其他
│
├── 今日生活
│
└── 历史记录入口
```

快捷入口优先级：

```text
Check-in
Meal
Sleep
Exercise
LifeRecord
```

---

## 8.2 交互原则

Life 是高频输入域。

要求：

- 少跳页面
- Modal / Drawer 优先
- 表单尽量短
- 提供合理默认时间
- 时间允许修改
- 保存后立即反馈
- 保存后对应 Timeline 可看到

不要：

```text
点击 Life
→ 点击 Sleep
→ 点击 Add
→ 再进新页面
→ 填十几个字段
```

---

# 9. Journal / 手动日记

## 9.1 数据模型

建议：

```text
JournalEntry
├── id
├── content
├── occurredAt
├── images
├── tags
├── createdAt
└── updatedAt
```

第一版不做复杂富文本。

支持：

- 长文本
- 一句话
- 标签
- 图片
- 指定发生时间
- 修改
- 删除

---

## 9.2 LifeEvent

生成：

```text
JOURNAL_WRITTEN
```

Timeline 中展示：

```text
00:14
日记

今天很喜欢……
```

内容较长时折叠。

---

# 10. Life Timeline

这是 v0.6 最核心的交付物之一。

---

## 10.1 聚合来源

至少正确显示现有：

```text
Focus
Task
Ritual
Growth
Sleep
Meal
Exercise
Check-in
LifeRecord
Journal
```

Dream / Now 如果已有适合进入 LifeEvent 的事件，也可以显示。

但禁止为了“覆盖模块”而强行制造无意义事件。

---

## 10.2 时间线按天组织

例如：

```text
2026-08-29

09:42
Sleep
睡眠 7h31min

10:06
Meal
早餐

12:17
Check-in
能量 7 / 心情 8

14:03
Focus
铁幕开幕

17:31
Focus
铁幕落幕
有效专注 182min

18:12
Task
完成 Life HUD v0.6

19:04
Meal
晚餐

21:13
Exercise
舞萌 92min

23:48
Journal
今天……
```

---

## 10.3 Timeline API

推荐提供：

```text
GET /api/timeline
```

支持：

```text
date
startDate
endDate
type
source
page
size
```

例如：

```text
GET /api/timeline?date=2026-08-29
```

或者：

```text
GET /api/timeline?startDate=...&endDate=...
```

不要让前端自己分别请求十个业务接口再拼时间线。

Timeline 聚合属于后端职责。

---

## 10.4 Timeline DTO

不要直接返回 Entity。

建议统一：

```text
TimelineItem
├── eventId
├── type
├── source
├── sourceId
├── occurredAt
├── title
├── summary
├── tags
├── media
└── metadata
```

其中 metadata 只放前端确实需要的展示信息。

---

## 10.5 图片事件

Meal / Journal 有图片时：

TimelineItem 支持：

```text
media[]
```

第一版：

- 显示缩略图
- 点击可查看大图

不要把 Timeline 变成瀑布流相册。

---

# 11. Timeline 过滤

第一版至少支持：

```text
全部
Focus
Task
Life
Journal
Growth
Ritual
```

Life 内部无需一开始再拆十几个按钮。

后端结构允许以后继续筛：

```text
Sleep
Meal
Exercise
Check-in
...
```

---

# 12. 删除与修改同步

这是本版必须重点检查的地方。

例如：

```text
创建 MealRecord
↓
产生 MEAL_RECORDED

修改 MealRecord
↓
Timeline 应反映新内容

删除 MealRecord
↓
对应事件不能继续作为幽灵存在
```

同理适用于：

```text
Sleep
Exercise
Check-in
LifeRecord
Journal
```

必须制定统一策略。

推荐：

```text
原业务记录修改
→ 同步更新对应 LifeEvent

原业务记录删除
→ 删除 / 软删除对应 LifeEvent
```

不要产生“记录已经没了但 Timeline 还活着”的幽灵事件。

---

# 13. 幂等与事务

每个业务记录原则上只对应一个主要 CREATED / RECORDED LifeEvent。

例如：

```text
SleepRecord #123
```

不应该因为重复保存或重试出现：

```text
SLEEP_RECORDED
SLEEP_RECORDED
SLEEP_RECORDED
```

保存业务记录和生成事件尽量放在同一事务语义中。

---

# 14. API 建议

建议统一风格：

```text
/api/life/sleep
/api/life/meals
/api/life/exercises
/api/life/check-ins
/api/life/records

/api/journal
/api/timeline
```

典型 CRUD：

```text
GET
POST
PUT / PATCH
DELETE
```

不要再创造每种业务完全不同的 API 命名风格。

---

# 15. 数据库迁移

必须通过正式 migration 更新。

新增核心表：

```text
sleep_records
meal_records
meal_images
exercise_records
check_ins
life_records
journal_entries
journal_images
```

LifeEvent 如需新增：

```text
source_id
```

等字段，也必须通过 migration。

不要依赖：

```text
ddl-auto=create
```

破坏已有 v0.2 ~ v0.5 数据。

---

# 16. 前端视觉与 UX

继续复用当前 Life HUD 已确定的视觉体系。

禁止 v0.6 又做成另一套后台管理 UI。

Life 页面和 Timeline 尤其应该有：

- 空气感
- 时间感
- 生活记录感
- 清晰但不过度信息密集

Timeline 卡片不要全部长得一样。

可以根据来源存在轻度差异：

```text
Sleep
Meal
Focus
Task
Journal
```

但保持统一设计语言。

---

# 17. Empty State

所有新模块必须处理空数据。

例如：

Sleep：

> 这里还没有睡眠记录。

Meal：

> 今天还没有留下吃饭的痕迹。

Timeline：

> 这一天暂时还是一张白纸。

不要显示：

```text
No data.
[]
null
```

---

# 18. 错误处理

最低要求：

- 表单校验
- 上传失败提示
- API 失败 Toast
- 时间非法提示
- 图片加载失败 fallback
- 空字段处理
- 删除确认
- 网络异常不导致整个页面白屏

---

# 19. 测试重点

## Sleep

测试：

```text
跨午夜
午睡
同日睡眠
过去日期补录
非法时间
修改
删除
```

---

## Meal

测试：

```text
无图
单图
多图
删除图片
替换图片
删除记录
错误格式
超大图片
```

---

## Check-in

测试：

```text
1
10
中间值
无备注
补录过去时间
```

---

## LifeRecord

测试：

```text
WATER
CAFFEINE
ALCOHOL
SUNLIGHT
CUSTOM
```

确认全部共用统一业务链路。

---

## Timeline

至少验证：

```text
Focus
Task
Ritual
Growth
Sleep
Meal
Exercise
Check-in
LifeRecord
Journal
```

可以正确混排。

重点检查：

```text
排序
跨天
分页
过滤
修改同步
删除同步
图片
空日期
```

---

# 20. 本版明确不做

严禁开发过程中顺手扩范围：

```text
❌ 卡路里计算
❌ 营养分析
❌ AI 菜品识别
❌ 睡眠阶段
❌ Apple Health
❌ 手环
❌ GPS
❌ 心率
❌ 自动运动识别
❌ AI Journal
❌ AI 日评
❌ AI 周报
❌ 行为相关性分析
❌ Media
❌ Anime / Game
❌ Agent
```

这些不是 v0.6 的工作。

---

# 21. 开发优先级

## P0

必须完成：

```text
SleepRecord
MealRecord + 图片
CheckIn
LifeRecord
LifeEvent 联动
Life Timeline
Manual Journal
```

---

## P1

强烈建议完成：

```text
ExerciseRecord
Timeline 过滤
图片预览
历史记录
Life 首页快速录入
```

---

## P2

有余力再做：

```text
更漂亮的 Timeline 动画
复杂筛选
Timeline 搜索
日历视图
统计图
大量快捷模板
```

---

# 22. 推荐施工顺序

不要五条业务线同时铺开。

按照：

```text
① LifeEvent / Timeline 地基
        ↓
② Sleep
        ↓
③ Check-in
        ↓
④ LifeRecord
        ↓
⑤ Meal + 图片系统
        ↓
⑥ Exercise
        ↓
⑦ Journal
        ↓
⑧ Timeline UI 聚合
        ↓
⑨ 删除 / 修改同步
        ↓
⑩ 回归测试
```

其中建议先让：

```text
Sleep → LifeEvent → Timeline
```

完整跑通一次。

确认整条链路正确之后，再复制模式给 Meal / Exercise / Check-in。

不要先造五套 CRUD，最后才发现事件系统不对。

---

# 23. 完整验收场景

最终进行一次真实生活录入测试。

例如：

```text
09:37
补录昨夜睡眠
7h31min / 质量 4

10:02
Check-in
能量 7
心情 8
专注意愿 6
疲劳 3

10:20
早餐
上传一张照片
“两个肉包”

14:00
铁幕开幕

17:30
铁幕落幕

18:20
晚餐
上传照片

20:10
舞萌
90min

23:40
写下一句话日记
```

然后打开 Timeline。

应该能够得到一条自然、完整、按时间排列的生活轨迹。

整个过程中：

- 不需要手动制造 LifeEvent
- 不需要刷新数据库
- 不出现重复事件
- 不出现幽灵事件
- 图片正常
- 修改后同步
- 删除后同步
- 页面不报错

---

# 24. v0.6 完成标准

只有同时满足以下条件，才能结束 v0.6：

- [ ] Sleep 可以完整记录
- [ ] 睡眠时长自动计算正确
- [ ] Meal 可以记录并上传图片
- [ ] Exercise 可以记录
- [ ] Check-in 可以快速完成
- [ ] LifeRecord 可以承载多个简单类别
- [ ] Journal 可以写文字 / 图片记录
- [ ] 所有重要行为自动生成 LifeEvent
- [ ] Timeline 可以聚合 v0.2 ~ v0.6 的主要事件
- [ ] Timeline 使用 occurredAt 正确排序
- [ ] 修改记录后 Timeline 同步
- [ ] 删除记录后不存在幽灵事件
- [ ] 图片上传和删除链路可靠
- [ ] 数据库 migration 不破坏历史数据
- [ ] 完成一次真实生活全链路测试
- [ ] 项目版本更新为 `v0.6.0`

---

# 25. v0.6 的最终意义

v0.5 以前，Life HUD 更多知道的是：

```text
暗苟想做什么
暗苟正在追逐什么
暗苟完成了什么
```

v0.6 之后，它开始知道：

```text
暗苟几点醒来
睡了多久
吃了什么
现在是什么状态
有没有运动
今天发生过什么
什么时候发生
当时留下了什么感受
```

这些数据暂时不需要被“理解”。

只需要真实、可靠、连续地存在。

因为未来：

```text
Sleep
Meal
Exercise
Check-in
Focus
Task
Dream
Ritual
Media
Journal
        ↓
    Life Timeline
        ↓
 Agent Context API
        ↓
      朝汐
```

朝汐真正需要的并不是更多漂亮页面。

而是一条足够真实的生活河流。

**v0.6 的任务，就是先让这条河开始流起来。**