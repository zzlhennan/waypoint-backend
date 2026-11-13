# 高德 API 对接文档

> 本文档专门记录「沿途」项目用到的**高德开放平台 API**。
> 需求文档 / CLAUDE.md / AGENTS.md 不再重复这些内容，统一指向本文。
> 编码涉及高德调用以本文为准；更详细字段以官方文档为准（链接见文末）。

---

## 0. MVP 接口速查（先看这个）

| 用途 | MVP 用哪个 | 端点 |
|---|---|---|
| 起点/终点定位（地名→坐标） | **关键字搜索 POI** | `v3/place/text` |
| A→B 路径坐标串 | **驾车路线规划** | `v3/direction/driving` |
| 采样点附近搜「咖啡」 | **POI 周边搜索** | `v5/place/around` |

> 其余接口（输入提示、地理/逆地理编码、路径规划 2.0、其他交通方式）为**备选/参考**，
> MVP 阶段不接，记录下来供后续升级用。⚠️ 定位**不要用 geocode**（对地名简称报 30001）。

---

## 1. 概述

| 项 | 值 |
|---|---|
| 平台 | 高德开放平台（console.amap.com） |
| 服务类型 | Web 服务 API（申请 key 时服务平台必须选「Web 服务」） |
| 调用方式 | HTTP GET，返回 JSON |
| HTTP 客户端 | Feign（OpenFeign，声明式，已拍板） |
| 调用模式 | MVP 串行调用，接受 5-10s 响应，不提前上并发 |

---

## 2. Key 管理（安全红线）

| 文件 | 是否入 git | 内容 |
|---|---|---|
| `application.yml` | ✅ 入 git | `amap.key: ${AMAP_KEY:}` 占位，无真实 key |
| `application-local.yml` | ❌ **git 忽略** | 真实 key，仅本地 |

- 本地开发默认激活 `local` profile，自动读 local 文件，IDEA 点运行即可
- 部署时走环境变量 `AMAP_KEY`
- **fail-fast**：key 未配置时启动期就报错
- **绝不把真实 key 写进任何入 git 的文件**
- 建议：高德控制台为 key 开启 **IP 白名单**，防止泄露被盗用

---

## 3. 接口清单

### 3.1 搜索定位类

#### ① 关键字搜索 POI（place/text）—— MVP 定位用

| 项 | 内容 |
|---|---|
| 用途 | 把「奥森公园」「望京SOHO」等地名转为经纬度 |
| URL | `GET https://restapi.amap.com/v3/place/text` |
| 关键参数 | `keywords={地名}`、`city={城市}`、`citylimit=true`、`key` |
| 返回 | `pois[]`，取 **`pois[0].location`**（`"经度,纬度"`） |
| 失败处理 | `pois` 为空 → 业务 400「找不到起点/终点」 |

> ⚠️ 用 place/text 而非 geocode：geocode 只认结构化地址，对「奥森公园」类简称报 30001（已实测）。

#### ② 输入提示（inputtips）—— 备选，搜索联想用

| 项 | 内容 |
|---|---|
| 用途 | 搜索框联想：用户输入几个字，返回候选 POI 列表 |
| URL | `GET https://restapi.amap.com/v3/assistant/inputtips` |
| 关键参数 | `keywords={输入}`（必填）、`city`、`citylimit`、`datatype`、`key` |
| 返回 | `tips[]`（含 `name`、`district`、`adcode`、`location`、`id`、`address`） |
| 备注 | 若将来做前端输入联想，可用它替代 place/text 做定位 |

#### ③ POI 周边搜索（place/around）—— MVP 采样点搜 POI 用

| 项 | 内容 |
|---|---|
| 用途 | 在每个采样点附近搜「咖啡」等 POI |
| URL | `GET https://restapi.amap.com/v5/place/around` |
| 关键参数 | `keywords={关键词}`、`location={经度,纬度}`、`radius={米}`、`key` |
| 返回 | `pois[]`（含 `name`、`address`、`location`） |
| 备注 | 半径由 `along.search-radius-meters` 驱动；搜不到按 PRD 扩到 1km 重试一次 |

### 3.2 地理 / 逆地理编码 —— 参考

#### ④ 地理编码（geocode/geo）

