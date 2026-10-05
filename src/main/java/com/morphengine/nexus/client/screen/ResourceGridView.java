package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.ResourceTypes;
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
import java.util.Locale;
import java.util.Map;

/**
 * The resources a terminal shows: those of the chosen type that match the
 * search, sorted as the player chose. The search is split into words that must
 * all match; a word starting with {@code @} matches the id of the mod that adds
 * the resource, any other word its name. The list is rebuilt only when the
 * contents, the settings or the search change. While the player holds Shift to
 * take many stacks, a change of the contents moves nothing: every resource stays
 * where it is until Shift is released. Client thread only.
 */
final class ResourceGridView {

    private static final String MOD_PREFIX = "@";
    /** Names are cached; the cache starts over past this size, as resources come and go. */
    private static final int NAME_CACHE_LIMIT = 8192;

    private final Map<NexusResource, String> names = new HashMap<>();
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
        if (names.size() > NAME_CACHE_LIMIT) {
            names.clear();
        }
        final List<String> words = List.of(query.toLowerCase(Locale.ROOT).strip().split("\\s+"));
        final List<TerminalEntry> matching = new ArrayList<>();
        for (TerminalEntry entry : contents.entries()) {
            if (isShownType(entry.resource(), settings) && matchesAll(entry.resource(), words)) {
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

    private boolean matchesAll(final NexusResource resource, final List<String> words) {
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            final boolean matches = word.startsWith(MOD_PREFIX)
                    ? resource.id().getNamespace().contains(word.substring(MOD_PREFIX.length()))
                    : nameOf(resource).contains(word);
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    private String nameOf(final NexusResource resource) {
        return names.computeIfAbsent(resource, key -> key.name().getString().toLowerCase(Locale.ROOT));
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
