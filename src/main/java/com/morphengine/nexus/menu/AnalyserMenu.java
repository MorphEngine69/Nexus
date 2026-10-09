package com.morphengine.nexus.menu;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.access.Secured;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.networking.AnalyserViewPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The panel of an Analyser: what the block it was used on draws, supplies and pays in fees, refreshed every second
 * while the player stands within reach of the block and may still open it.
 */
public final class AnalyserMenu extends AbstractContainerMenu implements PanelMenu {

    private static final int REFRESH_INTERVAL_TICKS = 20;
    /** The reach vanilla containers allow. */
    private static final double REACH = 4.0;

    private final Player player;
    private final BlockPos pos;
    private final @Nullable BlockEntity target;
    private int ticksSinceSent = REFRESH_INTERVAL_TICKS;
    /** On the server the view last sent, on the client the view last received. */
    private AnalyserView view = AnalyserView.EMPTY;

    public AnalyserMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.ANALYSER.get(), containerId);
        this.player = inventory.player;
        this.pos = pos;
        this.target = player.level().getBlockEntity(pos);
    }

    public AnalyserView view() {
        return view;
    }

    public void acceptView(final AnalyserView received) {
        view = received;
    }

    @Override
    public Component defaultTitle() {
        return Component.translatable("item.nexus.nexus_analyser");
    }

    @Override
    public boolean stillValid(final Player viewer) {
        return target != null && !target.isRemoved() && viewer.canInteractWithBlock(pos, REACH)
                && mayOpen(viewer, target);
    }

    private static boolean mayOpen(final Player viewer, final BlockEntity block) {
        return !(block instanceof Secured secured) || NetworkAccess.permits(viewer, secured, Permission.OPEN);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer viewer) || target == null) {
            return;
        }
        if (++ticksSinceSent < REFRESH_INTERVAL_TICKS) {
            return;
        }
        ticksSinceSent = 0;
        final AnalyserView current = AnalyserViews.of(target, viewer.level().getGameTime());
        if (!current.equals(view)) {
            view = current;
            PacketDistributor.sendToPlayer(viewer, new AnalyserViewPayload(containerId, current));
        }
    }

    @Override
    public ItemStack quickMoveStack(final Player clicker, final int slotIndex) {
        return ItemStack.EMPTY;
    }
}
