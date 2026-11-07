package com.proxi.potionstacking;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 纯逻辑校验：不需要启动游戏，直接验证「组件不会被 PatchedDataComponentMap 规范化掉」这件事。
 */
public final class PotionSyncVerify {
	private static int failures = 0;

	public static void main(String[] args) throws Exception {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		ItemStack potion = new ItemStack(Items.POTION, 16);
		check("原版原型上限是 1", potion.getMaxStackSize() == 1);

		ItemStack wire = PotionSync.toClient(potion);
		check("出站保留数量 16", wire.getCount() == 16);
		check("出站后 getMaxStackSize() == 16", wire.getMaxStackSize() == 16);

		DataComponentPatch patch = wire.getComponentsPatch();
		Integer wireMax = (Integer) patch.get(DataComponents.MAX_STACK_SIZE).orElse(null);
		check("组件补丁里真的带上了 max_stack_size=16", Integer.valueOf(16).equals(wireMax));
		check("原栈没有被改动", potion.getComponentsPatch().isEmpty());

		ItemStack back = PotionSync.fromClient(wire);
		java.util.Optional<?> backMax = back.getComponentsPatch().get(DataComponents.MAX_STACK_SIZE);
		check("入站把组件清掉（补丁里不再有 max_stack_size）", backMax == null || backMax.isEmpty());
		check("入站保留数量 16", back.getCount() == 16);

		ItemStack dirt = new ItemStack(Items.DIRT, 5);
		check("非药水原样返回", PotionSync.toClient(dirt) == dirt);

		if (failures > 0) {
			System.out.println("FAILED: " + failures + " check(s)");
			System.exit(1);
		}
		System.out.println("ALL CHECKS PASSED");
	}

	private static void check(String name, boolean ok) {
		if (!ok) {
			failures++;
			System.out.println("[FAIL] " + name);
		} else {
			System.out.println("[ ok ] " + name);
		}
	}
}