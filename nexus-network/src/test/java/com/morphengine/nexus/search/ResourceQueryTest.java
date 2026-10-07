package com.morphengine.nexus.search;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceQueryTest {

    private static final SearchTarget IRON_INGOT = target("iron ingot", "minecraft", "minecraft",
            List.of("c:ingots", "c:ingots/iron"), "a shiny bar");
    private static final SearchTarget STEEL_PLATE = target("steel plate", "nexus", "nexus",
            List.of("c:plates", "c:plates/steel"), "needs a compressor");
    private static final SearchTarget MITHRIL = target("mithril ingot", "othermod", "mithril mod",
            List.of("c:ingots"), "");

    @Test
    void blankQueryMatchesEverything() {
        assertThat(ResourceQuery.parse("   ").matches(IRON_INGOT)).isTrue();
    }

    @Test
    void plainWordMatchesAnyPartOfTheName() {
        assertThat(ResourceQuery.parse("ingot").matches(IRON_INGOT)).isTrue();
        assertThat(ResourceQuery.parse("ingot").matches(STEEL_PLATE)).isFalse();
    }

    @Test
    void wordsSideBySideMustAllMatch() {
        assertThat(ResourceQuery.parse("iron ingot").matches(IRON_INGOT)).isTrue();
        assertThat(ResourceQuery.parse("iron plate").matches(IRON_INGOT)).isFalse();
    }

    @Test
    void searchIsNotCaseSensitive() {
        assertThat(ResourceQuery.parse("IRON").matches(IRON_INGOT)).isTrue();
    }

    @Test
    void atSignMatchesModIdAndModName() {
        assertThat(ResourceQuery.parse("@nexus").matches(STEEL_PLATE)).isTrue();
        assertThat(ResourceQuery.parse("@mithril").matches(MITHRIL)).isTrue();
        assertThat(ResourceQuery.parse("@nexus").matches(IRON_INGOT)).isFalse();
    }

    @Test
    void hashMatchesTags() {
        assertThat(ResourceQuery.parse("#ingots/iron").matches(IRON_INGOT)).isTrue();
        assertThat(ResourceQuery.parse("#ingots/iron").matches(MITHRIL)).isFalse();
        assertThat(ResourceQuery.parse("#c:plates").matches(STEEL_PLATE)).isTrue();
    }

    @Test
    void dollarMatchesTheTooltip() {
        assertThat(ResourceQuery.parse("$compressor").matches(STEEL_PLATE)).isTrue();
        assertThat(ResourceQuery.parse("$compressor").matches(IRON_INGOT)).isFalse();
    }

    @Test
    void barLetsEitherSideMatch() {
        final ResourceQuery query = ResourceQuery.parse("plate | mithril");

        assertThat(query.matches(STEEL_PLATE)).isTrue();
        assertThat(query.matches(MITHRIL)).isTrue();
        assertThat(query.matches(IRON_INGOT)).isFalse();
    }

    @Test
    void bindsWordsTighterThanBar() {
        final ResourceQuery query = ResourceQuery.parse("iron ingot | steel");

        assertThat(query.matches(IRON_INGOT)).isTrue();
        assertThat(query.matches(STEEL_PLATE)).isTrue();
        assertThat(query.matches(MITHRIL)).isFalse();
    }

    @Test
    void exclamationAndDashTurnAWordRound() {
        assertThat(ResourceQuery.parse("!iron").matches(IRON_INGOT)).isFalse();
        assertThat(ResourceQuery.parse("-iron").matches(STEEL_PLATE)).isTrue();
        assertThat(ResourceQuery.parse("ingot -@minecraft").matches(MITHRIL)).isTrue();
        assertThat(ResourceQuery.parse("ingot -@minecraft").matches(IRON_INGOT)).isFalse();
    }

    @Test
    void parenthesesGroup() {
        final ResourceQuery query = ResourceQuery.parse("@nexus | (ingot !iron)");

        assertThat(query.matches(STEEL_PLATE)).isTrue();
        assertThat(query.matches(MITHRIL)).isTrue();
        assertThat(query.matches(IRON_INGOT)).isFalse();
    }

    @Test
    void quotesKeepSpacesInAWord() {
        assertThat(ResourceQuery.parse("\"ron ing\"").matches(IRON_INGOT)).isTrue();
        assertThat(ResourceQuery.parse("\"ingot iron\"").matches(IRON_INGOT)).isFalse();
        assertThat(ResourceQuery.parse("@\"mithril mod\"").matches(MITHRIL)).isTrue();
    }

    @Test
    void prefixWithNothingAfterItMatchesEverything() {
        assertThat(ResourceQuery.parse("@").matches(IRON_INGOT)).isTrue();
        assertThat(ResourceQuery.parse("iron #").matches(IRON_INGOT)).isTrue();
    }

    @Test
    void unfinishedQueryMatchesAsFarAsItGoes() {
        assertThat(ResourceQuery.parse("(iron").matches(IRON_INGOT)).isTrue();
        assertThat(ResourceQuery.parse("\"iron ing").matches(IRON_INGOT)).isTrue();
        assertThat(ResourceQuery.parse("iron |").matches(STEEL_PLATE)).isTrue();
        assertThat(ResourceQuery.parse("!").matches(IRON_INGOT)).isTrue();
    }

    @Test
    void strayClosingParenthesisIsSkipped() {
        assertThat(ResourceQuery.parse("iron) ingot").matches(IRON_INGOT)).isTrue();
        assertThat(ResourceQuery.parse("iron) plate").matches(IRON_INGOT)).isFalse();
    }

    @Test
    void lonelyDashIsAnOrdinaryWord() {
        assertThat(ResourceQuery.parse("- ").matches(IRON_INGOT)).isFalse();
    }

    private static SearchTarget target(
            final String name, final String modId, final String modName, final List<String> tags,
            final String tooltip) {
        return new Target(name, modId, modName, tags, tooltip);
    }

    private record Target(String name, String modId, String modName, Collection<String> tags, String tooltip)
            implements SearchTarget {
    }
}
