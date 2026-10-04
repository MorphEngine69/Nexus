package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.security.Permission;
import net.minecraft.world.inventory.DataSlot;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * What the player looking at a menu holds of the {@link Permission}s, one bit
 * each, sent to the client with the menu's other data. On the server it is
 * worked out again once a second, as rights may change while the menu is
 * open; the client only shows it, so until the first answer it shows
 * everything as held.
 */
public final class AccessSync extends DataSlot {

    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final Permission[] PERMISSIONS = Permission.values();
    private static final int EVERYTHING = (1 << PERMISSIONS.length) - 1;

    /** Whether the viewer holds a permission now; {@code null} on the client, which only receives. */
    private final @Nullable Predicate<Permission> holds;
    private int bits = EVERYTHING;
    private int ticks;

    private AccessSync(final @Nullable Predicate<Permission> holds) {
        this.holds = holds;
    }

    /**
     * @param holds whether the viewer holds a permission now
     */
    public static AccessSync onServer(final Predicate<Permission> holds) {
        return new AccessSync(holds);
    }

    public static AccessSync onClient() {
        return new AccessSync(null);
    }

    /**
     * On the server, asked every tick by the menu to see whether to send.
     */
    @Override
    public int get() {
        if (holds != null && ticks++ % REFRESH_INTERVAL_TICKS == 0) {
            int current = 0;
            for (Permission permission : PERMISSIONS) {
                if (holds.test(permission)) {
                    current |= 1 << permission.ordinal();
                }
            }
            bits = current;
        }
        return bits;
    }

    @Override
    public void set(final int value) {
        bits = value;
    }

    /**
     * @return whether the viewer holds {@code permission}, as last worked out or received
     */
    public boolean holds(final Permission permission) {
        return (bits & 1 << permission.ordinal()) != 0;
    }
}
