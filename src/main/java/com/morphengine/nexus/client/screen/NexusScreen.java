package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.Network;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.NetworkColoring;
import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.menu.NexusMenu;
import com.morphengine.nexus.networking.NexusEnergyTabPayload;
import com.morphengine.nexus.networking.NexusRecolorPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Nexus panel, in three tabs chosen by the buttons left of it: the network,
 * with its name in the title, its color, its figures, the upgrade slots and
 * the inventory; its energy, which device draws and which supplies how much;
 * and its access, who may do what with it. The tabs but the first put the slots
 * out of sight, as they need the room.
 */
public final class NexusScreen extends PanelScreen<NexusMenu> implements SideAreas {

    private static final int IMAGE_WIDTH = 236;
    private static final int IMAGE_HEIGHT = NexusMenu.INVENTORY_TOP + 84;
    private static final int LABEL_GAP = 11;
    private static final int PADDING = 8;
    private static final int ROW_GAP = 8;
    private static final int LINE_HEIGHT = 11;
    private static final int SWATCH_SIZE = 14;
    private static final int SWATCH_GAP = 4;
    private static final int SWATCHES_PER_ROW = 8;
    private static final int SWATCH_LABEL_GAP = 4;
    private static final int UNSELECTED_SWATCH_BORDER = 0xFF2C2D32;
    private static final int CONFLICT_TEXT = 0xFFE8605A;
    /** Where a slot goes while the access tab hides it: far outside any screen. */
    private static final int HIDDEN = -10_000;

    private final List<Swatch> swatches = new ArrayList<>();
    private final StatLine statLine = new StatLine();
    private final List<int[]> slotPlaces = new ArrayList<>();
    private Tab tab = Tab.NETWORK;
    private @Nullable AccessPanel access;
    private @Nullable EnergyPanel energy;
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
        if (slotPlaces.isEmpty()) {
            for (Slot slot : getMenu().slots) {
                slotPlaces.add(new int[] {slot.x, slot.y});
            }
        }
        if (access == null && minecraft != null && minecraft.player != null) {
            access = new AccessPanel(getMenu(), font, minecraft.player.getUUID());
        }
        if (energy == null) {
            energy = new EnergyPanel(getMenu(), font);
        }
        showSlots();
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

