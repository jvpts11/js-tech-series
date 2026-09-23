/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.advancement.OperationMilestones;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.core.JsCore;
import dev.jstech.core.event.IOperationLifecycleEvent;
import dev.jstech.core.operation.OperationBalance;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.operation.OperationStatistics;
import dev.jstech.core.operation.exec.QueueArbiter;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * What the Mainframe is running, and which of it gets to run this tick.
 *
 * <p>Every Operation the network is carrying out lives here from the moment it is taken on to the moment it
 * settles. Each tick this hands out the queue slots the hardware has, feeds the ones that got a slot, ages
 * the ones that did not so a long queue of urgent work can delay a patient request but never starve it, and
 * writes down what settled. The rule the whole thing turns on is that the slots are decided afresh every
 * tick: a request that arrives urgent takes a slot on the next tick rather than waiting for whatever is
 * streaming to finish.
 *
 * <p>It counts the ticks each Operation spent waiting and running, which is what a terminal reads as how long
 * something took, and it tells the rest of the series what became of each one.
 *
 * <p>This is the Mainframe's, and only the Mainframe's: it reads the machine's pooled capacity, its buffer and
 * its queue count from it, and hands finished Operations back to it for the log. It is a piece of that machine
 * rather than a thing of its own, which is why it takes the machine rather than a set of numbers.
 */
final class MainframeScheduler {

    /** The two things counted per Operation: the ticks it waited, and the ticks it actually ran. */
    private static final int WAITED = 0;
    private static final int RAN = 1;

    private final MainframeBlockEntity mainframe;

    /** What the network is carrying out right now, in the order it was taken on. */
    private final List<INetworkOperation> inFlight = new ArrayList<>();

    /** Work an Operation's settle asked for that has to wait a tick, until the index has caught up. */
    private final List<Runnable> deferredWork = new ArrayList<>();

    /**
     * Ticks a ready Operation has spent without a queue slot, per Operation. Every aging period of deferral
     * lifts its effective priority one level, so a long queue of higher-priority work delays a low-priority
     * request but can never starve it outright. The count only grows while the Operation is passed over and is
     * dropped when it settles, so a starved Operation that finally wins a slot keeps it instead of falling
     * straight back behind the newcomers.
     */
    private final Map<INetworkOperation, Integer> deferredTicks = new IdentityHashMap<>();

    /** Per in-flight Operation: ticks spent waiting (queued or on a lock) and ticks spent running. */
    private final Map<INetworkOperation, int[]> timing = new IdentityHashMap<>();

    /* Who asked for each Operation, when somebody was acting as it was taken on; the advancements credit them. */
    private final Map<INetworkOperation, UUID> askedBy = new IdentityHashMap<>();

    /** The last hour of settled Operations by type, and the day's peak concurrency; RAM only. */
    private final OperationStatistics statistics = new OperationStatistics();

    /** The Operations the last tick granted a queue slot to; the views report the rest as PENDING. */
    private Set<INetworkOperation> lastGranted = Set.of();

    /** How many have finished here since the world was made, which the save carries. */
    private long completedTotal;

    MainframeScheduler(final MainframeBlockEntity mainframe) {
        this.mainframe = mainframe;
    }

    /**
     * Takes an Operation on and announces it on the series' event bus.
     *
     * <p>Every submission and every resume goes through here, so the bus sees each Operation exactly once.
     */
    void track(final INetworkOperation operation) {
        inFlight.add(operation);
        Acting.current().ifPresent(player -> askedBy.put(operation, player));
        post(net -> new IOperationLifecycleEvent.Created(
                net, operation.operationId(), operation.typeId()));
    }

    /** Runs {@code work} on the next tick, after the storage index has caught up with this one. */
    void runNextTick(final Runnable work) {
        deferredWork.add(work);
    }

    /** The rolling statistics of this Mainframe's Operations. */
    OperationStatistics statistics() {
        return statistics;
    }

    /** How many Operations this Mainframe has finished, over its whole life. */
    long completedTotal() {
        return completedTotal;
    }

