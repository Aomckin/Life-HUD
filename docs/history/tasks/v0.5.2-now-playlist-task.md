# Life HUD v0.5.2 开发任务书
## `/now` 「现在。」歌单沉浸式横板重构

> 版本：`v0.5.2`
>
> 主题：**Now / 「现在。」沉浸歌单**
>
> 本版本属于 v0.5 Direction 阶段的体验补丁，重点重构 `/now` 的「现在。」歌单区域。
>
> 核心目标：
>
> **让十首歌不再像十个上传框，而像某个阶段里，被挂在空气中的十张心情卡片。**

---

# 0. 版本定位

当前 `/now` 已经具备：

- `NowState`
- `NowSnapshot`
- 当前游戏 / 番剧 / 书
- 当前 Dream / Goal
- 自由文字
- 图片
- 固定十首歌 Slot
- MP3 / FLAC 上传与基础元数据读取

但当前歌单 UI 仍然偏向：

```text
规则十宫格
+
等大卡片
+
上传器 / 后台管理页面
```

这与「现在。」的产品定位不符。

本版本需要把歌单区域重构为：

```text
横板沉浸展示
+
大背景
+
不等大歌曲卡片
+
空气感散落布局
+
手动听歌次数决定视觉权重
+
每首歌附带一句阶段记录
```

目标不是把 `/now` 做成音乐播放器，而是让音乐成为：

> **人生阶段快照的一部分。**

---

# 1. 本轮最高优先级

## P0：必须完成

- `/now` 歌单改为横板沉浸布局
- 支持歌单背景图
- 十首歌保持固定 Slot
- 歌曲卡片不等大
- 手动填写 `playCount`
- `playCount` 决定卡片视觉大小
- 每首歌支持 `note`
- `note` 以纸条 / 标签形式挂在歌曲卡片上
- 布局位置持久化，刷新后不随机乱跳
- 当前 Now 与历史 Snapshot 都能正确显示
- Snapshot 冻结背景、歌曲、次数、备注与布局
- MP3 / FLAC 与图片上传链路保持正常

## P1：尽量完成

- 卡片轻微旋转
- 图钉 / 胶带 / 纸夹等轻装饰
- hover 轻交互
- 编辑 Modal / Drawer
- 空 Slot 融入沉浸布局
- 最高频歌曲自动成为视觉主卡

## P2：可以后补

- 自由拖拽布局
- 卡片碰撞实时计算
- 背景视差
- 入场动画
- 全局播放器
- 自动统计真实播放次数
- 完整移动端沉浸布局

---

# 2. 产品定义

## 2.1 `/now`

`/now` 不是：

```text
资料填写页
后台表单
音乐管理器
普通日记
```

它是：

> **某个人生阶段里，“此刻的我”是什么样的陈列页。**

歌单属于其中最具情绪和记忆感的一部分。

---

## 2.2 「现在。」歌单

固定：

```text
10 个 Slot
```

不是无限歌曲收藏。

每个 Slot 对应：

```text
一首此刻最喜欢的歌
+
此刻听它的频率
+
为什么它属于这个阶段
```

支持：

```text
歌曲文件
封面
歌名
艺术家
时长
听歌次数
一句话 / note
```

---

# 3. 设计方向

整体参考：

> **MemeVault 沉浸浏览中的空气感与散落布局。**

但不要机械复制 MemeVault。

提取的是：

- 大背景承担整体氛围
- 内容像悬浮在空间中
- 大量留白
- 卡片有疏有密
- 不完全对齐
- 不等尺寸
- 浏览时更像“看一面记忆墙”而不是看后台面板

Life HUD 自身仍保持：

- 白 / 浅蓝主视觉
- 半透明卡片
- 轻毛玻璃
- 柔和阴影
- 克制装饰
- 夏日 / 空气 / 光感

禁止做成：

- 规整十宫格
- 复杂 Pinterest 瀑布流
- 素材堆满的手账页
- 视觉噪声过强的自由画布
- 完全随机的卡片迷宫

---

# 4. 数据结构改造

## 4.1 NowSong

如果当前歌曲仍依赖简单 `NowItem`，需要升级为独立结构。

建议：

```text
NowSong
├── slot
├── filePath
├── originalFilename
├── title
├── artist
├── album
├── durationSeconds
├── coverImagePath
├── format
├── playCount
├── note
├── posX
├── posY
├── rotationDeg
├── zIndex
├── createdAt
└── updatedAt
```

---

## 4.2 字段语义

