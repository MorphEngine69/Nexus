package com.morphengine.nexus.assembler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * How an Assembler is set up in its panel.
 *
 * @param priority its blueprints are preferred over those of Assemblers of a lower one
 * @param lock     whether it waits for its machine before handing it the next run
 */
public record AssemblerSettings(int priority, LockMode lock) {

    public static final AssemblerSettings DEFAULT = new AssemblerSettings(0, LockMode.NEVER);

    public static final Codec<AssemblerSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(AssemblerSettings::priority),
                    LockMode.CODEC.optionalFieldOf("lock", LockMode.NEVER).forGetter(AssemblerSettings::lock))
            .apply(instance, AssemblerSettings::new));

    public AssemblerSettings {
        Objects.requireNonNull(lock, "lock must not be null");
    }

    public AssemblerSettings withPriority(final int newPriority) {
        return new AssemblerSettings(newPriority, lock);
    }

    public AssemblerSettings withLock(final LockMode newLock) {
        return new AssemblerSettings(priority, newLock);
    }
}
