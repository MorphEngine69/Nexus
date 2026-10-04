package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.Role;
import com.morphengine.nexus.menu.AccessView;
import com.morphengine.nexus.menu.NexusMenu;
import com.morphengine.nexus.networking.NetworkAccessEditPayload;
import com.morphengine.nexus.security.Editor;
import com.morphengine.nexus.security.Member;
import com.morphengine.nexus.security.SecurityEdit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The access tab of the Nexus panel: the owner, the role of everyone else, the
 * members with their roles and adjusted permissions, and the players online
 * to add. It offers only what the viewer may change, by asking the rules the
 * server sent; the server decides again when the change arrives. Handing the
 * network on takes a second click.
 */
final class AccessPanel {

    private static final List<Role> MEMBER_ROLES = List.of(Role.ADMIN, Role.USER, Role.GUEST, Role.BLOCKED);
    private static final List<Role> DEFAULT_ROLES = List.of(Role.USER, Role.GUEST, Role.BLOCKED);
    private static final List<Permission> ADJUSTABLE = List.of(Permission.OPEN, Permission.INSERT,
            Permission.EXTRACT, Permission.AUTOCRAFTING, Permission.CONFIGURE, Permission.BUILD);
    private static final int ROWS = AccessLayout.ROWS;
    private static final long CONFIRM_MILLIS = 3000;
    private static final int GAP = 6;
    /** How far text sits below the top of a button or a list. */
    private static final int TEXT_INSET = 3;
    private static final int LABEL_GAP = 4;
    private static final int SELECTED_FILL = 0x40FFFFFF;

    private final NexusMenu menu;
    private final Font font;
    private final UUID viewer;
    private @Nullable AccessChoices choices;
    private @Nullable UUID selected;
    private boolean adding;
    private int scroll;
    private long transferArmedUntil;

    AccessPanel(final NexusMenu menu, final Font font, final UUID viewer) {
        this.menu = menu;
        this.font = font;
        this.viewer = viewer;
    }

    void draw(final GuiGraphicsExtractor graphics, final PanelStyle style, final AccessLayout layout) {
        final AccessChoices current = choices();
        if (current == null) {
            graphics.text(font, Component.translatable("gui.nexus.access.loading"), layout.left(), layout.ownerY(),
                    PanelStyle.TEXT_DIM, false);
            return;
        }
        drawSummary(graphics, style, layout, current);
        drawList(graphics, style, layout, current);
        final Member member = selectedMember(current);
        if (member != null && !adding) {
            drawDetails(graphics, style, layout, current, member);
            drawToggles(graphics, style, layout, current, member);
        } else if (!adding) {
            graphics.textWithWordWrap(font, Component.translatable("gui.nexus.access.select"), layout.left(),
                    layout.detailY(), layout.width(), PanelStyle.TEXT_DIM);
        }
    }

    boolean click(final AccessLayout layout, final double x, final double y) {
        final AccessChoices current = choices();
        if (current == null) {
            return false;
        }
        if (clickSummary(layout, current, x, y) || clickList(layout, current, x, y)) {
            return true;
        }
        final Member member = selectedMember(current);
        return member != null && !adding && clickDetails(layout, current, member, x, y);
    }

    boolean scroll(final AccessLayout layout, final double x, final double y, final double amount) {
        final AccessChoices current = choices();
        if (current == null || !layout.list().contains(x, y)) {
            return false;
        }
        final int rows = rows(current).size();
        scroll = Math.max(0, Math.min(rows - ROWS, scroll - (int) Math.signum(amount)));
        return true;
    }

    List<Component> tooltip(final AccessLayout layout, final double x, final double y) {
        final AccessChoices current = choices();
        if (current == null) {
            return List.of();
        }
        if (layout.defaultRole().contains(x, y)) {
            return List.of(AccessDrawing.description(current.view().defaultRole()));
        }
        final Member member = selectedMember(current);
        return member != null && !adding ? memberTooltip(layout, member, x, y) : List.of();
    }

    private static List<Component> memberTooltip(final AccessLayout layout, final Member member, final double x,
                                                 final double y) {
        if (layout.roleLabel().contains(x, y)) {
            return List.of(AccessDrawing.description(member.role()));
        }
        for (int index = 0; index < ADJUSTABLE.size(); index++) {
            if (layout.toggle(index).contains(x, y)) {
                return AccessDrawing.toggleTooltip(member, ADJUSTABLE.get(index));
            }
        }
        return List.of();
    }

