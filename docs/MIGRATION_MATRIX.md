# Python v1.2 → Java v0.1.0 迁移清单

## v0.4 Growth 迁移补充

| 旧状态 | v0.4 处理 |
|---|---|
| `save.exp` | 作为 total EXP 原值保留，按无硬上限的兼容曲线重新派生 Level |
| `save.energy` | 原值保留，继续限制在 0～180 |
| `unlocked_achievements` | id 原样保留；旧定义仅作历史展示，不再发放 Coin / EXP 奖励 |
| `unlocked_titles` / `equipped_title` | id、原名称与装备状态保留；Buff 不再进入 Growth 计算 |
| Coin / Shop purchase 字段 | 仅兼容读取；UI、状态主视图与购买命令已停用 |
| Focus / Task 的直接奖励 | 改为 LifeEvent → GrowthEngine；eventId 回执防止重复结算 |
| 旧 `energy-history.json` 记录 | 缺少 `type` / `requestedDelta` 字段时按 delta 符号推断（正为 EARN、负为 SPEND），请求量默认等于实际变化 |
| v0.4.0 的 Focus / Task EXP 规则 | 已被核心循环取代：行为只产 Energy（EARN），EXP 仅由 SPEND 结算 |
| `growth.json` 的 `exp_per_energy` | 首次读取时自动换算为 `energy_per_exp`（round(1/旧值)）并回写新格式，缺省 10 |
| v0.4.1 的 1 Energy ≈ 1 EXP | v0.4.2 改为 10 实际消耗 → 1 EXP，余数存入 `save.json` 的 `exp_conversion_remainder`（0 ≤ r < energy_per_exp） |
| v0.1 行动命令 `COMPLETE_ACTION` / `COMPLETE_TIMED_ACTION` | 已在 facade 层停用：旧行动直接加 EXP 绕过 GrowthEngine；未来模块经 LifeEvent / EnergyLedgerService 重新接入 |

状态只有在主要行为、JSON 兼容和对应 JUnit 验证已完成时才标记为“已迁移”。`ui.py`、`themes.py` 及 Tkinter 专用 API 客户端按任务书不在本阶段范围内。

## 核心与业务

| Python v1.2 | Java 等价实现 | 状态 |
|---|---|---|
| `core_types.py` | `domain/ActionDurationOption.java`, `domain/ActionTimerState.java`, `dto/OperationResult.java` | 已迁移 |
| `core_command.py` | `core/GameCommands.java`, `dto/GameCommand.java` | 已迁移 |
| `core_event.py` | `core/GameEvents.java`, `dto/GameEvent.java` | 已迁移 |
| `core.py` | `core/GameCore.java` 及各领域 Service | 已迁移 |
| `player.py` | `domain/Player.java`, `repository/PlayerRepository.java` | 已迁移 |
| `level.py` | `service/LevelService.java` | 已迁移 |
| `task.py` | `domain/DailyTask.java`, `domain/SpecialTask.java`, `service/DailyTaskManager.java`, `service/SpecialTaskManager.java` | 已迁移 |
| `shop.py` | `service/ShopManager.java` | 已迁移 |
| `achievement.py` | `service/AchievementSystem.java` | 已迁移 |
| `title.py` | `service/TitleSystem.java` | 已迁移 |
| `unlockable.py` | `service/UnlockableSystem.java` | 已迁移 |
| `main.py` | `LifeHudApplication.java` | 已迁移 |

## Services

| Python v1.2 | Java 等价实现 | 状态 |
|---|---|---|
| `services/action_service.py` | `service/ActionService.java` | 已迁移 |
| `services/player_service.py` | `service/PlayerService.java` | 已迁移 |
| `services/progression_service.py` | `service/ProgressionService.java` | 已迁移 |
| `services/task_service.py` | `service/TaskService.java` | 已迁移 |
| `services/shop_service.py` | `service/ShopService.java` | 已迁移 |
| `services/achievement_service.py` | `service/AchievementService.java` | 已迁移 |
| `services/title_service.py` | `service/TitleService.java` | 已迁移 |
| `services/view_service.py` | `service/ViewService.java` | 已迁移 |
| `services/__init__.py` | Java 包无需初始化文件 | 不适用 |

## 持久化与工具

| Python v1.2 | Java 等价实现 | 状态 |
|---|---|---|
| `repositories/player_repository.py` | `repository/PlayerRepository.java`, `repository/LogRepository.java` | 已迁移 |
| `repositories/__init__.py` | Java 包无需初始化文件 | 不适用 |
| `utils/json_store.py` | `repository/JsonRepository.java` | 已迁移 |
| `utils/paths.py` | `repository/JsonRepository.java` 的数据根目录解析 | 已迁移 |
| `utils/logger.py` | `repository/LogRepository.java`, `repository/JsonRepository.java` | 已迁移 |
| `utils/time_utils.py` | Java Time API 及各领域时间供应器 | 已迁移 |
| `utils/__init__.py` | Java 包无需初始化文件 | 不适用 |

## API 与 Web

| Python v1.2 | Java 等价实现 | 状态 |
|---|---|---|
| `api.py` | `controller/GameController.java`, `config/WebConfig.java` | 已迁移 |
| `static/index.html` | `src/main/resources/static/index.html` | 已迁移（仅品牌更名） |
| `static/style.css` | `src/main/resources/static/style.css` | 已迁移（字节一致） |
| `static/app.js` | `src/main/resources/static/app.js` | 已迁移（字节一致） |
| `clients/api_client.py` | Web 前端继续使用原生 Fetch；该文件仅供 Tkinter 客户端 | 不在本阶段范围 |
| `clients/__init__.py` | 同上 | 不在本阶段范围 |
| `ui.py` | Tkinter 客户端 | 不在本阶段范围 |
| `themes.py` | Tkinter 主题 | 不在本阶段范围 |

