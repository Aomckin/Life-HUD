# Life HUD v0.7 总开发任务书
## Media / 宅宅生活档案

> 版本：v0.7.0
> 基线：v0.6.2
> 主题：Media / 宅宅生活档案
> 核心目标：让 Life HUD 不只知道“今天娱乐了多久”，还知道**是什么作品，在什么阶段陪伴过自己，以及一次次观看 / 游玩的具体足迹**。

---

# 0. 开发前提

v0.7 必须建立在当前 v0.6.2 的事实层之上，不另起一套时间线逻辑。

当前已有能力应直接复用：

- Java 21 / Spring Boot 3
- JSON Repository 持久化
- `LifeEvent`
- `LifeFactRecorder`
- `TimelineService`
- `/api/images`
- Summer Sky Shell
- 统一 Modal / Toast / Empty / Confirm
- `LifeEventSourceType.MEDIA`

本版本的核心数据链应保持：

```text
作品档案
  ↓
消费 Session
  ↓
LifeEvent
  ↓
Journal / Life Timeline
  ↓
未来 v0.8 Growth / Agent Context
```

---

# 1. 必须先处理的现有代码边界

## 1.1 不要和现有 `GameController` 撞名

当前仓库已经存在：

```text
GameController
GameCommandFacade
/state
/command
```

这里的 `Game` 指旧有 Life HUD 游戏化命令入口，不是“电子游戏档案”。

因此 v0.7 禁止：

```text
新增另一个 GameController
重命名旧 GameController
为了 Media 改坏 /state 或 /command
```

Media 游戏模块建议命名：

```text
MediaGame
MediaGameSession

MediaGameController
MediaGameService
MediaGameRepository
```

API 使用：

```text
/api/media/games
```

避免概念冲突。

---

## 1.2 不要把 Media Session 和 EntertainmentRecord 混成一个东西

当前已有：

```text
EntertainmentRecord
/api/entertainment
```

它属于 v0.4 Growth 的 Energy SPEND 入口，主要回答：

> 这次娱乐消耗了多少 Energy？

v0.7 Media Session 回答：

> 我刚刚看了 / 玩了什么？

两者语义不同。

因此 v0.7：

- 不删除 `EntertainmentRecord`
- 不迁移旧娱乐记录
- 不让 Media Session 自动扣 Energy
- 不让 Media Session 自动加 EXP
- 不要求一次 Media Session 同时创建 EntertainmentRecord
- 不把 `/api/entertainment` 改成 `/api/media`

Growth 对真实 Media 行为的进一步消费留给 v0.8。

---

# 2. 产品定位

Media 不是“娱乐消费表格”。

它应该是：

> **作品档案 + 陪伴足迹。**

页面要回答：

```text
最近我在看什么？
最近我在玩什么？
一部番看到哪里了？
一个游戏玩了多久？
什么时候开始？
什么时候结束？
我对它有什么评价？
最近一次和它相处是什么时候？
```

Media 顶层类别：

```text
Anime
Game
Book
Manga
Movie
Other
```

Music 本版本不进入 Media。

音乐继续归属于：

```text
「现在。」
└── 十首歌 / 阶段歌单
```

---

# 3. 数据模型总原则

遵守 Life HUD 现有架构原则：

> 简单记录通用化，复杂业务升格独立模型。

因此：

```text
复杂：
Anime
AnimeWatchSession

MediaGame
MediaGameSession

简单：
MediaItem
(Book / Manga / Movie / Other)
```

不要为：

```text
Book
Manga
Movie
Other
```

第一版各自建立一整套 Controller / Service / Repository。

---

# 4. Anime

## 4.1 Anime 模型

建议：

```text
Anime
├── id
├── title
├── totalEpisodes
├── currentEpisode
├── status
├── score
├── startedAt
├── finishedAt
├── coverImage
├── note
├── createdAt
└── updatedAt
```

状态：

```text
PLANNED
WATCHING
PAUSED
COMPLETED
DROPPED
```

