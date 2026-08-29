# Life HUD 代码现状速览

> **当前分支 v0.5「Direction」（版本 0.5.3，行动台重构）**：`/tasks` 重构为卡片式行动台（Header 今日摘要 + Daily 紧凑卡 + Special 舒展卡 + 已完成折叠 + Direction 意义 chip + 编辑 Modal），完成走 `POST /api/task-directions/{source}/{taskId}/complete`（TaskService.completeDailyById/completeSpecialById，幂等）。`NowSong` 扩展 playCount / note / posX / posY / rotationDeg / zIndex；NowState 增加 playlistBackgroundImage / playlistTitle / playlistSubtitle；`PUT /api/now/songs/{slot}` 编辑、`POST|DELETE /api/now/background` 背景管理；前端 `now-playlist.js` PlaylistBoard 组件（锚带自动布局、playCount→尺寸分级、纸条备注、编辑 Modal、快照只读复用）。v0.5.1 重做 `/now` 为阶段陈列页：`NowSong`（10 个固定槽位）+ `AudioStorageService`（jaudiotagger 读取 MP3/FLAC 标签与内嵌封面，缺标签回退文件名/未知艺术家）；图片上传 MIME 缺失时按扩展名兜底，multipart 上限 64MB；NowSnapshot 冻结整条歌曲记录。Dream → Goal → DreamMilestone（`/api/dreams` 系）+ Task 可选方向关联（`/api/task-directions`）+ Ritual/RitualStep/RitualExecution（`/api/rituals` 系）+ NowState/NowSnapshot（`/api/now` 系）+ 统一图片上传（`/api/images`，存 `data/uploads/`）。新事件：DREAM_CREATED / DREAM_COMPLETED / GOAL_CREATED / GOAL_COMPLETED / DREAM_MILESTONE_COMPLETED / NOW_SNAPSHOT_CREATED；完成类操作幂等，编辑不产生事件。DreamService/DirectionLinkService 有循环依赖，用 ObjectProvider 打破。v0.4 Growth 结论见下。

> **v0.4 已封版（2026-08-29，最终版本号 v0.4.2）**。最终核心循环：Focus / Task → Energy EARN（每 3 有效分钟 +1，单事件上限 80）；娱乐记录（`EntertainmentRecordService`，`/api/entertainment`）→ 统一 Energy Ledger SPEND → 每 10 点实际消耗 +1 EXP（整数余数池 `exp_conversion_remainder`）→ Level。新的一天 Energy 向基准值回归（`midpoint=90`、`day_start_factor=0.75`，内容文件 `data/content/energy-drift.json`，每日最多一次、ADJUST 记账、不产 EXP）。Energy 不足时照常记录现实、按实际剩余结算；娱乐记录的 energyCost 手动填写、不可修改，删除不回滚账目。Achievement / Milestone / Title 只记录不参与数值；Shop / Coin / 旧行动命令全部停用。参数在 `data/growth.json`，内容在 `data/content/` + `static/content/copy.js`。Snapshot 的娱乐字段扩展留给 v0.5。详见 [GROWTH_V0.4.md](GROWTH_V0.4.md)。

> 下文保留 v0.3.0 的重构背景，供调用链追溯。

## 当前能力

- Java 21 / Spring Boot 3.5.5 后端提供单页 Life HUD；浏览器通过 `GET /state` 读取状态、`POST /command` 执行命令、`GET /actions/{actionName}/duration-options` 获取受控时长选项。
- 现有业务保持 Python 迁移版本的规则：行动、计时行动、能量/经验/等级、每日任务、特殊任务、成就、称号、商店和日志。
- 默认数据首次启动后复制到工作目录 `data/`；支持 `lifehud.data-dir`、`LIFEHUD_DATA_DIR`，并兼容旧变量 `OTAKU_ENERGY_DATA_DIR`。
- `PlayerRepository` 负责 `save.json` 兼容读取和归一化；旧存档缺失的可选字段会补充默认值。
- `JsonFileStore` 是无业务含义的文件基础设施；玩家、日志等通过专用 repository 访问持久化文件。
- v0.1.1 已完成成就子域的强类型收敛：`AchievementRepository` 将 `achievements.json` 映射为 `AchievementDefinition`，规则使用 `AchievementConditionType`、`TaskRequirement`、`TaskSource`，不再在成就判定中使用 unchecked cast。
- `/state` 与命令事件的既有 JSON 结构由 `GameViewAssembler` 在响应边界组装；强类型领域对象不会直接改变前端契约。
- v0.3.0 已建立统一 Focus 领域：`IRON_CURTAIN`、`POMODORO` 与 `FREE` 共用 `FocusSession`，支持运行、暂停、恢复、完成与中断。
- Focus 使用 `focus-sessions.json` 独立持久化；实际时长由后端累计有效秒数，暂停时间不计入，浏览器刷新后可恢复当前状态。
- `/focus` 已提供开始/运行/结算工作台、番茄预设与自定义时长、到点继续、简单休息、今日摘要和基础历史；Dashboard 会显示当前 Focus 或今日累计。
- Today Summary 按 Session 开始日归属；跨日 Session 保持完整，不按午夜拆分，也不会重复计入新一天。
- v0.3 RC 已完成轻量状态动画、减少动态适配、长标题截断、超长计时布局、输入边界和 1920/1440/1366/1280/1024/768/390px 响应式回归。
- `IRON_CURTAIN` 已具备专属开幕/落幕转场、壁纸铁幕态、独立运行与暂停语言，以及对导航、统计和 History 的克制弱化；其业务数据仍与其他模式共用统一 FocusSession。
- `FocusSegment` 已覆盖专注、休息与中断，支持实时切换、暂停续段、刷新恢复、有效时间、历史展开、落幕明细、多任务汇总和单段补录。
- Focus 生命周期及过程切换写入 `LifeEvent`，可直接作为未来 Timeline 数据源；Focus Service 不直接修改成长数值。

