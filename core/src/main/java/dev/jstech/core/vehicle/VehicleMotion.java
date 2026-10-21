/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One tick of a vehicle going, on whichever side drives it: on the road, each wheel on its spring over the ground
 * under it, the body leaning as the wheels stand, the engine's push held to what the tyres grip on that ground, the
 * brakes, the slope, the air and the road holding it back, and the steering turning it as tight as its grip lets it,
 * sliding wide past that and on ice; in the air, its rotors holding it up and lifting it; afloat, the water bearing it
 * and its propellers pushing it. A vehicle with no engine running only rolls, falls and floats.
 */
final class VehicleMotion {

    private final VehicleEntity vehicle;
    private float pitch;
    private float roll;
    private float steer;
    private float wheelTurn;
    private VehicleDrive.Result lastResult = new VehicleDrive.Result(0, 0, 0, 1, Double.NaN, 0);
    private double[] drops = new double[0];

    private static final double TICK = 0.05;
    private static final double TICKS = 20;
    /* A wheel looks this far below where it stands for the ground. */
    private static final double LOOK_BELOW = 0.6;
    /* How much of the way to where its wheels lean it the body goes each tick. */
    private static final float LEAN_EASE = 0.4F;
    /* Above this speed, in metres a second, a brake held backwards brakes; below it, it backs up. */
    private static final double STILL = 0.5;
    /* What a flat tyre grips, of what it did. */
    private static final double FLAT = 0.4;
    /* A flying vehicle climbs or sinks at most this fast, and closes on it by this much a second. */
    private static final double CLIMB = 4;
    private static final double CLIMB_GAIN = 3;
    /* How hard it speeds up ahead in the air, as a share of the pull, and how much of its speed the air takes. */
    private static final double AIR_PUSH = 0.6;
    private static final double AIR_DRAG = 0.6;
    /* How fast a vehicle in the air or afloat turns, in degrees a second. */
    private static final double TURN = 70;
    /* The density of water, and how deep a vehicle afloat floats, as a share of its height. */
    private static final double WATER = 1000;
    private static final double DRAFT = 0.35;
    private static final double FLOAT_GAIN = 6;
    private static final double FLOAT_DAMPING = 2;
    private static final double HULL = 0.05;
    private static final double SEA_LEVEL_DENSITY = 1.225;
    private static final double SEA_LEVEL_PRESSURE = 101.3;
    private static final double OVERWORLD_PULL = 9.81;

    VehicleMotion(final VehicleEntity vehicle) {
        this.vehicle = vehicle;
    }

    /** How far its nose leans up, in degrees, as its wheels stand. */
    float pitch() {
        return pitch;
    }

    /** How far it leans to its left, in degrees. */
    float roll() {
        return roll;
    }

    /** How far its steering is turned, from -1 right to 1 left. */
    float steer() {
        return steer;
    }

    /** How far its wheels have turned, in degrees, for drawing them. */
    float wheelTurn() {
        return wheelTurn;
    }

    /** How far below where it stands each wheel's ground is, in metres, in the order of its wheels. */
    double[] drops() {
        return drops;
    }

    /** What its drivetrain gave last tick. */
    VehicleDrive.Result lastResult() {
        return lastResult;
    }

