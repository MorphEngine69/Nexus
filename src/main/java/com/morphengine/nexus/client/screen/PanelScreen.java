package com.morphengine.nexus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.entity.Renamable;
import com.morphengine.nexus.menu.DeviceMenu;
import com.morphengine.nexus.menu.GuardedMenu;
import com.morphengine.nexus.menu.PanelMenu;
import com.morphengine.nexus.menu.RenamablePanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for a Nexus panel, of a network device or of an item in hand: a frame
 * tinted with the network's color, a close cross, and a title the player
 * renames by clicking it, unless {@link #isTitleEditable} says otherwise.
 * Enter or a click elsewhere keeps the new name, Escape drops it. Designed for
 * extension: subclasses pick the style and draw what lies below the header.
 *
 * @param <M> the panel's menu
 */
abstract class PanelScreen<M extends AbstractContainerMenu & PanelMenu> extends AbstractContainerScreen<M> {

    private static final int TITLE_LEFT = 8;
    private static final int TITLE_TOP = 4;
    private static final int TITLE_HEIGHT = 8;
    private static final int CLOSE_AREA = 20;
    private static final int UNDERLINE_GAP = 1;
    private static final int MARKER_SIZE = 12;
    private static final int MARKER_TOP = 2;
    private static final int ALERT_RGB = 0xFFE8605A;
    private static final Identifier LOCK = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "nexus/tab_access");
    private static final List<Permission> PANEL_PERMISSIONS = List.of(Permission.INSERT, Permission.EXTRACT,
            Permission.AUTOCRAFTING, Permission.CONFIGURE);

    private @Nullable CloseButton closeButton;
    private @Nullable EditBox titleEditor;
    private Component shownTitle;

    protected PanelScreen(
            final M menu, final Inventory inventory, final Component title, final int width, final int height) {
        super(menu, inventory, title, width, height);
        this.shownTitle = title;
    }

    @Override
    protected void init() {
        super.init();
        closeButton = CloseButton.inHeaderOf(panelBounds());
        titleEditor = null;
    }

    protected final PanelBounds panelBounds() {
        return new PanelBounds(leftPos, topPos, imageWidth, imageHeight);
    }

    /**
     * @return the name shown in the header; the device's name unless overridden
     */
    protected Component panelTitle() {
        return shownTitle;
    }

    protected abstract PanelStyle style();

    /**
     * @return whether a click on the title renames what the panel shows; by
     *         default whenever the menu is a {@link RenamablePanel}
     */
    protected boolean isTitleEditable() {
        return getMenu() instanceof RenamablePanel;
    }

    /**
     * Draws everything below the header.
     */
    protected abstract void extractPanel(GuiGraphicsExtractor graphics, PanelStyle style, int mouseX, int mouseY);

    @Override
    public final void extractBackground(
            final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        final PanelStyle style = style();
        style.drawFrame(graphics, font, panelBounds(), titleEditor != null ? Component.empty() : panelTitle());
        if (closeButton != null) {
            closeButton.draw(graphics);
        }
        final PanelBounds title = titleBounds();
        if (titleEditor != null || isTitleEditable() && title.contains(mouseX, mouseY)) {
            final int underline = title.top() + title.height() + UNDERLINE_GAP;
            graphics.fill(title.left(), underline, title.left() + title.width(), underline + 1, style.border());
        }
        drawMarkers(graphics);
        extractPanel(graphics, style, mouseX, mouseY);
    }

    /**
     * In the header, left of the close cross: a lock when the viewer may not do
     * everything here, a red mark when the device stands still because its
     * owner lacks a permission.
     */
    private void drawMarkers(final GuiGraphicsExtractor graphics) {
        if (!lacking().isEmpty()) {
            final PanelBounds lock = lockBounds();
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCK, lock.left(), lock.top(), lock.width(),
                    lock.height());
        }
        if (halted() != null) {
            final PanelBounds alert = alertBounds();
            graphics.text(font, Component.literal("!"), alert.left() + (alert.width() - font.width("!")) / 2,
                    alert.top() + 2, ALERT_RGB, false);
        }
    }

    private PanelBounds lockBounds() {
        return new PanelBounds(leftPos + imageWidth - CLOSE_AREA - MARKER_SIZE, topPos + MARKER_TOP, MARKER_SIZE,
                MARKER_SIZE);
    }

    private PanelBounds alertBounds() {
        return new PanelBounds(lockBounds().left() - MARKER_SIZE, topPos + MARKER_TOP, MARKER_SIZE, MARKER_SIZE);
    }

    /**
     * @return what the viewer may not do in the panel, as the server last said
     */
    private List<Permission> lacking() {
        if (!(getMenu() instanceof GuardedMenu guarded)) {
            return List.of();
        }
        final List<Permission> lacking = new ArrayList<>();
        for (Permission permission : PANEL_PERMISSIONS) {
            if (!guarded.viewerAccess().holds(permission)) {
                lacking.add(permission);
            }
        }
        return lacking;
    }

    private @Nullable Permission halted() {
        return getMenu() instanceof DeviceMenu<?> device ? device.haltedFor() : null;
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        final List<Permission> lacking = lacking();
        final Permission halted = halted();
        if (!lacking.isEmpty() && lockBounds().contains(mouseX, mouseY)) {
            final List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.nexus.access.restricted"));
            for (Permission permission : lacking) {
                lines.add(Component.literal("- ").append(NetworkAccess.nameOf(permission)));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        } else if (halted != null && alertBounds().contains(mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.nexus.access.halted",
                    NetworkAccess.nameOf(halted))), mouseX, mouseY);
        }
    }

    @Override
    protected final void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
    }

    private PanelBounds titleBounds() {
        return new PanelBounds(leftPos + TITLE_LEFT, topPos + TITLE_TOP,
                imageWidth - TITLE_LEFT - CLOSE_AREA, TITLE_HEIGHT);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (closeButton != null && closeButton.contains(event.x(), event.y())) {
            onClose();
            return true;
        }
        final boolean onTitle = isTitleEditable() && titleBounds().contains(event.x(), event.y());
        if (titleEditor != null && !onTitle) {
            commitTitle();
        } else if (titleEditor == null && onTitle) {
            startEditingTitle();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final EditBox editor = titleEditor;
        if (editor == null) {
            return super.keyPressed(event);
        }
        switch (event.key()) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> commitTitle();
            case InputConstants.KEY_ESCAPE -> stopEditingTitle();
            default -> editor.keyPressed(event);
        }
        return true;
    }

    @Override
    public void removed() {
        if (titleEditor != null) {
            commitTitle();
        }
        super.removed();
    }

    private void startEditingTitle() {
        final PanelBounds bounds = titleBounds();
        final EditBox editor = new EditBox(
                font, bounds.left(), bounds.top(), bounds.width(), bounds.height(), panelTitle());
        editor.setBordered(false);
        editor.setMaxLength(Renamable.MAX_NAME_LENGTH);
        editor.setTextColor(PanelStyle.TEXT_LIGHT);
        editor.setValue(panelTitle().getString());
        titleEditor = addRenderableWidget(editor);
        setFocused(editor);
    }

    private void commitTitle() {
        final EditBox editor = titleEditor;
        if (editor == null) {
            return;
        }
        final String name = editor.getValue().strip();
        stopEditingTitle();
        if (name.equals(panelTitle().getString())) {
            return;
        }
        if (getMenu() instanceof RenamablePanel renamable) {
            ClientPacketDistributor.sendToServer(renamable.renamePayload(name));
        }
        shownTitle = name.isEmpty() ? getMenu().defaultTitle() : Component.literal(name);
    }

    private void stopEditingTitle() {
        if (titleEditor != null) {
            removeWidget(titleEditor);
            titleEditor = null;
        }
    }
}
