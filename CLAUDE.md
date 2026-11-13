# CLAUDE.md

> Claude Code 项目入口（启动时自动读取）。本文件只做**导航 + 硬约束 + 实操指引**。
> 需求 / 高德细节在 `docs/`，**按需读取**——不要用 `@` 把全文 import 进上下文（浪费 token）。

## 1. 项目一句话

「沿途」——输入 城市+起点+终点+关键词（MVP 只做"咖啡"），返回沿途按「绕路直线距离」排序的 Top 3 POI。不是导航、不是点评、无用户系统。

## 2. 技术栈与包名

JDK 21 · Spring Boot 4.1.2 · Maven · H2（文件模式）· 高德 API · 无前端（Postman 自测）
包名 `com.zzl.waypointbackend`（勿改）
外部 HTTP 调用：**Feign（OpenFeign，声明式客户端）**，配 Spring Cloud **2025.1.x（Oakwood）**
持久层：`mapper` 包先留空，MVP 不引 ORM 框架
> Spring Boot **保持 4.1.2 不要降**（3.5 已临近 EOL，4.x 是当前主流）。加 Feign 依赖时引 Spring Cloud BOM，小版本号以官方对应表核对。

## 3. 工作方式（最重要，先读这条）

1. **先分析，不要急着写代码**：接到需求先读懂、想清楚再动手。
2. **告诉我你的想法**：方案、思路、会影响哪些文件。
3. **把待确定的事项都列出来跟我讨论**：选型、命名、边界、取舍，一个都别自己拍脑袋。
4. **等我说「开始改代码」，才动手写代码**。在此之前只讨论、不新建/修改任何代码文件。

## 4. 目录结构与分层职责

```
src/main/java/com/zzl/waypointbackend/
├── WaypointBackendApplication.java   # 启动类（已存在）
├── config/        # 配置类（@ConfigurationProperties：AmapProperties、AlongProperties）
├── constant/      # 常量（高德端点、错误码、业务常量）
├── controller/    # 接口层：AlongController（GET /along）
├── service/       # 业务接口：AlongService 等
│   └── impl/      # 业务实现类
├── feignclient/   # 外部服务调用层（高德 API，Feign 声明式客户端）
├── mapper/        # 数据访问层（MVP 暂无表，先留空）
├── dto/           # 请求 / 内部传输对象（含算法内部对象 Point、PoiCandidate）
├── vo/            # 返回给调用方的对象（AlongResponse 等）
├── exception/     # BizException、GlobalExceptionHandler、统一错误响应体
└── utils/         # 工具类（DetourCalculator 等）
```

## 5. 编码约定

- 配置一律读 `along.*` / `amap.*`（`@ConfigurationProperties`），勿硬编码
- 外部调用（高德）统一收口到 `feignclient` 层，用 **Feign 声明式客户端**，业务代码不直接发 HTTP
- 数据传输：入参 / 内部用 `dto`，返回给调用方用 `vo`
- 业务错误抛 `BizException`，配合全局异常处理返回统一错误结构（400/404/502）
- MVP 串行调高德，接受 5-10s，不提前上并发

## 6. 开工前必读（先读后写）

1. `docs/01-需求文档.md` —— 需求基线：用户故事、验收标准、算法口径、决策记录
2. `docs/02-高德API.md` —— 高德对接：接口、参数、key 管理、错误码（**开头有 MVP 接口速查表**）
3. `docs/03-接口文档.md` —— 概要设计 + 详细设计：接口字段定义、类设计、配置项、异常处理

## 7. 当前进度与下一步

已完成：骨架、依赖（web+H2）、`along.*` 配置、key 注入、包结构（10 个包）、文档体系（01需求/02高德/03接口）。
建议推进顺序（**每一步都要先走第 3 节的讨论流程，我说开始才写**）：
1. `AmapProperties` / `AlongProperties`（读配置）
2. 高德定位封装（Feign，先只做 place/text，验证高德能调通）
3. `DetourCalculator`（纯几何 + 单测，不依赖高德）
4. `AlongService` + `AlongController`（串起 `/along`）

## 8. 红线（违反立刻停手问我）

1. 不加 MVP 范围外功能（登录/评分/前端/并发）
2. 不换技术栈、不改包名、不擅自加 starter / 框架（Feign 已批准；MyBatis 等其他框架仍需先问我）
3. 真实高德 key 只在 `application-local.yml`（git 忽略），绝不入 git
4. **未经我明确说「开始改代码」，不写/不改任何代码文件**
5. scope 模糊先问再写
6. 小步提交，一个功能一个 commit

## 9. 常见坑

- 起点/终点定位用 **place/text**，**不要用 geocode**（对"奥森公园"报 30001）
- Windows 控制台中文乱码是 GBK 显示问题，文件本身 UTF-8 正常，不用改文件
