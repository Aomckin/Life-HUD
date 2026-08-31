# Life HUD v1.0.0 — First Stable Release

Life HUD 从最初的“宅宅能量条”成长为一套可以长期真实使用的个人生活事实系统。它不试图成为 AI，而是稳定记录生活，并把可信事实开放给外部 Agent。

## 核心能力

- Today：此刻状态、今日脉搏、方向陪伴与 Footprints。
- Focus：铁幕、番茄、自由专注、Segment、暂停恢复和补录。
- Tasks 与 Direction：Daily/Special、Dream、Goal、Milestone。
- Ritual：仪式定义、片段与沉浸执行。
- Life 与 Journal：睡眠、饮食、运动、Check-in、通用记录、图片和统一 Timeline。
- Media：Anime、Game、Book/Manga/Movie/Other 以及 Session 足迹。
- Now：当前阶段、歌单、图片、方向和历史快照。
- Growth：Energy、EXP、Level、Achievement、Title、Milestone 与幂等结算。

## Agent Contract

- `/api/agent/context/*`：只读语义聚合，`schemaVersion="1"`。
- Business API：人与外部 Agent 共用的唯一事实写入口。
- `docs/AGENT_ACTION_API.md`：权限、可逆性、幂等、时间与错误语义。

Life HUD 不包含 LLM、Prompt、Planner、Memory、Permission Engine 或 Tool Runtime。API 默认无鉴权，仅用于本机或可信私网，不能直接暴露公网。

## 数据与兼容

数据默认位于工作目录的 `data/`，包含 JSON 事实与 `uploads/` 媒体。v1.0 保持旧 Save、LifeEvent、Focus、Task、Now 和 Growth 字段兼容，不要求清档。升级前应备份整个数据目录；恢复流程见 `BACKUP_AND_RESTORE.md`。损坏的 JSON 会明确报告文件，不会被静默重置。

## 验收

- Maven：186 tests，0 failure，0 error。
- Agent Action：Sleep、Check-in、Task 幂等、Anime Session 与错误契约闭环通过。
- 独立临时 data dir：全业务写入和服务重启恢复通过。
- UI：空数据与有数据状态下，390px、1366px、1920px 无横向溢出。
- Browser Console：0 warning / 0 error。
- 无用户可见 `undefined`、`null`、`NaN`、`Invalid Date`。

## 已知限制

- 本地单用户、JSON 持久化。
- 生活数据以手动输入为主。
- 无云同步、无内置 Agent、无第三方媒体同步。
- Agent API 无鉴权。
- 图片上传仅校验 MIME 类型或扩展名，不执行文件 Magic Bytes/真实内容识别；本地单用户可信环境应只上传可信文件。

这些是 v1.0 的产品边界，不是未完成的隐性承诺。
