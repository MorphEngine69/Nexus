package com.morphengine.nexus.assembler;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Whether an Assembler waits for its machine before handing it the next run.
 */
public enum LockMode implements StringRepresentable {

    /** Hands out runs as long as the machine takes their inputs. */
    NEVER,

    /**
     * Hands out a run only while the machine holds none of the inputs of any
     * of its processing blueprints, so a machine that mixes whatever it holds
     * works on one run at a time.
     */
    UNTIL_EMPTY;

    public static final Codec<LockMode> CODEC = StringRepresentable.fromEnum(LockMode::values);

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
