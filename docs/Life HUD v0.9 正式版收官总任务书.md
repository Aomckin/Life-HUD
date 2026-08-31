# Life HUD v0.9 总开发任务书
## Finalization / Agent Interface Completion / v1.0 正式版收官

> 开发基线：`v0.8.1`
> 开发目标：**今天完成 Life HUD 正式版收官。**
> 版本策略：以 `v0.9` 分支完成全部 Release Preparation，验收通过后直接升级并封版为 **`v1.0.0`**。
> 性质：接口完善 + 内容补全 + 交互动效 + 稳定性收尾 + 正式版发布。
> 核心原则：**不再开发新的大业务域，不把朝汐塞进 Life HUD，不为了“1.0”重写已经稳定的架构。**

---

# 0. 当前基线

当前 `v0.8.1` 已经完成：

```text
Today / Dashboard HUD
Focus
Tasks
Dreams
Ritual
Life
Media
Journal
「现在。」
Growth

LifeEvent
Timeline
AchievementEvaluator
Agent Context API
```

当前稳定事实读取接口：

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

统一：

```text
schemaVersion = "1"
```

当前自动化测试基线：

```text
172 项通过
```

v0.9 必须以此为基线，禁止出现功能倒退。

---

# 1. v0.9 的真正目标

这一版不再回答：

> “Life HUD 还能加什么？”

而是回答：

> **“现在这套 Life HUD，是否已经值得挂上 v1.0.0？”**

v0.9 最终需要同时解决四件事：

```text
A. Agent Interface Completion
B. Content Completion
C. Motion & Interaction Polish
D. Stability / Release Engineering
```

完成后不再规划：

```text
Life HUD v0.10
```

而是直接：

```text
Life HUD v1.0.0
```

---

# 2. 正式版架构边界

Life HUD v1.0 必须坚持：

```text
Life HUD
=
事实
业务
规则
持久化
聚合
Growth
Timeline
API
```

朝汐：

```text
Zhaoxi
=
理解
Planner
Memory
Permission
Workflow
Tool Runtime
自然语言交互
```

最终调用关系：

```text
Zhaoxi
  ↓
lifehud-tool
  ↓ REST
Life HUD
```

Life HUD **不知道调用者一定是 Zhaoxi**。

任何遵守 API Contract 的外部 Agent 都应该能够使用 Life HUD。

---

# 3. 绝对禁止事项

v0.9 不做：

```text
LLM
Prompt
RAG
MCP Server
Agent Router
Planner
Memory
Conversation
AI 总结
AI 日评
AI 周报
主动建议
朝汐 Persona
GitHub Tool
Calendar Tool
Weather Tool
Steam / Bangumi / Last.fm
云同步
原生移动 App
复杂 BI
新大业务域
```

不要因为“正式版”而无限扩需求。

---

# 4. Phase A：Agent Interface Completion

## 4.1 目标

当前 v0.8 已经解决：

```text
Agent 怎么读 Life HUD
```

v0.9 需要解决：

```text
外部 Agent 怎么稳定、安全、明确地操作 Life HUD
```

注意：

> **不要另造一套重复的 Agent CRUD API。**

业务 API 仍然是唯一事实写入口。

---

# 5. Agent Context API 保持不变

现有：

```text
/api/agent/context/*
```

定位保持：

> 专门给 Agent 使用的语义聚合读取层。

v0.9 不应该把：

```text
POST
PUT
PATCH
DELETE
```

塞进 Context Controller。

Context API 仍然只读。

---

# 6. Business API 作为 Agent Action API

外部 Agent 写入时应直接调用现有业务 API：

```text
Agent
↓
Business API
↓
Service
↓
Repository
↓
LifeEvent
```

禁止形成：

```text
/api/life/sleep
/api/agent/actions/sleep
```

两套重复入口。

只有当某个现有 API：

```text
语义模糊
无法表达 Agent 操作
没有稳定请求 DTO
没有稳定错误
```

时才允许做最小兼容调整。

---

# 7. 新增 Agent Action Contract 文档

必须新增：

```text
docs/AGENT_ACTION_API.md
```

它与：

```text
docs/AGENT_CONTEXT_API.md
```

