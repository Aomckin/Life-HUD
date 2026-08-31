# Agent Action API

这是受信任工具操作 Life HUD 时使用的 v1.0 契约。本文记录现有业务 API，不会另建一套平行的 `/api/agent/actions` 接口。

权限含义：`READ` 查询事实，`WRITE` 创建或修改事实，`DESTRUCTIVE` 删除事实，`ADMIN` 修改派生状态或系统状态。下文中的“幂等”表示重复执行同一个状态转换不会重复生成 `LifeEvent` 或重复进行 Growth 结算；普通创建操作默认不幂等，除非调用方自行阻止重复请求。

## 通用行为

- JSON 请求使用 `Content-Type: application/json`；文件上传使用 `multipart/form-data`。
- `Instant` 使用带时区偏移或 `Z` 的 ISO-8601 时间戳；日期使用 `YYYY-MM-DD`。
- 省略可选字段时使用服务默认值。更新 Life 记录时，`images: null` 表示保留当前图片，显式传入数组则替换图片。
- 错误响应包含 `status`、`detail`、`path` 和 `timestamp`，不会返回堆栈信息。
- 创建操作返回新对象，更新与状态转换返回当前状态；除非另有说明，删除成功不返回响应体。

## Life 与 Journal

| 操作 | 方法与路径 | 权限 | 可逆性 / 幂等性 | 时间与事件 |
| --- | --- | --- | --- | --- |
| 查询/创建睡眠 | `GET/POST /api/life/sleep` | READ/WRITE | 创建不幂等 | 请求体：`sleepTime,wakeTime,quality,type,note,images`；事件发生于 `wakeTime` |
| 获取/更新/删除睡眠 | `GET/PUT/DELETE /api/life/sleep/{id}` | READ/WRITE/DESTRUCTIVE | 更新改写同一事件；删除同时移除事件 | `SLEEP_RECORDED` |
| 查询/创建饮食 | `GET/POST /api/life/meals` | READ/WRITE | 创建不幂等 | `mealType,time,description,satisfaction,note,images`；发生于 `time` |
| 获取/更新/删除饮食 | `GET/PUT/DELETE /api/life/meals/{id}` | READ/WRITE/DESTRUCTIVE | 使用稳定来源 ID | `MEAL_RECORDED` |
| 查询/创建运动 | `GET/POST /api/life/exercises` | READ/WRITE | 创建不幂等 | `type,startTime,durationMinutes,intensity,note,images`；发生于 `startTime` |
| 获取/更新/删除运动 | `GET/PUT/DELETE /api/life/exercises/{id}` | READ/WRITE/DESTRUCTIVE | 使用稳定来源 ID | `EXERCISE_RECORDED` |
| 查询/创建状态记录 | `GET/POST /api/life/check-ins` | READ/WRITE | 创建不幂等 | `energy,mood,focusDesire,fatigue,time,note,images`；发生于 `time` |
| 获取/更新/删除状态记录 | `GET/PUT/DELETE /api/life/check-ins/{id}` | READ/WRITE/DESTRUCTIVE | 使用稳定来源 ID | `CHECK_IN_RECORDED` |
| 查询/创建通用记录 | `GET/POST /api/life/records` | READ/WRITE | 创建不幂等 | `type,value,unit,time,label,note,metadata,images`；发生于 `time` |
| 获取/更新/删除通用记录 | `GET/PUT/DELETE /api/life/records/{id}` | READ/WRITE/DESTRUCTIVE | 使用稳定来源 ID | `LIFE_RECORDED` |
| 查询/创建日记 | `GET/POST /api/journal` | READ/WRITE | 创建不幂等 | `content,occurredAt,images,tags`；发生于 `occurredAt` |
| 获取/更新/删除日记 | `GET/PUT/DELETE /api/journal/{id}` | READ/WRITE/DESTRUCTIVE | 更新改写事件；删除清理事件 | `JOURNAL_WRITTEN` |

## 任务与方向关联

任务定义位于 `/api/task-pool`。`GET` 返回 `{daily,special}`；创建和更新请求体使用 `{name,energy,exp}`，其中特殊任务忽略 `energy`。启用接口接受 `{enabled:true|false}`。

| 操作 | 方法与路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| 读取任务池 | `GET /api/task-pool` | READ | 返回全部任务定义 |
| 创建/更新/删除每日任务 | `POST /api/task-pool/daily`、`PUT/DELETE /api/task-pool/daily/{id}` | WRITE/DESTRUCTIVE | 删除任务定义属于破坏性操作 |
| 启用每日任务 | `POST /api/task-pool/daily/{id}/enabled` | WRITE | 设置任务状态 |
| 创建/更新/删除/启用特殊任务 | `/special` 下的对应路径 | WRITE/DESTRUCTIVE | 语义相同 |
| 查询今日任务方向 | `GET /api/task-directions` | READ | 返回当前有效的每日任务与特殊任务实例 |
| 完成今日任务 | `POST /api/task-directions/{source}/{taskId}/complete` | WRITE | 完成操作幂等，不得重复结算 Energy/Growth |
| 设置/移除方向关联 | `PUT/DELETE /api/task-directions/{source}/{taskId}` | WRITE | 将任务关联到 Dream、Goal 或 Milestone |

## Dreams

Dream 请求体使用 `title,description,meaning,status,targetDate,coverImagePath,note`；Goal 使用 `title,description,status,targetDate,sortOrder`；Milestone 使用对应字段。

