/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
 */
public abstract class RobotEntity extends PathfinderMob implements IDriver, IOwned {

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    private final Deque<IRobotTask> tasks = new ArrayDeque<>();
    private DriverInput driving = DriverInput.NONE;
    private @Nullable UUID owner;
    /* Whether its owner's limit of robots counts it, which it gives back as it goes. */
    private boolean counted;

    /** How many stacks a robot carries. */
    public static final int INVENTORY_SIZE = 9;
    private static final Logger LOGGER = CoreLogs.ROBOTS.logger();
    private static final String SAVED_TASKS = "Tasks";
    private static final String SAVED_INVENTORY = "Inventory";
    private static final String SAVED_OWNER = "Owner";
    private static final String SAVED_COUNTED = "Counted";
    private static final double WALKING_SPEED = 0.25;
    private static final double HEALTH = 20.0;

    protected RobotEntity(final EntityType<? extends RobotEntity> type, final Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /** What a robot starts with, for a mod declaring one: a walker's speed and a player's health. */
    public static AttributeSupplier.Builder robotAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, WALKING_SPEED)
                .add(Attributes.MAX_HEALTH, HEALTH);
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

    @Override
    public boolean removeWhenFarAway(final double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        final ListTag saved = new ListTag();
        for (final IRobotTask task : tasks) {
            RobotTasks.CODEC.encodeStart(NbtOps.INSTANCE, task).resultOrPartial(problem ->
                    LOGGER.warn("A robot's task was not saved: {}", problem)).ifPresent(saved::add);
        }
        tag.put(SAVED_TASKS, saved);
        tag.put(SAVED_INVENTORY, inventory.createTag(registryAccess()));
        if (owner != null) {
            tag.putUUID(SAVED_OWNER, owner);
            tag.putBoolean(SAVED_COUNTED, counted);
        }
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tasks.clear();
        for (final Tag saved : tag.getList(SAVED_TASKS, Tag.TAG_COMPOUND)) {
            RobotTasks.CODEC.parse(NbtOps.INSTANCE, saved).resultOrPartial(problem ->
                    LOGGER.warn("A robot's task was not read back: {}", problem)).ifPresent(tasks::addLast);
        }
        inventory.fromTag(tag.getList(SAVED_INVENTORY, Tag.TAG_COMPOUND), registryAccess());
        owner = tag.hasUUID(SAVED_OWNER) ? tag.getUUID(SAVED_OWNER) : null;
        counted = owner != null && tag.getBoolean(SAVED_COUNTED);
    }

    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source, final boolean byPlayer) {
        super.dropCustomDeathLoot(level, source, byPlayer);
        Containers.dropContents(level, this, inventory);
    }

    /* One tick of the task at hand; a task done or given up makes way for the next at once. */
    private void work() {
        final IRobotTask task = tasks.peekFirst();
        if (task == null) {
            return;
        }
        final IRobotTask.Status status = task.tick(this);
        if (status != IRobotTask.Status.RUNNING) {
            tasks.pollFirst();
        }
    }
}
