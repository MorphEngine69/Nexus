package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.storage.Storage;

import java.util.ArrayList;
import java.util.List;

/**
 * The sources of a {@link NetworkStorage} in the order it uses them: sorted by priority, highest first, and sources of
 * equal priority in the order they were added. Server thread only.
 */
final class PrioritizedSources {

    private final List<Source> sources = new ArrayList<>();

    int size() {
        return sources.size();
    }

    Source get(final int index) {
        return sources.get(index);
    }

    Source remove(final int index) {
        return sources.remove(index);
    }

    /**
     * Puts {@code storage} after every source of its priority or a higher one.
     */
    void insert(final Storage storage, final int priority) {
        int index = 0;
        while (index < sources.size() && sources.get(index).priority() >= priority) {
            index++;
        }
        sources.add(index, new Source(storage, priority));
    }

    /**
     * @return where {@code storage} is, compared by identity, or -1
     */
    int indexOf(final Storage storage) {
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).storage() == storage) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return index just past the last source with the same priority as the one at {@code start}
     */
    int groupEnd(final int start) {
        final int priority = sources.get(start).priority();
        int end = start + 1;
        while (end < sources.size() && sources.get(end).priority() == priority) {
            end++;
        }
        return end;
    }

    /**
     * @return index of the first source with the same priority as the one just before {@code end}
     */
    int groupStart(final int end) {
        final int priority = sources.get(end - 1).priority();
        int start = end - 1;
        while (start > 0 && sources.get(start - 1).priority() == priority) {
            start--;
        }
        return start;
    }

    /**
     * @return index of the first source of {@code priority}, or where one would go
     */
    int bandStart(final int priority) {
        int start = 0;
        while (start < sources.size() && sources.get(start).priority() > priority) {
            start++;
        }
        return start;
    }

    /**
     * @return index just past the last source of {@code priority}, from {@code start}
     */
    int bandEnd(final int start, final int priority) {
        int end = start;
        while (end < sources.size() && sources.get(end).priority() == priority) {
            end++;
        }
        return end;
    }

    /**
     * A storage and the priority it is used at.
     */
    record Source(Storage storage, int priority) {
    }
}