### slot

严格：

```text
1 ~ 10
```

固定十个位置。

---

### playCount

用户手动填写：

```text
听了多少次
```

它不是播放器真实统计。

用途：

```text
playCount
↓
视觉权重
↓
卡片尺寸
```

必须持久化。

---

### note

每首歌允许记录 1~2 句话。

例如：

```text
八月末……
夏日落幕感……
```

或者：

```text
我发现，我真的很喜欢，
猫日里活跃又带点慵懒的超棒感觉。
```

它属于人生阶段记录，不只是歌曲元数据。

---

### posX / posY

推荐使用：

```text
0 ~ 1
```

归一化坐标。

意义：

- 页面首次生成布局后保存
- 分辨率变化时方便按比例计算
- 当前 Now 布局稳定
- Snapshot 可冻结当时布局

---

### rotationDeg

允许轻微旋转：

```text
-4° ~ +4°
```

不要夸张。

---

# 5. NowState 扩展

建议增加：

```text
NowState
├── ...
├── playlistBackgroundImage
├── playlistTitle
├── playlistSubtitle
└── songs[]
```

默认：

```text
playlistTitle:
「现在。」歌单

playlistSubtitle:
此刻最喜欢的十首歌
```

---

# 6. Snapshot 冻结

本版本必须保证：

```text
当前 Now
≠
历史 Snapshot
```

Snapshot 需要冻结：

```text
背景图
歌曲 slot
歌曲 title
artist
album
duration
cover
playCount
note
posX
posY
rotationDeg
zIndex
```

同时继续冻结已有：

```text
stageTitle
theme
currentDream
currentGoal
quote
content
images
```

测试场景：

```text
保存 Snapshot A

修改：
背景
歌曲
次数
note
布局

再次打开 Snapshot A
```

Snapshot A 必须完全保持原样。

---

# 7. 旧数据迁移

如果已有歌曲数据：

```text
旧 favoriteSongs
```

必须迁移，而不是清空。

建议：

```text
旧顺序
↓
slot 1~n

playCount = 0
note = ""
```

首次进入新版 `/now`：

```text
若无 posX / posY
→ 自动生成布局
→ 保存
```

不得每刷新一次重新随机。

---

# 8. 页面总体结构

`/now` 建议：

```text
NowPage

├── Stage Header
│
├── Playlist Board
│
├── Current Media
│   ├── Game
│   ├── Anime
│   └── Book
│
├── Current Direction
│   ├── Dream
│   └── Goal
│
├── Quote
│
├── Content
│
├── Images
│
├── Save Actions
│
└── Snapshot History
```

本版本重点只重构：

```text
Playlist Board
```

其他区域不需要同步大改。

---

# 9. Playlist Board

## 9.1 横板舞台

新增核心组件：

```text
PlaylistBoard
```

要求：

- 宽幅横板
- 桌面端优先
- 视觉上像一面有背景的空间
- 卡片散落于内部

建议：

```text
height: 620px ~ 820px
```

根据页面宽度自适应。

---

## 9.2 背景图

支持：

```text
上传
替换
删除
```

背景图不是普通 Now 图片。

单独作为：

```text
playlistBackgroundImage
```

背景显示时需要处理：

- cover
- blur
- 亮度 / 饱和度适当降低
- 浅色遮罩
- 保证卡片文字可读

推荐层次：

```text
原始背景
↓
轻 blur
↓
浅色半透明遮罩
↓
歌曲卡片
```

不要让背景抢走前景。

---

# 10. 歌曲卡片

## 10.1 横板卡片

卡片本体改为横向。

推荐：

```text
┌──────────────────────────┐
│ [封面]  歌名             │
│         艺术家           │
│         03:42   听了42次 │
└──────────────────────────┘
          │
          ▼
   [这一阶段的一句话]
```

不要继续使用竖向专辑墙样式。

---

## 10.2 卡片内容

至少包含：

```text
cover
title
artist
duration
playCount
```

交互操作：

```text
播放（可选）
编辑
替换
删除
```

编辑 / 删除不要永久裸露。

建议：

- hover 显示
- `⋯`
- 点击卡片打开编辑 Drawer / Modal

---

# 11. note 纸条

`note` 必须成为明显视觉元素。

推荐形态：

```text
卡片
   ↓
小纸条 / 标签 / 便签
```

可以使用：

- 胶带
- 图钉
- 纸夹
- 小折角

但只做轻装饰。

不要每张卡片都塞满不同素材。

