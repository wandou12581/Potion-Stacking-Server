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
{ "stackSize": 16, "throwCooldown": 1 }
```

`stackSize: 1` 等于原版；`throwCooldown: 0` 关闭冷却。

## 构建与自检

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-21
gradlew.bat build          :: 产物 build/libs/potionstacking-1.2.0+1.21.1.jar
gradlew.bat verifySync     :: 不需要启动游戏，验证组件补丁
```

服务端加 `-Dpotionstacking.selftest=true` 启动会打印一次运行期自检。
