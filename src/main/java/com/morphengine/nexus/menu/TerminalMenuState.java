package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.terminal.GridClick;
import com.morphengine.nexus.terminal.TerminalActions;
import com.morphengine.nexus.terminal.TerminalContents;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalSession;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What both terminal menus share: the terminal they are bound to, its kind
 * and settings as opened, on the server the session that feeds the client, on
 * the client what it was fed.
 */
public final class TerminalMenuState {

    private final DeviceBinding<TerminalBlockEntity> binding;
    private final TerminalSettings settings;
    private final TerminalKind kind;
    private final TerminalContents contents = new TerminalContents();
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    private final int containerId;
    private final @Nullable TerminalSession session;
    private @Nullable NetworkBadge badge;

    public TerminalMenuState(
            final Inventory inventory, final BlockPos pos, final TerminalKind kind, final TerminalSettings settings,
            final int containerId) {
        this.binding = new DeviceBinding<>(inventory, pos, TerminalBlockEntity.class);
        this.settings = settings;
        this.kind = kind;
        this.containerId = containerId;
        final ServerPlayer viewer = binding.viewer();
        final TerminalBlockEntity terminal = binding.blockEntity();
        this.session = viewer != null && terminal != null ? new TerminalSession(viewer, containerId, terminal) : null;
    }

    public DeviceBinding<TerminalBlockEntity> binding() {
        return binding;
    }

    /**
     * @return the settings the menu was opened with
     */
    public TerminalSettings settings() {
        return settings;
    }

    public TerminalKind kind() {
        return kind;
    }

    /**
     * @return what the client knows of the network's storage; empty on the server
     */
    public TerminalContents contents() {
        return contents;
    }

    public @Nullable NetworkBadge badge() {
        return badge;
    }

    public void acceptBadge(final @Nullable NetworkBadge received) {
        badge = received;
    }

    /**
     * Feeds the client what changed. Server side, once per tick.
     */
    public void tick() {
        final ServerPlayer viewer = binding.viewer();
        final TerminalBlockEntity terminal = binding.blockEntity();
        if (session == null || viewer == null || terminal == null) {
            return;
        }
        session.tick();
        badgeSync.tick(viewer, containerId, terminal.networkBadge());
    }

    public void close() {
        if (session != null) {
            session.close();
        }
        binding.release();
    }

    /**
     * @return the network's storage while the terminal is online; {@code null} otherwise
     */
    public @Nullable Storage onlineStorage() {
        final TerminalBlockEntity terminal = binding.blockEntity();
        return terminal != null ? terminal.onlineStorage() : null;
    }

    public void click(
            final ServerPlayer player, final AbstractContainerMenu menu, final @Nullable NexusResource resource,
            final GridClick click) {
        final Storage storage = onlineStorage();
        if (storage != null) {
            new TerminalActions(player, menu, storage).click(resource, click);
        }
    }

    /**
     * Stores as much of {@code stack} as the network takes and shrinks it by that much.
     */
    public void insert(final ServerPlayer player, final AbstractContainerMenu menu, final ItemStack stack) {
        final Storage storage = onlineStorage();
        if (storage != null) {
            new TerminalActions(player, menu, storage).insert(stack);
        }
    }
}
