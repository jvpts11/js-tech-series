/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.JsCore;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import dev.jstech.core.util.Loaded;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * The forces on a structure moving by its physics in one tick, each where it acts, summed into a {@link ForceSum}:
 * <ul>
 *   <li>its weight, at its centre;</li>
 *   <li>each engine's thrust, towards the way it looks, by its throttle's signal and the air;</li>
 *   <li>each wing's lift and drag, by the air going past it, the wind's and the structure's own;</li>
 *   <li>its drag, at the faces it shows the flow, by the air's density or a liquid's where they are in one;</li>
 *   <li>its floating, at each block under a liquid, by the volume it displaces, and at each block of the air its hull
 *       holds, so a ship of iron floats as a real one does;</li>
 *   <li>its levitation, holding up to its levitators' strength of its weight and stopping its fall, never lifting;</li>
 *   <li>each magnet's pull towards the nearest magnetic block of the world in its reach.</li>
 * </ul>
 */
final class StructureForces {

    private final Vector3d point = new Vector3d();
    private final Vector3d way = new Vector3d();
    private final Vector3d air = new Vector3d();
    private final Vector3d wind = new Vector3d();
    private final Vector3d middle = new Vector3d();
    private final Vector3d gravity = new Vector3d();
    private final Vector3d up = new Vector3d();
    private final Vector3d going = new Vector3d();
    /* The nearest magnetic block each magnet pulls to, looked for again every second, by the magnet's place. */
    private final Long2ObjectMap<BlockPos> magnetTargets = new Long2ObjectOpenHashMap<>();
    private long magnetsAt = -MAGNET_LOOK;
    /* How stiffly the liquids it floats in hold it at their surface, in newtons a metre, as last gathered. */
    private double floating;
    /* How much of the waves' hold is given, so it never stops a light body's bobbing faster than the step. */
    private double waves = 1;
    private RigidBody moving;
    /* What the structure is told to do now, by its helm or its autopilot. */
    private StructureControls controls = new StructureControls();
    /* How hard each engine pushed, as last gathered, from 0 to 1, by its place in the yard. */
    private final Long2FloatMap engines = new Long2FloatOpenHashMap();
    /* What kind of engine each one that pushed is, by its place in the yard. */
    private final Long2ObjectMap<Aerodynamics.ThrustKind> engineKinds = new Long2ObjectOpenHashMap<>();
    /* Its wings, as last gathered, by their place in the yard. */
    private final LongList wings = new LongArrayList();

