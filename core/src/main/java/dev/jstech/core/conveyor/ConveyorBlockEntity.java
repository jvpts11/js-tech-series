/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.ItemFilters;
import dev.jstech.core.mechanical.IMechanicalPart;
import dev.jstech.core.mechanical.ITorque;
import dev.jstech.core.mechanical.MechanicalPorts;
import dev.jstech.core.mechanical.ShaftContact;
import dev.jstech.core.mechanical.ShaftJoin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of a piece of conveyor ({@link ConveyorPieceBlock}): the items on it, in order along it, saved and
 * sent to the players who see it. The end of a run moves the whole run each tick, from its end back, so an item handed
 * on frees room behind it in the same tick; it keeps the run it walked, walking it again only once a piece is laid,
 * broken or turned somewhere, so a long run costs its items, not a search of its pieces every tick: each piece's items go as far as the run moves, and the first of each goes
 * on into the piece after it when there is room. What reaches the end goes to the piece's outlets, through the game's
 * item capability: a belt, a chute, an inventory; with nowhere to go, it waits there or falls off, as its kind says.
 *
 * <p>Its kind's drive moves it: by itself; by a shaft at any of its run's pieces' sides, the run's pieces joined in
 * one network of mechanical power, as fast as the shaft turns against its rated speed; or by energy any of its run's
 * pieces takes in, shared by the run, as fast as what it gets against what it draws.
 */
public class ConveyorBlockEntity extends SyncedBlockEntity implements IMechanicalPart {

    private final ConveyorType type;
    private final ConveyorItems items;
    private final PartField itemsField;
    private final ValueField<Double> rpm;
    private final @Nullable FieldEnergyStorage energy;
    private final @Nullable ValueField<ItemFilter> filter;
    /* At a run's end, whether its run may carry anything: an empty run is not walked again until an item comes on. */
    private boolean awake = true;
    /* Worked out from its block state when first asked after the state changes. */
    private @Nullable ConveyorOpening entry;
    private @Nullable ConveyorOpening exit;
    private @Nullable ConveyorGeometry line;
    /* For a piece that deals in turn, the outlet the next item tries first. */
    private int nextOutlet;
    /* For a piece that takes in turn: the side it last took from, and when each side last offered an item. */
    private @Nullable Direction lastTaken;
    private final long[] offeredAt = new long[Direction.values().length];
    /* The item the machine above holds still to work it, as the very stack on the lane; none while it holds none. */
    private @Nullable ItemStack heldItem;
    /*
     * At a run's end, its run as last walked, and how many times pieces had been laid, broken or turned then: it is
     * walked again only once that changes or a piece of it is gone, not every tick.
     */
    private @Nullable List<ConveyorBlockEntity> walked;
    private long walkedAt = -1;

    /* Past any run a world could hold: a walk along one stops here whatever comes. */
    private static final int WALK_LIMIT = 1 << 16;
    private static final double TICKS = 20.0;
    private static final double MOVED = 1.0e-6;
    /* A piece weighs little in turning and holds back a little in its rollers. */
    private static final double INERTIA = 0.01;
    private static final double FRICTION = 0.05;
    /* A piece moving on energy keeps a second of what it draws. */
    private static final int STORE_TICKS = 20;
    private static final ConveyorType UNKNOWN = ConveyorType.moving(1.0).build();
    private static final double PIXELS = 16.0;
    /* How long a side's offer keeps its turn at a piece that takes in turn, in ticks. */
    private static final long OFFER_KEPT = 2;

