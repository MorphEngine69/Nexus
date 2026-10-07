package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.storage.CellStatus;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.api.storage.StorageCell;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.storage.FilteredStorage;
import com.morphengine.nexus.storage.ObservedStorage;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The cell slots of a Storage Vault, each occupied slot with a live storage over
 * its cell. The network works on the live storages; their contents are written
 * back onto the cell items by {@link #flush()} and whenever an item is read, so
 * a cell taken out always carries what it holds. Server side only; the client
 * menu shows a plain container.
 */
final class VaultCellSlots extends SimpleContainer {

    static final int SIZE = 16;

    private final Owner owner;
    private final @Nullable LiveCell[] live = new LiveCell[SIZE];
    private final boolean[] dirty = new boolean[SIZE];
    private int touched;

    VaultCellSlots(final Owner owner) {
        super(SIZE);
        this.owner = owner;
    }

    /**
     * @return the storages of the occupied slots, in slot order
     */
    List<Storage> storages() {
        final List<Storage> storages = new ArrayList<>(SIZE);
        for (LiveCell cell : live) {
            if (cell != null) {
                storages.add(cell.storage());
            }
        }
        return List.copyOf(storages);
    }

    /**
     * @return how full the cell in {@code slot} is; {@code null} for an empty slot
     */
    @Nullable CellStatus statusOf(final int slot) {
        final LiveCell cell = live[slot];
        return cell != null ? cell.cell().usage().status() : null;
    }

    /**
     * @return the slots whose cell took or gave resources since the last call, one bit per slot, which the call clears
     */
    int takeTouched() {
        final int slots = touched;
        touched = 0;
        return slots;
    }

    /**
     * Writes the contents of every cell changed since the last flush onto its item.
     */
    void flush() {
        for (int slot = 0; slot < SIZE; slot++) {
            flushSlot(slot);
        }
    }

    void save(final ValueOutput output) {
        flush();
        ContainerHelper.saveAllItems(output, getItems());
    }

    /**
     * Loads the cells and builds their live storages. A vault saved when it had
     * more slots may hold cells at slots it no longer has; they move into empty
     * slots, and what finds no room is handed back.
     *
     * @return the cells left without a slot, for the caller to give back to the world
     */
    List<ItemStack> load(final ValueInput input) {
        final List<ItemStack> homeless = new ArrayList<>();
        for (ItemStackWithSlot saved : input.listOrEmpty(ContainerHelper.TAG_ITEMS, ItemStackWithSlot.CODEC)) {
            if (saved.isValidInContainer(SIZE)) {
                getItems().set(saved.slot(), saved.stack());
            } else {
                homeless.add(saved.stack());
            }
        }
        for (int slot = 0; slot < SIZE && !homeless.isEmpty(); slot++) {
            if (getItems().get(slot).isEmpty()) {
                getItems().set(slot, homeless.removeFirst());
            }
        }
        for (int slot = 0; slot < SIZE; slot++) {
            live[slot] = createLive(slot, getItems().get(slot));
            dirty[slot] = false;
        }
        return homeless;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return stack.getItem() instanceof VaultCellItem;
    }

    /**
     * Writes pending changes of the cell onto its item first, so whoever reads the
     * item, a menu copying it or a drop on removal, sees what the cell holds.
     */
    @Override
    public ItemStack getItem(final int slot) {
        flushSlot(slot);
        return super.getItem(slot);
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        flushSlot(slot);
        final ItemStack removed = super.removeItem(slot, count);
        refresh(slot);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        flushSlot(slot);
        final ItemStack removed = super.removeItemNoUpdate(slot);
        refresh(slot);
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        flushSlot(slot);
        super.setItem(slot, stack);
        refresh(slot);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        owner.contentsChanged();
    }

    private void flushSlot(final int slot) {
        final LiveCell cell = live[slot];
        if (cell != null && dirty[slot]) {
            VaultCellItem.saveContents(cell.stack(), cell.cell().contents());
        }
        dirty[slot] = false;
    }

    private void refresh(final int slot) {
        final ItemStack stack = getItem(slot);
        final LiveCell current = live[slot];
        if (current != null && current.stack() == stack) {
            return;
        }
        live[slot] = createLive(slot, stack);
        owner.cellsChanged();
    }

    private @Nullable LiveCell createLive(final int slot, final ItemStack stack) {
        if (!(stack.getItem() instanceof VaultCellItem item)) {
            return null;
        }
        final StorageCell cell = item.openStorage(stack);
        final Storage filtered = new FilteredStorage(cell, VaultCellItem.filterOf(stack).toResourceFilter());
        return new LiveCell(stack, cell, new ObservedStorage(filtered, () -> markDirty(slot)));
    }

    private void markDirty(final int slot) {
        dirty[slot] = true;
        touched |= 1 << slot;
        owner.contentsChanged();
    }

    /**
     * Told about changes of the slots.
     */
    interface Owner {

        /** A cell was added, removed or replaced; the network must learn about its storages. */
        void cellsChanged();

        /** A slot or the contents of a cell changed; the vault must be saved. */
        void contentsChanged();
    }

    /**
     * A cell item, the storage over its contents, and that storage as the network sees it.
     */
    private record LiveCell(ItemStack stack, StorageCell cell, Storage storage) {
    }
}
