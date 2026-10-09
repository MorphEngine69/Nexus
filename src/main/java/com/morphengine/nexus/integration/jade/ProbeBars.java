package com.morphengine.nexus.integration.jade;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.probe.BlockProbes;
import com.morphengine.nexus.probe.ProbeLine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ProgressView;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The bars of a block's report, drawn by Jade as it draws those of every other mod: energy, tanks and progress. A
 * provider has no group for a block whose report has no such line, and so leaves the block to Jade's own.
 *
 * @param <L> the kind of line the bar is made of
 * @param <D> what the server sends for one bar
 * @param <V> what the client shows for it
 */
final class ProbeBars<L extends ProbeLine, D, V>
        implements IServerExtensionProvider<D>, IClientExtensionProvider<D, V> {

    static final ProbeBars<ProbeLine.Energy, CompoundTag, EnergyView> ENERGY = new ProbeBars<>(
            "probe_energy", ProbeLine.Energy.class,
            energy -> EnergyView.of(energy.stored(), energy.capacity()),
            data -> EnergyView.read(data, "FE"));

    static final ProbeBars<ProbeLine.Tank, CompoundTag, FluidView> TANKS = new ProbeBars<>(
            "probe_tanks", ProbeLine.Tank.class,
            ProbeBars::tankData, data -> FluidView.readDefault(data));

    static final ProbeBars<ProbeLine.Progress, CompoundTag, ProgressView> PROGRESS = new ProbeBars<>(
            "probe_progress", ProbeLine.Progress.class,
            progress -> ProgressView.create(progress.percent() / ProbeBars.PERCENT), ProgressView::read);

    private static final float PERCENT = 100F;
    private static final long MILLIBUCKETS_PER_BUCKET = 1000;

    private final ResourceLocation uid;
    private final Class<L> lineType;
    private final Function<L, D> toData;
    private final Function<D, V> toView;

    private ProbeBars(
            final String name, final Class<L> lineType, final Function<L, D> toData, final Function<D, V> toView) {
        this.uid = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, name);
        this.lineType = lineType;
        this.toData = toData;
        this.toView = toView;
    }

    /**
     * @return the lines of {@code lineType} in the report of the block the accessor points at; none for anything that
     *         is not a block with a report
     */
    static <T extends ProbeLine> List<T> linesOf(final Accessor<?> accessor, final Class<T> lineType) {
        final List<T> found = new ArrayList<>();
        if (accessor instanceof BlockAccessor block && block.getBlockEntity() instanceof BlockEntity entity) {
            for (ProbeLine line : BlockProbes.of(entity).lines()) {
                if (lineType.isInstance(line)) {
                    found.add(lineType.cast(line));
                }
            }
        }
        return found;
    }

    @Override
    public ResourceLocation getUid() {
        return uid;
    }

    @Override
    public int getDefaultPriority() {
        return -1;
    }

    @Override
    public @Nullable List<ViewGroup<D>> getGroups(final Accessor<?> accessor) {
        final List<L> lines = linesOf(accessor, lineType);
        if (lines.isEmpty()) {
            return null;
        }
        return lines.stream().map(line -> new ViewGroup<>(List.of(toData.apply(line)))).toList();
    }

    @Override
    public List<ClientViewGroup<V>> getClientGroups(final Accessor<?> accessor, final List<ViewGroup<D>> groups) {
        return ClientViewGroup.map(groups, toView, null);
    }

    private static CompoundTag tankData(final ProbeLine.Tank tank) {
        final FluidStack contents = tank.contents();
        final JadeFluidObject fluid = contents.isEmpty()
                ? JadeFluidObject.empty() : JadeFluidObject.of(contents.getFluid(), inJadeUnits(contents.getAmount()));
        return FluidView.writeDefault(fluid, inJadeUnits(tank.capacity()));
    }

    private static long inJadeUnits(final long millibuckets) {
        return millibuckets * JadeFluidObject.bucketVolume() / MILLIBUCKETS_PER_BUCKET;
    }
}
