package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import com.morphengine.nexus.resource.ItemKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The buttons of a Blueprint encoder: the kind and substitutes buttons down
 * its first column, the clear button right of the first row of its grid, and
 * the encode button between its Blueprint slots.
 * A click goes to the server as a menu button.
 */
final class BlueprintEncoderButtons {

    private static final int ICON_SIZE = 16;
    private static final int NONE = -1;
    private static final Identifier ENCODE_ICON = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "blueprint/encode");
    private static final ItemKey CRAFTING_ICON = ItemKey.of(new ItemStack(Items.CRAFTING_TABLE));
    private static final ItemKey PROCESSING_ICON = ItemKey.of(new ItemStack(Items.FURNACE));

    private final BlueprintTerminalMenu menu;
    private final PanelBounds kind;
    private final PanelBounds substitution;
    private final PanelBounds clear;
    private final PanelBounds encode;

    BlueprintEncoderButtons(final BlueprintTerminalMenu menu, final PanelBounds kind, final PanelBounds substitution,
                            final PanelBounds clear, final PanelBounds encode) {
        this.menu = menu;
        this.kind = kind;
        this.substitution = substitution;
        this.clear = clear;
        this.encode = encode;
    }

    private boolean isCrafting() {
        return menu.draft().kind() == BlueprintKind.CRAFTING;
    }

    void draw(final GuiGraphicsExtractor graphics, final PanelStyle style, final int mouseX, final int mouseY) {
        drawButton(graphics, style, kind, mouseX, mouseY);
        ResourceRenderers.icon(isCrafting() ? CRAFTING_ICON : PROCESSING_ICON)
                .draw(graphics, kind.left() + 1, kind.top() + 1);
        drawButton(graphics, style, substitution, mouseX, mouseY);
        drawIcon(graphics, substitution, Identifier.fromNamespaceAndPath(Nexus.MOD_ID,
                "blueprint/substitutes_" + menu.draft().substitution().getSerializedName()));
        style.drawCrossButton(graphics, clear, clear.contains(mouseX, mouseY));
        drawButton(graphics, style, encode, mouseX, mouseY);
        drawIcon(graphics, encode, ENCODE_ICON);
    }

    private static void drawButton(final GuiGraphicsExtractor graphics, final PanelStyle style,
                                   final PanelBounds bounds, final int mouseX, final int mouseY) {
        graphics.fill(bounds.left(), bounds.top(), bounds.left() + bounds.width(), bounds.top() + bounds.height(),
                style.buttonFill());
        graphics.outline(bounds.left(), bounds.top(), bounds.width(), bounds.height(),
                bounds.contains(mouseX, mouseY) ? PanelStyle.TEXT_LIGHT : style.border());
    }

    private static void drawIcon(final GuiGraphicsExtractor graphics, final PanelBounds bounds,
                                 final Identifier icon) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, bounds.left() + 1, bounds.top() + 1, ICON_SIZE,
                ICON_SIZE);
    }

    /**
     * @return the tooltip of the button under the cursor; empty when there is none
     */
    List<Component> tooltip(final double mouseX, final double mouseY) {
        return switch (buttonAt(mouseX, mouseY)) {
            case NONE -> List.of();
            case BlueprintTerminalMenu.BUTTON_KIND -> List.of(Component.translatable("gui.nexus.blueprint.kind"),
                    Component.translatable("gui.nexus.blueprint.kind." + kindName()).withStyle(ChatFormatting.GRAY));
            case BlueprintTerminalMenu.BUTTON_SUBSTITUTION -> List.of(
                    Component.translatable("gui.nexus.blueprint.substitution"),
                    Component.translatable("gui.nexus.blueprint.substitution."
                            + menu.draft().substitution().getSerializedName()).withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.nexus.blueprint.substitution.hint." + kindName())
                            .withStyle(ChatFormatting.DARK_GRAY));
            case BlueprintTerminalMenu.BUTTON_CLEAR -> List.of(Component.translatable("gui.nexus.blueprint.clear"));
            default -> List.of(Component.translatable("gui.nexus.blueprint.encode"),
                    Component.translatable("gui.nexus.blueprint.encode.hint").withStyle(ChatFormatting.GRAY));
        };
    }

    private String kindName() {
        return isCrafting() ? "crafting" : "processing";
    }

    /**
     * @return whether a button was under the cursor and its click was sent
     */
    boolean click(final double x, final double y) {
        final int button = buttonAt(x, y);
        if (button != NONE) {
            press(button);
        }
        return button != NONE;
    }

    /**
     * Sends a press of menu button {@code button} of the Blueprint Terminal.
     */
    void press(final int button) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    /**
     * @return the menu button id of the button under the cursor; {@link #NONE} when there is none
     */
    private int buttonAt(final double x, final double y) {
        if (kind.contains(x, y)) {
            return BlueprintTerminalMenu.BUTTON_KIND;
        }
        if (substitution.contains(x, y)) {
            return BlueprintTerminalMenu.BUTTON_SUBSTITUTION;
        }
        if (clear.contains(x, y)) {
            return BlueprintTerminalMenu.BUTTON_CLEAR;
        }
        return encode.contains(x, y) ? BlueprintTerminalMenu.BUTTON_ENCODE : NONE;
    }
}
