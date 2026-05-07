# AircraftWar

Android 版飞机大战项目，支持单机闯关、本地排行榜、双人联机匹配与联机排行榜。

项目主体是 Android 客户端，另包含一个用于联机匹配和排行榜同步的 Java Socket 服务端。`legacy-desktop/` 中保留了早期 Swing/AWT 桌面版代码，仅作为历史归档，不参与当前 Android 构建。

## 功能概览

- 单机模式：简单、普通、困难三档难度。
- 联机模式：通过 Socket 服务端进行双人 FIFO 匹配，匹配成功后双方同步开局。
- 游戏操作：触屏拖动英雄机移动，自动射击、碰撞判定、道具拾取、Boss 战和音效播放。
- 本地排行榜：单机结算后写入 SQLite，保留每次成绩，支持长按删除单条和清空全部。
- 联机排行榜：服务端保存对局结果，按 `userId` 合并最高分并返回 Top10，客户端只展示不删除。
- 玩家身份：入口页支持输入 `userId`，联机模式必填；非法字符会被替换为 `_`，最长 24 位。

## 技术栈

- Android / Java 11
- Gradle Kotlin DSL
- AppCompat、Material Components、ConstraintLayout
- Android `SurfaceView` 游戏循环
- SQLite 本地持久化
- Java Socket 行文本协议

## 运行环境

- Android Studio
- JDK 11 或以上
- Android SDK：项目当前 `compileSdk` 为 36，`minSdk` 为 24
- Windows 可直接使用仓库内的 `gradlew.bat`；macOS / Linux 使用 `./gradlew`

## 项目结构

```text
.
├── app/                         # Android 客户端主模块
│   └── src/main/java/edu/hitsz/
│       ├── MainActivity.java     # 首页：难度、音乐、userId 入口
│       ├── GameActivity.java     # 游戏容器与结算跳转
│       ├── LeaderboardActivity.java
│       ├── application/          # 游戏主循环与模式实现
│       ├── aircraft/             # 英雄机、敌机、Boss
│       ├── bullet/               # 子弹模型
│       ├── prop/                 # 道具模型
│       ├── factory/              # 敌机/道具工厂
│       ├── strategy/             # 射击策略
│       ├── observer/             # 炸弹道具观察者
│       ├── dao/                  # 本地排行榜 DAO
│       └── audio/                # 音效与 BGM 管理
├── SocketServer/
│   └── SocketServer/
│       └── src/main/java/com/example/socketserver/MyClass.java
├── legacy-desktop/               # 旧桌面版归档
├── gradle/                       # Gradle 版本与依赖声明
└── README.md
```

## 快速开始

### 1. 构建 Android 客户端

在仓库根目录执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

或直接用 Android Studio 打开仓库根目录，等待 Gradle Sync 完成后运行 `app` 模块。

### 2. 启动 Socket 服务端

联机模式和联机排行榜需要先启动服务端。服务端入口为：

```text
SocketServer/SocketServer/src/main/java/com/example/socketserver/MyClass.java
```

Windows PowerShell 示例：

```powershell
cd SocketServer\SocketServer
javac -encoding UTF-8 src\main\java\com\example\socketserver\MyClass.java
java -cp src\main\java com.example.socketserver.MyClass
```

服务端默认监听 `9999` 端口，并在当前运行目录下读写 `rankings.json`。

### 3. 运行联机模式

1. 启动 Socket 服务端。
2. 启动两个 Android 客户端实例。
3. 在首页输入不同的 `userId`。
4. 点击“联机模式”进入匹配。
5. 两个客户端都收到匹配结果后开始游戏。

当前 Android 客户端中的服务端地址为 `10.0.2.2:9999`，适用于 Android 模拟器访问宿主机。如果使用真机，请把以下两个文件中的 `SERVER_HOST` 改为电脑在同一局域网内的 IP：

- `app/src/main/java/edu/hitsz/application/OnlineGame.java`
- `app/src/main/java/edu/hitsz/LeaderboardActivity.java`

## 游戏模式

| 模式 | 敌机上限 | Boss 出现分数 | 英雄机射击周期 | 精英敌机概率 | 精英 Plus 概率 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 简单 | 5 | 900 | 180 | 0.20 | 0.05 |
| 普通 | 7 | 700 | 220 | 0.30 | 0.10 |
| 困难 | 9 | 500 | 260 | 0.35 | 0.15 |
| 联机 | 继承普通模式 | 继承普通模式 | 继承普通模式 | 继承普通模式 | 继承普通模式 |

