# MVP 收尾清单

> 目的：把已锁定的 **PRD v1.3** 做扎实，**不引入新需求范围**。
> 三项收尾：开放预留参数 / edge case 验证 / 启动 README。
> 每项完成后勾选 `[x]`，不另起 PRD；做完本清单即代表 MVP 真正交付。
>
> **执行顺序**：先 ① 开放参数（有真实代码改动）→ 再 ② edge case 验证（可顺便把新参数的错误路径一起跑）→ 最后 ③ README（贴定稿后的真实返回）。

---

## 1. 开放预留参数 + 分类字典（types / strategy）

**现状**（03 接口文档 7.1 已标注）：Feign 方法签名已预留 `types`/`strategy`，但 `AlongServiceImpl` 写死传 `null`。
**目标**：打通 types 透传 + 新增"分类字典"接口，支撑未来前端的"分类 tab"（大众点评模式）。

### 1.1 设计（已和开发者确认）

**核心原则：keyword 是"搜索词"，types 是"过滤器"，两者独立、不自动耦合。**

用户体验流程（未来有前端时）：
```
搜索框输入"咖啡"          → keyword=咖啡，不加 category → 返回全部（含奶茶店）
   ↓
结果上方 tab：[全部] [咖啡厅] [冷饮] [糕饼] [加油站]
   ↓
用户点"咖啡厅"             → 前端重新请求，只把 category 参数改成"咖啡厅"
   ↓
后端 DictUtils.getValue("咖啡厅") → "050500" → 透传给高德 around(types="050500")
   ↓
结果只剩真正的咖啡厅
```

**关键点**：
- 前端**完全不感知编码**——它只传分类名字（"冷饮"、"咖啡厅"），翻译是后端的事
- 后端**不自动**根据 keyword 配 types——用户搜"咖啡"就是模糊搜，想搜奶茶店也能搜
- **现阶段字典写死在 JVM**（`Map` 常量），未来迁数据库 + 懒加载缓存

### 1.2 简化版设计（本期实现）

**核心思路**：用户传**分类名字**（如"冷饮"），后端翻译成**编码**（"050700"）再透传给高德。字典写死在代码常量，不做数据库、不做懒加载、不做出参翻译。

#### 入参变更

| | 旧（预留） | 新（简化版） |
|---|---|---|
| 参数名 | `types` | **`category`** |
| 传值 | `types=050700`（裸编码） | `category=冷饮`（传名字） |
| 后端处理 | 直接透传 | `DictUtils.getValue(category)` 翻译成编码再透传 |

> 为什么改成传名字：用户/前端不关心"050700"是什么，他只认"冷饮"两个字。后端负责翻译，这就是字典组件的价值。

#### 字典组件（DictUtils）

只做**入参翻译**一个方向，不搞双向 map（出参高德已返回中文名，不需要翻译）：

```java
public final class DictUtils {
    // 静态字典：label → value，写死在代码
    private static final Map<String, String> LABEL_TO_VALUE = Map.of(
        "咖啡厅", "050500",
        "冷饮",   "050700",
        "糕饼",   "050800",
        "甜品",   "050900",
        "茶艺",   "050600",
        "加油站", "010100",
        "便利店", "060200"
    );

    /** label → value；查不到返回 null（走纯 keyword 模糊搜，不报错） */
    public static String getValue(String label) {
        if (label == null || label.isBlank()) return null;
        return LABEL_TO_VALUE.get(label);
    }
}
```

#### 改动点清单

- `AlongQuery`：**新增** `category`（String，可选，默认 null）；**新增** `strategy`（Integer，可选，0–3，仅 driving 生效）
- `AlongServiceImpl`：调 `around(...)` 时，`types = DictUtils.getValue(query.getCategory())`
- `AlongServiceImpl`：调 `driving(...)` 时，把 `query.getStrategy()` 透传（不再写死 null；其他 mode 忽略）
- `AlongController`：参数校验——`category` 不校验枚举值（查不到就 null 兜底）；`strategy` 限定 0–3
- `AmapClient.around()` / `driving()`：方法签名不变
- 新增 `utils/DictUtils.java`：静态字典 + `getValue(label)` 方法
- 文档 03 第 7.1：参数表更新（`types` → `category`，状态 ⏸ → ✅）

#### 字典内容（固定 7 个 + 全部，编码已从官方 xlsx 核实）

| label | types | 说明 |
|---|---|---|
| 全部 | null | 不加 types，纯 keyword 模糊搜（不传 category 时即此行为） |
| 咖啡厅 | `050500` | 含星巴克/上岛等子类 |
| 冷饮 | `050700` | 冷饮店（含奶茶） |
| 糕饼 | `050800` | 糕饼店 |
| 甜品 | `050900` | 甜品店 |
| 茶艺 | `050600` | 茶艺馆 |
| 加油站 | `010100` | 含中石化/中石油/壳牌 |
| 便利店 | `060200` | 便民商店/便利店 |

