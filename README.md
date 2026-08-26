# Life HUD v0.1.1

宅宅能量条 v1.2 的 Java 21 / Spring Boot 3 行为保持型迁移版本。

## 运行

```powershell
.\mvnw.cmd spring-boot:run
```

浏览器访问 <http://localhost:8025>。首次启动会把随包默认 JSON 复制到工作目录的 `data/`，之后从该目录读写存档。也可用配置项 `lifehud.data-dir` 或环境变量 `LIFEHUD_DATA_DIR` 指定数据目录；为兼容旧存档工作流，同时识别 `OTAKU_ENERGY_DATA_DIR`。

## 测试

```powershell
.\mvnw.cmd test
```

旧 Python 实现保留在相邻的 `宅宅能量条` 仓库中，未被修改。

## 代码状态与交接

当前实现、已完成边界、尚未完成的重构项、主要调用链及验证命令见 [CODEBASE_STATUS.md](CODEBASE_STATUS.md)。开始新的开发任务前请先阅读该文件。