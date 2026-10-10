/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.energy.IEnergyHolder;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * A vehicle any mod builds on: seats players and robots ride in, driven by whoever sits in the first, ahead, back and
 * round, climbing and sinking if it flies, under the pull of whatever dimension it is in if it does not, and spending
 * the energy of its battery as it goes. A player's keys drive it on their own game, which tells the server where it
 * went, as a boat is driven; a robot drives it on the server. A player breaking it gets back what {@link #asItem}
 * says it is.
 *
 * <p>A mod gives it {@link #spec() what it is like} and the item it comes from; a model to draw it is the mod's.
 */
public abstract class VehicleEntity extends Entity implements IEnergyHolder {

    private final Battery battery = new Battery();

    private static final EntityDataAccessor<Integer> ENERGY =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final String SAVED_ENERGY = "Energy";
    /** How much of its speed it keeps each tick it is not driven ahead. */
    private static final double FRICTION = 0.9;
    /** What the overworld pulls a vehicle down by each tick, as it pulls a falling block. */
    private static final double PULL = 0.04;
    private static final float DEGREES_TO_RADIANS = Mth.DEG_TO_RAD;

    protected VehicleEntity(final EntityType<?> type, final Level level) {
        super(type, level);
    }

    /** What the vehicle is like. */
    public abstract VehicleSpec spec();

    /** The item it is placed from, and breaks back into. */
    protected abstract ItemStack asItem();

    @Override
    public IEnergyStorage energy() {
        return battery;
    }

    /** What it asks of itself this tick, from whoever drives it. */
    public DriverInput input() {
        return DriverInputs.of(getControllingPassenger());
    }

    @Override
    public void tick() {
        super.tick();
        if (isControlledByLocalInstance()) {
            drive(input());
            move(MoverType.SELF, getDeltaMovement());
        } else if (!level().isClientSide() && getControllingPassenger() instanceof ServerPlayer) {
            /*
             * A player's game drives the vehicle, so the server never runs drive() for it; it still takes the energy
             * of every tick the player's keys ask for, so the battery the server saves and shows is the true one.
             */
            if (input().moving()) {
                spend(spec().energyPerTick());
            }
        }
    }

    /**
     * One tick of driving: turned by the steering, sped up ahead by the throttle as far as its energy allows and its
     * top speed lets it, slowed when it is not, and pulled down or lifted by the climb.
     */
    public void drive(final DriverInput asked) {
        final VehicleSpec spec = spec();
        final DriverInput input = asked.moving() && !spend(spec.energyPerTick()) ? DriverInput.NONE : asked;
        setYRot(getYRot() - input.strafe() * spec.turnDegrees());
        final Vec3 ahead = Vec3.directionFromRotation(0.0F, getYRot());
        final Vec3 motion = getDeltaMovement();
        Vec3 flat = new Vec3(motion.x, 0.0, motion.z).scale(FRICTION).add(ahead.scale(input.forward()
                * spec.acceleration()));
        if (flat.length() > spec.maxSpeed()) {
            flat = flat.normalize().scale(spec.maxSpeed());
        }
        final double vertical;
        if (spec.flies()) {
            vertical = input.up() ? spec.climbSpeed() : input.down() ? -spec.climbSpeed() : motion.y * FRICTION;
        } else if (input.up() && onGround()) {
            vertical = spec.climbSpeed();
        } else {
            vertical = motion.y - PULL * DimensionRulesData.of(level()).gravity();
        }
        setDeltaMovement(flat.x, vertical, flat.z);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        final Entity first = getFirstPassenger();
        return first instanceof LivingEntity driver && (driver instanceof Player || driver instanceof IDriver)
                ? driver : null;
    }

    @Override
    protected boolean canAddPassenger(final Entity passenger) {
        return getPassengers().size() < spec().seats().size();
    }

    @Override
    protected void positionRider(final Entity passenger, final MoveFunction move) {
        final int seat = Math.max(0, getPassengers().indexOf(passenger));
        final List<Vec3> seats = spec().seats();
        final Vec3 offset = seats.get(Math.min(seat, seats.size() - 1)).yRot(-getYRot() * DEGREES_TO_RADIANS);
        move.accept(passenger, getX() + offset.x, getY() + offset.y, getZ() + offset.z);
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        if (player.isSecondaryUseActive() || !canAddPassenger(player)) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    /** A player's blow breaks it back into its item, kept by a player in survival. */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (isInvulnerableTo(source) || level().isClientSide() || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Player player) {
            final ItemStack item = asItem();
            if (!player.getAbilities().instabuild && !item.isEmpty()) {
                spawnAtLocation(item);
            }
            ejectPassengers();
            discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return asItem();
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(ENERGY, 0);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        entityData.set(ENERGY, Math.max(0, Math.min(spec().energyCapacity(), tag.getInt(SAVED_ENERGY))));
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        tag.putInt(SAVED_ENERGY, entityData.get(ENERGY));
    }

    /* Takes what one tick of driving costs; false when the battery cannot give it. */
    private boolean spend(final int perTick) {
        if (perTick <= 0) {
            return true;
        }
        final int held = entityData.get(ENERGY);
        if (held < perTick) {
            return false;
        }
        entityData.set(ENERGY, held - perTick);
        return true;
    }

    /** The battery, kept in the entity's synced data so the driver's game shows what is left. */
    private final class Battery implements IEnergyStorage {

        @Override
        public int receiveEnergy(final int amount, final boolean simulate) {
            final int held = entityData.get(ENERGY);
            final int taken = Math.max(0, Math.min(amount, spec().energyCapacity() - held));
            if (!simulate && taken > 0) {
                entityData.set(ENERGY, held + taken);
            }
            return taken;
        }

        @Override
        public int extractEnergy(final int amount, final boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return entityData.get(ENERGY);
        }

        @Override
        public int getMaxEnergyStored() {
            return spec().energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return spec().energyCapacity() > 0;
        }
    }
}
