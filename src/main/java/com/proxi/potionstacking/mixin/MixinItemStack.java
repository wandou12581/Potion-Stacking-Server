package com.proxi.potionstacking.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.proxi.potionstacking.PotionStackingConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 药水的「运行期最大堆叠数」。
 *
 * <p>故意不改 Item.Properties.stacksTo()（物品原型），因为一旦原型是 16，服务端给栈
 * set(MAX_STACK_SIZE, 16) 就会等于原型值而被 PatchedDataComponentMap 规范化掉，组件补丁变空、
 * 客户端永远收不到 16。原型保持 1 + 这里动态返回 16，才能让发出去的组件补丁是「非默认值」。
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStack {
	@ModifyReturnValue(method = "getMaxStackSize", at = @At("RETURN"))
	private int potionstacking$maxStackSize(int original) {
		ItemStack self = (ItemStack) (Object) this;
		if (!self.isEmpty() && self.getItem() instanceof PotionItem) {
			return Math.max(original, PotionStackingConfig.stackSize());
		}
		return original;
	}
}