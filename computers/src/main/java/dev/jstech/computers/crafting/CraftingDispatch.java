/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.FilteredDataPort;
import dev.jstech.computers.storage.IDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * What the Mainframe does each tick for the jobs of its processing recipes, before they feed: it gives each job a
 * Crafting Interface that holds its recipe and may take it now, and it credits what came out of the machines to the
 * jobs that fed them.
 *
 * <p>An interface takes a job when it is driven by a Crafting Computer, is not paused, has room under its most jobs,
 * and its machine is not being used by another interface (two interfaces on one machine take turns). An exclusive
 * interface runs one recipe at a time: jobs of the same recipe run together, and another recipe waits until it is
 * idle and owes nothing. A job keeps the interface it has; a new one goes to the least busy that takes it.
 *
 * <p>A Receiving Bus credits the interfaces it is tied to. When they go to work it notes what its machine holds, which
 * stays where it is; after that, what rises above it is paid out in the order the jobs were fed: first to what settled
 * jobs are still owed (their late outputs, which go where those jobs' outputs go), then to the live jobs, never more
 * than each fed for. An item no job is owed, and that is not an input seen at the output, goes to the network as
 * unexpected; a fluid or chemical no pattern lists stays, as the machine's own buffer.
 */
public final class CraftingDispatch {

    private long nextSeq;
    /* Whether a settled job is still owed an output or a bus still watches its machine, as of the last tick. */
    private boolean owing;

    /*
     * How often the crafting networks are looked at while no job runs and nothing seemed owed: what a network could
     * not be seen to owe (its computer was off, or the world was just loaded) is found within this many ticks.
     */
    private static final int IDLE_SWEEP_TICKS = 20;

    /** Runs a tick for {@code jobs}: assigns interfaces, credits outputs, lets go of what settled jobs owe. */
    public void tick(final ServerLevel level, final NetworkUuid network, final List<BlockPos> computers,
                     final List<NetworkProcessingOperation> jobs) {
        if (nextSeq == 0L) {
            nextSeq = level.getGameTime() << 10;
        }
        final List<CraftingFloor> floors = floors(level, computers);
        assign(level, floors, jobs);
        credit(level, network, floors, jobs);
        boolean still = false;
        for (final CraftingFloor floor : floors) {
            for (final CraftingFloor.Site site : floor.interfaces()) {
                final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
                if (part != null) {
                    part.drainTick();
                    still |= !part.owed().isEmpty();
                }
            }
            for (final CraftingFloor.Site site : floor.receivers()) {
                final ReceivingBusPart bus = site.part(level, ReceivingBusPart.class);
                still |= bus != null && bus.watching();
            }
        }
        owing = still;
    }

    /**
     * Whether the dispatch should run at {@code gameTime} though no job is running: every tick while a settled job's
     * late outputs are still to collect or a bus has yet to forget what its machine held, and now and then otherwise.
     */
    public boolean wantsIdleTick(final long gameTime) {
        return owing || gameTime % IDLE_SWEEP_TICKS == 0L;
    }

    /** The crafting networks of {@code computers}, each once. */
    public static List<CraftingFloor> floors(final ServerLevel level, final List<BlockPos> computers) {
        final List<CraftingFloor> floors = new ArrayList<>();
        final Set<BlockPos> covered = new HashSet<>();
        for (final BlockPos pos : computers) {
            if (covered.contains(pos)
                    || !(Loaded.blockEntity(level, pos) instanceof CraftingComputerBlockEntity computer)) {
                continue;
            }
            final CraftingFloor floor = computer.floor();
            if (floor == null) {
                continue;
            }
            covered.addAll(floor.computers());
            covered.add(pos);
            floors.add(floor);
        }
        return floors;
    }

    /** Whether the interface at {@code site} runs one recipe at a time. */
    public static boolean exclusive(final CraftingInterfacePart part, final CraftingFloor.Reach reach) {
        return part.exclusive(reach.mode() == CraftingFloor.Mode.CABLE);
    }

    private void assign(final ServerLevel level, final List<CraftingFloor> floors,
                        final List<NetworkProcessingOperation> jobs) {
        final Map<CraftingFloor.Site, Slot> slots = new LinkedHashMap<>();
        for (final CraftingFloor floor : floors) {
            for (final CraftingFloor.Site site : floor.driven()) {
                final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
                if (part != null && !slots.containsKey(site)) {
                    slots.put(site, new Slot(floor, site, part, floor.reach(site), floor.drivenBy(site)));
                }
            }
        }
        final List<NetworkProcessingOperation> waiting = new ArrayList<>();
        for (final NetworkProcessingOperation job : jobs) {
            final Slot kept = job.interfaceId() == null ? null : byId(slots, job);
            if (kept != null && !kept.part.paused() && runs(level, kept, job.pattern())) {
                kept.take(job);
                job.place(kept.floor, kept.site, kept.part.id(), kept.executor, job.seq());
            } else {
                waiting.add(job);
            }
        }
        for (final NetworkProcessingOperation job : waiting) {
            Slot best = null;
            boolean anyHolds = false;
            for (final Slot slot : slots.values()) {
                if (!runs(level, slot, job.pattern())) {
                    continue;
                }
                anyHolds = true;
                if (admits(slots, slot, job.pattern()) && (best == null || slot.jobs.size() < best.jobs.size())) {
                    best = slot;
                }
            }
            if (best != null) {
                best.take(job);
                job.place(best.floor, best.site, best.part.id(), best.executor, ++nextSeq);
            } else {
                job.unplace(anyHolds || heldByAny(level, floors, job.pattern()));
            }
        }
    }

    /* Whether the interface holds the recipe and can run it: it feeds a machine, and every input has a way in. */
    private static boolean runs(final ServerLevel level, final Slot slot, final ProcessingPattern pattern) {
        final CraftingInterfacePart.HeldPattern held = slot.part.holding(pattern);
        return held != null && InterfaceRoutes.runs(level, held, pattern, slot.reach);
    }

    /* Whether any interface on the networks holds the recipe, driven or not: then the job waits for one. */
    private static boolean heldByAny(final ServerLevel level, final List<CraftingFloor> floors,
                                     final ProcessingPattern pattern) {
        for (final CraftingFloor floor : floors) {
            for (final CraftingFloor.Site site : floor.interfaces()) {
                final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
                if (part != null && part.holding(pattern) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /* Whether {@code slot} takes another job of {@code pattern} now. */
    private static boolean admits(final Map<CraftingFloor.Site, Slot> slots, final Slot slot,
                                  final ProcessingPattern pattern) {
        if (slot.part.paused() || slot.part.maxJobs() > 0 && slot.jobs.size() >= slot.part.maxJobs()) {
            return false;
        }
        for (final Slot other : slots.values()) {
            if (other != slot && slot.reach.machine() != null && slot.reach.machine().equals(other.reach.machine())
                    && (!other.jobs.isEmpty() || !other.part.owed().isEmpty())) {
                return false; // two interfaces on one machine take turns
            }
        }
        if (!exclusive(slot.part, slot.reach)) {
            return true;
        }
        final String recipe = pattern.identity();
        for (final NetworkProcessingOperation running : slot.jobs) {
            if (!running.pattern().identity().equals(recipe)) {
                return false;
            }
        }
        for (final CraftingInterfacePart.Owed owed : slot.part.owed()) {
            if (!owed.recipe().equals(recipe)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Slot byId(final Map<CraftingFloor.Site, Slot> slots, final NetworkProcessingOperation job) {
        for (final Slot slot : slots.values()) {
            if (slot.part.id().equals(job.interfaceId())) {
                return slot;
            }
        }
        return null;
    }

    private void credit(final ServerLevel level, final NetworkUuid network, final List<CraftingFloor> floors,
                        final List<NetworkProcessingOperation> jobs) {
        final ICraftIo toNetwork = ICraftIo.network(level, network);
        final long now = level.getGameTime();
        for (final CraftingFloor floor : floors) {
            for (final CraftingFloor.Site busSite : floor.receivers()) {
                final ReceivingBusPart bus = busSite.part(level, ReceivingBusPart.class);
                if (bus == null) {
                    continue;
                }
                final List<CraftingFloor.Site> tied = floor.tiedTo(busSite);
                final List<NetworkProcessingOperation> live = new ArrayList<>();
                for (final NetworkProcessingOperation job : jobs) {
                    if (!job.isDone() && job.site() != null && tied.contains(job.site())) {
                        live.add(job);
                    }
                }
                live.sort(Comparator.comparingLong(NetworkProcessingOperation::seq));
                final List<CraftingInterfacePart> parts = new ArrayList<>();
                boolean owes = false;
                for (final CraftingFloor.Site site : tied) {
                    final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
                    if (part != null) {
                        parts.add(part);
                        owes |= !part.owed().isEmpty();
                    }
                }
                if (live.isEmpty() && !owes) {
                    bus.stopWatching();
                    continue;
                }
                final ExternalDataPort face = ExternalDataPort.at(level, busSite.faced(),
                        busSite.face().getOpposite());
                if (face.isEmpty()) {
                    continue;
                }
                final IDataPort port = new FilteredDataPort(face, bus.filterKeys());
                if (!bus.watching()) {
                    bus.startWatching(snapshot(port));
                    continue;
                }
                for (final StorageKey key : port.available()) {
                    creditKey(level, now, bus, port, key, live, parts, toNetwork);
                }
            }
        }
    }

    /* Pays out what rose of {@code key} at the bus: owed first, then the live jobs, the rest unexpected. */
    private static void creditKey(final ServerLevel level, final long now, final ReceivingBusPart bus,
                                  final IDataPort port, final StorageKey key,
                                  final List<NetworkProcessingOperation> live, final List<CraftingInterfacePart> parts,
                                  final ICraftIo toNetwork) {
        long above = port.count(key) - bus.baseline().getOrDefault(key, 0L);
        if (above <= 0L) {
            return;
        }
        final List<Account> accounts = new ArrayList<>();
        for (final CraftingInterfacePart part : parts) {
            for (final CraftingInterfacePart.Owed owed : part.owed()) {
                if (owed.key().equals(key) && owed.remaining() > 0L) {
                    accounts.add(new Account(owed.seq(), part, null, -1, owed));
                }
            }
        }
        for (final NetworkProcessingOperation job : live) {
            final List<ProcessingPattern.ProcessingOutput> outputs = job.pattern().outputs();
            for (int i = 0; i < outputs.size(); i++) {
                if (outputs.get(i).key().equals(key) && job.cap(i) > job.credited(i)) {
                    accounts.add(new Account(job.seq(), partOf(level, job, parts), job, i, null));
                }
            }
        }
        accounts.sort(Comparator.comparingLong(Account::seq));
        for (final Account account : accounts) {
            if (above <= 0L) {
                break;
            }
            final ICraftIo sink = account.job != null ? account.job.io()
                    : account.owed.sink() != null ? account.owed.sink() : toNetwork;
            final long owedNow = account.job != null ? account.job.cap(account.output) - account.job.credited(
                    account.output) : account.owed.remaining();
            final long moved = move(port, key, Math.min(above, owedNow), sink);
            if (moved <= 0L) {
                continue;
            }
            above -= moved;
            bus.worked(now);
            final String name = account.part == null ? "" : account.part.name();
            if (account.job != null) {
                account.job.credit(account.output, moved);
                bus.log().add(new CraftingLog.Entry(now, key.id(), moved, 0L, CraftingLog.CREDITED, name));
            } else {
                account.owed.paid(moved, CraftingInterfacePart.QUIET_TICKS);
                bus.log().add(new CraftingLog.Entry(now, key.id(), moved, 0L, CraftingLog.LATE, name));
            }
            if (account.part != null) {
                account.part.worked(now);
            }
        }
        if (above <= 0L) {
            return;
        }
        for (final NetworkProcessingOperation job : live) {
            if (job.consumes(key)) {
                return; // an input seen from the output side, still the machine's to use
            }
        }
        if (!key.isItem()) {
            /*
             * A tank no pattern lists is most often the machine's own buffer, seen from every side: the infusion an
             * infuser turned its redstone into, a generator's fuel. Taking it would starve the machine, so it stays.
             */
            return;
        }
        final long moved = move(port, key, above, toNetwork);
        if (moved > 0L) {
            bus.worked(now);
            final CraftingInterfacePart where = live.isEmpty() ? (parts.isEmpty() ? null : parts.get(0))
                    : partOf(level, live.get(live.size() - 1), parts);
            final String name = where == null ? "" : where.name();
            bus.log().add(new CraftingLog.Entry(now, key.id(), moved, 0L, CraftingLog.UNEXPECTED, name));
            if (where != null) {
                where.log().add(new CraftingLog.Entry(now, key.id(), moved, 0L, CraftingLog.UNEXPECTED, ""));
            }
        }
    }

    @Nullable
    private static CraftingInterfacePart partOf(final ServerLevel level, final NetworkProcessingOperation job,
                                                final List<CraftingInterfacePart> parts) {
        final CraftingInterfacePart part = job.site() == null ? null
                : job.site().part(level, CraftingInterfacePart.class);
        return part != null ? part : parts.isEmpty() ? null : parts.get(0);
    }

    /* What the bus sees now, key by key. */
    private static Map<StorageKey, Long> snapshot(final IDataPort port) {
        final Map<StorageKey, Long> seen = new LinkedHashMap<>();
        for (final StorageKey key : port.available()) {
            final long count = port.count(key);
            if (count > 0L) {
                seen.put(key, count);
            }
        }
        return seen;
    }

    /*
     * Moves up to {@code amount} of {@code key} out of the machine into {@code sink}: only what the sink takes leaves
     * the machine, so a full network leaves the rest where it was made.
     */
    private static long move(final IDataPort port, final StorageKey key, final long amount, final ICraftIo sink) {
        if (amount <= 0L) {
            return 0L;
        }
        final long offered = port.extract(key, amount, true);
        final long stored = offered > 0L ? sink.insert(key, offered) : 0L;
        if (stored <= 0L) {
            return 0L;
        }
        final long pulled = port.extract(key, stored, false);
        if (pulled < stored) {
            // The machine gave less than it offered a moment ago: what it kept must not be counted twice.
            sink.select(key, stored - pulled, (k, a, simulate) -> a);
        }
        return pulled;
    }

    /* One interface this tick, and the jobs given to it so far. */
    private static final class Slot {

        private final CraftingFloor floor;
        private final CraftingFloor.Site site;
        private final CraftingInterfacePart part;
        private final CraftingFloor.Reach reach;
        @Nullable
        private final BlockPos executor;
        private final List<NetworkProcessingOperation> jobs = new ArrayList<>();

        private Slot(final CraftingFloor floor, final CraftingFloor.Site site, final CraftingInterfacePart part,
                     final CraftingFloor.Reach reach, @Nullable final BlockPos executor) {
            this.floor = floor;
            this.site = site;
            this.part = part;
            this.reach = reach;
            this.executor = executor;
        }

        private void take(final NetworkProcessingOperation job) {
            jobs.add(job);
        }
    }

    /* Who is owed an output, in feed order: a live job's output, or what a settled job is still owed. */
    private record Account(long seq, @Nullable CraftingInterfacePart part, @Nullable NetworkProcessingOperation job,
                           int output, @Nullable CraftingInterfacePart.Owed owed) {
    }
}