约束：

- `title` 必填
- `totalEpisodes >= 0`
- `currentEpisode >= 0`
- 已知总集数时 `currentEpisode <= totalEpisodes`
- score 可空，建议 0~10
- `COMPLETED` 时允许自动补 `finishedAt`
- 用户仍可手工纠正 currentEpisode / 状态

---

## 4.2 AnimeWatchSession

```text
AnimeWatchSession
├── id
├── animeId
├── episodeStart
├── episodeEnd
├── watchedAt
├── durationMinutes
├── note
├── createdAt
└── updatedAt
```

规则：

- animeId 必须存在
- episodeStart / episodeEnd >= 1
- episodeEnd >= episodeStart
- durationMinutes 可空；填写时必须 > 0
- `watchedAt` 默认当前时间，可补录
- 保存 Session 后，Anime 的 `currentEpisode` 至少推进到 episodeEnd
- 若 episodeEnd 达到 totalEpisodes，可将 Anime 推进到 COMPLETED 并设置 finishedAt
- 不因删除旧 Session 强行让用户观看进度倒退

Session 才是进入 Life Timeline 的主要事实。

---

# 5. Game

## 5.1 MediaGame 模型

不要命名为普通 `Game`，避免与现有游戏化命令域混淆。

```text
MediaGame
├── id
├── title
├── platform
├── status
├── totalPlayTimeMinutes
├── startedAt
├── finishedAt
├── score
├── coverImage
├── note
├── createdAt
└── updatedAt
```

建议状态：

```text
PLANNED
PLAYING
PAUSED
COMPLETED
DROPPED
```

平台先使用普通字符串：

```text
PC
Steam
PS5
Switch
Mobile
Arcade
Other
```

不要为了平台单独做复杂枚举体系。

---

## 5.2 MediaGameSession

```text
MediaGameSession
├── id
├── gameId
├── startTime
├── endTime
├── durationMinutes
├── progress
├── note
├── createdAt
└── updatedAt
```

规则：

- gameId 必须存在
- startTime / endTime 可补录
- 后端统一计算或校验 duration
- endTime 不得早于 startTime
- duration 必须为正数
- `totalPlayTimeMinutes` 必须由 Session 维护，不能出现增删 Session 后累计时长漂移

推荐实现：

```text
totalPlayTimeMinutes
=
该 Game 当前所有有效 Session duration 之和
```

若为了兼容“加入档案前已经玩了很多小时”的场景，需要支持历史基准时间，可增加：

```text
basePlayTimeMinutes
```

并使：

```text
totalPlayTimeMinutes
=
basePlayTimeMinutes
+
Session 总时长
```

不要让前端直接随意修改一个无法解释来源的累计值。

---

# 6. Book / Manga / Movie / Other

第一版只做基础档案。

统一：

```text
MediaItem
├── id
├── type
├── title
├── status
├── score
├── startedAt
├── finishedAt
├── coverImage
├── note
├── createdAt
└── updatedAt
```

类型：

```text
BOOK
MANGA
MOVIE
OTHER
```

状态可以统一：

```text
PLANNED
IN_PROGRESS
PAUSED
COMPLETED
DROPPED
```

本版本不要继续扩：

- 书籍页码追踪
- 漫画卷数 / 话数追踪
- 电影多次观看历史
- 作者 / 导演 / 出版社知识库
- 标签推荐
- 外部 Metadata 抓取

先把“档案”做好。

---

# 7. 持久化

建议文件：

```text
data/media-anime.json
data/media-anime-sessions.json

data/media-games.json
data/media-game-sessions.json

data/media-items.json
```

要求：

- 使用现有 `JsonFileStore`
- Repository 保持强类型
- 空文件 / 文件不存在时正常初始化
- 不使用 `Map<String,Object>` 代替正式领域模型
- 不修改旧娱乐 / Growth JSON 的语义

---

# 8. API

## 8.1 Anime

建议：

