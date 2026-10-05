# Life HUD Agent Context API

> 面向外部 Agent 的只读对接契约。当前 API schema 版本为 `1`，对应 Life HUD v0.8+。

## 1. 快速接入

- Base URL：`http://localhost:8025/api/agent/context`
- 数据格式：JSON，UTF-8
- 鉴权：当前本地版本没有鉴权；不要直接暴露到公网
- 时区：服务端使用 Life HUD 所在系统时区（当前默认运行环境为 `Asia/Shanghai`）划分“今天”
- 时间：`Instant` 字段使用 ISO 8601 UTC 格式，例如 `2026-08-31T02:30:00Z`
- 日期：`LocalDate` 字段使用 `YYYY-MM-DD`
- 空数据：对象可能为 `null`，集合返回空数组 `[]`
- 写入：本组 API 全部为 `GET`，不会创建、修改或删除 Life HUD 数据

最推荐的上下文入口：

```bash
curl http://localhost:8025/api/agent/context/today
```

外部 Agent 应先读取 `schemaVersion`，仅在支持该版本时继续解析：

```js
const response = await fetch("http://localhost:8025/api/agent/context/today");
if (!response.ok) throw new Error(`Life HUD ${response.status}`);
const context = await response.json();
if (context.schemaVersion !== "1") throw new Error("Unsupported Agent Context schema");
```

## 2. 端点一览

| 方法 | 路径 | 参数 | 用途 |
| --- | --- | --- | --- |
| GET | `/today` | — | 今日完整上下文，建议作为 Agent 的默认入口 |
| GET | `/recent` | `days`，默认 7，范围 1–30 | 最近 N 个自然日的摘要与时间线 |
| GET | `/status` | — | 当前 Energy、等级、称号、状态与进行中的专注 |
| GET | `/focus` | — | 今日专注、当前 Session、近期 Session 与 7 日趋势 |
| GET | `/tasks` | — | 当前每日任务和特别行动 |
| GET | `/dreams` | — | 活跃 Dream、其活跃 Goal 与 Milestone |
| GET | `/life` | — | 今日生活事实与最近状态 |
| GET | `/journal` | `limit`，默认 20，范围 1–100 | 最近日记及同数量上限的时间线 |
| GET | `/media` | — | 在看、在玩、进行中的作品与近期 Session |
| GET | `/growth` | — | 当前成长状态、近期结算记录与快照 |

除 `/recent` 外，所有顶层响应都包含：

```json
{
  "schemaVersion": "1",
  "generatedAt": "2026-08-31T02:30:00Z"
}
```

`generatedAt` 表示本次聚合生成时间，不是数据最后修改时间。

## 3. 默认入口：`GET /today`

响应结构：

```json
{
  "schemaVersion": "1",
  "generatedAt": "2026-08-31T02:30:00Z",
  "date": "2026-08-31",
  "status": {
    "energy": 122,
    "level": 17,
    "exp": 526,
    "title": "铁幕行者",
    "checkIn": null,
    "activeFocus": null
  },
  "focus": {
    "effectiveMinutes": 45,
    "sessionCount": 2,
    "active": null,
    "recent": [],
    "daily": [
      { "date": "2026-08-31", "effectiveMinutes": 45 }
    ]
  },
  "tasks": {
    "completed": 2,
    "remaining": 3,
    "items": []
  },
  "sleep": null,
  "meal": null,
  "dreams": { "active": [], "goals": [], "milestones": [] },
  "rituals": { "completedToday": [], "available": [] },
  "media": {
    "watchingAnime": [],
    "playingGames": [],
    "animeSessions": [],
    "gameSessions": [],
    "items": [],
    "recentlyCompleted": []
  },
  "timeline": []
}
```

重要语义：

- `status.checkIn` 是全局最近一次 Check-in，未必发生在今天；`life.checkIn` 同样取最近一次状态。
- `sleep` 是在今天结束边界前最近一次醒来的睡眠，可来自前一晚。
- `meal` 是今天时间最新的一餐。
- `focus.effectiveMinutes` 和 `sessionCount` 只统计今天；`recent` 最多 30 条；`daily` 固定覆盖最近 7 个自然日。
- `tasks` 同时包含每日任务与特别行动，使用 `special` 区分。
- `timeline` 只含今天最新 10 条，按 `occurredAt` 倒序。
- 称号 `title` 已解析为展示名称，例如 `铁幕行者`，不是内部 ID `iron_walker`。

## 4. 分域响应

### `GET /recent?days=7`

