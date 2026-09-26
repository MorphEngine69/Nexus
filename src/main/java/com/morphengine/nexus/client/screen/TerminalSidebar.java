package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.resource.NexusResourceType;
import com.morphengine.nexus.resource.ResourceTypes;
import com.morphengine.nexus.terminal.EnumCycle;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The column of buttons left of a terminal panel: what to sort by, in which
 * direction, which type of resource to show, and how tall the panel is. A left
 * click steps to the next choice, a right click to the previous one.
 */
final class TerminalSidebar {

    private static final int BUTTON_SIZE = 18;
    private static final int ICON_SIZE = 16;
    private static final int SPACING = 20;
    private static final int OFFSET_LEFT = 22;
    private static final int OFFSET_TOP = 18;

    private final int left;
    private final int top;

    /**
     * @param panelLeft left edge of the panel; the buttons stand just left of it
     */
    TerminalSidebar(final int panelLeft, final int panelTop) {
        this.left = panelLeft - OFFSET_LEFT;
        this.top = panelTop + OFFSET_TOP;
    }

    boolean contains(final double x, final double y) {
        return area().contains(x, y);
    }

    PanelBounds area() {
        return new PanelBounds(left, top, BUTTON_SIZE, Control.values().length * SPACING);
    }

    void draw(final GuiGraphicsExtractor graphics, final PanelStyle style, final TerminalSettings settings,
              final int mouseX, final int mouseY) {
        for (Control control : Control.values()) {
            final PanelBounds bounds = boundsOf(control);
            graphics.fill(bounds.left(), bounds.top(), bounds.left() + BUTTON_SIZE, bounds.top() + BUTTON_SIZE,
                    style.buttonFill());
            graphics.outline(bounds.left(), bounds.top(), BUTTON_SIZE, BUTTON_SIZE,
                    bounds.contains(mouseX, mouseY) ? PanelStyle.TEXT_LIGHT : style.border());
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon(control, settings), bounds.left() + 1,
                    bounds.top() + 1, ICON_SIZE, ICON_SIZE);
        }
    }

    /**
     * @return the tooltip of the button under the cursor; empty when there is none
     */
    List<Component> tooltip(final TerminalSettings settings, final double mouseX, final double mouseY) {
        final Control control = controlAt(mouseX, mouseY);
        if (control == null) {
            return List.of();
        }
        final String key = "gui.nexus.terminal." + control.key;
        return List.of(Component.translatable(key),
                Component.translatable(key + "." + choiceOf(control, settings)).withStyle(ChatFormatting.GRAY));
    }

    /**
     * @return the settings after a click at {@code (x, y)}; {@code null} when no button is there
     */
    @Nullable TerminalSettings click(
            final TerminalSettings settings, final double x, final double y, final boolean backwards) {
        final Control control = controlAt(x, y);
        if (control == null) {
            return null;
        }
        return switch (control) {
            case SORT -> settings.withSort(EnumCycle.step(settings.sort(), backwards));
            case DIRECTION -> settings.withDirection(EnumCycle.step(settings.direction(), backwards));
            case TYPE -> settings.withShownType(nextType(settings.shownType(), backwards));
            case SIZE -> settings.withSize(EnumCycle.step(settings.size(), backwards));
        };
    }

    private @Nullable Control controlAt(final double x, final double y) {
        for (Control control : Control.values()) {
            if (boundsOf(control).contains(x, y)) {
                return control;
            }
        }
        return null;
    }

    private PanelBounds boundsOf(final Control control) {
        return new PanelBounds(left, top + control.ordinal() * SPACING, BUTTON_SIZE, BUTTON_SIZE);
    }

    /**
     * Cycles through showing everything, then each registered resource type.
     */
    private static @Nullable Identifier nextType(final @Nullable Identifier current, final boolean backwards) {
        final List<@Nullable Identifier> choices = new ArrayList<>();
        choices.add(null);
        for (NexusResourceType<?> type : ResourceTypes.REGISTRY) {
            choices.add(ResourceTypes.idOf(type));
        }
        final int index = Math.max(0, choices.indexOf(current));
        final int step = backwards ? choices.size() - 1 : 1;
        return choices.get((index + step) % choices.size());
    }

    private static String choiceOf(final Control control, final TerminalSettings settings) {
        return switch (control) {
            case SORT -> settings.sort().getSerializedName();
            case DIRECTION -> settings.direction().getSerializedName();
            case TYPE -> settings.shownType() == null ? "all" : settings.shownType().getPath();
            case SIZE -> settings.size().getSerializedName();
        };
    }

    private static Identifier icon(final Control control, final TerminalSettings settings) {
        final String choice = choiceOf(control, settings);
        final boolean ownType = control != Control.TYPE || settings.shownType() == null
                || Nexus.MOD_ID.equals(settings.shownType().getNamespace());
        return Identifier.fromNamespaceAndPath(Nexus.MOD_ID,
                "terminal/" + control.key + "_" + (ownType ? choice : "other"));
    }

    private enum Control {
        SORT,
        DIRECTION,
        TYPE,
        SIZE;

        private final String key = name().toLowerCase(Locale.ROOT);
    }
}
