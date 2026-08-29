# Life HUD v0.4.0 · Growth

Life HUD 的 Java 21 / Spring Boot 3 版本。v0.4.0 把 LifeEvent 变成统一成长输入：Focus 与 Task 只记录真实行为，Growth Engine 按 eventId 幂等推导 Energy、EXP、Level、Achievement 与 Title，并提供可编辑的人生 Milestone、每日 Growth Snapshot 和完整 Growth 页面。

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

`/growth` 包含 Overview、Achievements、Milestones 与 Titles。Overview 显示 Level / EXP、近期 Energy、当前称号、成长日志和 7 天轻量趋势；Milestone 支持创建、编辑、删除和 Pin；Title 支持装备与自定义。Today 的 Energy、EXP 和 Level 卡片会直接进入 Growth。

Growth 使用 `growth-events.json` 保存每个 LifeEvent 的处理回执，以 eventId 防止刷新、重试或重算带来重复成长；`energy-history.json` 解释 Energy 的每次变化；`growth-snapshots.json` 每日幂等更新当前快照。旧存档中的 Energy、EXP、Achievement 和 Title 会继续保留，旧 Shop / Coin 字段只为兼容读取而存在，不再进入 UI 或命令主流程。

API、规则、持久化与迁移细节见 [GROWTH_V0.4.md](docs/GROWTH_V0.4.md)。

## 测试

```powershell
.\mvnw.cmd test
```

旧 Python 实现保留在相邻的 `宅宅能量条` 仓库中，未被修改。

## 代码状态与交接

当前实现、已完成边界、尚未完成的重构项、主要调用链及验证命令见 [CODEBASE_STATUS.md](docs/CODEBASE_STATUS.md)。开始新的开发任务前请先阅读该文件。
