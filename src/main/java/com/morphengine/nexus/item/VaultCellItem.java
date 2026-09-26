package com.morphengine.nexus.item;

import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.storage.CellSpec;
import com.morphengine.nexus.api.storage.CellUsage;
import com.morphengine.nexus.block.entity.Renamable;
import com.morphengine.nexus.menu.VaultCellMenu;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.storage.CellStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A Vault Cell: a storage of one kind and size that works inside a Storage
 * Vault. Its contents, filter and name live on the item, so they travel with it.
 * Used in the main hand, it opens its panel.
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
    public CellStorage openStorage(final ItemStack stack) {
        return new CellStorage(kind.resourceType(), spec(), contentsOf(stack));
    }

    public static void saveContents(final ItemStack stack, final List<ResourceAmount> contents) {
        stack.set(NexusDataComponents.CELL_CONTENTS.get(), new CellContents(contents));
    }

    public static List<ResourceAmount> contentsOf(final ItemStack stack) {
        return stack.getOrDefault(NexusDataComponents.CELL_CONTENTS.get(), CellContents.EMPTY).contents();
    }

    public static CellFilter filterOf(final ItemStack stack) {
        return stack.getOrDefault(NexusDataComponents.CELL_FILTER.get(), CellFilter.EMPTY);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, opener) -> new VaultCellMenu(containerId, inventory),
                    player.getItemInHand(hand).getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        final CellUsage usage = openStorage(stack).usage();
        builder.accept(Component.translatable("tooltip.nexus.cell.bytes",
                grouped(usage.usedBytes()), grouped(usage.totalBytes())).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.nexus.cell.types",
                usage.storedTypes(), usage.maxTypes()).withStyle(ChatFormatting.GRAY));
        final CellFilter filter = filterOf(stack);
        if (!filter.entries().isEmpty()) {
            final String key = filter.mode() == FilterMode.ALLOW
                    ? "tooltip.nexus.cell.whitelist" : "tooltip.nexus.cell.blacklist";
            builder.accept(Component.translatable(key, filter.entries().size()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String grouped(final long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