建议只准备 2~3 套装饰样式，然后按 slot 稳定分配。

例如：

```text
slot 1,4,7 → tape
slot 2,5,8 → pin
slot 3,6,9 → clip
slot 10 → tape
```

这样既有变化又稳定。

---

# 12. 听歌次数 → 大小

## 12.1 基础规则

根据：

```text
playCount
```

计算：

```text
sizeLevel
```

建议第一版：

```text
0~4      → SMALL
5~14     → MEDIUM
15~29    → LARGE
30~49    → XL
50+      → FEATURED
```

具体数值可以根据实际页面调节。

---

## 12.2 卡片差异

不同级别体现为：

- width
- height
- cover size
- 字号
- note 可用宽度
- shadow 强度极轻微变化

不要简单做：

```text
transform: scale(2)
```

避免文字、圆角和布局一起失控。

---

## 12.3 主打歌

当前 `playCount` 最大的一首：

```text
Featured Song
```

可以：

- 成为最大卡
- 更接近视觉中心
- note 完整展示
- 卡片信息更丰富

如果播放次数相同：

```text
slot 更靠前者优先
```

确保确定性。

---

# 13. 布局系统

这是本版本最关键的技术部分。

## 13.1 禁止纯随机定位

禁止：

```text
Math.random()
↓
每次刷新换位置
```

页面不是粒子系统。

布局必须：

```text
稳定
可重复
可保存
```

---

## 13.2 推荐方案：预定义挂点

第一版不要开发复杂物理引擎。

推荐提前定义约 10~14 个“挂点区域”。

例如：

```text
A1 左上
A2 左中
A3 左下

B1 中上
B2 中心
B3 中下

C1 右上
C2 右中
C3 右下

D1 中偏左
D2 中偏右
...
```

每个区域定义：

```text
baseX
baseY
allowedWidth
allowedHeight
rotationRange
```

然后：

```text
根据 playCount 排序
↓
最大卡优先分配空间大的挂点
↓
剩余卡分配其他挂点
↓
加入轻微确定性 offset
```

确定性 offset 可以由：

```text
slot
```

作为 seed。

这样：

- 有变化
- 有空气感
- 不会随机跳
- 工程风险比物理模拟低很多

---

## 13.3 防重叠

第一版只需要保证：

```text
视觉上不严重遮挡
```

实现可采用：

```text
Bounding Box 检测
+
少量尝试重新分配挂点
```

不需要实时物理碰撞。

目标：

- 卡片允许轻微靠近
- note 可以轻微压到背景
- 卡片主体不能互相大面积重叠
- 不能超出容器

---

## 13.4 布局保存

首次生成：

```text
posX
posY
rotationDeg
zIndex
```

之后保存。

修改 `playCount` 导致尺寸级别变化时：

```text
若新尺寸仍适合原位置
→ 保留位置

若明显碰撞 / 越界
→ 重新布局相关卡片
```

无需每次编辑都全盘重排。

---

# 14. 空 Slot

空 Slot 不再占一个大白框。

建议表现：

```text
淡淡的悬浮标签

03
＋ 添加歌曲
```

要求：

- 存在感明显低于已有歌曲
- 可以放在外围位置
- 点击后打开上传
- 不影响主要歌曲的空气感

当歌很少时，不要让 8 个空卡把页面重新变成十宫格。

---

# 15. 歌曲编辑

点击歌曲卡片后：

```text
SongEditorModal / SongEditorDrawer
```

支持：

```text
歌曲文件
title
artist
album
playCount
note
封面
```

其中：

```text
playCount
note
```

是本版本新增核心编辑项。

---

# 16. 添加歌曲流程

点击空 Slot：

```text
上传音频
↓
保存文件
↓
读取 metadata
↓
提取封面
↓
预填歌曲信息
↓
用户填写 playCount
↓
用户填写 note
↓
保存
↓
生成 / 分配布局
↓
显示卡片
```

不得要求用户先手动填写完整歌名再上传。

---

# 17. 音频支持

至少支持：

```text
MP3
FLAC
```

若现有实现成本低，可继续支持：

```text
M4A
WAV
```

读取：

```text
title
artist
album
duration
embedded cover
format
```

缺失兜底：

```text
title → filename
artist → 未知艺术家
cover → 默认音乐封面
duration → --:--
```

---

# 18. 图片 / 文件链路

继续复用已有统一存储能力。

禁止新增：

```text
NowImageUploadService
PlaylistImageUploadService
SongCoverUploadService
```