    /** Sets the lifetime tally, which is what reading a save back does. */
    void completedTotal(final long total) {
        this.completedTotal = total;
    }

    /** The Operations in flight right now, for views that need the live objects rather than the log. */
    List<INetworkOperation> live() {
        return List.copyOf(inFlight);
    }

    /** How many are in flight, which is what the concurrency reading and the resume counting want. */
    int liveCount() {
        return inFlight.size();
    }

    /** The one taken on at that position, for code that has to reach the ones it just submitted. */
    INetworkOperation liveAt(final int index) {
        return inFlight.get(index);
    }

    boolean hasActive() {
        return !inFlight.isEmpty();
    }

    /**
     * How many are waiting rather than moving: blocked on somebody else's hold, or past the last queue slot.
     *
     * <p>A machine stage is neither: it runs under its Crafting Computer's threads and never takes a queue
     * slot at all, so it is left out of both this and the running count.
     */
    int queuedCount(final int slots) {
        int used = 0;
        int queued = 0;
        for (final INetworkOperation operation : inFlight) {
            if (operation.isDone()) {
                continue;
            }
            if (operation.isWaiting()) {
                queued++; // blocked on another Operation's hold, not streaming
            } else if (!occupiesQueue(operation)) {
                continue;
            } else if (used < slots) {
                used++;
            } else {
                queued++;
            }
        }
        return queued;
    }

    /** How many are moving right now, which is at most the number of queue slots there are. */
    int runningCount(final int slots) {
        int used = 0;
        for (final INetworkOperation operation : inFlight) {
            if (!operation.isDone() && !operation.isWaiting() && occupiesQueue(operation) && used < slots) {
                used++;
            }
        }
        return used;
    }

    /** Whether any Operation of a kind is in flight, asked with a test rather than by handing the list out. */
    boolean anyLive(final Predicate<INetworkOperation> test) {
        for (final INetworkOperation operation : inFlight) {
            if (test.test(operation)) {
                return true;
            }
        }
        return false;
    }

    /** The in-flight Operation with this id, or null when it has settled or never existed. */
    @Nullable
    INetworkOperation find(final UUID id) {
        for (final INetworkOperation operation : inFlight) {
            if (id.equals(operation.operationId())) {
                return operation;
            }
        }
        return null;
    }

    /**
     * Changes the scheduling level of an in-flight Operation; the next tick re-grants the queue slots with the
     * new level. Returns false when no Operation with that id is in flight any more.
     */
    boolean setPriority(final UUID id, final OperationPriority priority) {
        final INetworkOperation operation = find(id);
        if (operation == null || operation.isDone()) {
            return false;
        }
        operation.setPriority(priority);
        mainframe.setChanged();
        return true;
    }

    /** What every in-flight Operation looks like right now, as the views read it. */
    List<OperationRecord> liveRecords() {
        final List<OperationRecord> out = new ArrayList<>(inFlight.size());
        for (final INetworkOperation operation : inFlight) {
            final int[] counted = timingOf(operation);
            OperationRecord record = operation.liveRecord().withTiming(counted[WAITED], counted[RAN]);
            /*
             * A ready Operation the last tick did not grant a slot to is queued: the scheduler decides, the
             * view only reports it. A machine stage keeps its live status (PROCESSING while it runs): it is
             * gated by its Crafting Computer's threads, not a Mainframe queue, so it is never forced to PENDING.
             */
            if (!operation.isDone() && !operation.isWaiting() && occupiesQueue(operation)
                    && !lastGranted.contains(operation)) {
                record = record.withStatus(OperationRecord.STATUS_PENDING);
            }
            out.add(record);
        }
        return out;
    }

    /**
     * Drops everything in flight, which is what a Mainframe losing power or its network does.
     *
     * <p>Each one is told to give back what it was holding first; the Operations that keep a record of
     * themselves are settled by the caller before this, so nothing is lost by the forgetting.
     */
    void abandonAll() {
        for (final INetworkOperation operation : inFlight) {
            operation.abandon();
        }
        clear();
    }

