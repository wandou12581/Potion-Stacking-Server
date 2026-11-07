package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.PotionSync;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 入站：ServerboundContainerClickPacket 里 changedSlots / carriedItem 的唯一落点。
 * 这两个值只用于「服务端认为客户端手上有什么」的比对，清掉组件是安全的，
 * 不清的话服务端会一直认为不一致而反复重发槽位。
 */
@Mixin(AbstractContainerMenu.class)
public abstract class MixinAbstractContainerMenu {
	@ModifyVariable(method = "setRemoteSlotNoCopy", at = @At("HEAD"), argsOnly = true, index = 2)
	private static ItemStack potionstacking$inSlot(ItemStack stack) {
		return PotionSync.fromClient(stack);
	}

	@ModifyVariable(method = "setRemoteCarried", at = @At("HEAD"), argsOnly = true, index = 1)
	private static ItemStack potionstacking$inCarried(ItemStack stack) {
		return PotionSync.fromClient(stack);
	}
}