package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.ResourceTypes;
import com.morphengine.nexus.search.ResourceQuery;
import com.morphengine.nexus.terminal.SortDirection;
import com.morphengine.nexus.terminal.TerminalContents;
import com.morphengine.nexus.terminal.TerminalEntry;
import com.morphengine.nexus.terminal.TerminalSettings;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The resources a terminal shows: those of the chosen type that match the
 * search, sorted as the player chose. What the search understands is told by
 * {@link ResourceQuery}. The list is rebuilt only when the
 * contents, the settings or the search change. While the player holds Shift to
 * take many stacks, a change of the contents moves nothing: every resource stays
 * where it is until Shift is released. Client thread only.
 */
final class ResourceGridView {

    /** Targets are cached; the cache starts over past this size, as resources come and go. */
    private static final int TARGET_CACHE_LIMIT = 8192;

    private final Map<NexusResource, ResourceSearchTarget> targets = new HashMap<>();
    private List<TerminalEntry> shown = List.of();
    private int builtRevision = -1;
    private @Nullable TerminalSettings builtSettings;
    private String builtQuery = "";
    private boolean orderIsStale;

    List<TerminalEntry> update(final TerminalContents contents, final TerminalSettings settings, final String query) {
        if (contents.revision() != builtRevision || orderIsStale || isOtherView(settings, query)) {
            remember(contents, settings, query);
            orderIsStale = false;
            shown = build(contents, settings, query);
        }
        return shown;
    }

    /**
     * Like {@link #update}, but a change of the contents only changes the amounts: resources keep their places, those
     * that are gone leave, new ones go last. The next {@link #update} puts everything in order.
     */
    List<TerminalEntry> updateKeepingOrder(
            final TerminalContents contents, final TerminalSettings settings, final String query) {
        if (isOtherView(settings, query)) {
            return update(contents, settings, query);
        }
        if (contents.revision() != builtRevision) {
            remember(contents, settings, query);
            orderIsStale = true;
            shown = refreshInPlace(build(contents, settings, query));
        }
        return shown;
    }

    private boolean isOtherView(final TerminalSettings settings, final String query) {
        return !settings.equals(builtSettings) || !query.equals(builtQuery);
    }

    private void remember(final TerminalContents contents, final TerminalSettings settings, final String query) {
        builtRevision = contents.revision();
        builtSettings = settings;
        builtQuery = query;
    }

    private List<TerminalEntry> refreshInPlace(final List<TerminalEntry> current) {
        final Map<NexusResource, TerminalEntry> remaining = new LinkedHashMap<>();
        for (TerminalEntry entry : current) {
            remaining.put(entry.resource(), entry);
        }
        final List<TerminalEntry> kept = new ArrayList<>();
        for (TerminalEntry before : shown) {
            final TerminalEntry now = remaining.remove(before.resource());
            if (now != null) {
                kept.add(now);
            }
        }
        kept.addAll(remaining.values());
        return List.copyOf(kept);
    }

    private List<TerminalEntry> build(final TerminalContents contents, final TerminalSettings settings,
                                      final String query) {
        if (targets.size() > TARGET_CACHE_LIMIT) {
            targets.clear();
        }
        final ResourceQuery parsed = ResourceQuery.parse(query);
        final List<TerminalEntry> matching = new ArrayList<>();
        for (TerminalEntry entry : contents.entries()) {
            if (isShownType(entry.resource(), settings) && parsed.matches(targetOf(entry.resource()))) {
                matching.add(entry);
            }
        }
        matching.sort(comparator(settings));
        return List.copyOf(matching);
    }

    private static boolean isShownType(final NexusResource resource, final TerminalSettings settings) {
        return settings.shownType() == null
                || settings.shownType().equals(ResourceTypes.REGISTRY.getKey(resource.type()));
    }

    private ResourceSearchTarget targetOf(final NexusResource resource) {
        return targets.computeIfAbsent(resource, ResourceSearchTarget::new);
    }

    private String nameOf(final NexusResource resource) {
        return targetOf(resource).name();
    }

    private Comparator<TerminalEntry> comparator(final TerminalSettings settings) {
        final Comparator<TerminalEntry> byName = Comparator.comparing(entry -> nameOf(entry.resource()));
        final Comparator<TerminalEntry> chosen = switch (settings.sort()) {
            case AMOUNT -> Comparator.comparingLong(TerminalEntry::amount).thenComparing(byName);
            case NAME -> byName;
            case MOD -> Comparator.<TerminalEntry, String>comparing(entry -> entry.resource().id().getNamespace())
                    .thenComparing(byName);
            case ID -> Comparator.comparing(entry -> entry.resource().id().toString());
        };
        return settings.direction() == SortDirection.ASCENDING ? chosen : chosen.reversed();
    }
}
