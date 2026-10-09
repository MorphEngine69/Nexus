package com.morphengine.nexus.item;

import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.CellSpec;
import com.morphengine.nexus.api.storage.StorageCell;
import com.morphengine.nexus.block.entity.Renamable;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.menu.VaultCellMenu;
import com.morphengine.nexus.registry.NexusDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A Vault Cell: a storage of one kind and size that works inside a Storage
 * Vault. Its contents, filter and name live on the item, so they travel with it.
 * Used in the main hand, a cell with a filter opens its panel.
 */
public final class VaultCellItem extends Item {

    public static final int MAX_NAME_LENGTH = Renamable.MAX_NAME_LENGTH;

    private final CellKind kind;
    private final CellTier tier;

    public VaultCellItem(final CellKind kind, final CellTier tier, final Item.Properties properties) {
        super(properties.stacksTo(1));
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.tier = Objects.requireNonNull(tier, "tier must not be null");
    }

    public CellKind kind() {
        return kind;
    }

    public CellTier tier() {
        return tier;
    }

    public CellSpec spec() {
        return tier.specFor(kind);
    }

    /**
     * @return a storage over the contents saved on {@code stack}; changes to it
     *         reach the stack only through {@link #saveContents}
     */
    public StorageCell openStorage(final ItemStack stack) {
        return kind.createStorage(spec(), contentsOf(stack));
    }

    public static void saveContents(final ItemStack stack, final List<ResourceAmount> contents) {
        stack.set(NexusDataComponents.CELL_CONTENTS.get(), new CellContents(contents));
    }

    public static List<ResourceAmount> contentsOf(final ItemStack stack) {
        return stack.getOrDefault(NexusDataComponents.CELL_CONTENTS.get(), CellContents.EMPTY).contents();
    }

    public static FilterSlots filterOf(final ItemStack stack) {
        return stack.getOrDefault(NexusDataComponents.CELL_FILTER.get(), FilterSlots.EMPTY);
    }

    /**
     * Opens the panel of the cell's filter; a cell without a filter has no panel.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !kind.hasFilter()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, opener) -> new VaultCellMenu(containerId, inventory),
                    player.getItemInHand(hand).getHoverName()));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final List<Component> tooltip,
            final TooltipFlag flag) {
        final Consumer<Component> builder = tooltip::add;
        kind.describe(openStorage(stack), builder);
        final FilterSlots filter = filterOf(stack);
        if (!filter.entries().isEmpty()) {
            final String key = filter.mode() == FilterMode.ALLOW
                    ? "tooltip.nexus.cell.whitelist" : "tooltip.nexus.cell.blacklist";
            builder.accept(Component.translatable(key, filter.entries().size()).withStyle(ChatFormatting.GRAY));
        }
    }
}
