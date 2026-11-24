# 高德 API 对接文档

> 本文档记录「沿途」项目用到的**高德开放平台 API**。
> ✅ **全站统一 v5 2.0**，v3 接口一律不用。
> 编码涉及高德调用以本文为准；更详细字段以官方文档为准（链接见文末）。

---

## 0. MVP 接口速查（先看这个）

| 用途 | 接口 | 端点（全 v5） |
|---|---|---|
| 起点/终点定位（地名→坐标） | 关键字搜索 POI 2.0 | `v5/place/text` |
| A→B 路径坐标串 | 路径规划 2.0（按 mode 选） | `v5/direction/{driving\|walking\|bicycling}` |
| 采样点附近搜「咖啡」 | POI 周边搜索 2.0 | `v5/place/around` |

> 定位**不要用 geocode**（对地名简称报 30001）。
> ⚠️ **路径要坐标串必须传 `show_fields=polyline`**（默认 steps 不返回坐标）。

---

## 1. 概述

| 项 | 值 |
|---|---|
| 平台 | 高德开放平台（console.amap.com） |
| 服务类型 | Web 服务 API（申请 key 时服务平台必须选「Web 服务」） |
| 调用方式 | HTTP GET，返回 JSON |
| HTTP 客户端 | Feign（OpenFeign，声明式，已拍板） |
| 调用模式 | MVP 串行调用，接受 5-10s 响应，不提前上并发 |
| 接口版本 | **全部 v5 2.0**（v3 不用） |

---

## 2. Key 管理（安全红线）

| 文件 | 是否入 git | 内容 |
|---|---|---|
| `application.yml` | ✅ 入 git | `amap.keys: [${AMAP_KEY_1:}, ${AMAP_KEY_2:}, ${AMAP_KEY_3:}]` 占位，无真实 key |
| `application-local.yml` | ❌ **git 忽略** | 真实 keys 列表，仅本地 |

- 本地开发默认激活 `local` profile，自动读 local 文件，IDEA 点运行即可
- 部署时走环境变量 `AMAP_KEY_1` / `AMAP_KEY_2` / `AMAP_KEY_3`
- **fail-fast**：`keys` 列表为空或元素全空白时启动期报错
- **绝不把真实 key 写进任何入 git 的文件**
- 建议：高德控制台为 key 开启 **IP 白名单**，防止泄露被盗用

> **多 key 轮询（QPS 规避）**：免费版单 key 限 3 QPS，一次 `/along` 会连发十几次请求，容易触发 10021 限流。
> 因此 `amap.keys` 是**列表**，`AmapKeyInterceptor` 用 `AtomicInteger` 取模轮询分散压力；
> 同时每个 around 调用后 `sleep 200ms`（见 `AlongServiceImpl.sleepBeforeNext`）把突发请求摊到几秒内。

> **调用时 key 如何注入**：Java 方法签名里**不带 key**，由 `AmapKeyInterceptor`（Feign `RequestInterceptor`）统一在每个请求 URL 后追加 `?key=...`。
> 该拦截器通过 `AmapFeignConfig` **以 per-client 方式绑定到 AmapClient**（`@FeignClient(configuration=...)`），
> **故意不给 `AmapFeignConfig` 加 `@Configuration`**——否则会被组件扫描扫到主上下文，拦截器变成全局，污染未来其他 Feign 客户端。

---

## 3. 接口清单（全 v5 2.0）

### 3.1 关键字搜索 POI 2.0（place/text）—— 定位用

| 项 | 内容 |
|---|---|
| 用途 | 把「奥森公园」「望京SOHO」等地名转为经纬度 |
| URL | `GET https://restapi.amap.com/v5/place/text` |
| 关键参数 | `keywords={地名}`、**`region={城市}`**、**`city_limit=true`**（`key` 由拦截器自动注入） |
| 返回 | `pois[]`，取 **`pois[0].location`**（`"经度,纬度"`） |
| 失败处理 | `pois` 为空 → 业务 400「找不到起点/终点」 |

> ⚠️ **v5 城市入参改名**：`region`（≠v3 `city`）、`city_limit`（≠v3 `citylimit`）。旧名 `city`/`citylimit` 向后兼容（实测也能返回），但**建议用官方名**。`region` 是"区域内加权"，严格限城市需搭配 `city_limit=true`。
> 实测：「奥森公园」`pois[0]` = 奥林匹克森林公园 `116.392159,40.018635`，定位正常。
> ⚠️ 用 place/text 而非 geocode：geocode 只认结构化地址，对地名简称报 30001。

