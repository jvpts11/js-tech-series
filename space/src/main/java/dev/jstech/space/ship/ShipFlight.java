/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMotions;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.rocket.SpaceAhead;
import dev.jstech.space.universe.FarMaps;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.SystemSpaces;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A small ship that has left its dock, flown as a moving structure of J's Core a tick at a time by
 * {@link ShipFlightModel}: its blocks fly as they stand, its machines work, its crew walk about inside. Its pilot at
 * the helm, the cockpit's pilot seat, opens and closes its throttle, turns it and pitches its nose, and it goes where
 * its nose points; its wings hold it up in the air of the world it flies in, as thick as that world's air is at its
 * height, slower its nozzles turn down and it hovers, rising and coming down as its pilot holds, and its boost takes
 * it faster across the empty stretches. With nobody at the helm its throttle closes and it comes down gently. It rests
 * on the land under it, rolling on wheels or standing on feet, and is stopped by the land it would fly into
 * ({@link ShipGround}); the land ahead of it is loaded as it goes, as a rocket's is, so it climbs to space and crosses
 * into it without stopping. Its console shows its flight ({@link ShipReadings}) and keeps its fusion reactor's
 * deuterium, burnt as it flies: with none left, the reactor makes nothing, and its boost fills no more. Its rule goes
 * on through a save and into another dimension, as the structure does.
 *
 * @param state    where its flight stands
 * @param noseX    which way its nose points among its blocks, across, as its plan laid it
 * @param noseZ    which way its nose points among its blocks, along
 * @param ship     what it weighs and pushes, and what it stands on
 * @param grounded whether it stood on the land the tick before
 * @param console  where its flight console stands from its pivot's block, if it has one: so wherever a yard puts its
 *                 blocks, in this dimension or another
 * @param autopilot its autopilot engaged, if one is ({@link ShipAutopilots})
 */
