# Life HUD v0.4 · Growth 实现说明

## 架构与事实边界

```text
FocusSession / Task
        ↓
    LifeEvent
        ↓ eventId
   GrowthEngine
        ├─ GrowthEventRecord
        ├─ GrowthStats（增量统计）
        ├─ EnergyRecord
        ├─ EXP / Level
        ├─ Achievement / Title
        └─ derived LifeEvent
```

FocusSession 仍是专注时间的事实，Task 文件仍是任务完成事实，LifeEvent 表达发生了什么。Growth 只从 LifeEvent 派生状态；Focus 与 Task 不再直接增加 EXP。`GrowthRules` 是当前唯一计算入口，使用 Focus 的 `effectiveSeconds`，忽略 BREAK、INTERRUPTION 与 PAUSE 的自然跨度。

## 核心循环（v0.4.2 修订）

Energy 是短周期、快速变化的生活状态量；EXP 是 Energy 完成"获得 → 消费"生活循环后留下的长期累计；Level 是更长期的成长刻度。Energy 变化速度必须远大于 EXP：

```text
建设性活动（Focus / Task / 学习 / 项目 ...）
      → LifeEvent
      → Energy EARN
娱乐与生活消费（游戏 / 看番 / 电影 ...）
      → EnergyLedgerService.spend()
      → Energy SPEND（GrowthEngine 结算，只计实际消耗）
      → 每 10 点实际消耗沉淀 +1 EXP（整数余数池）
      → Level
```

- `FOCUS_FINISHED` 不产生 EXP，只按 `effectiveMinutes` 结算 Energy：每 3 有效分钟 +1，单事件上限 +80（约 4h）。超出上限的部分 FocusSession、effectiveMinutes、Achievement 与 GrowthSnapshot 仍完整记录，只是不再产 Energy。
- Energy 满值时 EARN 的溢出直接消失，不转换为 EXP：Energy = 178 时 60min Focus 请求 +20，实际 +2，EXP +0。
- `TASK_COMPLETED` 只产生 Energy（0～20），不产生 EXP；普通任务用自身 `baseEnergy`，特殊任务以旧 `exp` 值（clamp 0～20）作为 Energy 产出。
- 旧 `baseExp` 字段已废弃：任务 JSON 兼容读取，但新 Growth 规则不读取、新 UI 不展示、新事件不写入。
- Energy 变化类型 `EnergyChangeType`：EARN（建设性活动）、SPEND（真实消费，进入 EXP 转换）、DECAY（预留，当前无定时衰减业务）、ADJUST（数据修正）。DECAY / ADJUST 不产生 EXP 也不触碰余数池。
- EXP 只按 `actualEnergySpent` 结算：Energy = 30 时请求 SPEND 50，只有 30 进入转换池。
- 转换余数持久化在 `save.json` 的 `exp_conversion_remainder`（0 ≤ remainder < energy_per_exp）：`pool = oldRemainder + actualSpent`，`exp = pool / energy_per_exp`，`newRemainder = pool % energy_per_exp`，全程整数运算；小额消费（如 7 点）不会被吃掉。
- Energy 上限保持 0～180（来自 `config.json` 的 `max_energy`，不在 growth.json 重复配置）。
- **新的一天 Energy 向基准值回归**：每日首次进入应用时，多余或不足基准值的部分 × `day_start_factor`（0.75）作为当天起点，即 `新Energy = midpoint + (Energy − midpoint) × 0.75`（如 180 → 158、0 → 23）。它按 ADJUST 记入 Energy History（原因"新的一天，Energy 向基准值 90 回归"），不产生 EXP 也不触碰余数池；每天最多一次，标记存于 `save.json` 的 `energy_drift_date`，基准值与倍率在 `data/content/energy-drift.json` 调整。
- `GrowthEngine` 仍是唯一结算入口，按 eventId 幂等；同一 SPEND 事件重放只扣一次 Energy、推进一次余数、发一次 EXP。
- Achievement 由 Focus 时长、任务数等统计指标直接推进，解锁不发 EXP 也不发 Energy；Title 与 Milestone 不参与任何数值。

## 数值配置

全部集中在 `data/growth.json`，每个概念只配置一次：

```json
{
  "focus_minutes_per_energy": 3,
  "focus_energy_event_cap": 80,
  "energy_per_exp": 10,
  "task_energy_cap": 20,
  "overview_history_limit": 12,
  "trend_days": 7
}
```

