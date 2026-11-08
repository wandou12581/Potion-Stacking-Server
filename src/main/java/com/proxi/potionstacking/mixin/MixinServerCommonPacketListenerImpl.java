package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.PotionSync;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 鍑虹珯鎬婚椄锛氭墍鏈夊彂寰€瀹㈡埛绔殑鍖呮渶缁堥兘璧?ServerCommonPacketListenerImpl#send(Packet, PacketSendListener)銆? * 杩欓噷鎸夋帴鏀惰€呮敼鍐欙細鍩哄博鐗堢帺瀹讹紙Floodgate锛夋敹鍒扮殑鑽按鏍堟暟閲忎竴寰嬪帇鎴?1锛? * 鍚﹀垯鍩哄博瀹㈡埛绔殑 stack request 涓?Geyser 鏍￠獙瀵逛笉涓婏紝鐗╁搧浼氬脊鍥炪€佹斁涓嶈繘瀹瑰櫒銆? *
 * <p>Java 鐜╁涓嶅彈褰卞搷锛屼粛鏀跺埌甯?max_stack_size 缁勪欢鐨勫畬鏁存暟閲忋€? */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class MixinServerCommonPacketListenerImpl {
	@ModifyVariable(
			method = "send",
			at = @At("HEAD"),
			argsOnly = true,
			index = 1
	)
	private Packet<?> potionstacking$bedrockView(Packet<?> packet) {
		Object self = this;
		if (self instanceof ServerGamePacketListenerImpl listener) {
			return PotionSync.forBedrockView(listener.player, packet);
		}
		return packet;
	}
}