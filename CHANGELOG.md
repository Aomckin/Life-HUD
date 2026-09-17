# Changelog

本文件记录 Life HUD 的主要版本演进。完整设计背景、任务拆分与验收细节保留在 [`docs/`](docs/)；项目主页只展示当前稳定版本。

## v1.0.0 · First Stable Release

- 将 Life HUD 收敛为可长期本地使用的个人生活事实系统。
- 完成 Agent Context 与 Agent Action 契约、统一错误响应和正式 API 索引。
- 完成内容、交互、数据恢复、兼容性与 Release Gate 验收。
- 明确本地单用户、无内置 Agent、无公网鉴权的产品边界。

详见 [v1.0 Release Note](docs/RELEASE_V1.0.md)。

## v0.9 · Agent Interface Completion

- 补全外部 Agent 可读取的语义上下文与可执行的业务操作说明。
- 写入继续复用 Business API，不新增平行的 Agent Action 实现。
- 统一错误、时间、幂等、可逆性与高影响操作语义。

## v0.8.1 · Dashboard HUD 化

- 将状态、Check-in、Focus、Growth、今日脉搏和方向陪伴收拢为 Today 驾驶舱。
- 引入 Today 文案库与 Dashboard 陪伴位选择，降低面板对壁纸的遮挡。

## v0.8.0 · Integration

- 新增十个强类型 Agent Context 视图，统一 `schemaVersion` 与日期边界。
- Dashboard 改为单次聚合请求；Growth 增加跨域成就判定。

## v0.7.0 · Media

- 增加 Anime、Game、Book、Manga、Movie 与 Other 作品档案。
- Anime / Game Session 与唯一 LifeEvent 同步，进入 Journal 的 Media 时间线。

## v0.6.2 · Ritual 状态入口

- 将 Ritual 重构为轻量定义页与沉浸执行态。
- 仪式片段支持排序、折叠高级字段、完成与取消执行。

## v0.6.1 · 「现在。」时间线

- 增加稳定的 `NOW` 来源类型与具体对象元数据。
- Now 与 Life 图片进入统一时间线；Journal 的日期与日内记录统一倒序。

## v0.6.0 · Life / Journal

- 加入 Sleep、Meal、Exercise、Check-in、LifeRecord 与 Journal。
- 以 `LifeFactRecorder` 维护业务记录和 LifeEvent 的一对一关系。
- 新增统一 Timeline、来源筛选、图片缩略图与 lightbox。

## v0.5.x · Direction / Ritual / Now

- 建立 Dream → Goal → DreamMilestone 方向结构及 Task 关联。
- 加入 Ritual 定义与执行记录。
- 将「现在。」建设为阶段陈列页，支持歌单、图片、方向与历史快照。

## v0.4.x · Growth

- 建立 LifeEvent → GrowthEngine 的事件驱动成长链。
- Focus / Task 产出 Energy；娱乐消费按实际 Energy 消耗沉淀 EXP。
- 加入幂等回执、Energy Ledger、Level、Achievement、Title 与趋势快照。

## v0.3.x · Focus

- 建立统一 `FocusSession` 状态机与 `FocusSegment` 模型。
- 完成铁幕、番茄、自由专注、暂停恢复、跨日归属与手动补录。

## v0.2.x · Shell / LifeEvent

- 建立 Java / Spring Boot Web Shell、Summer Sky 视觉骨架与 LifeEvent 地基。

更早的迁移背景见 [Migration Matrix](docs/MIGRATION_MATRIX.md)。
