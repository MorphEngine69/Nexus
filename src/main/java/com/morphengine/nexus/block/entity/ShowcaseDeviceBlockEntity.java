package com.morphengine.nexus.block.entity;

import com.geckolib.animation.RawAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.function.Function;

/**
 * A device whose model shows a few items that the server tells the clients of, such as the items an Alloy Smelter
 * moves along its rails: one item for each place. A client gets them when it loads the block and whenever one changes,
 * and only then. Designed for extension.
 */
public abstract class ShowcaseDeviceBlockEntity extends AnimatedDeviceBlockEntity {

    /** Places on the model that can show an item. */
    public static final int SHOWN_PLACES = 4;

    private static final String TAG_SHOWN = "shown";
    private static final String TAG_CYCLE_ACTIVE = "cycle_active";
    private static final String TAG_CYCLE_PROGRESS = "cycle_progress";
    private static final String TAG_CYCLE_RATE = "cycle_rate";
    private static final int CYCLE_SYNC_TICKS = 5;
    private static final double CYCLE_END = 0.999;

    private final NonNullList<ItemStack> shown = NonNullList.withSize(SHOWN_PLACES, ItemStack.EMPTY);
    private boolean cycleActive;
    private float cycleProgress;
    private float cycleRate;
    private boolean cycleFresh;
    private double cycleReceived;
    private float lastProgress;
    private long lastSync;

    protected ShowcaseDeviceBlockEntity(
            final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
            final Function<BlockState, RawAnimation> animationOf) {
        super(type, pos, state, animationOf);
    }

    /**
     * Shows {@code item} in place {@code place}, an empty stack for nothing; clients are told only when this changes
     * what is shown. Server side only.
     */
    protected final void show(final int place, final ItemStack item) {
        if (ItemStack.isSameItemSameComponents(shown.get(place), item)) {
            return;
        }
        shown.set(place, item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1));
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Tells the clients how far the piece of work that the model animates is, so that the animation goes through one
     * cycle for each piece of work, as it does. The client carries on at the speed the work last had until the next
     * word, which comes every few ticks, so that it follows work that stops and starts, such as for want of energy,
     * without a message for each tick. Server side only.
     *
     * @param progress  how far the work is, from 0 up to 1; negative when there is none
     * @param firstRate how much of the work is done in a tick at the start, from which the client sets out
     */
    protected final void showCycle(final float progress, final float firstRate) {
        if (level == null) {
            return;
        }
        final long now = level.getGameTime();
        if (progress < 0) {
            if (cycleActive) {
                setCycle(false, 0, 0);
            }
        } else if (!cycleActive || progress < lastProgress) {
            lastProgress = progress;
            lastSync = now;
            setCycle(true, progress, firstRate);
        } else if (now - lastSync >= CYCLE_SYNC_TICKS) {
            setCycle(true, progress, (progress - lastProgress) / (now - lastSync));
            lastProgress = progress;
            lastSync = now;
        }
    }

    private void setCycle(final boolean active, final float progress, final float rate) {
        cycleActive = active;
        cycleProgress = progress;
        cycleRate = rate;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected final double cyclePhase() {
        if (!cycleActive) {
            return super.cyclePhase();
        }
        final double now = CycleClock.now();
        if (cycleFresh) {
            cycleReceived = now;
            cycleFresh = false;
        }
        return Math.clamp(cycleProgress + cycleRate * (now - cycleReceived), 0, CYCLE_END);
    }

    /**
     * @return the item in place {@code place}, one at most; on the client what the server last sent
     */
    public final ItemStack shownItem(final int place) {
        return shown.get(place);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        shown.replaceAll(place -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty(TAG_SHOWN), shown);
        cycleActive = input.getBooleanOr(TAG_CYCLE_ACTIVE, false);
        cycleProgress = input.getFloatOr(TAG_CYCLE_PROGRESS, 0);
        cycleRate = input.getFloatOr(TAG_CYCLE_RATE, 0);
        cycleFresh = true;
    }

    @Override
    public final CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        ContainerHelper.saveAllItems(output.child(TAG_SHOWN), shown);
        output.putBoolean(TAG_CYCLE_ACTIVE, cycleActive);
        output.putFloat(TAG_CYCLE_PROGRESS, cycleProgress);
        output.putFloat(TAG_CYCLE_RATE, cycleRate);
        return output.buildResult();
    }

    @Override
    public final Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
