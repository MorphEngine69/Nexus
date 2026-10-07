package com.morphengine.nexus.generator;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

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
    public static boolean drain(final Container slot, final ResourceHandler<FluidResource> tanks) {
        if (slot.getItem(0).isEmpty()) {
            return false;
        }
        final ItemAccess access = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(slot), 0).oneByOne();
        final ResourceHandler<FluidResource> container = access.getCapability(Capabilities.Fluid.ITEM);
        return container != null
                && ResourceHandlerUtil.move(container, tanks, fluid -> true, Integer.MAX_VALUE, null) > 0;
    }

    /**
     * @return whether the stack is a container with no fluid left in it, which the generator is done with
     */
    public static boolean isSpent(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final ResourceHandler<FluidResource> container =
                ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Fluid.ITEM);
        if (container == null) {
            return false;
        }
        for (int index = 0; index < container.size(); index++) {
            if (container.getAmountAsLong(index) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return whether the stack is a container that holds or can hold a fluid, so that the slot takes it
     */
    public static boolean isContainer(final ItemStack stack) {
        return !stack.isEmpty()
                && ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Fluid.ITEM) != null;
    }
}
