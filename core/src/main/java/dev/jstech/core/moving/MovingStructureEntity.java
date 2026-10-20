/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * A moving structure's place among the world's entities: it is seen by the players near it, saved with the chunk it
 * is over, and frozen with that chunk when no one is near. Its blocks are not in it but in the yard; it carries the
 * {@link MovingStructure} on the server and tells each player's game the plot, the pivot and the box of the blocks,
 * from which their game draws the structure where it is. Nothing hurts it, nothing pushes it, and a command that
 * kills every entity leaves it be.
 */
public final class MovingStructureEntity extends Entity {

    private @Nullable MovingStructure structure;
    private boolean following;

    private static final EntityDataAccessor<Optional<UUID>> DATA_ID =
            SynchedEntityData.defineId(MovingStructureEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_PLOT =
            SynchedEntityData.defineId(MovingStructureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> DATA_PIVOT =
            SynchedEntityData.defineId(MovingStructureEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Vector3f> DATA_PIVOT_REST =
            SynchedEntityData.defineId(MovingStructureEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<BlockPos> DATA_MIN =
            SynchedEntityData.defineId(MovingStructureEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DATA_MAX =
            SynchedEntityData.defineId(MovingStructureEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final Logger LOGGER = CoreLogs.MOVING.logger();

    public MovingStructureEntity(final EntityType<? extends MovingStructureEntity> type, final Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    /** The structure on the server, or null on a player's game and for an entity whose save held none. */
    public @Nullable MovingStructure structure() {
        return structure;
    }

    /** The structure's id, on both sides; null until it is known. */
    public @Nullable StructureUuid structureId() {
        return entityData.get(DATA_ID).map(StructureUuid::new).orElse(null);
    }

    /** The plot of the yard its blocks are in, on both sides; -1 until it is known. */
    public int plot() {
        return entityData.get(DATA_PLOT);
    }

    /** The box of the yard its blocks lie in, both corners included, on both sides. */
    public Box bounds() {
        final BlockPos min = entityData.get(DATA_MIN);
        final BlockPos max = entityData.get(DATA_MAX);
        return Box.between(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    /** The pivot its pose stands at, a place of the yard, as {@code {x, y, z}}, on both sides. */
    public double[] pivot() {
        final BlockPos block = entityData.get(DATA_PIVOT);
        final Vector3f rest = entityData.get(DATA_PIVOT_REST);
        // Out in the yard a float holds no fraction of a block: the sum is made in doubles.
        return new double[] {(double) block.getX() + rest.x(), (double) block.getY() + rest.y(),
                (double) block.getZ() + rest.z()};
    }

    @Override
    public void tick() {
        super.tick();
        if (structure != null) {
            structure.tick();
        } else if (!level().isClientSide()) {
            // An entity made by hand, or whose save lost its structure, holds no blocks: it has nothing to be.
            LOGGER.warn("a moving structure's entity at {} holds no structure and is taken away", blockPosition());
            discard();
        }
    }

    @Override
    public void setPos(final double x, final double y, final double z) {
        if (structure != null && !following) {
            final StructurePose pose = structure.pose();
            if (Math.abs(pose.x() - x) + Math.abs(pose.y() - y) + Math.abs(pose.z() - z) > 1.0e-9) {
                structure.teleport(pose.movedTo(x, y, z));
                return;
            }
        }
        super.setPos(x, y, z);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (structure != null && level() instanceof ServerLevel server) {
            structure.hold(server);
        } else if (level().isClientSide()) {
            StructureClientHooks.get().added(this);
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (structure != null && level() instanceof ServerLevel server) {
            structure.letGo(server);
        } else if (level().isClientSide()) {
            StructureClientHooks.get().removed(this);
        }
    }

    @Override
    public void startSeenByPlayer(final ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (structure != null) {
            StructureSync.watch(structure, player);
        }
    }

    @Override
    public void stopSeenByPlayer(final ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (structure != null) {
            StructureSync.unwatch(structure, player);
        }
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide()) {
            StructureClientHooks.get().changed(this);
        }
    }

    /* Sent to another dimension, the structure goes once the chunks it goes to have loaded, its entity made there. */
    @Override
    public @Nullable Entity changeDimension(final DimensionTransition transition) {
        if (structure != null) {
            StructureTravel.travel(structure, transition.newLevel(), transition.pos());
        }
        return null;
    }

    @Override
    public boolean canUsePortal(final boolean allowPassengers) {
        return false;
    }

    @Override
    public void kill() {
        // A structure is taken apart or taken away through the kit, never killed with the entities round it.
    }

    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(final DamageSource source) {
        return true;
    }

    @Override
    public boolean ignoreExplosion(final Explosion explosion) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public void lerpTo(final double x, final double y, final double z, final float yRot, final float xRot,
                       final int steps) {
        // A player's game moves it from the poses it is told, smoothly, not from the game's own updates.
    }

    @Override
    public boolean shouldRenderAtSqrDistance(final double distance) {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox();
    }

    /** Builds a structure's entity on the server, holding {@code moving}, which this sets. */
    void hold(final MovingStructure moving) {
        this.structure = moving;
        entityData.set(DATA_ID, Optional.of(moving.id().value()));
        entityData.set(DATA_PLOT, moving.plot());
        boundsChanged();
        pivotChanged();
        follow(moving.frame());
    }

    /** Moves the entity to where its structure's pose stands, its box the world's box of the blocks. */
    void follow(final StructureFrame frame) {
        following = true;
        try {
            final StructurePose pose = frame.pose();
            setPos(pose.x(), pose.y(), pose.z());
        } finally {
            following = false;
        }
    }

    /** Tells the players' games the box of the blocks, after it grew. */
    void boundsChanged() {
        if (structure == null) {
            return;
        }
        final Box box = structure.bounds();
        entityData.set(DATA_MIN, new BlockPos(box.minX(), box.minY(), box.minZ()));
        entityData.set(DATA_MAX, new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
    }

    /** Tells the players' games where the pivot is, after it moved. */
    void pivotChanged() {
        if (structure == null) {
            return;
        }
        final BlockPos block = BlockPos.containing(structure.pivotX(), structure.pivotY(), structure.pivotZ());
        entityData.set(DATA_PIVOT, block);
        entityData.set(DATA_PIVOT_REST, new Vector3f((float) (structure.pivotX() - block.getX()),
                (float) (structure.pivotY() - block.getY()), (float) (structure.pivotZ() - block.getZ())));
    }

    @Override
    protected AABB makeBoundingBox() {
        if (structure != null) {
            return structure.worldBox();
        }
        final AABB own = StructureClientHooks.get().box(this);
        return own != null ? own : super.makeBoundingBox();
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(DATA_ID, Optional.empty());
        builder.define(DATA_PLOT, -1);
        builder.define(DATA_PIVOT, BlockPos.ZERO);
        builder.define(DATA_PIVOT_REST, new Vector3f());
        builder.define(DATA_MIN, BlockPos.ZERO);
        builder.define(DATA_MAX, BlockPos.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        final MovingStructure read = MovingStructure.read(this, tag);
        if (read != null) {
            hold(read);
        }
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        if (structure != null) {
            structure.write(tag);
        }
    }
}
