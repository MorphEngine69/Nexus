package com.morphengine.nexus.upgrade;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Map;

/**
 * Which kinds of upgrade a device takes and how many of each. A kind the device
 * does not take, or of which it holds as many as it takes already, stays out of
 * its upgrade slots.
 *
 * @param maxCounts copies of each kind taken at most, each positive; copied
 */
public record UpgradeLimits(Map<DeferredHolder<NexusUpgradeType, NexusUpgradeType>, Integer> maxCounts) {

    public UpgradeLimits {
        maxCounts = Map.copyOf(maxCounts);
        for (Map.Entry<DeferredHolder<NexusUpgradeType, NexusUpgradeType>, Integer> max : maxCounts.entrySet()) {
            if (max.getValue() <= 0) {
                throw new IllegalArgumentException(
                        "upgrade " + max.getKey().getId() + " must be taken at least once: " + max.getValue());
            }
        }
    }

    /**
     * @return whether the device takes upgrades of the kind {@code stack} is,
     *         however many it holds already
     */
    public boolean takesKindOf(final ItemStack stack) {
        return maxOf(stack) > 0;
    }

    /**
     * @return whether one more {@code stack} fits the device holding {@code upgrades}
     */
    public boolean accepts(final ItemStack stack, final Container upgrades) {
        return stack.getItem() instanceof UpgradeItem upgrade && count(upgrades, upgrade.type()) < maxOf(stack);
    }

    /**
     * @return copies of {@code type} among {@code upgrades}
     */
    public static int count(final Container upgrades, final NexusUpgradeType type) {
        int count = 0;
        for (int slot = 0; slot < upgrades.getContainerSize(); slot++) {
            final ItemStack stack = upgrades.getItem(slot);
            if (stack.getItem() instanceof UpgradeItem upgrade && upgrade.type() == type) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * @return how many of {@code stack}'s kind may sit in {@code slot} at once,
     *         given how many copies the container's other slots already hold; a
     *         kind the device does not take gets none
     */
    public int capacityFor(final Container upgrades, final int slot, final ItemStack stack) {
        if (!(stack.getItem() instanceof UpgradeItem upgrade)) {
            return 0;
        }
        final int max = maxOf(upgrade.type());
        int elsewhere = 0;
        for (int index = 0; index < upgrades.getContainerSize(); index++) {
            if (index == slot) {
                continue;
            }
            final ItemStack held = upgrades.getItem(index);
            if (held.getItem() instanceof UpgradeItem other && other.type() == upgrade.type()) {
                elsewhere += held.getCount();
            }
        }
        return Math.max(0, max - elsewhere);
    }

    private int maxOf(final ItemStack stack) {
        return stack.getItem() instanceof UpgradeItem upgrade ? maxOf(upgrade.type()) : 0;
    }

    private int maxOf(final NexusUpgradeType type) {
        for (Map.Entry<DeferredHolder<NexusUpgradeType, NexusUpgradeType>, Integer> max : maxCounts.entrySet()) {
            if (max.getKey().get() == type) {
                return max.getValue();
            }
        }
        return 0;
    }
}