## 明确尚未完成

- v0.1.1 的称号和商店子域仍保留部分 `Map<String, Object>`、JSON 字段字符串与旧的 `Manager/System` 历史结构；它们是下一轮强类型化的优先目标。
- 任务管理器仍直接维护任务 JSON 的运行时状态；尚未拆为专用任务 repository。
- `GameCore` 仅为旧直接调用方保留的弃用兼容别名；生产入口是 `GameCommandFacade`。
- 当前没有数据库迁移、用户账户、多端同步、微服务、缓存或消息队列；这些均不属于现阶段范围。

## 技术结构

```text
src/main/java/io/github/aomckin/lifehud/
  controller/    HTTP API
  core/          命令常量、配置、行动目录与兼容入口
  service/       用例编排、游戏规则、查询和 ViewModel 组装
  domain/        Player、任务、行动选项、成就定义与枚举
  repository/    save/log/JSON 文件访问与领域 JSON 映射
  config/        Spring 组合根
  dto/           HTTP 命令、操作结果和事件
src/main/resources/
  static/        原生前端页面、脚本和样式
  application.yml
  data/          随包默认 JSON 数据
src/test/        JUnit 5 + Spring MVC 测试
```

## 主要调用链

```text
浏览器
  -> GameController
  -> GameCommandFacade
  -> ActionService / TaskService / ShopService / TitleService
  -> ProgressionService
  -> AchievementService / LevelService
  -> GameQueryService
  -> GameViewAssembler
  -> 兼容的 OperationResult /state JSON
```

成就链路：

```text
achievements.json
  -> AchievementRepository
  -> AchievementDefinition + enum / record
  -> AchievementSystem 判定
  -> AchievementService 发放奖励
  -> GameViewAssembler 组装旧事件与状态字段
```

## 主要 API

- `GET /state`
- `POST /command`
- `GET /actions/{actionName}/duration-options`
- `POST /api/focus/start`
- `POST /api/focus/{id}/pause|resume|complete|interrupt`
- `POST /api/focus/{id}/segments/switch`
- `PATCH /api/focus/{id}/segments/{segmentId}`
- `POST /api/focus/manual`
- `GET /api/focus/current|today|history`

`/command` 的具体命令名由 `GameCommands` 集中定义；不要在前端或业务服务中新增未受控的字符串命令。

## 数据与兼容性

- `actions.json`、`achievements.json`、`level.json`、`tasks.json`、`special_tasks.json`、`titles.json`、`shop.json`、`save.json`、`focus-sessions.json` 与 `life-events.json` 是当前 JSON 资产。
- 变更 JSON 前必须保留现有字段与默认语义，优先在 repository 层添加映射兼容，而非让服务层解析文件路径、键名或 `JsonNode`。
- `save.json` 是用户数据；不要在开发或测试期间覆写项目内真实 `data/` 存档。测试使用临时目录。

## 启动与验证

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
git diff --check
```

默认服务地址为 <http://localhost:8025>。当前基线测试：68 项通过；Focus 另经桌面端与 390px 小屏浏览器闭环验证，覆盖 Segment 切换、刷新恢复、休息续段、落幕总结、History 展开和番茄时间栏排版，Console 为 0 error / 0 warning。

## 接手建议

1. 先阅读本文件、[`ARCHITECTURE.md`](ARCHITECTURE.md) 和最新 Git 提交。
2. 修改前执行 `git status --short --branch`，确认没有用户未提交的工作。
3. 后续强类型化遵循成就子域的路径：JSON repository 映射 -> domain record / enum -> service 只处理领域类型 -> `GameViewAssembler` 保留 API 输出。
4. 每个可验证切片都运行 `./mvnw.cmd test` 并更新本文的“当前能力/尚未完成/提交基线”。