```text
GET    /api/media/anime
POST   /api/media/anime

GET    /api/media/anime/{id}
PUT    /api/media/anime/{id}
DELETE /api/media/anime/{id}
```

Session：

```text
GET    /api/media/anime/{animeId}/sessions
POST   /api/media/anime/{animeId}/sessions

PUT    /api/media/anime-sessions/{sessionId}
DELETE /api/media/anime-sessions/{sessionId}
```

---

## 8.2 Game

```text
GET    /api/media/games
POST   /api/media/games

GET    /api/media/games/{id}
PUT    /api/media/games/{id}
DELETE /api/media/games/{id}
```

Session：

```text
GET    /api/media/games/{gameId}/sessions
POST   /api/media/games/{gameId}/sessions

PUT    /api/media/game-sessions/{sessionId}
DELETE /api/media/game-sessions/{sessionId}
```

---

## 8.3 简单 MediaItem

```text
GET    /api/media/items
POST   /api/media/items

GET    /api/media/items/{id}
PUT    /api/media/items/{id}
DELETE /api/media/items/{id}
```

支持：

```text
?type=BOOK
?type=MANGA
?type=MOVIE
?type=OTHER
```

---

# 9. LifeEvent 联动

新增：

```text
ANIME_WATCHED
GAME_PLAYED
BOOK_READ
MANGA_READ
MOVIE_WATCHED
MEDIA_CONSUMED
```

其中 v0.7 P0 必须实际使用：

```text
ANIME_WATCHED
GAME_PLAYED
```

简单 Media 如果本版本没有 Session，可以暂时只保留事件类型，不强行制造事件。

---

## 9.1 Source

当前已经存在：

```text
LifeEventSourceType.MEDIA
```

必须复用。

Media 事件必须最终被识别为：

```text
sourceType = MEDIA
```

不要继续落成 SYSTEM。

---

## 9.2 事实同步

复用 v0.6 的模式：

```text
业务 Session
↕
LifeFactRecorder
↕
唯一 LifeEvent
```

创建：

```text
创建 Session
→ 创建一条 LifeEvent
```

编辑：

```text
编辑 Session
→ 原位 rewrite LifeEvent
→ version + 1
```

删除：

```text
删除 Session
→ 对应 LifeEvent 同步删除
```

不能出现：

- 改了 Session，Timeline 还是旧内容
- 删除 Session 后留下幽灵事件
- 编辑一次多生成一条重复事件

---

## 9.3 occurredAt

必须表示“事情实际发生的时间”。

建议：

Anime：

```text
occurredAt = watchedAt
```

Game：

```text
occurredAt = endTime
```

不要用 createdAt 代替。

---

## 9.4 Timeline 文案

Anime 示例：

```text
Anime
《幼女战记 第二季》
观看 EP03 - EP05 · 72 min
```

Game 示例：

```text
Game
《Cyberpunk 2077》
游玩 86 min · 推进主线任务
```

metadata 至少带：

```text
mediaType
mediaId
sessionId
durationMinutes
```

Anime 可增加：

```text
episodeStart
episodeEnd
```

Game 可增加：

```text
progress
platform
```

---

# 10. Journal / Timeline 接入

当前 `TimelineService.GROUPS` 必须新增：

```text
media -> MEDIA
```

Journal 前端增加：

```text
媒体
```

来源筛选。

要求：

- Media Session 创建后立即能在 Journal 看到
- 日期排序遵循 occurredAt
- 补录旧 Session 应回到正确日期
- Timeline 删除普通 LifeEvent 的现有能力不得破坏 Media 业务记录

注意：

> Timeline 手工删除事件不反向删除业务 Session。

保持 v0.6.1 已确定语义。

---

# 11. `/media` 前端

当前 `/media` 仍走 placeholder。

v0.7 必须正式新增：

```text
pages/media.js
styles/media.css
```

并接入：

```text
app-v02.js
api/client.js
```

---

# 12. Media 页面产品结构

不要做成三张 CRUD 表格。

建议结构：

