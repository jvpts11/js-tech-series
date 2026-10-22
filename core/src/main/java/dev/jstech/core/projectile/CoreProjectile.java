/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.projectile;

import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.room.SealedRooms;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A projectile any mod builds on: it flies as its {@link #spec() spec} says, falling by the pull of the dimension it
 * flies in, harms and pushes what it hits, sets it burning, passes through as many things as it pierces, and ends
 * against a block, in the last thing it hits, or in a blast. Whoever fired it is credited with the harm, and where a
 * protection keeps its shooter from shooting ({@link ProtectedAction#SHOOT}), it is spent there, harming nothing.
 *
 * <p>One with a mass flies by real ballistics: the air where it flies holds it back by its density, thin high up and
 * none in a vacuum, the water far more, and the wind carries it. One that gets through blocks passes a block as hard
 * as it can still get through, slowed by it, and stops in the first it cannot.
 *
 * <p>A mod gives it its spec; a model to draw it is the mod's. {@link Projectiles#fire} sends one on its way.
 */
public abstract class CoreProjectile extends ThrowableProjectile {

    /* What it has passed through, so it never hits one thing twice. */
    private final IntSet passedThrough = new IntOpenHashSet();
    /* The blocks it got through, which it is still inside of for a tick or two. */
    private final LongSet blocksThrough = new LongOpenHashSet();
    private int age;
    /* How many things it has passed through. Saved as a count, since entity ids last only for one session. */
    private int pierced;
    /* The hardness it can still get through; negative until it first meets a block. */
    private float penetrationLeft = -1.0F;

    private static final String SAVED_AGE = "Age";
    private static final String SAVED_PIERCED = "Pierced";
    private static final String SAVED_PENETRATION = "PenetrationLeft";
    private static final double TICKS = 20.0;
    /* What the game's thrown things keep of their speed each tick, in the air and in water. */
    private static final double AIR_KEPT = 0.99;
    private static final double WATER_KEPT = 0.8;

    protected CoreProjectile(final EntityType<? extends CoreProjectile> type, final Level level) {
        super(type, level);
    }

    /** How it behaves. */
    public abstract ProjectileSpec spec();

    /** The hardness of blocks it can still get through. */
    public float penetrationLeft() {
        return penetrationLeft < 0.0F ? spec().penetration() : penetrationLeft;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && ++age > spec().lifetimeTicks()) {
            discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return spec().gravity() * DimensionRulesData.of(level()).gravityAt(getY());
    }

    /*
     * Runs each tick just after the game has slowed it as its own thrown things are slowed: a projectile of real
     * ballistics takes that back and is slowed by the air or the water it flies through instead, against the wind.
     */
    @Override
    protected void applyGravity() {
        final ProjectileSpec spec = spec();
        if (spec.ballistic()) {
            final boolean water = isInWater();
            final Vec3 going = getDeltaMovement().scale(1.0 / (water ? WATER_KEPT : AIR_KEPT));
            final Wind wind = water ? Wind.STILL : Winds.at(level(), position());
            final Vec3 air = new Vec3(wind.x() / TICKS, 0.0, wind.z() / TICKS);
            final Vec3 through = going.subtract(air);
            final double density = water ? Ballistics.WATER : airDensity();
            final double kept = Ballistics.dragScale(through.length() * TICKS, density, spec.dragArea(),
                    spec.mass(), 1.0 / TICKS);
            setDeltaMovement(air.add(through.scale(kept)));
        }
        super.applyGravity();
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
    }

    @Override
    protected boolean canHitEntity(final Entity target) {
        return super.canHitEntity(target) && !passedThrough.contains(target.getId());
    }

    @Override
    protected void onHitEntity(final EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide()) {
            return;
        }
        final ProjectileSpec spec = spec();
        final Entity target = hit.getEntity();
        if (!mayHit(target.blockPosition())) {
            discard();
            return;
        }
        target.hurt(damageSources().thrown(this, getOwner()), spec.damage());
        if (spec.fireSeconds() > 0.0F) {
            target.igniteForSeconds(spec.fireSeconds());
        }
        if (spec.knockback() > 0.0 && target instanceof LivingEntity living) {
            final Vec3 push = getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize();
            living.knockback(spec.knockback(), -push.x, -push.z);
        }
        if (pierced < spec.pierce()) {
            pierced++;
            passedThrough.add(target.getId());
        } else {
            end();
        }
    }

    @Override
    protected void onHitBlock(final BlockHitResult hit) {
        if (blocksThrough.contains(hit.getBlockPos().asLong())) {
            // Still inside a block it got through: it goes on.
            return;
        }
        super.onHitBlock(hit);
        if (level().isClientSide()) {
            return;
        }
        if (!mayHit(hit.getBlockPos())) {
            discard();
        } else if (!getThrough(hit.getBlockPos())) {
            end();
        }
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(SAVED_AGE, age);
        tag.putInt(SAVED_PIERCED, pierced);
        tag.putFloat(SAVED_PENETRATION, penetrationLeft);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        age = tag.getInt(SAVED_AGE);
        pierced = tag.getInt(SAVED_PIERCED);
        penetrationLeft = tag.contains(SAVED_PENETRATION) ? tag.getFloat(SAVED_PENETRATION) : -1.0F;
    }

    /* Whether it gets through the block at {@code pos}, slowed by what that used up of what it gets through. */
    private boolean getThrough(final BlockPos pos) {
        final float before = penetrationLeft();
        if (before <= 0.0F) {
            return false;
        }
        final float hardness = level().getBlockState(pos).getDestroySpeed(level(), pos);
        if (hardness < 0.0F || hardness > before) {
            return false;
        }
        final float after = before - hardness;
        penetrationLeft = after;
        blocksThrough.add(pos.asLong());
        setDeltaMovement(getDeltaMovement().scale(Ballistics.throughScale(before, after)));
        return after > 0.0F || hardness == 0.0F;
    }

    /*
     * Whether it may hit what is at {@code pos}: no protection stops its shooter shooting there. One that may not is
     * spent against the claim's edge, harming nothing, getting through nothing and blowing nothing up.
     */
    private boolean mayHit(final BlockPos pos) {
        if (!(level() instanceof ServerLevel server)) {
            return true;
        }
        final Entity shooter = getOwner();
        final Actor actor = shooter instanceof ServerPlayer player ? Actor.of(player)
                : Actor.world(shooter == null ? null : shooter.blockPosition());
        return Protection.may(actor, ProtectedAction.SHOOT, server, pos);
    }

    /* How dense the air is where it flies: a sealed room's own, or the dimension's at its height. */
    private double airDensity() {
        final Climate climate = SealedRooms.climateAt(level(), blockPosition());
        return Ballistics.airDensity(climate.pressure(), climate.temperature());
    }

    /* Its end: a blast when it has one, then gone. */
    private void end() {
        final ProjectileSpec spec = spec();
        if (spec.explosion() > 0.0F) {
            level().explode(this, getX(), getY(), getZ(), spec.explosion(),
                    spec.breaksBlocks() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        }
        discard();
    }
}