旧字段 `exp_per_energy` 首次读取时自动换算为 `energy_per_exp`（`round(1 / 旧值)`）并回写新格式，不会导致启动失败。Level 曲线本轮保持不变（`level.json`），待真实使用后再单独调整。

## 内容层（data/content/）

成就、称号、文案与应用元信息是内容，不是代码。它们存放在专门的 `data/content/` 文件夹（打包默认在 `src/main/resources/data/content/`，首次启动播种到数据目录），修改后重启即可生效，无需改代码：

| 文件 | 内容 |
|---|---|
| `growth-achievements.json` | 成就目录：id、名称、描述、分类、hidden、图标、条件（`GrowthConditionType` + `GrowthMetric` + target）与关联称号 |
| `growth-titles.json` | 称号目录：id、名称、描述、来源、hidden、custom |
| `growth-copy.json` | 成长文案：结算 reason、Level Up / 成就 / 称号派生事件措辞与标签、旧版迁移兜底文案、停用命令提示、任务日志模板、里程碑默认分类 |
| `app.json` | 应用名与版本号（`/api/growth` 的 `version` 字段来源） |
| `energy-drift.json` | 新一天 Energy 回归参数：`midpoint`（基准值，默认 90）与 `day_start_factor`（倍率，默认 0.75） |

`GrowthCatalog` 负责加载成就/称号目录，`GrowthCopy` 负责文案，`AppInfo` 负责应用元信息；缺少内容文件会在启动时快速失败。前端对应的内容模块是 `static/content/copy.js`（模式标签、表单长度上限、共享文案）。

## 统一 Energy 接口

`EnergyLedgerService` 为后续 Media / Life / Ritual 等模块提供统一入口，所有变化都以 LifeEvent 落盘并由 GrowthEngine 结算：

- `spend(EnergySpendRequest)`：真实消费，SPEND 类型，EXP 由引擎按实际消耗结算。
- `adjust(EnergyAdjustRequest)`：手动修正，ADJUST 类型，不产生 EXP。
- `decay(amount, reason)`：自然衰减入口（供未来调度器使用），DECAY 类型，不产生 EXP。

REST 暴露 `POST /api/growth/energy/spend` 与 `POST /api/growth/energy/adjust`，返回 `{eventId, requestedDelta, actualDelta, expGained}` 回执。没有商店或虚假消费入口；Shop / Coin 保持停用状态。

## 娱乐记录（Entertainment，v0.4 封版）

`EntertainmentRecordService` 是 Energy 消费的真实生活入口，让 SPEND → EXP 链路跑通：

- **模型**：`EntertainmentRecord`（id、category、title、durationMinutes、energyCost、note、occurredAt、lifeEventId、createdAt、updatedAt），存于 `entertainment-records.json`。
- **分类**：GAME / ANIME / MOVIE / VIDEO / SOCIAL / OUTING / OTHER，中文标签在前端 `static/content/copy.js`。刻意保持轻量，未来 Media 模块逐步接管。
- **Energy 消耗手动确认**：`durationMinutes` 记录事实，`energyCost` 由用户填写，不做"每小时固定消耗"的自动算法。
- **调用链**（严格复用统一台账，无第二套消费逻辑）：记录事实 LifeEvent（`ENTERTAINMENT_RECORDED`，metadata 含 recordId / category / durationMinutes / energyCost）→ `EnergyLedgerService.spend()` → GrowthEngine 按 actualEnergySpent 结算。
- **Energy 不足**：照常记录现实。请求 20、当前 8 → 扣 8、按 8 结算 EXP；记录保留用户填写的 20，Energy History 记录实际 -8。两个概念不混淆。
- **编辑**：title / category / durationMinutes / note 可改；已结算的 energyCost 与 occurredAt 不可改，填错走 Energy Adjust 或删除重录。
- **删除**：只删事实记录，Energy / EXP 历史不自动回滚，账目可信度优先。
- **API**：`GET|POST /api/entertainment`、`PATCH|DELETE /api/entertainment/{id}`。
- Growth 页面提供"＋ 记录娱乐"快速入口、最近娱乐（3~5 条）与 Energy History（时间 / 变化 / 类型 / 原因，ADJUST 视觉弱化）；不做第五个巨型 Tab。Snapshot 的 entertainmentMinutes / energySpent 扩展留给 v0.5。

## v0.4 封版

