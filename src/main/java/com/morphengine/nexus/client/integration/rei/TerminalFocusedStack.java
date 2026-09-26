package com.morphengine.nexus.client.integration.rei;

import com.morphengine.nexus.client.screen.TerminalScreen;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.registry.screen.FocusedStackProvider;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Shows REI the resource under the cursor in a terminal's grid, so its recipe
 * and usage keys work there.
 */
final class TerminalFocusedStack implements FocusedStackProvider {

    @Override
    public CompoundEventResult<EntryStack<?>> provide(final Screen screen, final Point mouse) {
        if (!(screen instanceof TerminalScreen<?> terminal)) {
            return CompoundEventResult.pass();
        }
        final TerminalScreen.HoveredResource hovered = terminal.hoveredResource(mouse.x, mouse.y);
        if (hovered == null) {
            return CompoundEventResult.pass();
        }
        return switch (hovered.resource()) {
            case ItemKey item -> CompoundEventResult.interruptTrue(EntryStacks.of(item.toStack(1)));
            case FluidKey fluid -> CompoundEventResult.interruptTrue(
                    EntryStacks.of(fluid.fluid().getFluid(), FluidType.BUCKET_VOLUME));
            default -> CompoundEventResult.pass();
        };
    }
}
