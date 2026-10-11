package com.proxi.potionstacking.mixin;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionItem.class)
public abstract class MixinPotionItemUseOn {
    @Inject(method = "useOn", at = @At("RETURN"), cancellable = true)
    private void skipDuplicateRemainder(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();

        if (result instanceof InteractionResult.Success success && success.wasItemInteraction()) {
            Player player = context.getPlayer();

            if (player != null) {
                player.awardStat(Stats.ITEM_USED.get((Item)(Object)this));
            }

            cir.setReturnValue(success.withoutItem());
        }
    }
}
