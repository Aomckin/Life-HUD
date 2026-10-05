# Life HUD API 索引

本索引面向 Life HUD Web UI 与未来 `lifehud-tool`，列出 v1.0 的主要稳定业务入口。具体请求字段、状态转换、幂等与重试规则见 [Agent Action API](AGENT_ACTION_API.md)，只读聚合结构见 [Agent Context API](AGENT_CONTEXT_API.md)。

Context API 不是 CRUD API。所有写入都必须经过业务 API，确保校验、持久化、`LifeEvent`、Timeline 与 Growth 规则始终只有一个事实来源。

## 运行与错误边界

v1.0 是本地、单用户软件，不提供 API 身份认证，默认监听 `127.0.0.1:8025`，不应直接暴露到公共互联网。授权、确认、规划、自然语言理解与跨系统重试属于外部 Agent Runtime 的职责。

所有时间戳均为 ISO-8601 字符串，ID 均为不透明字符串。创建通常返回 `201`，删除通常返回 `204`，其他成功操作返回 `200`。错误统一使用 JSON：

```json
{
  "timestamp": "2026-08-31T06:00:00Z",
  "status": 400,
  "detail": "可读的错误说明",
  "path": "/api/..."
}
```

客户端应将 `400` 视为输入无效，`404` 视为对象不存在，`409` 视为状态冲突，`413` 视为上传文件过大，`5xx` 视为服务器或数据文件故障。

## Agent Context：优先读取的聚合视图

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/agent/context/today` | 今日总览 |
| GET | `/api/agent/context/recent?days=7` | 最近事实 |
| GET | `/api/agent/context/status` | 当前状态 |
| GET | `/api/agent/context/focus` | 当前与今日专注 |
| GET | `/api/agent/context/tasks` | 任务语义视图 |
| GET | `/api/agent/context/dreams` | 方向语义视图 |
| GET | `/api/agent/context/life` | 生活记录语义视图 |
| GET | `/api/agent/context/journal?limit=20` | 日记语义视图 |
| GET | `/api/agent/context/media` | 媒体语义视图 |
| GET | `/api/agent/context/growth` | 成长语义视图 |

## Dashboard / Today

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/dashboard` | Dashboard 聚合数据 |
| GET/POST | `/api/dashboard/headlines` | 查询或新增 Today 文案 |
| DELETE | `/api/dashboard/headlines/{id}` | 删除自定义 Today 文案 |
| GET | `/api/dashboard/selections` | 查询三个陪伴位选择 |
| PUT | `/api/dashboard/selections/{slot}` | 选择 Dream、Ritual 或 Media 展示项 |

`slot` 使用 Dashboard 支持的槽位标识；请求体携带业务对象 `id` 和 `type`。Ritual 在 Dashboard 中统一显示为“仪式”。

## Focus

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/api/focus/start` | 开始专注 |
| GET | `/api/focus/current` | 当前专注 |
| GET | `/api/focus/today` | 今日专注统计 |
| GET | `/api/focus/history?limit=30` | 专注历史 |
| POST | `/api/focus/{id}/pause`、`/resume` | 暂停或恢复 |
| POST | `/api/focus/{id}/segments/switch` | 切换 Segment |
| PATCH | `/api/focus/{id}/segments/{segmentId}` | 修改 Segment |
| POST | `/api/focus/{id}/complete`、`/interrupt` | 完成或中断 |
| POST | `/api/focus/manual` | 补录专注 |
| DELETE | `/api/focus/{id}` | 删除记录 |

## Tasks 与 Direction

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/task-pool` | 查询 Daily/Special 任务池 |
| POST | `/api/task-pool/daily`、`/special` | 创建任务 |
| PUT/DELETE | `/api/task-pool/daily/{id}`、`/special/{id}` | 修改或删除任务 |
| POST | `/api/task-pool/daily/{id}/enabled`、`/special/{id}/enabled` | 启停任务 |
| GET | `/api/task-directions` | 查询任务方向关联 |
| PUT/DELETE | `/api/task-directions/{source}/{taskId}` | 设置或删除关联 |
| POST | `/api/task-directions/{source}/{taskId}/complete` | 幂等完成任务 |

`source` 区分 Daily 与 Special 来源。外部工具完成任务时应优先使用 `/api/task-directions/.../complete`，不要直接修改 JSON。

## Dreams、Goals 与方向里程碑

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/POST | `/api/dreams` | 查询或创建 Dream |
| GET/PUT/DELETE | `/api/dreams/{id}` | 详情、修改或归档 |
| DELETE | `/api/dreams/{id}/purge` | 永久删除 Dream |
| POST | `/api/dreams/{id}/complete`、`/pause`、`/resume` | 状态转换 |
| POST | `/api/dreams/{dreamId}/goals` | 创建 Goal |
| PUT/DELETE | `/api/goals/{id}` | 修改或删除 Goal |
| POST | `/api/goals/{id}/complete` | 完成 Goal |
| POST | `/api/goals/{goalId}/milestones` | 创建 Dream Milestone |
| PUT/DELETE | `/api/dream-milestones/{id}` | 修改或删除 Dream Milestone |
| POST | `/api/dream-milestones/{id}/complete` | 完成 Dream Milestone |