共同形成 Life HUD v1.0 的 Agent Contract。

---

## 7.1 每个操作必须说明

```text
功能
Method
Path
Request
Response
权限等级
是否可逆
幂等性
发生时间语义
失败语义
相关 LifeEvent
```

权限等级使用：

```text
READ
WRITE
DESTRUCTIVE
ADMIN
```

其中：

```text
READ
查询

WRITE
创建 / 更新

DESTRUCTIVE
删除 / 不可逆业务操作

ADMIN
成长数值人工修正等不应由普通 Agent 自动执行的操作
```

---

# 8. Agent Action API 业务检查

必须逐域检查现有 API。

不要只写文档。

如果发现缺 API、参数无法稳定调用、返回结构极度不一致，要在本版本补齐。

---

## 8.1 Life

必须确认 Agent 可以稳定：

```text
Sleep
├── create
├── update
└── delete

Meal
├── create
├── update
└── delete

Exercise
├── create
├── update
└── delete

Check-in
├── create
├── update
└── delete

LifeRecord
├── create
├── update
└── delete
```

要求：

- 时间可补录
- null / omitted 字段语义明确
- 图片字段语义明确
- ID 不存在返回正确错误
- 删除同步清理 LifeEvent
- 编辑原位更新 LifeEvent

---

## 8.2 Journal

必须确认：

```text
create
update
delete
```

Agent 可以传：

```text
content
occurredAt
tags
images
```

并保持：

```text
JOURNAL_WRITTEN
```

时间线事实正确。

---

## 8.3 Tasks

Agent 应能够：

```text
读取当前任务
创建 Daily Task
创建 Special Task
修改
启用 / 禁用
完成
删除
管理 Direction Link
```

重点检查：

```text
完成操作幂等
```

不能重复调用后重复：

```text
TASK_COMPLETED
Energy
Achievement
```

---

## 8.4 Dreams

必须确认：

```text
Dream
Goal
DreamMilestone
```

具备稳定：

```text
create
update
complete
pause / resume
delete / archive
```

Agent Contract 需要明确：

```text
archive
delete
purge
```

三种不同语义。

如果当前存在高风险永久删除：

> 标记 DESTRUCTIVE。

---

## 8.5 Ritual

外部 Agent 可以：

```text
读取 Ritual
start
读取 execution
提交 step
complete
cancel
```

必须明确：

```text
执行中的 Ritual
≠
修改 Ritual 定义
```

修改 / 删除 Ritual 定义归 WRITE / DESTRUCTIVE。

---

## 8.6 Media

至少明确：

```text
Anime create/update/delete
AnimeWatchSession create/update/delete

MediaGame create/update/delete
MediaGameSession create/update/delete

MediaItem create/update/delete
```

要求保持：

```text
Session
↔
LifeEvent
```

唯一同步。

---

## 8.7 Focus

必须检查 Agent 可调用：

```text
GET current
GET today
GET history

POST start
POST manual
POST pause
POST resume
POST complete
POST interrupt

segment switch
segment update
```

风险说明：

```text
start / pause / resume
属于 WRITE

complete / interrupt
属于 WRITE

历史删除
属于 DESTRUCTIVE
```

不能让 API 文档把：

```text
实时运行状态操作
```

和：

```text
历史事实删除
```

混成同一级别。

---

## 8.8 Growth

默认 Agent 应读取：

```text
Energy
EXP
Level
Achievement
Title
Snapshot
```

但禁止普通 Agent 直接：

```text
set EXP
set Level
unlock Achievement
伪造 GrowthEvent
```

如果系统存在人工 Energy 调整：

```text
标记 ADMIN
```

Growth Engine 的派生事实不能被 Tool 随便伪造。

---

# 9. API 一致性收敛

v1.0 前统一检查：

```text
HTTP Method
HTTP Status
Content-Type
错误 JSON
空值
时间格式
日期格式
ID
分页
limit
枚举非法值
```

---

## 9.1 错误结构

尽量统一外部可见错误：

```json
{
  "status": 400,
  "detail": "..."
}
```

或保持当前 Spring 错误结构，但必须：