最终核心循环：建设性活动 → Energy EARN；娱乐性活动 → Energy SPEND → EXP → Level；Achievement / Milestone / Title 只记录生活成长，不参与数值奖励。三个尺度：Energy 看今天，EXP 看一段时间，Level 看很久以后。

## 持久化状态

- `save.json`：保留 total EXP、Energy、已解锁 Achievement / Title 与当前 Title，以及 EXP 转换余数 `exp_conversion_remainder` 与每日回归标记 `energy_drift_date`。旧 Coin / Shop 字段仅兼容读取。
- `life-events.json`：生活事件与 Level Up、Achievement、Milestone、Title、Entertainment 等重要成长节点。
- `growth-events.json`：每个输入 eventId 唯一对应的成长处理回执，是幂等依据。
- `energy-history.json`：Energy 前后值、类型（EARN / SPEND / DECAY / ADJUST）、请求量、原因与来源 eventId。
- `entertainment-records.json`：娱乐记录（类型、标题、时长、用户填写的 Energy 消耗、备注）。
- `milestones.json`：用户选择保存的人生里程碑。
- `custom-titles.json`：用户自定义 Title。
- `growth-snapshots.json`：同一日期 upsert 一条快照；当前日允许更新。
- `growth-stats.json`：事件增量维护的累计统计；首次迁移或显式重算时从旧事实重建。

写入继续通过 `JsonFileStore` 的临时文件 + 原子移动完成。

## Level 与迁移

v0.4 保留旧等级曲线的阶段阈值，移除旧 `max_level=20` 的硬上限。因此 total EXP 不变，用户不会因升级丢失累计值，也能继续长期升级。Energy 保留原值与 0～180 范围。

启动时旧 Achievement / Title id 原样保留。Growth 页面会从旧定义文件恢复已有名称与描述，但不展示金币奖励或称号 Buff；“信息绝缘体”等已拥有或已装备称号不会消失。新增规则可通过 `POST /api/growth/recalculate` 对既有事实重算，已解锁 id 会阻止重复事件。

Shop、Coin、Purchase 与旧称号 Buff 没有删除存档字段，以保证旧 JSON 可读取；它们已从 v0.4 UI、状态主视图和购买命令断开。购买命令只返回停用提示。

## REST API

- `GET /api/growth`：当前 Growth Overview 与 7 天趋势。
- `GET /api/growth/history`：有实际变化的成长日志。
- `GET /api/growth/energy-history`：Energy 变化原因（含类型与请求量）。
- `GET /api/growth/snapshots`：按天读取快照。
- `POST /api/growth/recalculate`：幂等重算 Achievement / Title。
- `POST /api/growth/energy/spend`、`POST /api/growth/energy/adjust`：统一 Energy 台账接口。
- `GET|POST /api/entertainment`、`PATCH|DELETE /api/entertainment/{id}`：娱乐记录 CRUD。
- `GET /api/achievements[/{id}]`：Definition、Progress 与隐藏状态。
- `GET|POST /api/milestones`、`PATCH|DELETE /api/milestones/{id}`：Milestone CRUD。
- `GET|POST /api/titles`、`POST /api/titles/{id}/equip`、`POST /api/titles/unequip`、`DELETE /api/titles/{id}`：Title 查询、创建、装备、取消装备与自定义删除。

## 前端与反馈

Growth 页面沿用 Summer Sky、浅蓝玻璃卡片与响应式 Shell，使用 Overview / Achievements / Milestones / Titles 四个 Tab。EXP、趋势柱和 Level 圆环使用 180～520ms 的克制动画，并遵守 `prefers-reduced-motion`。批量迁移不会逐条弹出奖励弹窗。

## 验证

```powershell
.\mvnw.cmd test
```

专项覆盖：Focus 只产 Energy（3min/点、单事件上限 80）、满值 clamp 不转换 EXP、小额 SPEND 余数累积、余额不足按实际消耗结算、ADJUST / DECAY 零 EXP 且不动余数、每日 Energy 回归（盈余 / 不足 / 正中基准 / 同日幂等 / 跨天再漂移）、SPEND 事件重放幂等、跨多级升级、旧 `exp_per_energy` 配置迁移、娱乐记录经统一台账结算（含 Energy 不足、重放幂等、编辑不改账、删除不回滚、重启恢复）、Achievement、Milestone、Snapshot、自定义 Title、旧行动命令停用与旧 Shop 主流程停用；既有 Focus、Task、Repository 与架构测试继续作为回归保护。
