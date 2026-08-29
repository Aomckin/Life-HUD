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

当前 Focus 规则为每 5 个有效分钟 1 EXP、每 30 个有效分钟 1 Energy；单事件分别上限 240 EXP 与 12 Energy。Task 使用任务定义中已有的 EXP / Energy 作为事件元数据，Growth 再执行安全范围限制。测试事件可用 `metadata.test=true` 排除。

## 持久化状态

- `save.json`：保留 total EXP、Energy、已解锁 Achievement / Title 与当前 Title。旧 Coin / Shop 字段仅兼容读取。
- `life-events.json`：生活事件与 Level Up、Achievement、Milestone、Title 等重要成长节点。
- `growth-events.json`：每个输入 eventId 唯一对应的成长处理回执，是幂等依据。
- `energy-history.json`：Energy 前后值、原因与来源 eventId。
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
- `GET /api/growth/energy-history`：Energy 变化原因。
- `GET /api/growth/snapshots`：按天读取快照。
- `POST /api/growth/recalculate`：幂等重算 Achievement / Title。
- `GET /api/achievements[/{id}]`：Definition、Progress 与隐藏状态。
- `GET|POST /api/milestones`、`PATCH|DELETE /api/milestones/{id}`：Milestone CRUD。
- `GET|POST /api/titles`、`POST /api/titles/{id}/equip`、`POST /api/titles/unequip`、`DELETE /api/titles/{id}`：Title 查询、创建、装备、取消装备与自定义删除。

## 前端与反馈

Growth 页面沿用 Summer Sky、浅蓝玻璃卡片与响应式 Shell，使用 Overview / Achievements / Milestones / Titles 四个 Tab。EXP、趋势柱和 Level 圆环使用 180～520ms 的克制动画，并遵守 `prefers-reduced-motion`。批量迁移不会逐条弹出奖励弹窗。

## 验证

```powershell
.\mvnw.cmd test
```

专项覆盖 Focus effectiveMinutes、重复 eventId、跨多级升级、Achievement、Milestone、Snapshot、自定义 Title 与旧 Shop 主流程停用；既有 Focus、Task、Repository 与架构测试继续作为回归保护。
