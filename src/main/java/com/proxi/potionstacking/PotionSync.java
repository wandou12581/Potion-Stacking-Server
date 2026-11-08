package com.proxi.potionstacking;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

import java.util.List;
import java.util.UUID;

/**
 * 药水栈的「线上表示」与「服务端表示」之间的转换。
 *
 * <p>Java 客户端：发给它的药水栈带上 max_stack_size 组件，客户端 getMaxStackSize() 就是 16，
 * 容器里不再把数量压成 1（MC-270846）。服务端真实数据始终不带这个组件，入站回包时剥除。
 *
 * <p>基岩版（Geyser）：基岩客户端的堆叠上限是客户端内置的（药水=1），Java 组件对它无效；
 * 发 count=16 会让基岩客户端的 stack request 与 Geyser 校验对不上，导致物品弹回、放不进容器。
 * 因此对 Floodgate 玩家把药水栈的数量压成 1（他们看到的就是原版行为，但操作正常）。
 */
public final class PotionSync {
	private static final long WARN_INTERVAL_MS = 60_000L;
	private static long lastWarnAt = 0L;
	private static int suppressedCount = 0;

	private PotionSync() {
	}

	public static boolean isPotion(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof PotionItem;
	}

	/** 服务端 -> Java 客户端：给药水栈补上 max_stack_size 组件（返回新栈，不改原栈）。 */
	public static ItemStack toClient(ItemStack stack) {
		if (!isPotion(stack)) {
			return stack;
		}
		int size = PotionStackingConfig.stackSize();
		if (stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) == size) {
			return stack;
		}
		ItemStack copy = stack.copy();
		copy.set(DataComponents.MAX_STACK_SIZE, size);
		return copy;
	}

	/**
	 * 客户端 -> 服务端：把组件写回原版默认值（等于物品原型，补丁项会被自动移除）。
	 * 顺带做入站自检：值既非 1 也非配置值、或剥除后残留时，限频告警。
	 */
	public static ItemStack fromClient(ItemStack stack) {
		if (!isPotion(stack)) {
			return stack;
		}
		int expected = PotionStackingConfig.stackSize();
		Integer incoming = stack.get(DataComponents.MAX_STACK_SIZE);
		ItemStack copy = stack.copy();
		copy.set(DataComponents.MAX_STACK_SIZE, 1);
		Integer after = copy.get(DataComponents.MAX_STACK_SIZE);

		boolean unexpected = incoming != null && incoming != 1 && incoming != expected;
		boolean stripFailed = after == null || after != 1;
		if (unexpected || stripFailed) {
			warnRateLimited("[potionstacking] 入站药水栈的 max_stack_size 异常：收到 {}，剥除后 {}，期望 1（配置 {}）；"
							+ "值异常={}，剥除失败={}。可能另有模组在改这个组件，或剥除路径有遗漏。",
					incoming, after, expected, unexpected, stripFailed);
		}
		return copy;
	}

	// ---------------------------------------------------------------- 基岩版（Geyser）

	/** 是否基岩版玩家：Floodgate 用 UUID(0, xuid) 生成，mostSigBits 为 0；名字前缀作兜底。 */
	public static boolean isBedrock(Player player) {
		if (player == null) {
			return false;
		}
		UUID uuid = player.getUUID();
		if (uuid != null && uuid.getMostSignificantBits() == 0L) {
			return true;
		}
		String prefix = PotionStackingConfig.bedrockNamePrefix();
		return !prefix.isEmpty() && player.getGameProfile().getName().startsWith(prefix);
	}

	/** 基岩玩家视图：药水栈只发 1 个（多出来的仍留在服务端真实数据里）。 */
	public static ItemStack clampForBedrock(ItemStack stack) {
		if (!isPotion(stack) || stack.getCount() <= 1) {
			return stack;
		}
		return stack.copyWithCount(1);
	}

	/**
	 * 出站包按接收者改写：基岩玩家看到的药水栈一律是 1 个，避免 Geyser 的 stack request 校验失败。
	 * 只处理容器相关的两种包，其它包原样返回。
	 */
	public static Packet<?> forBedrockView(Player player, Packet<?> packet) {
		if (!PotionStackingConfig.bedrockClamp() || !isBedrock(player)) {
			return packet;
		}

		if (packet instanceof ClientboundContainerSetSlotPacket slot) {
			ItemStack original = slot.getItem();
			ItemStack clamped = clampForBedrock(original);
			if (clamped != original) {
				return new ClientboundContainerSetSlotPacket(slot.getContainerId(), slot.getStateId(), slot.getSlot(), clamped);
			}
			return packet;
		}

		if (packet instanceof ClientboundContainerSetContentPacket content) {
			List<ItemStack> items = content.getItems();
			ItemStack carried = content.getCarriedItem();
			boolean changed = false;

			NonNullList<ItemStack> mapped = NonNullList.withSize(items.size(), ItemStack.EMPTY);
			for (int i = 0; i < items.size(); i++) {
				ItemStack original = items.get(i);
				ItemStack clamped = clampForBedrock(original);
				if (clamped != original) {
					changed = true;
				}
				mapped.set(i, clamped);
			}
			ItemStack clampedCarried = clampForBedrock(carried);
			if (clampedCarried != carried) {
				changed = true;
			}

			if (changed) {
				return new ClientboundContainerSetContentPacket(content.getContainerId(), content.getStateId(), mapped, clampedCarried);
			}
		}

		return packet;
	}

	/** 全局限频告警：最多每分钟一条，并统计期间被抑制的次数。 */
	public static void warnRateLimited(String message, Object... args) {
		long now = System.currentTimeMillis();
		synchronized (PotionSync.class) {
			if (lastWarnAt != 0L && now - lastWarnAt < WARN_INTERVAL_MS) {
				suppressedCount++;
				return;
			}
			long gapSeconds = lastWarnAt == 0L ? 0L : (now - lastWarnAt) / 1000L;
			Object[] full = new Object[args.length + 2];
			System.arraycopy(args, 0, full, 0, args.length);
			full[args.length] = gapSeconds;
			full[args.length + 1] = suppressedCount;
			PotionStackingConfig.LOG.warn(message + "（距上次告警 {} 秒，期间抑制 {} 次）", full);
			lastWarnAt = now;
			suppressedCount = 0;
		}
	}
}