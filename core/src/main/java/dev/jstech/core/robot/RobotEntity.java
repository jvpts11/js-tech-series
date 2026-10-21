/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import dev.jstech.core.JsCore;
import dev.jstech.core.energy.IEnergyHolder;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.MachineActors;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.team.IOwned;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.vehicle.DriverInput;
import dev.jstech.core.vehicle.IDriver;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * A robot any mod builds on: a worker that does what it is told, one task after another, a tick at a time, and
 * keeps its tasks, its inventory and its owner with the world. It goes, breaks, gathers and drives; it never fights,
 * and nothing living is its target.
 *
 * <pre>{@code
 * robot.queue(new CoreRobotTasks.BreakBlock(pos));
 * robot.queue(new CoreRobotTasks.Collect(3));
 * }</pre>
 *
 * <p>It acts in the world as its owner would: a block a protected place would not let its owner break, it does not
 * break either, for it asks the Core's protection first. A player has as many robots as the server allows. It drives a
 * vehicle it sits in the front seat of with the {@link CoreRobotTasks.Drive} task.
 *
 * <p>A mod's robot walks, flies or swims ({@link #movement()}), carries as much as its size lets it ({@link
 * #inventorySize()}), digs with whatever tool it carries ({@link RobotTools}), and runs on a battery when it has one
 * ({@link #batteryCapacity()}): working spends it, and a robot with an empty battery stands still until it is charged,
 * at a charging station ({@link RobotCharger}) or through the game's energy capability.
 */
public abstract class RobotEntity extends PathfinderMob implements IDriver, IOwned, IEnergyHolder {

    private final SimpleContainer inventory = new SimpleContainer(inventorySize());
    private final Deque<IRobotTask> tasks = new ArrayDeque<>();
    private final Battery battery = new Battery();
    private DriverInput driving = DriverInput.NONE;
    private @Nullable UUID owner;
    /* Whether its owner's limit of robots counts it, which it gives back as it goes. */
    private boolean counted;
    private int charge;

    /** The most stacks a robot carries, however big it is. */
    public static final int MOST_SLOTS = 54;
    private static final Logger LOGGER = CoreLogs.ROBOTS.logger();
    private static final String SAVED_TASKS = "Tasks";
    private static final String SAVED_INVENTORY = "Inventory";
    private static final String SAVED_OWNER = "Owner";
    private static final String SAVED_COUNTED = "Counted";
    private static final String SAVED_CHARGE = "Charge";
    private static final double WALKING_SPEED = 0.25;
    private static final double FLYING_SPEED = 0.4;
    private static final double HEALTH = 20.0;
    /* How many slots a cubic metre of robot holds: a robot as wide as a player and two thirds as tall holds nine. */
    private static final double SLOTS_PER_CUBIC_METRE = 20.0;
    /* What it spends getting about, in joules a tick: 400 W walking, 600 W swimming, 1.2 kW holding itself up. */
    private static final int WALKING_COST = 20;
    private static final int SWIMMING_COST = 30;
    private static final int FLYING_COST = 60;
    /* How far a flying robot turns at most each tick, in degrees. */
    private static final int FLY_TURN = 10;
    /*
     * How a swimming robot steers in the water, as the game's swimmers do: its pitch and turn at most, in degrees, and
     * how fast it goes in the water and out of it, as shares of its speed.
     */
    private static final int SWIM_PITCH = 85;
    private static final int SWIM_TURN = 10;
    private static final float IN_WATER = 0.1F;
    private static final float OUT_OF_WATER = 0.5F;

    protected RobotEntity(final EntityType<? extends RobotEntity> type, final Level level) {
        super(type, level);
        setPersistenceRequired();
        switch (movement()) {
            case FLY -> {
                this.moveControl = new FlyingMoveControl(this, FLY_TURN, true);
                setNoGravity(true);
            }
            case SWIM -> this.moveControl = new SmoothSwimmingMoveControl(this, SWIM_PITCH, SWIM_TURN, IN_WATER,
                    OUT_OF_WATER, false);
            case WALK -> {
                // The walker's own controls, as every creature that walks has.
            }
        }
    }

    /** What a robot starts with, for a mod declaring one: a walker's speed, a flier's, and a player's health. */
    public static AttributeSupplier.Builder robotAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, WALKING_SPEED)
                .add(Attributes.FLYING_SPEED, FLYING_SPEED).add(Attributes.MAX_HEALTH, HEALTH);
    }

    /**
     * How it gets about: walking, unless a mod's robot says otherwise. It is asked while the robot is made, before any
     * field of a subclass is set, so it gives the same answer always, by the kind of robot alone.
     */
    public RobotMovement movement() {
        return RobotMovement.WALK;
    }

    /** What its battery holds at most, in joules: none, running on nothing, unless a mod's robot says otherwise. */
    public int batteryCapacity() {
        return 0;
    }

    /** The charge in its battery, in joules. */
    public int charge() {
        return charge;
    }

    /** How full its battery is, from nought to 1; full for a robot that runs on nothing. */
    public float chargeShare() {
        final int capacity = batteryCapacity();
        return capacity <= 0 ? 1.0F : (float) charge / capacity;
    }

    /**
     * Spends {@code joules} of its battery on what it does; whether it had them. A robot that runs on nothing always
     * has them, and one that has not enough spends nothing.
     */
    public boolean spend(final int joules) {
        if (batteryCapacity() <= 0 || joules <= 0) {
            return true;
        }
        if (charge < joules) {
            return false;
        }
        charge -= joules;
        return true;
    }

    @Override
    public IEnergyStorage energy() {
        return battery;
    }

    /** Adds a task after the ones it already has. */
    public void queue(final IRobotTask task) {
        tasks.addLast(task);
    }

    /** Forgets every task, the one at hand too, and stops driving. */
    public void clearTasks() {
        tasks.clear();
        driving = DriverInput.NONE;
        getNavigation().stop();
    }

    /** The tasks it has, the one at hand first. */
    public List<IRobotTask> tasks() {
        return List.copyOf(tasks);
    }

    /** What it carries. */
    public SimpleContainer inventory() {
        return inventory;
    }

    /** Who owns it, or none. */
    public @Nullable UUID owner() {
        return owner;
    }

    /**
     * Gives it to {@code player}, who owns what it does, when they may have one more robot ({@link
     * PlayerLimits#ROBOTS}); the owner it had before gives one back.
     *
     * @return whether it is now theirs; a player at their limit does not get it
     */
    public boolean giveTo(final UUID player) {
        if (player.equals(owner)) {
            return true;
        }
        if (level() instanceof ServerLevel server) {
            if (!PlayerLimits.take(server.getServer(), player, PlayerLimits.ROBOTS)) {
                return false;
            }
            if (owner != null && counted) {
                PlayerLimits.giveBack(server.getServer(), owner, PlayerLimits.ROBOTS);
            }
            counted = true;
        }
        this.owner = player;
        return true;
    }

    @Override
    @Nullable
    public Ownership ownership() {
        return owner == null ? null : Ownership.of(owner);
    }

    /** Who answers for what it does: its owner, or the world when it is nobody's. */
    public Actor actor() {
        return owner == null ? Actor.world(blockPosition()) : Actor.forOwner(owner);
    }

    /**
     * Who the robot acts as when the world asks who is breaking a block: a stand-in for its owner, under the owner's
     * id and name, so whatever asks by either finds the owner.
     */
    public Player actingPlayer() {
        return owner == null ? MachineActors.standIn((ServerLevel) level(), getUUID(), getType().toShortString())
                : MachineActors.standIn((ServerLevel) level(), owner);
    }

    @Override
    public void remove(final RemovalReason reason) {
        // A robot gone for good gives its owner one back; one only unloaded with its chunk is still theirs.
        if (reason.shouldDestroy() && owner != null && counted && level() instanceof ServerLevel server) {
            PlayerLimits.giveBack(server.getServer(), owner, PlayerLimits.ROBOTS);
            counted = false;
        }
        super.remove(reason);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide()) {
            Robots.loaded(this, true);
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (!level().isClientSide()) {
            Robots.loaded(this, false);
        }
    }

    @Override
    public DriverInput driverInput() {
        return driving;
    }

    /** What it asks of the vehicle it drives, for the {@link CoreRobotTasks.Drive} task. */
    public void drive(final DriverInput input) {
        this.driving = input;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide()) {
            work();
        }
    }

    /* A swimming robot in the water swims where it heads, as a fish does; out of it, it walks. */
    @Override
    public void travel(final Vec3 travelVector) {
        if (movement() == RobotMovement.SWIM && isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), travelVector);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            return;
        }
        super.travel(travelVector);
    }

    /** A robot never drowns: it breathes nothing. */
    @Override
    public boolean canDrownInFluidType(final FluidType type) {
        return false;
    }

    /** A flying robot never falls far enough to be hurt by it. */
    @Override
    public boolean causeFallDamage(final float distance, final float multiplier, final DamageSource source) {
        return movement() != RobotMovement.FLY && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public boolean removeWhenFarAway(final double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        final RegistryOps<Tag> ops = registryAccess().createSerializationContext(NbtOps.INSTANCE);
        final ListTag saved = new ListTag();
        for (final IRobotTask task : tasks) {
            RobotTasks.CODEC.encodeStart(ops, task).resultOrPartial(problem ->
                    LOGGER.warn("A robot's task was not saved: {}", problem)).ifPresent(saved::add);
        }
        tag.put(SAVED_TASKS, saved);
        tag.put(SAVED_INVENTORY, inventory.createTag(registryAccess()));
        tag.putInt(SAVED_CHARGE, charge);
        if (owner != null) {
            tag.putUUID(SAVED_OWNER, owner);
            tag.putBoolean(SAVED_COUNTED, counted);
        }
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        final RegistryOps<Tag> ops = registryAccess().createSerializationContext(NbtOps.INSTANCE);
        tasks.clear();
        for (final Tag saved : tag.getList(SAVED_TASKS, Tag.TAG_COMPOUND)) {
            RobotTasks.CODEC.parse(ops, saved).resultOrPartial(problem ->
                    LOGGER.warn("A robot's task was not read back: {}", problem)).ifPresent(tasks::addLast);
        }
        inventory.fromTag(tag.getList(SAVED_INVENTORY, Tag.TAG_COMPOUND), registryAccess());
        charge = Mth.clamp(tag.getInt(SAVED_CHARGE), 0, Math.max(0, batteryCapacity()));
        owner = tag.hasUUID(SAVED_OWNER) ? tag.getUUID(SAVED_OWNER) : null;
        counted = owner != null && tag.getBoolean(SAVED_COUNTED);
    }

    /**
     * How many slots its inventory has: as many as its size lets it carry, one for each twentieth of a cubic metre of
     * it, one at the least and {@value #MOST_SLOTS} at the most; a mod's robot may say otherwise. It is asked while
     * the robot is made, before any field of a subclass is set.
     */
    protected int inventorySize() {
        final double volume = getBbWidth() * getBbWidth() * getBbHeight();
        return (int) Math.clamp(Math.round(volume * SLOTS_PER_CUBIC_METRE), 1, MOST_SLOTS);
    }

    /** What it spends getting about, in joules each tick it moves. */
    protected int movingCost() {
        return switch (movement()) {
            case WALK -> WALKING_COST;
            case SWIM -> SWIMMING_COST;
            case FLY -> FLYING_COST;
        };
    }

    @Override
    protected PathNavigation createNavigation(final Level level) {
        return switch (movement()) {
            case FLY -> {
                final FlyingPathNavigation flying = new FlyingPathNavigation(this, level);
                flying.setCanOpenDoors(false);
                flying.setCanFloat(true);
                flying.setCanPassDoors(true);
                yield flying;
            }
            case SWIM -> new AmphibiousPathNavigation(this, level);
            case WALK -> super.createNavigation(level);
        };
    }

    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source, final boolean byPlayer) {
        super.dropCustomDeathLoot(level, source, byPlayer);
        Containers.dropContents(level, this, inventory);
    }

    /*
     * One tick of the task at hand; a task done or given up makes way for the next at once, and is told on the Core's
     * events. Getting about spends its battery; with it empty, it stands still.
     */
    private void work() {
        final boolean onBattery = batteryCapacity() > 0;
        if (onBattery && !getNavigation().isDone() && !spend(movingCost())) {
            getNavigation().stop();
        }
        final IRobotTask task = tasks.peekFirst();
        if (task == null || onBattery && charge <= 0) {
            return;
        }
        final IRobotTask.Status status = task.tick(this);
        if (status != IRobotTask.Status.RUNNING) {
            tasks.pollFirst();
            JsCore.events().post(new RobotTaskEvent(this, task, status));
        }
    }

    /* Its battery, as chargers and cables reach it: it takes charge, and gives none but to its own work. */
    private final class Battery implements IEnergyStorage {

        @Override
        public int receiveEnergy(final int amount, final boolean simulate) {
            final int taken = Math.max(0, Math.min(amount, batteryCapacity() - charge));
            if (!simulate) {
                charge += taken;
            }
            return taken;
        }

        @Override
        public int extractEnergy(final int amount, final boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return charge;
        }

        @Override
        public int getMaxEnergyStored() {
            return Math.max(0, batteryCapacity());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return batteryCapacity() > 0;
        }
    }
}