```text
Media / 宅宅生活档案

┌ 最近正在陪伴我的作品 ──────────────┐
│ 当前 Anime / 当前 Game           │
└─────────────────────────────────┘

[全部] [番剧] [游戏] [书] [漫画] [电影] [其他]

作品档案卡片墙
```

右上：

```text
+ 加入作品
```

---

## 12.1 作品卡片

卡片建议展示：

```text
封面
标题
类别
状态
进度
评分
最近一次活动
```

Anime：

```text
EP 7 / 12
```

Game：

```text
23h 42min
Steam · PLAYING
```

Book / Manga / Movie：

```text
状态 + 评分
```

不要一张卡塞满全部 metadata。

---

## 12.2 详情态

点击作品卡进入详情层 / Drawer / Modal / 页面内展开均可。

必须能够看到：

```text
作品信息
当前进度
备注
Session 历史
```

Anime：

```text
最近观看
EP 7-8 · 48 min · 8月30日
EP 5-6 · 51 min · 8月28日
```

Game：

```text
最近游玩
92 min · 推进第三章
41 min · 刷分
```

底部：

```text
+ 记录观看
+ 记录游玩
```

---

# 13. 快速记录 Session

## Anime

Modal 至少：

```text
起始集数
结束集数
观看时间
时长（可选）
一句备注
```

默认值应尽量减少输入：

```text
episodeStart = currentEpisode + 1
episodeEnd   = currentEpisode + 1
watchedAt    = now
```

---

## Game

Modal 至少：

```text
开始 / 结束时间
或明确可推导的时长输入
进度
备注
```

不要在 v0.7 做运行中的实时计时状态机。

Media Game Session 是记录，不是 Focus Timer。

---

# 14. 封面图片

封面属于高收益、低复杂度能力，建议纳入 v0.7。

直接复用：

```text
POST /api/images
```

支持：

- Anime 封面
- Game 封面
- MediaItem 封面

要求：

- 可为空
- 编辑时可替换
- 无封面时使用统一占位
- 不新增第二套上传系统

本版本不做：

- Bangumi 自动搜图
- Steam API 抓封面
- 豆瓣 / TMDB 抓取
- 网络 URL 自动下载

---

# 15. 与「现在。」的关系

v0.7 Media 和 `/now` 不做自动双向同步。

原因：

```text
Media = 长期作品档案
Now   = 当前阶段快照
```

例如：

- Media 中可以有 100 部番
- 「现在。」只挑当前阶段最有意义的几部

因此禁止：

```text
看番 Session 自动改 Now
游戏 Session 自动加入 Now
Media 删除作品自动删除 Now 项目
```

未来如需联动，显式设计“加入现在。”按钮，再单独开发。

---

# 16. 与 Growth 的关系

v0.7 不改现有成长结算规则。

禁止：

```text
GAME_PLAYED -> 自动扣 Energy
ANIME_WATCHED -> 自动扣 Energy
GAME_PLAYED -> 自动加 EXP
```

当前：

```text
EntertainmentRecord
```

仍是 Growth 的娱乐 SPEND 入口。

v0.8 再决定真实 Media 行为如何参与 Growth。

---

# 17. API Client

在现有 `api/client.js` 增加独立：

```text
api.media
```

建议：

```text
api.media.anime
api.media.animeSessions

api.media.games
api.media.gameSessions

api.media.items
```

不要把 Media API 塞进：

```text
api.entertainment
```

两者保持边界。

---

# 18. 文案与视觉

延续 Summer Sky。

Media 是“生活档案”，视觉上应比 Tasks 更松弛，比 Journal 更像陈列。

关键词：

```text
作品墙
档案
封面
陪伴
进度
最近足迹
```

避免：

```text
后台表格
库存管理
ERP
数据库管理页
```

页面可以有一定“收藏柜 / 书架”气质，但不要引入与现有 Shell 冲突的新主题。

---

# 19. 测试

至少新增：

## Anime

