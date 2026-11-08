package com.proxi.potionstacking;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 纯逻辑校验：不需要启动游戏。 */
public final class PotionSyncVerify {
	private static int failures = 0;

	public static void main(String[] args) throws Exception {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		// ---------------- Java 端：出站补组件 / 入站剥除 ----------------
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
		check("入站把组件清掉", backMax == null || backMax.isEmpty());
		check("入站保留数量 16", back.getCount() == 16);

		// ---------------- 基岩版：只发 1 个 ----------------
		ItemStack clamped = PotionSync.clampForBedrock(potion);
		check("基岩视图把 16 压成 1", clamped.getCount() == 1);
		check("基岩视图不改原栈", potion.getCount() == 16);
		check("基岩视图仍认得是药水", clamped.getItem() == Items.POTION);

		ItemStack single = new ItemStack(Items.POTION, 1);
		check("基岩视图对 1 个原样返回（同一对象）", PotionSync.clampForBedrock(single) == single);

		ItemStack dirt = new ItemStack(Items.DIRT, 16);
		check("非药水不受基岩视图影响", PotionSync.clampForBedrock(dirt) == dirt);

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