- 有 `status`
- 有可读 `detail` / `message`
- 不返回 Java stack trace
- 不返回 HTML 错误页给 API Client

---

## 9.2 错误码原则

建议：

```text
400
请求字段非法

404
业务对象不存在

409
状态冲突 / 当前操作不允许

413
文件过大

500
真正服务器错误
```

不要：

```text
所有错误都 500
```

---

# 10. API 文档总入口

建议新增：

```text
docs/API_INDEX.md
```

至少链接：

```text
AGENT_CONTEXT_API.md
AGENT_ACTION_API.md
```

并解释：

```text
Context = Agent 语义读
Business API = Agent 与人类共用写入口
```

---

# 11. Phase B：Content Completion

## 11.1 原则

正式版不是把代码写满。

而是让 Life HUD 不再到处留下：

```text
开发占位
机械空状态
undefined
Demo 文案
```

---

# 12. Today 文案库

继续扩展当前随机标题文案库。

目标数量建议：

```text
至少 20 条
```

内容保持：

```text
短
有力
生活化
不鸡汤
不 AI 味
```

示例风格：

```text
真挚！与热诚！
今天从这里开始。
正在活着。
向着想去的地方。
别浪费这个晴天。
```

具体文案由当前已有文案风格延展。

---

## 12.1 随机策略检查

如果当前每次打开 Dashboard 随机：

允许保持。

但必须保证：

```text
不会因为组件局部刷新疯狂换标题
```

不要求额外做复杂日 seed。

---

# 13. Empty State 全局补完

逐页检查：

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

禁止空状态只显示：

```text
暂无数据
No Data
null
undefined
```

应根据业务语义写轻量提示。

例如：

```text
Media
最近还没有新的作品足迹。

Dream
这里还没有一个明确的方向。

Journal
今天还没有留下文字。
```

不要过度诗化，不要阻碍使用。

---

# 14. Achievement 内容补完

检查 v0.8 已有跨域规则。

要求：

- 不重复
- 不互相冲突
- target 合理
- 文案外置
- 有清晰 description
- 数量不追求无限

建议覆盖：

```text
Focus
Task
Dream
Ritual
Sleep
Meal
Exercise
Journal
Anime
Game
LifeEvent 综合
```

---

# 15. Title 内容补完

Title 继续：

```text
身份标签
无 Buff
手动装备
```

要求：

```text
名字有辨识度
来源明确
不大量廉价解锁
```

不要因为 v1.0 一口气塞几十个无意义称号。

---

# 16. 默认 Ritual / 默认内容检查

检查首次启动时：

```text
Ritual
LifeRecord Type
Growth 文案
Media 状态
Now 空内容
```

用户能理解怎么玩。

但：

> 不要替用户自动创建大量私人生活数据。

---

# 17. Phase C：Motion & Interaction Polish

## 17.1 核心原则

动画只用于表达：

```text
进入
状态变化
完成
解锁
反馈
```

不是为了炫技。

要求：

```text
轻
快
不阻塞
可重复
不会因为动画拖慢操作
```

---

# 18. P0 动效

必须优先完善：

```text
页面切换
Dashboard 状态舱进入
Modal / Drawer
Toast
Timeline 新事件
主要按钮反馈
```

---

## 18.1 页面切换

要求：

- 不整页白闪
- 不过长
- 不影响浏览器返回
- 不重复触发大量动画

建议：

```text
opacity + translateY
150 ~ 250ms
```

具体以现有 CSS 体系为准。

---

## 18.2 Dashboard

建议：

```text
状态舱轻量出现
今日脉搏稍后跟进
方向陪伴与 Footprints 分层出现
```

不要每次数据刷新从头播一遍大动画。

---

# 19. P1 语义动效

尽量完成：

```text
Energy 改变
EXP / Level 变化
Achievement Unlock
Title Unlock
Title Equip
Ritual 开场 / 落幕
Focus 铁幕
```

---

## 19.1 Achievement HUD Notification

新增或完善：

```text
ACHIEVEMENT UNLOCKED

[成就名称]
[简短描述]
```

要求：

```text
自动消失
不过度遮挡
刷新不重复播放旧成就
```

如果无法可靠区分“新解锁”：

