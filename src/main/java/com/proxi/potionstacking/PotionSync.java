package com.proxi.potionstacking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

/**
 * 药水栈的「线上表示」与「服务端表示」之间的转换。
 *
 * <p>背景：1.20.5+ 客户端在把收到的槽位数据写进 SimpleContainer 时，会调用
 * stack.limitSize(container.getMaxStackSize(stack))，也就是把数量压到客户端自己算出来的
 * max_stack_size。所以只要客户端不知道药水能叠 16，箱子/漏斗等容器里的数量就会显示成 1
 * （MC-270846，Won't Fix）。
 *
 * <p>解决办法：发给客户端的每个药水栈都显式带上 max_stack_size 组件。服务端的物品原型保持原版 1
 * （不改 Item.Properties），所以这个组件值和原型不同，会真的写进组件补丁、随包下发；客户端拿到后
 * getMaxStackSize() 就是 16，不再压数量。反过来，客户端回传的栈要把它清掉，避免组件被写进服务端
 * 存档、或破坏栈的等价性。
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

	/** 服务端 -> 客户端：给药水栈补上 max_stack_size 组件（返回新栈，不改原栈）。 */
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
	 *
	 * <p>顺带做一次入站自检：正常情况下收到的药水栈要么没带组件（值=1），要么带的是本模组写入的
	 * stackSize。所以「值既不是 1 也不是配置值」或「剥除后还残留」都说明有别的东西在改这个组件、
	 * 或剥除路径有遗漏 —— 这种情况按分钟限频打一条 warn，避免刷屏。
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