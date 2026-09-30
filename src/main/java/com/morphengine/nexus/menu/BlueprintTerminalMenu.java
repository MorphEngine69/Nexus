package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.block.entity.BlueprintEncoder;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.blueprint.BlueprintDraft;
import com.morphengine.nexus.item.BlueprintItem;
import com.morphengine.nexus.networking.BlueprintDraftPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalLayout;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Blueprint Terminal panel: the network's storage, the encoder kept by the
 * terminal, and the player's inventory. The draft is set in ghost slots,
 * whose changes reach the server as payloads; the server sends the draft back,
 * with what its crafting grid crafts, whenever it changes. The kind,
 * substitutes, the tag of a processing input, clearing and encoding go
 * through the vanilla menu button packet. The screen places the slots through
 * {@link #layOut}.
 */
public final class BlueprintTerminalMenu extends AbstractContainerMenu implements TerminalPanel {

    /** Menu button ids. */
    public static final int BUTTON_KIND = 0;
    public static final int BUTTON_CLEAR = 1;
    public static final int BUTTON_ENCODE = 2;
    public static final int BUTTON_SUBSTITUTION = 3;
    /** The next tag of processing input {@code i} is {@code BUTTON_NEXT_TAG + i}, the previous one follows. */
    public static final int BUTTON_NEXT_TAG = 4;
    public static final int BUTTON_PREVIOUS_TAG = BUTTON_NEXT_TAG + BlueprintDraft.INPUTS;

    /** Column of the encoder's area its Blueprint slots stand in, counting from zero. */
    public static final int BLUEPRINT_COLUMN = 8;

    private static final int ENCODER_SLOTS = 2;

    private final TerminalMenuState terminal;
    private final @Nullable BlueprintEncoder encoder;
    private BlueprintDraft draft = BlueprintDraft.EMPTY;
    private List<ResourceAmount> craftingOutputs = List.of();
    private @Nullable BlueprintDraft sentDraft;

    public BlueprintTerminalMenu(
            final int containerId, final Inventory inventory, final BlockPos pos, final TerminalSettings settings) {
        super(NexusMenuTypes.BLUEPRINT_TERMINAL.get(), containerId);
        this.terminal = new TerminalMenuState(inventory, pos, TerminalKind.BLUEPRINT_TERMINAL, settings, containerId);
        final TerminalBlockEntity terminalEntity = terminal.binding().blockEntity();
        this.encoder = inventory.player instanceof ServerPlayer && terminalEntity != null
                ? terminalEntity.encoder() : null;
        final Container blueprints = encoder != null ? encoder.blueprints() : new SimpleContainer(ENCODER_SLOTS);
        addSlot(new EncoderSlot(blueprints, BlueprintEncoder.BLANK_SLOT, encoder));
        addSlot(new EncoderSlot(blueprints, BlueprintEncoder.OUTPUT_SLOT, encoder));
        addStandardInventorySlots(inventory, 0, 0);
    }

    @Override
    public TerminalMenuState terminal() {
        return terminal;
    }

    @Override
    public void layOut(final TerminalLayout layout) {
        final int left = layout.encoderLeft() + BLUEPRINT_COLUMN * TerminalLayout.SLOT;
        slots.get(BlueprintEncoder.BLANK_SLOT).x = left;
        slots.get(BlueprintEncoder.BLANK_SLOT).y = layout.craftingTop();
        slots.get(BlueprintEncoder.OUTPUT_SLOT).x = left;
        slots.get(BlueprintEncoder.OUTPUT_SLOT).y = layout.craftingTop() + 2 * TerminalLayout.SLOT;
        InventorySlots.place(slots, ENCODER_SLOTS, layout.inventoryLeft(), layout.inventoryTop());
    }

    /**
     * @return the draft; on the client, as last received
     */
    public BlueprintDraft draft() {
        return encoder != null ? encoder.draft() : draft;
    }

    /**
     * @return what the draft's crafting grid crafts, as last received; empty on the server
     */
    public List<ResourceAmount> craftingOutputs() {
        return craftingOutputs;
    }

    public void acceptDraft(final BlueprintDraft received, final List<ResourceAmount> outputs) {
        draft = Objects.requireNonNull(received, "received must not be null");
        craftingOutputs = List.copyOf(outputs);
    }

    /**
     * Lists {@code resource} in draft slot {@code slot}, or empties it. Server side only.
     */
    public void setDraftSlot(final int slot, final @Nullable NexusResource resource) {
        if (encoder != null) {
            encoder.changeDraft(encoder.draft().with(slot, resource));
        }
    }

    /**
     * Changes how much draft slot {@code slot} lists. Server side only.
     */
    public void setDraftAmount(final int slot, final long amount) {
        if (encoder != null) {
            encoder.changeDraft(encoder.draft().withAmount(slot, amount));
        }
    }

    /**
     * Makes the draft the recipe a recipe viewer laid out, cut to what a draft
     * of its kind takes. Server side only.
     */
    public void replaceDraft(final BlueprintDraft laidOut) {
        if (encoder != null) {
            encoder.changeDraft(laidOut.withKind(laidOut.kind()));
        }
    }

    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        if (encoder == null || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        final BlueprintDraft current = encoder.draft();
        if (buttonId >= BUTTON_NEXT_TAG && buttonId < BUTTON_PREVIOUS_TAG + BlueprintDraft.INPUTS) {
            final boolean forwards = buttonId < BUTTON_PREVIOUS_TAG;
            final int slot = (buttonId - BUTTON_NEXT_TAG) % BlueprintDraft.INPUTS;
            encoder.changeDraft(current.withNextTag(slot, forwards ? 1 : -1));
            return true;
        }
        switch (buttonId) {
            case BUTTON_KIND -> encoder.changeDraft(current.withKind(current.kind() == BlueprintKind.CRAFTING
                    ? BlueprintKind.PROCESSING : BlueprintKind.CRAFTING));
            case BUTTON_CLEAR -> encoder.changeDraft(current.cleared());
            case BUTTON_SUBSTITUTION -> encoder.changeDraft(current.withSubstitution(current.substitution().toggled()));
            case BUTTON_ENCODE -> encoder.encode(serverPlayer.level());
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        terminal.tick();
        final ServerPlayer viewer = terminal.binding().viewer();
        if (encoder != null && viewer != null && encoder.draft() != sentDraft) {
            sentDraft = encoder.draft();
            PacketDistributor.sendToPlayer(viewer, new BlueprintDraftPayload(containerId, sentDraft,
                    encoder.craftingOutputs(viewer.level())));
        }
    }

    /**
     * Shift-clicked, a blank Blueprint from the inventory goes to the blank
     * slot, anything else into the network; the encoder's slots empty into the
     * inventory.
     */
    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        final Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        if (slotIndex < ENCODER_SLOTS) {
            moveItemStackTo(stack, ENCODER_SLOTS, slots.size(), true);
        } else if (BlueprintItem.isBlank(stack)) {
            moveItemStackTo(stack, BlueprintEncoder.BLANK_SLOT, BlueprintEncoder.BLANK_SLOT + 1, false);
        } else if (player instanceof ServerPlayer serverPlayer) {
            terminal.insert(serverPlayer, this, stack);
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        return terminal.binding().stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        terminal.close();
    }
}
