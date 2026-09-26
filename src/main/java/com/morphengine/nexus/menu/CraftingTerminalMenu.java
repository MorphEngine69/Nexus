package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.block.entity.TerminalCraftingGrid;
import com.morphengine.nexus.level.PlayerActor;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.terminal.GridFill;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalLayout;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Crafting Terminal panel: the network's storage, a crafting grid kept by the
 * terminal, and the player's inventory. A craft that uses up a grid slot fills
 * it again from the network, then from the inventory, so shift-clicking the
 * result crafts as long as ingredients last. A recipe viewer lays out recipes
 * with ingredients from the inventory and the network. The screen places the
 * slots through {@link #layOut}.
 */
public final class CraftingTerminalMenu extends AbstractContainerMenu implements TerminalPanel {

    /** Menu button id that returns the grid to the network. */
    public static final int BUTTON_CLEAR_GRID = 0;

    private static final int RESULT_SLOT = 0;
    private static final int INVENTORY_START = 1 + TerminalCraftingGrid.SIDE * TerminalCraftingGrid.SIDE;

    private final TerminalMenuState terminal;
    private final Player player;
    private final TerminalCraftingGrid grid;
    private final ResultContainer result = new ResultContainer();

    public CraftingTerminalMenu(
            final int containerId, final Inventory inventory, final BlockPos pos, final TerminalSettings settings) {
        super(NexusMenuTypes.CRAFTING_TERMINAL.get(), containerId);
        this.player = inventory.player;
        this.terminal = new TerminalMenuState(inventory, pos, TerminalKind.CRAFTING_TERMINAL, settings, containerId);
        this.grid = sharedGridOr(terminal.binding().blockEntity());
        grid.addViewer(this);
        addSlot(new TerminalResultSlot(player, grid, result, 0, 0, this::refill));
        for (int slot = 0; slot < TerminalCraftingGrid.SIDE * TerminalCraftingGrid.SIDE; slot++) {
            addSlot(new Slot(grid, slot, 0, 0));
        }
        addStandardInventorySlots(inventory, 0, 0);
        slotsChanged(grid);
    }

    /**
     * On the server the terminal's own grid, shared by everyone at it; on the
     * client a copy the server keeps in step through the slots.
     */
    private TerminalCraftingGrid sharedGridOr(final @Nullable TerminalBlockEntity terminalEntity) {
        final TerminalCraftingGrid shared = player instanceof ServerPlayer && terminalEntity != null
                ? terminalEntity.craftingGrid() : null;
        return shared != null ? shared : new TerminalCraftingGrid(() -> { });
    }

    @Override
    public TerminalMenuState terminal() {
        return terminal;
    }

    @Override
    public void layOut(final TerminalLayout layout) {
        final Slot resultSlot = slots.get(RESULT_SLOT);
        resultSlot.x = layout.resultLeft();
        resultSlot.y = layout.resultTop();
        for (int slot = 0; slot < TerminalCraftingGrid.SIDE * TerminalCraftingGrid.SIDE; slot++) {
            final Slot gridSlot = slots.get(RESULT_SLOT + 1 + slot);
            gridSlot.x = layout.craftingLeft() + slot % TerminalCraftingGrid.SIDE * TerminalLayout.SLOT;
            gridSlot.y = layout.craftingTop() + slot / TerminalCraftingGrid.SIDE * TerminalLayout.SLOT;
        }
        InventorySlots.place(slots, INVENTORY_START, layout.inventoryLeft(), layout.inventoryTop());
    }

    @Override
    public void slotsChanged(final Container container) {
        if (container == grid && player instanceof ServerPlayer serverPlayer) {
            updateResult(serverPlayer);
        }
    }

    private void updateResult(final ServerPlayer serverPlayer) {
        final ServerLevel level = serverPlayer.level();
        final CraftingInput input = grid.asCraftInput();
        ItemStack crafted = ItemStack.EMPTY;
        final Optional<RecipeHolder<CraftingRecipe>> recipe =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isPresent() && result.setRecipeUsed(serverPlayer, recipe.get())) {
            final ItemStack assembled = recipe.get().value().assemble(input);
            if (assembled.isItemEnabled(level.enabledFeatures())) {
                crafted = assembled;
            }
        }
        result.setItem(0, crafted);
        setRemoteSlot(RESULT_SLOT, crafted);
        serverPlayer.connection.send(
                new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT, crafted));
    }

    /**
     * Fills grid slots a craft emptied with the item they held, from the network
     * or else from the player's inventory.
     */
    private void refill(final List<ItemStack> before) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        for (int slot = 0; slot < before.size(); slot++) {
            final ItemStack used = before.get(slot);
            if (!used.isEmpty() && grid.getItem(slot).isEmpty()) {
                grid.setItem(slot, takeOne(serverPlayer, used));
            }
        }
    }

    private ItemStack takeOne(final ServerPlayer serverPlayer, final ItemStack like) {
        final Storage storage = terminal.onlineStorage();
        if (storage != null
                && storage.extract(ItemKey.of(like), 1, Action.EXECUTE, PlayerActor.of(serverPlayer)) == 1) {
            return like.copyWithCount(1);
        }
        final Inventory inventory = serverPlayer.getInventory();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            if (ItemStack.isSameItemSameComponents(inventory.getItem(slot), like)) {
                return inventory.removeItem(slot, 1);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * @return the stacks on the crafting grid and in the player's inventory,
     *         where a recipe viewer's recipe takes items from besides the network
     */
    public List<ItemStack> ingredientsAtHand() {
        final List<ItemStack> stacks = new ArrayList<>(slots.size() - 1);
        for (int slot = RESULT_SLOT + 1; slot < slots.size(); slot++) {
            stacks.add(slots.get(slot).getItem());
        }
        return stacks;
    }

    /**
     * Lays out a recipe sent by a recipe viewer. Server side only.
     *
     * @param slots for each grid slot, the items that fit it in order of preference
     */
    public void fillGrid(final List<List<ItemKey>> slots, final GridFill amount) {
        if (player instanceof ServerPlayer serverPlayer) {
            new CraftingGridFiller(serverPlayer, grid, terminal.onlineStorage()).fill(slots, amount);
        }
    }

    /**
     * Returns everything on the grid to the network, or to the inventory for
     * what the network does not take.
     */
    @Override
    public boolean clickMenuButton(final Player clicker, final int buttonId) {
        if (buttonId != BUTTON_CLEAR_GRID || !(clicker instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        new CraftingGridFiller(serverPlayer, grid, terminal.onlineStorage()).returnGrid();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(final Player clicker, final int slotIndex) {
        final Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (slotIndex == RESULT_SLOT) {
            return craftIntoInventory(clicker, slot);
        }
        if (clicker instanceof ServerPlayer serverPlayer) {
            final ItemStack stack = slot.getItem();
            terminal.insert(serverPlayer, this, stack);
            if (slotIndex < INVENTORY_START && !stack.isEmpty()) {
                moveItemStackTo(stack, INVENTORY_START, slots.size(), true);
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack craftIntoInventory(final Player clicker, final Slot slot) {
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        stack.getItem().onCraftedBy(stack, clicker);
        if (!moveItemStackTo(stack, INVENTORY_START, slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        slot.onQuickCraft(stack, original);
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(clicker, stack);
        clicker.drop(stack, false);
        return original;
    }

    @Override
    public boolean canTakeItemForPickAll(final ItemStack carried, final Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(carried, slot);
    }

    @Override
    public boolean stillValid(final Player clicker) {
        return terminal.binding().stillValid(clicker);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        terminal.tick();
    }

    @Override
    public void removed(final Player clicker) {
        super.removed(clicker);
        grid.removeViewer(this);
        terminal.close();
    }
}
