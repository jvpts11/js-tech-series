/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureRays;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * The wheels of a structure moving by its physics. Each hangs on a spring along the structure's down, and looks
 * down for the ground, of the world or of another structure, a car on a ship's deck as well as on a road: its spring,
 * pressed, holds the structure up, as stiff and as damped as it is, worked so a stiff spring under a light structure
 * holds steady. Its tyre grips the ground as the ground lets it, ice barely: it holds against sliding sideways,
 * drives and brakes by its signals, and steers as its signals at its left and right say; all of it no more than the
 * grip of the weight on it allows, so a wheel driven too hard spins and one braked too hard skids. The tyres are
 * worked together, again and again, as the contacts are, so four wheels agree and a car stands still. What a wheel
 * pushes on a structure moving by its physics is pushed back as hard.
 */
final class StructureWheels {

    private final Vector3d mount = new Vector3d();
    private final Vector3d down = new Vector3d();
    private final Vector3d up = new Vector3d();
    private final Vector3d relative = new Vector3d();
    private final Vector3d scratch = new Vector3d();
    private final Vector3d pushed = new Vector3d();
    private final RigidBody ground = new RigidBody();
    /* The tyres on the ground this step, and those kept for the next. */
    private final List<Tyre> tyres = new ArrayList<>();
    private final List<Tyre> spare = new ArrayList<>();
    /* Where the last wheel looked found the ground: the point, its face's way, what it is, and how far down. */
    private final Vector3d hitPoint = new Vector3d();
    private final Vector3d hitNormal = new Vector3d();
    private RigidBody hitBody;
    private double hitGrip;
    private double hitDistance;

    /* A full signal. */
    private static final double FULL = 15.0;
    /* How much of the weight on it a rolling tyre gives back against its rolling. */
    private static final double ROLLING = 0.015;
    /* The grip of plain ground a tyre's own grip is told against. */
    private static final double PLAIN_GRIP = ContactMath.grip(0.6);
    /* How many times the tyres are worked together in a step. */
    private static final int ITERATIONS = 8;

    StructureWheels() {
        ground.fixed();
    }

    /**
     * Works the wheels of the structure standing at {@code frame}, moving as {@code body}, over a step of
     * {@code seconds}; the force they gave, all told, is added to {@code given}.
     */
    void step(final ServerLevel level, final MovingStructure structure, final StructureFrame frame,
              final StructureParts parts, final RigidBody body, final double seconds, final Vector3d given) {
        spare.addAll(tyres);
        tyres.clear();
        for (final StructureParts.Part part : parts.parts()) {
            if (part.physics().wheel().isPresent()) {
                spring(level, structure, frame, part, part.physics().wheel().get(), body, seconds, given);
            }
        }
        if (tyres.isEmpty()) {
            return;
        }
        for (final Tyre tyre : tyres) {
            push(body, tyre.under, tyre.forward, tyre.drive, tyre.point, given, seconds);
        }
        for (int i = 0; i < ITERATIONS; i++) {
            for (final Tyre tyre : tyres) {
                grip(body, tyre, given, seconds);
            }
        }
    }

