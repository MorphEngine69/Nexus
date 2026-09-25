package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.network.Network;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.NetworkColoring;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.menu.NexusMenu;
import com.morphengine.nexus.networking.NexusRecolorPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Nexus panel: the network's name in the title, its color and its figures.
 */
public final class NexusScreen extends DeviceScreen<NexusMenu> {

    private static final int IMAGE_WIDTH = 236;
    private static final int IMAGE_HEIGHT = 184;
    private static final int PADDING = 8;
    private static final int ROW_GAP = 8;
    private static final int LINE_HEIGHT = 11;
    private static final int SWATCH_SIZE = 14;
    private static final int SWATCH_GAP = 4;
    private static final int SWATCHES_PER_ROW = 8;
    private static final int SWATCH_LABEL_GAP = 4;
    private static final int UNSELECTED_SWATCH_BORDER = 0xFF2C2D32;
    private static final int CONFLICT_TEXT = 0xFFE8605A;

    private final List<Swatch> swatches = new ArrayList<>();
    private int colorLabelY;
    private int statsY;

    public NexusScreen(final NexusMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        colorLabelY = topPos + PanelStyle.HEADER_HEIGHT + PADDING;
        final int swatchesY = colorLabelY + LINE_HEIGHT + SWATCH_LABEL_GAP;
        buildSwatches(swatchesY);
        final int swatchRows = (DyeColor.values().length + SWATCHES_PER_ROW - 1) / SWATCHES_PER_ROW;
        statsY = swatchesY + swatchRows * (SWATCH_SIZE + SWATCH_GAP) + ROW_GAP;
    }

    private void buildSwatches(final int startY) {
        swatches.clear();
        final int startX = leftPos + PADDING;
        final DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < colors.length; i++) {
            final int column = i % SWATCHES_PER_ROW;
            final int row = i / SWATCHES_PER_ROW;
            final int x = startX + column * (SWATCH_SIZE + SWATCH_GAP);
            final int y = startY + row * (SWATCH_SIZE + SWATCH_GAP);
            swatches.add(new Swatch(colors[i], new PanelBounds(x, y, SWATCH_SIZE, SWATCH_SIZE)));
        }
    }

    private @Nullable Network currentNetwork() {
        return getMenu().blockEntity() != null ? getMenu().blockEntity().network() : null;
    }

    @Override
    protected Component panelTitle() {
        final Network network = currentNetwork();
        return network != null ? Component.literal(network.name()) : super.panelTitle();
    }

    @Override
    protected PanelStyle style() {
        final Network network = currentNetwork();
        return network != null ? PanelStyle.tinted(network.color().rgb()) : PanelStyle.neutral();
    }

    private static int rgbOf(final DyeColor color) {
        return NetworkColoring.colorOf(color).rgb();
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        graphics.text(font, Component.translatable("gui.nexus.color"), leftPos + PADDING, colorLabelY,
                PanelStyle.TEXT_DIM, false);
        renderSwatches(graphics, currentNetwork());
        final NexusBlockEntity nexus = getMenu().blockEntity();
        if (nexus != null && nexus.isInConflict()) {
            graphics.textWithWordWrap(font, Component.translatable("gui.nexus.conflict"), leftPos + PADDING, statsY,
                    imageWidth - PADDING * 2, CONFLICT_TEXT);
        } else {
            renderStats(graphics, leftPos + PADDING, statsY);
        }
    }

    private void renderSwatches(final GuiGraphicsExtractor graphics, final @Nullable Network network) {
        for (Swatch swatch : swatches) {
            final PanelBounds bounds = swatch.bounds();
            final boolean selected = network != null && network.color().rgb() == rgbOf(swatch.color());
            final int border = selected ? PanelStyle.TEXT_LIGHT : UNSELECTED_SWATCH_BORDER;
            graphics.fill(bounds.left() - 1, bounds.top() - 1,
                    bounds.left() + bounds.width() + 1, bounds.top() + bounds.height() + 1, border);
            graphics.fill(bounds.left(), bounds.top(), bounds.left() + bounds.width(), bounds.top() + bounds.height(),
                    ARGB.opaque(rgbOf(swatch.color())));
        }
    }

    private void renderStats(final GuiGraphicsExtractor graphics, final int x, final int startY) {
        final NetworkStatistics statistics = getMenu().statistics();
        final List<Component> lines = List.of(
                Component.translatable("gui.nexus.stats.devices", statistics.devices()),
                Component.translatable("gui.nexus.stats.energy",
                        EnergyFormat.amount(statistics.energyStored()) + " / "
                                + EnergyFormat.amount(statistics.energyCapacity())),
                Component.translatable("gui.nexus.stats.input", EnergyFormat.amount(statistics.energyInput())),
                Component.translatable("gui.nexus.stats.output", EnergyFormat.amount(statistics.energyOutput())),
                Component.translatable("gui.nexus.stats.machines", 0),
                Component.translatable("gui.nexus.stats.pullers", 0),
                Component.translatable("gui.nexus.stats.pushers", 0),
                Component.translatable("gui.nexus.stats.storages", 0));
        int y = startY;
        for (Component line : lines) {
            graphics.text(font, line, x, y, PanelStyle.TEXT_DIM, false);
            y += LINE_HEIGHT;
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        for (Swatch swatch : swatches) {
            if (swatch.bounds().contains(event.x(), event.y())) {
                ClientPacketDistributor.sendToServer(new NexusRecolorPayload(getMenu().pos(), rgbOf(swatch.color())));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private record Swatch(DyeColor color, PanelBounds bounds) {
    }
}
