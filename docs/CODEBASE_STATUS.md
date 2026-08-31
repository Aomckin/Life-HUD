# Life HUD 代码现状速览

> **当前分支 v0.7「Media / 宅宅生活档案」（版本 0.7.0）**：新增强类型 Anime / AnimeWatchSession、MediaGame / MediaGameSession 与统一 MediaItem（BOOK / MANGA / MOVIE / OTHER），由 `MediaRepository` 分别持久化到五份 JSON。`MediaService` 校验进度、评分与时长；Anime Session 自动向前推进集数并在最终集完成，Game 总时长始终按 base + 当前 Session 求和重算。Session 创建、编辑、删除通过 `LifeFactRecorder` 维护唯一 `ANIME_WATCHED` / `GAME_PLAYED` 事件，来源为 MEDIA，发生时间使用 watchedAt / endTime；作品档案加入、编辑、移除另行记录 `MEDIA_ADDED` / `MEDIA_UPDATED` / `MEDIA_REMOVED` 历史活动，更新事件正文会列出进度、状态、评分、平台等具体前后差异。`TimelineService` 与 Journal 增加 media 来源组。`/media` 已由 placeholder 升级为响应式作品墙，包含当前陪伴 Hero、分类筛选、中文状态、封面文件/剪贴板上传、作品编辑删除、详情与快速 Session 记录。Focus 历史新增删除入口并同步清理时间线事件，空 Focus JSON 也按无记录安全读取；Now 歌单最大卡阈值调整为 1000 次，歌曲编辑文案不再出现 undefined。Media 与旧 `/api/entertainment`、Growth、`/now` 保持隔离，未修改旧 `GameController`。版本和静态资源标识升级至 0.7.0；自动化测试 168 项全部通过。

> **当前分支 v0.6.2「Ritual 状态入口」**：Ritual 默认页改为轻量卡片与按需出现的编辑区；用户侧“步骤”统一为“仪式片段”，默认仅展示核心提示，高级字段折叠，支持片段增删和上下排序。执行链新增开场、隐藏普通导航的沉浸逐片段执行态、进度/上一步/继续/跳过/中途退出，以及明确落幕页；宽屏沉浸态显式脱离普通 12 列布局。复用原 Ritual API 与 JSON 字段，旧数据无需迁移。全局根滚动容器固定预留纵向滚动槽，壁纸不再因滚动条出现而横向跳变。「现在。」歌单从 13 个固定锚点扩展为 26 个不规则挂点，重新排版会改变挂点、微偏移与旋转，空槽随布局散开且视觉弱化。版本与相关静态资源缓存标识升级至 0.6.2；测试基线 158 项通过，布局算法检查确认不同随机盐下 10/10 卡片换位且坐标、旋转均在合法范围。

> **当前分支 v0.6.1「让『现在。』留下具体足迹」**：Now LifeEvent 新增稳定 `LifeEventSourceType.NOW`；旧存档中 `source=now/sourceType=SYSTEM` 的事件在读取边界自动归一化，不改写用户 JSON。图片由数量差改为逐路径差异并写入 `metadata.images`，Journal 可复用缩略图与 lightbox；歌曲替换保留 before/after，歌曲移除、背景设置/替换/清除、玩看读条目、梦想/方向与快照删除均保存具体对象元数据。`TimelineService` 新增 `now` 来源组，`/journal` 新增用户可见文案严格为 `「现在。」` 的独立筛选与专属空状态；日期和日内事件统一按 occurredAt 倒序。普通 LifeEvent 支持确认后单独删除且不反向撤销业务记录，Now 各删除入口统一复用 Dreams 确认弹窗。Life 的状态、饮食、睡眠、运动和通用记录全部支持多图选择、即时预览、旧图逐张移除与追加，图片随业务记录和 LifeEvent 同步；页面修复宽屏栅格挤压、重点卡片内边距及主面板重叠。Ritual 修复表单按钮 undefined、新建/编辑提交状态串用、卡片内容与操作区挤压、无步骤误启动及窄屏步骤编辑器溢出。版本与静态资源缓存标识升级至 0.6.1。测试基线 158 项通过；桌面端与 390px 窄屏验收通过，Console 0 warning / 0 error。开发范围与验收标准见 [V0.6.1_NOW_TIMELINE_TASK.md](V0.6.1_NOW_TIMELINE_TASK.md)。