    /**
     * @return what the viewer may change in the view last received; {@code null} before one arrived
     */
    private @Nullable AccessChoices choices() {
        final AccessView view = menu.access();
        if (view == null) {
            return null;
        }
        if (choices == null || choices.view() != view) {
            choices = new AccessChoices(view, viewer);
        }
        return choices;
    }

    private void drawSummary(final GuiGraphicsExtractor graphics, final PanelStyle style, final AccessLayout layout,
                             final AccessChoices current) {
        final Member owner = current.rules().owner().flatMap(current.rules()::member).orElse(null);
        final Component ownerLine = owner != null ? Component.translatable("gui.nexus.access.owner", owner.name())
                : Component.translatable("gui.nexus.access.unclaimed");
        final Component standing = current.view().standing() == Editor.Standing.OPERATOR
                ? Component.translatable("gui.nexus.access.operator")
                : NetworkAccess.nameOf(current.rules().roleOf(viewer));
        final Component you = Component.translatable("gui.nexus.access.you", standing);
        graphics.text(font, you, layout.right() - font.width(you), layout.ownerY(), PanelStyle.TEXT_DIM, false);
        graphics.text(font, font.plainSubstrByWidth(ownerLine.getString(), layout.width() - font.width(you) - GAP),
                layout.left(), layout.ownerY(), PanelStyle.TEXT_LIGHT, false);
        graphics.text(font, Component.translatable("gui.nexus.access.default"), layout.left(),
                layout.defaultRole().top() + TEXT_INSET, PanelStyle.TEXT_DIM, false);
        button(graphics, style, layout.defaultRole(), NetworkAccess.nameOf(current.view().defaultRole()),
                nextDefault(current) != null);
        final Component heading = adding ? Component.translatable("gui.nexus.access.candidates")
                : Component.translatable("gui.nexus.access.members", current.view().members().size());
        graphics.text(font, heading, layout.left(), layout.header().top() + TEXT_INSET, PanelStyle.TEXT_DIM, false);
        final HeaderAction action = headerAction(current);
        button(graphics, style, layout.header(), action.label(), action.isEnabled(current));
    }

    private void drawList(final GuiGraphicsExtractor graphics, final PanelStyle style, final AccessLayout layout,
                          final AccessChoices current) {
        final PanelBounds list = layout.list();
        graphics.fill(list.left(), list.top(), list.left() + list.width(), list.top() + list.height(), style.track());
        graphics.outline(list.left(), list.top(), list.width(), list.height(), style.border());
        final List<Row> rows = rows(current);
        if (rows.isEmpty()) {
            graphics.text(font, Component.translatable(adding ? "gui.nexus.access.no_candidates"
                    : "gui.nexus.access.no_members"), list.left() + TEXT_INSET, list.top() + TEXT_INSET,
                    PanelStyle.TEXT_DIM, false);
            return;
        }
        for (int index = 0; index < Math.min(ROWS, rows.size() - scroll); index++) {
            final Row row = rows.get(scroll + index);
            final PanelBounds bounds = layout.row(index);
            if (row.id().equals(selected) && !adding) {
                graphics.fill(bounds.left(), bounds.top(), bounds.left() + bounds.width(),
                        bounds.top() + bounds.height(), SELECTED_FILL);
            }
            final String name = row.id().equals(viewer) ? row.name() + " *" : row.name();
            graphics.text(font, font.plainSubstrByWidth(name, bounds.width() / 2), bounds.left() + 2,
                    bounds.top() + 2, PanelStyle.TEXT_LIGHT, false);
            if (row.role() != null) {
                final Component role = NetworkAccess.nameOf(row.role());
                graphics.text(font, role, bounds.left() + bounds.width() - font.width(role) - 2, bounds.top() + 2,
                        AccessDrawing.colorOf(row.role()), false);
            }
        }
    }

