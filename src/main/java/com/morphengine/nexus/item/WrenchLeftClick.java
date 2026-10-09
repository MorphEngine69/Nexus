package com.morphengine.nexus.item;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.NetworkBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.SwingAnimation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A left click with the Wrench on a block of a network sets the front of the block to the side that was struck, and
 * never breaks it: taking a block down is the sneaking right click.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
final class WrenchLeftClick {

    private WrenchLeftClick() {
    }

    @SubscribeEvent
    static void onLeftClick(final PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof WrenchItem)
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof NetworkBlock)) {
            return;
        }
        event.setCanceled(true);
        final Direction side = event.getFace();
        final boolean isStrike = event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START;
        if (side == null || event.getLevel().isClientSide() || !isStrike) {
            return;
        }
        final InteractionResult result = WrenchActions.turn(event.getLevel(), event.getPos(), event.getEntity(), side);
        if (result.consumesAction()) {
            event.getEntity().swing(event.getHand(), SwingAnimation.DEFAULT, false);
        }
    }
}