三套重复逻辑。

优先：

```text
统一 File / Image Storage
```

需要保证：

```text
上传
刷新
重新加载
Snapshot
```

全链路正常。

---

# 19. Snapshot 展示

快照详情页与当前 `/now` 尽量共用：

```text
PlaylistBoard
```

提供：

```text
editable = true / false
```

当前 Now：

```text
editable = true
```

历史 Snapshot：

```text
editable = false
```

只读模式：

- 不显示添加
- 不显示删除
- 不显示编辑
- 不显示背景上传
- 可保留播放能力（若已有）

---

# 20. 当前游戏 / 番剧 / 书

本版本不提前开发 v0.7 Media。

继续使用轻量 NowItem。

只需保证：

- 不与新版歌单视觉冲突
- 不再抢占大量纵向空间
- 能正常保存
- Snapshot 正常冻结

不进行大规模业务重构。

---

# 21. Dream / Goal

继续复用 v0.5 Direction 真实数据。

本版：

```text
不改 Dream 模型
不改 Goal 模型
不改 Task Direction
```

只确保 `/now` 页面关联内容仍正常显示。

---

# 22. 前端组件建议

推荐：

```text
NowPage
│
├── NowHeaderSection
│
├── PlaylistBoard
│   ├── PlaylistBackground
│   ├── PlaylistHeader
│   ├── PlaylistSongCard
│   ├── PlaylistSongNote
│   ├── PlaylistEmptySlot
│   └── PlaylistControls
│
├── SongEditorModal / Drawer
│
├── NowMediaSection
├── NowDirectionSection
├── NowQuoteSection
├── NowContentSection
├── NowImagesSection
│
└── SnapshotSection
```

不要把：

```text
上传
布局
音乐 metadata
编辑
Snapshot
```

全部写进一个超大页面 JS。

---

# 23. CSS / 布局要求

推荐 PlaylistBoard：

```css
position: relative;
overflow: hidden;
```

歌曲：

```css
position: absolute;
```

位置根据：

```text
posX
posY
sizeLevel
rotationDeg
```

计算。

要求：

- `transform-origin` 合理
- 卡片 hover 不引起整体 layout shift
- note 与卡片一起构成整体 bounding box
- 旋转时注意越界
- 背景响应容器尺寸

---

# 24. 响应式

本版本以桌面端为主。

桌面：

```text
完整沉浸布局
```

小屏：

优先降级为：

```text
2 列 / 1 列普通歌曲卡片
```

而不是把绝对定位舞台硬压到手机上。

要求：

```text
不溢出
不横向炸屏
基本可编辑
```

暂不要求移动端保持完整“记忆墙”效果。

---

# 25. API 建议

根据现有项目 API 风格调整。

## Now

```text
GET    /api/now
PUT    /api/now

POST   /api/now/snapshots
GET    /api/now/snapshots
GET    /api/now/snapshots/{id}
DELETE /api/now/snapshots/{id}
```

## Songs

可实现：

```text
POST   /api/now/songs/{slot}/upload
PUT    /api/now/songs/{slot}
DELETE /api/now/songs/{slot}
POST   /api/now/songs/{slot}/replace
```

或者继续使用统一 `PUT /api/now`。

重点不是 URL，而是完整支持：

```text
playCount
note
layout
audio
cover
```

## Background

可以：

```text
POST /api/now/background
DELETE /api/now/background
```

也可以复用通用图片上传后写回 NowState。

---

# 26. 数据一致性

以下操作必须正确：

### 删除歌曲

只清空对应 Slot。

不要导致：

```text
后面歌曲全部向前挪
```

因为 Slot 有阶段意义。

---

### 替换歌曲

默认保留：

```text
slot
posX
posY
```

新的：

```text
title
artist
cover
duration
```

重新读取。

`playCount / note`：

建议由用户确认是否保留。

---

### 修改 playCount

更新：

```text
sizeLevel
```

必要时局部重新布局。

---

# 27. 本版明确禁止

不要开发：

```text
Spotify
Last.fm
Apple Music

真实播放次数监听
播放历史
歌曲推荐

完整 Media 数据库
音乐搜索服务

Canvas 自由编辑器
节点编辑器
复杂拖拽系统

物理模拟
力导向布局
WebGL
Three.js

整个 Life HUD UI 重构
Growth 重构
Dream 重构
Ritual 重构
Agent API
朝汐 Agent
```

---

# 28. 功能测试

## Songs

