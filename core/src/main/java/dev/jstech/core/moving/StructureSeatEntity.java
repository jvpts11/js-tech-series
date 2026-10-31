/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.network.PacketDistributor;
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
    /* Got up from a seat with nowhere to stand round it, its sitter stands on the seat block, a hair above it. */
    private static final double ABOVE = 0.01;
    /* On a player's game, where the server told the player they stand getting up, kept till they are off the seat. */
    private static WeakReference<Entity> toldRider = new WeakReference<>(null);
    private static @Nullable StructureGotUpPayload toldPlace;

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

    /*
     * Its sitter gets up in the nearest place they stand in round the seat, crouching or crawling where there is no
     * more room, as from a minecart ({@link StructureDismount}), set where the structure has that place now; with
     * none within reach, on the seat block. A seat under a low roof, a cockpit's, would otherwise stand them with
     * their head through it.
     */
    @Override
    public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
        final IStructureBody body = structure();
        final StructureDismount.Place place = body == null ? null : gettingUp(passenger);
        if (place == null) {
            return new Vec3(getX(), getY() + 1 - entityData.get(DATA_HEIGHT) + ABOVE, getZ());
        }
        passenger.setPose(place.pose());
        if (passenger instanceof ServerPlayer player && body instanceof MovingStructure structure) {
            // Their game is told where on the structure they stand, and stands them there itself.
            StructureFooting.gotUpAt(player, structure, place.feet());
            final Vec3 at = place.feet();
            PacketDistributor.sendToPlayer(player, new StructureGotUpPayload(at.x, at.y, at.z, place.pose()));
        }
        return world(body.frame(), place.feet());
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

    /*
     * Got up, its sitter stands on the structure from that very tick, on the server and on their game alike, so the
     * deck carries them on as it moves: a structure in flight would otherwise go on from under them before they had
     * touched it, leaving them behind to fall through it. A player's own game stands them where the server told it
     * they get up ({@link StructureGotUpPayload}), on the structure as it draws it: the server only moves them on its
     * side, and set from the server's place, a deck drawn a few ticks behind it would have them slide back along it.
     */
    @Override
    protected void removePassenger(final Entity passenger) {
        super.removePassenger(passenger);
        if (lyingHead() != null && !(passenger instanceof Player) && passenger.getPose() == Pose.SLEEPING) {
            passenger.setPose(Pose.STANDING);
        }
        final IStructureBody body = structure();
        if (body == null || passenger.isRemoved()) {
            return;
        }
        StructureFooting.stood(passenger, body, seat());
        if (!level().isClientSide() || !passenger.isControlledByLocalInstance()) {
            return;
        }
        // Where the server stood them, told just before; or, not told yet, where they would get up, till it is.
        final StructureGotUpPayload told = toldRider.get() == passenger ? toldPlace : null;
        toldRider = new WeakReference<>(null);
        toldPlace = null;
        if (told != null) {
            standUp(passenger, body, told.place(), told.pose());
        } else if (passenger instanceof LivingEntity rider) {
            final StructureDismount.Place place = gettingUp(rider);
            if (place != null) {
                standUp(rider, body, place.feet(), place.pose());
            }
        }
    }

    /**
     * On a player's game, the server told {@code player} where they stand getting up from a seat of a structure:
     * still on the seat, as the server says they are off it only as its tick ends, it is kept till they are; already
     * off it, they are stood there now.
     */
    static void toldGotUp(final Player player, final StructureGotUpPayload payload) {
        if (player.getVehicle() instanceof StructureSeatEntity) {
            toldRider = new WeakReference<>(player);
            toldPlace = payload;
            return;
        }
        final IStructureBody body = LevelMovingStructures.of(player.level()).atYard(Mth.floor(payload.x()),
                Mth.floor(payload.z()));
        if (body != null && StructureFooting.standingOn(player) == body) {
            standUp(player, body, payload.place(), payload.pose());
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

    /* Where, in the yard, and how {@code rider} gets up from it ({@link StructureDismount}); null with nowhere. */
    private StructureDismount.@Nullable Place gettingUp(final LivingEntity rider) {
        final IStructureBody body = structure();
        if (body == null) {
            return null;
        }
        final Direction looks = BlockAboard.wayAt(level(), seat());
        return StructureDismount.of(level(), rider, seat(), looks.getAxis().isHorizontal() ? looks : Direction.NORTH,
                body.frame());
    }

    /*
     * On a player's game, stands {@code rider} at {@code place} of {@code body}'s yard as {@code pose} has them, on the
     * structure as their game draws it, and clear of its blocks as it stands there now, as the deck's carrying keeps
     * them each tick: the server found the room as its own structure stood, a moment apart. Where they stood before is
     * set as the structure stood the tick before, so they are drawn on the deck from the first frame; the tick's end
     * sets that again, should they get up within it.
     */
    private static void standUp(final Entity rider, final IStructureBody body, final Vec3 place, final Pose pose) {
        rider.setPose(pose);
        final Vec3 at = world(body.frame(), place);
        rider.setPos(at.x, at.y, at.z);
        StructureCollisions.pushOut(rider, body);
        final StructureFrame frame = body.frame();
        final Vec3 stands = new Vec3(frame.yardX(rider.getX(), rider.getY(), rider.getZ()),
                frame.yardY(rider.getX(), rider.getY(), rider.getZ()), frame.yardZ(rider.getX(), rider.getY(),
                rider.getZ()));
        final Vec3 before = world(body.previousFrame(), stands);
        rider.xo = before.x;
        rider.yo = before.y;
        rider.zo = before.z;
        rider.xOld = before.x;
        rider.yOld = before.y;
        rider.zOld = before.z;
        rider.setDeltaMovement(Vec3.ZERO);
        StructureFooting.gotUp(rider);
    }

    /* The place of the world {@code at} of the yard stands at, the structure standing as {@code frame}. */
    private static Vec3 world(final StructureFrame frame, final Vec3 at) {
        return new Vec3(frame.worldX(at.x, at.y, at.z), frame.worldY(at.x, at.y, at.z), frame.worldZ(at.x, at.y,
                at.z));
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
