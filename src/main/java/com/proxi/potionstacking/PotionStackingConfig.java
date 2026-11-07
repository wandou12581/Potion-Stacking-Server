package com.proxi.potionstacking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 纯服务端用的极简配置：config/potionstacking.json，首次启动自动生成。
 * 不依赖 Cloth Config / ModMenu，服务器上不用多装任何库。
 *
 * <p>拿不到 Fabric 配置目录时（例如在 Gradle 里跑逻辑自检）直接退回默认值。
 */
public final class PotionStackingConfig {
	public static final Logger LOG = LoggerFactory.getLogger("potionstacking");

	/** 药水最大堆叠数（1 等于原版行为）。 */
	public int stackSize = 16;
	/** 溅射/滞留药水使用后的冷却，单位秒，0 关闭。 */
	public int throwCooldown = 1;

	private static PotionStackingConfig instance;

	private PotionStackingConfig() {
	}

	public static PotionStackingConfig get() {
		PotionStackingConfig cfg = instance;
		if (cfg == null) {
			cfg = load();
			instance = cfg;
		}
		return cfg;
	}

	public static int stackSize() {
		return Math.max(1, Math.min(99, get().stackSize));
	}

	public static int throwCooldownTicks() {
		return Math.max(0, Math.min(120, get().throwCooldown)) * 20;
	}

	private static PotionStackingConfig load() {
		PotionStackingConfig cfg = new PotionStackingConfig();
		try {
			Path path = FabricLoader.getInstance().getConfigDir().resolve("potionstacking.json");
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path)) {
					PotionStackingConfig read = gson.fromJson(reader, PotionStackingConfig.class);
					if (read != null) {
						cfg = read;
					}
				}
			}
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				gson.toJson(cfg, writer);
			}
		} catch (Throwable t) {
			LOG.warn("[potionstacking] 读取配置失败，使用默认值 (stackSize=16, throwCooldown=1)", t);
		}
		return cfg;
	}
}