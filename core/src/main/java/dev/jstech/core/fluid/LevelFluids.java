/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableEnd;
import dev.jstech.core.cable.CableParts;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.LineBreach;
import dev.jstech.core.cable.LineFlows;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.EndRouter;
import dev.jstech.core.grid.FlowTrace;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.grid.GridPlaces;
import dev.jstech.core.grid.IGridMover;
import dev.jstech.core.inventory.FluidSubject;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.transfer.Batch;
import dev.jstech.core.transfer.HandlerSteps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid grid of one dimension as it moves fluids: each connected part of the grid with the ends its pipes plug
 * into. Pipes are passive: a part moves, each tick, at most what its slowest pipe carries (the pressure it holds), and
 * only the fluids every pipe of it is made for, which the rest leave where they are.
 *
 * <p>The ends share what flows as {@link EndRouter} says: a machine's output feeds the inputs, then fills the tanks,
 * and the tanks feed what the inputs still want; each end by its mode, priority, rate, channel and filter. Each move
 * takes out and puts in as one batch, so nothing is lost on the way. Two tanks alone never pour into each other unless
 * an end pulls from one. The parts are worked out again only when the grid's shape changes, a pipe comes to meet
 * another block, or an end is set.
 */
public final class LevelFluids implements IGridMover {

    private final GridKind kind;
    private final List<Part> parts = new ArrayList<>();
    private final Map<Long, Integer> partOfWire = new HashMap<>();
    private long[] lastTick = new long[0];
    private long builtVersion = -1L;
    private long builtTaps = -1L;
    private long taps;
    private long turn;
    private final LineFlows flows = new LineFlows();

