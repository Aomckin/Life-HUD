# Life HUD v0.6.1 · Life / 生活输入与万能时间线

Life HUD 的 Java 21 / Spring Boot 3 版本。v0.4 完成了成长循环：Focus / Task 赚取 Energy（每 3 有效分钟 +1），娱乐消费（SPEND）按实际消耗沉淀 EXP（每 10 点 +1）并推进 Level。v0.5「Direction」让 Life HUD 开始知道你为什么往前走：`/dreams` 承载 Dream → Goal → DreamMilestone 三层方向；`/tasks` 是行动台——每日任务紧凑卡片、特别行动舒展卡片，完成即弱化并折叠，方向以「✦ 梦想」的意义展示而非外键字段；`/rituals` 用"仪式 + 步骤 + 执行记录"进入一种状态；`/now` 是陈列页——「现在。」歌单是一面横板沉浸记忆墙：真实 MP3 / FLAC 上传（自动读取元数据与内嵌封面），十首歌以不等大的卡片挂在可设背景的舞台上，听歌次数决定卡片大小，每首歌挂着一句阶段备注，布局持久化、刷新不乱跳；阶段快照把背景、歌曲、次数、备注与布局完整封存。这些模块只产生事实 LifeEvent，成长结算仍由 GrowthEngine 唯一负责。

v0.6.1 让「现在。」的变化留下具体足迹：图片逐张记录路径并可在 Journal 查看缩略图，歌曲替换保留旧歌与新歌，背景、阶段条目、方向和快照事件保存对应对象信息；Journal 增加独立的 `「现在。」` 来源分区，并兼容读取 v0.5.5 已落盘为 SYSTEM 的旧 Now 事件。时间线按日期和日内时间统一倒序，越往下离现在越远；普通 LifeEvent 可在确认后单独删除，Now 页面中的移除操作同样统一复用确认弹窗。

Life 的状态、饮食、睡眠、运动和通用记录均支持一次选择多张图片、保存前即时预览，以及编辑时保留、逐张删除或追加旧图；图片随业务记录持久化并同步进入时间线媒体。Life 页主面板使用固定纵向间距，避免卡片边缘重叠。

Ritual 修正了新建表单按钮文案与新建/编辑状态串用问题；仪式卡片改为稳定的正文与操作区布局，无步骤仪式会引导先编辑而不会进入空执行页，步骤编辑器在窄屏下改为单列且不再撑宽页面。

## v0.6 · Life 与时间线

v0.6 让 Life HUD 开始记录「人是怎么生活的」。`/life` 是今天生活的输入面板：最近一次 Check-in 状态卡、五个快速记录入口（状态 / 饮食 / 睡眠 / 运动 / 通用记录），全部走短表单 Modal，时间可改、默认合理，保存即反馈；「今日生活」列出当天的生活条目，点卡片即可编辑、点删除即同步清理。`/journal` 是统一生活时间线：后端 `GET /api/timeline` 按天聚合 Focus、任务、仪式、成长、睡眠、饮食、运动、Check-in、通用记录与日记，支持日期 / 来源组 / 类型过滤与分页；页首是手动日记 Composer（长文本 + 标签 + 照片）。业务记录是唯一事实源：每个记录拥有且只拥有一个 RECORDED LifeEvent——编辑原位重写事件（version+1），删除连事件一起消失，时间线上没有幽灵。睡眠时长由后端按时间戳计算，跨午夜与补录天然正确；事件时间语义用 occurredAt（何时发生）而非 createdAt（何时补录）。通用记录（喝水 / 咖啡因 / 酒精 / 晒太阳 / 社交 / 身体状态 / 外出）共用同一模型与 API，小众需求走 CUSTOM 标签，不再开新 Controller。数据仍为 `data/` 下的 JSON 存档（sleep-records / meal-records / exercise-records / check-ins / life-records / journal-entries），图片走统一 `/api/images` 上传存储。

## 运行

```powershell
.\mvnw.cmd spring-boot:run
```