public record ShipFlight(ShipFlightModel.State state, int noseX, int noseZ, ShipFlightModel.Performance ship,
                         boolean grounded, Optional<BlockPos> console, Optional<ShipAutopilot> autopilot)
        implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_flight");
    private static final MapCodec<ShipFlightModel.State> STATE = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.DOUBLE.fieldOf("speed").forGetter(ShipFlightModel.State::speed),
                    Codec.DOUBLE.fieldOf("sink").forGetter(ShipFlightModel.State::sink),
                    Codec.DOUBLE.fieldOf("throttle").forGetter(ShipFlightModel.State::throttle),
                    Codec.DOUBLE.fieldOf("charge").forGetter(ShipFlightModel.State::charge),
                    Codec.DOUBLE.optionalFieldOf("rise", 0.0).forGetter(ShipFlightModel.State::rise),
                    Codec.DOUBLE.optionalFieldOf("nozzles", 0.0).forGetter(ShipFlightModel.State::nozzles))
            .apply(instance, ShipFlightModel.State::new));
    private static final Codec<ShipFlightModel.Gear> GEAR = StableCodecs.byName(ShipFlightModel.Gear.class);
    private static final MapCodec<ShipFlightModel.Performance> PERFORMANCE = RecordCodecBuilder.mapCodec(
            instance -> instance.group(Codec.DOUBLE.fieldOf("mass").forGetter(ShipFlightModel.Performance::mass),
                    Codec.DOUBLE.fieldOf("air_thrust").forGetter(ShipFlightModel.Performance::airThrust),
                    Codec.DOUBLE.fieldOf("bell_thrust").forGetter(ShipFlightModel.Performance::bellThrust),
                    Codec.DOUBLE.fieldOf("wing").forGetter(ShipFlightModel.Performance::wing),
                    Codec.DOUBLE.fieldOf("power").forGetter(ShipFlightModel.Performance::power),
                    GEAR.optionalFieldOf("gear", ShipFlightModel.Gear.NONE)
                            .forGetter(ShipFlightModel.Performance::gear))
                    .apply(instance, ShipFlightModel.Performance::new));
    public static final MapCodec<ShipFlight> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            STATE.forGetter(ShipFlight::state),
            Codec.INT.fieldOf("nose_x").forGetter(ShipFlight::noseX),
            Codec.INT.fieldOf("nose_z").forGetter(ShipFlight::noseZ),
            PERFORMANCE.fieldOf("ship").forGetter(ShipFlight::ship),
            Codec.BOOL.optionalFieldOf("grounded", false).forGetter(ShipFlight::grounded),
            BlockPos.CODEC.optionalFieldOf("console").forGetter(ShipFlight::console),
            ShipAutopilot.CODEC.optionalFieldOf("autopilot").forGetter(ShipFlight::autopilot)
    ).apply(instance, ShipFlight::new));
    /* The overworld's air at sea level, in kilopascals, what its air's thickness is told against. */
    private static final double SEA_PRESSURE = 101.3;
    /* How far over the land a stretch is empty for the boost, in blocks. */
    private static final int EMPTY_OVER = 24;
    /* Below this, in metres a second or blocks, nothing moves. */
    private static final double STILL = 1.0e-4;
    /* How high over the land a ship over land not loaded is taken to be, in blocks: far. */
    private static final double FAR = 1000;
    /* How far ahead the land is loaded, in ticks of its flight, and from what speed, in metres a second. */
    private static final int AHEAD = 40;
    private static final double LOADS_FROM = 1.0;
    /* How often along its way the land is held, in blocks: half a chunk, so no chunk the way crosses is missed. */
    private static final double HELD_EVERY = 8;
    /* How long a full load of deuterium keeps its reactor going in flight, in seconds: four hours. */
    private static final double REACTOR_LOAD_SECONDS = 4 * 3600;
    /* What the autopilot holds docking: its throttle closing. */
    private static final ShipFlightModel.Held BRAKING = new ShipFlightModel.Held(0, 1, 0, 0, 0, 0, false, true);
    /* Below this, in metres a second, a ship has set down, or stopped to dock; how near its spot LAND sets it down. */
    private static final double LANDED_SPEED = 0.5;
    private static final double ARRIVED = 3;
    /* How far ahead along the climb ORBIT's caret stands, in blocks. */
    private static final double CARET_AHEAD = 60;
    /* How fast it turns docking, and rolls back level, in degrees a second; within what of level it is left be. */
    private static final double DOCK_TURNS = 20;
    private static final double LEVELS = 20;
    private static final double LEVEL_WITHIN = 0.2;
    /* Below this, its nose straight up or down, which way is level is not told, and it is left be. */
    private static final double UPRIGHT_NOSE = 0.05;

    /** Registers the rule's kind, once, from the mod's constructor. */
    public static void declare() {
        StructureMotions.register(TYPE, CODEC);
    }

    /**
     * A ship of {@code ship} at rest, its nose pointing {@code noseX}, {@code noseZ} among its blocks, its console at
     * {@code console} from its pivot's block ({@link #fromPivot}).
     */
    public static ShipFlight resting(final int noseX, final int noseZ, final ShipFlightModel.Performance ship,
                                     final Optional<BlockPos> console) {
        return new ShipFlight(ShipFlightModel.State.RESTING, noseX, noseZ, ship, true, console, Optional.empty());
    }

    /**
     * What a ship of {@code design} weighs and pushes, by its parts' figures in {@code parts}, and what it stands on:
     * its air engines push what they lift, its reactor's power beyond what its parts take fills its boost, its gear
     * rolls or stands.
     */
    public static ShipFlightModel.Performance performance(final ShipDesign design,
                                                          final Map<ResourceLocation, ShipPart> parts) {
        final List<ResourceLocation> all = new ArrayList<>(design.spine());
        all.addAll(design.hung());
        double mass = 0;
        double air = 0;
        double bells = 0;
        double wing = 0;
        double power = 0;
        ShipFlightModel.Gear gear = ShipFlightModel.Gear.NONE;
        for (final ResourceLocation id : all) {
            final ShipPart part = parts.get(id);
            if (part == null) {
                continue;
            }
            final ShipPart.Figures figures = part.figures();
            mass += figures.mass();
            air += part.kind() == ShipPartKind.AIR_ENGINE ? figures.lift() : 0;
            bells += figures.thrust();
            wing += figures.wing();
            power += figures.power();
            if (part.kind() == ShipPartKind.GEAR) {
                gear = part.rolls() ? ShipFlightModel.Gear.WHEELS : ShipFlightModel.Gear.LEGS;
            }
        }
        return new ShipFlightModel.Performance(mass, air, bells, wing, power, gear);
    }

    /** Where the block {@code yard} of {@code body}'s yard stands from its pivot's block, as its console is kept. */
    public static BlockPos fromPivot(final MovingStructure body, final BlockPos yard) {
        return yard.subtract(pivot(body));
    }

    /** Its flight as it stands, another way. */
    public ShipFlight with(final ShipFlightModel.State next) {
        return new ShipFlight(next, this.noseX, this.noseZ, this.ship, this.grounded, this.console, this.autopilot);
    }

    /** Its flight with {@code pilot} engaged, or none. */
    public ShipFlight engaged(final Optional<ShipAutopilot> pilot) {
        return new ShipFlight(this.state, this.noseX, this.noseZ, this.ship, this.grounded, this.console, pilot);
    }

    /** Where its console stands in {@code body}'s yard now, if it has one. */
    public Optional<BlockPos> consoleIn(final MovingStructure body) {
        return this.console.map(from -> pivot(body).offset(from));
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        if (!(context.world() instanceof ServerLevel level) || !(context.structure() instanceof MovingStructure body)) {
            return new Step(now, this);
        }
        if (ShipDocks.docked(body)) {
            // Docked, it goes where the ship it is docked to goes, held where it stands on it: its own flight still.
            return new Step(now, this);
        }
        final StructureControls controls = body.controls();
        final long tick = level.getGameTime();
        final boolean piloted = controls.piloted(tick) && controls.live(tick);
        final ShipFlightModel.Held helm = !piloted ? ShipFlightModel.Held.NOBODY : new ShipFlightModel.Held(
                controls.value(StructureControls.ControlAxis.FORWARD, tick),
                controls.value(StructureControls.ControlAxis.BACK, tick),
                controls.value(StructureControls.ControlAxis.LEFT, tick),
                controls.value(StructureControls.ControlAxis.RIGHT, tick),
                controls.value(StructureControls.ControlAxis.UP, tick),
                controls.value(StructureControls.ControlAxis.DOWN, tick), ShipBoost.held(body, tick), true);
        final StructureFrame frame = context.frame();
        final double[] nose = nose(frame);
        final double pitch = Math.toDegrees(Math.asin(Math.clamp(nose[1], -1.0, 1.0)));
        final double heading = heading(nose);
        // Its reactor burns its deuterium as it flies; with none left it makes nothing, its boost filling no more.
        final AirReserveEntity keeper = keeper(level, body);
        final boolean fuelled = keeper == null || (this.grounded ? keeper.deuterium() > 0
                : keeper.burn(ShipFlightModel.TICK / REACTOR_LOAD_SECONDS));
        final ShipFlightModel.Performance running = fuelled ? this.ship : new ShipFlightModel.Performance(
                this.ship.mass(), this.ship.airThrust(), this.ship.bellThrust(), this.ship.wing(),
                Math.min(0, this.ship.power()), this.ship.gear());
        // Its pilot taking the helm, holding any of its keys, lets its autopilot go; so does an autopilot done.
        final Optional<ShipAutopilot> pilot = this.autopilot.filter(engaged -> !holding(helm)
                && !done(level, now, engaged));
        final ShipFlight self = pilot.equals(this.autopilot) ? this : engaged(pilot);
        final int altitude = ShipAltitude.of(level, now.x(), body.worldBox().minY, now.z());
        if (pilot.isPresent() && pilot.get().mode() == ShipAutopilot.Mode.DOCK) {
            return self.docking(level, body, now, pilot.get(), pitch, running, keeper, altitude);
        }
        ShipFlightModel.Held held = helm;
        ShipReadings.Pilot shown = ShipReadings.Pilot.NONE;
        if (pilot.isPresent() && pilot.get().mode() == ShipAutopilot.Mode.LAND) {
            final Vec3 spot = pilot.get().spot().orElseThrow();
            held = ShipAutopilots.land(this.state, heading, pitch, spot.x - now.x(), spot.y - body.worldBox().minY,
                    spot.z - now.z());
            shown = ShipReadings.Pilot.of(ShipAutopilot.Mode.LAND, spot, Math.hypot(spot.x - now.x(),
                    spot.z - now.z()), this.state.speed(), false, 0, 0);
        } else if (pilot.isPresent()) {
            held = ShipAutopilots.orbit(this.state, pitch);
            shown = climbing(level, now, nose, altitude, this.state.speed() * nose[1]);
        }
        final ShipFlightModel.Tick next = ShipFlightModel.step(this.state, held, air(level, now, body), running,
                pitch);
        if (keeper != null) {
            keeper.flew(ShipReadings.of(next, altitude, shown));
        }
        return self.flownOn(level, body, now, frame, next);
    }

    /** Which way {@code nose} points, in degrees east of north, as a compass reads it. */
    public static double heading(final double[] nose) {
        return Math.floorMod(Math.round(Math.toDegrees(Math.atan2(nose[0], -nose[2])) * 1000), 360_000) / 1000.0;
    }

    /** Which way its nose points in the world, standing as {@code frame}, as a unit vector. */
    public double[] noseIn(final StructureFrame frame) {
        return nose(frame);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    /* The rule reads its pilot's controls, which only the structure itself holds, and the land and air round it. */
    @Override
    public boolean readsWorld() {
        return true;
    }

    /* Its tick on from {@code now}, as {@code next} turns, pitches and flies it, the land ahead loaded first. */
    private Step flownOn(final ServerLevel level, final MovingStructure body, final StructurePose now,
                         final StructureFrame frame, final ShipFlightModel.Tick next) {
        StructurePose turned = next.turn() == 0 ? now : now.turnedBy(0, 1, 0, Math.toRadians(next.turn()));
        if (next.pitch() != 0) {
            final double[] way = nose(new StructureFrame(turned, frame.pivotX(), frame.pivotY(), frame.pivotZ()));
            final double across = Math.hypot(way[0], way[2]);
            if (across > STILL) {
                turned = turned.turnedBy(-way[2] / across, 0, way[0] / across, Math.toRadians(next.pitch()));
            }
        }
        turned = levelled(turned, frame);
        final double[] way = nose(new StructureFrame(turned, frame.pivotX(), frame.pivotY(), frame.pivotZ()));
        final double ahead = next.state().speed() * ShipFlightModel.TICK;
        final double up = (next.state().rise() - next.state().sink()) * ShipFlightModel.TICK;
        if (next.state().speed() > LOADS_FROM) {
            holdAhead(level, now, way, ahead * AHEAD);
        }
        if (!SpaceAhead.ready(level, now.x() + way[0] * ahead, now.y(), now.z() + way[2] * ahead)) {
            // Land not going yet would freeze it there: it waits on its way, as it flew, till the land goes.
            return new Step(now, flying(next.state(), this.grounded));
        }
        return flown(level, body, now, turned, way[0] * ahead, way[1] * ahead + up, way[2] * ahead, next.state());
    }

    /*
     * A tick of DOCK: its throttle closed, it slows along its nose; stopped, it drifts on its thrusters till its
     * collar stands off the other ship's, in line with it, then closes till they touch, and holds there, turning all
     * the while to face the other's collar, collar to collar, nose by nose; the other gone, or either with no collar,
     * it lets the autopilot go.
     */
    private Step docking(final ServerLevel level, final MovingStructure body, final StructurePose now,
                         final ShipAutopilot pilot, final double pitch, final ShipFlightModel.Performance running,
                         final @Nullable AirReserveEntity keeper, final int altitude) {
        final MovingStructure other = pilot.other().map(id -> MovingStructures.byId(level, id)).orElse(null);
        final Optional<Vec3> ours = ShipCollars.mouth(level, body);
        final Optional<Vec3> theirs = other == null || !(other.motion() instanceof ShipFlight)
                ? Optional.empty() : ShipCollars.mouth(level, other);
        if (ours.isEmpty() || theirs.isEmpty()) {
            return new Step(now, engaged(Optional.empty()));
        }
        final ShipFlight them = (ShipFlight) other.motion();
        final StructureFrame their = other.frame();
        final double[] axis = {their.turnX(0, 1, 0), their.turnY(0, 1, 0), their.turnZ(0, 1, 0)};
        final StructurePose facing = facing(now, them, their);
        final double off = now.angleTo(facing);
        final Vec3 offset = theirs.get().subtract(ours.get());
        final Vec3 was = pilot.drift();
        final boolean stopped = this.state.speed() < LANDED_SPEED;
        final ShipAutopilots.Docking docking = ShipAutopilots.dock(new double[] {offset.x, offset.y, offset.z}, axis,
                Math.toDegrees(off), new double[] {was.x, was.y, was.z});
        if (docking.touching() && docking.aligned()) {
            // Collar to collar, in line: the two are made one, its autopilot done ({@link ShipDocks}).
            ShipDocks.join(level, body, other);
        }
        final Vec3 drift = stopped ? new Vec3(docking.drift()[0], docking.drift()[1], docking.drift()[2]) : was;
        final ShipFlightModel.Tick next = ShipFlightModel.step(this.state, BRAKING, air(level, now, body), running,
                pitch);
        if (keeper != null) {
            final double closing = offset.length() < STILL ? 0 : drift.dot(offset.normalize());
            keeper.flew(ShipReadings.of(next, altitude, ShipReadings.Pilot.of(ShipAutopilot.Mode.DOCK,
                    theirs.get(), offset.length(), closing, docking.aligned(), 0, 0)));
        }
        final StructurePose turned = off < STILL ? now
                : StructurePose.between(now, facing, Math.min(1, Math.toRadians(DOCK_TURNS) * ShipFlightModel.TICK
                / off));
        final double[] nose = nose(frameOf(body, now));
        final double ahead = next.state().speed() * ShipFlightModel.TICK;
        final StructurePose moved = turned.movedTo(now.x() + nose[0] * ahead + drift.x * ShipFlightModel.TICK,
                now.y() + nose[1] * ahead + drift.y * ShipFlightModel.TICK,
                now.z() + nose[2] * ahead + drift.z * ShipFlightModel.TICK);
        return engaged(Optional.of(pilot.drifting(drift))).still(now, moved, next.state(), false);
    }

    /*
     * How it stands, where it stands {@code now}, facing {@code them}, standing as {@code their}: its collar's top
     * turned to the other's, so the two face each other, and its nose the way the other's points.
     */
    private StructurePose facing(final StructurePose now, final ShipFlight them, final StructureFrame their) {
        final double yaw = Math.atan2(them.noseX, them.noseZ) - Math.atan2(this.noseX, this.noseZ);
        final StructurePose over = StructurePose.turned(now.x(), now.y(), now.z(), 0, 1, 0, yaw)
                .then(StructurePose.turned(0, 0, 0, them.noseX, 0, them.noseZ, Math.PI));
        final StructurePose at = their.pose();
        return over.then(new StructurePose(0, 0, 0, at.qx(), at.qy(), at.qz(), at.qw()));
    }

    /*
     * Its flight turned {@code turned}, rolled back towards level about its nose: only docking rolls it, and once it
     * flies on its own it comes back upright, as a pilot rights a ship before setting it down.
     */
    private StructurePose levelled(final StructurePose turned, final StructureFrame frame) {
        final StructureFrame at = new StructureFrame(turned, frame.pivotX(), frame.pivotY(), frame.pivotZ());
        final double[] n = nose(at);
        final double[] level = {-n[1] * n[0], 1 - n[1] * n[1], -n[1] * n[2]};
        final double length = Math.sqrt(level[0] * level[0] + level[1] * level[1] + level[2] * level[2]);
        if (length < UPRIGHT_NOSE) {
            return turned;
        }
        final double ux = at.turnX(0, 1, 0);
        final double uy = at.turnY(0, 1, 0);
        final double uz = at.turnZ(0, 1, 0);
        final double lx = level[0] / length;
        final double ly = level[1] / length;
        final double lz = level[2] / length;
        final double sin = (uy * lz - uz * ly) * n[0] + (uz * lx - ux * lz) * n[1] + (ux * ly - uy * lx) * n[2];
        final double roll = Math.atan2(sin, ux * lx + uy * ly + uz * lz);
        if (Math.abs(roll) < Math.toRadians(LEVEL_WITHIN)) {
            return turned;
        }
        final double by = Math.copySign(Math.min(Math.abs(roll), Math.toRadians(LEVELS) * ShipFlightModel.TICK),
                roll);
        return turned.turnedBy(n[0], n[1], n[2], by);
    }

    /*
     * Whether the autopilot {@code pilot} is done, the ship standing at {@code now}: LAND set down on its spot, not
     * only on the land, as it stood before it took off; ORBIT over the line in space.
     */
    private boolean done(final ServerLevel level, final StructurePose now, final ShipAutopilot pilot) {
        return switch (pilot.mode()) {
            case LAND -> this.grounded && this.state.speed() < LANDED_SPEED && pilot.spot().map(spot -> Math.hypot(
                    spot.x - now.x(), spot.z - now.z()) <= ARRIVED).orElse(true);
            case ORBIT -> SystemSpaces.systemOf(level.dimension()).isPresent();
            case DOCK -> false;
        };
    }

    /*
     * What ORBIT shows: its caret on the climb ahead, how high the climb tops out over the sea, where its world
     * becomes its space, and how long till then at the rate it climbs.
     */
    private static ShipReadings.Pilot climbing(final ServerLevel level, final StructurePose now, final double[] nose,
                                               final int altitude, final double climb) {
        final double across = Math.hypot(nose[0], nose[2]);
        final double up = Math.sin(Math.toRadians(ShipAutopilots.CLIMB_PITCH));
        final double flat = Math.cos(Math.toRadians(ShipAutopilots.CLIMB_PITCH));
        final Vec3 ahead = across < STILL ? new Vec3(now.x(), now.y() + CARET_AHEAD, now.z()) : new Vec3(now.x()
                + nose[0] / across * flat * CARET_AHEAD, now.y() + up * CARET_AHEAD, now.z() + nose[2] / across
                * flat * CARET_AHEAD);
        final int apogee = SpaceClimbs.ceiling(level) - FarMaps.seaOf(level);
        final int seconds = climb > STILL && altitude != ShipAltitude.NONE
                ? (int) Math.round(Math.max(0, apogee - altitude) / climb) : 0;
        return ShipReadings.Pilot.of(ShipAutopilot.Mode.ORBIT, ahead, 0, climb, false, apogee, seconds);
    }

    /* Whether its pilot holds any of the helm's keys, taking the helm back from the autopilot. */
    private static boolean holding(final ShipFlightModel.Held held) {
        return held.forward() > 0 || held.back() > 0 || held.left() > 0 || held.right() > 0 || held.up() > 0
                || held.down() > 0 || held.boost();
    }

    /*
     * The step that takes {@code body} from {@code now}, turned to {@code turned}, on by {@code dx, dy, dz}: resting on
     * the land it comes down on, stepping up what is low enough, and stopped short by the land it would fly into.
     */
    private Step flown(final ServerLevel level, final MovingStructure body, final StructurePose now,
                       final StructurePose turned, final double dx, final double dy, final double dz,
                       final ShipFlightModel.State next) {
        // Coming down, it may rise back as far as it came down and a step, to stand on the land it came down on.
        final double most = ShipGround.STEP + Math.max(0, -dy);
        final StructurePose moved = turned.movedTo(now.x() + dx, now.y() + dy, now.z() + dz);
        final ShipGround.Contact meets = ShipGround.meet(level, body, frameOf(body, moved), most);
        if (!meets.blocked()) {
            return still(now, moved.movedTo(moved.x(), moved.y() + meets.rise(), moved.z()),
                    meets.touching() ? stood(next) : next, meets.touching());
        }
        if (dy >= 0 && ShipGround.meet(level, body, frameOf(body, now)).blocked()) {
            // Already in the land where it stands, built into it or set there: it flies on out of it, never deeper.
            return still(now, moved, next, false);
        }
        // Run into land: it stops where it was, only coming down onto what is under it, or not at all.
        final ShipFlightModel.State stopped = new ShipFlightModel.State(0, next.sink(), next.throttle(),
                next.charge(), next.rise(), next.nozzles());
        final StructurePose dropped = turned.movedTo(now.x(), now.y() + Math.min(0, dy), now.z());
        final ShipGround.Contact under = ShipGround.meet(level, body, frameOf(body, dropped), most);
        if (!under.blocked()) {
            return still(now, dropped.movedTo(dropped.x(), dropped.y() + under.rise(), dropped.z()),
                    under.touching() ? stood(stopped) : stopped, under.touching());
        }
        return new Step(now, flying(stood(stopped), true));
    }

    /* The step to {@code to}, or none where it barely moved: a ship at rest sends nothing. */
    private Step still(final StructurePose now, final StructurePose to, final ShipFlightModel.State next,
                       final boolean landed) {
        if (to.distanceTo(now) < STILL && to.angleTo(now) < STILL) {
            return new Step(now, flying(next, landed));
        }
        return new Step(to, flying(next, landed));
    }

    /* Its flight standing as {@code next}, on the land or not. */
    private ShipFlight flying(final ShipFlightModel.State next, final boolean landed) {
        return new ShipFlight(next, this.noseX, this.noseZ, this.ship, landed, this.console, this.autopilot);
    }

    /* Its console, which shows its flight and keeps its reactor's deuterium, where its yard's land is loaded. */
    private @Nullable AirReserveEntity keeper(final ServerLevel level, final MovingStructure body) {
        final Optional<BlockPos> at = consoleIn(body);
        return at.isPresent() && level.isLoaded(at.get())
                && level.getBlockEntity(at.get()) instanceof AirReserveEntity keeper ? keeper : null;
    }

    /*
     * The land on its way loads before it gets there, and stays going while it passes: the chunks round each one from
     * where it stands {@code far} blocks on along {@code way}, so every block and seat of it keeps going, not only its
     * middle; a seat in a chunk not going would stop there, its sitter left behind.
     */
    private static void holdAhead(final ServerLevel level, final StructurePose now, final double[] way,
                                  final double far) {
        for (double along = 0; along < far + HELD_EVERY; along += HELD_EVERY) {
            final double at = Math.min(far, along);
            SpaceAhead.holdAround(level, now.x() + way[0] * at, now.y(), now.z() + way[2] * at);
        }
    }

    /* Its flight standing on the land: it sinks no more and comes down no further. */
    private static ShipFlightModel.State stood(final ShipFlightModel.State next) {
        return new ShipFlightModel.State(next.speed(), 0, next.throttle(), next.charge(), Math.max(0, next.rise()),
                next.nozzles());
    }

    /*
     * The air round {@code body} standing at {@code now}: its world's at that height, whether it is empty, whether it
     * stood on the land, and how high over the land it is.
     */
    private ShipFlightModel.Air air(final ServerLevel level, final StructurePose now, final MovingStructure body) {
        final DimensionRules rules = DimensionRulesData.of(level);
        final Climate climate = rules.at(now.y());
        final double gravity = rules.gravityAt(now.y());
        final double density = climate.pressure() / SEA_PRESSURE;
        final BlockPos under = BlockPos.containing(now.x(), now.y(), now.z());
        final double height = level.isLoaded(under) ? body.worldBox().minY - level.getHeight(
                Heightmap.Types.MOTION_BLOCKING, under.getX(), under.getZ()) : FAR;
        final boolean empty = density <= 0 || gravity <= 0 || height >= EMPTY_OVER;
        return new ShipFlightModel.Air(density, gravity, empty, this.grounded, height);
    }

    /* Which way its nose points in the world, standing as {@code frame}. */
    private double[] nose(final StructureFrame frame) {
        final double x = frame.turnX(this.noseX, 0, this.noseZ);
        final double y = frame.turnY(this.noseX, 0, this.noseZ);
        final double z = frame.turnZ(this.noseX, 0, this.noseZ);
        final double length = Math.sqrt(x * x + y * y + z * z);
        return length < STILL ? new double[] {0, 0, -1} : new double[] {x / length, y / length, z / length};
    }

    private static StructureFrame frameOf(final MovingStructure body, final StructurePose pose) {
        return new StructureFrame(pose, body.pivotX(), body.pivotY(), body.pivotZ());
    }

    /* The block of {@code body}'s yard its pivot is in: a yard moves it with its blocks, whole blocks at a time. */
    private static BlockPos pivot(final MovingStructure body) {
        return BlockPos.containing(body.pivotX(), body.pivotY(), body.pivotZ());
    }
}
