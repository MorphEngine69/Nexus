package com.morphengine.nexus.gametest;

import java.util.ArrayList;
import java.util.List;

/**
 * A scope of changes that is undone when it closes without being committed, so a test can try a transfer and look at
 * what it did without keeping it.
 */
final class Transaction implements AutoCloseable {

    private final List<Runnable> undo = new ArrayList<>();
    private boolean committed;

    private Transaction() {
    }

    static Transaction openRoot() {
        return new Transaction();
    }

    void onRollback(final Runnable step) {
        undo.add(step);
    }

    void commit() {
        committed = true;
    }

    @Override
    public void close() {
        if (committed) {
            return;
        }
        for (int index = undo.size() - 1; index >= 0; index--) {
            undo.get(index).run();
        }
    }
}