浏览器访问 <http://localhost:8025>。首次启动会把随包默认 JSON 复制到工作目录的 `data/`，之后从该目录读写存档。也可用配置项 `lifehud.data-dir` 或环境变量 `LIFEHUD_DATA_DIR` 指定数据目录；为兼容旧存档工作流，同时识别 `OTAKU_ENERGY_DATA_DIR`。

## Focus System

`/focus` 提供铁幕、番茄和自由专注三种节奏，它们共享同一个 `FocusSession` 状态机：`RUNNING ↔ PAUSED → COMPLETED`，运行或暂停状态也可标记为 `INTERRUPTED`。后端保存当前活动片段与已累计有效秒数，因此刷新页面、切换标签页和暂停都不会破坏实际时长。

铁幕模式具有独立的数字仪式感：进入时短暂显示开幕提示，运行期间将当前或自定义壁纸转为低亮、低饱和、轻模糊的铁幕态，并弱化导航、统计和历史；暂停会保留铁幕环境，完成后显示克制的落幕反馈。它不切换独立主题，也不会锁定页面。

铁幕过程由持久化的 `FOCUS`、`BREAK`、`INTERRUPTION` Segment 构成，可切换事项、恢复当前段、汇总多任务并由 FOCUS 段推导有效专注。History、落幕总结和手动补录共用这套模型；番茄支持自定义休息、跳过休息与轻量连续轮次。

番茄配置将“自定义专注”和“休息时长”作为两个独立、对齐的输入字段；运行页只保留底部主结束入口，避免重复操作。若前端资源与尚未重启的旧后端不匹配，Segment 请求会提示重启服务，而非只显示泛化 404。

Focus API 提供 Current Focus、Today Summary 和 History；Today 统计按 Session 的开始日期归属，跨日 Session 保持完整，不拆分或重复统计。Dashboard 只展示当前 Focus 或今日摘要，并可返回工作台。

## Growth System

`/growth` 包含 Overview、Achievements、Milestones 与 Titles。Overview 显示 Level / EXP、近期 Energy、当前称号、成长日志和 7 天轻量趋势，并提供"＋ 记录娱乐"快速入口：选择类型（游戏 / 看番 / 电影 / 视频 / 社交 / 外出娱乐 / 其他）、填写做了什么与 Energy 消耗，提交后经统一 Energy Ledger 真实扣减并结算 EXP。Overview 同时展示最近娱乐与 Energy History（时间 / 变化 / 类型 / 原因）。Milestone 支持创建、编辑、删除和 Pin；Title 支持装备与自定义。Today 的 Energy（今日 +获得 / −消耗）、EXP 和 Level 卡片会直接进入 Growth。

Growth 使用 `growth-events.json` 保存每个 LifeEvent 的处理回执，以 eventId 防止刷新、重试或重算带来重复成长；`energy-history.json` 解释 Energy 的每次变化（EARN / SPEND / DECAY / ADJUST 及请求量）；`growth-snapshots.json` 每日幂等更新当前快照。数值参数全部外置在 `data/growth.json`，EXP 转换余数保存在 `save.json` 的 `exp_conversion_remainder`，小额消费不会被吃掉；新的一天 Energy 会向基准值回归（基准值与倍率在 `data/content/energy-drift.json`）。旧存档中的 Energy、EXP、Achievement 和 Title 会继续保留，旧 Shop / Coin 字段只为兼容读取而存在，不再进入 UI 或命令主流程。

API、规则、持久化与迁移细节见 [GROWTH_V0.4.md](docs/GROWTH_V0.4.md)。

## 测试

```powershell
.\mvnw.cmd test
```

旧 Python 实现保留在相邻的 `宅宅能量条` 仓库中，未被修改。

## 代码状态与交接

当前实现、已完成边界、尚未完成的重构项、主要调用链及验证命令见 [CODEBASE_STATUS.md](docs/CODEBASE_STATUS.md)。开始新的开发任务前请先阅读该文件。
