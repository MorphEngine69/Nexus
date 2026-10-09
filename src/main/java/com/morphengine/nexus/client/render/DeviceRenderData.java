package com.morphengine.nexus.client.render;

import com.morphengine.nexus.block.SideConnections;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * What a device renderer works out from the block state when it draws a frame: the sides with a port, and which bones
 * to show for them. An item has no block state and shows no cable.
 */
final class DeviceRenderData {

    private static final String PORT_BONE_PREFIX = "port_";
    private static final int SIDES = Direction.values().length;
    /** For a facing and a side of the world, the side of the model that the facing turns onto it. */
    private static final Direction[][] MODEL_SIDE_OF = modelSides();
    private static final List<String> ITEM_BONES = List.of("item_core", "item_arm");

    private DeviceRenderData() {
    }

    /**
     * @return the sides of the block with a port, one bit for each by {@link Direction#ordinal()}
     */
    static int portsOf(final BlockState state) {
        return SideConnections.hasSides(state) ? SideConnections.attachedMask(state) : 0;
    }

    /**
     * Turns the sides with a port, as the world sees them, to the sides of a model that GeckoLib then turns to
     * {@code facing}, since the port bones are named by the side of the model they stand on.
     *
     * @param ports one bit per side of the world, by {@link Direction#ordinal()}
     * @return the same, by side of the model
     */
    static int portsInModel(final int ports, final Direction facing) {
        int inModel = 0;
        for (Direction side : Direction.values()) {
            if ((ports & 1 << side.ordinal()) != 0) {
                inModel |= 1 << MODEL_SIDE_OF[facing.ordinal()][side.ordinal()].ordinal();
            }
        }
        return inModel;
    }

    private static Direction[][] modelSides() {
        final Direction[][] table = new Direction[SIDES][SIDES];
        for (Direction facing : Direction.values()) {
            final Quaternionf undo = turnOf(facing).invert();
            for (Direction side : Direction.values()) {
                final Vector3f normal = new Vector3f(side.getStepX(), side.getStepY(), side.getStepZ());
                undo.transform(normal);
                table[facing.ordinal()][side.ordinal()] =
                        Direction.getNearest(normal.x(), normal.y(), normal.z());
            }
        }
        return table;
    }

    /**
     * @return the turn GeckoLib gives a model that faces {@code facing}, as a block renderer does it
     */
    private static Quaternionf turnOf(final Direction facing) {
        return switch (facing) {
            case SOUTH -> new Quaternionf().rotationY((float) Math.PI);
            case WEST -> new Quaternionf().rotationY((float) Math.PI / 2);
            case EAST -> new Quaternionf().rotationY((float) -Math.PI / 2);
            case UP -> new Quaternionf().rotationX((float) Math.PI / 2);
            case DOWN -> new Quaternionf().rotationX((float) -Math.PI / 2);
            default -> new Quaternionf();
        };
    }

    /**
     * Shows the port bone of every side set in {@code ports}, one bit per side
     * by {@link Direction#ordinal()}, and hides the rest. A model without the
     * bone of a side simply has no port there.
     */
    static void showPorts(final Bones bones, final int ports) {
        for (Direction side : Direction.values()) {
            final boolean attached = (ports & 1 << side.ordinal()) != 0;
            bones.hide(PORT_BONE_PREFIX + side.getName(), !attached);
        }
    }

    /**
     * Hides, on every side with a port, the bones {@code <prefix>_<side>}: what the port stands in place of.
     */
    static void hideUnderPorts(final Bones bones, final int ports, final List<String> prefixes) {
        for (String prefix : prefixes) {
            for (Direction side : Direction.values()) {
                final boolean covered = (ports & 1 << side.ordinal()) != 0;
                bones.hide(prefix + "_" + side.getName(), covered);
            }
        }
    }

    /**
     * Hides the bones in {@code names} unless the device is lit.
     */
    static void hideUnlessLit(final Bones bones, final boolean lit, final List<String> names) {
        for (String name : names) {
            bones.hide(name, !lit);
        }
    }

    /**
     * Hides the bones that only the item shows, such as the coupling and arm
     * that a head has beside it in the world.
     */
    static void hideItemBones(final Bones bones) {
        setItemBonesHidden(bones, true);
    }

    static void showItemBones(final Bones bones) {
        setItemBonesHidden(bones, false);
    }

    private static void setItemBonesHidden(final Bones bones, final boolean hidden) {
        for (String name : ITEM_BONES) {
            bones.hide(name, hidden);
        }
    }
}
