package com.morphengine.nexus.level;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The access points of one network, and whether a Nexus Terminal at some place
 * is within reach of any of them: in the same dimension and no farther than
 * its range. Server thread only.
 */
public final class WirelessAccessComponent implements NetworkComponent {

    private final List<AccessPoint> points = new ArrayList<>();

    @Override
    public void adopt(final List<NetworkMember> members) {
        points.clear();
        for (NetworkMember member : members) {
            if (member instanceof AccessPoint point) {
                points.add(point);
            }
        }
    }

    /**
     * @param at where the terminal is in {@code dimension}
     * @return whether an access point of the network standing now reaches there
     */
    public boolean reaches(final ResourceKey<Level> dimension, final Vec3 at) {
        for (AccessPoint point : points) {
            final GlobalPos position = point.position();
            if (!point.isRemoved() && position.dimension().equals(dimension)
                    && Vec3.atCenterOf(position.pos()).closerThan(at, point.range())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAccessPoints() {
        return !points.isEmpty();
    }
}