> **当前分支 v0.6「Life / 生活输入与万能时间线」（版本 0.6.0）**：按《Life HUD v0.6 总开发任务书》完成。业务侧六条 JSON 链路（domain record + repository + service + controller）：Sleep（`/api/life/sleep`，durationMinutes 后端按时间戳计算、跨午夜/补录天然正确、>20h 拒绝、occurredAt=wakeTime）、Meal（`/api/life/meals`，含 images 路径列表，update 传 null 保留图片）、Exercise（`/api/life/exercises`，1~1440min 校验）、CheckIn（`/api/life/check-ins`，四项 1~10 服务端 clamp，latest 按时间而非插入序）、LifeRecord（`/api/life/records`，WATER/CAFFEINE/ALCOHOL/SUNLIGHT/SOCIAL/BODY_STATUS/OUTDOOR/CUSTOM 共用一链，CUSTOM 强制 label，单位有默认值）、Journal（`/api/journal`，长文本+tags+images）。事件同步统一走 `LifeFactRecorder`：创建幂等发射一条 RECORDED 事件、编辑原位重写（version+1）、删除连事件清除——无幽灵；`LifeEventRepository` 新增 replace/delete，`LifeEventService` 新增 recordAt（补录时 occurredAt=发生时间）与 findBySource。聚合侧 `TimelineService` + `GET /api/timeline`（date/startDate/endDate/sources/type/page/size，source 组：focus/task/life/journal/growth/ritual/dream，按 occurredAt 倒序分页，DTO 为 TimelineItem，media 取 metadata.images）。文案在 growth-copy.json 的 `life` 段（GrowthCopy.life* 方法）。前端：`/life` 输入面板（life.js：最近状态卡 + 五个 Modal 快速录入 + 今日生活，点卡片编辑、带删除确认）；`/journal` 重建为按天分组时间线（timeline.js：来源组过滤 tabs、日记 Composer、图片 lightbox、长文折叠、往更早翻）。缓存版本 `?v=0.6.0`。测试 146 项（新增 Sleep/CheckIn/LifeRecord/Journal/Meal+Exercise/Timeline 六个测试类 28 项）。已通过真实生活全链路验收（补录跨午夜睡眠 7h31min、Check-in、带图饮食、舞萌 92min、UI 日记、编辑同步 250→500ml、删除无幽灵），验收测试数据已清理。明确定位：v0.7 Media / v0.8 Agent Context 的统一生活事实层。

> **v0.5.5 追加：「现在。」变更事实化 + 表单样式修复**：NowService 现在把「现在。」的增删类变更写入 LifeEvent——歌曲上餐/换曲/取下（NOW_SONG_ADDED / NOW_SONG_REPLACED / NOW_SONG_REMOVED，slot 入 metadata）、歌单背景挂上/取下（NOW_BACKGROUND_CHANGED）、玩/看/读清单条目进出（NOW_STAGE_ITEM_ADDED / REMOVED，按标题 diff，同标题只改备注不记）、照片贴上/揭下（NOW_IMAGE_ADDED / REMOVED，按张数）、当下梦想/方向进出（NOW_DIRECTION_CHANGED，逐条解析标题）、快照删除（NOW_SNAPSHOT_DELETED）；快照创建沿用手有 NOW_SNAPSHOT_CREATED。措辞类编辑（阶段标题、主题、引言、正文、歌单题注）与纯排版重排（updateSong 布局字段）刻意不产生事件。文案在 growth-copy.json 的 direction 段，GrowthCopy 以 {slot}/{verb}/{count}/{title} 模板渲染；用户已确认不必兼容旧存档文案。样式：`.field input[type=file]` 不再继承 48px 高度（focus.css），dreams 详情编辑表单的封面选择框文字不再歪斜。

> **当前分支 v0.5.5（修复版，基于 v0.5.4）**：三项修复——① `/now` 歌单「重新排版」按钮此前只在前端内存里重算且确定性算法产出与原布局完全相同、也从不持久化；现在 `relayout(songs, salt)` 以点击时刻为盐值旋转锚位分配（`pickAnchor`），每次点击产出可见的新布局，并逐首 `PUT /api/now/songs/{slot}` 持久化 posX/posY/rotationDeg/zIndex，刷新不回退。② dreams 页删除/归档此前依赖原生 `window.confirm`，在嵌入式浏览器中会被静默拦截或冻结页面（表现为「点了没反应/卡死」）；`ui.js` 新增 `confirmDialog(message)` 页内确认弹窗（复用 board-modal 样式，`.confirm-modal`），dreams 页全部 5 处原生 confirm 已替换。③ 静态资源缓存版本号 `?v=0.5.3` 全量升级为 `?v=0.5.5`（v0.5.4 修删除时未升版本号，浏览器可能一直使用旧缓存的 dreams.js，这是「修了还是坏」的直接原因）。growth/tasks/rituals/now 快照删除仍用原生 confirm，留待后续统一替换。