- 上传 MP3
- 上传 FLAC
- metadata 正常读取
- 无 metadata 可兜底
- 有封面正确提取
- 无封面使用默认封面
- playCount 正确持久化
- note 正确持久化
- 删除不破坏其他 Slot
- 替换正确

## Layout

- 首次自动生成
- 页面刷新后不变
- 卡片不大面积遮挡
- 最大 playCount 卡片视觉权重最大
- 0 次播放也可正常显示
- 只有 1 首歌时布局不怪
- 10 首全满时不拥挤失控

## Background

- 上传
- 替换
- 删除
- 刷新后存在
- Snapshot 正确冻结

## Snapshot

保存 Snapshot A 后修改：

```text
背景
歌曲
playCount
note
布局
```

Snapshot A 必须完全不变。

---

# 29. 视觉验收

必须满足：

- 第一眼不再像十个上传槽
- 有明显横板感
- 有大片空气与背景空间
- 卡片有大小层级
- 最高频歌曲自然成为视觉重心
- 歌曲像“随手挂在背景上的卡片”
- note 像附着于歌曲的阶段记忆
- 背景有存在感但不影响阅读
- 卡片之间不机械等距
- 整体仍属于 Life HUD

---

# 30. 真实验收流程

## Flow A：基础展示

上传背景图。

上传至少 5 首歌：

```text
Song A → 42 次
Song B → 31 次
Song C → 22 次
Song D → 12 次
Song E → 3 次
```

分别填写 note。

确认：

- A 最大
- B 次之
- E 最小
- 大小差异明显但不夸张
- 布局有空气感
- note 正常展示

---

## Flow B：刷新稳定性

刷新浏览器 3 次。

确认：

```text
所有卡片位置保持稳定
```

---

## Flow C：修改次数

把：

```text
Song E
3 次
```

改成：

```text
60 次
```

确认：

- E 成为主卡或最大卡之一
- 页面没有严重重叠
- 其他数据不变

---

## Flow D：Snapshot

保存：

```text
2026 盛夏
```

快照。

随后：

- 替换主打歌
- 修改 note
- 修改背景
- 修改 playCount

重新打开旧 Snapshot。

确认：

```text
旧背景不变
旧歌曲不变
旧大小不变
旧 note 不变
旧布局不变
```

---

# 31. 回归测试

必须保证：

```text
Dream
Goal
DreamMilestone
Task Direction
Ritual
RitualExecution
NowState
NowSnapshot
LifeEvent
Growth
```

没有明显回归。

尤其检查：

```text
NOW_SNAPSHOT_CREATED
```

仍正常生成。

---

# 32. 工程约束

继续遵循当前 Java 21 / Spring Boot 项目结构。

禁止：

```text
Controller 写全部逻辑
Map<String,Object> 到处传
页面 JS 变成数千行万能文件
重复 File Service
重复上传逻辑
```

优先：

```text
DTO
Service
Repository
enum
record
组件拆分
共用 Storage Service
```

但不要为了本补丁过度抽象。

---

# 33. 版本完成标准

v0.5.2 只有满足以下条件才算完成：

- `/now` 歌单已改为横板沉浸式展示
- 支持独立大背景图
- 十个 Slot 完整保留
- 歌曲卡片横向展示
- 卡片不等大
- 大小由手动 `playCount` 决定
- 支持每首歌 `note`
- note 以挂着的纸条 / 标签方式展示
- 布局有空气感而非规则 Grid
- 布局刷新后稳定
- 当前 Now 与 Snapshot 都能正确渲染
- Snapshot 冻结歌曲、次数、note、背景与布局
- MP3 / FLAC 上传正常
- 图片上传正常
- v0.5 其他模块无明显回归
- 后端测试通过
- 前端无明显控制台错误
- 版本号更新为 `v0.5.2`

---

# 34. 最终提交要求

开发结束后输出：

```text
1. v0.5.2 实际完成内容
2. 数据结构变化
3. 数据迁移方式
4. 新增 / 修改 API
5. PlaylistBoard 组件结构
6. playCount → size 映射规则
7. 自动布局算法说明
8. Snapshot 冻结方式
9. 上传链路说明
10. 测试结果
11. 已知问题
12. 与任务书的偏差及原因
```

不要只输出：

```text
v0.5.2 已完成
```

需要留下可验收的实现报告。

---

# 35. 一句话版本目标

> **不是把十首歌整齐地摆出来。**
>
> **而是让它们像这一阶段留下的十张标签一样，大小不一地挂在属于“现在”的空气里。**
