/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.projectile;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A projectile any mod builds on: it flies as its {@link #spec() spec} says, falling as hard as its gravity, harms and
 * pushes what it hits, sets it burning, passes through as many things as it pierces, and ends against a block, in the
 * last thing it hits, or in a blast. Whoever fired it is credited with the harm.
 *
 * <p>A mod gives it its spec; a model to draw it is the mod's. {@link Projectiles#fire} sends one on its way.
 */
public abstract class CoreProjectile extends ThrowableProjectile {

    /* What it has passed through, so it never hits one thing twice. */
    private final IntSet passedThrough = new IntOpenHashSet();
    private int age;
    /* How many things it has passed through. Saved as a count, since entity ids last only for one session. */
    private int pierced;

    private static final String SAVED_AGE = "Age";
    private static final String SAVED_PIERCED = "Pierced";

    protected CoreProjectile(final EntityType<? extends CoreProjectile> type, final Level level) {
        super(type, level);
    }

    /** How it behaves. */
    public abstract ProjectileSpec spec();

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && ++age > spec().lifetimeTicks()) {
            discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return spec().gravity();
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
        super.onHitBlock(hit);
        if (!level().isClientSide()) {
            end();
        }
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(SAVED_AGE, age);
        tag.putInt(SAVED_PIERCED, pierced);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        age = tag.getInt(SAVED_AGE);
        pierced = tag.getInt(SAVED_PIERCED);
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
