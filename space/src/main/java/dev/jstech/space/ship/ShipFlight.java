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
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMotions;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.rocket.SpaceAhead;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A small ship that has left its dock, flown as a moving structure of J's Core a tick at a time by
 * {@link ShipFlightModel}: its blocks fly as they stand, its machines work, its crew walk about inside. Its pilot at
 * the helm, the cockpit's pilot seat, opens and closes its throttle, turns it and pitches its nose, and it goes where
 * its nose points; its wings hold it up in the air of the world it flies in, as thick as that world's air is at its
 * height, slower its nozzles turn down and it hovers, rising and coming down as its pilot holds, and its boost takes
 * it faster across the empty stretches. With nobody at the helm its throttle closes and it comes down gently. It rests
 * on the land under it, rolling on wheels or standing on feet, and is stopped by the land it would fly into
 * ({@link ShipGround}); the land ahead of it is loaded as it goes, as a rocket's is, so it climbs to space and crosses
 * into it without stopping. Its console shows its flight ({@link ShipReadings}). Its rule goes on through a save and
 * into another dimension, as the structure does.
 *
 * @param state    where its flight stands
 * @param noseX    which way its nose points among its blocks, across, as its plan laid it
 * @param noseZ    which way its nose points among its blocks, along
 * @param ship     what it weighs and pushes, and what it stands on
 * @param grounded whether it stood on the land the tick before
 * @param console  where its flight console stands from its pivot's block, if it has one: so wherever a yard puts its
 *                 blocks, in this dimension or another
 */
public record ShipFlight(ShipFlightModel.State state, int noseX, int noseZ, ShipFlightModel.Performance ship,
                         boolean grounded, Optional<BlockPos> console) implements IStructureMotion {

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
            BlockPos.CODEC.optionalFieldOf("console").forGetter(ShipFlight::console)
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
        return new ShipFlight(ShipFlightModel.State.RESTING, noseX, noseZ, ship, true, console);
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
        return new ShipFlight(next, this.noseX, this.noseZ, this.ship, this.grounded, this.console);
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
        final StructureControls controls = body.controls();
        final long tick = level.getGameTime();
        final boolean piloted = controls.piloted(tick) && controls.live(tick);
        final ShipFlightModel.Held held = !piloted ? ShipFlightModel.Held.NOBODY : new ShipFlightModel.Held(
                controls.value(StructureControls.ControlAxis.FORWARD, tick),
                controls.value(StructureControls.ControlAxis.BACK, tick),
                controls.value(StructureControls.ControlAxis.LEFT, tick),
                controls.value(StructureControls.ControlAxis.RIGHT, tick),
                controls.value(StructureControls.ControlAxis.UP, tick),
                controls.value(StructureControls.ControlAxis.DOWN, tick), ShipBoost.held(body, tick), true);
        final StructureFrame frame = context.frame();
        final double[] nose = nose(frame);
        final double pitch = Math.toDegrees(Math.asin(Math.clamp(nose[1], -1.0, 1.0)));
        final ShipFlightModel.Tick next = ShipFlightModel.step(this.state, held, air(level, now, body), this.ship,
                pitch);
        show(level, body, next);
        StructurePose turned = next.turn() == 0 ? now : now.turnedBy(0, 1, 0, Math.toRadians(next.turn()));
        if (next.pitch() != 0) {
            final double[] way = nose(new StructureFrame(turned, frame.pivotX(), frame.pivotY(), frame.pivotZ()));
            final double across = Math.hypot(way[0], way[2]);
            if (across > STILL) {
                turned = turned.turnedBy(-way[2] / across, 0, way[0] / across, Math.toRadians(next.pitch()));
            }
        }
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

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    /* The rule reads its pilot's controls, which only the structure itself holds, and the land and air round it. */
    @Override
    public boolean readsWorld() {
        return true;
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
        return new ShipFlight(next, this.noseX, this.noseZ, this.ship, landed, this.console);
    }

    /* Its console shows the tick's flight, where its yard's land is loaded. */
    private void show(final ServerLevel level, final MovingStructure body, final ShipFlightModel.Tick tick) {
        final Optional<BlockPos> at = consoleIn(body);
        if (at.isPresent() && level.isLoaded(at.get())
                && level.getBlockEntity(at.get()) instanceof AirReserveEntity keeper) {
            keeper.flew(ShipReadings.of(tick));
        }
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
