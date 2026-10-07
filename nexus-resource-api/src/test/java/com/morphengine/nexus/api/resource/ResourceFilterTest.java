package com.morphengine.nexus.api.resource;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceFilterTest {

    private static final ResourceType TYPE = new ResourceType() {
    };
    private static final ResourceKey STONE = new Named("stone");
    private static final ResourceKey DIRT = new Named("dirt");

    @Test
    void emptyFilterAllowsEverythingInEitherMode() {
        assertThat(ResourceFilter.NONE.allows(STONE)).isTrue();
        assertThat(new ResourceFilter(FilterMode.DENY, Set.of()).allows(STONE)).isTrue();
    }

    @Test
    void whitelistAllowsOnlyListedResources() {
        final ResourceFilter filter = new ResourceFilter(FilterMode.ALLOW, Set.of(STONE));

        assertThat(filter.allows(STONE)).isTrue();
        assertThat(filter.allows(DIRT)).isFalse();
    }

    @Test
    void blacklistDeniesOnlyListedResources() {
        final ResourceFilter filter = new ResourceFilter(FilterMode.DENY, Set.of(STONE));

        assertThat(filter.allows(STONE)).isFalse();
        assertThat(filter.allows(DIRT)).isTrue();
    }

    @Test
    void onlyWhitelistSinglesOutItsResources() {
        assertThat(new ResourceFilter(FilterMode.ALLOW, Set.of(STONE)).singlesOut(STONE)).isTrue();
        assertThat(new ResourceFilter(FilterMode.ALLOW, Set.of(STONE)).singlesOut(DIRT)).isFalse();
        assertThat(new ResourceFilter(FilterMode.DENY, Set.of(STONE)).singlesOut(STONE)).isFalse();
    }

    @Test
    void listedResourcesAreCopied() {
        final Set<ResourceKey> listed = new HashSet<>(Set.of(STONE));
        final ResourceFilter filter = new ResourceFilter(FilterMode.ALLOW, listed);

        listed.add(DIRT);

        assertThat(filter.allows(DIRT)).isFalse();
    }

    @Test
    void toggledModeSwapsWhitelistAndBlacklist() {
        assertThat(FilterMode.ALLOW.toggled()).isEqualTo(FilterMode.DENY);
        assertThat(FilterMode.DENY.toggled()).isEqualTo(FilterMode.ALLOW);
    }

    @Test
    void looseMatchModeIgnoresWhatItNormalizesAway() {
        final ResourceFilter filter = new ResourceFilter(
                FilterMode.ALLOW, FilterMatchMode.IGNORE_DURABILITY, Set.of(new Fuzzy("pick", 0)));

        assertThat(filter.allows(new Fuzzy("pick", 5))).isTrue();
        assertThat(filter.allows(new Fuzzy("shovel", 0))).isFalse();
    }

    @Test
    void exactMatchModeStillTellsNormalizedKeysApart() {
        final ResourceFilter filter = new ResourceFilter(
                FilterMode.ALLOW, FilterMatchMode.EXACT, Set.of(new Fuzzy("pick", 0)));

        assertThat(filter.allows(new Fuzzy("pick", 5))).isFalse();
    }

    @Test
    void looseMatchModeSinglesOutAWornResourceToo() {
        final ResourceFilter filter = new ResourceFilter(
                FilterMode.ALLOW, FilterMatchMode.IGNORE_DURABILITY, Set.of(new Fuzzy("pick", 0)));

        assertThat(filter.singlesOut(new Fuzzy("pick", 5))).isTrue();
    }

    @Test
    void whitelistedGroupAllowsItsMembersOnly() {
        final ResourceFilter filter = new ResourceFilter(
                FilterMode.ALLOW, FilterMatchMode.EXACT, Set.of(), List.of(new Prefix("st")));

        assertThat(filter.allows(STONE)).isTrue();
        assertThat(filter.allows(DIRT)).isFalse();
    }

    @Test
    void blacklistedGroupDeniesItsMembersOnly() {
        final ResourceFilter filter = new ResourceFilter(
                FilterMode.DENY, FilterMatchMode.EXACT, Set.of(), List.of(new Prefix("st")));

        assertThat(filter.allows(STONE)).isFalse();
        assertThat(filter.allows(DIRT)).isTrue();
    }

    @Test
    void groupAndResourceListedTogetherBothCount() {
        final ResourceFilter filter = new ResourceFilter(
                FilterMode.ALLOW, FilterMatchMode.EXACT, Set.of(DIRT), List.of(new Prefix("st")));

        assertThat(filter.allows(STONE)).isTrue();
        assertThat(filter.allows(DIRT)).isTrue();
        assertThat(filter.allows(new Named("sand"))).isFalse();
    }

    @Test
    void whitelistedGroupSinglesOutItsMembers() {
        final ResourceFilter filter = new ResourceFilter(
                FilterMode.ALLOW, FilterMatchMode.EXACT, Set.of(), List.of(new Prefix("st")));

        assertThat(filter.singlesOut(STONE)).isTrue();
        assertThat(filter.singlesOut(DIRT)).isFalse();
    }

    @Test
    void groupWithNoMemberPresentStillMakesTheFilterNonEmpty() {
        final ResourceFilter filter = new ResourceFilter(
                FilterMode.ALLOW, FilterMatchMode.EXACT, Set.of(), List.of(new Prefix("zz")));

        assertThat(filter.allows(STONE)).isFalse();
    }

    @Test
    void groupMembershipIgnoresMatchMode() {
        final ResourceFilter filter = new ResourceFilter(
                FilterMode.ALLOW, FilterMatchMode.IGNORE_DURABILITY, Set.of(), List.of(new Prefix("pi")));

        assertThat(filter.allows(new Fuzzy("pick", 5))).isTrue();
    }

    private record Prefix(String prefix) implements ResourceGroup {

        @Override
        public boolean contains(final ResourceKey resource) {
            return resource instanceof Named named && named.name().startsWith(prefix)
                    || resource instanceof Fuzzy fuzzy && fuzzy.name().startsWith(prefix);
        }
    }

    private record Named(String name) implements ResourceKey {

        @Override
        public ResourceType type() {
            return TYPE;
        }
    }

    /**
     * A resource with wear a looser match mode ignores, such as an item's damage.
     */
    private record Fuzzy(String name, int wear) implements ResourceKey {

        @Override
        public ResourceType type() {
            return TYPE;
        }

        @Override
        public ResourceKey normalized(final FilterMatchMode mode) {
            return mode == FilterMatchMode.EXACT ? this : new Fuzzy(name, 0);
        }
    }
}
