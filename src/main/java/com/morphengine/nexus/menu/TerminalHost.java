package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.entity.BlueprintEncoder;
import com.morphengine.nexus.block.entity.TerminalCraftingGrid;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.terminal.TerminalStatus;
import org.jspecify.annotations.Nullable;

/**
 * What a terminal menu works for: a terminal block, or a Nexus Terminal in a
 * player's hand. It reaches a network's storage while it is {@link
 * TerminalStatus#ONLINE}, keeps the player's settings, and may keep a crafting
 * grid or a Blueprint encoder. Server side only.
 */
public interface TerminalHost {

    TerminalStatus status();

    /**
     * @return everything the network holds, its energy pool included, while
     *         online; {@code null} otherwise
     */
    @Nullable Storage onlineResources();

    /**
     * @return the network's storage while online; {@code null} otherwise
     */
    @Nullable NetworkStorage onlineStorage();

    /**
     * @return the network's autocrafting while online; {@code null} otherwise
     */
    @Nullable AutocraftingComponent onlineAutocrafting();

    /**
     * @return name and color of the network; {@code null} when there is none
     */
    @Nullable NetworkBadge networkBadge();

    void changeSettings(TerminalSettings settings);

    /**
     * @return the crafting grid; {@code null} for a host without one
     */
    @Nullable TerminalCraftingGrid craftingGrid();

    /**
     * @return the Blueprint encoder; {@code null} for a host without one
     */
    @Nullable BlueprintEncoder encoder();
}
