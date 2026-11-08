## ！！代码主要由AI生成！！

原模组必须在客户端安装才能看到药水堆叠数量，本次修改解决了这个问题，可以让模组仅安装在服务端时玩家也能正常看到药水堆叠数量

纯服务端版药水堆叠模组（Fabric 1.21.1）：药水可叠到 16，**只装在服务端**时容器里也能正确显示堆叠数量。

该模组在Geyser/基岩版内仍无法正常显示堆叠数

## 修改思路

1.20.5+ 客户端收到槽位数据后，`SimpleContainer.setItem()` 会执行
`stack.limitSize(container.getMaxStackSize(stack))`，把数量压到**客户端自己算出的 max_stack_size**。
服务端单边改堆叠上限时客户端仍认为药水只能叠 1，于是容器里一律显示 1（[MC-270846](https://mojira.dev/MC-270846)，Won't Fix）。

## 实现

- **运行期上限**：`MixinItemStack` 让药水的 `getMaxStackSize()` 返回配置值；物品原型**保持原版 1**
  （若原型改成 16，`PatchedDataComponentMap.set()` 会判定「等于原型」而把组件从补丁里删掉，客户端收不到）。
- **出站**：`MixinClientboundContainerSetSlotPacket` / `MixinClientboundContainerSetContentPacket`
  给发往客户端的副本补 `max_stack_size` 组件。
- **入站**：`MixinAbstractContainerMenu`（`setRemoteSlotNoCopy` / `setRemoteCarried`）与
  `MixinServerGamePacketListenerImpl`（创造模式）把组件剥掉，避免写进存档、破坏栈等价性。
- **冷却**：`MixinThrowablePotionItem` 给溅射/滞留药水加 1 秒使用冷却（可配置关闭）。
- **诊断**：`PotionSync.fromClient` 的入站异常告警 + `MixinContainerMenuAudit` 在关容器时抽查槽位，
  发现 `max_stack_size ≠ 1` 就限频告警（每分钟最多一条）。

## 配置

首次启动生成 `config/potionstacking.json`：

```json
{
  "stackSize": 16,
  "throwCooldown": 1,
  "bedrockClamp": true,
  "bedrockNamePrefix": "be."
}
```

`stackSize（最大堆叠数）: ` 若设为`1`，等于原版；  
`throwCooldown（溅射/滞留药水使用后的冷却时间（秒））: ` 若设为`0`，关闭冷却；  
`bedrockClamp（基岩版玩家不显示药水堆叠数量）: ` 如果设为`false`，基岩版玩家无法将堆叠的药水放入容器中（具体原因请看[基岩版（Geyser / Floodgate）](https://github.com/wandou12581/Potion-Stacking-Server/edit/v1.3/README.md#%E5%9F%BA%E5%B2%A9%E7%89%88geyser--floodgate)）；  
`bedrockNamePrefix（识别基岩玩家的名字前缀）: ` 默认设为`be.`，但**Geyser / Floodgate的默认值为**`.`,请保证`bedrockNamePrefix`与`Geyser / Floodgate`的值**相同**，否则会**爆炸！** ~~其实我也不知道会发生什么，我没有这么试过~~。

## 构建与自检

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-21
gradlew.bat build          :: 产物 build/libs/potionstacking-1.2.0+1.21.1.jar
gradlew.bat verifySync     :: 不需要启动游戏，验证组件补丁
```

服务端加 `-Dpotionstacking.selftest=true` 启动会打印一次运行期自检。

## 基岩版（Geyser / Floodgate）

基岩客户端的物品堆叠上限是**客户端内置**的（药水 = 1），Java 的 `max_stack_size` 组件对它无效。
把 16 个药水发给基岩玩家会让 Geyser 的 stack request 校验失败 —— 表现就是"物品一闪弹回、放不进箱子"。

因此 v1.3 起：**发给基岩玩家的药水栈数量一律压成 1**（`bedrockClamp`，默认开启）。
- 基岩玩家能正常操作（放箱子、拿取、扔出），只是看不到堆叠（等于原版体验）；
- Java 玩家完全不受影响，仍然看到 16；
- 服务端真实数据始终是 16，压的只是"发给基岩玩家的那份副本"。

识别方式：Floodgate 的基岩玩家 UUID 形如 `00000000-...`（`mostSigBits == 0`），另用名字前缀兜底（`bedrockNamePrefix`，默认 `be.`）。
