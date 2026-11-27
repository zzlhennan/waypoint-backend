# 高德 API 对接文档

> 本文档记录「沿途」项目用到的**高德开放平台 API**。
> 主体业务用 **v5 2.0**，地名/城市联想用 **v3**（inputtips 天生为联想设计；district 行政区查询只有 v3）。
> 编码涉及高德调用以本文为准；更详细字段以官方文档为准（链接见文末）。

---

## 0. 接口速查（先看这个）

| 用途 | 接口 | 端点 |
|---|---|---|
| 起点/终点定位（地名→坐标） | 关键字搜索 POI 2.0 | `v5/place/text` |
| A→B 路径坐标串 | 路径规划 2.0（按 mode 选） | `v5/direction/{driving\|walking\|bicycling}` |
| 采样点附近搜「咖啡」 | POI 周边搜索 2.0 | `v5/place/around` |
| 地名联想（起点/终点输入框） | 输入提示 | `v3/assistant/inputtips` |
| 城市联想（城市输入框） | 行政区划查询 | `v3/config/district` |

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
| 接口版本 | 主体 **v5 2.0**；联想类 **v3**（inputtips / district） |

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

## 3. 接口清单

### 3.1 关键字搜索 POI 2.0（place/text）—— 定位用

| 项 | 内容 |
|---|---|
| 用途 | 把「奥森公园」「望京SOHO」等地名转为经纬度 |
| URL | `GET https://restapi.amap.com/v5/place/text` |
| 关键参数 | `keywords={地名}`、**`region={城市}`**、**`city_limit=true`**（`key` 由拦截器自动注入） |
| 失败处理 | `pois` 为空 → 业务 400「找不到起点/终点」 |

> ⚠️ **v5 城市入参改名**：`region`（≠v3 `city`）、`city_limit`（≠v3 `citylimit`）。旧名 `city`/`citylimit` 向后兼容（实测也能返回），但**建议用官方名**。`region` 是"区域内加权"，严格限城市需搭配 `city_limit=true`。
> 实测：「奥森公园」`pois[0]` = 奥林匹克森林公园 `116.392159,40.018635`，定位正常。
> ⚠️ 用 place/text 而非 geocode：geocode 只认结构化地址，对地名简称报 30001。

**返回字段**（对应 DTO `AmapV5PoiResponse`）：

| 字段 | 含义 | 本项目用途 |
|---|---|---|
| `status` | "1"=成功，其他=失败 | 判断调用是否成功 |
| `info` / `infocode` | 结果描述 / 结果编码 | 失败时排查用 |
| `pois[]` | POI 列表 | 取 `pois[0]` 定位 |

**pois[] 每项字段**：

| 字段 | 含义 | 本项目用途 |
|---|---|---|
| `id` | 高德 POI 唯一 id | 候选去重 |
| `name` | POI 名（如"奥林匹克森林公园"） | 结果展示 |
| `location` | "经度,纬度"（如 "116.392,40.018"） | 定位取坐标 |
| `address` | 地址 | 结果展示 |
| `type` | 类型中文，分号分级（如"餐饮服务;咖啡厅;星巴克咖啡"） | 截 `[1]` 得二级分类 |
| `typecode` | 类型编码（如"050501"） | 取前 4 位聚合二级分类 |
| `adname` | 区县名（如"朝阳区"） | 结果展示 |
| `business.tel` | 电话（需 `show_fields=business` 才返回，嵌套子对象） | 结果展示，前端拼 `tel:` 链接 |

---

### 3.2 POI 周边搜索 2.0（place/around）—— 采样点搜 POI 用

| 项 | 内容 |
|---|---|
| 用途 | 在每个采样点附近搜「咖啡」等 POI |
| URL | `GET https://restapi.amap.com/v5/place/around` |
| 关键参数 | `keywords={关键词}`、`location={经度,纬度}`、`radius={米}`、`types`（`key` 由拦截器自动注入） |
| 字段控制 | `show_fields=business` 才返回 `business` 子对象（含 tel） |
| 备注 | 半径由 `along.search-radius-meters` 驱动；全程空结果时整体用 `search-radius-retry-meters` 补搜一轮（**不做单采样点独立重试**——避免 QPS 翻倍） |

**返回字段**与 3.1 共用同一个 DTO `AmapV5PoiResponse`（结构一致），不再重复列。

> ⚠️ **两个 distance 别混**：around 返回的 `poi.distance` = POI→**采样点**（我们**不用**）；我们自己算的 `distanceToRouteMeters` = POI→**整条折线**。

---

### 3.3 路径规划 2.0（direction）—— 路线坐标用

| 项 | 内容 |
|---|---|
| 用途 | A→B 路径坐标串（算法采样的基础） |
| URL | `GET https://restapi.amap.com/v5/direction/{driving\|walking\|bicycling}`（按 mode 选） |
| 关键参数 | `origin={经度,纬度}`、`destination={经度,纬度}`；驾车可加 `strategy`（`key` 由拦截器自动注入） |
| **⚠️ polyline 必传** | **默认 steps 只有 `instruction`/`orientation`/`step_distance`/`road_name`，没有坐标串！** 必须传 **`show_fields=polyline`** 才返回 `polyline` |
| 返回 | `route.paths[]`（默认 3 条备选），取 `paths[0].steps[].polyline`，解析成 `List<Point>` |

