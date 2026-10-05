# Life HUD v0.3 第二次追加任务书

## Focus Segment / 铁幕过程记录与剩余功能补完

## 版本阶段

Life HUD v0.3 · Focus 收尾第二阶段

当前已经基本完成：

- FocusSession
- IRON_CURTAIN / POMODORO / FREE
- 开始 / 暂停 / 恢复 / 完成
- Session 持久化
- 当前 Focus 恢复
- 今日 Focus 统计
- Focus History
- Dashboard 联动
- 番茄基础计时
- 铁幕专属视觉
- 铁幕开幕 / 落幕动画
- 铁幕运行态视觉

本轮重点不再进行大规模视觉调整。

核心目标：

> 让铁幕从“长时间计时器”变成能够完整记录一段真实工作过程的系统。

同时补齐原 v0.3 计划中尚未实现或实现不完整的功能。

---

# 一、核心目标：FocusSegment

新增 FocusSession 内部的时间切片模型。

当前 FocusSession 只能表达：

```text
14:10
↓
18:20

铁幕 250min

但真实铁幕可能是：

```
14:10 开幕算法           50min休息           10minLife HUD       80min中断           12minJava           40min其他切换       ...18:20 落幕
```

Life HUD 必须能够记录这些内部过程。

---

# 二、FocusSegment 模型

建议建立：

```
FocusSegment├── id├── focusSessionId├── type├── title├── startTime├── endTime├── actualSeconds / actualMinutes├── effective├── relatedTaskId├── note├── order├── createdAt└── updatedAt
```

字段可以根据现有 Java 项目规范调整。

不要机械复制字段名。

---

## 2.1 Segment 类型

至少支持：

```
FOCUSBREAKINTERRUPTION
```

语义：

### FOCUS

真正进行工作 / 学习的有效时间。

例如：

```
算法JavaLife HUD八股投递
```

---

### BREAK

主动休息。

例如：

```
喝水吃东西短暂休息
```

BREAK：

```
不计入 effectiveMinutes
```

但属于铁幕总时长的一部分。

---

### INTERRUPTION

非计划中断。

例如：

```
接电话处理突然的事情被其他事情打断
```

同样：

```
不计入 effectiveMinutes
```

并应该能够在最终铁幕总结中独立看到。

---

# 三、Segment 生命周期

同一个 FocusSession 同一时间最多只能存在：

```
1 个 active Segment
```

状态逻辑：

```
创建 Segment↓开始计时↓切换事项 / 休息 / 中断↓关闭旧 Segment↓创建新 Segment
```

例如：

```
14:10开始：算法15:00切换：休息15:10切换：Life HUD16:30中断16:42继续：Java
```

最终自动生成：

```
算法         50min休息         10minLife HUD     80min中断         12minJava         ...
```

---

# 四、铁幕开幕时创建首个 Segment

进入铁幕时：

当前 Focus 目标可以直接作为首个 Segment。

例如：

```
进入铁幕目标：算法刷题
```

自动：

```
FocusSessionmode = IRON_CURTAINFocusSegmenttype = FOCUStitle = 算法刷题
```

无需让用户进入铁幕后再点一次“开始事项”。

---

# 五、铁幕运行中的事项切换

铁幕运行态新增：

```
当前事项Life HUD v0.301:12:34
```

提供操作：

```
[ 切换事项 ][ 休息 ][ 中断 ]
```

不要让这些按钮抢过：

```
暂停落幕
```

的视觉权重。

---

## 5.1 切换事项

点击：

```
切换事项
```

弹出轻量输入：

```
接下来做什么？[ Java ]可选关联任务[ 开始下一段 ]
```

确认后：

```
关闭当前 Segment↓保存结束时间↓创建新的 FOCUS Segment
```

整个 FocusSession 不结束。

---

# 六、休息 Segment

点击：

```
休息
```

应：

1. 关闭当前 FOCUS Segment
2. 创建 BREAK Segment
3. Session 本身仍然保持运行

页面显示：

```
铁幕进行中当前：休息00:08:12
```

提供：

```
[ 结束休息 ]
```

结束休息后：

允许：

```
继续上一事项
```

或：

```
开始新事项
```

---

# 七、中断 Segment

点击：

```
中断
```

应创建：

```
INTERRUPTION
```

可选填写：

```
中断原因
```

例如：

```
电话临时事务朋友消息外出其他
```

本轮不要强制用户分类。

自由输入即可。

结束中断后：

```
继续上一事项
```

或：

```
开始新事项
```

---

# 八、Session Pause 与 Segment 的区别

必须明确区分：

```
BREAKINTERRUPTIONPAUSE
```

### BREAK

铁幕仍然在进行。

属于铁幕过程。

计入：

```
actualMinutes
```

但不计：

```
effectiveMinutes
```

---

### INTERRUPTION

铁幕仍然在进行。

属于铁幕过程。

计入：

```
actualMinutes
```

但不计：

```
effectiveMinutes
```

---

### PAUSE

整个 FocusSession 暂停。

例如用户真正离开：

```
半小时数小时
```

暂停期间：

不计入：

```
actualMinuteseffectiveMinutes
```

也不产生正常 Segment 时间。

如果当前存在 active Segment：

Pause 时应先关闭或冻结该 Segment。

Resume 后正确恢复。

实现方式根据当前时间模型选择，但必须保证统计正确。

---

# 九、effectiveMinutes

正式补齐：

```
actualMinuteseffectiveMinutes
```

语义。

例如：

```
铁幕：14:00 - 18:00
```

Session 内：

```
FOCUS         60minBREAK         20minFOCUS         90minINTERRUPTION  10minFOCUS         40min
```

另外：

```
Session Pause 20min
```

则：

```
自然跨度：240minactualMinutes：220mineffectiveMinutes：190min
```

建议：

```
effectiveMinutes=所有 FOCUS Segment 有效时长之和
```

不要同时维护一个不断 +1 的有效时间计数器。

应由时间记录可靠计算。

---

# 十、铁幕运行态实时统计

铁幕页面可以展示：

```
铁幕持续03:24:16有效专注02:48:03
```

无需两个数字都做成巨大 Timer。

主 Timer 仍然应该是：

```
铁幕总运行时间
```

有效时间作为次级信息。

---

# 十一、铁幕过程 Timeline

运行态增加轻量过程记录。

例如：

```
本轮铁幕14:10  算法15:00  休息15:10  Life HUD16:30  中断16:42  Java
```

无需做复杂 Timeline UI。

重点是：

> 用户能看到今天这层幕布里到底发生了什么。

当前 Segment 应明显标识：

```
进行中
```

---

# 十二、铁幕落幕总结升级

当前已有基础落幕总结。

本轮升级为完整总结。

例如：

```
铁幕落幕14:10 - 18:20总跨度        4h 10min实际铁幕      3h 58min有效专注      3h 10min──────────────算法          50min休息          10minLife HUD      1h 20min中断          12minJava          40min……──────────────完成了什么？[ 总结 / Note ]
```

视觉继续沿用刚刚已经实现的铁幕落幕设计。

不要重新设计整体视觉。

---

# 十三、Segment 编辑能力

为了现实记录可靠性，允许对已经结束的 Segment 进行轻量修正。

至少支持：

```
修改标题修改类型修改关联任务修改 note
```

时间修改可以根据实现难度决定。

如果支持修改时间：

必须保证：

- Segment 不能互相重叠
- Segment 不能超出 Session 范围
- effectiveMinutes 自动重新计算

---

# 十四、手动补录铁幕

补齐原计划：

```
手动补录
```

允许用户记录没有通过实时 Timer 开启的铁幕。

入口不要做得太突出。

例如放在：

```
Focus History+补录
```

---

## 14.1 最低录入内容

```
日期开始时间结束时间标题 / 总结
```

默认可以生成：

```
一个 FOCUS Segment
```

覆盖整段有效时间。

---

## 14.2 可选详细补录

允许之后进一步拆分：

```
算法        50min休息        10min项目        80min
```

如果实现成本较高：

v0.3 最低要求只实现单 Segment 补录。

但数据模型必须允许未来补充多个 Segment。

---

# 十五、relatedTaskIds 多任务关联

当前实现如果仍然只有：

```
taskId
```

需要升级。

Session 层：

```
relatedTaskIds
```

允许一个铁幕关联多个任务。

原因：

真实铁幕往往会完成：

```
算法八股项目投递
```

而不是只有一个任务。

---

## 15.1 Segment 任务关联

同时允许：

```
FocusSegment.relatedTaskId
```

例如：

```
算法 Segment→ Task ALife HUD Segment→ Task B
```

最终 Session 的：

```
relatedTaskIds
```

可以：

- 自动汇总 Segment 任务
- 或允许额外手动关联

避免数据重复失真。

实现方式由当前架构决定。

---

# 十六、Focus History 升级

History 中铁幕记录支持展开查看。

例如：

```
铁幕开发 / 学习14:10 - 18:20有效 3h10min
```

展开：

```
算法          50min休息          10minLife HUD      80min中断          12minJava          40min
```

不要默认把所有 Segment 展开。

否则 History 会非常长。

---

# 十七、番茄钟剩余功能补齐

原 v0.3 计划中的番茄部分需要再次核查。

已完成的功能不要重做。

只补缺失项。

---

## 17.1 自定义休息时长

番茄配置加入：

```
专注：45 min休息：10 min
```

允许：

```
自定义
```

休息不能强制执行。

---

## 17.2 跳过休息

专注结束后：

```
本轮完成[ 开始休息 ][ 跳过休息 ][ 继续专注 ]
```

允许完全跳过。

---

## 17.3 连续轮次

支持轻量连续番茄。

例如：

```
Round 1Focus↓Break↓Round 2
```

不要实现复杂传统番茄规则。

不要求：

```
4轮后强制长休息复杂周期模板严格自动循环
```

只需要能够：

```
完成一轮↓休息↓再开始下一轮
```

---

## 17.4 番茄 Segment

番茄同样可以复用 FocusSegment。

例如：

```
FOCUSBREAKFOCUSBREAK
```

这样：

```
effectiveMinutes
```

可以与铁幕共享统一统计逻辑。

不要为番茄单独复制另一套时间分段实现。

---

# 十八、FocusSession 统一模型检查

原设计模型：

```
FocusSession├── id├── type├── startTime├── endTime├── plannedMinutes├── actualMinutes├── effectiveMinutes├── status├── note├── interruptions└── relatedTaskIds
```

本轮检查现有实现。

缺少的字段根据现有架构补齐。

其中：

```
interruptions
```

如果 Segment 已经完整表达 INTERRUPTION：

不建议再保存重复的 interruption duration 数组。

可以：

```
通过 Segment 查询得到
```

避免出现两份事实来源。

---

# 十九、LifeEvent

补齐原 v0.3 规划的 LifeEvent 联动。

Focus 业务行为应产生统一事件。

至少包括：

```
FOCUS_STARTEDFOCUS_PAUSEDFOCUS_RESUMEDFOCUS_FINISHED
```

可以进一步补充：

```
FOCUS_INTERRUPTEDFOCUS_SEGMENT_CHANGED
```

但不要为了事件数量而事件化所有点击。

---

# 二十、LifeEvent 设计原则

Focus Service 负责：

```
完成 Focus 业务
```

LifeEvent 负责描述：

```
生活中发生了什么
```

例如：

```
FOCUS_FINISHEDsourceId = focusSessionIdmode = IRON_CURTAINactualMinutes = 238effectiveMinutes = 190occurredAt = ...
```

后续：

```
TimelineProgression朝汐统计
```

都可以消费这些事件。

---

# 二十一、不要把 Progression 写进 Focus

本轮可以建立 LifeEvent。

但暂时不要让 Focus Service 直接出现：

```
exp += 10energy += 5
```

后续成长体系应通过：

```
LifeEvent↓Progression Engine
```

独立处理。

这条架构边界必须保留。

如果实现 LifeEvent 后顺手建立 Progression 基础框架可以接受，但不要污染 Focus 核心逻辑。

---

# 二十二、Timeline 数据源

补齐：

```
Focus 自动进入 Timeline
```

Timeline 最低要求可以只是数据层接入。

例如：

```
14:10铁幕开幕15:00切换到休息15:10继续 Life HUD18:20铁幕落幕有效专注 3h10min
```

如果当前 Life HUD 尚无完整 Timeline 页面：

本轮不要求提前做完整 Timeline UI。

但是：

```
LifeEvent 数据必须可作为未来 Timeline 的统一数据源。
```

如果已有 Timeline：

直接接入。

---

# 二十三、Dashboard Focus 数据调整

Dashboard 当前已有今日 Focus。

升级统计逻辑：

优先展示：

```
今日有效 Focus
```

而不是简单自然跨度。

例如：

```
Focus3h 10min4 Sessions
```

其中：

```
3h10min = effectiveMinutes
```

可以在次级信息展示：

```
铁幕总运行 4h
```

但不要让 Dashboard 复杂化。

---

# 二十四、跨日 Segment

必须处理：

```
23:30铁幕开始00:30结束
```

Segment 仍然保持完整。

今日统计行为必须明确。

建议优先：

```
按实际发生时间切分今日 effectiveMinutes
```

如果当前架构暂时不适合：

允许继续按照 Session 归属日计算。

但必须：

- 不重复
- 不丢失
- 文档记录规则

---

# 二十五、异常恢复

重点处理页面刷新 / 崩溃恢复。

例如：

```
当前铁幕↓当前 Segment = Life HUD↓浏览器刷新
```

刷新后必须恢复：

```
FocusSession+当前 Segment
```

不能只知道：

```
铁幕还在运行
```

却不知道：

```
当前正在做 Life HUD
```

---

# 二十六、异常关闭

如果应用退出时：

```
FocusSession = RUNNINGFocusSegment = ACTIVE
```

下次打开：

继续按照时间恢复。

不要自动结束 Session。

未来可以增加“检测异常未结束”的机制。

v0.3 暂不要求。

---

# 二十七、数据兼容

当前已经存在旧 FocusSession 数据。

新增 Segment 后：

必须保证旧数据仍然可读取。

建议迁移策略：

对于没有 Segment 的历史 FocusSession：

可在读取 / migration 时视作：

```
一个默认 FOCUS Segment
```

其范围为：

```
有效运行时间
```

或保持：

```
segments = []
```

并在 UI 中兼容。

禁止因为新增 Segment 导致旧历史无法加载。

---

# 二十八、API 建议

根据现有 REST 结构调整。

可考虑：

```
POST /api/focus/{id}/segmentsPOST /api/focus/{id}/segments/switchPOST /api/focus/{id}/breakPOST /api/focus/{id}/interruptPOST /api/focus/{id}/break/endPOST /api/focus/{id}/interrupt/endGET /api/focus/{id}/segmentsPATCH /api/focus/{id}/segments/{segmentId}
```

不要为了严格匹配这些路径破坏现有 API 风格。

---

# 二十九、Service 边界

建议：

```
FocusSessionService
```

负责：

- Session 生命周期
- Start / Pause / Resume / Finish
- Session 聚合统计

```
FocusSegmentService
```

负责：

- Segment 创建
- 切换
- Break
- Interruption
- Segment 校验
- effective time 统计

如果当前规模较小，也可以在同一个 FocusService 内保持清晰的方法边界。

不要为了“分层”制造大量空壳 Service。

---

# 三十、测试重点

至少覆盖：

## Case A：普通铁幕

```
14:00 算法15:00 Life HUD16:30 落幕
```

结果：

```
算法       60minLife HUD   90mineffective = 150min
```

---

## Case B：休息

```
Focus 60Break 20Focus 40
```

结果：

```
actual = 120effective = 100
```

---

## Case C：中断

```
Focus 60Interruption 15Focus 45
```

结果：

```
actual = 120effective = 105
```

---

## Case D：Pause

```
Focus 60Pause 30Focus 60
```

结果：

```
actual = 120effective = 120
```

Pause 的 30min：

不得计入 Session actual。

---

## Case E：混合

```
Focus          50Break          10Focus          80Interruption   12Focus          40Pause          30Focus          20
```

结果必须严格正确。

---

## Case F：刷新

Segment 运行 20min 后刷新。

恢复后：

- Session 正常
- Segment 正常
- 时间正常
- effective 正常

---

## Case G：旧数据

加载 v0.3 之前已经保存的 FocusSession。

不得报错。

---

# 三十一、最终铁幕体验

完成本轮后，一次真实铁幕应能够：

```
开幕↓算法↓休息↓Life HUD↓临时中断↓Java↓暂停离开↓回来恢复↓继续项目↓落幕
```

Life HUD 自动得到：

```
什么时候开始什么时候结束总共运行多久真正专注多久休息多久中断多久做过哪些事情每件事情持续多久关联了哪些任务最后完成了什么
```

用户不再需要手工重新回忆整段铁幕。

---

# 三十二、本轮完成标准

必须完成：

- FocusSegment 数据模型
- Segment 持久化
- Segment 实时切换
- BREAK
- INTERRUPTION
- effectiveMinutes
- 铁幕过程记录
- 铁幕落幕详细总结
- 刷新恢复 active Segment
- History 查看 Segment
- 多任务关联基础能力
- 手动补录基础能力
- 番茄自定义休息检查 / 补齐
- 番茄跳过休息检查 / 补齐
- 连续轮次基础能力
- LifeEvent 生成
- Timeline 数据源接入
- Dashboard 使用有效 Focus 时间
- 旧数据兼容
- 测试通过

已经存在并正确工作的能力不要重复重构。

---

# Definition of Done

完成后，IRON_CURTAIN 与 FREE 不再只是两个名称不同的正计时器。

FREE：

> 我准备专注一会。

IRON_CURTAIN：

> 我要展开一段完整的工作时段，并让 Life HUD 记录这段时间里发生的一切。

这就是两者真正的产品区别。