| 操作 | 方法与路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| 查询/创建/详情/更新 Dream | `GET/POST /api/dreams`、`GET/PUT /api/dreams/{id}` | READ/WRITE | 创建返回 `201` |
| 归档 Dream | `DELETE /api/dreams/{id}` | WRITE | 可通过更新或恢复撤销；返回归档后的 Dream |
| 彻底删除 Dream | `DELETE /api/dreams/{id}/purge` | DESTRUCTIVE | 永久级联删除，返回 `204` |
| 完成/暂停/恢复 | `POST /api/dreams/{id}/{complete|pause|resume}` | WRITE | 状态转换 |
| 创建/更新/完成/删除 Goal | `POST /api/dreams/{dreamId}/goals`；`PUT/POST/DELETE /api/goals/{id}[ /complete]` | WRITE/DESTRUCTIVE | 删除不可恢复 |
| 创建/更新/完成/删除 Milestone | `POST /api/goals/{goalId}/milestones`；`PUT/POST/DELETE /api/dream-milestones/{id}[ /complete]` | WRITE/DESTRUCTIVE | 完成属于状态设置操作 |

## Ritual

仪式定义使用 `/api/rituals`，仪式执行是独立的状态机。

| 操作 | 方法与路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| 查询/详情/创建/更新/删除 | `GET/POST /api/rituals`、`GET/PUT/DELETE /api/rituals/{id}` | READ/WRITE/DESTRUCTIVE | 详情包含仪式片段 |
| 启用/停用 | `POST /api/rituals/{id}/{enable|disable}` | WRITE | 仅修改仪式定义状态 |
| 开始执行 | `POST /api/rituals/{id}/start` | WRITE | 创建一次执行记录 |
| 读取执行 | `GET /api/ritual-executions/{id}` | READ | 返回定义与执行进度视图 |
| 记录片段 | `POST /api/ritual-executions/{id}/steps/{stepId}` | WRITE | 只更新本次执行 |
| 完成/取消 | `POST /api/ritual-executions/{id}/{complete|cancel}` | WRITE | 终态转换；生成既定 Ritual 事实 |

## Media

番剧使用 `/api/media/anime`，游戏使用 `/api/media/games`，通用书籍、漫画、电影及其他作品使用 `/api/media/items`。三类接口均按常规集合/单项路径支持查询列表、获取详情、创建、更新和删除。创建属于 `WRITE`，删除属于 `DESTRUCTIVE`。

番剧 Session 使用 `POST /api/media/anime/{id}/sessions`、`GET /api/media/anime/{id}/sessions` 和 `PUT/DELETE /api/media/anime-sessions/{sessionId}`。游戏 Session 使用 `games` 与 `game-sessions` 下的相同模式。创建、更新或删除 Session 时，系统始终维护且只维护一条 `ANIME_WATCHED` 或 `GAME_PLAYED` 事件；事件发生时间为 `watchedAt` 或 `endTime`。

## Focus

| 操作 | 方法与路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| 当前/今日/历史 | `GET /api/focus/current`、`/today`、`/history?limit=30` | READ | 当前专注可能为 `null` |
| 开始 | `POST /api/focus/start` | WRITE | 请求体：`mode,title,taskId,plannedMinutes,...`；已有活动 Session 时返回冲突 |
| 暂停/恢复 | `POST /api/focus/{id}/{pause|resume}` | WRITE | 状态转换；状态无效时返回 `409` |
| 切换/更新 Segment | `POST /api/focus/{id}/segments/switch`；`PATCH /api/focus/{id}/segments/{segmentId}` | WRITE | Segment 类型为 `FOCUS`、`BREAK` 或 `INTERRUPTION` |
| 完成/中断 | `POST /api/focus/{id}/{complete|interrupt}` | WRITE | 可传 `{note}`；进入终态且不会重复结算 Growth |
| 手动补录 | `POST /api/focus/manual` | WRITE | 使用请求中提供的时间区间；不幂等 |
| 删除历史 | `DELETE /api/focus/{id}` | DESTRUCTIVE | 不能删除活动 Session；同时移除时间线事实 |

## Growth 与文件上传

Growth 是派生数据。可通过 `GET /api/growth`、`/history`、`/energy-history` 和 `/snapshots` 读取。`POST /api/growth/energy/spend` 属于 `WRITE`，表示一次真实的娱乐消耗。`POST /api/growth/energy/adjust` 与 `/recalculate` 属于 `ADMIN`；普通 Agent 不得直接设置 EXP、Level、成就、称号，也不得伪造 Growth 事件。

图片通过 `POST /api/images` 上传，multipart 字段名为 `file`。响应包含公开的 `/uploads/...` 路径。系统只接受图片内容，配置上限为 64 MB。上传属于 `WRITE`；调用方随后将返回路径附加到业务记录。上传文件按 SHA-256 内容去重：即使文件名不同，只要文件本体相同，就会复用已有路径。

## 失败与重试规则

- 不要原样重试 `400`、`404` 或 `409`。
- 网络错误与 `5xx` 只应进行少量退避重试。
- 不要盲目重试创建请求：创建操作通常不幂等。
- 写入后重新读取对象或相关 Context 视图，确认操作结果。
- 调用 `DESTRUCTIVE` 或 `ADMIN` 接口前，必须获得用户明确确认。