    private void drawDetails(final GuiGraphicsExtractor graphics, final PanelStyle style, final AccessLayout layout,
                             final AccessChoices current, final Member member) {
        graphics.text(font, font.plainSubstrByWidth(member.name(), layout.width()), layout.left(), layout.detailY(),
                PanelStyle.TEXT_LIGHT, false);
        button(graphics, style, layout.rolePrevious(), Component.literal("<"),
                stepRole(current, member, -1) != null);
        final PanelBounds label = layout.roleLabel();
        final Component role = NetworkAccess.nameOf(member.role());
        graphics.text(font, role, label.left() + (label.width() - font.width(role)) / 2, label.top() + TEXT_INSET,
                AccessDrawing.colorOf(member.role()), false);
        button(graphics, style, layout.roleNext(), Component.literal(">"), stepRole(current, member, 1) != null);
        button(graphics, style, layout.remove(), Component.translatable("gui.nexus.access.remove"),
                current.allows(new SecurityEdit.RemoveMember(member.id())));
        final boolean armed = member.id().equals(selected) && Util.getMillis() < transferArmedUntil;
        button(graphics, style, layout.transfer(), Component.translatable(armed ? "gui.nexus.access.transfer.confirm"
                : "gui.nexus.access.transfer"), current.allows(new SecurityEdit.TransferOwnership(member.id())));
    }

    private void drawToggles(final GuiGraphicsExtractor graphics, final PanelStyle style, final AccessLayout layout,
                             final AccessChoices current, final Member member) {
        for (int index = 0; index < ADJUSTABLE.size(); index++) {
            final Permission permission = ADJUSTABLE.get(index);
            final PanelBounds toggle = layout.toggle(index);
            final PanelBounds box = new PanelBounds(toggle.left(), toggle.top() + 2, AccessLayout.BOX,
                    AccessLayout.BOX);
            final boolean enabled = current.allows(new SecurityEdit.Adjust(member.id(), permission,
                    AccessDrawing.next(member.stateOf(permission))));
            graphics.fill(box.left(), box.top(), box.left() + box.width(), box.top() + box.height(), style.track());
            graphics.outline(box.left(), box.top(), box.width(), box.height(),
                    enabled ? style.border() : style.track());
            AccessDrawing.drawState(graphics, box, member, permission);
            graphics.text(font, NetworkAccess.nameOf(permission), box.left() + box.width() + LABEL_GAP,
                    toggle.top() + TEXT_INSET,
                    member.grants(permission) ? PanelStyle.TEXT_LIGHT : PanelStyle.TEXT_DIM, false);
        }
    }

    private boolean clickSummary(final AccessLayout layout, final AccessChoices current, final double x,
                                 final double y) {
        if (layout.defaultRole().contains(x, y)) {
            send(nextDefault(current));
            return true;
        }
        if (!layout.header().contains(x, y)) {
            return false;
        }
        final HeaderAction action = headerAction(current);
        if (action == HeaderAction.CLAIM && action.isEnabled(current)) {
            send(new SecurityEdit.Claim(""));
        } else if (action == HeaderAction.ADD || action == HeaderAction.BACK) {
            adding = action == HeaderAction.ADD;
            scroll = 0;
        }
        return true;
    }

    private boolean clickList(final AccessLayout layout, final AccessChoices current, final double x,
                              final double y) {
        final List<Row> rows = rows(current);
        for (int index = 0; index < Math.min(ROWS, rows.size() - scroll); index++) {
            if (!layout.row(index).contains(x, y)) {
                continue;
            }
            final Row row = rows.get(scroll + index);
            if (adding) {
                send(new SecurityEdit.AddMember(row.id(), row.name()));
                adding = false;
                scroll = 0;
            }
            selected = row.id();
            transferArmedUntil = 0;
            return true;
        }
        return false;
    }

    private boolean clickDetails(final AccessLayout layout, final AccessChoices current, final Member member,
                                 final double x, final double y) {
        if (layout.rolePrevious().contains(x, y) || layout.roleNext().contains(x, y)) {
            send(stepRole(current, member, layout.roleNext().contains(x, y) ? 1 : -1));
            return true;
        }
        if (layout.remove().contains(x, y)) {
            send(allowed(current, new SecurityEdit.RemoveMember(member.id())));
            return true;
        }
        if (layout.transfer().contains(x, y)) {
            confirmTransfer(current, member);
            return true;
        }
        return clickToggles(layout, current, member, x, y);
    }

    private boolean clickToggles(final AccessLayout layout, final AccessChoices current, final Member member,
                                 final double x, final double y) {
        for (int index = 0; index < ADJUSTABLE.size(); index++) {
            if (layout.toggle(index).contains(x, y)) {
                final Permission permission = ADJUSTABLE.get(index);
                send(allowed(current, new SecurityEdit.Adjust(member.id(), permission,
                        AccessDrawing.next(member.stateOf(permission)))));
                return true;
            }
        }
        return false;
    }

