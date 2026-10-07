package com.morphengine.nexus.probe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What a tooltip mod shows for a block, line by line, top to bottom. Built on the server, where the block entity is,
 * and sent to the client by the mod that shows it.
 *
 * @param lines the lines; copied
 */
public record ProbeReport(List<ProbeLine> lines) {

    public static final ProbeReport EMPTY = new ProbeReport(List.of());

    public static final StreamCodec<RegistryFriendlyByteBuf, ProbeReport> STREAM_CODEC =
            ProbeLine.STREAM_CODEC.apply(ByteBufCodecs.list()).map(ProbeReport::new, ProbeReport::lines);

    public ProbeReport {
        lines = List.copyOf(Objects.requireNonNull(lines, "lines must not be null"));
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /**
     * @return this report with the lines of {@code more} after its own
     */
    public ProbeReport and(final ProbeReport more) {
        final List<ProbeLine> joined = new ArrayList<>(lines);
        joined.addAll(more.lines);
        return new ProbeReport(joined);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Collects the lines of a report in the order they are added.
     */
    public static final class Builder {

        private final List<ProbeLine> lines = new ArrayList<>();

        private Builder() {
        }

        public Builder text(final Component text) {
            lines.add(new ProbeLine.Text(text));
            return this;
        }

        public Builder energy(final long stored, final long capacity) {
            lines.add(new ProbeLine.Energy(stored, capacity));
            return this;
        }

        public Builder tank(final FluidStack contents, final long capacity, final Component name) {
            lines.add(new ProbeLine.Tank(contents, capacity, name));
            return this;
        }

        public Builder progress(final Component label, final int percent) {
            lines.add(new ProbeLine.Progress(label, percent));
            return this;
        }

        public ProbeReport build() {
            return new ProbeReport(lines);
        }
    }
}
