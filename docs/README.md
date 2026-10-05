# 文档导航

Life HUD 的当前入口是仓库根目录的 [README](../README.md)。本目录按用途保存 API、架构、运维、发布和历史资料。`history/` 中的任务书与计划保留编写时的版本语境；其中的“当前状态”和旧路径不代表 v1.0.0 的现状。

## 当前参考文档

| 分类 | 文档 |
| --- | --- |
| API | [API 索引](api/API_INDEX.md) · [Agent Context API](api/AGENT_CONTEXT_API.md) · [Agent Action API](api/AGENT_ACTION_API.md) |
| 架构 | [架构演进与调用链](architecture/ARCHITECTURE.md)（含早期 GameCore）· [Growth v0.4 实现说明](architecture/GROWTH_V0.4.md) |
| 运维 | [备份与恢复](operations/BACKUP_AND_RESTORE.md) |
| 发布 | [v1.0.0 Release Note](releases/RELEASE_V1.0.md) · [版本历史](../CHANGELOG.md) |
| 方向 | [未来开发方向记录](planning/FUTURE_DIRECTIONS.md)（设想，不等于承诺） |

## 历史资料

- [Python → Java 迁移清单](history/MIGRATION_MATRIX.md)。
- [2026-08-31 修订版 v0.2–v1.0 计划](history/plans/v0.2-v1.0-development-plan-2026-08-31.md)：保留当时的规划语境；实际发布能力以当前文档为准。

### 历史任务书

文件名采用 `v版本号-主题-task.md`；追加任务使用 `addendum`，没有明确版本号的任务保留主题名。版本号和标题按原始文档记录，不把任务书中的计划当作已实现功能。

| 阶段 | 任务书 |
| --- | --- |
| v0.3 Focus | [主任务](history/tasks/v0.3.0-focus-task.md) · [追加一](history/tasks/v0.3-focus-addendum-1.md) · [追加二](history/tasks/v0.3-focus-addendum-2.md) · [RC 收尾](history/tasks/v0.3-rc-task.md) |
| v0.4 Growth | [总任务](history/tasks/v0.4-growth-task.md) |
| v0.5 Direction / Now / Tasks | [v0.5.0](history/tasks/v0.5.0-direction-task.md) · [v0.5.1](history/tasks/v0.5.1-now-task.md) · [v0.5.2](history/tasks/v0.5.2-now-playlist-task.md) · [v0.5.3](history/tasks/v0.5.3-tasks-task.md) · [v0.5.3.1 追加](history/tasks/v0.5.3.1-task-pool-addendum.md) |
| v0.6 Life / Ritual | [v0.6](history/tasks/v0.6-life-timeline-task.md) · [v0.6.1](history/tasks/v0.6.1-now-timeline-task.md) · [v0.6.2](history/tasks/v0.6.2-ritual-task.md) |
| v0.7–v0.9 | [Media](history/tasks/v0.7-media-task.md) · [Integration](history/tasks/v0.8-integration-task.md) · [正式版收官](history/tasks/v0.9-release-task.md) |
| 单项任务 | [Task Pool 扩充](history/tasks/task-pool-expansion.md) |

截图统一放在 [`assets/`](assets/)；主页使用的是经过挑选的展示图。