| 项 | 内容 |
|---|---|
| 用途 | **结构化地址** → 经纬度（如「北京市朝阳区阜通东大街6号」） |
| URL | `GET https://restapi.amap.com/v3/geocode/geo` |
| 关键参数 | `address={结构化地址}`（必填）、`city`、`key` |
| 返回 | `geocodes[]`（含 `formatted_address`、`location`、`level`） |
| 备注 | ⚠️ 只认结构化地址，对地名简称报 30001，**MVP 不用于定位** |

#### ⑤ 逆地理编码（geocode/regeo）

| 项 | 内容 |
|---|---|
| 用途 | 经纬度 → 地址描述（坐标反查「这是哪」） |
| URL | `GET https://restapi.amap.com/v3/geocode/regeo` |
| 关键参数 | `location={经度,纬度}`（必填）、`radius`、`extensions`（`base`/`all`）、`key` |
| 返回 | `regeocode`（含 `formatted_address`、`addressComponent`；`extensions=all` 时含 `pois`/`roads`） |
| 备注 | 可用于把采样点/POI 坐标转成可读地址 |

### 3.3 路径规划（v3）—— MVP 用驾车

| 交通方式 | 端点 | 说明 |
|---|---|---|
| 步行 | `GET https://restapi.amap.com/v3/direction/walking` | 100km 内 |
| **驾车（MVP）** | `GET https://restapi.amap.com/v3/direction/driving` | A→B 路径坐标串 |
| 公交 | `GET https://restapi.amap.com/v3/direction/transit/integrated` | 需 `city`/`cityd` |
| 骑行 | `GET https://restapi.amap.com/v3/direction/riding` | 建议直接用 2.0 |

**驾车关键参数**：`origin={经度,纬度}`、`destination={经度,纬度}`、`key`
**驾车返回**：`route.paths[].steps[].polyline`，坐标串需解析成 `List<Point>`

### 3.4 路径规划 2.0（v5）—— 备选升级

2.0 支持**驾车、公交、步行、骑行、电动车**，返回更简洁、支持多途经点、车牌规避限行、多算路策略。

| 交通方式 | 端点 |
|---|---|
| 驾车 | `GET https://restapi.amap.com/v5/direction/driving` |
| 步行 | `GET https://restapi.amap.com/v5/direction/walking` |
| 骑行 | `GET https://restapi.amap.com/v5/direction/bicycling` |
| 电动车 | `GET https://restapi.amap.com/v5/direction/electrobike` |
| 公交 | `GET https://restapi.amap.com/v5/direction/transit/integrated` |

**关键参数**：`origin`、`destination`、`key`（经度在前纬度在后，小数点后不超 6 位）
**备注**：参数过长时驾车 2.0 可改用 POST。若未来从驾车切骑行（开发者骑车场景），优先用 2.0 的 `bicycling`。

---

## 4. 错误码与异常约定

- 高德返回 `status != "1"` → 包装成业务异常抛出
- 业务侧错误映射：
  | HTTP | 触发条件 |
  |---|---|
  | 400 | 起点/终点定位失败（`pois` 为空） |
  | 404 | 整条路线 1km 内无任何 POI |
  | 502 | 高德 API 调用失败 / 网络异常 |

**实测踩坑记录**：
| infocode | info | 含义 | 本项目对策 |
|---|---|---|---|
| 30001 | ENGINE_RESPONSE_DATA_ERROR | 参数问题（geocode 遇地名简称触发） | 定位改用 place/text |
| 10001 | INVALID_USER_KEY | key 错误/过期 | 启动 fail-fast 检查 |
| 10009 | USERKEY_PLAT_NOMATCH | key 平台不符 | 申请时选「Web 服务」 |

---

## 5. 官方文档链接

- 搜索 POI / 搜索 POI 2.0：https://lbs.amap.com/api/webservice/guide/api-advanced/newpoisearch
- 输入提示：https://lbs.amap.com/api/webservice/guide/api-advanced/inputtips
- 地理/逆地理编码：https://lbs.amap.com/api/webservice/guide/api/georegeo
- 路径规划（v3）：https://lbs.amap.com/api/webservice/guide/api/direction
- 路径规划 2.0（v5）：https://lbs.amap.com/api/webservice/guide/api/newroute
- 错误码说明：https://lbs.amap.com/api/webservice/guide/tools/info

---

*唯一事实源 · 位于 waypoint-backend/docs/ · 高德对接相关只维护这一份*