    /** Says an Operation was given up on, for the caller that settled one outside the tick. */
    void postDiscarded(final INetworkOperation operation) {
        post(net -> new IOperationLifecycleEvent.Discarded(
                net, operation.operationId(), operation.typeId()));
    }

    /** Forgets everything without telling any of them, for the caller that has already dealt with them. */
    void clear() {
        inFlight.clear();
        timing.clear();
        deferredTicks.clear();
        askedBy.clear();
        lastGranted = Set.of();
    }

    /**
     * One tick of the whole thing: the deferred work, the slots, the feeding, and what settled.
     */
    void tick() {
        if (!deferredWork.isEmpty()) {
            final List<Runnable> work = new ArrayList<>(deferredWork);
            deferredWork.clear();
            work.forEach(Runnable::run);
        }
        if (inFlight.isEmpty()) {
            lastGranted = Set.of();
            deferredTicks.clear();
            timing.clear();
            return;
        }
        // Progress lives in the Operations themselves and is saved with the block entity.
        mainframe.setChanged();
        statistics.observeConcurrency(mainframe.gameTime(), inFlight.size());
        /*
         * A queue processes at most the RAM buffer per tick: a buffer smaller than the CPU leaves the CPU idle
         * waiting on RAM, so the effective rate is the lesser of the two. Subframes pool their share of
         * capacity and their GPUs' queues into the Mainframe that orchestrates them.
         */
        final long effectiveCapacity = Math.min(mainframe.pooledCapacity(), mainframe.ramBuffer());
        final int slots = Math.max(1, mainframe.pooledQueues());
        assignMachines();
        feed(effectiveCapacity, slots);
        settle();
    }

    /*
     * A machine step feeds at its Crafting Computer's crafting-card throughput (card x CPU), NOT the
     * Mainframe's capacity; the card is what governs how fast any craft runs, bench or machine. That
     * throughput is SHARED among the steps one computer is driving at once, so a computer feeding three
     * machines splits its card's throughput three ways (the machine's own speed is still the ceiling).
     */
    private void feed(final long effectiveCapacity, final int slots) {
        final Map<BlockPos, Integer> stepsPerComputer = new HashMap<>();
        for (final INetworkOperation operation : inFlight) {
            if (operation instanceof NetworkProcessingOperation proc
                    && !proc.isDone() && !proc.isWaiting()) {
                final BlockPos cc = proc.executorComputer();
                if (cc != null) {
                    stepsPerComputer.merge(cc, 1, Integer::sum);
                }
            }
        }
        /*
         * Iterate a snapshot: a multi-stage operation submits its sub-stage into the list mid-tick, which
         * would otherwise be a concurrent modification. The new stage simply ticks next tick.
         */
        final List<INetworkOperation> snapshot = new ArrayList<>(inFlight);
        /*
         * The queue slots go to the ready Operations by effective priority (level plus aging), submission
         * order inside a level. Re-deciding every tick means a higher-priority request takes over a slot the
         * next tick instead of waiting for whatever was streaming to finish.
         */
        final List<QueueArbiter.Candidate<INetworkOperation>> ready = new ArrayList<>();
        for (final INetworkOperation operation : snapshot) {
            if (!operation.isDone() && !operation.isWaiting() && occupiesQueue(operation)) {
                ready.add(new QueueArbiter.Candidate<>(
                        operation, operation.priority(), deferredTicks.getOrDefault(operation, 0)));
            }
        }
        final Set<INetworkOperation> granted = Collections.newSetFromMap(new IdentityHashMap<>());
        granted.addAll(QueueArbiter.grant(ready, slots, OperationBalance.priorityAgingTicks()));
        lastGranted = granted;
        for (final INetworkOperation operation : snapshot) {
            if (operation.isDone()) {
                continue;
            }
            if (operation.isWaiting()) {
                countTick(operation, WAITED);
                operation.tick(0L); // lock retry + timeout only; holds no queue slot
            } else if (!occupiesQueue(operation)) {
                /*
                 * A craft's machine stage runs under its Crafting Computer's thread ceiling, not a Mainframe
                 * queue: it always gets its feed and never counts against the queue budget.
                 */
                countTick(operation, RAN);
                operation.tick(machineFeedBudget(operation, effectiveCapacity, stepsPerComputer));
            } else if (granted.contains(operation)) {
                countTick(operation, RAN);
                operation.tick(machineFeedBudget(operation, effectiveCapacity, stepsPerComputer));
            } else {
                /*
                 * Ready Operations beyond the queue count stay PENDING this tick: no progress, no latency
                 * countdown, since their disks have not started reading yet. Their wait is what ages them.
                 */
                countTick(operation, WAITED);
                deferredTicks.merge(operation, 1, Integer::sum);
            }
        }
    }

