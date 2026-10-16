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
import dev.jstech.core.mechanical.IMechanicalPart;
import dev.jstech.core.mechanical.ITorque;
import dev.jstech.core.mechanical.MechanicalPorts;
import dev.jstech.core.mechanical.ShaftContact;
import dev.jstech.core.mechanical.ShaftJoin;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
 * The block entity of a block of conveyor: the items on it, in order along it, saved and sent to the players who see
 * it. The end of a run moves the whole run each tick, from its end back, so an item handed on frees room behind it in
 * the same tick: each belt's items go as far as the run moves, and the first of each goes on into the belt in front
 * when there is room. What reaches the end goes into a belt in front of another way, at its middle, or into an
 * inventory in front, or, with nowhere to go, waits there or falls off, as its kind says.
 *
 * <p>Its kind's drive moves it: by itself; by a shaft at any of its run's belts' sides, the run's belts joined in
 * one network of mechanical power, as fast as the shaft turns against its rated speed; or by energy any of its run's
 * belts takes in, shared by the run, as fast as what it gets against what it draws.
 */
public class ConveyorBlockEntity extends SyncedBlockEntity implements IMechanicalPart {

    private final ConveyorType type;
    private final ConveyorItems items;
    private final PartField itemsField;
    private final ValueField<Double> rpm;
    private final @Nullable FieldEnergyStorage energy;
    /* At a run's end, whether its run may carry anything: an empty run is not walked again until an item comes on. */
    private boolean awake = true;

    /** The longest run of belts the end of one moves. */
    public static final int LONGEST_RUN = 256;
    private static final double TICKS = 20.0;
    private static final double START = 0.0;
    private static final double MIDDLE = 0.5;
    private static final double MOVED = 1.0e-6;
    /* A belt weighs little in turning and holds back a little in its rollers. */
    private static final double INERTIA = 0.01;
    private static final double FRICTION = 0.05;
    /* A belt moving on energy keeps a second of what it draws. */
    private static final int STORE_TICKS = 20;
    /* How far above its belt an item lying on it may be and still be taken onto it, in blocks. */
    private static final double LYING = 0.25;
    private static final ConveyorType UNKNOWN = ConveyorType.moving(1.0).build();
    private static final int PIXELS = 16;

