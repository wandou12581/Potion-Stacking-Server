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
 */
public final class PotionStackingConfig {
	public static final Logger LOG = LoggerFactory.getLogger("potionstacking");

	/** 药水最大堆叠数（1 等于原版行为）。 */
	public int stackSize = 16;
	/** 溅射/滞留药水使用后的冷却，单位秒，0 关闭。 */
	public int throwCooldown = 1;
	/**
	 * 对基岩版玩家（Floodgate）把药水栈发送数量压成 1。
	 * 基岩客户端认为药水上限是 1，发 16 会导致它无法放入容器；压成 1 后操作正常，但基岩端看不到堆叠。
	 */
	public boolean bedrockClamp = true;
	/** 识别基岩玩家的名字前缀（Floodgate 配置的前缀），兜底用；UUID 判断优先。 */
	public String bedrockNamePrefix = "be.";

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

	public static boolean bedrockClamp() {
		return get().bedrockClamp;
	}

	public static String bedrockNamePrefix() {
		String p = get().bedrockNamePrefix;
		return p == null ? "" : p;
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