- [ ] 创建 / 编辑 / 删除 Anime
- [ ] status 解析与校验
- [ ] currentEpisode 不能非法超过 totalEpisodes
- [ ] 创建 Session 自动推进 currentEpisode
- [ ] 到最终集时完成状态正确
- [ ] 补录 watchedAt 正确

## Game

- [ ] 创建 / 编辑 / 删除 MediaGame
- [ ] 不与现有 `GameController` 冲突
- [ ] Session duration 校验
- [ ] 创建 Session 后累计时长正确
- [ ] 编辑 Session 后累计时长重新正确
- [ ] 删除 Session 后累计时长重新正确

## LifeEvent

- [ ] Anime Session 创建只有一个事件
- [ ] Game Session 创建只有一个事件
- [ ] 编辑 Session 原位更新事件
- [ ] 删除 Session 无幽灵
- [ ] sourceType 为 MEDIA
- [ ] occurredAt 为真实消费时间

## Timeline

- [ ] `sources=media` 能过滤
- [ ] Journal “媒体”筛选可用
- [ ] 补录旧 Session 排在正确日期
- [ ] 普通事件排序无回归

## 回归

- [ ] `/state`
- [ ] `/command`
- [ ] Growth 娱乐 SPEND
- [ ] Focus
- [ ] Life
- [ ] Ritual
- [ ] Now
- [ ] Journal
- [ ] Dreams / Tasks

全部不得因 Media 引入而破坏。

---

# 20. 前端验收

桌面端：

- [ ] `/media` 不再显示 placeholder
- [ ] Anime / Game / Book / Manga / Movie / Other 能切换
- [ ] 新建作品正常
- [ ] 编辑作品正常
- [ ] 删除有确认
- [ ] Anime Session 可快速记录
- [ ] Game Session 可快速记录
- [ ] 详情能看到 Session 历史
- [ ] 空状态自然
- [ ] Console 0 error

窄屏：

- [ ] 390px 不横向溢出
- [ ] 卡片自动单列 / 合理换行
- [ ] Modal 可完整操作
- [ ] Session 历史不挤坏布局

---

# 21. 不要在 v0.7 做

严格不扩：

- 外部媒体数据库 API
- Bangumi / Steam / TMDB / 豆瓣同步
- 自动识别作品
- 游戏运行自动检测
- Steam 游戏时长同步
- Anime 自动追番
- 音乐历史统计
- 推荐系统
- AI 评分分析
- Agent
- 复杂标签系统
- 社交分享
- 多用户
- Media 自动 Growth 结算
- Media 自动修改「现在。」

这些全部留给以后。

---

# 22. P0 / P1

## P0

必须完成：

```text
Anime 档案
AnimeWatchSession

MediaGame 档案
MediaGameSession

Book / Manga / Movie 基础档案

LifeEvent
Timeline Media 分组
/media 正式页面
```

## P1

尽量完成：

```text
作品封面
Other
当前作品 Hero
最近一次活动
更舒服的详情档案布局
```

---

# 23. 完成定义

v0.7 完成后必须走通：

```text
加入一部番
↓
记录观看 EP01 - EP03
↓
Anime 进度推进
↓
生成 ANIME_WATCHED
↓
Journal 当天出现观看记录
```

以及：

```text
加入一个游戏
↓
记录一次游玩 Session
↓
累计游玩时间更新
↓
生成 GAME_PLAYED
↓
Journal 出现游戏足迹
```

以及：

```text
加入一本书 / 一部电影 / 一部漫画
↓
形成长期作品档案
```

到此结束 v0.7。

---

# 24. 版本收尾

完成后：

- 版本号更新为 `v0.7.0`
- 静态资源缓存标识统一升级
- README 增加 v0.7 Media 概览
- CODEBASE_STATUS 增加完整实现边界
- 测试数量与结果写入 CODEBASE_STATUS
- 不把 v0.8 内容提前塞进来

最终一句：

> **Media 记录的不是“消耗了多少娱乐时间”，而是“什么作品在什么时候陪伴过自己”。**
