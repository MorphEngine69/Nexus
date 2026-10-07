package com.morphengine.nexus.charging;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Charges the item in a slot from an energy buffer, through the energy capability of the item, so that items of
 * other mods charge as well as those of Nexus. Server thread only.
 */
public final class ItemCharger {

    private ItemCharger() {
    }

    /**
     * @return whether the stack takes FE, so that a charging slot accepts it
     */
    public static boolean isChargeable(final ItemStack stack) {
        return !stack.isEmpty() && handlerOf(stack.copyWithCount(1)) != null;
    }

    /**
     * Moves FE from {@code source} into the item in the first slot of {@code slot}, as much as the item takes and the
     * buffer gives in one call.
     *
     * @return the FE that moved; zero when the slot is empty or holds an item that takes none
     */
    public static long charge(final Container slot, final EnergyBuffer source) {
        Objects.requireNonNull(slot, "slot must not be null");
        Objects.requireNonNull(source, "source must not be null");
        final EnergyHandler item = slot.getItem(0).isEmpty() ? null : handlerOf(slot);
        final int offered = (int) Math.min(Integer.MAX_VALUE, source.extract(Integer.MAX_VALUE, Action.SIMULATE));
        if (item == null || offered <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            final int taken = item.insert(offered, transaction);
            transaction.commit();
            return taken > 0 ? source.extract(taken, Action.EXECUTE) : 0;
        }
    }

    private static @Nullable EnergyHandler handlerOf(final Container slot) {
        return ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(slot), 0).oneByOne()
                .getCapability(Capabilities.Energy.ITEM);
    }

    private static @Nullable EnergyHandler handlerOf(final ItemStack stack) {
        return ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
    }
}
