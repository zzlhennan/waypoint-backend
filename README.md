# 沿途（waypoint-backend）

输入 **城市 + 起点 + 终点 + 关键词**，返回沿途按「绕路直线距离」排序的 Top 3 POI（MVP 只验证"咖啡"场景）。
不是导航、不是点评、无用户系统——核心价值是把"绕路成本"算出来，告诉你哪家最顺路。

## 环境要求

- JDK 21（本机在 `D:\zzl_program\jdk21`）
- Maven 3.9+（本机用系统 `mvn`，项目自带 `mvnw` 不可用）
- 高德开放平台 Web 服务 key（多个，用于轮询规避免费版 QPS 限流）

## 配置 key

真实 key 放 `src/main/resources/application-local.yml`（**已 gitignore，绝不提交**）：

```yaml
amap:
  keys:
    - 你的第一个key
    - 你的第二个key
    - 你的第三个key
```

> 免费版单 key 限 3 QPS，一次 `/along` 会连发十几次请求，所以多 key 轮询 + around 间隔 200ms 限速（见 `AlongServiceImpl.sleepBeforeNext`）。

## 启动

```bash
mvn spring-boot:run
# 端口 22830
```

## 测一条

```
GET http://localhost:22830/along?city=北京&start=奥森公园&end=望京SOHO&keyword=咖啡
```

返回示例（绕路距离已取整成米）：

```json
{
  "query": { "city": "北京", "start": "奥森公园", "end": "望京SOHO", "keyword": "咖啡", "mode": "driving" },
  "result": [
    { "name": "星巴克(北京望京SOHO T3店)", "address": "望京街10号望京SOHO T3F1层",
      "distanceToRouteMeters": 12, "location": "116.480893,39.995884",
      "tel": "4008206998", "type": "餐饮服务;咖啡厅;星巴克咖啡", "adname": "朝阳区" }
  ]
}
```

可选参数：`mode`(driving/walking/bicycling)、`topN`、`radius`、`category`(分类名如"冷饮"，后端字典翻译成 types)、`strategy`(0-3，仅 driving)。

## 想了解更多

- 需求 / 算法口径 / 决策记录：`docs/01-需求文档.md`
- 高德接口细节（v5 2.0、show_fields、QPS 坑）：`docs/02-高德API.md`
- 接口定义与类设计：`docs/03-接口文档.md`
- 绕路距离的数学原理：`docs/Attachment01-绕路距离算法.md`
- 项目工作约定（怎么跟 AI 协作）：`CLAUDE.md`