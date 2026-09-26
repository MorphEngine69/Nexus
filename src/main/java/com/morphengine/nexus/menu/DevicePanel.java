package com.morphengine.nexus.menu;

import com.morphengine.nexus.networking.DeviceRenamePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A menu bound to one device, whose panel title is the device's name.
 */
public interface DevicePanel extends PanelMenu {

    DeviceBinding<?> binding();

    @Override
    default CustomPacketPayload renamePayload(final String name) {
        return new DeviceRenamePayload(binding().pos(), name);
    }

    @Override
    default Component defaultTitle() {
        return binding().defaultTitle();
    }
}
