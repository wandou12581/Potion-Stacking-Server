package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.PotionSync;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 出站：打开容器时的整包内容。参数列表会被拷进新的 NonNullList，不会碰到服务端的 remoteSlots。
 */
@Mixin(ClientboundContainerSetContentPacket.class)
public abstract class MixinClientboundContainerSetContentPacket {
	@ModifyVariable(
			method = "<init>(IILnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;)V",
			at = @At("HEAD"),
			argsOnly = true,
			index = 3
	)
	private static NonNullList<ItemStack> potionstacking$out(NonNullList<ItemStack> items) {
		NonNullList<ItemStack> patched = NonNullList.withSize(items.size(), ItemStack.EMPTY);
		for (int i = 0; i < items.size(); i++) {
			patched.set(i, PotionSync.toClient(items.get(i)));
		}
		return patched;
	}
}