    /** Writes down and lets go of everything that finished this tick. */
    private void settle() {
        final Iterator<INetworkOperation> it = inFlight.iterator();
        while (it.hasNext()) {
            final INetworkOperation operation = it.next();
            if (!operation.isDone()) {
                continue;
            }
            deferredTicks.remove(operation);
            final UUID asker = askedBy.remove(operation);
            final int[] counted = timing.remove(operation);
            final int waited = counted == null ? 0 : counted[WAITED];
            final int ran = counted == null ? 0 : counted[RAN];
            /*
             * A craft's machine steps are nested stages, not operations of their own: the parent craft logs
             * them as its sub-operations, so don't write them to the log or the lifetime tally separately.
             * A silent Operation (a placeholder that became a real one) leaves no trace either.
             */
            final boolean nested = operation instanceof NetworkProcessingOperation proc && proc.isNested();
            if (!nested && !operation.silent()) {
                final OperationRecord record = operation.toRecord().withTiming(waited, ran);
                mainframe.recordOperation(record);
                statistics.record(mainframe.gameTime(), record.type(), !record.completed(), waited, ran,
                        record.moved());
                if (record.completed()) {
                    completedTotal++; // network Operations count toward the lifetime tally too
                }
                postSettled(operation, record);
                OperationMilestones.report(mainframe, asker, operation.typeId(), record);
            }
            it.remove();
        }
    }

    /**
     * Gives every running processing job a distinct physical machine, so concurrency on a machine type scales
     * with the machines actually present, so two same-type jobs never share (and jam) one block. A job keeps
     * the machine it already holds (as long as it is still there and routable); a new job claims a free one of
     * the ones its recipe can route to. A job with no free machine is flagged blocked (it waits, it does not
     * time out). The Machines tab's Max Jobs is an OPTIONAL per-type ceiling on top of this: 0 means "use them
     * all".
     */
    private void assignMachines() {
        final Set<BlockPos> taken = new HashSet<>();
        final Map<String, Integer> perType = new HashMap<>();
        final List<NetworkProcessingOperation> jobs = new ArrayList<>();
        for (final INetworkOperation operation : inFlight) {
            if (operation instanceof NetworkProcessingOperation proc && !proc.isDone()) {
                jobs.add(proc);
            }
        }
        /*
         * Pass 1: a job that already holds a still-valid, unclaimed machine keeps it (stable across ticks so a
         * machine is never fed by two jobs turn and turn about).
         */
        for (final NetworkProcessingOperation proc : jobs) {
            final BlockPos held = proc.assignedMachine();
            if (held != null && !taken.contains(held) && proc.routableMachines().contains(held)) {
                taken.add(held);
                perType.merge(proc.machineKey(), 1, Integer::sum);
                proc.setConcurrencyBlocked(false);
            } else {
                proc.setAssignedMachine(null);
            }
        }
        // Pass 2: an unassigned job claims a free routable machine, within its type's optional Max Jobs ceiling.
        for (final NetworkProcessingOperation proc : jobs) {
            if (proc.assignedMachine() != null) {
                continue;
            }
            final String key = proc.machineKey();
            final int ceiling = mainframe.maxJobsFor(key); // 0 = auto: bounded only by the machines present
            if (ceiling > 0 && perType.getOrDefault(key, 0) >= ceiling) {
                proc.setConcurrencyBlocked(true);
                continue;
            }
            BlockPos free = null;
            for (final BlockPos candidate : proc.routableMachines()) {
                if (!taken.contains(candidate)) {
                    free = candidate;
                    break;
                }
            }
            if (free != null) {
                proc.setAssignedMachine(free);
                taken.add(free);
                perType.merge(key, 1, Integer::sum);
                proc.setConcurrencyBlocked(false);
            } else {
                proc.setConcurrencyBlocked(true); // every machine of this type is busy: wait, do not time out
            }
        }
    }

