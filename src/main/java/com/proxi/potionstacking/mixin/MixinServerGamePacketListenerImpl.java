package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.PotionSync;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 入站：创造模式放物品。必须清组件，否则它会作为真实数据被写进存档。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl {
	@Redirect(
			method = "handleSetCreativeModeSlot",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;itemStack()Lnet/minecraft/world/item/ItemStack;"
			)
	)
	private static ItemStack potionstacking$inCreative(ServerboundSetCreativeModeSlotPacket packet) {
		return PotionSync.fromClient(packet.itemStack());
	}
}