> **当前分支 v0.5「Direction」（版本 0.5.3，行动台重构）**：`/tasks` 重构为卡片式行动台（Header 今日摘要 + Daily 紧凑卡 + Special 舒展卡 + 已完成折叠 + Direction 意义 chip + 编辑 Modal），完成走 `POST /api/task-directions/{source}/{taskId}/complete`（TaskService.completeDailyById/completeSpecialById，幂等）。`NowSong` 扩展 playCount / note / posX / posY / rotationDeg / zIndex；NowState 增加 playlistBackgroundImage / playlistTitle / playlistSubtitle；`PUT /api/now/songs/{slot}` 编辑、`POST|DELETE /api/now/background` 背景管理；前端 `now-playlist.js` PlaylistBoard 组件（锚带自动布局、playCount→尺寸分级、纸条备注、编辑 Modal、快照只读复用）。v0.5.1 重做 `/now` 为阶段陈列页：`NowSong`（10 个固定槽位）+ `AudioStorageService`（jaudiotagger 读取 MP3/FLAC 标签与内嵌封面，缺标签回退文件名/未知艺术家）；图片上传 MIME 缺失时按扩展名兜底，multipart 上限 64MB；NowSnapshot 冻结整条歌曲记录。Dream → Goal → DreamMilestone（`/api/dreams` 系）+ Task 可选方向关联（`/api/task-directions`）+ Ritual/RitualStep/RitualExecution（`/api/rituals` 系）+ NowState/NowSnapshot（`/api/now` 系）+ 统一图片上传（`/api/images`，存 `data/uploads/`）。新事件：DREAM_CREATED / DREAM_COMPLETED / GOAL_CREATED / GOAL_COMPLETED / DREAM_MILESTONE_COMPLETED / NOW_SNAPSHOT_CREATED；完成类操作幂等，编辑不产生事件。DreamService/DirectionLinkService 有循环依赖，用 ObjectProvider 打破。v0.4 Growth 结论见下。

> **v0.4 已封版（2026-08-29，最终版本号 v0.4.2）**。最终核心循环：Focus / Task → Energy EARN（每 3 有效分钟 +1，单事件上限 80）；娱乐记录（`EntertainmentRecordService`，`/api/entertainment`）→ 统一 Energy Ledger SPEND → 每 10 点实际消耗 +1 EXP（整数余数池 `exp_conversion_remainder`）→ Level。新的一天 Energy 向基准值回归（`midpoint=90`、`day_start_factor=0.75`，内容文件 `data/content/energy-drift.json`，每日最多一次、ADJUST 记账、不产 EXP）。Energy 不足时照常记录现实、按实际剩余结算；娱乐记录的 energyCost 手动填写、不可修改，删除不回滚账目。Achievement / Milestone / Title 只记录不参与数值；Shop / Coin / 旧行动命令全部停用。参数在 `data/growth.json`，内容在 `data/content/` + `static/content/copy.js`。Snapshot 的娱乐字段扩展留给 v0.5。详见 [GROWTH_V0.4.md](GROWTH_V0.4.md)。

> 下文保留 v0.3.0 的重构背景，供调用链追溯。

## 当前能力

- `/api/media/anime`、`/api/media/games`、`/api/media/items` 提供作品 CRUD；Anime / Game 各有独立 Session CRUD。
- Media 数据使用 `media-anime.json`、`media-anime-sessions.json`、`media-games.json`、`media-game-sessions.json`、`media-items.json`，文件不存在时按空集合启动。
- `/media` 提供作品墙、详情、封面、状态/评分/备注和快速观看/游玩足迹；`/journal?sources=media` 对应前端“媒体”筛选。

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

- `actions.json`、`achievements.json`、`level.json`、`tasks.json`、`special_tasks.json`、`titles.json`、`shop.json`、`save.json`、`focus-sessions.json`、`life-events.json` 与五份 `media-*.json` 是当前 JSON 资产。
- 变更 JSON 前必须保留现有字段与默认语义，优先在 repository 层添加映射兼容，而非让服务层解析文件路径、键名或 `JsonNode`。
- `save.json` 是用户数据；不要在开发或测试期间覆写项目内真实 `data/` 存档。测试使用临时目录。

## 启动与验证

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
git diff --check
```

默认服务地址为 <http://localhost:8025>。当前自动化测试基线：168 项通过。

## 接手建议

1. 先阅读本文件、[`ARCHITECTURE.md`](ARCHITECTURE.md) 和最新 Git 提交。
2. 修改前执行 `git status --short --branch`，确认没有用户未提交的工作。
3. 后续强类型化遵循成就子域的路径：JSON repository 映射 -> domain record / enum -> service 只处理领域类型 -> `GameViewAssembler` 保留 API 输出。
4. 每个可验证切片都运行 `./mvnw.cmd test` 并更新本文的“当前能力/尚未完成/提交基线”。