    /**
     * The per-tick throughput to run {@code operation} at. A machine step feeds at its Crafting Computer's
     * crafting-card throughput (card x CPU), shared among the steps that computer drives at once; the card,
     * not the machine, sets the crafting speed. Everything else runs at the Mainframe's own capacity.
     */
    private long machineFeedBudget(final INetworkOperation operation, final long effectiveCapacity,
                                   final Map<BlockPos, Integer> stepsPerComputer) {
        if (operation instanceof NetworkProcessingOperation proc) {
            final BlockPos cc = proc.executorComputer();
            if (cc == null) {
                return 0L; // no Crafting Computer drives this machine, so it cannot be fed
            }
            final long throughput = mainframe.craftingThroughputAt(cc);
            return throughput / Math.max(1, stepsPerComputer.getOrDefault(cc, 1));
        }
        return effectiveCapacity;
    }

    /**
     * Whether {@code operation} occupies one of the Mainframe's operation queues. A craft's machine stage is
     * orchestrated by its Crafting Computer and gated by that computer's crafting threads, not by the
     * Mainframe, so it never occupies a Mainframe queue: the queues gate distinct operations (a craft, a
     * SELECT, an INSERT), the crafting threads gate one craft's concurrent stages. Every non-stage operation
     * occupies a queue.
     */
    private static boolean occupiesQueue(final INetworkOperation operation) {
        return !(operation instanceof NetworkProcessingOperation proc && proc.isNested());
    }

    private void countTick(final INetworkOperation operation, final int slot) {
        final int[] counted = timing.computeIfAbsent(operation, o -> new int[2]);
        if (slot == RAN && counted[RAN] == 0) {
            post(net -> new IOperationLifecycleEvent.Started(
                    net, operation.operationId(), operation.typeId()));
        }
        counted[slot]++;
    }

    /** The scheduler's timing of an in-flight Operation as {@code [waited, ran]}, zeros before its first tick. */
    private int[] timingOf(final INetworkOperation operation) {
        final int[] counted = timing.get(operation);
        return counted == null ? new int[2] : counted;
    }

    /** Posts one settled Operation's outcome: completed, failed (short), or discarded. */
    private void postSettled(final INetworkOperation operation, final OperationRecord record) {
        if (record.completed()) {
            post(net -> new IOperationLifecycleEvent.Completed(net,
                    operation.operationId(), operation.typeId(), record.waitedTicks() + record.ranTicks()));
        } else if (record.status() == OperationRecord.STATUS_DISCARDED) {
            post(net -> new IOperationLifecycleEvent.Discarded(net,
                    operation.operationId(), operation.typeId()));
        } else {
            post(net -> new IOperationLifecycleEvent.Failed(net,
                    operation.operationId(), operation.typeId(), settleReason(record)));
        }
    }

    private static String settleReason(final OperationRecord record) {
        return switch (record.status()) {
            case OperationRecord.STATUS_PARTIAL -> "delivered " + record.moved() + " of " + record.requested();
            case OperationRecord.STATUS_RESOURCE_LOCKED -> "timed out waiting on a locked resource";
            default -> "failed";
        };
    }

    /**
     * Posts a lifecycle event for this Mainframe's network. Without a network (a Mainframe that just left one,
     * dropping its Operations on the way out) there is nobody to tell, so nothing is built or posted.
     */
    private void post(final Function<NetworkUuid, IOperationLifecycleEvent> event) {
        final NetworkUuid net = mainframe.networkUuid();
        if (net != null) {
            JsCore.events().post(event.apply(net));
        }
    }
}
