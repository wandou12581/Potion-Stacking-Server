package com.proxi.potionstacking;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 诊断用入口：只有加 JVM 参数 -Dpotionstacking.selftest=true 才会输出，
 * 平时完全静默。用来在真实服务端里确认「运行期堆叠上限」和「出站组件补丁」是否生效。
 */
public class PotionStackingSelfTest implements ModInitializer {
	@Override
	public void onInitialize() {
		if (!Boolean.getBoolean("potionstacking.selftest")) {
			return;
		}
		ItemStack stack = new ItemStack(Items.POTION, 16);
		PotionStackingConfig.LOG.info("[selftest] 原版栈 getMaxStackSize() = {}", stack.getMaxStackSize());
		ItemStack wire = PotionSync.toClient(stack);
		PotionStackingConfig.LOG.info("[selftest] 出站 count = {}, 组件补丁 = {}",
				wire.getCount(), wire.getComponentsPatch());
		PotionStackingConfig.LOG.info("[selftest] 出站 getMaxStackSize() = {}, 组件值 = {}",
				wire.getMaxStackSize(), wire.get(DataComponents.MAX_STACK_SIZE));
	}
}