    /** One tick of going, asked {@code input} by whoever drives it, its engine {@code running} or not. */
    VehicleDrive.Result step(final VehicleDef def, final VehicleBuild build, final DriverInput input,
                             final boolean running) {
        final Level level = vehicle.level();
        final DimensionRules rules = DimensionRulesData.of(level);
        final Climate climate = rules.at(vehicle.getY());
        final double pull = OVERWORLD_PULL * rules.gravityAt(vehicle.getY());
        final double density = SEA_LEVEL_DENSITY * climate.pressure() / SEA_LEVEL_PRESSURE;
        final float yaw = vehicle.getYRot();
        final Vec3 ahead = Vec3.directionFromRotation(0, yaw);
        final Vec3 left = new Vec3(ahead.z, 0, -ahead.x);
        final Vec3 velocity = vehicle.getDeltaMovement().scale(TICKS);
        double along = velocity.dot(ahead);
        double sideways = velocity.dot(left);
        double up = velocity.y;
        final double mass = Math.max(1, build.mass());
        steer = Mth.clamp(input.strafe(), -1, 1);
        final VehicleDrive.Result result;
        switch (def.movement()) {
            case AIR -> {
                result = fly(build, input, running, climate.temperature());
                final double wantedUp = input.up() ? CLIMB : input.down() ? -CLIMB : 0;
                final double asked = mass * (pull + CLIMB_GAIN * (wantedUp - up));
                final double lift = running ? Math.clamp(asked, 0, build.lift()) : 0;
                up += (lift / mass - pull) * TICK;
                if (running && build.lift() > mass * pull) {
                    along += input.forward() * AIR_PUSH * pull * TICK;
                    sideways *= 1 - AIR_DRAG * TICK;
                }
                along -= VehicleMath.resistance(along, def.body().dragArea(), density, mass, false) / mass * TICK;
                along *= 1 - AIR_DRAG * TICK * (vehicle.onGround() ? 4 : 1);
                vehicle.setYRot(yaw - (float) (steer * TURN * TICK));
                lean(0, 0);
            }
            case WATER -> {
                result = sail(build, input, running, climate.temperature());
                final double surface = surface(level);
                if (!Double.isNaN(surface)) {
                    final double sink = vehicle.getY() + DRAFT * def.body().height() - surface;
                    up += (-FLOAT_GAIN * sink - FLOAT_DAMPING * up) * TICK;
                    final double push = running ? build.push() * forwardThrottle(input) : 0;
                    final double hull = 0.5 * WATER * def.body().dragArea() * HULL;
                    along += (push - hull * along * Math.abs(along)) / mass * TICK;
                    sideways *= 1 - 2 * TICK;
                    final double rudder = Math.min(1, Math.abs(along) / 3);
                    vehicle.setYRot(yaw - (float) (steer * TURN * rudder * Math.signum(along) * TICK));
                } else {
                    up -= pull * TICK;
                    along *= 1 - 4 * TICK;
                    sideways *= 1 - 4 * TICK;
                }
                lean(0, 0);
            }
            default -> {
                final Ground ground = ground(level, build);
                final double grip = ground.grip(build);
                final boolean backing = input.forward() < 0 && along < STILL;
                final double throttle = backing ? -input.forward() : Math.max(0, input.forward());
                final VehicleDrive.Controls controls = new VehicleDrive.Controls(throttle, backing,
                        !vehicle.switches().manual(), vehicle.takeShift(), running);
                result = build.engine().isPresent()
                        ? drive().step(build.engine().get().spec().drive(build.engine().get().health()),
                                build.gearbox(), build.drivenRadius(), controls, along, climate.temperature(), TICK)
                        : new VehicleDrive.Result(0, 0, 0, 0, climate.temperature(), 0);
                double force = 0;
                double braking = 0;
                if (ground.touching()) {
                    final double driven = mass * pull * Math.max(1, build.drivenWheels())
                            / Math.max(1, build.wheels().size());
                    force = Math.clamp(result.force(), -grip * driven, grip * driven);
                    final boolean brakes = input.forward() < 0 && along > STILL;
                    braking = brakes ? -input.forward() : vehicle.switches().parkingBrake() ? 1 : 0;
                }
                vehicle.setSwitches(vehicle.switches().braking(braking > 0));
                final double slope = -mass * pull * Math.sin(Math.toRadians(pitch));
                final double held = VehicleMath.resistance(along, def.body().dragArea(), density, mass,
                        ground.touching());
                along += (force + slope - held) / mass * TICK;
                if (braking > 0) {
                    final double stop = braking * grip * pull * TICK;
                    along = Math.abs(along) <= stop ? 0 : along - Math.signum(along) * stop;
                }
                if (ground.touching()) {
                    final double angle = VehicleMath.steerAngle(steer, along);
                    final double rate = VehicleMath.yawRate(along, angle, build.wheelbase(), grip);
                    vehicle.setYRot(yaw - (float) (rate * TICK));
                    sideways = VehicleMath.slide(sideways, grip, TICK);
                }
                up -= pull * TICK;
                lean(ground.pitch(), ground.roll());
                turnWheels(along, build);
            }
        }
        final Vec3 nowAhead = Vec3.directionFromRotation(0, vehicle.getYRot());
        final Vec3 nowLeft = new Vec3(nowAhead.z, 0, -nowAhead.x);
        final Vec3 going = nowAhead.scale(along).add(nowLeft.scale(sideways)).add(0, up, 0);
        vehicle.setDeltaMovement(going.scale(1 / TICKS));
        vehicle.move(MoverType.SELF, vehicle.getDeltaMovement());
        if (vehicle.horizontalCollision) {
            // Into a wall it stops along it, as far as the wall stops it; the server tells what that broke.
            final Vec3 stopped = vehicle.getDeltaMovement();
            vehicle.setDeltaMovement(stopped.x * 0.2, stopped.y, stopped.z * 0.2);
        }
        lastResult = result;
        return result;
    }