> 不要为了动画制造错误逻辑。

可以只在明确的当前业务响应返回 unlockedAchievements 时触发。

---

# 20. Reduced Motion

正式版建议支持：

```css
@media (prefers-reduced-motion: reduce)
```

至少：

- 关闭大幅位移
- 缩短动画
- 不影响功能

这是低成本正式版细节。

---

# 21. Phase D：Stability / Release Engineering

这是本版本最重要的收官部分。

不要只顾视觉。

---

# 22. 全量自动化测试

先运行：

```powershell
.\mvnw.cmd test
```

基线：

```text
172 项通过
```

v0.9 必须：

```text
现有测试全部通过
新增测试全部通过
0 failure
0 error
```

不要求测试数必须达到某个漂亮整数。

---

# 23. Agent Contract 测试

建议新增专门测试覆盖：

```text
Agent Context schemaVersion
today
recent
status
各分域 context

Business API 常用 Agent 写入
错误响应
状态冲突
ID 不存在
删除
```

至少验证几个真实 Tool 闭环：

---

## 23.1 Sleep

```text
POST Sleep
↓
GET context/today
↓
能看到 Sleep
↓
Timeline 能看到
↓
DELETE
↓
Context / Timeline 消失
```

---

## 23.2 Check-in

```text
POST Check-in
↓
GET context/status
↓
latest checkIn 正确
```

---

## 23.3 Task

```text
创建 Task
↓
GET context/tasks
↓
完成 Task
↓
重复完成
↓
不重复产生事实 / Growth
```

---

## 23.4 Media

```text
创建 Anime
↓
记录 Session
↓
GET context/media
↓
Timeline 出现
```

---

# 24. 真实全链路验收

v1.0 发布前使用独立测试 data dir。

不要污染用户真实数据。

使用：

```text
LIFEHUD_DATA_DIR=<temp>
```

或当前项目支持的等价配置。

完整录入：

```text
Sleep
Meal + Image
Check-in
Exercise
LifeRecord
Focus
Task
Dream
Ritual
Anime Session
Game Session
Journal
Now
```

验证：

```text
Today
Journal
Growth
Agent Context
重启
```

全部仍正确。

---

# 25. 重启持久化测试

必须至少做一次：

```text
启动
↓
写数据
↓
关闭
↓
重新启动
↓
读取
```

检查：

```text
Focus
Life
Dream
Ritual
Media
Now
Growth
```

无静默丢失。

---

# 26. 数据兼容

必须使用现有数据样本或测试 fixture 验证：

```text
v0.5
v0.6
v0.7
v0.8
```

旧字段缺失时：

```text
有默认值
可读
不 crash
```

禁止要求用户：

```text
删除 data/
重新初始化
```

---

# 27. Backup / Restore

正式版至少提供文档级能力。

新增：

```text
docs/BACKUP_AND_RESTORE.md
```

说明：

```text
默认 data/ 位置
自定义 LIFEHUD_DATA_DIR
需要备份哪些文件
uploads 是否包含在备份里
恢复步骤
版本升级建议
```

不要求做云备份 UI。

---

## 27.1 可选：简单本地备份脚本

如果非常低成本，可以新增：

```text
backup-lifehud.ps1
```

功能：

```text
停止写入后
复制 data/
生成带时间戳目录 / zip
```

但这是 P1。

不要因为脚本拖住 v1.0。

---

# 28. 文件损坏错误

检查 JSON 读取失败。

至少做到：

```text
不要静默覆盖损坏文件
不要直接初始化成空数组把旧数据冲掉
错误信息说明哪个文件损坏
```

如果现有 JsonFileStore 已经足够安全：

> 保留，不重写。

---

# 29. 图片安全与异常

检查：

```text
无 MIME
非法扩展名
文件过大
文件不存在
旧图片路径
```

页面：

```text
图片缺失不应崩
```

显示占位即可。

---

# 30. 前端异常收口

最终检查所有页面：

```text
undefined
null
[object Object]
NaN
Invalid Date
```

任何一个用户可见，都应修。

---

# 31. 浏览器 Console 验收

至少：

