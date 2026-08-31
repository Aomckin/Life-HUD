# Life HUD v0.2 ~ v1.0 开发计划（2026-08-31 修订版）

> 文档关系：本文件是当前有效的 v0.2～v1.0 总计划；`Life_HUD_v0.2-v0.9_Development_Plan_ARCHIVE.md` 是历史计划归档；`Life HUD v0.9 正式版收官总任务书.md` 是从 v0.8.1 向 v1.0.0 封版的执行与验收依据。

> 当前状态：Life HUD 主体业务已经完成到 v0.8.1。
> 下一阶段目标：**不再继续横向扩张大业务域，而是完善 Agent 对外接口、补齐内容与交互细节、完成稳定性收尾，然后进入 v1.0。**
>
> 核心路线：
>
> **Life HUD 先成为一套完整、独立、稳定的个人生活事实系统。**
>
> **朝汐是外部全能 Agent，Life HUD 只是它能够调用的一套大型 Tool。**
>
> 两者版本线从此正式解耦。

---

# 0. 产品定位

Life HUD 不是：

> 任务管理 + 能量条。

也不是：

> 朝汐的后端数据库。

Life HUD 应该独立成为：

> **一套记录一个人如何生活、如何行动、如何休息、如何娱乐、如何成长、正在追逐什么，以及这些事实如何彼此关联的个人 Life HUD。**

朝汐不属于 Life HUD 本体。

朝汐是：

> **连接 Life HUD、GitHub、Calendar、Files、MemeVault 等外部系统的全能 Agent。**

因此最终关系为：

```text
                        Zhaoxi
                  General-purpose Agent
                         │
              ┌──────────┼──────────┐
              │          │          │
              ▼          ▼          ▼
         Life HUD      GitHub     Calendar
          Tool Pack     Tool        Tool
              │
              ▼
           REST API
              │
              ▼
          Life HUD
```

Life HUD 不需要知道调用者一定是朝汐。

它只需要提供：

```text
稳定事实
稳定业务规则
稳定 API
稳定 Agent Contract
```

---

# 1. 最终产品结构

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
               Public / Agent API
                          │
                ┌─────────┴─────────┐
                ▼                   ▼
              Zhaoxi             Other Agent
```

核心数据流：

```text
现实生活
   ↓
业务记录
   ↓
LifeEvent
   ↓
