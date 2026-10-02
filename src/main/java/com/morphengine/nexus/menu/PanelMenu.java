package com.morphengine.nexus.menu;

import net.minecraft.network.chat.Component;

/**
 * A menu shown in a Nexus panel, with a title in its header.
 */
public interface PanelMenu {

    /**
     * @return the title shown when nothing was named
     */
    Component defaultTitle();
}