    /**
     * What its drivetrain gives and burns for {@code input} going at {@code along} metres a second, without moving it:
     * on the server, for a vehicle a player's own game drives.
     */
    VehicleDrive.Result account(final VehicleDef def, final VehicleBuild build, final DriverInput input,
                                final boolean running, final double along) {
        final double air = DimensionRulesData.of(vehicle.level()).at(vehicle.getY()).temperature();
        final VehicleDrive.Result result = switch (def.movement()) {
            case AIR -> fly(build, input, running, air);
            case WATER -> sail(build, input, running, air);
            case ROAD -> {
                if (build.engine().isEmpty()) {
                    yield new VehicleDrive.Result(0, 0, 0, 0, air, 0);
                }
                final boolean backing = input.forward() < 0 && along < STILL;
                final double throttle = backing ? -input.forward() : Math.max(0, input.forward());
                yield drive().step(build.engine().get().spec().drive(build.engine().get().health()),
                        build.gearbox(), build.drivenRadius(), new VehicleDrive.Controls(throttle, backing,
                                !vehicle.switches().manual(), vehicle.takeShift(), running), along, air, TICK);
            }
        };
        lastResult = result;
        return result;
    }

    /**
     * Leans it as its wheels stand and turns its steering as {@code input} asks, without moving it: on the server, for
     * a vehicle a player's own game drives, so every other player sees it lean and steer.
     */
    void follow(final VehicleDef def, final VehicleBuild build, final DriverInput input, final double along) {
        steer = Mth.clamp(input.strafe(), -1, 1);
        if (def.movement() == VehicleDef.Movement.ROAD) {
            final Ground ground = ground(vehicle.level(), build);
            lean(ground.pitch(), ground.roll());
            turnWheels(along, build);
        }
    }

    /* Turns its wheels by how far it rolled this tick, kept within a turn. */
    private void turnWheels(final double along, final VehicleBuild build) {
        wheelTurn = (wheelTurn + (float) Math.toDegrees(along * TICK / build.drivenRadius())) % 360;
    }

    /* The drivetrain it steps. */
    private VehicleDrive drive() {
        return vehicle.drivetrain();
    }

    /* What a flying vehicle's engine gives and burns, its rotors taking its power as they hold it up. */
    private VehicleDrive.Result fly(final VehicleBuild build, final DriverInput input, final boolean running,
                                    final double air) {
        if (build.engine().isEmpty()) {
            return new VehicleDrive.Result(0, 0, 0, 0, air, 0);
        }
        final VehicleDrive.Engine engine = build.engine().get().spec().drive(build.engine().get().health());
        final double working = running ? Math.clamp(0.6 + 0.4 * Math.abs(input.forward())
                + (input.up() ? 0.3 : 0), 0, 1) : 0;
        return burn(engine, working, running, air);
    }

    /* What a vehicle afloat's engine gives and burns, turning its propellers as the throttle asks. */
    private VehicleDrive.Result sail(final VehicleBuild build, final DriverInput input, final boolean running,
                                     final double air) {
        if (build.engine().isEmpty()) {
            return new VehicleDrive.Result(0, 0, 0, 0, air, 0);
        }
        final VehicleDrive.Engine engine = build.engine().get().spec().drive(build.engine().get().health());
        return burn(engine, running ? Math.abs(forwardThrottle(input)) : 0, running, air);
    }

    /* The engine working at {@code working}, its share of its top power, with no gearbox between it and its load. */
    private VehicleDrive.Result burn(final VehicleDrive.Engine engine, final double working, final boolean running,
                                     final double air) {
        final VehicleDrive.Result idle = drive().step(engine, VehicleDrive.Gearbox.SINGLE, 1,
                new VehicleDrive.Controls(0, false, true, 0, running), 0, air, TICK);
        if (!running) {
            return idle;
        }
        final double joules = engine.power() * working / engine.efficiency() * TICK + idle.joules();
        final double rpm = engine.idleRpm() + (engine.maxRpm() - engine.idleRpm()) * working;
        return new VehicleDrive.Result(0, joules, rpm, 0, idle.temperature(), working);
    }

    /* The throttle afloat: ahead, or half astern held backwards. */
    private static double forwardThrottle(final DriverInput input) {
        return input.forward() >= 0 ? input.forward() : input.forward() * 0.5;
    }

    /* Eases the body toward the lean its wheels give. */
    private void lean(final double towardPitch, final double towardRoll) {
        pitch += (float) ((towardPitch - pitch) * LEAN_EASE);
        roll += (float) ((towardRoll - roll) * LEAN_EASE);
    }

