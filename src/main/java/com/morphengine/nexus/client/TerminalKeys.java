package com.morphengine.nexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.networking.PortableTerminalPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The keys of the Nexus Terminal the player carries: one opens it, one switches its mode. They work wherever the
 * terminal is carried, in a hand, in Curios or in the inventory, and are set in the controls of the game.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID, value = Dist.CLIENT)
public final class TerminalKeys {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "nexus"));
    private static final KeyMapping OPEN =
            new KeyMapping("key.nexus.open_terminal", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, CATEGORY);
    private static final KeyMapping SWITCH_MODE =
            new KeyMapping(
                    "key.nexus.switch_terminal_mode", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, CATEGORY);

    private TerminalKeys() {
    }

    @SubscribeEvent
    static void register(final RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN);
        event.register(SWITCH_MODE);
    }

    @SubscribeEvent
    static void tick(final ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        while (OPEN.consumeClick()) {
            ClientPacketDistributor.sendToServer(new PortableTerminalPayload(PortableTerminalPayload.Action.OPEN));
        }
        while (SWITCH_MODE.consumeClick()) {
            ClientPacketDistributor.sendToServer(
                    new PortableTerminalPayload(PortableTerminalPayload.Action.SWITCH_MODE));
        }
    }
}
