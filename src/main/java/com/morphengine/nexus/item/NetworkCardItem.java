package com.morphengine.nexus.item;

import com.morphengine.nexus.block.WirelessBlock;
import com.morphengine.nexus.block.WirelessKind;
import com.morphengine.nexus.registry.NexusDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A Network Card: a right click on a Network Receiver links the card to it,
 * and a Network Transmitter holding the card carries its network there. Used
 * while sneaking in the air, it forgets its receiver.
 */
public final class NetworkCardItem extends Item {

    public NetworkCardItem(final Item.Properties properties) {
        super(properties);
    }

    /**
     * @return the receiver {@code stack} is linked to; {@code null} when it is none, or no card
     */
    public static @Nullable GlobalPos receiverOf(final ItemStack stack) {
        return stack.getItem() instanceof NetworkCardItem ? stack.get(NexusDataComponents.LINKED_RECEIVER.get()) : null;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof WirelessBlock wireless)
                || wireless.kind() != WirelessKind.RECEIVER) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            context.getItemInHand().set(NexusDataComponents.LINKED_RECEIVER.get(), GlobalPos.of(level.dimension(),
                    pos.immutable()));
            final Player player = context.getPlayer();
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("item.nexus.network_card.linked",
                        pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() || receiverOf(stack) == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            stack.remove(NexusDataComponents.LINKED_RECEIVER.get());
            player.sendOverlayMessage(Component.translatable("item.nexus.network_card.cleared"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        final GlobalPos receiver = receiverOf(stack);
        if (receiver == null) {
            builder.accept(Component.translatable("tooltip.nexus.network_card.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        final BlockPos pos = receiver.pos();
        builder.accept(Component.translatable("tooltip.nexus.network_card.linked", pos.getX(), pos.getY(),
                pos.getZ()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal(receiver.dimension().identifier().toString())
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