    /* The wheel's spring: it looks for the ground, and holds the structure up from it; its tyre is kept to grip. */
    private void spring(final ServerLevel level, final MovingStructure structure, final StructureFrame frame,
                        final StructureParts.Part part, final BlockPhysics.Wheel wheel, final RigidBody body,
                        final double seconds, final Vector3d given) {
        final BlockPos pos = part.pos();
        FrameMath.middle(frame, pos.getX(), pos.getY(), pos.getZ(), mount);
        FrameMath.turned(Direction.DOWN, frame, down);
        up.set(down).negate();
        final double length = wheel.travel() + wheel.radius();
        if (!look(level, structure, length)) {
            return;
        }
        final RigidBody under = hitBody;
        final Vector3d at = hitPoint;
        // The spring, as a soft hold along the structure's up: steady however stiff it is.
        final double pressed = length - hitDistance;
        final double give = body.give(up, at) + under.give(up, at);
        final double softness = wheel.damping() + seconds * wheel.stiffness();
        if (give <= 0 || softness <= 0) {
            return;
        }
        final double gamma = 1.0 / (seconds * softness);
        final double beta = seconds * wheel.stiffness() / softness;
        relativeAt(body, under, at);
        final double rising = relative.dot(up);
        final double spring = Math.max(0, (beta * pressed / seconds - rising) / (give + gamma));
        push(body, under, up, spring, at, given, seconds);
        // The tyre: its way along the ground, steered, and its side.
        final Tyre tyre = spare.isEmpty() ? new Tyre() : spare.remove(spare.size() - 1);
        final Vector3d forward = tyre.forward;
        FrameMath.turned(part.way(), frame, forward);
        final double steering = wheel.steer() <= 0 ? 0 : Math.toRadians(wheel.steer())
                * (signal(level, part, wheel.left()) - signal(level, part, wheel.right()));
        if (steering != 0) {
            forward.rotateAxis(steering, up.x, up.y, up.z);
        }
        forward.fma(-forward.dot(hitNormal), hitNormal);
        if (forward.lengthSquared() < 1.0e-9) {
            spare.add(tyre);
            return;
        }
        forward.normalize();
        hitNormal.cross(forward, tyre.side);
        tyre.point.set(at);
        tyre.under = under;
        tyre.most = wheel.grip() * hitGrip / PLAIN_GRIP * spring;
        tyre.drive = wheel.drive() * (signal(level, part, wheel.throttle()) - signal(level, part, wheel.reverse()))
                * seconds;
        tyre.holding = wheel.brake() * signal(level, part, wheel.stop()) * seconds + ROLLING * spring;
        tyre.forwardMass = mass(body, under, forward, at);
        tyre.sideMass = mass(body, under, tyre.side, at);
        tyre.braking = 0;
        tyre.sideways = 0;
        tyres.add(tyre);
    }

    /*
     * One working of a tyre: its brake holds against rolling, its side against sliding, each as far as stops it, and
     * together, with what it drives, no more than its grip.
     */
    private void grip(final RigidBody body, final Tyre tyre, final Vector3d given, final double seconds) {
        relativeAt(body, tyre.under, tyre.point);
        final double rolling = relative.dot(tyre.forward);
        final double sliding = relative.dot(tyre.side);
        double braking = Math.max(-tyre.holding, Math.min(tyre.holding,
                tyre.braking - rolling * tyre.forwardMass));
        double sideways = tyre.sideways - sliding * tyre.sideMass;
        // The friction circle: what it drives, brakes and holds sideways, together, is no more than its grip.
        final double along = tyre.drive + braking;
        final double total = Math.sqrt(along * along + sideways * sideways);
        if (total > tyre.most && total > 0) {
            final double share = tyre.most / total;
            braking = along * share - tyre.drive;
            sideways *= share;
        }
        push(body, tyre.under, tyre.forward, braking - tyre.braking, tyre.point, given, seconds);
        push(body, tyre.under, tyre.side, sideways - tyre.sideways, tyre.point, given, seconds);
        tyre.braking = braking;
        tyre.sideways = sideways;
    }

