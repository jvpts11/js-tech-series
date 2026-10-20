/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.moving.joint.JointEnd;
import dev.jstech.core.moving.joint.JointKind;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Riding a rope down, a zipline from a ship to the ground: a player who takes hold of a rope ({@link JointKind.Rope})
 * hangs under it and slides along it, the way down, faster the steeper it is and held back as a pulley on a rope is,
 * and lets go at its end. The rope is where its ends are now, so a zipline from a moving ship goes with the ship. Its
 * place along the rope is all it sends; each player's game hangs it from the rope as it draws it. Never saved.
 */
public final class ZiplineEntity extends Entity {

    private static final EntityDataAccessor<Optional<UUID>> DATA_JOINT =
            SynchedEntityData.defineId(ZiplineEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_ALONG =
            SynchedEntityData.defineId(ZiplineEntity.class, EntityDataSerializers.FLOAT);
    private static final double TICKS = 20;
    private static final double GRAVITY = 9.81;
    /* How much a pulley's speed is held back a second, as a share, and the least slope it starts down. */
    private static final double HELD_BACK = 0.25;
    /* How far below the rope the rider hangs, in blocks: a hand's reach above their head. */
    static final double HANGS = 2.1;
    /* Within this share of a rope's end, the rider lets go. */
    private static final double AT_END = 0.02;
    private double speed;

    public ZiplineEntity(final EntityType<? extends ZiplineEntity> type, final Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** A rider's hold on the rope of {@code joint}, a share {@code along} of it from its first end. */
    static ZiplineEntity made(final ServerLevel level, final StructureJoint joint, final double along) {
        final ZiplineEntity made = MovingStructures.ZIPLINE.get().create(level);
        if (made == null) {
            throw new IllegalStateException("a zipline's hold could not be made");
        }
        made.entityData.set(DATA_JOINT, Optional.of(joint.id()));
        made.entityData.set(DATA_ALONG, (float) along);
        made.hang(level, joint, along, true);
        return made;
    }

    /**
     * Where the rope from {@code a} to {@code b} is a share {@code along} of it from {@code a}: taken straight, as a
     * rope is under a rider.
     */
    public static Vec3 along(final Vec3 a, final Vec3 b, final double along) {
        return a.lerp(b, along);
    }

    @Override
    public void tick() {
        super.tick();
        final UUID id = entityData.get(DATA_JOINT).orElse(null);
        if (level() instanceof ServerLevel server) {
            final StructureJoint joint = id == null ? null : StructureJoints.byId(server, id);
            if (joint == null || getPassengers().isEmpty() || !(joint.kind() instanceof JointKind.Rope)) {
                letGo();
                return;
            }
            slide(server, joint);
            return;
        }
        ClientHooks.hang(this);
    }

    /** The rope's joint, as the server sent it. */
    public @Nullable UUID joint() {
        return entityData.get(DATA_JOINT).orElse(null);
    }

    /** How far along the rope it is, a share from its first end. */
    public double place() {
        return entityData.get(DATA_ALONG);
    }

    /** Hangs it under {@code ropeAt}, a point of the rope, where it was too the tick before when {@code first}. */
    public void hangUnder(final Vec3 ropeAt, final boolean first) {
        final Vec3 at = ropeAt.subtract(0, HANGS, 0);
        if (first) {
            setPos(at.x, at.y, at.z);
            xo = at.x;
            yo = at.y;
            zo = at.z;
        } else {
            xo = getX();
            yo = getY();
            zo = getZ();
            setPos(at.x, at.y, at.z);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
        return position();
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
    public boolean hurt(final DamageSource source, final float amount) {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void lerpTo(final double x, final double y, final double z, final float yRot, final float xRot,
                       final int steps) {
        // A player's game hangs it from the rope as it draws it.
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(DATA_JOINT, Optional.empty());
        builder.define(DATA_ALONG, 0F);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        // Never saved: a rider left on a rope as the world closes stands where they were as it opens.
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        // Never saved.
    }

    /* One tick on the server: it slides along the rope the way down, its ends where they are now. */
    private void slide(final ServerLevel level, final StructureJoint joint) {
        final Vec3 a = endOf(level, joint.a());
        final Vec3 b = endOf(level, joint.b());
        if (a == null || b == null) {
            return;
        }
        final double length = a.distanceTo(b);
        if (length < 1.0e-3) {
            letGo();
            return;
        }
        final Vec3 way = b.subtract(a).scale(1 / length);
        final double pull = GRAVITY * DimensionRulesData.of(level).gravityAt(getY());
        speed += (-pull * way.y - HELD_BACK * speed) / TICKS;
        double along = place() + speed / TICKS / length;
        if (along <= AT_END || along >= 1 - AT_END) {
            along = Math.max(0, Math.min(1, along));
            entityData.set(DATA_ALONG, (float) along);
            hangUnder(along(a, b, along), false);
            letGo();
            return;
        }
        entityData.set(DATA_ALONG, (float) along);
        hangUnder(along(a, b, along), false);
    }

    /* Hangs it from the rope of {@code joint} at {@code along}, its ends where they are now. */
    private void hang(final ServerLevel level, final StructureJoint joint, final double along, final boolean first) {
        final Vec3 a = endOf(level, joint.a());
        final Vec3 b = endOf(level, joint.b());
        if (a != null && b != null) {
            hangUnder(along(a, b, along), first);
        }
    }

    private void letGo() {
        ejectPassengers();
        discard();
    }

    /* Where a rope's end is now on the server: on its structure as it stands, or on the world. */
    private static @Nullable Vec3 endOf(final ServerLevel level, final JointEnd end) {
        final Vec3 place = end.place();
        if (end.onWorld()) {
            return place;
        }
        final MovingStructure structure = MovingStructures.byId(level, end.structure().get());
        if (structure == null) {
            return null;
        }
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.worldX(place.x, place.y, place.z), frame.worldY(place.x, place.y, place.z),
                frame.worldZ(place.x, place.y, place.z));
    }

    /** What a player's game hangs a zipline's hold from; a dedicated server keeps the one that does nothing. */
    public static final class ClientHooks {

        private static volatile Consumer<ZiplineEntity> hanging = ignored -> { };

        private ClientHooks() {
        }

        /** Hands in a player's game's hanging, once, as it starts. */
        public static void set(final Consumer<ZiplineEntity> hang) {
            hanging = hang;
        }

        static void hang(final ZiplineEntity zipline) {
            hanging.accept(zipline);
        }
    }
}