    public ConveyorBlockEntity(final BlockEntityType<?> entityType, final BlockPos pos, final BlockState state) {
        super(entityType, pos, state);
        final @Nullable ConveyorPieceBlock piece = state.getBlock() instanceof ConveyorPieceBlock block ? block
                : null;
        this.type = piece != null ? piece.type() : UNKNOWN;
        this.items = new ConveyorItems(this.type.gap());
        this.itemsField = fields().part(ConveyorItems.KEY, this.items).save().toClient();
        this.rpm = fields().value("Rpm", Codec.DOUBLE, 0.0).save();
        if (this.type.drive() instanceof ConveyorDrive.Energy power) {
            final int draw = perTick(power);
            this.energy = fields().energy("Energy", draw * STORE_TICKS, draw * STORE_TICKS, 0).save().exposed();
        } else {
            this.energy = null;
        }
        this.filter = piece != null && piece.filters()
                ? fields().value("Filter", ItemFilters.CODEC, ItemFilter.EVERYTHING).save() : null;
        if (this.type.drive() instanceof ConveyorDrive.Mechanical) {
            MechanicalPorts.keep(this);
        }
        // Broken, it lets fall what was on it, and a machine working an item of it starts over.
        fields().whenBroken((level, at) -> {
            for (final BeltLane.Entry<ItemStack> entry : lane().entries()) {
                Containers.dropItemStack(level, at.getX(), at.getY(), at.getZ(), entry.item().copy());
            }
            if (this.heldItem != null && level.getBlockEntity(at.above()) instanceof IBeltWorker worker) {
                worker.abandoned();
            }
        });
    }

    /** Moves the run whose end it is, on the server. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final ConveyorBlockEntity end) {
        if (level instanceof ServerLevel server) {
            end.moveRun(server);
        }
    }

    /** The kind of conveyor it is. */
    public ConveyorType type() {
        return this.type;
    }

    /** The line its items lie along, and how they lie on it. */
    public ConveyorGeometry geometry() {
        if (this.line == null) {
            this.line = piece().geometry(getBlockState());
        }
        return this.line;
    }

    /** The items on it, the first along it first. */
    public BeltLane<ItemStack> lane() {
        return this.items.lane();
    }

    /** How far it moved in the last tick, in blocks; on a player's game, as last sent. */
    public double moving() {
        return this.items.speed();
    }

    /** On a player's game, the game time its items were last sent at. */
    public long sentAt() {
        return this.items.sentAt();
    }

    /** The place in its lane of the item a machine above holds still; {@link ConveyorItems#NONE_HELD} for none. */
    public int held() {
        return this.items.held();
    }

    /** The energy it holds, for a kind moving on energy; none for any other. */
    public @Nullable FieldEnergyStorage energy() {
        return this.energy;
    }

    /** The filter of a piece that keeps one (a diverter's); a filter that lets everything pass for any other. */
    public ItemFilter filter() {
        final ItemFilter kept = this.filter != null ? this.filter.get() : null;
        return kept != null ? kept : ItemFilter.EVERYTHING;
    }

    /** Sets the filter of a piece that keeps one; on the server. A piece that keeps none ignores it. */
    public void setFilter(final ItemFilter to) {
        if (this.filter != null) {
            this.filter.set(Objects.requireNonNull(to, "to"));
        }
    }

    /**
     * Puts {@code stack} on at {@code at}, from 0 where the piece begins to 1 where it ends, when there is room; on the
     * server.
     *
     * @return whether it went on
     */
    public boolean put(final ItemStack stack, final double at) {
        if (stack.isEmpty() || !lane().put(stack.copy(), at)) {
            return false;
        }
        changed();
        wakeRun();
        return true;
    }

    /** What puts items onto it through its {@code side} face, where its piece takes them in; none where it gives. */
    public @Nullable IItemHandler inserter(@Nullable final Direction side) {
        final double at = piece().intakeAt(getBlockState(), side);
        return at < ConveyorPieceBlock.START ? null : new Inserter(at, side);
    }

