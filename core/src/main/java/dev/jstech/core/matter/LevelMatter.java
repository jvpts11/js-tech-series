/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableParts;
import dev.jstech.core.chemical.ChemicalBridges;
import dev.jstech.core.chemical.IChemicalPort;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.energy.internal.ProportionalSplitter;
import dev.jstech.core.fluid.CoreFluidTags;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.IGridMover;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.transfer.Batch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
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
 * <p>The rounds are the fluid grid's: what gives and would not take it back (a machine's output) feeds what takes and
 * gives nothing of it (a machine's input); what is left fills what both takes and gives it (a tank); and a tank feeds
 * what the inputs still want. Shares follow what is wanted, and each move takes out and puts in as one batch, so
 * nothing is lost on the way. A material linked to a chemical or a fluid ({@link MatterBridges}) passes between them
 * as the same thing; one with no link moves only between stores of its own kind.
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
        for (int i = 0; i < moved.length; i++) {
            moved[i] = this.parts.get(i).tick();
        }
        this.lastTick = moved;
    }

    private void build(final ServerLevel level, final Grid grid) {
        this.parts.clear();
        this.partOfWire.clear();
        for (final CableParts.Part found : CableParts.of(level, grid)) {
            long rate = Long.MAX_VALUE;
            for (final CableParts.WireAt wire : found.wires()) {
                rate = Math.min(rate, wire.runTooLong() ? 0L : wire.type().throughput());
                this.partOfWire.put(wire.number(), this.parts.size());
            }
            final Set<Long> seen = new HashSet<>();
            final List<Supplier<@Nullable IStore>> ends = new ArrayList<>();
            for (final CableParts.Plug plug : found.plugs()) {
                if (seen.add(plug.at().asLong())) {
                    ends.add(end(level, plug.at(), plug.face()));
                }
            }
            this.parts.add(new Part(rate, ends));
        }
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

        /* What it would give first, up to {@code most}; null for nothing. */
        @Nullable
        Offer offer(long most);

        /* Puts in up to {@code amount}; how much it took, or would. */
        long take(Substance substance, long amount, boolean simulate);

        /* Takes out up to {@code amount}; how much came out, or would. */
        long give(Substance substance, long amount, boolean simulate);
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
        public @Nullable Offer offer(final long most) {
            return this.handler.extractAny(most, true).map(stack -> new Offer(Substance.of(stack.material()),
                    stack.amount())).orElse(null);
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
        public @Nullable Offer offer(final long most) {
            for (final ResourceLocation chemical : this.port.available()) {
                if (ChemicalBridges.stateOf(chemical).orElse(null) != this.state) {
                    continue;
                }
                final long amount = this.port.drain(chemical, most, true);
                if (amount > 0) {
                    return new Offer(Substance.chemical(chemical, this.state), amount);
                }
            }
            return null;
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
        public @Nullable Offer offer(final long most) {
            for (int tank = 0; tank < this.handler.getTanks(); tank++) {
                final FluidStack held = this.handler.getFluidInTank(tank);
                if (held.isEmpty() || !held.is(CoreFluidTags.GASES)) {
                    continue;
                }
                final int amount = this.handler.drain(held.copyWithAmount(clamp(most)),
                        IFluidHandler.FluidAction.SIMULATE).getAmount();
                if (amount > 0) {
                    return new Offer(Substance.fluid(BuiltInRegistries.FLUID.getKey(held.getFluid())), amount);
                }
            }
            return null;
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

    /* One connected part of the grid: how much it moves in a tick, and the blocks it meets. */
    private static final class Part {

        private final long rate;
        private final List<Supplier<@Nullable IStore>> ends;

        private Part(final long rate, final List<Supplier<@Nullable IStore>> ends) {
            this.rate = rate;
            this.ends = ends;
        }

        /* Moves this tick's matter, and says how many millibuckets moved. */
        private long tick() {
            if (this.rate <= 0 || this.ends.size() < 2) {
                return 0L;
            }
            final List<IStore> stores = new ArrayList<>(this.ends.size());
            for (final Supplier<@Nullable IStore> end : this.ends) {
                final IStore store = end.get();
                if (store != null) {
                    stores.add(store);
                }
            }
            final long[] budget = {this.rate};
            for (final IStore from : stores) {
                give(from, stores, false, false, budget);
            }
            for (final IStore from : stores) {
                give(from, stores, false, true, budget);
            }
            for (final IStore from : stores) {
                give(from, stores, true, false, budget);
            }
            return this.rate - budget[0];
        }

        /*
         * Gives what {@code from} offers to the stores of the round that take it: from one that would not take it back
         * (or, in the last round, from one that would) to ones that would not give it (or, in the second round, to
         * ones that would).
         */
        private static void give(final IStore from, final List<IStore> stores, final boolean fromStore,
                                 final boolean toStores, final long[] budget) {
            if (budget[0] <= 0) {
                return;
            }
            final Offer offer = from.offer(budget[0]);
            if (offer == null || takes(from, offer.substance()) != fromStore) {
                return;
            }
            final Map<Long, Long> wants = new LinkedHashMap<>();
            for (int i = 0; i < stores.size(); i++) {
                final IStore to = stores.get(i);
                if (to == from || !takes(to, offer.substance()) || gives(to, offer.substance()) != toStores) {
                    continue;
                }
                final long want = to.take(offer.substance(), offer.amount(), true);
                if (want > 0) {
                    wants.put((long) i, want);
                }
            }
            if (wants.isEmpty()) {
                return;
            }
            final Map<Long, Long> shares = ProportionalSplitter.split(offer.amount(), wants, Map.of());
            for (final Long index : wants.keySet()) {
                final long amount = Math.min(budget[0], shares.getOrDefault(index, 0L));
                if (amount <= 0) {
                    continue;
                }
                final Batch.Outcome outcome = new Batch()
                        .add(new Step(from, offer.substance(), amount, false))
                        .add(new Step(stores.get(index.intValue()), offer.substance(), amount, true))
                        .commit();
                if (outcome.done()) {
                    budget[0] -= amount;
                }
            }
        }

        private static boolean takes(final IStore store, final Substance substance) {
            return store.take(substance, 1L, true) > 0;
        }

        private static boolean gives(final IStore store, final Substance substance) {
            return store.give(substance, 1L, true) > 0;
        }
    }
}
