package com.morphengine.nexus.upgrade;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Objects;

/**
 * The upgrade slots of a device: one upgrade in a slot, and only the kinds the
 * device takes, as many as it takes of each.
 */
public final class UpgradeContainer extends SimpleContainer {

    private final UpgradeLimits limits;
    private final Runnable onChange;

    /**
     * @param onChange run after every change of the slots, for the device to
     *                 take in what it holds now
     */
    public UpgradeContainer(final int size, final UpgradeLimits limits, final Runnable onChange) {
        super(size);
        this.limits = Objects.requireNonNull(limits, "limits must not be null");
        this.onChange = Objects.requireNonNull(onChange, "onChange must not be null");
    }

    /**
     * @return copies of {@code type} held
     */
    public int count(final DeferredHolder<NexusUpgradeType, NexusUpgradeType> type) {
        return UpgradeLimits.count(this, type.get());
    }

    /**
     * @return how many of {@code stack}'s kind may sit in {@code slot} at once,
     *         so several copies of a kind the device takes more than one of
     *         share a slot instead of needing one each
     */
    public int capacityOf(final int slot, final ItemStack stack) {
        return limits.capacityFor(this, slot, stack);
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return limits.accepts(stack, this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        onChange.run();
    }
}
