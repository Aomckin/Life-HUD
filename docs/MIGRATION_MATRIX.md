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
