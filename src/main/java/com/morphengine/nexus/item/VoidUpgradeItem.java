package com.morphengine.nexus.item;

import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.menu.VoidUpgradeMenu;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.upgrade.NexusUpgradeType;
import com.morphengine.nexus.upgrade.UpgradeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The Void Upgrade: in a Storage Vault or an External Vault it has the network destroy what it lists, instead of
 * storing it. The list, of nine items or fluids, lives on the item and is set from the main hand, as a Vault Cell's
 * filter is.
 */
public final class VoidUpgradeItem extends UpgradeItem {

    public VoidUpgradeItem(
            final DeferredHolder<NexusUpgradeType, NexusUpgradeType> type, final Item.Properties properties) {
        super(type, properties);
    }

    /**
     * @return what {@code stack} lists, always as a whitelist
     */
    public static FilterSlots listOf(final ItemStack stack) {
        return stack.getOrDefault(NexusDataComponents.DISCARD_FILTER.get(), FilterSlots.EMPTY)
                .withMode(FilterMode.ALLOW);
    }

    public static void setList(final ItemStack stack, final FilterSlots list) {
        stack.set(NexusDataComponents.DISCARD_FILTER.get(), list.withMode(FilterMode.ALLOW));
    }

    /**
     * @return what the Void Upgrade among {@code upgrades} has the network destroy; empty without one, or while its
     *         list lists nothing
     */
    public static Optional<ResourceFilter> discardedBy(final Container upgrades) {
        for (int slot = 0; slot < upgrades.getContainerSize(); slot++) {
            final ItemStack stack = upgrades.getItem(slot);
            if (stack.getItem() instanceof VoidUpgradeItem) {
                return discardedBy(stack);
            }
        }
        return Optional.empty();
    }

    /**
     * @return what {@code stack} has the network destroy; empty while the list lists nothing, since a filter that
     *         lists nothing would let everything pass
     */
    public static Optional<ResourceFilter> discardedBy(final ItemStack stack) {
        final FilterSlots list = listOf(stack);
        return list.entries().isEmpty() ? Optional.empty() : Optional.of(list.toResourceFilter());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, opener) -> new VoidUpgradeMenu(containerId, inventory),
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
        super.appendHoverText(stack, context, tooltip, flag);
        final int listed = listOf(stack).entries().size();
        builder.accept(Component.translatable(listed == 0
                ? "tooltip.nexus.upgrade.void.empty" : "tooltip.nexus.upgrade.void.listed", listed)
                .withStyle(listed == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW));
    }
}