## Ritual

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/POST | `/api/rituals` | 查询或创建仪式 |
| GET/PUT/DELETE | `/api/rituals/{id}` | 详情、修改或删除 |
| POST | `/api/rituals/{id}/enable`、`/disable` | 启停仪式 |
| POST | `/api/rituals/{id}/start` | 开始一次执行 |
| GET | `/api/ritual-executions/{id}` | 执行详情 |
| POST | `/api/ritual-executions/{id}/steps/{stepId}` | 更新步骤状态 |
| POST | `/api/ritual-executions/{id}/complete`、`/cancel` | 完成或取消执行 |

## Life、Journal 与 Timeline

以下资源支持集合 GET/POST 与单项 GET/PUT/DELETE：

| 资源 | 路径 |
| --- | --- |
| 睡眠 | `/api/life/sleep[/{id}]` |
| 饮食 | `/api/life/meals[/{id}]` |
| 运动 | `/api/life/exercises[/{id}]` |
| Check-in | `/api/life/check-ins[/{id}]` |
| 通用生活记录 | `/api/life/records[/{id}]` |
| Journal | `/api/journal[/{id}]` |

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/timeline` | 统一时间线 |
| GET | `/api/life-events?limit=8`、`/api/life-events/recent?limit=8` | 最近事实 |
| DELETE | `/api/life-events/{id}` | 删除允许删除的事实 |
| POST | `/api/images` | 上传可信图片，返回 `/uploads/...` 地址 |

图片上传仅校验 MIME 或扩展名，不做 Magic Bytes/真实内容识别；这是本地可信环境的已知限制。

## Media

| 资源 | 集合与单项路径 | Session 路径 |
| --- | --- | --- |
| Anime | `/api/media/anime[/{id}]` | `/api/media/anime/{id}/sessions`、`/api/media/anime-sessions/{id}` |
| Game | `/api/media/games[/{id}]` | `/api/media/games/{id}/sessions`、`/api/media/game-sessions/{id}` |
| Book/Manga/Movie/Other | `/api/media/items[/{id}]?type=...` | 无独立 Session 入口 |

集合支持 GET/POST，单项支持 GET/PUT/DELETE；Session 支持查询、创建、修改和删除。记录观看或游玩事实必须调用 Session API。

## Now

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/PUT | `/api/now` | 读取或更新当前阶段状态 |
| POST | `/api/now/songs` | multipart 上传歌曲并加入歌单 |
| PUT/DELETE | `/api/now/songs/{slot}` | 修改或移除指定槽位歌曲 |
| POST/DELETE | `/api/now/background` | multipart 设置或清除背景 |
| GET/POST | `/api/now/snapshots` | 查询或创建阶段快照 |
| GET/DELETE | `/api/now/snapshots/{id}` | 查询或删除指定快照 |

歌曲与背景进入统一 uploads 存储；相同文件内容复用同一路径。快照封存当前阶段、歌曲、备注、次数、布局和背景。

## Growth、Titles、Milestones 与 Entertainment

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/growth` | Growth 总览 |
| GET | `/api/growth/history?limit=30` | EXP/成长历史 |
| GET | `/api/growth/energy-history` | Energy 历史 |
| GET | `/api/growth/snapshots?days=30` | 成长快照 |
| POST | `/api/growth/recalculate` | 从事实重新结算 |
| POST | `/api/growth/energy/spend` | 消耗 Energy |
| POST | `/api/growth/energy/adjust` | 人工调整 Energy |
| GET/POST | `/api/titles` | 查询或创建 Title |
| POST | `/api/titles/{id}/equip` | 装备 Title |
| POST | `/api/titles/unequip` | 卸下当前 Title |
| DELETE | `/api/titles/{id}` | 删除 Title |
| GET/POST | `/api/milestones` | 查询或创建成长 Milestone |
| PATCH/DELETE | `/api/milestones/{id}` | 修改或删除 Milestone |
| GET/POST | `/api/entertainment` | 查询或记录娱乐消费 |
| PATCH/DELETE | `/api/entertainment/{id}` | 修改或删除娱乐记录 |
| GET | `/api/achievements`、`/api/achievements/{id}` | 查询 Achievement |

`/api/growth/recalculate`、Energy 调整和永久删除属于高影响操作；未来 `lifehud-tool` 应在调用前取得用户确认。普通事实创建也应携带稳定的幂等键，规则见 Agent Action API。

## 兼容接口

`/state`、`/actions/{actionName}/duration-options` 与 `/command` 是早期 GameCore 兼容接口。新工具应优先使用上述业务 API；兼容接口在 v1.0 保留，但不作为新集成的首选入口。