    /* The top of the water at its middle, or none out of water. */
    private double surface(final Level level) {
        final BlockPos at = BlockPos.containing(vehicle.getX(), vehicle.getY() + 0.5, vehicle.getZ());
        for (int dy = 1; dy >= -1; dy--) {
            final BlockPos pos = at.above(dy);
            final float height = level.getFluidState(pos).getHeight(level, pos);
            if (height > 0) {
                return pos.getY() + height;
            }
        }
        return Double.NaN;
    }

    /* The ground under its wheels: how high it stands under each, how it leans the body, and how slippery it is. */
    private Ground ground(final Level level, final VehicleBuild build) {
        final int count = build.wheels().size();
        if (drops.length != count) {
            drops = new double[count];
        }
        if (count == 0) {
            return new Ground(vehicle.onGround(), 0, 0, slipperiness(level));
        }
        final float yaw = vehicle.getYRot();
        final Vec3 ahead = Vec3.directionFromRotation(0, yaw);
        final Vec3 left = new Vec3(ahead.z, 0, -ahead.x);
        double front = 0;
        double back = 0;
        double port = 0;
        double starboard = 0;
        int fronts = 0;
        int backs = 0;
        int ports = 0;
        int starboards = 0;
        boolean touching = vehicle.onGround();
        for (int i = 0; i < count; i++) {
            final VehicleBuild.PartAt<VehiclePartDef.Wheel> wheel = build.wheels().get(i);
            final Vec3 at = wheel.place().at();
            final double x = vehicle.getX() + left.x * at.x + ahead.x * at.z;
            final double z = vehicle.getZ() + left.z * at.x + ahead.z * at.z;
            final double reach = wheel.spec().travel() + LOOK_BELOW;
            final double top = groundTop(level, x, vehicle.getY() + wheel.spec().radius(), z, reach);
            final double drop = Double.isNaN(top) ? reach : Math.clamp(vehicle.getY() - top, -wheel.spec().travel(),
                    reach);
            drops[i] = drop;
            if (drop < wheel.spec().travel() + 0.05) {
                touching = true;
            }
            if (at.z >= 0) {
                front += drop;
                fronts++;
            } else {
                back += drop;
                backs++;
            }
            if (at.x >= 0) {
                port += drop;
                ports++;
            } else {
                starboard += drop;
                starboards++;
            }
        }
        // The ground falling away under the front wheels tips the nose down; under the left wheels, the left side.
        final double rise = (backs > 0 ? back / backs : 0) - (fronts > 0 ? front / fronts : 0);
        final double tip = (ports > 0 ? port / ports : 0) - (starboards > 0 ? starboard / starboards : 0);
        return new Ground(touching, VehicleMath.lean(rise, build.wheelbase()),
                VehicleMath.lean(tip, Math.max(0.5, build.track())), slipperiness(level));
    }

    /* The top of what a wheel would stand on below {@code y} at {@code x, z}, within {@code reach}; none past it. */
    private double groundTop(final Level level, final double x, final double y, final double z, final double reach) {
        final AABB column = new AABB(x - 0.05, y - reach - 0.6, z - 0.05, x + 0.05, y, z + 0.05);
        double top = Double.NaN;
        for (final VoxelShape shape : level.getBlockCollisions(vehicle, column)) {
            final double max = shape.max(Direction.Axis.Y);
            if (max <= y && (Double.isNaN(top) || max > top)) {
                top = max;
            }
        }
        return top;
    }

    /* How slippery the block under its middle is. */
    private float slipperiness(final Level level) {
        final BlockPos below = BlockPos.containing(vehicle.getX(), vehicle.getY() - 0.2, vehicle.getZ());
        final BlockState state = level.getBlockState(below);
        return state.getFriction(level, below, vehicle);
    }

    /* The ground under it: whether a wheel touches it, the lean it gives, and how slippery it is. */
    private record Ground(boolean touching, double pitch, double roll, float slipperiness) {

        /* How well its wheels grip on it, the flat ones less. */
        double grip(final VehicleBuild build) {
            if (build.wheels().isEmpty()) {
                return VehicleMath.grip(1, slipperiness);
            }
            double sum = 0;
            for (final VehicleBuild.PartAt<VehiclePartDef.Wheel> wheel : build.wheels()) {
                sum += wheel.spec().grip() * (wheel.health() < 0.05 ? FLAT : 1);
            }
            return VehicleMath.grip(sum / build.wheels().size(), slipperiness);
        }
    }
}
