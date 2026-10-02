package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.AssemblerChain;
import com.morphengine.nexus.menu.AssemblerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Assembler panel, tinted with its network's color: the network, the
 * priority, the Blueprint slots with the machine it faces and the number of
 * its tasks beside them, the upgrade slots and the inventory, with the lock
 * button left of the panel.
 */
public final class AssemblerScreen extends PanelScreen<AssemblerMenu> {

    private static final int IMAGE_WIDTH = 224;
    private static final int IMAGE_HEIGHT = AssemblerMenu.INVENTORY_TOP + 84;
    private static final int PRIORITY_TOP = 32;
    private static final int LABEL_GAP = 11;
    private static final int INFO_LEFT = AssemblerMenu.BLUEPRINTS_LEFT + 3 * PanelStyle.SLOT_SIZE + 10;
    private static final int INFO_LINE = 12;
    /** The number of tasks stands a blank line below the name of the machine. */
    private static final int TASKS_LINE = 3 * INFO_LINE;

    private PriorityRow priority;

    public AssemblerScreen(final AssemblerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.priority = createPriorityRow();
    }

    @Override
    protected void init() {
        super.init();
        priority = createPriorityRow();
    }

    private PriorityRow createPriorityRow() {
        return new PriorityRow(panelBounds(), topPos + PRIORITY_TOP, AssemblerMenu.BUTTON_PRIORITY);
    }

    private SideButtons sideButtons() {
        return new SideButtons(leftPos, topPos, 1);
    }

    /**
     * @return the lock button left of the panel, for a recipe viewer to keep clear of
     */
    public Rect2i sidebarArea() {
        return sideButtons().area().toRect();
    }

    @Override
    protected PanelStyle style() {
        return PanelStyle.of(getMenu().badge());
    }

    @Override
    protected void extractPanel(
            final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        PanelStyle.drawNetwork(graphics, font, getMenu().badge(), leftPos, topPos);
        priority.draw(graphics, font, style, getMenu().priority());
        graphics.text(font, Component.translatable("gui.nexus.assembler.blueprints"),
                leftPos + AssemblerMenu.BLUEPRINTS_LEFT, topPos + AssemblerMenu.BLUEPRINTS_TOP - LABEL_GAP,
                PanelStyle.TEXT_DIM, false);
        for (Slot slot : getMenu().slots) {
            style.drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        drawInfo(graphics);
        final SideButtons buttons = sideButtons();
        buttons.draw(graphics, style, 0, lockIcon(), buttons.buttonAt(mouseX, mouseY) == 0);
        graphics.text(font, playerInventoryTitle, leftPos + AssemblerMenu.INVENTORY_LEFT,
                topPos + AssemblerMenu.INVENTORY_TOP - LABEL_GAP, PanelStyle.TEXT_DIM, false);
    }

    /**
     * The block the Assembler faces, read from the world the client sees, and
     * how many tasks it keeps.
     */
    private void drawInfo(final GuiGraphicsExtractor graphics) {
        final int left = leftPos + INFO_LEFT;
        final int top = topPos + AssemblerMenu.BLUEPRINTS_TOP;
        graphics.text(font, Component.translatable("gui.nexus.assembler.machine"), left, top, PanelStyle.TEXT_DIM,
                false);
        graphics.text(font, machineName(), left, top + INFO_LINE, PanelStyle.TEXT_LIGHT, false);
        graphics.text(font, Component.translatable("gui.nexus.assembler.tasks", getMenu().taskCount()), left,
                top + TASKS_LINE, PanelStyle.TEXT_DIM, false);
    }

    private Component machineName() {
        if (minecraft == null || minecraft.level == null) {
            return Component.empty();
        }
        final BlockState assembler = minecraft.level.getBlockState(getMenu().pos());
        if (!(assembler.getBlock() instanceof AssemblerBlock)) {
            return Component.empty();
        }
        final BlockPos machine = AssemblerChain.linkOf(minecraft.level, getMenu().pos()).machine();
        final BlockState state = minecraft.level.getBlockState(machine);
        return state.isAir() ? Component.translatable("gui.nexus.assembler.no_machine")
                : state.getBlock().getName();
    }

    private Identifier lockIcon() {
        return Identifier.fromNamespaceAndPath(Nexus.MOD_ID,
                "assembler/lock_" + getMenu().lock().getSerializedName());
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (sideButtons().buttonAt(mouseX, mouseY) == 0) {
            final String choice = "gui.nexus.assembler.lock." + getMenu().lock().getSerializedName();
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.nexus.assembler.lock"),
                    Component.translatable(choice).withStyle(ChatFormatting.GRAY),
                    Component.translatable(choice + ".hint").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (minecraft == null) {
            return super.mouseClicked(event, doubleClick);
        }
        if (sideButtons().buttonAt(event.x(), event.y()) == 0 && minecraft.gameMode != null) {
            final boolean backwards = event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId,
                    AssemblerMenu.BUTTON_LOCK + (backwards ? 1 : 0));
            return true;
        }
        return priority.click(minecraft, getMenu().containerId, event.x(), event.y())
                || super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(final double mouseX, final double mouseY, final int left, final int top) {
        return super.hasClickedOutside(mouseX, mouseY, left, top) && !sideButtons().area().contains(mouseX, mouseY);
    }
}
