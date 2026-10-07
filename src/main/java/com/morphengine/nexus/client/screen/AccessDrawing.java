package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.PermissionState;
import com.morphengine.nexus.api.network.security.Role;
import com.morphengine.nexus.security.Member;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * How roles and permissions look and read in the access tab.
 */
final class AccessDrawing {

    private static final int ALLOW_RGB = 0xFF6EBA68;
    private static final int DENY_RGB = 0xFFCE5046;
    private static final int OWNER_RGB = 0xFFD9B45A;
    private static final int ADMIN_RGB = 0xFF7FB77E;
    private static final int USER_RGB = 0xFF7FA6D9;
    private static final int BLOCKED_RGB = 0xFFC86464;

    private AccessDrawing() {
    }

    static int colorOf(final Role role) {
        return switch (role) {
            case OWNER -> OWNER_RGB;
            case ADMIN -> ADMIN_RGB;
            case USER -> USER_RGB;
            case GUEST -> PanelStyle.TEXT_DIM;
            case BLOCKED -> BLOCKED_RGB;
        };
    }

    /**
     * Inside the box of a permission: a green square when allowed apart from
     * the role, a red cross when denied, and as the role says a dim dot when
     * the role grants it, nothing when not.
     */
    static void drawState(final GuiGraphicsExtractor graphics, final PanelBounds box, final Member member,
                          final Permission permission) {
        switch (member.stateOf(permission)) {
            case ALLOW -> graphics.fill(box.left() + 2, box.top() + 2, box.left() + box.width() - 2,
                    box.top() + box.height() - 2, ALLOW_RGB);
            case DENY -> PanelStyle.drawCross(graphics, box, DENY_RGB);
            case INHERIT -> drawInherited(graphics, box, member.grants(permission));
        }
    }

    private static void drawInherited(final GuiGraphicsExtractor graphics, final PanelBounds box,
                                      final boolean granted) {
        if (granted) {
            final int middle = box.width() / 2;
            graphics.fill(box.left() + middle - 1, box.top() + middle - 1, box.left() + middle + 2,
                    box.top() + middle + 2, PanelStyle.TEXT_DIM);
        }
    }

    static List<Component> toggleTooltip(final Member member, final Permission permission) {
        final PermissionState state = member.stateOf(permission);
        final Component standing = state == PermissionState.INHERIT
                ? Component.translatable("gui.nexus.access.state.inherit", Component.translatable(
                        member.grants(permission) ? "gui.nexus.access.state.held" : "gui.nexus.access.state.not_held"))
                : Component.translatable("gui.nexus.access.state." + state.name().toLowerCase(Locale.ROOT));
        return List.of(NetworkAccess.nameOf(permission), description(permission), standing,
                Component.translatable("gui.nexus.access.state.hint"));
    }

    static Component description(final Permission permission) {
        return Component.translatable("gui.nexus.access.permission." + permission.name().toLowerCase(Locale.ROOT)
                + ".description");
    }

    static Component description(final Role role) {
        return Component.translatable("gui.nexus.access.role." + role.name().toLowerCase(Locale.ROOT)
                + ".description");
    }

    static PermissionState next(final PermissionState state) {
        return switch (state) {
            case INHERIT -> PermissionState.ALLOW;
            case ALLOW -> PermissionState.DENY;
            case DENY -> PermissionState.INHERIT;
        };
    }
}