### 3.2 POI 周边搜索 2.0（place/around）—— 采样点搜 POI 用

| 项 | 内容 |
|---|---|
| 用途 | 在每个采样点附近搜「咖啡」等 POI |
| URL | `GET https://restapi.amap.com/v5/place/around` |
| 关键参数 | `keywords={关键词}`、`location={经度,纬度}`、`radius={米}`、`types`（`key` 由拦截器自动注入） |
| 字段控制 | `show_fields`：默认返回 `name`/`address`/`location`/`type`/`typecode`/`adname`/`pname`/`cityname`/`distance` 等（**`type`/`adname` 默认就有**）；加 **`show_fields=business`** 才多 `business` 子对象 |
| 返回 | `pois[]`；⚠️ **`tel`/`rating`/`cost`/营业时间 在 `poi.business.*`（嵌套子对象，非平铺）**，需 `show_fields=business` |
| 备注 | 半径由 `along.search-radius-meters` 驱动；全程空结果时整体用 `search-radius-retry-meters` 补搜一轮（**不做单采样点独立重试**——避免 QPS 翻倍） |

### 3.3 路径规划 2.0（direction）—— 路线坐标用

| 项 | 内容 |
|---|---|
| 用途 | A→B 路径坐标串（算法采样的基础） |
| URL | `GET https://restapi.amap.com/v5/direction/{driving\|walking\|bicycling}`（按 mode 选） |
| 关键参数 | `origin={经度,纬度}`、`destination={经度,纬度}`；驾车可加 `strategy`（`key` 由拦截器自动注入） |
| **⚠️ polyline 必传** | **默认 steps 只有 `instruction`/`orientation`/`step_distance`/`road_name`，没有坐标串！** 必须传 **`show_fields=polyline`** 才返回 `polyline` |
| 返回 | `route.paths[]`（默认 3 条备选，`count=3`），取 `paths[0].steps[].polyline`，解析成 `List<Point>` |

> ✅ 实测确认（v5 driving 奥森→望京SOHO）：默认 `steps[]` 无 polyline；传 **`show_fields=polyline`** 后每个 step 返回 `polyline`。
> **polyline 格式**：`"116.392067,40.01889;116.391713,40.018955;..."`——按 `;` 分割成点，每点按 `,` 分割为 `经度,纬度`，解析成 `List<Point>`。

---

## 4. 全 v5 统一说明（DTO 设计）

只需 **2 个响应 DTO**：

| DTO | 覆盖接口 | 结构要点 |
|---|---|---|
| `AmapV5PoiResponse` | place/text + place/around | `status`/`info`/`infocode` + `pois[]`；POI 含 `business` 子对象（show_fields=business 时） |
| `AmapV5RouteResponse` | driving/walking/bicycling | `route.paths[].steps[].polyline` 坐标串（需 show_fields=polyline，已实测） |

- **定位 + 周边共用 `AmapV5PoiResponse`**（搜索 2.0 同结构）
- **路径各方式共用 `AmapV5RouteResponse`**
- 字段控制统一 `show_fields`：POI 要 `tel` 传 `business`，路径要坐标串传 `polyline`

---

## 5. 错误码与异常约定

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
| 30001 | ENGINE_RESPONSE_DATA_ERROR | geocode 遇地名简称触发 | 定位用 place/text，不用 geocode |
| 10001 | INVALID_USER_KEY | key 错误/过期 | 启动 fail-fast 检查 |
| 10009 | USERKEY_PLAT_NOMATCH | key 平台不符 | 申请时选「Web 服务」 |

---

## 6. 官方文档链接

- 搜索 POI 2.0（place/text、around）：https://lbs.amap.com/api/webservice/guide/api-advanced/newpoisearch
- 路径规划 2.0（direction）：https://lbs.amap.com/api/webservice/guide/api/newroute
- 错误码说明：https://lbs.amap.com/api/webservice/guide/tools/info

---

*唯一事实源 · 位于 waypoint-backend/docs/ · 全站统一 v5 2.0*
