package com.morphengine.nexus.level;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.entity.NetworkDeviceBlockEntity;
import com.morphengine.nexus.config.NexusConfig;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/**
 * Keeps loaded the chunk of every block entity that holds a Chunk Loader
 * Upgrade, with its blocks ticking, so a network works while nobody is near:
 * a Network Transmitter reaches a Network Receiver in another dimension, an
 * Assembler goes on crafting.
 *
 * <p>Each such block entity owns one ticket, named by its position, on the
 * chunk it stands in. Tickets of several blocks in one chunk share the one
 * chunk ticket NeoForge adds, and the chunk is let go when the last of them
 * is. NeoForge saves the tickets with the level, so the chunks are loaded again
 * after a restart before any player joins.
 *
 * <p>Asking is idempotent, and nothing is asked about on load: a block entity
 * only follows what its upgrade slots hold when they change, and lets go when
 * it is broken. A block entity that is merely unloaded keeps its ticket.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class ChunkAnchors {

    private static final TicketController CONTROLLER =
            new TicketController(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "chunk_loader"));

    private ChunkAnchors() {
    }

    @SubscribeEvent
    static void registerController(final RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /**
     * Brings the ticket of {@code owner} in step with {@code upgrades}: held
     * while they include a Chunk Loader Upgrade, let go otherwise. Does nothing
     * on the client.
     */
    public static void follow(final BlockEntity owner, final Container upgrades) {
        final boolean wanted = UpgradeLimits.count(upgrades, UpgradeTypes.CHUNK_LOADER.get()) > 0;
        setHeld(owner, wanted && isWithinLimit(owner));
    }

    /**
     * @return whether the network of {@code owner} may keep one more chunk loaded under the limit the settings of the
     *         world set; always for a block in no network
     */
    private static boolean isWithinLimit(final BlockEntity owner) {
        final NetworkController network = owner instanceof NetworkController nexus ? nexus
                : owner instanceof NetworkDeviceBlockEntity device ? device.controller() : null;
        return !(network instanceof BlockEntity nexusBlock)
                || network.component(NetworkComponentTypes.CHUNK_LOADERS)
                        .allows(owner, nexusBlock, NexusConfig.chunkLimit());
    }

    /**
     * Lets go of the ticket of {@code owner}, for when it is broken. Does nothing
     * on the client.
     */
    public static void release(final BlockEntity owner) {
        setHeld(owner, false);
    }

    private static void setHeld(final BlockEntity owner, final boolean held) {
        if (owner.getLevel() instanceof ServerLevel level) {
            final BlockPos pos = owner.getBlockPos();
            CONTROLLER.forceChunk(level, pos, SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getZ()), held, false);
        }
    }
}