    private void confirmTransfer(final AccessChoices current, final Member member) {
        final SecurityEdit transfer = allowed(current, new SecurityEdit.TransferOwnership(member.id()));
        if (transfer == null) {
            return;
        }
        if (Util.getMillis() < transferArmedUntil) {
            transferArmedUntil = 0;
            send(transfer);
        } else {
            transferArmedUntil = Util.getMillis() + CONFIRM_MILLIS;
        }
    }

    /**
     * @return the members, highest role first and by name, or while adding,
     *         the players online who are not members
     */
    private List<Row> rows(final AccessChoices current) {
        final List<Row> rows = new ArrayList<>();
        if (adding) {
            for (NameAndId candidate : current.view().candidates()) {
                rows.add(new Row(candidate.id(), candidate.name(), null));
            }
            return rows;
        }
        for (Member member : current.view().members()) {
            rows.add(new Row(member.id(), member.name(), member.role()));
        }
        rows.sort(Comparator.comparing((Row row) -> row.role() != null ? row.role().ordinal() : 0)
                .thenComparing(row -> row.name().toLowerCase(Locale.ROOT)));
        return rows;
    }

    private @Nullable Member selectedMember(final AccessChoices current) {
        final UUID chosen = selected;
        return chosen != null ? current.rules().member(chosen).orElse(null) : null;
    }

    /**
     * @param direction 1 for the next role down the list, -1 for the one before
     * @return the change to the nearest role that way the viewer may give the
     *         member; {@code null} when there is none
     */
    private static @Nullable SecurityEdit stepRole(final AccessChoices current, final Member member,
                                                   final int direction) {
        final int start = MEMBER_ROLES.indexOf(member.role());
        for (int step = 1; step < MEMBER_ROLES.size(); step++) {
            final int index = Math.floorMod(start + direction * step, MEMBER_ROLES.size());
            final SecurityEdit change = new SecurityEdit.ChangeRole(member.id(), MEMBER_ROLES.get(index));
            if (start >= 0 && current.allows(change)) {
                return change;
            }
        }
        return null;
    }

    private static @Nullable SecurityEdit nextDefault(final AccessChoices current) {
        final int start = DEFAULT_ROLES.indexOf(current.view().defaultRole());
        for (int step = 1; step < DEFAULT_ROLES.size(); step++) {
            final SecurityEdit change = new SecurityEdit.ChangeDefaultRole(
                    DEFAULT_ROLES.get(Math.floorMod(start + step, DEFAULT_ROLES.size())));
            if (current.allows(change)) {
                return change;
            }
        }
        return null;
    }

    private static @Nullable SecurityEdit allowed(final AccessChoices current, final SecurityEdit edit) {
        return current.allows(edit) ? edit : null;
    }

    private HeaderAction headerAction(final AccessChoices current) {
        if (current.rules().owner().isEmpty()) {
            return HeaderAction.CLAIM;
        }
        return adding ? HeaderAction.BACK : HeaderAction.ADD;
    }

    private void send(final @Nullable SecurityEdit edit) {
        if (edit != null) {
            ClientPacketDistributor.sendToServer(new NetworkAccessEditPayload(menu.pos(), edit));
        }
    }

    private void button(final GuiGraphicsExtractor graphics, final PanelStyle style, final PanelBounds bounds,
                        final Component label, final boolean enabled) {
        style.drawButton(graphics, font, bounds, label);
        if (!enabled) {
            graphics.fill(bounds.left(), bounds.top(), bounds.left() + bounds.width(), bounds.top() + bounds.height(),
                    style.disabledVeil());
        }
    }

    /**
     * One line of the list: a member with their role, or a player to add, without one.
     */
    private record Row(UUID id, String name, @Nullable Role role) {
    }

    /**
     * What the button beside the list heading does.
     */
    private enum HeaderAction {
        ADD, BACK, CLAIM;

        Component label() {
            return Component.translatable("gui.nexus.access." + name().toLowerCase(Locale.ROOT));
        }

        /**
         * Adding is asked for a player nobody is: whether the viewer may add anyone at all.
         */
        boolean isEnabled(final AccessChoices current) {
            return switch (this) {
                case ADD -> current.allows(new SecurityEdit.AddMember(Util.NIL_UUID, "?"));
                case BACK -> true;
                case CLAIM -> current.allows(new SecurityEdit.Claim("?"));
            };
        }
    }
}
