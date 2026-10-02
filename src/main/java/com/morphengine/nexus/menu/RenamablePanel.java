package com.morphengine.nexus.menu;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A panel whose title the player renames by clicking it.
 */
public interface RenamablePanel extends PanelMenu {

    /**
     * @param name the name typed by the player; blank restores the default name
     * @return the payload asking the server to rename what the panel shows
     */
    CustomPacketPayload renamePayload(String name);
}
