package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.assembler.LockMode;
import com.morphengine.nexus.block.AssemblerChain;
import com.morphengine.nexus.level.NeighbourCapabilities;
import com.morphengine.nexus.level.SideStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * The machine an Assembler works with: the one at the end of its {@link
 * AssemblerChain}, reached from the side the root of the chain touches.
 * Server thread only.
 */
final class ChainedMachine {

    private final NeighbourCapabilities neighbour = new NeighbourCapabilities();

    /**
     * @param assembler position of the Assembler asking
     * @return the machine's items and fluids, as the root of the chain sees them
     */
    SideStorage of(final ServerLevel level, final BlockPos assembler) {
        return accessTo(level, AssemblerChain.linkOf(level, assembler));
    }

    /**
     * A machine of Nexus is reached through all its slots whatever its sides say; another block through the side the
     * Assembler touches.
     */
    private SideStorage accessTo(final ServerLevel level, final AssemblerChain.Link link) {
        if (level.getBlockEntity(link.root().relative(link.face())) instanceof MachineBlockEntity machine) {
            return machine.assemblerAccess();
        }
        return neighbour.itemsAndFluids(level, link.root(), link.face());
    }

    /**
     * @param assembler position of the Assembler asking
     * @return whether a run must wait: the root of the chain waits for its
     *         machine, and the machine holds an input of a processing
     *         Blueprint of any Assembler of the chain
     */
    boolean isBusy(final ServerLevel level, final BlockPos assembler, final AssemblerWork work) {
        final AssemblerChain.Link link = AssemblerChain.linkOf(level, assembler);
        if (!(level.getBlockEntity(link.root()) instanceof AssemblerBlockEntity root)
                || root.settings().lock() != LockMode.UNTIL_EMPTY) {
            return false;
        }
        return work.isBusy(accessTo(level, link), chainBlueprints(level, link));
    }

    private static List<Blueprint> chainBlueprints(final ServerLevel level, final AssemblerChain.Link link) {
        final List<Blueprint> blueprints = new ArrayList<>();
        for (BlockPos member : AssemblerChain.membersOf(level, link.root())) {
            if (level.getBlockEntity(member) instanceof AssemblerBlockEntity assembler) {
                blueprints.addAll(assembler.processingBlueprints());
            }
        }
        return blueprints;
    }
}
