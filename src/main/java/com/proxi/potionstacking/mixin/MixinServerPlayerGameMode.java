package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.PotionSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 基岩版（Geyser）在用完/投掷药水后会本地先扣数量，而服务端在冷却中会直接 PASS、不消耗也不广播，
 * 导致客户端显示"药水没了"，直到玩家打开背包（触发全量同步）才恢复。
 *
 * <p>这里在 useItem 的所有返回路径上，对基岩玩家补一次全量同步，等于自动帮他们"开一次背包"：
 * 冷却被拒时把真实数量推回去；正常投掷时把剩余数量推回去。Java 玩家不受影响。
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class MixinServerPlayerGameMode {
	@Inject(method = "useItem", at = @At("RETURN"))
	private void potionstacking$resyncAfterUse(ServerPlayer player, Level level, ItemStack stack,
											   InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (level.isClientSide || player == null) {
			return;
		}
		if (!(stack.getItem() instanceof ThrowablePotionItem)) {
			return;
		}
		if (!PotionSync.isBedrock(player)) {
			return;
		}
		player.containerMenu.sendAllDataToRemote();
	}
}