## JSON 数据

| Python v1.2 | Java 资源 | 状态 |
|---|---|---|
| `actions.json` | `data/actions.json` | 已迁移，6 个行动，语义等价 |
| `level.json` | `data/level.json` | 已迁移，语义等价 |
| `tasks.json` | `data/tasks.json` | 已迁移，8 个每日任务，语义等价 |
| `special_tasks.json` | `data/special_tasks.json` | 已迁移，17 个特殊任务，语义等价 |
| `shop.json` | `data/shop.json` | 已迁移，4 个商品，语义等价 |
| `achievements.json` | `data/achievements.json` | 已迁移，41 个成就，语义等价 |
| `titles.json` | `data/titles.json` | 已迁移，13 个称号，语义等价 |
| `save.json` | `data/save.json` | 已迁移，存档字段集与旧存档缺省兼容已验证 |
| `config.json` | `data/config.json` | 已迁移，仅 `window_title` 更名为 Life HUD |

## 测试

| Python v1.2 测试 | Java JUnit 5 对应 | 状态 |
|---|---|---|
| `test_core.py` | `core/GameCoreTest.java` | 已迁移 |
| `test_timed_action.py` | `domain/ActionTimerStateTest.java`, `core/GameCoreTest.java` | 已迁移 |
| `test_task_manager.py` | `service/TaskManagerTest.java`, `core/GameCoreTest.java` | 已迁移 |
| `test_shop.py` | `service/ShopManagerTest.java`, `core/GameCoreTest.java` | 已迁移 |
| `test_achievement.py` | `service/AchievementSystemTest.java` | 已迁移 |
| `test_title.py` | `service/TitleSystemTest.java` | 已迁移 |
| `test_player_service.py` | `service/PlayerAndLevelServiceTest.java`, `ArchitectureTest.java` | 已迁移 |
| `test_player_repository.py` | `repository/RepositoryTest.java` | 已迁移 |
| `test_utils.py` | `repository/RepositoryTest.java`, `service/TaskManagerTest.java` | 已迁移 |
| `test_api.py` | `controller/GameControllerTest.java` | 已迁移 |
| `test_architecture.py` | `ArchitectureTest.java` | 已迁移 |
| `test_api_client.py` | Tkinter API 客户端专用 | 不在本阶段范围 |
| `test_theme.py` | Tkinter 主题专用 | 不在本阶段范围 |

所有 Java 主代码和测试代码均位于 `io.github.aomckin.lifehud` 根包下。

## v0.4.2 内容外置补充

| 原硬编码位置 | v0.4.2 去向 |
|---|---|
| `GrowthCatalog` 内 14 个成就 + 5 个称号（文案 / 条件 / hidden / 图标 / 映射） | `data/content/growth-achievements.json`、`growth-titles.json`，启动时播种 |
| 成就 / 称号结算 reason、派生事件措辞与标签、旧版兜底文案、停用命令提示、任务日志模板、里程碑默认分类 | `data/content/growth-copy.json`（`GrowthCopy` 加载） |
| `/api/growth` 的 version 字符串 | `data/content/app.json`（`AppInfo` 加载） |
| Task Energy 上限 20（双重 clamp）、Overview 的 history(12) / trend(7) 窗口 | `growth.json` 的 `task_energy_cap`、`overview_history_limit`、`trend_days` |
| energyDelta clamp ±180、LevelService 防御上限 10000 | 引用 `config.json` 的 `max_energy`；`LevelService.LEVEL_CEILING` 常量 |
| 前端 MODE_LABELS、表单 maxlength、共享文案 | `static/content/copy.js` |

## v0.4 封版补充（Entertainment）

| 事项 | 处理 |
|---|---|
| Energy 消费的真实生活入口 | `EntertainmentRecordService`（`/api/entertainment`）：记录事实 LifeEvent 后复用 `EnergyLedgerService.spend()`，无第二套消费逻辑 |
| 娱乐 Energy 消耗算法 | 无自动算法；`durationMinutes` 记录事实，`energyCost` 由用户填写 |
| Energy 不足时的娱乐记录 | 照常记录现实：记录保留请求量 `energyCost`，Growth 按实际扣减结算 EXP |
| 娱乐记录编辑 | title / category / durationMinutes / note 可改；已结算的 energyCost 与 occurredAt 不可修改 |
| 娱乐记录删除 | 只删事实记录，Energy / EXP 历史不回滚；需修正走 Energy Adjust |
| GrowthSnapshot 的 entertainmentMinutes / energySpent | 留给 v0.5 扩展 |

## v0.4.2 每日 Energy 回归补充

| 事项 | 处理 |
|---|---|
| 新一天 Energy 向基准值回归 | `midpoint=90`、`day_start_factor=0.75` 外置在 `data/content/energy-drift.json`；每日最多一次，标记在 `save.json` 的 `energy_drift_date` |
| 回归的账目表达 | 以 ADJUST 类型 LifeEvent（`ENERGY_CHANGED`）经 GrowthEngine 结算，进 Energy History，不产生 EXP、不触碰转换余数 |