```text
Dashboard
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

Console：

```text
0 uncaught error
```

Warning 若存在，必须明确不是业务错误。

---

# 32. 响应式验收

至少检查：

```text
390px
1366px
1920px+
```

重点：

```text
Dashboard
Ritual
Media
Journal
Now
Focus
```

不能：

```text
横向溢出
按钮跑出屏幕
Modal 无法保存
文字覆盖
底部操作无法点击
```

---

# 33. 极端内容测试

必须至少人工检查：

```text
超长 Dream 名
超长 Task 名
超长 Journal
20+ Timeline Item
无封面 Media
超长 Media 标题
大量图片
空列表
全部列表都有数据
```

不要只用 Demo 的短字符串验收。

---

# 34. 删除行为检查

所有删除按钮必须：

```text
明确确认
```

优先统一使用现有：

```text
confirmDialog
```

禁止重新出现：

```text
window.confirm
```

除非当前项目明确仍保留且稳定。

删除后：

```text
UI
Repository
LifeEvent
Timeline
Context
```

按各业务既定语义同步。

---

# 35. 技术债检查

仅处理 v1.0 阻塞债。

搜索并处理：

```text
TODO
FIXME
deprecated UI
placeholder route
v0.4.0 硬编码旧文案
旧 Shop / Coin 主入口
未使用 controller
重复 endpoint
死 JS
缓存版本号不一致
```

注意：

> 不要为了清技术债大规模架构重构。

稳定第一。

---

# 36. 静态资源缓存

正式版前统一：

```text
?v=1.0.0
```

或项目当前统一缓存版本机制。

禁止出现：

```text
部分 0.6.1
部分 0.7
部分 0.8.1
```

造成旧浏览器继续加载旧 JS。

---

# 37. Release Metadata

正式版必须统一：

```text
pom.xml / app info / UI
→ 1.0.0
```

页面用户可见版本：

```text
Life HUD v1.0.0
```

README：

```text
Life HUD v1.0.0
First Stable Release
```

---

# 38. README 正式版重写 / 收口

README 第一屏应该能回答：

```text
Life HUD 是什么？
能做什么？
怎么运行？
数据存哪里？
如何接 Agent？
```

不要让 README 第一屏继续像开发流水账。

历史版本说明可以下沉。

建议正式版 README 开头：

```text
# Life HUD

A personal life state and activity HUD built with Java 21 / Spring Boot.