    /** The blocks of the world a magnet pulls. */
    static final TagKey<Block> MAGNETIC = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "magnetic"));
    /* How often a magnet looks again for the block it pulls, in ticks. */
    private static final int MAGNET_LOOK = 20;
    /* A magnet's pull grows no more nearer than a block. */
    private static final double MAGNET_NEAREST = 1.0;
    /* A full signal. */
    private static final double FULL = 15.0;
    /*
     * How long the waves a body bobbing at a liquid's surface makes take to carry off its bobbing, in seconds: each
     * block across the surface is held back by the liquid's weight a metre this long, so a boat settles.
     */
    private static final double WAVES = 0.15;
    private static final double HALF = 0.5;

    /**
     * Sums the forces on the structure standing at {@code frame}, moving as {@code body}, into {@code sum}, over a
     * step of {@code seconds}; the pull at its centre is left in {@link #gravity()}.
     */
    void gather(final Level level, final StructureFrame frame, final StructureParts parts, final Box bounds,
                final RigidBody body, final double seconds, final ForceSum sum, final StructureControls told) {
        this.controls = told;
        engines.clear();
        engineKinds.clear();
        wings.clear();
        if (level.getGameTime() - magnetsAt >= MAGNET_LOOK) {
            magnetTargets.clear();
            magnetsAt = level.getGameTime();
        }
        final double mass = parts.mass().mass();
        waves = floating > 0 ? Math.min(1, mass / (seconds * floating * WAVES)) : 1;
        floating = 0;
        moving = body;
        PhysicsWorld.gravity(level, body.position(), gravity);
        up.set(gravity).negate();
        if (up.lengthSquared() > 0) {
            up.normalize();
        }
        sum.addAtCentre(ForceSum.ForceKind.GRAVITY, new Vector3d(gravity).mul(mass));
        for (final Long2DoubleMap.Entry each : parts.displaced().long2DoubleEntrySet()) {
            buoyancy(level, frame, each.getLongKey(), each.getDoubleValue(), sum);
        }
        for (final long air : parts.hull(bounds, level.getGameTime())) {
            buoyancy(level, frame, air, 1.0, sum);
        }
        drag(level, frame, parts, body, mass, seconds, sum);
        double levitation = 0;
        final Vector3d levitators = new Vector3d();
        for (final StructureParts.Part part : parts.parts()) {
            final BlockPos pos = part.pos();
            FrameMath.middle(frame, pos.getX(), pos.getY(), pos.getZ(), point);
            final BlockPhysics physics = part.physics();
            if (physics.thrust().isPresent()) {
                thrust(level, frame, part, physics.thrust().get(), body, sum);
            }
            if (physics.wing().isPresent()) {
                wing(level, frame, part, physics.wing().get(), body, sum);
                wings.add(pos.asLong());
            }
            if (physics.magnet().isPresent()) {
                magnet(level, part, physics.magnet().get(), sum);
            }
            if (physics.levitation().isPresent()) {
                levitation += physics.levitation().get();
                levitators.fma(physics.levitation().get(), point);
            }
        }
        if (levitation > 0 && gravity.lengthSquared() > 0) {
            levitate(levitation, levitators.div(levitation), mass, body, seconds, sum);
        }
    }

    /** The pull at the structure's centre in the last {@link #gather}, in metres a second a second. */
    Vector3d gravity() {
        return gravity;
    }

    /**
     * How stiffly the liquids it floats in held it at their surface in the last {@link #gather}, in newtons a metre:
     * the liquid's weight a metre for each of its blocks across a surface. A light boat is held stiffly, and is worked
     * in steps short enough to hold it steady.
     */
    double floating() {
        return floating;
    }

    /* How hard each engine pushed in the last gathering, from 0 to 1, by its place in the yard. */
    Long2FloatMap engines() {
        return engines;
    }

    /* What kind of engine the one at the yard's packed place {@code pos} is, of those that pushed last. */
    Aerodynamics.ThrustKind engineKind(final long pos) {
        return engineKinds.getOrDefault(pos, Aerodynamics.ThrustKind.ROCKET);
    }

    /* Its wings in the last gathering, by their place in the yard. */
    LongList wings() {
        return wings;
    }

    /*
     * An engine's thrust, towards the way it looks, by its throttle and the air it works in: its throttle its signal,
     * or the structure's controls for the way it pushes, whichever is the more.
     */
    private void thrust(final Level level, final StructureFrame frame, final StructureParts.Part part,
                        final BlockPhysics.Thrust thrust, final RigidBody body, final ForceSum sum) {
        FrameMath.turned(part.way(), frame, way);
        final double throttle = Math.max(BlockPhysics.signal(level, part.pos(), part.way(), thrust.throttle()) / FULL,
                controls.forEngine(new Vec3(way.x, way.y, way.z), level.getGameTime()));
        if (throttle <= 0) {
            return;
        }
        final Vector3d at = new Vector3d(point);
        final double along = body.velocityAt(at, air).dot(way);
        final double share = Aerodynamics.thrustShare(thrust.kind(), PhysicsWorld.airDensity(level, at.y), along,
                thrust.exhaust());
        sum.add(ForceSum.ForceKind.THRUST, new Vector3d(way).mul(thrust.newtons() * share * throttle), at);
        engines.put(part.pos().asLong(), (float) (throttle * share));
        engineKinds.put(part.pos().asLong(), thrust.kind());
    }

    /* A wing's lift and drag, by the air going past it. */
    private void wing(final Level level, final StructureFrame frame, final StructureParts.Part part,
                      final BlockPhysics.Wing wing, final RigidBody body, final ForceSum sum) {
        final Vector3d at = new Vector3d(point);
        FrameMath.turned(wing.face() == BlockPhysics.WingFace.FACING ? part.way() : Direction.UP, frame, way);
        flowAt(level, body, at);
        final Vector3d force = Aerodynamics.wing(air, way, wing.area(), PhysicsWorld.airDensity(level, at.y),
                Math.toRadians(wing.stall()), wing.lift(), new Vector3d());
        sum.add(ForceSum.ForceKind.LIFT, force, at);
    }

    /*
     * The drag of its faces: each way's faces meeting the air, at their middle, drag by the flow coming at them,
     * in the air or in a liquid there, never more than would stop the flow past them in the step.
     */
    private void drag(final Level level, final StructureFrame frame, final StructureParts parts,
                      final RigidBody body, final double mass, final double seconds, final ForceSum sum) {
        for (final Direction face : Direction.values()) {
            final double area = parts.faceArea(face);
            if (area <= 0) {
                continue;
            }
            parts.faceMiddle(face, middle);
            final Vector3d at = FrameMath.world(frame, middle, new Vector3d());
            FrameMath.turned(face, frame, way);
            flowAt(level, body, at);
            final double speed = air.length();
            if (speed <= 0) {
                continue;
            }
            // Only the faces the flow comes at drag, as much as they look into it.
            final double facing = -way.dot(air) / speed;
            if (facing <= 0) {
                continue;
            }
            final PhysicsWorld.Liquid liquid = PhysicsWorld.liquidAt(level, at);
            final double density = liquid.share() * liquid.density()
                    + (1 - liquid.share()) * PhysicsWorld.airDensity(level, at.y);
            final double drag = Math.min(Aerodynamics.drag(speed, density, area * facing, 1.0),
                    mass * speed / seconds);
            sum.add(ForceSum.ForceKind.DRAG, new Vector3d(air).mul(drag / speed), at);
        }
    }

    /*
     * Its floating: the block, or the air of its hull, at {@code key} of the yard, of {@code volume} cubic metres, is
     * held up under a liquid by the weight of the liquid it displaces.
     */
    private void buoyancy(final Level level, final StructureFrame frame, final long key, final double volume,
                          final ForceSum sum) {
        FrameMath.middle(frame, BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key), point);
        if (YardLayout.isYard(point.x)) {
            return;
        }
        final PhysicsWorld.Liquid liquid = PhysicsWorld.liquidAt(level, point);
        if (liquid.share() <= 0) {
            return;
        }
        final double lift = liquid.density() * volume * liquid.share();
        final Vector3d push = new Vector3d(gravity).mul(-lift);
        if (liquid.share() < 1) {
            // Across the surface: held there as stiffly as the liquid weighs, and its bobbing carried off by waves.
            final double weight = liquid.density() * gravity.length();
            floating += weight;
            final double rising = moving.velocityAt(point, going).dot(up);
            push.fma(-weight * WAVES * waves * rising, up);
        }
        sum.add(ForceSum.ForceKind.BUOYANCY, push, new Vector3d(point));
    }

    /*
     * Levitation holds up to {@code strength} newtons of its weight against the pull, and stops its fall with the
     * rest, at its levitators' middle {@code at}, never lifting it past where it would hang still.
     */
    private void levitate(final double strength, final Vector3d at, final double mass, final RigidBody body,
                          final double seconds, final ForceSum sum) {
        final Vector3d up = new Vector3d(gravity).negate().normalize();
        double held = Math.min(strength, mass * gravity.length());
        final double falling = -body.velocity().dot(up);
        if (falling > 0) {
            held = Math.min(strength, held + mass * falling / seconds);
        }
        sum.add(ForceSum.ForceKind.LEVITATION, up.mul(held), at);
    }

    /* A magnet pulls to the nearest magnetic block of the world in its reach. */
    private void magnet(final Level level, final StructureParts.Part part, final BlockPhysics.Magnet magnet,
                        final ForceSum sum) {
        final double strength = magnet.power().isEmpty() ? 1.0
                : BlockPhysics.signal(level, part.pos(), part.way(), magnet.power().get()) / FULL;
        if (strength <= 0) {
            return;
        }
        final long key = part.pos().asLong();
        if (!magnetTargets.containsKey(key)) {
            magnetTargets.put(key, nearestMagnetic(level, point, magnet.reach()));
        }
        final BlockPos target = magnetTargets.get(key);
        if (target == null) {
            return;
        }
        final Vector3d at = new Vector3d(point);
        final Vector3d toward = new Vector3d(target.getX() + HALF, target.getY() + HALF, target.getZ() + HALF)
                .sub(at);
        final double distance = Math.max(MAGNET_NEAREST, toward.length());
        if (distance > magnet.reach()) {
            return;
        }
        sum.add(ForceSum.ForceKind.MAGNETISM,
                toward.normalize(strength * magnet.newtons() / (distance * distance)), at);
    }

    /* How fast the air, or the liquid, goes past the point {@code at}: the wind less the structure's own, into air. */
    private void flowAt(final Level level, final RigidBody body, final Vector3d at) {
        PhysicsWorld.wind(level, at, wind);
        body.velocityAt(at, air);
        air.negate().add(wind);
    }

    /* The nearest magnetic block of the world within {@code reach} of {@code at}, or null. */
    private static BlockPos nearestMagnetic(final Level level, final Vector3d at, final double reach) {
        final int range = (int) Math.ceil(reach);
        final BlockPos centre = BlockPos.containing(at.x, at.y, at.z);
        BlockPos best = null;
        double bestDistance = reach * reach;
        final BlockPos.MutableBlockPos look = new BlockPos.MutableBlockPos();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    final double distance = dx * dx + dy * dy + dz * dz;
                    if (distance > bestDistance) {
                        continue;
                    }
                    look.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (Loaded.at(level, look) && level.getBlockState(look).is(MAGNETIC)) {
                        best = look.immutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }
}
