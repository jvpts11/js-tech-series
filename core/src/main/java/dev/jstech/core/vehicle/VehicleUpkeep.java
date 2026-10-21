/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructureEntity;
import dev.jstech.core.moving.StructureFooting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What the server keeps of a vehicle each tick, whoever drives it: the engine of one a player's own game drives,
 * worked out from where it went and what the player asks; the fuel it burns; the blow of running into a wall, worn into
 * the parts on the side that struck; what it runs over and pushes aside; a hard landing worn into its wheels; an engine
 * run too hot wearing itself; a holed tank leaking; and what its instruments read, told to every player. How it went is
 * read against the deck it stands on, when it stands on a moving structure: a van parked on a ferry goes nowhere.
 */
final class VehicleUpkeep {

    private final VehicleEntity vehicle;
    /* Where it stood the tick before; none until its first tick, when it has a place in the world. */
    private @Nullable Vec3 last;
    private double lastSpeed;
    private double lastFall;
    private boolean wasGrounded = true;
    private VehicleDrive.Result result = new VehicleDrive.Result(0, 0, 0, 1, Double.NaN, 0);

    private static final double TICKS = 20;
    /* A blow is a fall in speed of more than this in a tick against a wall, in metres a second. */
    private static final double BLOW = 4;
    /* What a part takes of a blow's energy to wear it out from new, in joules. */
    private static final double TOUGHNESS = 30_000;
    /* What it pushes aside above this speed, and runs over above the second, in metres a second. */
    private static final double PUSHES = 2;
    private static final double RUNS_OVER = 4;
    private static final double HURT_PER_SPEED = 2;
    /* A landing faster than this wears its wheels, by this much for each metre a second past it. */
    private static final double HARD_LANDING = 8;
    private static final float LANDING_WEAR = 0.06F;
    /* An engine past this heat wears itself each tick it stays there. */
    private static final double TOO_HOT = 120;
    private static final float HOT_WEAR = 0.001F;
    /* A tank worn below this leaks, faster as it wears. */
    private static final float LEAKS_BELOW = 0.3F;
    private static final int LEAK_EVERY = 20;
    /*
     * Farther than this in a tick, in blocks, is no drive but a jump: a command, a portal, a place set by hand. It
     * neither runs over what lies between nor lands hard.
     */
    private static final double JUMP = 20;

    VehicleUpkeep(final VehicleEntity vehicle) {
        this.vehicle = vehicle;
    }

    /** Wears every part of {@code vehicle} by {@code by}, as a fire or an explosion does. */
    static void wearAll(final VehicleEntity vehicle, final VehicleDef def, final float by) {
        for (final VehicleDef.Place place : def.places()) {
            vehicle.parts().wear(place.name(), by);
        }
        vehicle.partsChanged();
    }

    /** Burns what its engine used driving on the server, by a robot or by nobody. */
    void burn(final VehicleBuild build, final VehicleDrive.Result driven) {
        this.result = driven;
        vehicle.holds().ifPresent(held -> held.draw(driven.joules(), build.electric()));
    }

    /** One tick of keeping it, on the server. */
    void tick(final ServerLevel level, final VehicleDef def, final VehicleBuild build, final VehicleMotion motion) {
        final Vec3 now = vehicle.position();
        final Vec3 was = last == null ? now : last;
        final IStructureBody deck = StructureFooting.standingOn(vehicle);
        final Vec3 carried = deck == null ? Vec3.ZERO : StructureFooting.speedAt(deck, was);
        final Vec3 step = now.subtract(was).subtract(carried);
        if (step.lengthSqr() > JUMP * JUMP) {
            last = now;
            wasGrounded = vehicle.onGround();
            lastSpeed = 0;
            lastFall = 0;
            return;
        }
        final Vec3 moved = step.scale(TICKS);
        final Vec3 ahead = Vec3.directionFromRotation(0, vehicle.getYRot());
        final double along = moved.dot(ahead);
        if (vehicle.getControllingPassenger() instanceof ServerPlayer && !vehicle.isControlledByLocalInstance()
                && vehicle.towedBy() == null) {
            // The player's own game moved it; its engine is worked out here from where it went and what they ask.
            final DriverInput input = vehicle.input();
            result = motion.account(def, build, input, vehicle.running(), along);
            vehicle.holds().ifPresent(held -> held.draw(result.joules(), build.electric()));
            motion.follow(def, build, input, along);
        }
        final double speed = Math.sqrt(moved.x * moved.x + moved.z * moved.z);
        if (vehicle.horizontalCollision && lastSpeed - speed > BLOW) {
            blow(def, lastSpeed - speed, Math.signum(along == 0 ? lastSpeed : along), build);
        }
        if (speed > PUSHES) {
            runInto(level, moved, speed, build);
        }
        if (vehicle.onGround() && !wasGrounded && lastFall > HARD_LANDING) {
            wearWheels(def, (float) ((lastFall - HARD_LANDING) * LANDING_WEAR));
        }
        if (result.temperature() > TOO_HOT) {
            build.engine().ifPresent(engine -> vehicle.parts().wear(engine.place().name(), HOT_WEAR));
            vehicle.partsChanged();
        }
        if (vehicle.tickCount % LEAK_EVERY == 0) {
            leak(def);
        }
        vehicle.report(result, vehicle.holds().map(held -> held.fullness(build.electric())).orElse(0F));
        wasGrounded = vehicle.onGround();
        lastFall = Math.max(0, -moved.y);
        lastSpeed = speed;
        last = now;
    }

