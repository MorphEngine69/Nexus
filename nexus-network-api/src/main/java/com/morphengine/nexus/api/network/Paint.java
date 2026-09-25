package com.morphengine.nexus.api.network;

/**
 * Paint of a network block as seen by connection rules. Painted blocks join only
 * blocks of the same paint, so two colored cable runs can lie side by side without
 * merging. An unpainted block joins anything and therefore bridges runs of
 * different colors; separate networks are made with separate controllers, not
 * with paint.
 */
public sealed interface Paint {

    Paint UNPAINTED = new Unpainted();

    static Paint dye(int dyeId) {
        return new Dyed(dyeId);
    }

    boolean connectsTo(Paint other);

    record Unpainted() implements Paint {

        @Override
        public boolean connectsTo(final Paint other) {
            return true;
        }
    }

    /**
     * @param dyeId identifier of the dye, must not be negative
     */
    record Dyed(int dyeId) implements Paint {

        public Dyed {
            if (dyeId < 0) {
                throw new IllegalArgumentException("dye id must not be negative: " + dyeId);
            }
        }

        @Override
        public boolean connectsTo(final Paint other) {
            return switch (other) {
                case Unpainted unpainted -> true;
                case Dyed dyed -> dyed.dyeId == dyeId;
            };
        }
    }
}