```json
{
  "schemaVersion": "1",
  "generatedAt": "2026-08-31T02:30:00Z",
  "startDate": "2026-08-25",
  "endDate": "2026-08-31",
  "days": 7,
  "lifeEventCount": 18,
  "focusMinutes": 320,
  "tasksCompleted": 9,
  "timeline": []
}
```

`timeline` 当前最多返回 100 条，因此 `lifeEventCount` 是该窗口中实际返回的条目数，不应当作无限历史总数。

### `/status`

返回 `{ schemaVersion, generatedAt, status }`。`status` 字段见第 5 节。

### `/focus`

返回 `{ schemaVersion, generatedAt, date, focus }`。其中 `recent` 最多 30 条，`daily` 为最近 7 日。

### `/tasks`

返回 `{ schemaVersion, generatedAt, date, tasks }`。任务的方向关联字段可以为 `null`。

### `/dreams`

返回 `{ schemaVersion, generatedAt, dreams }`。只返回状态为 `ACTIVE` 的 Dream、其 `ACTIVE` Goal，以及这些 Goal 下的 Milestone。

### `/life`

返回 `{ schemaVersion, generatedAt, date, life }`：

- `sleep`：今天结束边界前最近一次睡眠
- `meals`：今天全部饮食，时间倒序
- `exercise`：全局最近一次运动
- `checkIn`：全局最近一次 Check-in
- `records`：今天的通用生活记录，时间倒序

### `/journal?limit=20`

返回 `{ schemaVersion, generatedAt, entries, timeline }`。`entries` 按 `occurredAt` 倒序；`timeline` 是不限定来源的最近足迹，不等同于仅日记事件。

### `/media`

返回 `{ schemaVersion, generatedAt, media }`：在看 Anime、在玩 Game、最新 10 条 Anime/Game Session、进行中的通用媒体，以及最近完成的 10 部 Anime/Game。

### `/growth`

返回 `{ schemaVersion, generatedAt, growth }`。`recentEvents` 最多 20 条，`snapshots` 最多 7 条。

## 5. 公共数据类型

以下使用简化 TypeScript 表达 JSON 契约；`Instant`、`LocalDate` 都以字符串传输。

```ts
type Instant = string;
type LocalDate = string;

interface Status {
  energy: number;
  level: number;
  exp: number;
  title: string;
  checkIn: CheckIn | null;
  activeFocus: FocusSession | null;
}

interface Focus {
  effectiveMinutes: number;
  sessionCount: number;
  active: FocusSession | null;
  recent: FocusSession[];
  daily: Array<{ date: LocalDate; effectiveMinutes: number }>;
}

interface FocusSession {
  id: string;
  mode: "IRON_CURTAIN" | "POMODORO" | "FREE";
  status: "RUNNING" | "PAUSED" | "COMPLETED" | "INTERRUPTED";
  title: string;
  taskId: string | null;
  startedAt: Instant;
  endedAt: Instant | null;
  plannedMinutes: number | null;
  actualSeconds: number;
  actualMinutes: number;
  effectiveSeconds: number;
  effectiveMinutes: number;
  note: string;
  updatedAt: Instant;
  relatedTaskIds: string[];
  segments: object[];
  breakMinutes: number;
}

interface Tasks {
  completed: number;
  remaining: number;
  items: Array<{
    id: string;
    name: string;
    completed: boolean;
    special: boolean;
    dreamId: string | null;
    goalId: string | null;
    milestoneId: string | null;
  }>;
}

interface CheckIn {
  id: string;
  energy: number;
  mood: number;
  focusDesire: number;
  fatigue: number;
  time: Instant;
  note: string;
  images: string[];
  createdAt: Instant;
  updatedAt: Instant;
}

interface TimelineItem {
  eventId: string;
  type: string;
  source: string;
  sourceId: string;
  occurredAt: Instant;
  title: string;
  summary: string;
  tags: string[];
  media: string[];
  metadata: Record<string, unknown>;
}
```

其余分域对象直接沿用 Life HUD 的公开业务记录字段：