    /* A blow against a wall, losing {@code lost} metres a second: its energy shared out over the parts on that side. */
    private void blow(final VehicleDef def, final double lost, final double forward, final VehicleBuild build) {
        final double energy = 0.5 * build.mass() * lost * lost;
        final List<VehicleDef.Place> fitted = new ArrayList<>();
        for (final VehicleDef.Place place : def.places()) {
            if (vehicle.parts().at(place.name()).isPresent()) {
                fitted.add(place);
            }
        }
        final double[] along = new double[fitted.size()];
        for (int i = 0; i < along.length; i++) {
            along[i] = fitted.get(i).at().z * forward;
        }
        final double[] shares = VehicleMath.shares(along);
        for (int i = 0; i < shares.length; i++) {
            vehicle.parts().wear(fitted.get(i).name(), (float) (energy * shares[i] / TOUGHNESS));
        }
        vehicle.partsChanged();
    }

    /* Pushes aside what stands in its way, and runs over what it hits fast. */
    private void runInto(final ServerLevel level, final Vec3 moved, final double speed, final VehicleBuild build) {
        final Vec3 way = moved.normalize();
        final AABB swept = vehicle.getBoundingBox().inflate(0.1).expandTowards(moved.scale(1 / TICKS));
        final Entity driver = vehicle.getControllingPassenger();
        for (final Entity hit : level.getEntities(vehicle, swept, other -> !other.isPassengerOfSameVehicle(vehicle)
                && !vehicle.hasPassenger(other) && !(other instanceof MovingStructureEntity) && !other.isSpectator()
                && !hitched(other))) {
            final double closing = moved.subtract(hit.getDeltaMovement().scale(TICKS)).dot(way);
            if (closing <= PUSHES) {
                continue;
            }
            hit.push(way.x * closing * 0.05, 0.1, way.z * closing * 0.05);
            if (closing > RUNS_OVER && hit instanceof LivingEntity living) {
                final DamageSource source = Vehicles.runOver(level, vehicle, driver);
                living.hurt(source, (float) ((closing - RUNS_OVER) * HURT_PER_SPEED));
            }
            // What it hits slows it, the more the heavier the thing is beside it.
            final double share = Math.min(1, hit.getBbWidth() * hit.getBbHeight() * 100 / Math.max(1, build.mass()));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().scale(1 - 0.5 * share));
        }
    }

    /* Whether {@code other} is hitched to it, ahead or behind. */
    private boolean hitched(final Entity other) {
        return other instanceof VehicleEntity towing && (towing.towedBy() == vehicle || vehicle.towedBy() == towing);
    }

    private void wearWheels(final VehicleDef def, final float by) {
        for (final VehicleDef.Place place : def.places()) {
            if (place.kind() == VehiclePartKind.WHEEL) {
                vehicle.parts().wear(place.name(), by);
            }
        }
        vehicle.partsChanged();
    }

    /* A worn tank leaks what it holds, faster the more it is worn. */
    private void leak(final VehicleDef def) {
        for (final Map.Entry<String, FittedParts.Fitted> each : vehicle.parts().all().entrySet()) {
            final VehicleDef.Place place = def.place(each.getKey()).orElse(null);
            final float health = each.getValue().health();
            if (place == null || place.kind() != VehiclePartKind.TANK || health >= LEAKS_BELOW) {
                continue;
            }
            final int lost = 1 + (int) ((LEAKS_BELOW - health) * 100);
            vehicle.holds().ifPresent(held -> held.liquid().drain(lost, IFluidHandler.FluidAction.EXECUTE));
        }
    }
}
