package com.morphengine.nexus.menu;

import com.morphengine.nexus.access.NetworkSecurityData;
import com.morphengine.nexus.access.Secured;
import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.entity.BlueprintEncoder;
import com.morphengine.nexus.block.entity.TerminalCraftingGrid;
import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.security.NetworkSecurity;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.terminal.TerminalStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A Nexus Terminal a player carries, as the host of its menu. It reaches the
 * network of the Nexus it is bound to while that Nexus is loaded and a Nexus
 * Link of the network reaches the player. Its settings live on the item. Its
 * crafting grid and encoder are the menu's alone: when the menu closes, the
 * grid goes back to the network and the Blueprints to the player, as a
 * crafting table gives its grid back. Server thread only.
 */
final class PortableTerminal implements TerminalHost, Secured {

    private final ServerPlayer player;
    private final TerminalSlot carriedIn;
    private final @Nullable TerminalCraftingGrid grid;
    private final @Nullable BlueprintEncoder encoder;

    PortableTerminal(final ServerPlayer player, final TerminalSlot carriedIn, final TerminalKind mode) {
        this.player = player;
        this.carriedIn = carriedIn;
        this.grid = mode.hasCraftingGrid() ? new TerminalCraftingGrid(() -> { }) : null;
        this.encoder = mode.hasEncoder() ? new BlueprintEncoder(() -> { }) : null;
    }

    ItemStack stack() {
        return carriedIn.stackOf(player);
    }

    /**
     * @return the Nexus of the network the terminal is bound to, while it stands
     *         in a loaded chunk; {@code null} otherwise
     */
    private @Nullable NetworkController controller() {
        return NexusTerminalItem.nexusOf(stack(), player.level().getServer());
    }

    /**
     * @return the rules of the network the terminal is bound to, also while its
     *         Nexus is unloaded; none for a terminal bound to a network without
     *         rules, as nothing can be reached through it
     */
    @Override
    public AccessPolicy accessPolicy() {
        final NetworkController controller = controller();
        if (controller != null) {
            return controller.security();
        }
        final UUID network = NexusTerminalItem.boundNetwork(stack());
        final NetworkSecurity known = network != null
                ? NetworkSecurityData.of(player.level().getServer()).find(network) : null;
        return known != null ? known : AccessPolicy.UNRESTRICTED;
    }

    @Override
    public TerminalStatus status() {
        final NetworkController controller = controller();
        if (controller == null) {
            return TerminalStatus.NO_NETWORK;
        }
        if (!controller.component(NetworkComponentTypes.WIRELESS_ACCESS).reaches(player.level().dimension(),
                player.position())) {
            return TerminalStatus.OUT_OF_RANGE;
        }
        return controller.energy().stored() > 0 ? TerminalStatus.ONLINE : TerminalStatus.NO_ENERGY;
    }

    @Override
    public @Nullable NetworkController onlineNetwork() {
        return status() == TerminalStatus.ONLINE ? controller() : null;
    }

    @Override
    public @Nullable Storage onlineResources() {
        final NetworkController controller = onlineNetwork();
        return controller != null ? controller.resources() : null;
    }

    @Override
    public @Nullable NetworkStorage onlineStorage() {
        final NetworkController controller = onlineNetwork();
        return controller != null ? controller.component(NetworkComponentTypes.STORAGE).storage() : null;
    }

    @Override
    public @Nullable AutocraftingComponent onlineAutocrafting() {
        final NetworkController controller = onlineNetwork();
        return controller != null ? controller.component(NetworkComponentTypes.AUTOCRAFTING) : null;
    }

    @Override
    public @Nullable NetworkBadge networkBadge() {
        return NetworkBadge.of(controller());
    }

    @Override
    public void changeSettings(final TerminalSettings settings) {
        if (stack().getItem() instanceof NexusTerminalItem) {
            stack().set(NexusDataComponents.TERMINAL_SETTINGS.get(), settings);
        }
    }

    @Override
    public @Nullable TerminalCraftingGrid craftingGrid() {
        return grid;
    }

    @Override
    public @Nullable BlueprintEncoder encoder() {
        return encoder;
    }

    /**
     * Gives back what the menu held: the grid to the network, or to the player
     * what it does not take, and the encoder's Blueprints to the player.
     */
    void giveBack() {
        if (grid != null) {
            new CraftingGridFiller(player, grid, onlineStorage()).returnGrid();
        }
        if (encoder != null) {
            for (int slot = 0; slot < encoder.blueprints().getContainerSize(); slot++) {
                player.getInventory().placeItemBackInInventory(encoder.blueprints().removeItemNoUpdate(slot));
            }
        }
    }
}