    /** {@code entity} stands in its block: an item lying on it is taken onto it, and a mob or player carried. */
    public void touched(final Entity entity) {
        if (this.level == null) {
            return;
        }
        final Vec3 local = entity.position().subtract(Vec3.atLowerCornerOf(this.worldPosition));
        if (entity instanceof ItemEntity lying) {
            if (this.level.isClientSide() || !lying.isAlive()) {
                return;
            }
            final double at = piece().landsAt(getBlockState(), this.worldPosition, local);
            if (at >= ConveyorPieceBlock.START && put(lying.getItem(), at)) {
                lying.discard();
            }
            return;
        }
        final Direction way = piece().carries(getBlockState());
        if (way == null || !this.type.carriesEntities() || entity.isShiftKeyDown()) {
            return;
        }
        final ConveyorGeometry on = geometry();
        if (local.y - on.at(on.along(local)).y <= ConveyorPieceBlock.LYING) {
            carry(entity, way);
        }
    }

    @Override
    public void setBlockState(final BlockState state) {
        super.setBlockState(state);
        this.entry = null;
        this.exit = null;
        this.line = null;
    }

    @Override
    public ShaftContact contact(final Direction face) {
        final Direction.Axis axle = piece().axle(getBlockState());
        return this.type.drive() instanceof ConveyorDrive.Mechanical && axle != null && face.getAxis() == axle
                ? ShaftContact.axle(axle) : ShaftContact.NONE;
    }

    /* The pieces of its run before and after it turn with it: a belt is a chain of rollers. */
    @Override
    public List<ShaftJoin> joins() {
        if (!(this.type.drive() instanceof ConveyorDrive.Mechanical) || this.level == null) {
            return List.of();
        }
        final List<ShaftJoin> out = new ArrayList<>(2);
        final ConveyorBlockEntity ahead = ahead(this.level);
        if (ahead != null) {
            out.add(new ShaftJoin(ahead.worldPosition, 1.0));
        }
        final ConveyorBlockEntity behind = behind(this.level);
        if (behind != null) {
            out.add(new ShaftJoin(behind.worldPosition, 1.0));
        }
        return out;
    }

    @Override
    public double inertia() {
        return INERTIA;
    }

    @Override
    public double friction() {
        return FRICTION;
    }

    @Override
    public @Nullable ITorque torque() {
        return this.type.drive() instanceof ConveyorDrive.Mechanical mechanical ? ITorque.load(mechanical.torque())
                : null;
    }

    @Override
    public double rpm() {
        return this.rpm.get();
    }

    @Override
    public void turning(final double newRpm) {
        this.rpm.set(newRpm);
    }

    /* The items arrived on a player's game: they move on from now. */
    @Override
    protected void afterClientUpdate() {
        if (this.level != null) {
            this.items.arrivedAt(this.level.getGameTime());
        }
    }

    /* Its block, a piece of conveyor while the block entity stands in it. */
    private ConveyorPieceBlock piece() {
        return (ConveyorPieceBlock) getBlockState().getBlock();
    }

    private ConveyorOpening entry() {
        if (this.entry == null) {
            this.entry = piece().entry(getBlockState(), this.worldPosition);
        }
        return this.entry;
    }

    private ConveyorOpening exit() {
        if (this.exit == null) {
            this.exit = piece().exit(getBlockState(), this.worldPosition);
        }
        return this.exit;
    }

    /* The items changed: they are saved and sent. */
    private void changed() {
        this.itemsField.changed();
    }

    /* The piece its run goes on into, when each is the other's first: the two are of one run. */
    private @Nullable ConveyorBlockEntity ahead(final Level level) {
        final ConveyorBlockEntity next = firstAhead(level);
        return next != null && next.firstBehind(level) == this ? next : null;
    }

    private @Nullable ConveyorBlockEntity behind(final Level level) {
        final ConveyorBlockEntity before = firstBehind(level);
        return before != null && before.firstAhead(level) == this ? before : null;
    }

    /* The first piece of its kind that comes on where it leaves: in that block, or a falling belt in the one below. */
    private @Nullable ConveyorBlockEntity firstAhead(final Level level) {
        final ConveyorOpening out = exit();
        final ConveyorBlockEntity there = meeting(level, out.cell(), out, true);
        return there != null ? there : meeting(level, out.cell().below(), out, true);
    }