Focus · Life · Direction · Ritual · Media · Timeline · Growth
```

中文说明可作为主体或并列。

---

# 39. 文档清单

v1.0 至少保证：

```text
README.md
docs/CODEBASE_STATUS.md
docs/AGENT_CONTEXT_API.md
docs/AGENT_ACTION_API.md
docs/API_INDEX.md
docs/BACKUP_AND_RESTORE.md
```

旧专项文档保留即可。

---

# 40. CODEBASE_STATUS v1.0

更新时不要只写“做了什么”。

必须明确：

```text
当前正式版边界
核心数据流
主要 API
测试数
数据目录
已知非目标
未来开发原则
```

并明确：

```text
Life HUD 不包含 Agent Runtime
```

---

# 41. v1.0 Release Notes

建议新增：

```text
docs/RELEASE_V1.0.md
```

至少包含：

```text
从宅宅能量条到 Life HUD
v1.0 核心能力
数据兼容
Agent API
运行方式
已知限制
```

这会成为正式版很好的里程碑记录。

---

# 42. 版本升级流程

不要在开发刚开始就立即把所有版本号写成 1.0。

顺序：

```text
v0.9 分支
↓
实现
↓
测试
↓
真实验收
↓
Release Candidate 确认
↓
版本号统一升级 1.0.0
↓
最终测试
↓
Commit
↓
Push
↓
Tag / Release（若当前工作流有）
```

---

# 43. Release Candidate Gate

只有以下全通过才允许升级 `1.0.0`：

```text
[ ] Maven 全量测试 0 failure
[ ] git diff --check
[ ] Agent Context 契约测试
[ ] Business API Agent Action 检查
[ ] 真实生活全链路测试
[ ] 重启持久化测试
[ ] 旧数据兼容测试
[ ] 桌面端验收
[ ] 390px 验收
[ ] Console 0 uncaught error
[ ] 无用户可见 undefined/null/NaN
[ ] 文档齐全
[ ] Backup / Restore 有说明
```

任何一项严重失败：

> 暂时保持 v0.9，不挂 v1.0。

---

# 44. Git 检查

最终：

```bash
git status
git diff --check
```

确保：

```text
没有临时测试数据
没有 IDE 文件
没有真实用户 data/
没有备份 zip
没有临时截图
没有未解释 debug log
```

---

# 45. 正式版数据禁止提交

必须检查 `.gitignore`。

严禁提交：

```text
用户 data/
上传照片
歌曲
封面
私人 Journal
备份
```

仓库只保留：

```text
默认模板 / 默认 content
```

---

# 46. v1.0 视觉完成标准

不是要求所有页面重新设计。

而是：

```text
Summer Sky 视觉语言一致
关键页面有自己的语义
无明显开发态 UI
无大面积密集后台表格
```

重点看：

```text
Today
Focus
Ritual
Now
Growth
Media
```

---

# 47. 不要为了 v1.0 再改这些

如果已经稳定：

```text
LifeEvent 模型
Growth 核心数值
Focus 状态机
Media 数据模型
Dream 三层结构
JSON 持久化
Summer Sky Shell
```

不要重写。

v1.0 的目标是：

> **稳定封版，而不是架构洁癖。**

---

# 48. v1.0 对外 Agent 最终契约

最终关系必须清楚：

```text
READ
Agent Context API
/api/agent/context/*

WRITE
现有 Business API

RULES
AGENT_ACTION_API.md
```

Zhaoxi 的 `lifehud-tool` 未来自行负责：

```text
HTTP Client
Tool Schema
Permission
参数转换
错误翻译
调用重试
```

这些不进入 Life HUD。

---

# 49. 最终真实验收剧本

建议使用全新临时 data dir，完整走一次：

```text
1. 打开 Dashboard
2. Check-in
3. 补录昨夜 Sleep
4. 记录 Meal + 图片
5. 记录 Exercise
6. 开始 Focus
7. 切换一次 Segment
8. 完成 Focus
9. 创建 Task
10. 关联 Dream
11. 完成 Task
12. 开始 Ritual
13. 完成 Ritual
14. 创建 Anime
15. 记录 Anime Session
16. 创建 Game
17. 记录 Game Session
18. 写 Journal
19. 修改「现在。」
20. 查看 Journal Timeline
21. 查看 Growth
22. GET /api/agent/context/today
23. GET /api/agent/context/recent?days=7
24. 关闭服务
25. 重启
26. 再次检查 Dashboard / Journal / Growth / Context
```

这套链完整通过：

> Life HUD v1.0 的主体就成立。

---

# 50. 验收时的数据一致性

重点检查：

```text
一个业务事实
→ 一个正确 LifeEvent

编辑
→ 原位更新

删除
→ 按既定语义清理

Growth
→ 不重复结算

Dashboard
→ 聚合当前事实

Agent Context
→ 与 Dashboard / Repository 一致
```

不能出现：

```text
业务显示完成
Journal 没有

Journal 有
业务已经删了

Context 和页面数字不同

重启后数据回退

同一操作刷出两个 Achievement
```

---

# 51. 性能底线

本地个人系统无需过度优化。

但要求：

```text
Dashboard 正常打开
Journal 正常滚动
Media 正常加载
Agent Context 不出现明显秒级卡顿
```

不要在 v1.0 引入：

```text
Redis
数据库迁移
复杂缓存
消息队列
```

来解决不存在的问题。

---

# 52. 安全边界

当前本地版本 Agent API 无鉴权。

因此文档必须明确：

```text
Life HUD 默认只适合本机 / 可信局域环境
不要直接暴露公网
```

v1.0 不要求实现 OAuth / JWT。

未来有真实远程访问需求再做。

---

# 53. 正式版已知限制

在 README / RELEASE 中主动写明：

```text
本地单用户
JSON 持久化
手动 Life 数据输入为主
无云同步
无内置 Agent
无第三方媒体同步
Agent API 默认无鉴权
```

这不是缺陷。

这是 v1.0 的产品边界。

---

# 54. Codex 执行要求

本任务允许 Codex：

```text
一次完整执行
```

不要每做一个小点停下来询问。

但必须：

1. 先阅读：
   - `README.md`
   - `docs/CODEBASE_STATUS.md`
   - `docs/AGENT_CONTEXT_API.md`
2. 运行当前测试获取真实基线。
3. 再开始修改。
4. 优先复用现有 Service / Component / CSS。
5. 不修改用户真实 data。
6. 不为了统一风格大规模重构稳定模块。
7. 发现与本任务无关的轻微问题可记录，不要无限扩需求。
8. 遇到真正阻塞 v1.0 的 Bug 可直接修。

---

# 55. Codex 最终交付说明

完成后必须输出：

```text
1. 修改概要
2. Agent Interface 完成情况
3. 新增 / 修改 API
4. 新增文档
5. 内容与动效调整
6. Bug / 技术债修复
7. 测试结果
8. 手工验收结果
9. 数据兼容结果
10. 是否满足 v1.0 Release Gate
11. 最终版本号
12. 未完成 / 已知限制
```

不要只说：

```text
“已完成。”
```

---

# 56. v0.9 完成定义

v0.9 开发阶段完成的判断：

```text
Life HUD 已经拥有稳定对外 Agent 读取契约
+
稳定 Business Action 契约
+
正式版级内容 / 交互
+
完整测试与数据安全边界
```

此时进入：

```text
Release Candidate
```

---

# 57. v1.0.0 最终完成定义

只有以下全部成立，才真正封 `v1.0.0`：

## 产品

```text
[ ] 不运行 Zhaoxi 也可完整使用
[ ] Today 能完整描述今天
[ ] Focus / Task / Dream / Ritual 正常
[ ] Life / Journal 正常
[ ] Media 正常
[ ] Now 正常
[ ] Growth 正常
```

## 事实

```text
[ ] LifeEvent 无明显重复
[ ] 编辑同步
[ ] 删除同步
[ ] Timeline 正确
[ ] Growth 幂等
```

## Agent

```text
[ ] Agent Context schemaVersion=1
[ ] Agent Context 文档稳定
[ ] Agent Action Contract 完整
[ ] 主要 Business API 可被外部 Tool 调用
```

## 数据

```text
[ ] 旧数据兼容
[ ] 重启持久化
[ ] 有备份恢复说明
[ ] 不要求清档升级
```

## 工程

```text
[ ] Maven 全绿
[ ] git diff --check
[ ] 无临时用户数据
[ ] 版本缓存统一
[ ] README / STATUS / API 文档完整
```

## UI

```text
[ ] 桌面宽屏正常
[ ] 1366px 正常
[ ] 390px 正常
[ ] Console 无 uncaught error
[ ] 无 undefined / NaN / Invalid Date
[ ] 关键动效克制可用
```

---

# 58. 正式版一句话

Life HUD v1.0 应当最终成为：

> **一套独立、可信、可以长期真实使用，并通过稳定接口向外部 Agent 开放的个人生活 HUD。**

它记录：

```text
如何行动
如何专注
如何休息
如何生活
如何娱乐
如何成长
正在追逐什么
此刻是什么样
```

它不负责成为 AI。

---

# 59. 最终架构

```text
                   Zhaoxi
             General-purpose Agent
                    │
             lifehud-tool
                    │
                    ▼
        ┌─────────────────────┐
        │     Life HUD v1.0   │
        │                     │
        │ Today       Focus   │
        │ Tasks       Dreams  │
        │ Ritual      Life    │
        │ Media       Journal │
        │ Now         Growth  │
        │                     │
        │      LifeEvent      │
        │      Timeline       │
        │                     │
        │ Agent Context API   │
        │ Business API        │
        └─────────────────────┘
```

---

# 60. 今天的终点

今天不要停在：

```text
“v0.9 差不多了。”
```

目标是：

```text
v0.8.1
↓
v0.9 Release Preparation
↓
RC Gate 全通过
↓
v1.0.0
↓
Push
↓
Life HUD First Stable Release
```

到这里再停。

> **从“宅宅能量条”一路长出来的车，今天挂上第一块正式牌照。**
