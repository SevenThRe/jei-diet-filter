# JEI Diet Filter

[English below](#english)

一个**仅客户端**的 JEI 附属模组（Minecraft 1.20.1 / Forge 47.x）。

在 JEI 搜索框输入 `!`，只显示你**还没吃过**的食物——解决"JEI 搜不出来哪些吃过、哪些没吃过"的问题。

## 功能特性

- **`!`** —— 列出所有未食用的可食物品
- **`!关键词`** —— 在未食用食物中按名字过滤，如 `!面包`
- **全角 `！`** —— 中文输入法打出的全角感叹号同样有效
- **实时刷新** —— 吃掉一种新食物后，它会立刻从 `!` 结果中消失（下一 tick 生效）
- **自定义前缀** —— 前缀字符可在 JEI 配置文件中修改（见下方配置）

## 前置需求

| 依赖 | 版本 | 必需性 |
|---|---|---|
| Minecraft | 1.20.1 | 必需 |
| Forge | 47.x | 必需 |
| JEI (Just Enough Items) | 15.20.0.129+ | 必需 |
| solcarrot (Spice of Life: Carrot Edition) | 1.20.1-1.15.1+ | 必需 |
| Diet | 任意 | **非必需**（与 Diet 无交互，见"工作原理"） |

## 安装

把 jar 放进 `mods/` 文件夹，与 JEI、solcarrot 一同加载即可。

## 配置

前缀字符可自定义：

```ini
# config/jei/jei-client.ini
[search]
UneatenFoodPrefix = "!"
```

- 填任意非空字符串，**第一个字符**作为搜索前缀，如 `&`、`+`
- 修改后需重启游戏生效
- 当配置为 `!` 时，全角 `！` 自动作为别名

## 工作原理

- **数据源**：已食用状态读取自 **solcarrot** 的 `FoodList`（与食物图鉴、tooltip 中"已食用"标记使用完全相同的数据路径），保证 JEI 搜索结果与游戏内显示一致。**不读取 Diet 的数据**——两者在整合包中各自独立记录，内容并不一致
- **索引阶段**：JEI 建立物品索引时（通过 Mixin 注入），所有可食物品的搜索字符串中追加一个 `uneaten` 哨兵词
- **查询阶段**：带本模组前缀的搜索在返回结果前实时剔除"已食用"物品，与玩家状态、数据同步时序完全解耦
- **实时监听**：客户端每 tick 对比 solcarrot 已吃列表快照，发现新增时对 JEI 缓存执行 runtime 移除/重加，强制搜索缓存失效

## 构建

```bash
./gradlew build
```

产物位于 `build/libs/`。`libs/` 目录已包含 JEI 之外的编译依赖（solcarrot、diet），无需额外下载。

## 英文说明 (English)

A **client-side-only** JEI addon for Minecraft 1.20.1 (Forge 47.x).

Type `!` in the JEI search box to show only foods you have **not eaten yet**.

- `!` — list all uneaten foods; `!keyword` — filter uneaten foods by name
- Full-width `！` works as an alias; the prefix character is customizable via `config/jei/jei-client.ini` → `[search]` → `UneatenFoodPrefix` (restart to apply)
- Eaten-state is read from **solcarrot** (`FoodList`) — the exact same data path as the "eaten" tooltip — so results always match what the game shows. **Diet is not required** and not read.
- Eating a new food removes it from `!` results within a tick

**Requirements**: Minecraft 1.20.1, Forge 47.x, JEI 15.20.0.129+, solcarrot 1.20.1-1.15.1+

## License

MIT