| 对象 | 关键字段 |
| --- | --- |
| `SleepRecord` | `id, sleepTime, wakeTime, durationMinutes, quality, type, note, images, createdAt, updatedAt` |
| `MealRecord` | `id, mealType, time, description, satisfaction, note, images, createdAt, updatedAt` |
| `ExerciseRecord` | `id, type, startTime, durationMinutes, intensity, note, images, createdAt, updatedAt` |
| `LifeRecord` | `id, type, value, unit, time, note, metadata, images, createdAt, updatedAt` |
| `Dream` | `id, title, description, meaning, status, targetDate, coverImagePath, note, createdAt, updatedAt` |
| `Goal` | `id, dreamId, title, description, status, targetDate, sortOrder, createdAt, updatedAt` |
| `DreamMilestone` | `id, goalId, title, description, status, targetDate, completedAt, sortOrder, createdAt, updatedAt` |
| `Ritual` | `id, name, description, category, triggerTime, enabled, createdAt, updatedAt` |
| `RitualExecution` | `id, ritualId, ritualName, startedAt, completedAt, status, note, stepResults, createdAt, updatedAt` |
| `Anime` | `id, title, totalEpisodes, currentEpisode, status, score, startedAt, finishedAt, coverImage, note, createdAt, updatedAt` |
| `MediaGame` | `id, title, platform, status, basePlayTimeMinutes, totalPlayTimeMinutes, startedAt, finishedAt, score, coverImage, note, createdAt, updatedAt` |
| `AnimeWatchSession` | `id, animeId, episodeStart, episodeEnd, watchedAt, durationMinutes, note, createdAt, updatedAt` |
| `MediaGameSession` | `id, gameId, startTime, endTime, durationMinutes, progress, note, createdAt, updatedAt` |
| `MediaItem` | `id, type, title, status, score, startedAt, finishedAt, coverImage, note, createdAt, updatedAt` |
| `JournalEntry` | `id, content, occurredAt, images, tags, createdAt, updatedAt` |
| `GrowthEventRecord` | `eventId, processedAt, expDelta, energyDelta, levelBefore, levelAfter, result, unlockedAchievements, unlockedTitles` |
| `GrowthSnapshot` | `date, level, totalExp, energy, totalFocusMinutes, totalEffectiveFocusMinutes, totalTaskCompleted, lifeEventCount, achievementCount, milestoneCount, createdAt` |

`LifeRecord.type` 当前可能为：`WATER`、`CAFFEINE`、`ALCOHOL`、`SUNLIGHT`、`SOCIAL`、`BODY_STATUS`、`OUTDOOR`、`CUSTOM`。媒体状态通常使用 `PLANNED / WATCHING|PLAYING|IN_PROGRESS / PAUSED / COMPLETED / DROPPED`。

## 6. 错误处理

参数越界返回 HTTP `400`：

```json
{
  "status": 400,
  "detail": "days 必须在 1 到 30 之间"
}
```

外部 Agent 应：

1. 检查 HTTP 状态码后再解析业务字段。
2. 对 `400` 修正参数，不要原样重试。
3. 对网络错误或 `5xx` 使用有限次数指数退避。
4. 把未知 JSON 字段忽略，以便兼容 schema `1` 内的向后兼容扩展。
5. 不要根据显示文案反推内部 ID；使用响应中的 `id` 和关联字段。

## 7. 给 Agent 的使用建议

- 生成“今天该做什么”时，优先使用 `/today`，按 `status → activeFocus → tasks → dreams → life` 的顺序理解上下文。
- 做周回顾时使用 `/recent?days=7`，需要原始日记正文时再补读 `/journal`。
- 不要把 `null` 当异常；它表示用户尚未记录或当前没有活动项。
- Check-in 是用户主动记录的状态快照，不是实时传感器数据。
- `timeline.metadata` 是开放结构，只把已认识的 key 当作增强信息，核心逻辑应依赖稳定顶层字段。
- 当前接口仅提供事实读取。需要代替用户写入数据时，应对接对应业务 API，并在执行前获得明确授权。

## 8. 维护规则

修改 `AgentContextController`、`AgentContext` DTO 或 `AgentContextService` 的以下内容时，必须同步更新本文档：

- 新增、删除或重命名端点
- 查询参数及范围变化
- 顶层或嵌套字段变化
- 日期、排序、条数上限或空值语义变化
- `schemaVersion` 变化

破坏兼容性的字段变更必须提升 `schemaVersion`；新增可选字段可以保留当前版本，但外部消费者仍应忽略未知字段。

实现依据：

- `src/main/java/io/github/aomckin/lifehud/controller/AgentContextController.java`
- `src/main/java/io/github/aomckin/lifehud/dto/agent/AgentContext.java`
- `src/main/java/io/github/aomckin/lifehud/service/AgentContextService.java`
- `src/test/java/io/github/aomckin/lifehud/controller/AgentContextControllerTest.java`
