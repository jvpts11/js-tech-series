/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where someone sits on a seat of a moving structure ({@link BlockAboard.Seat}): unseen, it stands on its seat block
 * wherever the structure carries it, on the server and on each player's game alike from the structure each has, so
 * the sitter rides as steady as the deck. It goes when its sitter gets up, or its seat is gone; it is never saved, so
 * a sitter left in it when the world closes stands up as it opens. A seat that is a helm hands its sitter's controls
 * to the structure ({@link StructureControls}). On a seat its sitter lies on, they lie as a sleeper does, their eyes
 * low over it towards their head, the camera there, looking round as they like.
 */
public final class StructureSeatEntity extends Entity {

    private static final EntityDataAccessor<BlockPos> DATA_SEAT =
            SynchedEntityData.defineId(StructureSeatEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(StructureSeatEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_HELM =
            SynchedEntityData.defineId(StructureSeatEntity.class, EntityDataSerializers.BOOLEAN);
    /* The way a sitter lying on it has their head, in the yard, as a level way's number; or none, sat upright. */
    private static final EntityDataAccessor<Integer> DATA_HEAD =
            SynchedEntityData.defineId(StructureSeatEntity.class, EntityDataSerializers.INT);
    private static final int UPRIGHT = -1;
    private static final double HALF = 0.5;
    /* Got up from a seat, its sitter stands on the seat block, a hair above it. */
    private static final double ABOVE = 0.01;

    public StructureSeatEntity(final EntityType<? extends StructureSeatEntity> type, final Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** A seat at the yard's block {@code seat}, sitting {@code height} above its bottom, a helm or not. */
    static StructureSeatEntity made(final ServerLevel level, final BlockPos seat, final BlockAboard.Seat kind) {
        final StructureSeatEntity made = MovingStructures.SEAT.get().create(level);
        if (made == null) {
            throw new IllegalStateException("a structure's seat could not be made");
        }
        made.entityData.set(DATA_SEAT, seat.immutable());
        made.entityData.set(DATA_HEIGHT, (float) kind.height());
        made.entityData.set(DATA_HELM, kind.helm());
        if (kind.lying()) {
            // Their feet the way the seat looks, their head the other; a seat looking up or down is sat upright.
            final Direction feet = BlockAboard.wayAt(level, seat);
            if (feet.getAxis().isHorizontal()) {
                made.entityData.set(DATA_HEAD, feet.getOpposite().get2DDataValue());
            }
        }
        final IStructureBody body = MovingStructures.at(level, seat);
        if (body != null) {
            made.follow(body, true);
        }
        return made;
    }

    /** The seat block, a place of the yard. */
    public BlockPos seat() {
        return entityData.get(DATA_SEAT);
    }

    /** Whether it is a helm, its sitter steering the structure. */
    public boolean helm() {
        return entityData.get(DATA_HELM);
    }

    /** The way its sitter's head lies, in the yard, for a seat its sitter lies on; none for one sat upright. */
    public @Nullable Direction lyingHead() {
        final int head = entityData.get(DATA_HEAD);
        return head == UPRIGHT ? null : Direction.from2DDataValue(head);
    }

    /** The structure its seat is on, loaded on this side, or null. */
    public @Nullable IStructureBody structure() {
        final BlockPos at = seat();
        return LevelMovingStructures.of(level()).atYard(at.getX(), at.getZ());
    }

    @Override
    public void tick() {
        super.tick();
        final IStructureBody body = structure();
        if (!level().isClientSide()) {
            if (getPassengers().isEmpty() || body == null || BlockAboard.at(level(), seat()).seat().isEmpty()) {
                ejectPassengers();
                discard();
                return;
            }
        }
        if (body != null && body.ready()) {
            follow(body, false);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
        return new Vec3(getX(), getY() + 1 - entityData.get(DATA_HEIGHT) + ABOVE, getZ());
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
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
    public boolean hurt(final DamageSource source, final float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(final DamageSource source) {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public void lerpTo(final double x, final double y, final double z, final float yRot, final float xRot,
                       final int steps) {
        // A player's game sets it on its seat from the structure it has, not from the game's own updates.
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(DATA_SEAT, BlockPos.ZERO);
        builder.define(DATA_HEIGHT, (float) HALF);
        builder.define(DATA_HELM, false);
        builder.define(DATA_HEAD, UPRIGHT);
    }

    /*
     * A sitter lying on it lies from its middle towards their head, as the structure has the seat turned: they are
     * set there with their eyes low over it, as a sleeper's are, and drawn lying the way their head is.
     */
    @Override
    protected void positionRider(final Entity passenger, final MoveFunction move) {
        final Direction head = lyingHead();
        if (head == null) {
            super.positionRider(passenger, move);
            return;
        }
        final double x = head.getStepX() * BlockAboard.Seat.LYING_REACH;
        final double z = head.getStepZ() * BlockAboard.Seat.LYING_REACH;
        final IStructureBody body = structure();
        if (body == null) {
            move.accept(passenger, getX() + x, getY(), getZ() + z);
        } else {
            final StructureFrame frame = body.frame();
            move.accept(passenger, getX() + frame.turnX(x, 0, z), getY() + frame.turnY(x, 0, z),
                    getZ() + frame.turnZ(x, 0, z));
        }
        // A player keeps lying by the game's own reckoning of their pose; anyone else is laid down here.
        if (!(passenger instanceof Player) && passenger.getPose() != Pose.SLEEPING) {
            passenger.setPose(Pose.SLEEPING);
        }
    }

    @Override
    protected void removePassenger(final Entity passenger) {
        super.removePassenger(passenger);
        if (lyingHead() != null && !(passenger instanceof Player) && passenger.getPose() == Pose.SLEEPING) {
            passenger.setPose(Pose.STANDING);
        }
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        // Never saved: a sitter left in it stands up as the world opens.
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        // Never saved.
    }

    /*
     * Stands on its seat block where the structure has it now, as it was the tick before, so it is drawn going
     * between the two; its sitter turned as the structure turned.
     */
    private void follow(final IStructureBody body, final boolean first) {
        final BlockPos at = seat();
        final double x = at.getX() + HALF;
        final double y = at.getY() + entityData.get(DATA_HEIGHT);
        final double z = at.getZ() + HALF;
        final StructureFrame now = body.frame();
        final StructureFrame before = first ? now : body.previousFrame();
        setPos(now.worldX(x, y, z), now.worldY(x, y, z), now.worldZ(x, y, z));
        xo = before.worldX(x, y, z);
        yo = before.worldY(x, y, z);
        zo = before.worldZ(x, y, z);
        xOld = xo;
        yOld = yo;
        zOld = zo;
        final float turn = (float) Mth.wrapDegrees(now.pose().yawDegrees() - before.pose().yawDegrees());
        if (Math.abs(turn) > 1.0e-4F) {
            for (final Entity sitter : getPassengers()) {
                // A player's own game turns them; the server turns the rest.
                final boolean turns = level().isClientSide() ? sitter.isControlledByLocalInstance()
                        : !(sitter instanceof Player);
                if (turns) {
                    sitter.setYRot(sitter.getYRot() + turn);
                    sitter.setYHeadRot(sitter.getYHeadRot() + turn);
                }
            }
        }
    }
}