    public ConveyorBlockEntity(final BlockEntityType<?> entityType, final BlockPos pos, final BlockState state) {
        super(entityType, pos, state);
        this.type = state.getBlock() instanceof ConveyorBlock block ? block.type() : UNKNOWN;
        this.items = new ConveyorItems(this.type.gap());
        this.itemsField = fields().part(ConveyorItems.KEY, this.items).save().toClient();
        this.rpm = fields().value("Rpm", Codec.DOUBLE, 0.0).save();
        if (this.type.drive() instanceof ConveyorDrive.Energy power) {
            final int draw = perTick(power);
            this.energy = fields().energy("Energy", draw * STORE_TICKS, draw * STORE_TICKS, 0).save().exposed();
        } else {
            this.energy = null;
        }
        if (this.type.drive() instanceof ConveyorDrive.Mechanical) {
            MechanicalPorts.keep(this);
        }
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

    /** The way its belt moves. */
    public Direction facing() {
        return getBlockState().getValue(ConveyorBlock.FACING);
    }

    /** The items on it, the first along the belt first. */
    public BeltLane<ItemStack> lane() {
        return this.items.lane();
    }

    /** How far its belt moved in the last tick, in blocks; on a player's game, as last sent. */
    public double moving() {
        return this.items.speed();
    }

    /** On a player's game, the game time its items were last sent at. */
    public long sentAt() {
        return this.items.sentAt();
    }

    /** The energy it holds, for a kind moving on energy; none for any other. */
    public @Nullable FieldEnergyStorage energy() {
        return this.energy;
    }

    /**
     * Puts {@code stack} on at {@code at}, from 0 where the belt begins to 1 where it ends, when there is room; on the
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

    /**
     * What puts items onto it from {@code side}: from behind at its start, from the sides or above at its middle; none
     * from its front, where it gives.
     */
    public @Nullable IItemHandler inserter(@Nullable final Direction side) {
        final Direction facing = facing();
        if (side == facing) {
            return null;
        }
        return new Inserter(side == facing.getOpposite() ? START : MIDDLE);
    }

    /** {@code entity} stands in its block: an item lying on the belt is taken onto it, and a mob or player carried. */
    public void touched(final Entity entity) {
        if (this.level == null) {
            return;
        }
        final double above = entity.getY() - this.worldPosition.getY() - this.type.height() / (double) PIXELS;
        if (entity instanceof ItemEntity lying) {
            if (!this.level.isClientSide() && lying.isAlive() && above <= LYING && put(lying.getItem(),
                    along(entity.position()))) {
                lying.discard();
            }
            return;
        }
        if (this.type.carriesEntities() && above <= LYING && !entity.isShiftKeyDown()) {
            carry(entity);
        }
    }

    @Override
    public ShaftContact contact(final Direction face) {
        final Direction across = facing().getClockWise();
        return this.type.drive() instanceof ConveyorDrive.Mechanical && face.getAxis() == across.getAxis()
                ? ShaftContact.axle(across.getAxis()) : ShaftContact.NONE;
    }

    /* The belts of its run in front and behind turn with it: a belt is a chain of rollers. */
    @Override
    public List<ShaftJoin> joins() {
        if (!(this.type.drive() instanceof ConveyorDrive.Mechanical) || this.level == null) {
            return List.of();
        }
        final List<ShaftJoin> out = new ArrayList<>(2);
        for (final Direction way : new Direction[] {facing(), facing().getOpposite()}) {
            final BlockPos at = this.worldPosition.relative(way);
            if (this.level.getBlockEntity(at) instanceof ConveyorBlockEntity belt && belt.type == this.type
                    && belt.facing() == facing()) {
                out.add(new ShaftJoin(at, 1.0));
            }
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

    /* The items changed: they are saved and sent. */
    private void changed() {
        this.itemsField.changed();
    }

    /* Each belt of the run, from its end back, as far as the run goes. */
    private List<ConveyorBlockEntity> run(final ServerLevel level) {
        final List<ConveyorBlockEntity> run = new ArrayList<>();
        run.add(this);
        final Direction back = facing().getOpposite();
        BlockPos at = this.worldPosition;
        while (run.size() < LONGEST_RUN) {
            at = at.relative(back);
            if (!(level.getBlockEntity(at) instanceof ConveyorBlockEntity belt) || belt.type != this.type
                    || belt.facing() != facing()) {
                break;
            }
            run.add(belt);
        }
        return run;
    }

    /* Wakes the end of its run, which an item on it gives work to. */
    private void wakeRun() {
        if (this.level == null) {
            return;
        }
        ConveyorBlockEntity end = this;
        for (int k = 0; k < LONGEST_RUN; k++) {
            final BlockPos ahead = end.worldPosition.relative(facing());
            if (!(this.level.getBlockEntity(ahead) instanceof ConveyorBlockEntity belt) || belt.type != this.type
                    || belt.facing() != facing()) {
                break;
            }
            end = belt;
        }
        end.awake = true;
    }

    private void moveRun(final ServerLevel level) {
        if (!this.awake) {
            return;
        }
        final List<ConveyorBlockEntity> run = run(level);
        boolean carrying = false;
        for (final ConveyorBlockEntity belt : run) {
            carrying |= !belt.lane().isEmpty();
        }
        // An empty run is let be, its belts told once that they stand still, until an item comes on.
        this.awake = carrying;
        final double distance = carrying ? this.type.speed() * share(run) / TICKS : 0.0;
        for (int i = 0; i < run.size(); i++) {
            final ConveyorBlockEntity belt = run.get(i);
            belt.speedNow(distance);
            if (distance <= 0.0 || belt.lane().isEmpty() || !belt.lane().advance(distance, 1.0, -1)) {
                continue;
            }
            if (i == 0) {
                deliver(level);
            } else {
                final ConveyorBlockEntity ahead = run.get(i - 1);
                if (ahead.lane().roomAt(START)) {
                    ahead.lane().put(belt.lane().takeFirst(), START);
                    ahead.changed();
                    belt.changed();
                }
            }
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
                for (final ConveyorBlockEntity belt : run) {
                    if (belt.energy != null && got < needed) {
                        got += belt.energy.spendUpTo(needed - got);
                    }
                }
                yield got / (double) needed;
            }
        };
    }

    /* Hands the first item at the run's end on: into a belt or an inventory in front, or as its kind says. */
    private void deliver(final ServerLevel level) {
        final Direction facing = facing();
        final BlockPos front = this.worldPosition.relative(facing);
        final ItemStack first = lane().entries().getFirst().item();
        if (level.getBlockEntity(front) instanceof ConveyorBlockEntity belt) {
            // A belt of another kind ahead takes it at its start; one across, at its middle; one head on, never.
            final double at = belt.facing() == facing ? START : belt.facing() == facing.getOpposite() ? -1.0 : MIDDLE;
            if (at >= START && belt.put(first, at)) {
                lane().takeFirst();
                changed();
            }
            return;
        }
        final IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, front,
                facing.getOpposite());
        if (inventory != null) {
            final ItemStack left = ItemHandlerHelper.insertItem(inventory, first.copy(), false);
            if (left.getCount() != first.getCount()) {
                if (left.isEmpty()) {
                    lane().takeFirst();
                } else {
                    lane().replace(0, left);
                }
                changed();
            }
            return;
        }
        if (this.type.end() == ConveyorEnd.DROP) {
            final Vec3 off = Vec3.atBottomCenterOf(front).add(0.0, this.type.height() / (double) PIXELS, 0.0);
            final ItemEntity fallen = new ItemEntity(level, off.x, off.y, off.z, lane().takeFirst());
            fallen.setDeltaMovement(Vec3.atLowerCornerOf(facing.getNormal()).scale(moving()));
            level.addFreshEntity(fallen);
            changed();
        }
    }

    /* Sets how far the belt moves a tick, sent to its players when it changes. */
    private void speedNow(final double blocksATick) {
        if (Math.abs(this.items.speed() - blocksATick) > MOVED) {
            this.items.moving(blocksATick);
            changed();
        }
    }

    /* How far along the belt {@code point} lies, from 0 where it begins to 1 where it ends. */
    private double along(final Vec3 point) {
        final Direction facing = facing();
        final double local = facing.getAxis() == Direction.Axis.X ? point.x - this.worldPosition.getX()
                : point.z - this.worldPosition.getZ();
        final double forward = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? local : 1.0 - local;
        return Math.clamp(forward, START, 1.0);
    }

    /* Carries {@code entity} the way the belt moves, as fast as it moves, on either side. */
    private void carry(final Entity entity) {
        final double speed = moving();
        if (speed <= 0.0) {
            return;
        }
        final Vec3 way = Vec3.atLowerCornerOf(facing().getNormal());
        final Vec3 motion = entity.getDeltaMovement();
        final double already = motion.dot(way);
        if (already < speed) {
            entity.setDeltaMovement(motion.add(way.scale(speed - already)));
        }
    }

    /* What a belt moving on energy draws a tick, in the energy's units: its watts over the ticks of a second. */
    private static int perTick(final ConveyorDrive.Energy power) {
        return Math.max(1, (int) Math.ceil(power.watts() / TICKS));
    }

    /* Puts what it is given on the belt where it was made to, one stack a place. */
    private final class Inserter implements IItemHandler {

        private final double at;

        private Inserter(final double at) {
            this.at = at;
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
            if (stack.isEmpty() || !lane().roomAt(this.at)) {
                return stack;
            }
            if (!simulate) {
                put(stack, this.at);
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
