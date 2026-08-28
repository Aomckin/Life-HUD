# Life HUD v0.3.0

Life HUD 的 Java 21 / Spring Boot 3 版本。v0.3.0 “Focus / 铁幕与人性化番茄钟”提供统一的 Focus Session：铁幕、番茄与自由专注共享可靠的后端计时、暂停/恢复、刷新恢复、今日摘要和历史记录，并继续使用 Summer Sky 与玻璃化 App Shell。

## 运行

```powershell
.\mvnw.cmd spring-boot:run
```

浏览器访问 <http://localhost:8025>。首次启动会把随包默认 JSON 复制到工作目录的 `data/`，之后从该目录读写存档。也可用配置项 `lifehud.data-dir` 或环境变量 `LIFEHUD_DATA_DIR` 指定数据目录；为兼容旧存档工作流，同时识别 `OTAKU_ENERGY_DATA_DIR`。

## Focus System

`/focus` 提供铁幕、番茄和自由专注三种节奏，它们共享同一个 `FocusSession` 状态机：`RUNNING ↔ PAUSED → COMPLETED`，运行或暂停状态也可标记为 `INTERRUPTED`。后端保存当前活动片段与已累计有效秒数，因此刷新页面、切换标签页和暂停都不会破坏实际时长。

铁幕模式具有独立的数字仪式感：进入时短暂显示开幕提示，运行期间将当前或自定义壁纸转为低亮、低饱和、轻模糊的铁幕态，并弱化导航、统计和历史；暂停会保留铁幕环境，完成后显示克制的落幕反馈。它不切换独立主题，也不会锁定页面。

铁幕过程由持久化的 `FOCUS`、`BREAK`、`INTERRUPTION` Segment 构成，可切换事项、恢复当前段、汇总多任务并由 FOCUS 段推导有效专注。History、落幕总结和手动补录共用这套模型；番茄支持自定义休息、跳过休息与轻量连续轮次。

番茄配置将“自定义专注”和“休息时长”作为两个独立、对齐的输入字段；运行页只保留底部主结束入口，避免重复操作。若前端资源与尚未重启的旧后端不匹配，Segment 请求会提示重启服务，而非只显示泛化 404。

Focus API 提供 Current Focus、Today Summary 和 History；Today 统计按 Session 的开始日期归属，跨日 Session 保持完整，不拆分或重复统计。Dashboard 只展示当前 Focus 或今日摘要，并可返回工作台。

## 测试

```powershell
.\mvnw.cmd test
```

旧 Python 实现保留在相邻的 `宅宅能量条` 仓库中，未被修改。

## 代码状态与交接

当前实现、已完成边界、尚未完成的重构项、主要调用链及验证命令见 [CODEBASE_STATUS.md](docs/CODEBASE_STATUS.md)。开始新的开发任务前请先阅读该文件。
