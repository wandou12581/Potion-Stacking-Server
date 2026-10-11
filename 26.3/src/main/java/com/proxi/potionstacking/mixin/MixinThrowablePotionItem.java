package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ThrowablePotionItem.class)
public abstract class MixinThrowablePotionItem {
    
	// I don't really need to access the config like this I could just raw dog it but it's easier to identify with the identical class over in MixinPotionItem
	public class ConfigAccess {
		public static int getThrowCooldown() {
			return AutoConfig.getConfigHolder(ModConfig.class).getConfig().throwCooldown;
		}
	}
	
	int throwCooldown = ConfigAccess.getThrowCooldown();
	
    @Inject(method = "use", at = @At("RETURN"))
    private void addCooldown(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (level instanceof ServerLevel serverLevel) {
            // Get the ItemStack the player is holding
            ItemStack stack = player.getItemInHand(hand);
            
            // Apply cooldown on the specific ItemStack
            player.getCooldowns().addCooldown(stack, throwCooldown*20); // 20 ticks = 1 second
        }
    }
}
