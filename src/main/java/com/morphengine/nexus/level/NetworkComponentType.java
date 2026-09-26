package com.morphengine.nexus.level;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A kind of {@link NetworkComponent}, and the key under which a network hands
 * out its instance.
 *
 * @param <C> the component
 */
public final class NetworkComponentType<C extends NetworkComponent> {

    private final String name;
    private final Supplier<C> factory;

    /**
     * @param name    shown in diagnostics
     * @param factory creates the component of a new network
     */
    public NetworkComponentType(final String name, final Supplier<C> factory) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
    }

    C create() {
        return factory.get();
    }

    @Override
    public String toString() {
        return name;
    }
}