    /**
     * Puts the slots where the menu has them on the network tab, and out of sight on the access tab.
     */
    private void showSlots() {
        for (int index = 0; index < getMenu().slots.size() && index < slotPlaces.size(); index++) {
            final Slot slot = getMenu().slots.get(index);
            slot.x = tab == Tab.NETWORK ? slotPlaces.get(index)[0] : HIDDEN;
            slot.y = tab == Tab.NETWORK ? slotPlaces.get(index)[1] : HIDDEN;
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
        return PanelStyle.of(network != null ? network.color() : null);
    }

    private static int rgbOf(final DyeColor color) {
        return NetworkColoring.colorOf(color).rgb();
    }

    @Override
    public List<Rect2i> extraAreas() {
        return List.of(tabs().area().toRect());
    }

    private SideButtons tabs() {
        return new SideButtons(leftPos, topPos, Tab.values().length);
    }

    private EnergyLayout energyLayout() {
        return EnergyLayout.of(leftPos, topPos, imageWidth);
    }

    private AccessLayout accessLayout() {
        return AccessLayout.of(leftPos, topPos, imageWidth);
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        final SideButtons buttons = tabs();
        for (Tab shown : Tab.values()) {
            buttons.draw(graphics, style, shown.ordinal(), shown.icon(),
                    shown == tab || buttons.buttonAt(mouseX, mouseY) == shown.ordinal());
        }
        if (tab == Tab.ACCESS && access != null) {
            access.draw(graphics, style, accessLayout());
            return;
        }
        if (tab == Tab.ENERGY && energy != null) {
            energy.draw(graphics, style, energyLayout());
            return;
        }
        drawNetworkTab(graphics, style);
    }

    private void drawNetworkTab(final GuiGraphicsExtractor graphics, final PanelStyle style) {
        statLine.begin();
        graphics.text(font, Component.translatable("gui.nexus.color"), leftPos + PADDING, colorLabelY,
                PanelStyle.TEXT_DIM, false);
        renderSwatches(graphics, currentNetwork());
        final NexusBlockEntity nexus = getMenu().blockEntity();
        if (nexus != null && NexusBlock.isInConflict(nexus.getBlockState())) {
            graphics.textWithWordWrap(font, Component.translatable("gui.nexus.conflict"), leftPos + PADDING, statsY,
                    NexusMenu.UPGRADES_LEFT - PADDING * 2, CONFLICT_TEXT);
        } else {
            renderStats(graphics, leftPos + PADDING, statsY);
        }
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        graphics.text(font, playerInventoryTitle, leftPos + NexusMenu.INVENTORY_LEFT,
                topPos + NexusMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
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
        final List<StatLine.Stat> lines = List.of(
                StatLine.Stat.plain(Component.translatable("gui.nexus.stats.devices", statistics.devices())),
                EnergyFormat.stored(statistics.energyStored(), statistics.energyCapacity()),
                EnergyFormat.rate("input", statistics.energyInput()),
                EnergyFormat.rate("output", statistics.energyOutput()),
                StatLine.Stat.plain(
                        Component.translatable("gui.nexus.stats.machines", statistics.count(DeviceRole.MACHINE))),
                StatLine.Stat.plain(
                        Component.translatable("gui.nexus.stats.pullers", statistics.count(DeviceRole.PULLER))),
                StatLine.Stat.plain(
                        Component.translatable("gui.nexus.stats.pushers", statistics.count(DeviceRole.PUSHER))),
                StatLine.Stat.plain(
                        Component.translatable("gui.nexus.stats.storages", statistics.count(DeviceRole.STORAGE))));
        int y = startY;
        for (StatLine.Stat line : lines) {
            y += statLine.draw(graphics, font, line,
                    new PanelBounds(x, y, NexusMenu.UPGRADES_LEFT - PADDING * 2, 0), PanelStyle.TEXT_DIM);
        }
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        final int button = tabs().buttonAt(mouseX, mouseY);
        if (button >= 0) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Tab.values()[button].label()), mouseX, mouseY);
        } else if (tab == Tab.ACCESS && access != null) {
            final List<Component> lines = access.tooltip(accessLayout(), mouseX, mouseY);
            if (!lines.isEmpty()) {
                graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            }
        } else if (tab == Tab.ENERGY && energy != null) {
            final List<Component> lines = energy.tooltip(energyLayout(), mouseX, mouseY);
            if (!lines.isEmpty()) {
                graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            }
        } else {
            statLine.showTooltip(graphics, font, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final int button = tabs().buttonAt(event.x(), event.y());
        if (button >= 0) {
            switchTo(Tab.values()[button]);
            return true;
        }
        return switch (tab) {
            case ENERGY -> energy != null && energy.click(energyLayout(), event.x(), event.y())
                    || super.mouseClicked(event, doubleClick);
            case ACCESS -> access != null && access.click(accessLayout(), event.x(), event.y())
                    || super.mouseClicked(event, doubleClick);
            case NETWORK -> clickNetworkTab(event) || super.mouseClicked(event, doubleClick);
        };
    }

    private boolean clickNetworkTab(final MouseButtonEvent event) {
        for (Swatch swatch : swatches) {
            if (swatch.bounds().contains(event.x(), event.y())) {
                ClientPacketDistributor.sendToServer(new NexusRecolorPayload(getMenu().pos(), rgbOf(swatch.color())));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        return scrollTab(x, y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private boolean scrollTab(final double x, final double y, final double amount) {
        return switch (tab) {
            case ACCESS -> access != null && access.scroll(accessLayout(), x, y, amount);
            case ENERGY -> energy != null && energy.scroll(energyLayout(), x, y, amount);
            case NETWORK -> false;
        };
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        return tab == Tab.ENERGY && energy != null && energy.drag(energyLayout(), event.y())
                || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (energy != null) {
            energy.release();
        }
        return super.mouseReleased(event);
    }

    /**
     * Shows {@code next}, and tells the server whether the energy report is wanted.
     */
    private void switchTo(final Tab next) {
        final boolean wasEnergy = tab == Tab.ENERGY;
        tab = next;
        showSlots();
        if (wasEnergy != (next == Tab.ENERGY)) {
            ClientPacketDistributor.sendToServer(new NexusEnergyTabPayload(getMenu().containerId, next == Tab.ENERGY));
        }
    }

    private record Swatch(DyeColor color, PanelBounds bounds) {
    }

    /**
     * The tabs of the panel, in the order of their buttons.
     */
    private enum Tab {
        NETWORK, ENERGY, ACCESS;

        Identifier icon() {
            return Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "nexus/tab_" + name().toLowerCase(Locale.ROOT));
        }

        Component label() {
            return Component.translatable("gui.nexus.access.tab." + name().toLowerCase(Locale.ROOT));
        }
    }
}
