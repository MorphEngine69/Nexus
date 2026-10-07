package com.morphengine.nexus.search;

import java.util.Collection;

/**
 * Something a {@link ResourceQuery} is matched against. Every text is lower case, and the ones that are costly to find
 * out are asked for only when the query uses them.
 */
public interface SearchTarget {

    /**
     * @return the name players see
     */
    String name();

    /**
     * @return the id of the mod that adds it, such as {@code minecraft}
     */
    String modId();

    /**
     * @return the name of the mod that adds it, such as {@code nexus}; the id when the mod has no other name
     */
    String modName();

    /**
     * @return the ids of its tags, such as {@code c:ingots/iron}
     */
    Collection<String> tags();

    /**
     * @return the text of its tooltip, lines joined
     */
    String tooltip();
}