┌────────────┬────────────┬──────────────┐
│            │            │              │
Journal    Growth      Dashboard     Agent Context
│            │            │              │
历史        成长反馈      给人看          给 Agent 看
```

---

# 2. 长期架构约束

## 2.1 Life HUD 是事实与业务系统

负责：

```text
记录
校验
持久化
业务规则
LifeEvent
Timeline
Growth
Dashboard 聚合
Agent Context
```

## 2.2 Agent 是理解与交互层

Agent 可以：

```text
查询
总结
分析
规划
建议
调用 Life HUD API 执行业务
```

但不能：

```text
直接读取 data/*.json
直接修改 Life HUD 数据库
复制一份 Life HUD 动态状态作为自己的第二事实源
```

## 2.3 原始事实与 AI 解释分离

```text
Life HUD
= 原始事实

Zhaoxi / AI
= 对事实的理解与解释
```

AI 日评、周报、建议和分析：

> **永远不能覆盖原始 LifeEvent。**

## 2.4 大域稳定，小类别可扩展

继续遵守：

> **简单记录走通用模型，复杂业务升格独立模型。**

例如：

```text
独立模型
SleepRecord
MealRecord
FocusSession
Dream
Anime
MediaGame

通用模型
LifeRecord
```

禁止重新回到：

```text
WaterController
CoffeeController
SunlightController
AlcoholController
...
```

---

# 3. 已完成版本

## v0.2 —— Shell / Summer Sky / 全局骨架

完成方向：

```text
Summer Sky Shell
全局导航
统一卡片
统一按钮
统一表单
Modal
Toast
Loading / Empty / Error
Appearance
自定义壁纸
LifeEvent 地基
```

结果：

> Life HUD 从旧 Python/FastAPI 时代顺手做的网页界面，进入独立产品阶段。

**状态：已完成。**

## v0.3 —— Focus / 铁幕 / 人性化番茄钟

完成：

```text
FocusSession
IRON_CURTAIN
POMODORO
FREE

暂停 / 恢复
Segment
有效时长
中断
手动补录
任务关联
History
Today Summary
```

铁幕从个人习惯正式产品化。

**状态：已完成。**

## v0.4 —— Growth / 长期成长循环

完成核心循环：

```text
Focus / Task
→ Energy EARN

Entertainment SPEND
→ 实际 Energy 消耗
→ EXP

EXP
→ Level
```

并建立：

```text
Achievement
Milestone
Title
Growth Snapshot
Energy Ledger
Growth Event 幂等
```

核心原则：

```text
Energy
= 短期状态，变化快

EXP
= 长期积累，变化慢
```

Shop / Coin / Title Buff 退出主流程。

**状态：已完成。**

## v0.5 —— Direction / 梦想、任务、仪式与「现在。」

完成：

```text
Dream
↓
Goal
↓
DreamMilestone
↓
Task Direction
```

以及：

```text
Ritual
```

和：

```text
「现在。」
阶段陈列 / 歌单 / 图片 / 当前方向 / 阶段快照
```

后续 v0.5.x 完成：

```text
Tasks 行动台重构
Now 真实媒体上传
歌单沉浸布局
LifeEvent 事实化
Ritual 状态入口重构
```

**状态：已完成。**

## v0.6 —— Life / 生活输入与万能时间线

完成 Life 大域：

```text
Sleep
Meal
Exercise
Check-in
LifeRecord
Journal
图片上传
```

统一事实链：

```text
业务记录
↕
唯一 LifeEvent
↕
Timeline
```

支持：

```text
创建
编辑原位重写
删除同步清理
occurredAt 真实时间语义
补录
多图
```

Journal 成为统一 Life Timeline。

**状态：已完成。**

## v0.7 —— Media / 宅宅生活档案

完成：

```text
Anime
AnimeWatchSession

MediaGame
MediaGameSession

Book
Manga
Movie
Other
```

Media 的核心不再是：

> “娱乐了多久？”

而是：

> **什么作品在什么时候陪伴过自己。**

Session 与 LifeEvent 同步：

```text
ANIME_WATCHED
GAME_PLAYED
```

并接入 Journal。

**状态：已完成。**

## v0.8 —— Integration / 驾驶舱组装

完成：

```text
Growth Integration
AchievementEvaluator
跨域 Achievement
Title 联动
Growth Snapshot

Dashboard 2.0
Agent Context API
统一日期边界
```

Agent Context API：

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

所有 Context 响应使用：

```text
schemaVersion = "1"
```

Life HUD 至此已经拥有：

```text
事实层
业务层
成长层
时间线
聚合层
Agent 读取边界
```

**状态：已完成。**

## v0.8.1 —— Dashboard HUD 化

完成方向：

```text
主状态舱
今日脉搏
Dream / Ritual / Media 陪伴舱
Footprints
Summer Sky 更深参与页面层次
随机今日文案
```

日期降级为辅助信息。

今日大标题改为文案库随机抽取，例如：

```text
真挚！与热诚！
```

Dashboard 从：

> “今天的数据摘要”

继续靠近：

> **“今天的自己。”**

**状态：已完成。**

---

# 4. 版本路线修订

旧计划中的：

```text
v0.9
朝汐 Agent
```

从 Life HUD 版本路线中正式移除。

原因：

```text
Zhaoxi
≠
Life HUD 内置 Agent

Zhaoxi
=
独立全能 Agent
```

Life HUD 只负责成为它的一套外部 Tool。

因此后续路线修改为：

```text
v0.8.1
    ↓
v0.9
Agent Interface Completion
+
Release Preparation
    ↓
v1.0
First Stable Life HUD
```

---

# v0.9 —— Agent Interface Completion / 正式版前收尾

## 版本目标

v0.9 不再开发新的大业务域。

这一版负责：

> **把 Life HUD 从“主体功能完整”推到“可以作为稳定独立产品和外部 Tool 使用”。**

核心施工区：

```text
A. Agent Interface Completion
B. Content Expansion
C. Motion & Interaction Polish
D. Stability / Release Preparation
```

---

# A. Agent Interface Completion

## 1. Context API 保持只读聚合入口

现有：

```text
/api/agent/context/*
```

继续定位为：

> **Agent 专用语义读取层。**

不把它改造成 CRUD API。

## 2. 业务 API 是唯一事实写入口

Agent 需要写入 Life HUD 时：

```text
Agent
↓
Life HUD Business API
↓
Service
↓
Repository
↓
LifeEvent
```

禁止建立两套意义相同的写 API，例如：

```text
/api/life/sleep
/api/agent/actions/sleep
```

如果现有业务 API 已经适合 Agent 使用：

> 直接复用。

只有在语义、参数或安全边界确实无法满足时，才增加薄适配层。

## 3. Agent Contract

v0.9 需要正式整理：

```text
AGENT_ACTION_API.md
```

与现有：

```text
AGENT_CONTEXT_API.md
```

并列。

每个 Agent 可调用业务操作至少标明：

```text
用途
HTTP Method
Path
Request
Response
幂等语义
时间语义
错误语义
权限级别
是否可逆
```

建议权限分类：

```text
READ
读取事实

WRITE
新增 / 修改事实

DESTRUCTIVE
删除 / 不可逆修改
```

注意：

> 权限分类只是对外 Contract，不在 Life HUD 内实现 Zhaoxi Permission 系统。

真正“是否自动执行 / 是否询问用户”由外部 Agent 自己决定。

## 4. 优先补全 Agent 可操作业务

至少检查以下能力是否已经具有稳定、明确的 API。

### Life

```text
记录 / 修改 / 删除 Sleep
记录 / 修改 / 删除 Meal
记录 / 修改 / 删除 Exercise
记录 / 修改 / 删除 Check-in
记录 / 修改 / 删除 LifeRecord
```

### Journal

```text
写 Journal
编辑 Journal
删除 Journal
```

### Tasks

```text
读取任务
创建任务
修改任务
完成任务
删除任务
```

### Dreams

```text
创建 / 修改 Dream
Goal
Milestone
完成 / 暂停 / 恢复
```

### Ritual

```text
读取 Ritual
开始 Ritual
记录步骤
完成 / 取消
```

### Media

```text
创建 / 修改作品
记录 Anime Session
记录 Game Session
```

### Focus

```text
开始
暂停
恢复
切换 Segment
完成
中断
补录
```

### Growth

Growth 继续以系统派生为主。

Agent 默认不应自由修改：

```text
EXP
Level
Achievement
Growth Event
```

手动调整 Energy 如已有合法业务入口，可明确标记为高风险 / 管理操作。

## 5. API 一致性检查

统一检查：

```text
HTTP Method
Status Code
错误结构
空值语义
时间格式
ID 语义
分页
limit
日期范围
```

目标：

> Agent 不需要针对每个模块学习完全不同的错误习惯。

## 6. Agent-safe 原则

API 要允许外部 Agent：

```text
读取
调用
检查结果
```

但 Life HUD 不承担：

```text
LLM
Prompt
Planner
Memory
Permission Engine
Workflow
Tool Runtime
```

这些全部属于 Zhaoxi。

---

# B. Content Expansion

## 目标

不继续扩系统骨架，而是提高 Life HUD 的“生活感”和完成度。

## 1. Today 文案库

继续扩充：

```text
今日标题
空状态
状态提示
阶段文案
```

要求：

- 文案外置
- 不塞进 JS / Service
- 不每次刷新造成过度跳变
- 保持 Life HUD 自己的语言气质

## 2. Achievement 内容

增加一批真正来源于生活的 Achievement。

例如：

```text
Focus
Ritual
Dream
Sleep
Meal
Media
Journal
Timeline
综合生活
```

重点：

> 少而有意义。

不要把 Achievement 做成“每点一个按钮都弹成就”。

## 3. Title 内容

扩充 Title：

```text
来源清晰
无 Buff
可装备
有身份感
```

## 4. 默认内容

检查并完善：

```text
默认 Ritual
默认 LifeRecord Type
默认空状态
默认 Media 文案
默认 Growth 文案
```

不强行预置过多用户数据。

---

# C. Motion & Interaction Polish

## 原则

动画必须帮助表达：

```text
状态变化
进入
完成
解锁
反馈
```

而不是：

> 为了“高级感”让整个页面到处漂。

## P0 动效

优先：

```text
Dashboard 状态舱进入
页面切换
Modal / Drawer
Timeline 新事件
数值变化
```

## P1 动效

建议：

```text
Energy 改变
EXP / Level 变化
Achievement Unlock
Title Unlock / Equip
Ritual 开幕 / 落幕
Focus 铁幕
```

## Achievement Unlock

建议做成真正的 HUD 通知：

```text
ACHIEVEMENT UNLOCKED

铁幕行者
累计专注 50 小时
```

强调短暂、克制。

不要长时间挡住操作。

---

# D. Stability / Release Preparation

## 1. 全量回归

必须覆盖：

```text
Today
Focus
Tasks
Dreams
Ritual
Life
Media
Journal
Now
Growth
Settings
```

## 2. API 回归

重点：

```text
Agent Context
Business API
LifeEvent
Timeline
图片
Focus
Media
Growth
```

## 3. 数据兼容

要求：

```text
旧 data/ 可直接继续使用
缺失新字段有默认值
旧事件可读取
不要求清档
不静默丢数据
```

## 4. 数据安全

v1.0 前至少明确：

```text
data/ 目录位置
备份方式
恢复方式
迁移原则
删除行为
```

可提供简单：

```text
backup / restore 文档
```

第一版不必开发复杂云备份。

## 5. 错误体验

整理：

```text
404
400
文件损坏
旧后端 / 新前端版本不匹配
上传失败
非法时间
非法 ID
```

用户可见错误不能只剩：

```text
undefined
500
[object Object]
```

## 6. 前端完成度

检查：

```text
桌面宽屏
1366px
390px
长标题
空列表
大量数据
超长 Journal
大量 Timeline
无图片
多图片
```

## 7. 技术债

只清真正影响 v1.0 的债。

优先：

```text
明显重复代码
遗留死入口
旧 Shop / Coin 主流程残留
版本缓存混乱
命名冲突
接口文档漂移
测试脆弱点
```

不要为了“优雅”重写稳定模块。

---

# v0.9 完成标准

以下成立即可封版：

```text
1.
Life HUD 的主要业务 API
已经能够稳定被外部 Agent 使用。

2.
Agent Context + Agent Action Contract
文档完整、语义稳定。

3.
不需要 Zhaoxi 本体，
Life HUD 仍能独立完成全部核心生活记录与查看。

4.
Today / Growth / Ritual / Focus 等关键反馈
拥有适量动效与完成感。

5.
内容库、Achievement、Title、空状态
已经不像开发 Demo。

6.
全量测试、数据兼容、错误边界
达到正式版前要求。
```

---

# 5. v1.0 —— First Stable Life HUD

## 正式版定义

v1.0 不代表：

```text
所有想法都做完
所有外部服务都接入
所有统计都存在
所有动画都完美
```

v1.0 代表：

> **Life HUD 已经是一套可以长期真实使用、数据可信、业务完整、接口稳定的个人生活 HUD。**

## v1.0 必须具备

### 1. 独立完整

即使：

```text
Zhaoxi 不运行
```

Life HUD 仍然能够完整使用。

### 2. 事实可信

```text
业务记录
LifeEvent
Timeline
Growth
Dashboard
```

之间不存在明显幽灵、重复、漂移。

### 3. 核心业务完整

```text
Focus
Tasks
Dreams
Ritual
Life
Media
Journal
Now
Growth
Today
```

均达到可日常使用状态。

### 4. 对外接口稳定

具备：

```text
Agent Context API
Business API Contract
schemaVersion
错误语义
时间语义
权限级别说明
```

### 5. 数据可持续

```text
旧数据兼容
可备份
可恢复
升级不要求清档
```

### 6. 视觉完成

Summer Sky 不再只是主题皮肤，而成为：

> Life HUD 的完整视觉语言。

关键页面：

```text
Today
Focus
Ritual
Now
Growth
Media
```

具备各自明确的情绪与状态表达。

---

# 6. v1.0 不要求

以下明确不作为 v1.0 阻塞项：

```text
Zhaoxi 内置
LLM
RAG
AI 日评
AI 周报
主动建议
Calendar
Weather
GitHub
Steam
Bangumi
Last.fm
可穿戴设备
手机原生 App
云同步
多人系统
社交
复杂 BI
```

这些属于：

```text
外部 Tool
未来版本
或 Zhaoxi
```

---

# 7. Life HUD 与 Zhaoxi 的版本线

从现在开始：

```text
Life HUD 版本
≠
Zhaoxi 版本
≠
lifehud-tool 版本
```

例如完全允许：

```text
Life HUD v1.0
Zhaoxi Core v0.6
lifehud-tool v0.3
```

它们不需要同步升级。

---

# 8. Zhaoxi 对接原则

Zhaoxi 最终通过大型：

```text
lifehud-tool
```

使用 Life HUD。

Tool 内部负责：

```text
HTTP Client
Schema 校验
Context Tool
Business Tool
错误转换
Tool Descriptor
Permission Metadata
```

Zhaoxi Core 只负责：

```text
理解
规划
选择 Tool
Permission
执行
检查
继续推理
```

---

# 9. Life HUD 不拥有 Agent Runtime

禁止在 Life HUD 中新增：

```text
Prompt
LLM Client
Planner
Memory
Agent Router
Tool Calling
Conversation History
AI Persona
```

Life HUD 只提供：

```text
事实
业务
聚合
API
```

---

# 10. v1.0 后的方向

v1.0 后不急于继续堆版本号。

后续需求按真实使用出现。

可能方向：

```text
v1.1
Statistics / 趋势

v1.2
Import / Export

v1.x
外部数据源

未来
移动端
自动设备数据
更完整 Media
更丰富 Growth
```

但原则：

> **没有真实需求，不为了 roadmap 人造功能。**

---

# 11. 新版施工优先级

## P0：v1.0 前必须完成

```text
Agent Action Contract
主要业务 API 检查与补齐
API 一致性
错误语义
全量回归
数据兼容
README / API 文档
关键页面完成度
```

## P1：强烈建议完成

```text
Today 文案库
Achievement / Title 内容
关键动效
空状态
Backup / Restore 文档
移动端再验收
```

## P2：v1.0 可不做

```text
复杂动画
复杂统计
更多 LifeRecord Type
Book / Manga 深化
第三方同步
自动数据采集
```

---

# 12. 从旧计划删除的内容

旧版：

```text
# v0.9 —— 朝汐 Agent
```

整节从 Life HUD roadmap 删除。

这些能力：

```text
自然语言查询
今日总结
日评
周报
趋势分析
行为关联
梦想推进分析
主动建议
自然语言写入
```

全部归属：

> **Zhaoxi / lifehud-tool**

而不是 Life HUD。

---

# 13. 新的一句话版本路线

| 版本 | 定位 | 状态 |
|---|---|---|
| v0.2 | Life HUD 长出新骨架 | ✅ |
| v0.3 | Life HUD 学会专注 | ✅ |
| v0.4 | Life HUD 拥有成长循环 | ✅ |
| v0.5 | Life HUD 知道方向、仪式与「现在。」 | ✅ |
| v0.6 | Life HUD 开始记录生活本身 | ✅ |
| v0.7 | Life HUD 记住作品如何陪伴生活 | ✅ |
| v0.8 | Life HUD 把事实连接成驾驶舱 | ✅ |
| v0.8.1 | Today 真正 HUD 化 | ✅ |
| **v0.9** | **完善外部 Agent 接口并完成正式版收尾** | ▶ NEXT |
| **v1.0** | **First Stable Life HUD** | ◯ |

---

# 14. 最终架构判断

Life HUD v1.0 的完成标准不再是：

> “朝汐终于接进来了。”

而是：

> **任何时候关掉朝汐，Life HUD 依然是一套完整的个人生活系统。**

同时：

> **任何遵守 API Contract 的外部 Agent，都可以安全理解并操作 Life HUD。**

朝汐只是其中最重要、最熟悉、权限最高的一位调用者。

---

# 15. 最终愿景

```text
现实生活
   ↓
Life HUD
   ↓
留下可信事实
   ↓
组织成时间、成长、方向与状态
   ↓
通过稳定接口开放给外部世界
```

然后：

```text
Zhaoxi
↓
连接 Life HUD
连接 GitHub
连接 Calendar
连接 Files
连接 MemeVault
连接未来更多系统
```

最终形成：

> **Life HUD 记录生活。**

> **Zhaoxi 连接世界。**

---

# 16. 当前下一步

现在不再继续规划 Life HUD v0.10。

下一阶段只做：

```text
v0.9
Agent Interface Completion
+
Content / Motion
+
Release Preparation
```

完成后：

```text
Life HUD v1.0.0
```

正式封出第一版稳定产品。

> **车已经造完整了。**
>
> **接下来只是把接口、内饰、仪表与螺丝最后检查一遍，然后挂上 v1.0 的牌照。**
