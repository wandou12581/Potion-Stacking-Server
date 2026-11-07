package com.proxi.potionstacking.mixin;

import com.proxi.potionstacking.PotionSync;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 兜底审计：容器关闭时抽查该菜单里的真实槽位。
 *
 * <p>服务端真实数据里的药水栈永远不该带 max_stack_size（值应为原版 1）；只有「发往客户端的那份副本」
 * 才带。所以关容器时如果发现某个药水槽位的 max_stack_size 不等于 1，说明有某条入站路径把客户端的栈
 * 写进了真实数据而没被剥除（典型是新版本新增了回包路径）——这时打一条限频 warn。
 *
 * <p>事件驱动（每个玩家每次关容器跑一次，不超过 100 格），不在 tick 里轮询。
 */
@Mixin(AbstractContainerMenu.class)
public abstract class MixinContainerMenuAudit {
	@Inject(method = "removed", at = @At("TAIL"))
	private void potionstacking$auditOnClose(Player player, CallbackInfo ci) {
		if (player == null || player.level().isClientSide) {
			return;
		}

		AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
		int leaks = 0;
		String sample = null;

		for (Slot slot : menu.slots) {
			ItemStack stack = slot.getItem();
			if (stack.isEmpty() || !(stack.getItem() instanceof PotionItem)) {
				continue;
			}
			Integer max = stack.get(DataComponents.MAX_STACK_SIZE);
			if (max != null && max != 1) {
				leaks++;
				if (sample == null) {
					sample = "slot " + slot.index + " x" + stack.getCount() + " (max=" + max + ")";
				}
			}
		}

		ItemStack carried = menu.getCarried();
		if (!carried.isEmpty() && carried.getItem() instanceof PotionItem) {
			Integer max = carried.get(DataComponents.MAX_STACK_SIZE);
			if (max != null && max != 1) {
				leaks++;
				if (sample == null) {
					sample = "carried x" + carried.getCount() + " (max=" + max + ")";
				}
			}
		}

		if (leaks > 0) {
			PotionSync.warnRateLimited("[potionstacking] 关容器时发现 {} 个药水槽位的 max_stack_size 被污染（示例 {}）——"
					+ "说明有入站路径没被剥除，组件可能已写进存档，请核对当前 MC 版本的 packet 路径。", leaks, sample);
		}
	}
}