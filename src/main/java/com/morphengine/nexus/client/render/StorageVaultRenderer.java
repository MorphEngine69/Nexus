package com.morphengine.nexus.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.VaultLamp;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a Storage Vault: every drawer has a meter of three steps, lit as far as its cell is full, and while the cell
 * works its top lit step slowly swells and settles. A drawer with no cell, or a vault without energy, shows no step.
 * The swell is one beat for every cell, so a cell that starts working never starts it in the middle.
 */
public final class StorageVaultRenderer extends FacedDeviceRenderer<StorageVaultBlockEntity> {

    private static final DataTicket<Long> METERS = DataTicket.create("vault_meters", Long.class);
    private static final DataTicket<Integer> BUSY = DataTicket.create("vault_busy", Integer.class);
    private static final DataTicket<Float> TICKS = DataTicket.create("vault_ticks", Float.class);
    private static final String[] STEP_BONES = {"lamp_green_", "lamp_orange_", "lamp_red_"};
    private static final String[][] BONE_NAMES = boneNames();
    private static final float SWELL = 0.35F;
    private static final float HALF = 0.5F;

    public StorageVaultRenderer(final BlockEntityRendererProvider.Context context) {
        super(context, new DeviceGeoModel<>("storage_vault"), state -> true, StorageVaultBlock.FACING,
                List.of(), List.of());
    }

    /**
     * @return the meter bones an item hides: of every drawer the orange and the red step, so that the item shows a
     *         green lamp on each drawer, which is what tells a vault from the other machines in a slot
     */
    static List<String> itemHiddenBones() {
        final List<String> names = new ArrayList<>();
        for (String[] steps : BONE_NAMES) {
            names.addAll(List.of(steps).subList(1, steps.length));
        }
        return List.copyOf(names);
    }

    private static String[][] boneNames() {
        final String[][] names = new String[StorageVaultBlockEntity.SLOTS][STEP_BONES.length];
        for (int slot = 0; slot < names.length; slot++) {
            for (int step = 0; step < STEP_BONES.length; step++) {
                names[slot][step] = STEP_BONES[step] + slot;
            }
        }
        return names;
    }

    @Override
    public void addRenderData(
            final StorageVaultBlockEntity vault, final @Nullable Void relatedObject,
            final BlockEntityRenderState renderState, final float partialTick) {
        super.addRenderData(vault, relatedObject, renderState, partialTick);
        long meters = 0;
        int busy = 0;
        for (int slot = 0; slot < StorageVaultBlockEntity.SLOTS; slot++) {
            meters = vault.lampAt(slot).packInto(meters, slot);
            busy |= vault.isBusyAt(slot) ? 1 << slot : 0;
        }
        renderState.addGeckolibData(METERS, meters);
        renderState.addGeckolibData(BUSY, busy);
        renderState.addGeckolibData(TICKS, vault.getLevel() == null
                ? 0F : vault.getLevel().getGameTime() + partialTick);
    }

    @Override
    protected void adjustDeviceBones(final GeoRenderState renderState, final BoneSnapshots snapshots) {
        final long meters = renderState.getOrDefaultGeckolibData(METERS, 0L);
        final int busy = renderState.getOrDefaultGeckolibData(BUSY, 0);
        final float swell = swellAt(renderState.getOrDefaultGeckolibData(TICKS, 0F));
        for (int slot = 0; slot < StorageVaultBlockEntity.SLOTS; slot++) {
            final int lit = VaultLamp.unpack(meters, slot).ordinal();
            final boolean working = (busy & 1 << slot) != 0;
            for (int step = 0; step < STEP_BONES.length; step++) {
                final float scale = working && step == lit - 1 ? swell : 1;
                final boolean hidden = step >= lit;
                snapshots.ifPresent(BONE_NAMES[slot][step], bone -> {
                    bone.skipRender(hidden);
                    bone.setScale(scale, scale, scale);
                });
            }
        }
    }

    private static float swellAt(final float ticks) {
        final float phase = ticks % StorageVaultBlockEntity.BUSY_PERIOD_TICKS
                / StorageVaultBlockEntity.BUSY_PERIOD_TICKS;
        return 1 + SWELL * (HALF - HALF * (float) Math.cos(2 * Math.PI * phase));
    }
}
