package com.morphengine.nexus.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.morphengine.nexus.automation.TaskRows;
import com.morphengine.nexus.block.CraftingMonitorBlock;
import com.morphengine.nexus.block.entity.CraftingMonitorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a Crafting Monitor: a row for each of the first tasks, an icon and a bar of segments lit as far as the task has
 * got. The icon and the lit segments are in the color of the network while it has energy; with tasks but no energy the
 * icons are orange and the bars stand dark; a row with no task shows a dark icon, and a monitor with energy and no
 * tasks a ready mark in each row. While a task runs its segments grow into their places one after another from the
 * left, as those of the bar of an Energy Cell do ({@link SegmentSweep}).
 */
public final class CraftingMonitorRenderer extends FacedDeviceRenderer<CraftingMonitorBlockEntity> {

    private static final DataTicket<Integer> ROWS = DataTicket.create("monitor_rows", Integer.class);
    private static final DataTicket<Boolean> ACTIVE = DataTicket.create("monitor_active", Boolean.class);
    private static final DataTicket<Float> TICKS = DataTicket.create("monitor_ticks", Float.class);
    private static final String[] ICON_LIT = rowBones("icon_lit_");
    private static final String[] ICON_DARK = rowBones("icon_dark_");
    private static final String[] ICON_PAUSE = rowBones("icon_pause_");
    private static final String[][] FILL_LIT = segmentBones("fill_lit_");
    private static final String[][] FILL_DARK = segmentBones("fill_dark_");

    public CraftingMonitorRenderer(final BlockEntityRendererProvider.Context context) {
        super(context, new DeviceGeoModel<>("crafting_monitor"), state -> state.getValue(CraftingMonitorBlock.POWERED),
                CraftingMonitorBlock.FACING, List.of(), List.of());
    }

    /**
     * @return the bones an item hides, so that it shows the screen of a monitor on standby: lit icons and one lit
     *         segment in each row, which is what tells a monitor from the other machines in a slot
     */
    static List<String> itemHiddenBones() {
        final List<String> names = new ArrayList<>(List.of(ICON_DARK));
        names.addAll(List.of(ICON_PAUSE));
        for (int row = 0; row < TaskRows.ROWS; row++) {
            names.addAll(List.of(FILL_LIT[row]).subList(1, TaskRows.SEGMENTS));
            names.addAll(List.of(FILL_DARK[row]));
        }
        return List.copyOf(names);
    }

    private static String[] rowBones(final String prefix) {
        final String[] names = new String[TaskRows.ROWS];
        for (int row = 0; row < names.length; row++) {
            names[row] = prefix + row;
        }
        return names;
    }

    private static String[][] segmentBones(final String prefix) {
        final String[][] names = new String[TaskRows.ROWS][TaskRows.SEGMENTS];
        for (int row = 0; row < names.length; row++) {
            for (int segment = 0; segment < TaskRows.SEGMENTS; segment++) {
                names[row][segment] = prefix + row + "_" + segment;
            }
        }
        return names;
    }

    @Override
    public void addRenderData(
            final CraftingMonitorBlockEntity monitor, final @Nullable Void relatedObject,
            final BlockEntityRenderState renderState, final float partialTick) {
        super.addRenderData(monitor, relatedObject, renderState, partialTick);
        renderState.addGeckolibData(ROWS, monitor.rows());
        renderState.addGeckolibData(ACTIVE, monitor.getBlockState().getValue(CraftingMonitorBlock.ACTIVE));
        renderState.addGeckolibData(TICKS, monitor.getLevel() == null
                ? 0F : monitor.getLevel().getGameTime() + partialTick);
    }

    @Override
    protected void adjustDeviceBones(final GeoRenderState renderState, final BoneSnapshots snapshots) {
        final boolean powered = DeviceRenderData.isLit(renderState);
        final boolean active = renderState.getOrDefaultGeckolibData(ACTIVE, false);
        final int rows = renderState.getOrDefaultGeckolibData(ROWS, 0);
        final float ticks = renderState.getOrDefaultGeckolibData(TICKS, 0F);
        for (int row = 0; row < TaskRows.ROWS; row++) {
            final boolean hasTask = active && TaskRows.hasTask(rows, row);
            final boolean ready = powered && !active;
            final boolean litIcon = powered && (ready || hasTask);
            final boolean pausedIcon = !powered && hasTask;
            show(snapshots, ICON_LIT[row], litIcon);
            show(snapshots, ICON_PAUSE[row], pausedIcon);
            show(snapshots, ICON_DARK[row], !litIcon && !pausedIcon);
            final int segments = hasTask ? TaskRows.segmentsOf(rows, row) : 0;
            final boolean running = powered && hasTask;
            for (int segment = 0; segment < TaskRows.SEGMENTS; segment++) {
                final float lit = powered ? litSegment(segment, ready ? 1 : segments, running, ticks) : 0;
                grow(snapshots, FILL_LIT[row][segment], lit);
                grow(snapshots, FILL_DARK[row][segment], !powered && segment < segments ? 1 : 0);
            }
        }
    }

    private static float litSegment(final int segment, final int level, final boolean running, final float ticks) {
        return SegmentSweep.SLOW.fill(segment, level, TaskRows.SEGMENTS, running, ticks);
    }

    private static void grow(final BoneSnapshots snapshots, final String name, final float fill) {
        snapshots.ifPresent(name, bone -> {
            bone.skipRender(fill <= 0);
            bone.setScale(fill, 1, 1);
        });
    }

    private static void show(final BoneSnapshots snapshots, final String name, final boolean visible) {
        snapshots.ifPresent(name, bone -> bone.skipRender(!visible));
    }
}
