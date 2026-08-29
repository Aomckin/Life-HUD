### Life HUD v0.5.3.1 小任务：Task Pool / Today Task 语义修正

当前 `/tasks` 的卡片视觉已经基本可用，但数据源语义错误：页面把 **Daily / Special 整个任务池全部展开**，导致“今日行动”和“任务模板”混在了一起。

本轮只修这一点，不再重做 UI。

目标改为：

```
Task Pool
任务定义 / 模板池
        ↓
今日抽取 / 生成 / 选择
        ↓
Today Tasks
今天真正需要完成的任务
        ↓
完成
        ↓
TASK_COMPLETED / History
```

具体要求：

1. `/tasks` 主页面禁止直接展示全部任务池，只展示“今天实际激活的任务”。
2. Daily 任务区展示今天生成 / 激活的 Daily 实例，而不是所有 Daily Definition。
3. Special 更不能把整个池子展开。每天只展示少量实际抽取或手动选中的 Special，例如 2~4 个。
4. 增加次级入口：

```
管理任务池 →
```

任务池管理页面 / Modal 中才展示全部：

- Daily 模板

- Special 模板

- 新增

- 编辑

- 删除 / 停用
5. 明确区分：

```
Task Definition
≠
Today Task / Task Occurrence
```

如果当前数据结构已经存在“日期完成记录 / 今日状态”，优先复用；若没有，则新增最小必要的 TodayTask / occurrence 记录，不要大规模重构 Task 系统。

6. 今日任务至少保存：

```
date
taskDefinitionId
status
completedAt
```

如 Direction 需要展示，可继续复用原 Definition 的 Dream / Goal / DreamMilestone 关联。

7. `/tasks` 页面最终结构应接近：

```
任务 · Tasks

今天
4 / 7 已完成

今日行动
[Daily 今日实例]

特别行动
[今天选中的少量 Special]

今天完成了 · 4
[折叠]

管理任务池 →
```

8. Special 的目标语义：

> 像从“生活挑战牌堆”里翻出几张今天想做的牌。

不是：

> 把全部特殊任务一次性交给用户完成。

9. 保留 v0.5.3 已完成的：
- Task Card
- Daily / Special 视觉差异
- 完成按钮
- Direction chip
- 已完成弱化
- Today Summary

不要重新设计 UI。

10. 验收：

准备 Special Pool 至少 10 条，刷新 `/tasks` 后主页面只能看到今天激活的少数几条，而不是 10 条全部出现。

Daily Pool 同理，只展示今日实例。

进入“管理任务池”后，才可以看到完整池子。

版本更新为：

```
v0.5.3.1
```

一句话：

> **主页面看今天，任务池看所有。两者不要再混在一起。**