/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.operation.ComputingOperations;
import dev.jstech.computers.operation.IPersistentOperation;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.IDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.WatchedDataPort;
import dev.jstech.core.operation.OperationFailure;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.persistence.SavedValue;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Sizes;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * One job of a {@link ProcessingPattern}: the Mainframe's dispatcher gives it a Crafting Interface that holds the
 * pattern, and the job feeds that interface's machine one lot of inputs at a time from the network (or the craft it is
 * a step of), against the machine or through the routers of the interface's own cable, then waits while the machine
 * works. What comes out is credited to it by the Receiving Buses tied to the interface, never more than it fed for;
 * it counts the REAL yield (a probabilistic output's chance only guided the plan). It completes when it has the amount
 * asked for; if nothing moves on its interface for the pattern's timeout it settles partial, or failed when nothing
 * came out at all. What it is still owed when it settles stays with the interface for a while, so a late output goes
 * where this job's outputs go and never to the next job.
 */
@TextHolder
public final class NetworkProcessingOperation implements IPersistentOperation {

    private final ServerLevel level;
    private final NetworkUuid network;
    private final ProcessingPattern pattern;
    private final long requested;
    private final List<BlockPos> candidateComputers;
    private final UUID operationId;
    private final String requesterLabel;
    @Nullable
    private final StorageKey resultKey;
    /*
     * A machine step run inside a craft (given a pool ICraftIo) is "nested": it is one stage of the parent craft, so
     * it is not logged as an operation of its own; the parent's log entry carries it as a sub-operation.
     */
    private final boolean nested;
    /*
     * Where inputs are drawn from and outputs returned to: the network by default; a craft's isolated pool for a
     * machine step run inside a recursive craft, so concurrent steps pipeline without racing on network stock.
     */
    private ICraftIo io;
    /* The interface the dispatcher gave this job, kept over a reload so the job goes on where it was. */
    @Nullable
    private UUID interfaceId;
    /* Where that interface is and the network it is on, as the dispatcher read them this tick. */
    @Nullable
    private CraftingFloor floor;
    @Nullable
    private CraftingFloor.Site site;
    @Nullable
    private BlockPos executor;
    private long seq;
    private long produced;
    private long lotsFed;
    /** Per pattern input: how much has reached the machine so far, against {@code lotsFed} lots' worth. */
    private long[] delivered;
    /** Per pattern output: how much has been credited back. */
    private long[] credited;
    private boolean done;
    private boolean waiting = true;
    private boolean blocked;
    private boolean placed;
    private int idleTicks;
    private int feedCooldown;
    private byte status = OperationRecord.STATUS_FAILED;
    private OperationFailure cause = OperationFailure.NONE;
    private OperationPriority priority = OperationPriority.DEFAULT;
    @Nullable
    private Runnable onSettle;

    public static final String KIND = "processing";

    /** The machine stopped taking what it was being fed, so the run was given up on where it stood. */
    private static final TextKey MACHINE_STOPPED = TextKey.of("jsc.operation.failure.machine_stopped",
            "the machine making the %s stopped taking anything");
    /** No Crafting Interface on the network holds the recipe, so there was no machine to run it on. */
    private static final TextKey NO_INTERFACE = TextKey.of("jsc.operation.failure.no_interface",
            "no Crafting Interface holds the recipe for the %s");
    private static final int FEED_INTERVAL = 4;
    /** How long a machine that has run dry of a gas or a fluid sits still before it is given another lot of it. */
    private static final int STARVED_TICKS = 2 * FEED_INTERVAL;
    /** The most lots that go in on one feed, as far as the machine takes them. */
    private static final int MOST_LOTS = 64;

    public NetworkProcessingOperation(final ServerLevel level, final NetworkUuid network,
                                      final ProcessingPattern pattern, final long requested,
                                      final List<BlockPos> candidateComputers, final UUID operationId,
                                      final String requesterLabel) {
        this(level, network, pattern, requested, candidateComputers, operationId, requesterLabel, null);
    }

    public NetworkProcessingOperation(final ServerLevel level, final NetworkUuid network,
                                      final ProcessingPattern pattern, final long requested,
                                      final List<BlockPos> candidateComputers, final UUID operationId,
                                      final String requesterLabel, @Nullable final ICraftIo io) {
        this.level = level;
        this.network = network;
        this.pattern = pattern;
        this.requested = requested;
        this.candidateComputers = List.copyOf(candidateComputers);
        this.operationId = operationId;
        this.requesterLabel = requesterLabel;
        this.io = io != null ? io : ICraftIo.network(level, network);
        this.nested = io != null;
        final ProcessingPattern.ProcessingOutput primary = pattern.primaryOutput();
        this.resultKey = primary == null ? null : primary.key();
        this.delivered = new long[pattern.inputs().size()];
        this.credited = new long[pattern.outputs().size()];
        if (this.resultKey == null || pattern.inputs().isEmpty() || requested <= 0) {
            finish(); // malformed pattern: settle immediately as FAILED
        }
    }

    @Override
    public void tick(final long throughputBudget) {
        if (done) {
            return;
        }
        if (produced >= requested) {
            status = OperationRecord.STATUS_COMPLETED;
            finish();
            return;
        }
        final CraftingInterfacePart part = site == null ? null : site.part(level, CraftingInterfacePart.class);
        if (part == null || floor == null || blocked) {
            /*
             * No interface for it now: every one that holds the recipe is busy, paused or full, and it waits without
             * a timeout; when none holds the recipe at all, it times out like a machine that never answers.
             */
            waiting = true;
            if (!placed && part == null && ++idleTicks > pattern.timeoutTicks()) {
                cause = OperationFailure.of(NO_INTERFACE, resultName());
                status = produced > 0 ? OperationRecord.STATUS_PARTIAL : OperationRecord.STATUS_FAILED;
                finish();
            }
            return;
        }
        waiting = false;
        final long now = level.getGameTime();
        boolean progressed = false;
        /*
         * Feed when the cooldown elapses, as many lots as the machine takes and the request needs: lots still inside
         * the machine are expected to yield their share, so nothing beyond what the request needs leaves the network.
         * A lot whose chance-based output fell short is simply fed again. A lot is only ever committed whole: what the
         * machine could not take at once stays owed and is topped up as it consumes. What leaves the network for the
         * machine in one tick is bounded by its Crafting Computer's crafting throughput.
         */
        if (--feedCooldown <= 0) {
            feedCooldown = FEED_INTERVAL;
            final IDataPort[] ports = inputPorts(part, now);
            if (ports != null) {
                long budgetLeft = Math.max(0L, throughputBudget) * StorageKey.MB_EQ_PER_ITEM;
                for (int lot = 0; lot < MOST_LOTS && budgetLeft > 0L; lot++) {
                    if (fullyDelivered()) {
                        if (lotsFed >= lotsNeeded()) {
                            if (deliverOwed(ports, budgetLeft) > 0) {
                                progressed = true;
                            }
                            break;
                        }
                        lotsFed++;
                    }
                    final long movedWeight = deliverOwed(ports, budgetLeft);
                    if (movedWeight <= 0) {
                        break; // the machine is full, the network is drained, or the budget is spent
                    }
                    budgetLeft -= movedWeight;
                    progressed = true;
                    if (!fullyDelivered()) {
                        break; // the machine could not take the whole lot yet; finish it before the next one
                    }
                }
            }
        }
        /*
         * The machine is shared by every job on the interface: while any of them is fed or credited the machine is
         * moving, and a job queued behind another's inputs is not timed out for waiting its turn.
         */
        if (progressed || part.workedAt() >= now - 1) {
            idleTicks = 0;
        } else if (++idleTicks > pattern.timeoutTicks()) {
            status = produced > 0 ? OperationRecord.STATUS_PARTIAL : OperationRecord.STATUS_FAILED;
            cause = OperationFailure.of(MACHINE_STOPPED, resultName());
            finish();
        }
    }

    /** The recipe this job runs. */
    public ProcessingPattern pattern() {
        return pattern;
    }

    /** The interface the dispatcher gave this job, or null while it has none. */
    @Nullable
    public UUID interfaceId() {
        return interfaceId;
    }

    /** Where that interface is, as read this tick, or null. */
    @Nullable
    public CraftingFloor.Site site() {
        return site;
    }

    /** The order this job was first fed in, against the other jobs and what settled ones are still owed. */
    public long seq() {
        return seq;
    }

    /**
     * The dispatcher gives the job the interface at {@code at} on {@code where}, driven by the Crafting Computer at
     * {@code by}; a job is numbered in the order it was first given one, which is the order it is credited in.
     */
    public void place(final CraftingFloor where, final CraftingFloor.Site at, final UUID id,
                      @Nullable final BlockPos by, final long order) {
        if (interfaceId == null || seq == 0L) {
            seq = order;
        }
        floor = where;
        site = at;
        interfaceId = id;
        executor = by;
        blocked = false;
        placed = true;
    }

    /**
     * The dispatcher found no interface this job may use now. {@code anyHolds} is whether some interface holds its
     * recipe (busy, paused or full), in which case it waits without a timeout.
     */
    public void unplace(final boolean anyHolds) {
        site = null;
        floor = null;
        executor = null;
        placed = anyHolds;
        blocked = anyHolds;
    }

    /** The Crafting Computer driving this job's interface, whose card sets its feed rate; null without one. */
    @Nullable
    public BlockPos executorComputer() {
        return executor;
    }

    /** How much of output {@code index} this job may be credited: what it fed for. */
    public long cap(final int index) {
        return lotsFed * pattern.outputs().get(index).amount();
    }

    /** How much of output {@code index} has come back to it. */
    public long credited(final int index) {
        return credited[index];
    }

    /** {@code amount} of output {@code index} came back to this job and went to {@link #io()}. */
    public void credit(final int index, final long amount) {
        credited[index] += amount;
        if (pattern.outputs().get(index).key().equals(resultKey)) {
            produced += amount;
        }
    }

    /** Where this job's inputs come from and its outputs go: the network, or the craft it is a step of. */
    public ICraftIo io() {
        return io;
    }

    /** Whether it has fed its machine anything. */
    public boolean fedAny() {
        return lotsFed > 0;
    }

    /** How many lots it has fed its machine. */
    public long lotsFed() {
        return lotsFed;
    }

    /** Whether the recipe consumes {@code key}: an input seen at the machine's output is not an output. */
    public boolean consumes(final StorageKey key) {
        for (final ProcessingPattern.ProcessingInput in : pattern.inputs()) {
            if (in.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** Puts this step under the craft or pipeline it is part of again, after a reload. */
    public void adoptIo(final ICraftIo parent) {
        if (io instanceof HeldIo held) {
            held.flush();
        }
        io = Objects.requireNonNull(parent, "parent");
    }

    public NetworkProcessingOperation onSettle(final Runnable callback) {
        this.onSettle = callback;
        if (done && callback != null) {
            callback.run();
        }
        return this;
    }

    public long produced() {
        return produced;
    }

    /** What the job asked for, of the recipe's primary output. */
    public long requested() {
        return requested;
    }

    @Override
    public boolean isEphemeral() {
        return false;
    }

    /**
     * Whether this is a craft's internal machine stage (fed from the craft's pool), not a standalone operation. Nested
     * steps are not written to the operations log on their own; the parent craft records them as its sub-operations.
     */
    public boolean isNested() {
        return nested;
    }

    @Override
    public UUID operationId() {
        return operationId;
    }

    @Override
    public String typeId() {
        return ComputingOperations.PROCESSING;
    }

    @Override
    public CompoundTag saveState(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putString(KIND_KEY, KIND);
        tag.putUUID(ID_KEY, operationId);
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        ProcessingPattern.CODEC.encodeStart(ops, pattern).result().ifPresent(t -> tag.put("Pattern", t));
        tag.putLong("Requested", requested);
        tag.putLong("Produced", produced);
        tag.putLong("LotsFed", lotsFed);
        tag.putLongArray("Delivered", delivered);
        tag.putLongArray("Credited", credited);
        tag.putInt("IdleTicks", idleTicks);
        tag.putString("Label", requesterLabel);
        tag.putByte(NetworkCraftOperation.PRIORITY_KEY, (byte) priority.id());
        tag.putLong("Seq", seq);
        tag.putBoolean("Nested", nested);
        if (interfaceId != null) {
            tag.putUUID("Interface", interfaceId);
        }
        return tag;
    }

    /**
     * Rebuilds a processing operation saved by {@link #saveState}. It goes back to the interface it had, so what it
     * fed that machine is credited to it as it comes out; the yield counted so far carries over. A step that was part
     * of a craft keeps what it makes apart, in a pool of its own, until the craft takes it again or it settles.
     * Returns null when the saved pattern cannot be read.
     */
    @Nullable
    public static NetworkProcessingOperation restore(final CompoundTag tag, final ServerLevel level,
                                                     final NetworkUuid network, final List<BlockPos> candidateComputers,
                                                     final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final ProcessingPattern pattern = tag.contains("Pattern")
                ? SavedValue.readOr(ProcessingPattern.CODEC.parse(ops, tag.get("Pattern")),
                        JsComputers.LOGGER, "the recipe a saved machine run was working through", null)
                : null;
        if (pattern == null) {
            return null;
        }
        final NetworkProcessingOperation op = new NetworkProcessingOperation(level, network, pattern,
                tag.getLong("Requested"), candidateComputers,
                tag.hasUUID(ID_KEY) ? tag.getUUID(ID_KEY) : UUID.randomUUID(), tag.getString("Label"),
                tag.getBoolean("Nested") ? new HeldIo(level, network) : null);
        op.produced = tag.getLong("Produced");
        op.lotsFed = tag.getLong("LotsFed");
        final long[] savedDelivered = tag.getLongArray("Delivered");
        if (savedDelivered.length == op.delivered.length) {
            op.delivered = savedDelivered;
        }
        final long[] savedCredited = tag.getLongArray("Credited");
        if (savedCredited.length == op.credited.length) {
            op.credited = savedCredited;
        }
        op.idleTicks = tag.getInt("IdleTicks");
        op.priority = NetworkCraftOperation.savedPriority(tag);
        op.seq = tag.getLong("Seq");
        op.interfaceId = tag.hasUUID("Interface") ? tag.getUUID("Interface") : null;
        return op;
    }

    @Override
    public OperationPriority priority() {
        return priority;
    }

    @Override
    public void setPriority(final OperationPriority priority) {
        this.priority = Objects.requireNonNull(priority, "priority");
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean isWaiting() {
        return waiting && !done;
    }

    @Override
    public void abandon() {
        finish();
    }

    @Override
    public void cancel() {
        if (done) {
            return;
        }
        // A run that already made everything it was asked for keeps its COMPLETED; anything short is DISCARDED.
        if (status != OperationRecord.STATUS_COMPLETED) {
            status = OperationRecord.STATUS_DISCARDED;
        }
        finish();
    }

    @Override
    public OperationRecord toRecord() {
        return buildRecord(status);
    }

    @Override
    public OperationRecord liveRecord() {
        final byte live = done ? status
                : waiting ? OperationRecord.STATUS_WAITING : OperationRecord.STATUS_PROCESSING;
        return buildRecord(live);
    }

    /*
     * The ports each input goes in through: the machine's face the interface sits against, or the face of the router
     * that carries it. Null when an input has no way in now (a router was taken off), and nothing is fed this tick.
     */
    @Nullable
    private IDataPort[] inputPorts(final CraftingInterfacePart part, final long now) {
        final CraftingFloor.Reach reach = floor.reach(site);
        if (reach.machine() == null) {
            return null;
        }
        final IDataPort[] ports = new IDataPort[pattern.inputs().size()];
        if (reach.mode() == CraftingFloor.Mode.DIRECT) {
            final IDataPort face = new WatchedDataPort(
                    ExternalDataPort.at(level, reach.machine(), site.face().getOpposite()), () -> part.worked(now));
            Arrays.fill(ports, face);
            return ports;
        }
        final CraftingInterfacePart.HeldPattern held = part.holding(pattern);
        if (held == null) {
            return null;
        }
        for (int i = 0; i < ports.length; i++) {
            final InterfaceRoutes.Route route = InterfaceRoutes.routeFor(level, held, pattern.inputs().get(i).key(),
                    reach);
            if (route.router() == null) {
                return null;
            }
            final CraftingFloor.Site router = route.router();
            final CraftingRouterPart routerPart = router.part(level, CraftingRouterPart.class);
            ports[i] = new WatchedDataPort(ExternalDataPort.at(level, router.faced(), router.face().getOpposite()),
                    () -> {
                        part.worked(now);
                        if (routerPart != null) {
                            routerPart.worked(now);
                        }
                    });
        }
        return ports;
    }

    /** Whether every input of the lots committed so far has reached the machine in full. */
    private boolean fullyDelivered() {
        final List<ProcessingPattern.ProcessingInput> inputs = pattern.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            if (delivered[i] < lotsFed * inputs.get(i).amount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Moves what the machine is owed into it, within {@code budgetWeight} (mB-equivalent) for this tick, and returns
     * the weight moved. Items are owed lot by lot. Fluids and chemicals are continuous: the machine is kept topped up
     * with as much as the whole request still needs, so a tank never starves a machine that could run faster than one
     * lot every few ticks, since the pattern's amount only sets the ratio.
     *
     * <p>That amount is what a lot is expected to use, not a ceiling: a machine burns a gas or a fluid at a rate that
     * only averages it, so the whole request's worth can run out before the last lot is done. A machine that has run
     * dry of one and made no progress for a couple of feed cycles while the request is still short gets another lot's
     * worth of it, so it never stalls on its last lot.
     */
    private long deliverOwed(final IDataPort[] ports, final long budgetWeight) {
        final List<ProcessingPattern.ProcessingInput> inputs = pattern.inputs();
        long movedWeight = 0L;
        for (int i = 0; i < inputs.size() && movedWeight < budgetWeight; i++) {
            final ProcessingPattern.ProcessingInput in = inputs.get(i);
            final long lots = in.key().isItem() ? lotsFed : Math.max(lotsFed, lotsNeeded());
            long owed = lots * in.amount() - delivered[i];
            if (owed <= 0 && !in.key().isItem() && produced < requested && idleTicks >= STARVED_TICKS
                    && ports[i].count(in.key()) <= 0) {
                owed = in.amount();
            }
            if (owed <= 0) {
                continue;
            }
            final long unitWeight = Math.max(1L, in.key().weight(1));
            final long affordable = Math.min(owed, (budgetWeight - movedWeight) / unitWeight);
            if (affordable <= 0) {
                break;
            }
            final long sent = io.select(in.key(), affordable, ports[i]);
            delivered[i] += sent;
            movedWeight += sent * unitWeight;
        }
        return movedWeight;
    }

    /**
     * How many lots the request justifies. A guaranteed primary output needs exactly {@code ceil(requested / amount)}
     * lots, no more, so nothing is wasted. A probabilistic primary output cannot be counted ahead of time, so while the
     * request is still short, one more lot than has been fed is always allowed (a lot that rolled low is simply
     * replaced) and the request-complete check in {@link #tick} stops the job the moment real production catches up.
     */
    private long lotsNeeded() {
        final ProcessingPattern.ProcessingOutput primary = pattern.primaryOutput();
        final long perLot = Math.max(1L, primary.amount());
        if (!primary.probabilistic()) {
            return Sizes.ceilDiv(requested, perLot);
        }
        return produced >= requested ? lotsFed : lotsFed + 1;
    }

    private String resultName() {
        return resultKey == null ? "" : resultKey.displayName().getString();
    }

    /*
     * Settles the job: the interface writes down how it ended and keeps what it is still owed, and a step restored
     * from a save hands what it held to the network.
     */
    private void finish() {
        if (done) {
            return;
        }
        done = true;
        waiting = false;
        final CraftingInterfacePart part = site == null ? null : site.part(level, CraftingInterfacePart.class);
        if (part != null) {
            final byte kind = switch (status) {
                case OperationRecord.STATUS_COMPLETED -> CraftingLog.COMPLETED;
                case OperationRecord.STATUS_PARTIAL -> CraftingLog.PARTIAL;
                default -> CraftingLog.FAILED;
            };
            if (status != OperationRecord.STATUS_DISCARDED || produced > 0) {
                part.log().add(new CraftingLog.Entry(level.getGameTime(), resultKey == null ? "" : resultKey.id(),
                        produced, requested, kind, ""));
            }
            final int window = Math.min(pattern.timeoutTicks(), CraftingInterfacePart.QUIET_TICKS);
            for (int i = 0; i < credited.length; i++) {
                final long remaining = cap(i) - credited[i];
                if (remaining > 0) {
                    part.owed().add(new CraftingInterfacePart.Owed(pattern.outputs().get(i).key(), remaining, window,
                            seq, pattern.identity(), io instanceof HeldIo ? null : io));
                }
            }
        }
        if (io instanceof HeldIo held) {
            held.flush();
        }
        if (onSettle != null) {
            onSettle.run();
        }
    }

    private OperationRecord buildRecord(final byte recordStatus) {
        final StorageKey key = resultKey != null ? resultKey
                : (pattern.inputs().isEmpty() ? null : pattern.inputs().get(0).key());
        return new OperationRecord(operationId, OperationRecord.TYPE_CRAFT, key, requested, produced,
                recordStatus, priority, List.of(), List.of()).withCause(cause);
    }
}
