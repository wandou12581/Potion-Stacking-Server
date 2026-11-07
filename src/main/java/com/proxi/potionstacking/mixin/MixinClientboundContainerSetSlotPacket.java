package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.PotionSync;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 出站：单槽更新。同时覆盖容器槽位、containerId=-1 的手持物品、-2 的玩家背包直通。
 */
@Mixin(ClientboundContainerSetSlotPacket.class)
public abstract class MixinClientboundContainerSetSlotPacket {
	@ModifyVariable(
			method = "<init>(IIILnet/minecraft/world/item/ItemStack;)V",
			at = @At("HEAD"),
			argsOnly = true,
			index = 4
	)
	private static ItemStack potionstacking$out(ItemStack stack) {
		return PotionSync.toClient(stack);
	}
}