> ✅ 实测确认（v5 driving 奥森→望京SOHO）：默认 `steps[]` 无 polyline；传 **`show_fields=polyline`** 后每个 step 返回 `polyline`。
> **polyline 格式**：`"116.392067,40.01889;116.391713,40.018955;..."`——按 `;` 分割成点，每点按 `,` 分割为 `经度,纬度`，解析成 `List<Point>`。

**返回字段**（对应 DTO `AmapV5RouteResponse`）：

| 字段 | 含义 | 本项目用途 |
|---|---|---|
| `status` | "1"=成功 | 判断调用是否成功 |
| `info` / `infocode` | 结果描述 / 结果编码 | 失败排查 |
| `route.paths[]` | 备选路线列表（默认 3 条） | 取 `paths[0]` |
| `route.paths[].steps[]` | 一条路线的分段列表 | 遍历每段 |
| `route.paths[].steps[].polyline` | 该段的坐标串（需 show_fields=polyline） | 解析成 List<Point> |

---

### 3.4 输入提示（inputtips，v3）—— 地名联想用

| 项 | 内容 |
|---|---|
| 用途 | 地名联想弹窗（输"奥森"弹"奥森公园南门/奥森停车场"） |
| URL | `GET https://restapi.amap.com/v3/assistant/inputtips` |
| 官方文档 | https://lbs.amap.com/api/webservice/guide/api-advanced/inputtips |
| 关键参数 | `keywords={输入}`、`datatype=poi`、`region={城市}`（可选）、`citylimit=true`（city 非空时严格限城市） |
| datatype 可选值 | `all`（全部）、`poi`（POI）、`bus`（公交站）、`busline`（公交线路）——**注意：没有 district** |
| ⚠️ v3 城市入参 | v3 用 **`citylimit`**（无下划线），≠v5 的 `city_limit`。参数名别抄错 |
| 与 place/text 区别 | inputtips 天生为联想设计，返回更短更准；place/text 是精确搜索 |

**返回字段**（对应 DTO `AmapInputtipsResponse`）：

| 字段 | 含义 | 本项目用途 |
|---|---|---|
| `status` | "1"=成功 | 判断调用是否成功 |
| `tips[]` | 候选列表 | 渲染联想弹窗 |

**tips[] 每项字段**：

| 字段 | 含义 | 本项目用途 |
|---|---|---|
| `name` | 候选名（如"奥林匹克森林公园南门"） | 填入输入框 |
| `location` | "经度,纬度" | （预留，弹窗暂不展示） |
| `address` | 地址 | 弹窗辅助展示 |
| `district` | 所属区县 | （预留） |

---

### 3.5 行政区划查询（district，v3）—— 城市联想用

| 项 | 内容 |
|---|---|
| 用途 | 城市联想弹窗（输"北"返回北京市/北海市） |
| URL | `GET https://restapi.amap.com/v3/config/district` |
| 官方文档 | https://lbs.amap.com/api/webservice/guide/api/district |
| 关键参数 | `keywords={输入}`、`subdistrict=0`（**不返回下一级区县**，只留当前级） |
| 模糊匹配 | **支持**：输"北"返回北京市/北海市等（已实测） |

**返回字段**（对应 DTO `AmapDistrictResponse`）：

| 字段 | 含义 | 本项目用途 |
|---|---|---|
| `status` | "1"=成功 | 判断调用是否成功 |
| `districts[]` | 行政区候选列表 | 渲染联想弹窗 |

**districts[] 每项字段**：

| 字段 | 含义 | 本项目用途 |
|---|---|---|
| `name` | 行政区名（如"北京市"） | 填入输入框 |
| `adcode` | 行政区划代码（6 位数字，如 110000） | （预留） |
| `level` | 层级：province / city / district | 弹窗辅助展示（告诉用户是省还是市） |
| `center` | 城市中心"经度,纬度" | （预留） |

---

## 4. DTO 设计汇总

项目共 **4 个高德响应 DTO**：

| DTO | 覆盖接口 | 结构要点 |
|---|---|---|
| `AmapV5PoiResponse` | place/text + place/around | `status`/`info`/`infocode` + `pois[]`；POI 含 `business` 子对象（show_fields=business 时） |
| `AmapV5RouteResponse` | driving/walking/bicycling | `route.paths[].steps[].polyline` 坐标串（需 show_fields=polyline，已实测） |
| `AmapInputtipsResponse` | inputtips v3 | `status` + `tips[]`（name/location/address/district） |
| `AmapDistrictResponse` | district v3 | `status` + `districts[]`（name/adcode/level/center） |

- **定位 + 周边共用 `AmapV5PoiResponse`**（搜索 2.0 同结构）
- **路径各方式共用 `AmapV5RouteResponse`**
- v3 两个联想接口各自独立 DTO（字段结构与 v5 不同，不强行共用）
- 字段控制统一 `show_fields`：POI 要 `tel` 传 `business`，路径要坐标串传 `polyline`

---

## 5. 错误码与异常约定

- 高德返回 `status != "1"` → 包装成业务异常抛出
- 业务侧错误映射：
  | HTTP | 触发条件 |
  |---|---|
  | 400 | 起点/终点定位失败（`pois` 为空） |
  | 404 | 整条路线 2km 内无任何 POI |
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
- 输入提示（inputtips）：https://lbs.amap.com/api/webservice/guide/api-advanced/inputtips
- 行政区划查询（district）：https://lbs.amap.com/api/webservice/guide/api/district
- 错误码说明：https://lbs.amap.com/api/webservice/guide/tools/info

---

*唯一事实源 · 位于 waypoint-backend/docs/ · 主体 v5 2.0 + 联想类 v3*