    /** What the debug screen calls the time the fluid grid takes to move a tick's fluids. */
    public static final ResourceLocation TIMING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "fluid_grid");
    /** The work of moving a tick's fluids, timed, named in profilers and watched for stalls. */
    public static final Section SECTION = Section.builder(TIMING, CoreLogs.FLUIDS).declare();

    /** The work that moves fluids along the grid of {@code kind}, one for each dimension. */
    public LevelFluids(final GridKind kind) {
        this.kind = kind;
    }

    /** How many millibuckets the part of the grid the wire numbered {@code wire} is in moved in the last tick. */
    public OptionalLong movedLastTickOf(final long wire) {
        final Integer part = this.partOfWire.get(wire);
        return part == null || part >= this.lastTick.length ? OptionalLong.empty()
                : OptionalLong.of(this.lastTick[part]);
    }

    /** A pipe came to meet a block, stopped meeting one, or an end was set: worked out again before the next tick. */
    @Override
    public void tapsChanged() {
        this.taps++;
    }

    /** Moves the tick's fluids through every part of the grid of {@code level}. */
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

    /** How many connected parts the grid is in. */
    public int partCount() {
        return this.parts.size();
    }

    /* The parts worked out again when the grid changed, and the tick's fluids moved through each. */
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
            moved[i] = part.rate <= 0 ? 0L : EndRouter.route(part.ends, part.rate, this.turn,
                    part.carries, (from, to, fluid, amount) -> move(part.trace, from, to, fluid, amount));
            if (!part.breakable.isEmpty()) {
                reach(level, part);
            }
            traces.add(part.trace);
        }
        this.turn++;
        this.lastTick = moved;
        this.flows.tick(level, traces);
    }

    /*
     * Tells each pipe of {@code part} that breaks what reaches it that it is not made for: what an end offers that the
     * part does not carry reaches every pipe of it that cannot take it.
     */
    private static void reach(final ServerLevel level, final Part part) {
        for (final FluidEnd end : part.ends) {
            if (!end.mode().draws()) {
                continue;
            }
            final EndRouter.Offer<FluidStack> offer = end.offer(Math.max(1L, part.rate), fluid -> true);
            if (offer == null || part.carries.test(offer.resource())) {
                continue;
            }
            final FluidStack fluid = offer.resource();
            final String what = BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
            for (final Breakable pipe : part.breakable) {
                pipe.type().pipe().beyond(fluid.getFluid()).ifPresent(reason -> pipe.type().breach().exposed(level,
                        new LineBreach(pipe.at(), pipe.type(), reason, what), level.getGameTime()));
            }
        }
    }

    private void build(final ServerLevel level, final Grid grid) {
        this.parts.clear();
        this.partOfWire.clear();
        final GridPlaces places = CoreGrids.places(level);
        for (final CableParts.Part found : CableParts.of(level, grid)) {
            long rate = Long.MAX_VALUE;
            PipeLimits limits = null;
            final List<Breakable> breakable = new ArrayList<>();
            for (final CableParts.WireAt wire : found.wires()) {
                rate = Math.min(rate, wire.runTooLong() ? 0L : wire.type().throughput());
                limits = limits == null ? wire.type().pipe() : limits.and(wire.type().pipe());
                this.partOfWire.put(wire.number(), this.parts.size());
                final GridPlace place = places.place(wire.number());
                if (wire.type().breaks() && place != null) {
                    breakable.add(new Breakable(BlockPos.of(place.pos()), wire.type()));
                }
            }
            if (limits == null) {
                continue;
            }
            final Set<String> seen = new HashSet<>();
            final List<FluidEnd> ends = new ArrayList<>();
            for (final CableParts.Plug plug : found.plugs()) {
                if (plug.end().mode() != EndMode.OFF && seen.add(plug.at().asLong() + "/" + plug.face())) {
                    ends.add(new FluidEnd(plug.at(), plug.wire(), plug.face().getOpposite(), plug.end(),
                            BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level, plug.at(),
                                    plug.face())));
                }
            }
            final PipeLimits carried = limits;
            this.parts.add(new Part(rate, fluid -> carried.carries(fluid.getFluid()), ends, breakable,
                    LineFlows.traceOf(level, grid, found)));
        }
    }

    /*
     * Moves {@code amount} of {@code fluid} from one end's block to the other's, all or nothing, and has
     * {@code trace} follow it.
     */
    private static long move(final FlowTrace trace, final EndRouter.IEnd<FluidStack> from,
                             final EndRouter.IEnd<FluidStack> to, final FluidStack fluid, final long amount) {
        final FluidEnd source = (FluidEnd) from;
        final FluidEnd target = (FluidEnd) to;
        final IFluidHandler drained = source.handler();
        final IFluidHandler filled = target.handler();
        if (drained == null || filled == null) {
            return 0L;
        }
        final FluidStack moving = fluid.copyWithAmount((int) Math.min(Integer.MAX_VALUE, amount));
        final Batch.Outcome outcome = new Batch().add(HandlerSteps.drain(drained, moving))
                .add(HandlerSteps.fill(filled, moving)).commit();
        if (!outcome.done()) {
            return 0L;
        }
        trace.moved(source.wire(), target.wire(), target.into(),
                BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString(), moving.getAmount());
        return moving.getAmount();
    }

    /*
     * One connected part of the grid: how much it moves in a tick, which fluids its pipes carry, the ends it plugs
     * into, its pipes that break when something reaches them they are not made for, and where what it moves goes.
     */
    private record Part(long rate, Predicate<FluidStack> carries, List<FluidEnd> ends, List<Breakable> breakable,
                        FlowTrace trace) {
    }

    /* A pipe that breaks when something reaches it that it is not made for: where, and of which cable. */
    private record Breakable(BlockPos at, CableType type) {
    }

    /*
     * One end of a part: the block's fluids through the face it meets, as the end is set; the wire it is the end of,
     * and the face of that wire's block it goes into the block through.
     */
    private record FluidEnd(BlockPos at, long wire, Direction into, CableEnd end,
                            BlockCapabilityCache<IFluidHandler, @Nullable Direction> cache)
            implements EndRouter.IEnd<FluidStack> {

        @Nullable
        IFluidHandler handler() {
            return this.cache.getCapability();
        }

        @Override
        public long key() {
            return this.at.asLong();
        }

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
        public boolean allows(final FluidStack fluid) {
            return this.end.filter().allows(FluidSubject.of(fluid));
        }

        @Override
        public long keep(final FluidStack fluid) {
            return this.end.keep(FluidSubject.of(fluid));
        }

        @Override
        public EndRouter.@Nullable Offer<FluidStack> offer(final long most, final Predicate<FluidStack> wanted) {
            final IFluidHandler handler = handler();
            if (handler == null || most <= 0) {
                return null;
            }
            final int asked = (int) Math.min(Integer.MAX_VALUE, most);
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                final FluidStack held = handler.getFluidInTank(tank);
                if (held.isEmpty() || !wanted.test(held)) {
                    continue;
                }
                final FluidStack given = handler.drain(held.copyWithAmount(asked), IFluidHandler.FluidAction.SIMULATE);
                if (!given.isEmpty()) {
                    return new EndRouter.Offer<>(given.copyWithAmount(1), given.getAmount());
                }
            }
            return null;
        }

        @Override
        public boolean gives(final FluidStack fluid) {
            final IFluidHandler handler = handler();
            return handler != null && !handler.drain(fluid.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE)
                    .isEmpty();
        }

        @Override
        public boolean takes(final FluidStack fluid) {
            final IFluidHandler handler = handler();
            return handler != null && handler.fill(fluid.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) > 0;
        }

        @Override
        public long take(final FluidStack fluid, final long most) {
            final IFluidHandler handler = handler();
            return handler == null || most <= 0 ? 0L : handler.fill(fluid.copyWithAmount((int) Math.min(
                    Integer.MAX_VALUE, most)), IFluidHandler.FluidAction.SIMULATE);
        }

        @Override
        public long holding(final FluidStack fluid) {
            final IFluidHandler handler = handler();
            long held = 0L;
            if (handler != null) {
                for (int tank = 0; tank < handler.getTanks(); tank++) {
                    final FluidStack in = handler.getFluidInTank(tank);
                    if (FluidStack.isSameFluidSameComponents(in, fluid)) {
                        held += in.getAmount();
                    }
                }
            }
            return held;
        }
    }
}