    /* The first piece of its kind that leaves where it comes on: behind, or a rising belt a block below that. */
    private @Nullable ConveyorBlockEntity firstBehind(final Level level) {
        final ConveyorOpening in = entry();
        final BlockPos back = in.cell().relative(in.way().getOpposite());
        final ConveyorBlockEntity there = meeting(level, back, in, false);
        return there != null ? there : meeting(level, back.below(), in, false);
    }

    /* The piece of its kind at {@code at} that comes on (or, not {@code asEntry}, leaves) at {@code where}. */
    private @Nullable ConveyorBlockEntity meeting(final Level level, final BlockPos at, final ConveyorOpening where,
                                                  final boolean asEntry) {
        if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof ConveyorBlockEntity other)
                || other.type != this.type || !piece().joinsRuns() || !other.piece().joinsRuns()) {
            return null;
        }
        return (asEntry ? other.entry() : other.exit()).equals(where) ? other : null;
    }

    /* Whether its state says it ends its run. */
    private boolean ends() {
        return getBlockState().getValue(ConveyorPieceBlock.END);
    }

    /*
     * Each piece of its run, from its end back, up to the end of the run behind it, or round a loop to itself: walked
     * again only once a piece was laid, broken or turned since, or a piece of it is gone, as with its chunk.
     */
    private List<ConveyorBlockEntity> run(final ServerLevel level) {
        final long shapes = ConveyorPieceBlock.shapes();
        if (this.walked != null && this.walkedAt == shapes && whole(this.walked)) {
            return this.walked;
        }
        final List<ConveyorBlockEntity> run = new ArrayList<>();
        run.add(this);
        ConveyorBlockEntity at = behind(level);
        while (at != null && at != this && !at.ends() && run.size() < WALK_LIMIT) {
            run.add(at);
            at = at.behind(level);
        }
        this.walked = run;
        this.walkedAt = shapes;
        return run;
    }

    /* Whether every piece of {@code run} still stands. */
    private static boolean whole(final List<ConveyorBlockEntity> run) {
        for (final ConveyorBlockEntity piece : run) {
            if (piece.isRemoved()) {
                return false;
            }
        }
        return true;
    }

    /* Wakes the end of its run, which an item on it gives work to. */
    private void wakeRun() {
        if (this.level == null) {
            return;
        }
        ConveyorBlockEntity end = this;
        for (int k = 0; k < WALK_LIMIT && !end.ends(); k++) {
            final ConveyorBlockEntity next = end.ahead(this.level);
            if (next == null) {
                break;
            }
            end = next;
        }
        end.awake = true;
    }

    private void moveRun(final ServerLevel level) {
        if (!this.awake) {
            return;
        }
        final List<ConveyorBlockEntity> run = run(level);
        boolean carrying = false;
        for (final ConveyorBlockEntity piece : run) {
            carrying |= !piece.lane().isEmpty();
        }
        // An empty run is let be, its pieces told once that they stand still, until an item comes on.
        this.awake = carrying;
        final double distance = carrying ? this.type.speed() * share(run) / TICKS : 0.0;
        for (int i = 0; i < run.size(); i++) {
            final ConveyorBlockEntity piece = run.get(i);
            piece.speedNow(distance);
            if (distance <= 0.0 || piece.lane().isEmpty() || !piece.moveItems(level, distance)) {
                continue;
            }
            if (i == 0) {
                deliver(level);
            } else {
                final ConveyorBlockEntity ahead = run.get(i - 1);
                if (ahead.lane().roomAt(ConveyorPieceBlock.START)) {
                    ahead.lane().put(piece.lane().takeFirst(), ConveyorPieceBlock.START);
                    ahead.changed();
                    piece.changed();
                }
            }
        }
    }

    /*
     * Moves its items {@code distance} along, the one a machine above works held at its middle until the machine is
     * done with it.
     *
     * @return whether its first item stands at its end, ready to be handed on
     */
    private boolean moveItems(final ServerLevel level, final double distance) {
        final BlockPos above = this.worldPosition.above();
        final IBeltWorker worker = piece().workedFromAbove(getBlockState()) && level.isLoaded(above)
                && level.getBlockEntity(above) instanceof IBeltWorker machine ? machine : null;
        final int held = heldFor(worker, distance);
        if (held == ConveyorItems.NONE_HELD) {
            return lane().advance(distance, 1.0, ConveyorItems.NONE_HELD);
        }
        lane().advance(distance, 1.0, held, ConveyorPieceBlock.MIDDLE);
        final BeltLane.Entry<ItemStack> under = lane().entries().get(held);
        if (worker != null && under.at() >= ConveyorPieceBlock.MIDDLE) {
            final ItemStack made = worker.work(under.item());
            if (made != null) {
                if (made.isEmpty()) {
                    lane().take(held);
                } else {
                    lane().replace(held, made);
                }
                this.heldItem = null;
                showHeld(ConveyorItems.NONE_HELD);
                changed();
            }
        }
        return !lane().isEmpty() && lane().entries().getFirst().at() >= 1.0;
    }

    /*
     * The place of the item {@code worker} holds still: the one it was working, while it is still on the lane, or the
     * first coming to the middle this tick that it works.
     */
    private int heldFor(final @Nullable IBeltWorker worker, final double distance) {
        final List<BeltLane.Entry<ItemStack>> entries = lane().entries();
        if (worker == null) {
            this.heldItem = null;
            showHeld(ConveyorItems.NONE_HELD);
            return ConveyorItems.NONE_HELD;
        }
        if (this.heldItem != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).item() == this.heldItem) {
                    showHeld(i);
                    return i;
                }
            }
            worker.abandoned();
            this.heldItem = null;
        }
        for (int i = 0; i < entries.size(); i++) {
            final BeltLane.Entry<ItemStack> entry = entries.get(i);
            if (entry.at() <= ConveyorPieceBlock.MIDDLE && entry.at() + distance >= ConveyorPieceBlock.MIDDLE
                    && worker.works(entry.item())) {
                this.heldItem = entry.item();
                showHeld(i);
                return i;
            }
        }
        showHeld(ConveyorItems.NONE_HELD);
        return ConveyorItems.NONE_HELD;
    }

    /* Tells its players which item stands still, when that changes. */
    private void showHeld(final int index) {
        if (this.items.held() != index) {
            this.items.hold(index);
            changed();
        }
    }

    /* How much of its full speed the run moves at this tick, as its drive gives it: from none to all. */
    private double share(final List<ConveyorBlockEntity> run) {
        return switch (this.type.drive()) {
            case ConveyorDrive.Free free -> 1.0;
            case ConveyorDrive.Mechanical mechanical -> Math.min(1.0, Math.abs(rpm()) / mechanical.ratedRpm());
            case ConveyorDrive.Energy power -> {
                final int needed = perTick(power) * run.size();
                int got = 0;
                for (final ConveyorBlockEntity piece : run) {
                    if (piece.energy != null && got < needed) {
                        got += piece.energy.spendUpTo(needed - got);
                    }
                }
                yield got / (double) needed;
            }
        };
    }

    /*
     * Hands the first item at the run's end to its piece's outlets, in order, or in turn for a piece that deals in
     * turn; with no outlet that takes items at all, a kind that drops them lets it fall off at the first.
     */
    private void deliver(final ServerLevel level) {
        final ItemStack first = lane().entries().getFirst().item();
        final List<ConveyorOutlet> outlets = piece().outlets(getBlockState(), this.worldPosition, this, first);
        final boolean inTurn = piece().dealsInTurn();
        boolean anyTaker = false;
        for (int k = 0; k < outlets.size(); k++) {
            final int index = inTurn ? (this.nextOutlet + k) % outlets.size() : k;
            final ConveyorOutlet outlet = outlets.get(index);
            final IItemHandler taker = level.getCapability(Capabilities.ItemHandler.BLOCK, outlet.cell(),
                    outlet.side());
            if (taker == null) {
                continue;
            }
            anyTaker = true;
            final ItemStack left = ItemHandlerHelper.insertItem(taker, first.copy(), false);
            if (left.getCount() != first.getCount()) {
                if (left.isEmpty()) {
                    lane().takeFirst();
                } else {
                    lane().replace(0, left);
                }
                this.nextOutlet = (index + 1) % outlets.size();
                changed();
                return;
            }
        }
        if (!anyTaker && this.type.end() == ConveyorEnd.DROP && !outlets.isEmpty()) {
            final ConveyorOutlet off = outlets.getFirst();
            final Vec3 at = Vec3.atBottomCenterOf(off.cell()).add(0.0, this.type.height() / PIXELS, 0.0);
            final ItemEntity fallen = new ItemEntity(level, at.x, at.y, at.z, lane().takeFirst());
            fallen.setDeltaMovement(Vec3.atLowerCornerOf(off.side().getOpposite().getNormal()).scale(moving()));
            level.addFreshEntity(fallen);
            changed();
        }
    }

    /* Sets how far it moves a tick, sent to its players when it changes. */
    private void speedNow(final double blocksATick) {
        if (Math.abs(this.items.speed() - blocksATick) > MOVED) {
            this.items.moving(blocksATick);
            changed();
        }
    }

    /* Carries {@code entity} {@code way}, as fast as the piece moves, on either side of it. */
    private void carry(final Entity entity, final Direction way) {
        final double speed = moving();
        if (speed <= 0.0) {
            return;
        }
        final Vec3 along = Vec3.atLowerCornerOf(way.getNormal());
        final Vec3 motion = entity.getDeltaMovement();
        final double already = motion.dot(along);
        if (already < speed) {
            entity.setDeltaMovement(motion.add(along.scale(speed - already)));
        }
    }

    /*
     * For a piece that takes in turn, whether {@code side} may put an item on now: not when it put the last one and
     * another side offered one a moment ago. A real offer is noted, taken or not; a trial one is not.
     */
    private boolean yourTurn(final @Nullable Direction side, final boolean trial) {
        if (!piece().takesInTurn() || side == null || this.level == null) {
            return true;
        }
        final long now = this.level.getGameTime();
        if (!trial) {
            this.offeredAt[side.get3DDataValue()] = now;
        }
        if (side != this.lastTaken) {
            return true;
        }
        for (final Direction other : Direction.values()) {
            final long offered = this.offeredAt[other.get3DDataValue()];
            if (other != side && offered > 0 && now - offered <= OFFER_KEPT) {
                return false;
            }
        }
        return true;
    }

    /* What a piece moving on energy draws a tick, in the energy's units: its watts over the ticks of a second. */
    private static int perTick(final ConveyorDrive.Energy power) {
        return Math.max(1, (int) Math.ceil(power.watts() / TICKS));
    }

    /* Puts what it is given on the piece where its piece takes it in from that side, one stack a place. */
    private final class Inserter implements IItemHandler {

        private final double at;
        private final @Nullable Direction side;

        private Inserter(final double at, final @Nullable Direction side) {
            this.at = at;
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (stack.isEmpty()) {
                return stack;
            }
            // The offer counts toward the turns even when there is no room for it yet.
            final boolean turn = yourTurn(this.side, simulate);
            if (!turn || !lane().roomAt(this.at)) {
                return stack;
            }
            if (!simulate) {
                put(stack, this.at);
                ConveyorBlockEntity.this.lastTaken = this.side;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(final int slot) {
            return Item.ABSOLUTE_MAX_STACK_SIZE;
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return true;
        }
    }
}
