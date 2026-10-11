package com.proxi.potionstacking;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "potionstacking")
public class ModConfig implements ConfigData {

    @ConfigEntry.BoundedDiscrete(max = 64)
    public int stackSize = 16;
	
	@ConfigEntry.BoundedDiscrete(max = 120)
    public int throwCooldown = 1;
}
