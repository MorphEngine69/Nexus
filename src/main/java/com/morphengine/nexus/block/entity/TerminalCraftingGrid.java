package com.morphengine.nexus.block.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The 3x3 crafting grid of a Crafting Terminal. The terminal keeps it, so its
 * items stay when the panel closes, and every player looking at the terminal
 * works on the same grid; each open menu is told about every change so it can
 * work out its result.
 */
public final class TerminalCraftingGrid implements CraftingContainer {

    public static final int SIDE = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIDE * SIDE, ItemStack.EMPTY);
    private final List<AbstractContainerMenu> viewers = new ArrayList<>();
    private final Runnable onChange;

    /**
     * @param onChange run after every change, before viewers are told
     */
    public TerminalCraftingGrid(final Runnable onChange) {
        this.onChange = Objects.requireNonNull(onChange, "onChange must not be null");
    }

    public void addViewer(final AbstractContainerMenu menu) {
        viewers.add(menu);
    }

    public void removeViewer(final AbstractContainerMenu menu) {
        viewers.remove(menu);
    }

    NonNullList<ItemStack> stacks() {
        return items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        final ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        if (!removed.isEmpty()) {
            changed();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        onChange.run();
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        items.set(slot, stack);
        changed();
    }

    @Override
    public void setChanged() {
        changed();
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
        changed();
    }

    @Override
    public int getWidth() {
        return SIDE;
    }

    @Override
    public int getHeight() {
        return SIDE;
    }

    @Override
    public List<ItemStack> getItems() {
        return List.copyOf(items);
    }

    @Override
    public void fillStackedContents(final StackedContents contents) {
        for (ItemStack stack : items) {
            contents.accountSimpleStack(stack);
        }
    }

    private void changed() {
        onChange.run();
        for (int i = 0; i < viewers.size(); i++) {
            viewers.get(i).slotsChanged(this);
        }
    }
}
