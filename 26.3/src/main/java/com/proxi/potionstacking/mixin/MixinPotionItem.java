package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PotionItem.class)
public abstract class MixinPotionItem {

	public class ConfigAccess {
		// Uhh so I know this is a shitty place to put the config registry but I'm dumb
		// I need to access the config values in my mixins and this is the first mixin that loads
		// So hence loading it right here instead of in the init in a main class is my solution
		static {
			AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		}
		public static int getStackSize() {
			return AutoConfig.getConfigHolder(ModConfig.class).getConfig().stackSize;
		}
	}

	private static int stackSize = ConfigAccess.getStackSize();

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static Item.Properties modifyStackSize(Item.Properties properties) {
        return properties.stacksTo(stackSize);
    }
}

