package com.morphengine.nexus.generator;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jspecify.annotations.Nullable;

/**
 * The slot of a generator that takes a bucket, or any other container of a fluid: its fluid goes into the tanks as
 * far as they take it and the empty container stays in the slot, for the player to take. Server thread only.
 */
public final class BucketSlot {

    private BucketSlot() {
    }

    /**
     * Pours what the container in {@code slot} holds into {@code tanks}.
     *
     * @return whether any fluid moved
     */
    public static boolean drain(final Container slot, final IFluidHandler tanks) {
        final ItemStack stack = slot.getItem(0);
        final IFluidHandlerItem container = stack.isEmpty() || stack.getCount() != 1 ? null : containerOf(stack);
        if (container == null) {
            return false;
        }
        final FluidStack offered = container.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
        final int accepted = offered.isEmpty() ? 0 : tanks.fill(offered, FluidAction.SIMULATE);
        if (accepted <= 0) {
            return false;
        }
        final FluidStack drained = container.drain(offered.copyWithAmount(accepted), FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }
        tanks.fill(drained, FluidAction.EXECUTE);
        slot.setItem(0, container.getContainer());
        return true;
    }

    /**
     * @return whether the stack is a container with no fluid left in it, which the generator is done with
     */
    public static boolean isSpent(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final IFluidHandlerItem container = containerOf(stack);
        if (container == null) {
            return false;
        }
        for (int index = 0; index < container.getTanks(); index++) {
            if (!container.getFluidInTank(index).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return whether the stack is a container that holds or can hold a fluid, so that the slot takes it
     */
    public static boolean isContainer(final ItemStack stack) {
        return !stack.isEmpty() && containerOf(stack) != null;
    }

    private static @Nullable IFluidHandlerItem containerOf(final ItemStack stack) {
        return stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
    }
}