> **编码来源**：`docs/Attachment02-高德POI分类与编码（中英文）_V1.06_20230208.xlsx`
> ⚠️ **历史错误**：早期文档写的 `050300` 是错的（那是**快餐厅**），咖啡厅真实编码是 `050500`——不要回退。
> ⏳ **待 Postman 实测**：`types=050500` 在 around 接口是否真的只返回咖啡厅（静态编码表 ≠ 实际返回，跑一次确认）。

#### 兜底逻辑

- 用户传 `category=冷饮` → 翻译成 `050700` → 透传
- 用户传 `category=不存在的名字` → `DictUtils.getValue` 返回 null → types=null → 走纯 keyword 模糊搜（**不报错、不拦着**）
- 用户不传 `category` → 同上，types=null → 模糊搜

### 1.3 未来扩展（本期不做，仅记录）

| 能力 | 现在（简化版） | 未来（完整版） |
|---|---|---|
| 字典存储 | 代码常量 `Map.of(...)` | 数据库表 `sys_dict` |
| 加载方式 | JVM 启动即加载（类初始化） | 懒加载 + 缓存（第一次访问查库） |
| 字典种类 | 只有一种（POI 分类） | 多种字典，按 `dictType` 分层 |
| 双向翻译 | 只做入参（label→value） | 入参 + 出参双向（value→label） |
| 静态 map 结构 | `Map<String,String>` | `Map<String, Map<String,String>>`（dictType 分桶） |

> 未来要做时，`DictUtils` 内部实现换掉，对外接口（`getValue`）不变，业务代码不用改。

---

## 2. Edge case 真实验证（Postman）

正常路径已测通，但下面三类**错误路径**未必跑过——它们是对外接口的质量底线，必须逐一实测。

### 2.1 定位失败 → 400

| 用例 | 怎么测 | 预期 |
|---|---|---|
| A. 不存在的地名 | `start=不存在的鬼地方xyz`，其余正常 | HTTP **400**，body 统一错误结构，提示"找不到起点/终点：xxx"，**不能是 500 堆栈** |
| B. 城市写错 | `city=火星` | 同上 400 |
| C. 空参数 | 不传 `city` 或 `keyword` | 400，参数校验提示 |

> 验收标准：无论哪种失败，用户看到的都是**友好的中文提示**，不是堆栈。

### 2.2 沿途无 POI → 404

| 用例 | 怎么测 | 预期 |
|---|---|---|
| 荒凉路线 | 选一条周围没有咖啡店的路线（如郊野公园之间、深夜路线） | HTTP **404**，提示"沿途 1km 内没找到 咖啡"，**不能返回 200 + 空列表** |

> 这是 US-1 验收标准里明确写的，必须真跑一次确认 GlobalExceptionHandler 把"空结果"映射成了 404。

### 2.3 高德故障 → 502

| 用例 | 怎么测 | 预期 |
|---|---|---|
| A. 错误 key | 用命令行参数 `--amap.key=错误值` 临时覆盖（**不改本地文件**），或改 `application-local.yml` 重启 | HTTP **502**，提示"地图服务暂时不可用"，**不能是 500** |
| B. 断网 | 拔网线 / 关 WiFi 再请求 | 同上 502（Feign 异常被捕获包装） |

> 验收标准：高德挂了我们要**兜底**，把"外部服务故障"和"我们自己代码 bug"区分开。

---

## 3. 启动 README

在 `waypoint-code/waypoint-backend/README.md` 写一份简短的（半页以内）：

- **项目一句话**：沿途 = A 到 B 路上顺便找咖啡的决策工具
- **环境要求**：JDK 21、Maven 3.9+
- **怎么配 key**：复制 `application-local.yml.example`（如无则新建）填真实 key；或设环境变量 `AMAP_KEY`
- **怎么启动**：`mvn spring-boot:run`，端口 **22830**
- **怎么测**：一条 Postman 示例 URL
  ```
  GET http://localhost:22830/along?city=北京&start=奥森公园&end=望京SOHO&keyword=咖啡
  ```
- **返回示例**：贴一段真实 Postman 返回的 JSON

> 目的：3 个月后自己回来不懵，新会话 AI 也能快速上手。

---

## 完成勾选

- [x] 1.1 开放参数：types(category) 透传 + DictUtils + strategy 透传
- [x] 2.1 定位失败 → 400（A/B/C 三个用例）
- [x] 2.2 沿途无 POI → 404
- [x] 2.3 高德故障 → 502（A/B 两个用例）
- [x] 3.0 README

---

*MVP 收尾 · 做完这份清单 = MVP 真正交付*