联机模式继承 `NormalGame` 的基础规则，但在匹配成功前不会推进游戏逻辑，避免单端提前开局。

## 排行榜规则

### 本地排行榜

- 仅用于简单、普通、困难单机模式。
- 数据写入 Android 本地 SQLite。
- 每次游戏成绩都会保留为独立记录。
- 不按 `userId` 合并。
- 支持长按删除单条记录。
- 支持清空全部记录。

### 联机排行榜

- 仅用于联机模式。
- 数据由 Socket 服务端维护，客户端通过 `GET_RANKING` 拉取。
- 服务端每局保存两条记录：玩家 A 视角和玩家 B 视角。
- 同一 `userId` 只展示最高分。
- 同分时保留最近成绩。
- 最多返回 Top10。
- 客户端只展示联机榜，不提供删除服务器数据的入口。

## Socket 协议

通信采用 UTF-8 行文本协议，每条消息以换行结束。

### Client -> Server

| 消息 | 说明 |
| --- | --- |
| `JOIN:<userId>` | 进入联机匹配队列 |
| `SCORE:<value>` | 上报当前分数 |
| `GAME_OVER` | 上报本端游戏结束 |
| `GET_RANKING` | 短连接请求联机排行榜 |
| `bye` | 主动断开连接 |

### Server -> Client

| 消息 | 说明 |
| --- | --- |
| `INFO:WAITING` | 已进入队列，等待匹配 |
| `MATCH:<opponentUserId>` | 匹配成功，并返回对手 ID |
| `OPP_SCORE:<value>` | 对手分数更新 |
| `OPP_GAME_OVER` | 对手游戏结束 |
| `ERROR:<message>` | 协议或参数错误 |

## 联机对战流程

1. 客户端连接服务端并发送 `JOIN:<userId>`。
2. 服务端将玩家放入 FIFO 等待队列。
3. 队列中出现两个有效连接后，服务端创建一场 `MatchSession`。
4. 双方收到 `MATCH` 后，客户端开始推进游戏逻辑。
5. 游戏中客户端在分数变化时发送 `SCORE`，服务端只转发给同房间对手。
6. 任一方结束后发送 `GAME_OVER`，服务端通知另一方 `OPP_GAME_OVER`。
7. 双方均结束后，客户端进入结算页，服务端将对局结果追加到 `rankings.json`。

## 关键实现

- `BaseGame`：Android `SurfaceView` 游戏循环、绘制、触摸控制、碰撞检测和公共结算流程。
- `EasyGame` / `NormalGame` / `HardGame`：按模式覆盖背景、敌机上限、Boss 阈值、射击周期和敌机概率。
- `OnlineGame`：连接 Socket 服务端，处理匹配、分数同步和双端结束结算。
- `LeaderboardActivity`：根据模式切换本地 SQLite 榜和联机服务端榜。
- `SQLiteScoreDaoImpl`：本地排行榜增删查。
- `MyClass`：服务端主入口，负责匹配、转发、排行榜落盘和 Top10 JSON 返回。

## 构建与测试

构建 Debug APK：

```powershell
.\gradlew.bat :app:assembleDebug
```

运行本地单元测试：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

运行 Android 仪器测试需要连接模拟器或真机：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 常见问题

### 联机模式一直显示 `MATCHING...`

- 确认服务端已经启动并监听 `9999` 端口。
- 至少需要两个客户端同时进入联机模式。
- 模拟器访问电脑本机使用 `10.0.2.2`；真机需要改成电脑局域网 IP。
- 检查电脑防火墙是否允许 Java 进程监听和接收连接。

### 联机排行榜为空

- 只有联机对局双方都结束后，服务端才会写入 `rankings.json`。
- 确认客户端请求的服务端地址与运行中的服务端一致。
- 确认服务端运行目录下存在可写的 `rankings.json`，或当前目录允许创建该文件。

### 本地排行榜和联机排行榜数据不一致

这是预期行为。单机榜使用 Android 本地 SQLite，联机榜使用服务端 `rankings.json`，两者互不覆盖。

## 维护说明

- `SocketServer/rankings.json` 和 `SocketServer/SocketServer/rankings.json` 是运行数据，提交前请确认是否需要保留。
- `legacy-desktop/` 不参与当前 Android 构建，除非明确维护旧桌面版，否则不要把新功能写入该目录。
- 修改联机协议时，需要同步更新 `OnlineGame`、`LeaderboardActivity` 和 `MyClass`。
- 修改排行榜规则时，需要同时检查本地 DAO 和服务端 `buildRankingJson()` 的语义是否仍然一致。
