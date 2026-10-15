/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableEnd;
import dev.jstech.core.cable.CableParts;
import dev.jstech.core.cable.LineFlows;
import dev.jstech.core.cable.NarrowestWire;
import dev.jstech.core.chemical.ChemicalBridges;
import dev.jstech.core.chemical.IChemicalPort;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.fluid.CoreFluidTags;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.EndRouter;
import dev.jstech.core.grid.FlowTrace;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPlaces;
import dev.jstech.core.grid.IGridMover;
import dev.jstech.core.grid.LineSurvey;
import dev.jstech.core.grid.PartTally;
import dev.jstech.core.inventory.NamedSubject;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.transfer.Batch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The grid of one state of matter in one dimension as it moves it: gas along gas lines, slurry along lined lines,
 * plasma along its conduits. Each connected part moves, each tick, at most what its slowest line carries (the pressure
 * it holds), between the blocks it meets: the Core's stores of that state, a chemical mod's stores through its bridge
 * (each chemical in the state its bridge says), and, for gases, the tanks of mods that keep gases as fluids.
 *
 * <p>The ends share as the fluid grid's do ({@link EndRouter}): what gives and would not take it back (a machine's
 * output) feeds what takes and gives nothing of it (a machine's input); what is left fills what both takes and gives it
 * (a tank); and a tank feeds what the inputs still want, each end by its mode, priority, rate, channel and filter. Each
 * move takes out and puts in as one batch, so nothing is lost on the way. A material linked to a chemical or a fluid
 * ({@link MatterBridges}) passes between them as the same thing; one with no link moves only between stores of its own
 * kind.
 */
public final class LevelMatter implements IGridMover {

    private final GridKind kind;
    private final MatterState state;
    private final List<Part> parts = new ArrayList<>();
    private final Map<Long, Integer> partOfWire = new HashMap<>();
    private long[] lastTick = new long[0];
    private long builtVersion = -1L;
    private long builtTaps = -1L;
    private long taps;
    private long turn;
    private final LineFlows flows = new LineFlows();

    /** What the debug screen calls the time the grids of the states of matter take to move a tick's matter. */
    public static final ResourceLocation TIMING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "matter_grid");
    /** The work of moving a tick's matter, timed, named in profilers and watched for stalls. */
    public static final Section SECTION = Section.builder(TIMING, CoreLogs.MATTER).declare();

    /** The work that moves {@code state} along the grid of {@code kind}, one for each dimension. */
    public LevelMatter(final GridKind kind, final MatterState state) {
        this.kind = kind;
        this.state = state;
    }

    /**
     * Whether a line of {@code state} plugs into the block at {@code pos}, met on its {@code face}: one that offers the
     * Core's store of that state, a chemical store through a bridge, or, for a gas, a tank that holds or takes a fluid
     * marked as a gas.
     */
    public static boolean plugsInto(final MatterState state, final ServerLevel level, final BlockPos pos,
                                    final Direction face) {
        final Optional<BlockCapability<IMatterHandler, @Nullable Direction>> capability = MatterCapabilities.block(state);
        if (capability.isPresent() && level.getCapability(capability.get(), pos, face) != null) {
            return true;
        }
        if (ChemicalBridges.portFor(level, pos, face).isPresent()) {
            return true;
        }
        return state == MatterState.GAS && holdsOrTakesGas(level.getCapability(Capabilities.FluidHandler.BLOCK, pos,
                face));
    }

    /** The state it moves. */
    public MatterState state() {
        return this.state;
    }

    /** How many millibuckets the part the wire numbered {@code wire} is in moved in the last tick. */
    public OptionalLong movedLastTickOf(final long wire) {
        final Integer part = this.partOfWire.get(wire);
        return part == null || part >= this.lastTick.length ? OptionalLong.empty()
                : OptionalLong.of(this.lastTick[part]);
    }

    /** How many connected parts the grid is in. */
    public int partCount() {
        return this.parts.size();
    }

    @Override
    public void tapsChanged() {
        this.taps++;
    }

    @Override
    public Optional<LineSurvey> survey(final long wire) {
        final Integer part = this.partOfWire.get(wire);
        return part == null || part >= this.parts.size() ? Optional.empty()
                : Optional.of(this.parts.get(part).tally().survey());
    }

    @Override
    public void tick(final ServerLevel level) {
        final Grid grid = CoreGrids.of(level, this.kind);
        if (grid.size() == 0) {
            this.parts.clear();
            this.partOfWire.clear();
            this.lastTick = new long[0];
            return;
        }
        SECTION.run(() -> move(level, grid));
    }

    /* Whether a fluid store holds a fluid marked as a gas, or would take one. */
    private static boolean holdsOrTakesGas(@Nullable final IFluidHandler handler) {
        if (handler == null) {
            return false;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (handler.getFluidInTank(tank).is(CoreFluidTags.GASES)) {
                return true;
            }
        }
        for (final Holder<Fluid> gas : BuiltInRegistries.FLUID.getTagOrEmpty(CoreFluidTags.GASES)) {
            if (handler.fill(new FluidStack(gas, 1), IFluidHandler.FluidAction.SIMULATE) > 0) {
                return true;
            }
        }
        return false;
    }

    /* The parts worked out again when the grid changed, and the tick's matter moved through each. */
    private void move(final ServerLevel level, final Grid grid) {
        if (grid.version() != this.builtVersion || this.taps != this.builtTaps) {
            build(level, grid);
            this.builtVersion = grid.version();
            this.builtTaps = this.taps;
        }
        final long[] moved = new long[this.parts.size()];
        final List<FlowTrace> traces = new ArrayList<>(this.parts.size());
        for (int i = 0; i < moved.length; i++) {
            final Part part = this.parts.get(i);
            part.tally().reset();
            moved[i] = part.rate() <= 0 ? 0L : EndRouter.route(part.ends(), part.rate(), this.turn,
                    substance -> true, (from, to, substance, amount) -> part.tally().moved(from.key(), to.key(),
                            move(part.trace(), from, to, substance, amount)));
            traces.add(part.trace());
        }
        this.turn++;
        this.lastTick = moved;
        this.flows.tick(level, traces);
    }

    private void build(final ServerLevel level, final Grid grid) {
        this.parts.clear();
        this.partOfWire.clear();
        final GridPlaces places = CoreGrids.places(level);
        for (final CableParts.Part found : CableParts.of(level, grid)) {
            long rate = Long.MAX_VALUE;
            final NarrowestWire narrowest = new NarrowestWire();
            for (final CableParts.WireAt wire : found.wires()) {
                rate = Math.min(rate, wire.runTooLong() ? 0L : wire.type().throughput());
                this.partOfWire.put(wire.number(), this.parts.size());
                narrowest.consider(places.place(wire.number()), wire);
            }
            final Set<String> seen = new HashSet<>();
            final List<MatterEnd> ends = new ArrayList<>();
            for (final CableParts.Plug plug : found.plugs()) {
                if (plug.end().mode() != EndMode.OFF && seen.add(plug.at().asLong() + "/" + plug.face())) {
                    ends.add(new MatterEnd(plug.at().asLong(), plug.wire(), plug.face().getOpposite(), plug.end(),
                            end(level, plug.at(), plug.face())));
                }
            }
            this.parts.add(new Part(rate, ends, LineFlows.traceOf(level, grid, found), new PartTally(
                    found.wires().size(), narrowest.found(), ends.stream().map(MatterEnd::key).toList())));
        }
    }

    /*
     * Moves {@code amount} of {@code substance} from one end's store to the other's, all or nothing, and has
     * {@code trace} follow it.
     */
    private static long move(final FlowTrace trace, final EndRouter.IEnd<Substance> from,
                             final EndRouter.IEnd<Substance> to, final Substance substance, final long amount) {
        final MatterEnd source = (MatterEnd) from;
        final MatterEnd target = (MatterEnd) to;
        final IStore given = source.store().get();
        final IStore taken = target.store().get();
        if (given == null || taken == null) {
            return 0L;
        }
        final Batch.Outcome outcome = new Batch().add(new Step(given, substance, amount, false))
                .add(new Step(taken, substance, amount, true)).commit();
        if (!outcome.done()) {
            return 0L;
        }
        trace.moved(source.wire(), target.wire(), target.into(), substance.id().toString(), amount);
        return amount;
    }

    /* The block at {@code at} as a store of this state, found again each tick through cached capabilities. */
    private Supplier<@Nullable IStore> end(final ServerLevel level, final BlockPos at, final Direction face) {
        final MatterState carried = this.state;
        final @Nullable BlockCapabilityCache<IMatterHandler, @Nullable Direction> matter = MatterCapabilities
                .block(carried).map(capability -> BlockCapabilityCache.create(capability, level, at, face))
                .orElse(null);
        final Supplier<Optional<IChemicalPort>> chemical = ChemicalBridges.portCache(level, at, face);
        final @Nullable BlockCapabilityCache<IFluidHandler, @Nullable Direction> fluid = carried == MatterState.GAS
                ? BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level, at, face) : null;
        return () -> {
            final IMatterHandler handler = matter == null ? null : matter.getCapability();
            if (handler != null) {
                return new MatterStore(handler);
            }
            final Optional<IChemicalPort> port = chemical.get();
            if (port.isPresent()) {
                return new ChemicalStore(port.get(), carried);
            }
            final IFluidHandler tank = fluid == null ? null : fluid.getCapability();
            return tank == null ? null : new GasFluidStore(tank);
        };
    }

    /* Where a substance comes from: a material of the catalogue, or a chemical or a fluid no material is linked to. */
    private enum Origin {
        MATERIAL, CHEMICAL, FLUID
    }

    /* A block a part meets, seen as a store of the line's state whatever mod made it. */
    private interface IStore {

        /* What it would give first of what {@code wanted} lets through, up to {@code most}; null for nothing. */
        @Nullable
        Offer offer(long most, Predicate<Substance> wanted);

        /* Puts in up to {@code amount}; how much it took, or would. */
        long take(Substance substance, long amount, boolean simulate);

        /* Takes out up to {@code amount}; how much came out, or would. */
        long give(Substance substance, long amount, boolean simulate);

        /* How much of {@code substance} it holds. */
        long holding(Substance substance);
    }

    /*
     * What moves: a material, or a chemical or a fluid of another mod with no material linked to it, which moves only
     * between stores of its own kind.
     */
    private record Substance(Origin origin, ResourceLocation id, @Nullable Material material) {

        private static Substance of(final Material material) {
            return new Substance(Origin.MATERIAL, material.id(), material);
        }

        private static Substance chemical(final ResourceLocation chemical, final MatterState state) {
            return MatterBridges.materialOfChemical(chemical).filter(material -> material.takes(state))
                    .map(Substance::of).orElseGet(() -> new Substance(Origin.CHEMICAL, chemical, null));
        }

        private static Substance fluid(final ResourceLocation fluid) {
            return MatterBridges.materialOfFluid(fluid).filter(material -> material.takes(MatterState.GAS))
                    .map(Substance::of).orElseGet(() -> new Substance(Origin.FLUID, fluid, null));
        }

        /* The chemical it is in a chemical mod, if any. */
        private Optional<ResourceLocation> asChemical() {
            return switch (this.origin) {
                case MATERIAL -> MatterBridges.chemicalOf(this.material);
                case CHEMICAL -> Optional.of(this.id);
                case FLUID -> Optional.empty();
            };
        }

        /* The fluid it is kept as by a mod that keeps gases as fluids, if any. */
        private Optional<ResourceLocation> asFluid() {
            return switch (this.origin) {
                case MATERIAL -> MatterBridges.fluidOf(this.material);
                case FLUID -> Optional.of(this.id);
                case CHEMICAL -> Optional.empty();
            };
        }
    }

    /* What a store offers: a substance and how much of it. */
    private record Offer(Substance substance, long amount) {
    }

    /* The Core's own store of the state. */
    private record MatterStore(IMatterHandler handler) implements IStore {

        @Override
        public @Nullable Offer offer(final long most, final Predicate<Substance> wanted) {
            for (int tank = 0; tank < this.handler.tanks(); tank++) {
                final Optional<MatterStack> held = this.handler.stack(tank);
                if (held.isEmpty() || !wanted.test(Substance.of(held.get().material()))) {
                    continue;
                }
                final long amount = this.handler.extract(held.get().material(), most, true);
                if (amount > 0) {
                    return new Offer(Substance.of(held.get().material()), amount);
                }
            }
            return null;
        }

        @Override
        public long holding(final Substance substance) {
            long held = 0L;
            for (int tank = 0; tank < this.handler.tanks(); tank++) {
                final Optional<MatterStack> stack = this.handler.stack(tank);
                if (stack.isPresent() && stack.get().material() == substance.material()) {
                    held += stack.get().amount();
                }
            }
            return held;
        }

        @Override
        public long take(final Substance substance, final long amount, final boolean simulate) {
            return substance.material() == null || amount <= 0 ? 0L
                    : this.handler.insert(new MatterStack(substance.material(), amount), simulate);
        }

        @Override
        public long give(final Substance substance, final long amount, final boolean simulate) {
            return substance.material() == null ? 0L : this.handler.extract(substance.material(), amount, simulate);
        }
    }

    /* A chemical mod's store, through its bridge: only the chemicals the bridge says are in the line's state. */
    private record ChemicalStore(IChemicalPort port, MatterState state) implements IStore {

        @Override
        public @Nullable Offer offer(final long most, final Predicate<Substance> wanted) {
            for (final ResourceLocation chemical : this.port.available()) {
                if (ChemicalBridges.stateOf(chemical).orElse(null) != this.state) {
                    continue;
                }
                final Substance substance = Substance.chemical(chemical, this.state);
                if (!wanted.test(substance)) {
                    continue;
                }
                final long amount = this.port.drain(chemical, most, true);
                if (amount > 0) {
                    return new Offer(substance, amount);
                }
            }
            return null;
        }

        @Override
        public long holding(final Substance substance) {
            return give(substance, Long.MAX_VALUE, true);
        }

        @Override
        public long take(final Substance substance, final long amount, final boolean simulate) {
            return inState(substance).map(chemical -> this.port.fill(chemical, amount, simulate)).orElse(0L);
        }

        @Override
        public long give(final Substance substance, final long amount, final boolean simulate) {
            return inState(substance).map(chemical -> this.port.drain(chemical, amount, simulate)).orElse(0L);
        }

        private Optional<ResourceLocation> inState(final Substance substance) {
            return substance.asChemical().filter(chemical -> ChemicalBridges.stateOf(chemical).orElse(null)
                    == this.state);
        }
    }

    /* A tank of a mod that keeps gases as fluids: only the fluids marked as gases. */
    private record GasFluidStore(IFluidHandler handler) implements IStore {

        @Override
        public @Nullable Offer offer(final long most, final Predicate<Substance> wanted) {
            for (int tank = 0; tank < this.handler.getTanks(); tank++) {
                final FluidStack held = this.handler.getFluidInTank(tank);
                if (held.isEmpty() || !held.is(CoreFluidTags.GASES)) {
                    continue;
                }
                final Substance substance = Substance.fluid(BuiltInRegistries.FLUID.getKey(held.getFluid()));
                if (!wanted.test(substance)) {
                    continue;
                }
                final int amount = this.handler.drain(held.copyWithAmount(clamp(most)),
                        IFluidHandler.FluidAction.SIMULATE).getAmount();
                if (amount > 0) {
                    return new Offer(substance, amount);
                }
            }
            return null;
        }

        @Override
        public long holding(final Substance substance) {
            final Fluid gas = gas(substance);
            long held = 0L;
            for (int tank = 0; tank < this.handler.getTanks(); tank++) {
                final FluidStack in = this.handler.getFluidInTank(tank);
                if (gas != Fluids.EMPTY && in.getFluid() == gas) {
                    held += in.getAmount();
                }
            }
            return held;
        }

        @Override
        public long take(final Substance substance, final long amount, final boolean simulate) {
            final Fluid gas = gas(substance);
            return gas == Fluids.EMPTY || amount <= 0 ? 0L
                    : this.handler.fill(new FluidStack(gas, clamp(amount)), action(simulate));
        }

        @Override
        public long give(final Substance substance, final long amount, final boolean simulate) {
            final Fluid gas = gas(substance);
            return gas == Fluids.EMPTY || amount <= 0 ? 0L
                    : this.handler.drain(new FluidStack(gas, clamp(amount)), action(simulate)).getAmount();
        }

        private static Fluid gas(final Substance substance) {
            return substance.asFluid().map(BuiltInRegistries.FLUID::get)
                    .filter(fluid -> fluid.defaultFluidState().is(CoreFluidTags.GASES)).orElse(Fluids.EMPTY);
        }

        private static int clamp(final long amount) {
            return (int) Math.min(Integer.MAX_VALUE, amount);
        }

        private static IFluidHandler.FluidAction action(final boolean simulate) {
            return simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        }
    }

    /* One step of a move: taking out of a store, or putting into one, undone by the opposite. */
    private static final class Step implements Batch.IStep {

        private final IStore store;
        private final Substance substance;
        private final long amount;
        private final boolean into;
        private long moved;

        private Step(final IStore store, final Substance substance, final long amount, final boolean into) {
            this.store = store;
            this.substance = substance;
            this.amount = amount;
            this.into = into;
        }

        @Override
        public long wants() {
            return this.amount;
        }

        @Override
        public long simulate() {
            return this.into ? this.store.take(this.substance, this.amount, true)
                    : this.store.give(this.substance, this.amount, true);
        }

        @Override
        public long execute() {
            this.moved = this.into ? this.store.take(this.substance, this.amount, false)
                    : this.store.give(this.substance, this.amount, false);
            return this.moved;
        }

        @Override
        public boolean undo() {
            if (this.moved == 0) {
                return true;
            }
            final long back = this.into ? this.store.give(this.substance, this.moved, false)
                    : this.store.take(this.substance, this.moved, false);
            final boolean whole = back == this.moved;
            this.moved = 0;
            return whole;
        }
    }

    /* One connected part of the grid: how much it moves in a tick, the ends it plugs into, where what it moves goes. */
    private record Part(long rate, List<MatterEnd> ends, FlowTrace trace, PartTally tally) {
    }

    /*
     * One end of a part: the block, as a store of the line's state, through the face it meets, as the end is set; the
     * wire it is the end of, and the face of that wire's block it goes into the block through.
     */
    private record MatterEnd(long key, long wire, Direction into, CableEnd end, Supplier<@Nullable IStore> store)
            implements EndRouter.IEnd<Substance> {

        @Override
        public EndMode mode() {
            return this.end.mode();
        }

        @Override
        public int priority() {
            return this.end.priority();
        }

        @Override
        public long rate() {
            return this.end.rate();
        }

        @Override
        public int channel() {
            return this.end.channel();
        }

        @Override
        public boolean allows(final Substance substance) {
            return this.end.filter().allows(new NamedSubject(substance.id().toString()));
        }

        @Override
        public long keep(final Substance substance) {
            return this.end.keep(new NamedSubject(substance.id().toString()));
        }

        @Override
        public EndRouter.@Nullable Offer<Substance> offer(final long most, final Predicate<Substance> wanted) {
            final IStore found = this.store.get();
            final Offer offer = found == null ? null : found.offer(most, wanted);
            return offer == null ? null : new EndRouter.Offer<>(offer.substance(), offer.amount());
        }

        @Override
        public boolean gives(final Substance substance) {
            final IStore found = this.store.get();
            return found != null && found.give(substance, 1L, true) > 0;
        }

        @Override
        public boolean takes(final Substance substance) {
            final IStore found = this.store.get();
            return found != null && found.take(substance, 1L, true) > 0;
        }

        @Override
        public long take(final Substance substance, final long most) {
            final IStore found = this.store.get();
            return found == null || most <= 0 ? 0L : found.take(substance, most, true);
        }

        @Override
        public long holding(final Substance substance) {
            final IStore found = this.store.get();
            return found == null ? 0L : found.holding(substance);
        }
    }
}
