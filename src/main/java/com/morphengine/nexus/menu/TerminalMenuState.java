package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.networking.CraftPlanPayload;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.terminal.CraftRequest;
import com.morphengine.nexus.terminal.GridClick;
import com.morphengine.nexus.terminal.PlanPreview;
import com.morphengine.nexus.terminal.TerminalActions;
import com.morphengine.nexus.terminal.TerminalContents;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalSession;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * What every terminal menu shares: what it is bound to, its kind and settings
 * as opened, on the server the session that feeds the client, on the client
 * what it was fed.
 */
public final class TerminalMenuState {

    private final TerminalBinding binding;
    private final TerminalSettings settings;
    private final TerminalKind kind;
    private final TerminalContents contents = new TerminalContents();
    private final NetworkBadgeSync badgeSync = new NetworkBadgeSync();
    private final int containerId;
    private final @Nullable TerminalSession session;
    private @Nullable NetworkBadge badge;

    public TerminalMenuState(final TerminalOpening opening, final TerminalKind kind, final int containerId) {
        this.binding = opening.binding();
        this.settings = opening.settings();
        this.kind = kind;
        this.containerId = containerId;
        final ServerPlayer viewer = binding.viewer();
        final TerminalHost host = binding.host();
        this.session = viewer != null && host != null ? new TerminalSession(viewer, containerId, host) : null;
    }

    public TerminalBinding binding() {
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
        final TerminalHost host = binding.host();
        if (session == null || viewer == null || host == null) {
            return;
        }
        session.tick();
        badgeSync.tick(viewer, containerId, host.networkBadge());
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
        final TerminalHost host = binding.host();
        return host != null ? host.onlineStorage() : null;
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
     * Plans crafting {@code amount} of {@code resource}, or for {@link
     * CraftRequest#CRAFT_LESS} the most of it that can start, and, when asked
     * to and the plan can start, starts it; the player is sent the plan either way.
     */
    public void requestCraft(
            final ServerPlayer player, final NexusResource resource, final long amount, final CraftRequest request) {
        final TerminalHost host = binding.host();
        final AutocraftingComponent autocrafting = host != null ? host.onlineAutocrafting() : null;
        final Storage storage = onlineStorage();
        if (autocrafting == null || storage == null || amount <= 0) {
            return;
        }
        final CraftingPlan plan = request == CraftRequest.CRAFT_LESS
                ? autocrafting.planLargest(resource, amount, storage)
                : autocrafting.plan(resource, amount, storage);
        final boolean started = request == CraftRequest.START && autocrafting.start(plan, player.getName().getString());
        PacketDistributor.sendToPlayer(player, new CraftPlanPayload(containerId, PlanPreview.of(plan),
                outcomeOf(request, started)));
    }

    private static CraftRequest outcomeOf(final CraftRequest request, final boolean started) {
        return switch (request) {
            case START -> started ? CraftRequest.START : CraftRequest.PREVIEW;
            case CRAFT_LESS -> CraftRequest.CRAFT_LESS;
            case PREVIEW -> CraftRequest.PREVIEW;
        };
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
