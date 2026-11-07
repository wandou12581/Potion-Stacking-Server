package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.PotionStackingConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 溅射/滞留药水的使用冷却（默认 1 秒），避免可堆叠后过强；配置里设 0 可关闭。
 * 覆盖 SplashPotionItem 与 LingeringPotionItem（两者都继承 ThrowablePotionItem）。
 */
@Mixin(ThrowablePotionItem.class)
public abstract class MixinThrowablePotionItem {
	@Inject(method = "use", at = @At("RETURN"))
	private void potionstacking$cooldown(Level level, Player player, InteractionHand hand,
										 CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		int ticks = PotionStackingConfig.throwCooldownTicks();
		if (ticks > 0 && !level.isClientSide) {
			player.getCooldowns().addCooldown((Item) (Object) this, ticks);
		}
	}
}