    /*
     * Looks down from the wheel's mount for the ground within {@code length}: the world's blocks, and the other
     * structures near; whether it found any, the nearest kept.
     */
    private boolean look(final ServerLevel level, final MovingStructure structure, final double length) {
        final Vec3 from = new Vec3(mount.x, mount.y, mount.z);
        final Vec3 to = new Vec3(mount.x + down.x * length, mount.y + down.y * length, mount.z + down.z * length);
        hitDistance = Double.MAX_VALUE;
        hitBody = null;
        final BlockHitResult world = BlockGetter.traverseBlocks(from, to, level,
                (reading, pos) -> hitIn(reading, pos, from, to), reading -> null);
        if (world != null) {
            final BlockPos pos = world.getBlockPos();
            final BlockState state = level.getBlockState(pos);
            take(world.getLocation(), world.getDirection(), ground,
                    ContactMath.grip(state.getFriction(level, pos, null)), from, null);
        }
        final ClipContext ray = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty());
        for (final IStructureBody near : MovingStructures.near(level, new AABB(from, to).inflate(1))) {
            if (near == structure || !near.ready() || !(near instanceof MovingStructure them)) {
                continue;
            }
            final StructureFrame theirFrame = them.frame();
            final BlockHitResult hit = StructureRays.clipYard(level, ray, theirFrame);
            if (hit == null) {
                continue;
            }
            final Vec3 yard = hit.getLocation();
            final Vec3 inWorld = new Vec3(theirFrame.worldX(yard.x, yard.y, yard.z),
                    theirFrame.worldY(yard.x, yard.y, yard.z), theirFrame.worldZ(yard.x, yard.y, yard.z));
            final StructurePhysics physics = them.physics();
            final RigidBody body;
            final double grip;
            if (physics != null && physics.active()) {
                physics.prepareBody();
                body = physics.body();
                grip = physics.parts().grip(hit.getBlockPos().asLong());
            } else {
                body = StructureContacts.held(them);
                grip = ContactMath.grip(level.getBlockState(hit.getBlockPos()).getFriction(level,
                        hit.getBlockPos(), null));
            }
            take(inWorld, hit.getDirection(), body, grip, from, theirFrame);
        }
        return hitBody != null;
    }

    /* Keeps the hit at {@code location} if it is the nearest yet. */
    private void take(final Vec3 location, final Direction face, final RigidBody body, final double grip,
                      final Vec3 from, @Nullable final StructureFrame turned) {
        final double distance = location.distanceTo(from);
        if (distance >= hitDistance) {
            return;
        }
        hitDistance = distance;
        hitPoint.set(location.x, location.y, location.z);
        if (turned == null) {
            hitNormal.set(face.getStepX(), face.getStepY(), face.getStepZ());
        } else {
            FrameMath.turned(face, turned, hitNormal);
        }
        hitBody = body;
        hitGrip = grip;
    }

    /* Pushes the body by {@code size} along {@code way} at {@code at}, and what it stands on back as much. */
    private void push(final RigidBody body, final RigidBody under, final Vector3d way, final double size,
                      final Vector3d at, final Vector3d given, final double seconds) {
        if (size == 0) {
            return;
        }
        body.kick(pushed.set(way), size, at);
        under.kick(pushed.set(way), -size, at);
        given.fma(size / seconds, way);
    }

    /* How fast the body goes past what it stands on at {@code at}, into {@link #relative}. */
    private void relativeAt(final RigidBody body, final RigidBody under, final Vector3d at) {
        body.velocityAt(at, relative);
        under.velocityAt(at, scratch);
        relative.sub(scratch);
    }

    /* How strong a signal reaches the wheel from {@code side}, from 0 to 1. */
    private static double signal(final ServerLevel level, final StructureParts.Part part,
                                 final BlockPhysics.SignalSide side) {
        return BlockPhysics.signal(level, part.pos(), part.way(), side) / FULL;
    }

    /* One over how much the two give against a push along {@code way} at {@code at}. */
    private static double mass(final RigidBody body, final RigidBody under, final Vector3d way, final Vector3d at) {
        final double give = body.give(way, at) + under.give(way, at);
        return give > 0 ? 1.0 / give : 0;
    }

    /* Where the ray meets the world's block at {@code pos}, read only where it is loaded. */
    private static @Nullable BlockHitResult hitIn(final ServerLevel level, final BlockPos pos, final Vec3 from,
                                                  final Vec3 to) {
        if (!Loaded.at(level, pos)) {
            return null;
        }
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        final VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() ? null : shape.clip(from, to, pos);
    }

    /* A tyre on the ground this step: where, its ways, what it stands on, and what it gave so far. */
    private static final class Tyre {

        private final Vector3d point = new Vector3d();
        private final Vector3d forward = new Vector3d();
        private final Vector3d side = new Vector3d();
        private RigidBody under;
        private double most;
        private double drive;
        private double holding;
        private double forwardMass;
        private double sideMass;
        private double braking;
        private double sideways;
    }
}
