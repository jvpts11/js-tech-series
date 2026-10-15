/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableEnd;
import dev.jstech.core.cable.CableParts;
import dev.jstech.core.cable.LineFlows;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.EndRouter;
import dev.jstech.core.grid.FlowTrace;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.IGridMover;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * The item grid of one dimension as it moves items along the lines that carry them: each connected part of the grid
 * with the ends its lines plug into, an inventory on each. A part moves, each tick, at most as many items as its slowest
 * line carries, shared among the ends as {@link EndRouter} says, each end by its mode, priority, rate, channel and
 * filter, whose rules may keep a number of an item in its inventory. Each move takes out and puts in as one batch, so
 * no item is lost on the way.
 */
public final class LevelItems implements IGridMover {

    private final GridKind kind;
    private final List<Part> parts = new ArrayList<>();
    private final Map<Long, Integer> partOfWire = new HashMap<>();
    private long[] lastTick = new long[0];
    private long builtVersion = -1L;
    private long builtTaps = -1L;
    private long taps;
    private long turn;
    private final LineFlows flows = new LineFlows();

    /** What the debug screen calls the time the item grid takes to move a tick's items. */
    public static final ResourceLocation TIMING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "item_grid");
    /** The work of moving a tick's items, timed, named in profilers and watched for stalls. */
    public static final Section SECTION = Section.builder(TIMING, CoreLogs.ITEMS).declare();

    /** The work that moves items along the grid of {@code kind}, one for each dimension. */
    public LevelItems(final GridKind kind) {
        this.kind = kind;
    }

    /** How many items the part of the grid the wire numbered {@code wire} is in moved in the last tick. */
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

    /* The parts worked out again when the grid changed, and the tick's items moved through each. */
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
            moved[i] = part.rate() <= 0 ? 0L : EndRouter.route(part.ends(), part.rate(), this.turn, stack -> true,
                    (from, to, item, amount) -> move(part.trace(), from, to, item, amount));
            traces.add(part.trace());
        }
        this.turn++;
        this.lastTick = moved;
        this.flows.tick(level, traces);
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
            final Set<String> seen = new HashSet<>();
            final List<ItemEnd> ends = new ArrayList<>();
            for (final CableParts.Plug plug : found.plugs()) {
                if (plug.end().mode() != EndMode.OFF && seen.add(plug.at().asLong() + "/" + plug.face())) {
                    ends.add(new ItemEnd(plug.at(), plug.wire(), plug.face().getOpposite(), plug.end(),
                            BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, plug.at(),
                                    plug.face())));
                }
            }
            this.parts.add(new Part(rate, ends, LineFlows.traceOf(level, grid, found)));
        }
    }

    /*
     * Moves {@code amount} of {@code item} from one end's inventory to the other's, all or nothing, and has
     * {@code trace} follow it.
     */
    private static long move(final FlowTrace trace, final EndRouter.IEnd<ItemStack> from,
                             final EndRouter.IEnd<ItemStack> to, final ItemStack item, final long amount) {
        final ItemEnd source = (ItemEnd) from;
        final ItemEnd target = (ItemEnd) to;
        final IItemHandler given = source.handler();
        final IItemHandler taken = target.handler();
        if (given == null || taken == null) {
            return 0L;
        }
        final int count = (int) Math.min(Integer.MAX_VALUE, amount);
        final Batch.Outcome outcome = new Batch().add(HandlerSteps.extract(given, item, count))
                .add(HandlerSteps.insert(taken, item.copyWithCount(count))).commit();
        if (!outcome.done()) {
            return 0L;
        }
        trace.moved(source.wire(), target.wire(), target.into(),
                BuiltInRegistries.ITEM.getKey(item.getItem()).toString(), count);
        return count;
    }

    /* One connected part of the grid: how many items it moves in a tick, its ends, and where what it moves goes. */
    private record Part(long rate, List<ItemEnd> ends, FlowTrace trace) {
    }

    /*
     * One end of a part: the block's inventory through the face it meets, as the end is set; the wire it is the end
     * of, and the face of that wire's block it goes into the block through.
     */
    private record ItemEnd(BlockPos at, long wire, Direction into, CableEnd end,
                           BlockCapabilityCache<IItemHandler, @Nullable Direction> cache)
            implements EndRouter.IEnd<ItemStack> {

        @Nullable
        IItemHandler handler() {
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
        public boolean allows(final ItemStack item) {
            return this.end.filter().allows(StackSubject.of(item));
        }

        @Override
        public long keep(final ItemStack item) {
            return this.end.keep(StackSubject.of(item));
        }

        @Override
        public EndRouter.@Nullable Offer<ItemStack> offer(final long most, final Predicate<ItemStack> wanted) {
            final IItemHandler handler = handler();
            if (handler == null || most <= 0) {
                return null;
            }
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                final ItemStack held = handler.getStackInSlot(slot);
                if (held.isEmpty() || !wanted.test(held)) {
                    continue;
                }
                final long available = givable(handler, held, most);
                if (available > 0) {
                    return new EndRouter.Offer<>(held.copyWithCount(1), available);
                }
            }
            return null;
        }

        @Override
        public boolean gives(final ItemStack item) {
            final IItemHandler handler = handler();
            return handler != null && givable(handler, item, 1L) > 0;
        }

        @Override
        public boolean takes(final ItemStack item) {
            return take(item, 1L) > 0;
        }

        @Override
        public long take(final ItemStack item, final long most) {
            final IItemHandler handler = handler();
            if (handler == null || most <= 0) {
                return 0L;
            }
            final int count = (int) Math.min(Integer.MAX_VALUE, most);
            return count - ItemHandlerHelper.insertItemStacked(handler, item.copyWithCount(count), true).getCount();
        }

        @Override
        public long holding(final ItemStack item) {
            final IItemHandler handler = handler();
            long held = 0L;
            if (handler != null) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    final ItemStack in = handler.getStackInSlot(slot);
                    if (ItemStack.isSameItemSameComponents(in, item)) {
                        held += in.getCount();
                    }
                }
            }
            return held;
        }

        /* How many of {@code item}, up to {@code most}, the inventory would give out, slot by slot. */
        private static long givable(final IItemHandler handler, final ItemStack item, final long most) {
            long found = 0L;
            for (int slot = 0; slot < handler.getSlots() && found < most; slot++) {
                if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), item)) {
                    found += handler.extractItem(slot, (int) Math.min(Integer.MAX_VALUE, most - found), true)
                            .getCount();
                }
            }
            return found;
        }
    }
}
