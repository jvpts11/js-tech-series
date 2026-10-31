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
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMotions;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A small ship that has left its dock, flown as a moving structure of J's Core a tick at a time by
 * {@link ShipFlightModel}: its blocks fly as they stand, its machines work, its crew walk about inside. Its pilot at
 * the helm, the cockpit's pilot seat, opens and closes its throttle, turns it and pitches its nose, and it goes where
 * its nose points; its wings hold it up in the air of the world it flies in, as thick as that world's air is at its
 * height, and its boost takes it faster across the empty stretches. With nobody at the helm its throttle closes. It
 * rests on the land under it and is stopped by the land it would fly into ({@link ShipGround}). Its rule goes on
 * through a save, as the structure does.
 *
 * @param state  where its flight stands
 * @param noseX  which way its nose points among its blocks, across, as its plan laid it
 * @param noseZ  which way its nose points among its blocks, along
 * @param ship   what it weighs and pushes
 */
public record ShipFlight(ShipFlightModel.State state, int noseX, int noseZ, ShipFlightModel.Performance ship)
        implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_flight");
    private static final MapCodec<ShipFlightModel.State> STATE = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.DOUBLE.fieldOf("speed").forGetter(ShipFlightModel.State::speed),
                    Codec.DOUBLE.fieldOf("sink").forGetter(ShipFlightModel.State::sink),
                    Codec.DOUBLE.fieldOf("throttle").forGetter(ShipFlightModel.State::throttle),
                    Codec.DOUBLE.fieldOf("charge").forGetter(ShipFlightModel.State::charge))
            .apply(instance, ShipFlightModel.State::new));
    private static final MapCodec<ShipFlightModel.Performance> PERFORMANCE = RecordCodecBuilder.mapCodec(
            instance -> instance.group(Codec.DOUBLE.fieldOf("mass").forGetter(ShipFlightModel.Performance::mass),
                    Codec.DOUBLE.fieldOf("air_thrust").forGetter(ShipFlightModel.Performance::airThrust),
                    Codec.DOUBLE.fieldOf("bell_thrust").forGetter(ShipFlightModel.Performance::bellThrust),
                    Codec.DOUBLE.fieldOf("wing").forGetter(ShipFlightModel.Performance::wing),
                    Codec.DOUBLE.fieldOf("power").forGetter(ShipFlightModel.Performance::power))
                    .apply(instance, ShipFlightModel.Performance::new));
    public static final MapCodec<ShipFlight> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            STATE.forGetter(ShipFlight::state),
            Codec.INT.fieldOf("nose_x").forGetter(ShipFlight::noseX),
            Codec.INT.fieldOf("nose_z").forGetter(ShipFlight::noseZ),
            PERFORMANCE.fieldOf("ship").forGetter(ShipFlight::ship)
    ).apply(instance, ShipFlight::new));
    /* The overworld's air at sea level, in kilopascals, what its air's thickness is told against. */
    private static final double SEA_PRESSURE = 101.3;
    /* How far over the land a stretch is empty for the boost, in blocks. */
    private static final int EMPTY_OVER = 24;
    /* Below this, in metres a second or blocks, nothing moves. */
    private static final double STILL = 1.0e-4;

    /** Registers the rule's kind, once, from the mod's constructor. */
    public static void declare() {
        StructureMotions.register(TYPE, CODEC);
    }

    /** A ship of {@code ship} at rest, its nose pointing {@code noseX}, {@code noseZ} among its blocks. */
    public static ShipFlight resting(final int noseX, final int noseZ, final ShipFlightModel.Performance ship) {
        return new ShipFlight(ShipFlightModel.State.RESTING, noseX, noseZ, ship);
    }

    /**
     * What a ship of {@code design} weighs and pushes, by its parts' figures in {@code parts}: its air engines push
     * what they lift, its reactor's power beyond what its parts take fills its boost.
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
        }
        return new ShipFlightModel.Performance(mass, air, bells, wing, power);
    }

    /** Its flight as it stands, another way. */
    public ShipFlight with(final ShipFlightModel.State next) {
        return new ShipFlight(next, this.noseX, this.noseZ, this.ship);
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
        final double down = next.state().sink() * ShipFlightModel.TICK;
        return flown(level, body, now, turned, way[0] * ahead, way[1] * ahead - down, way[2] * ahead, next.state());
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
            final ShipFlightModel.State landed = meets.touching() ? stood(next) : next;
            return still(now, moved.movedTo(moved.x(), moved.y() + meets.rise(), moved.z()), landed);
        }
        if (dy >= 0 && ShipGround.meet(level, body, frameOf(body, now)).blocked()) {
            // Already in the land where it stands, built into it or set there: it flies on out of it, never deeper.
            return still(now, moved, next);
        }
        // Run into land: it stops where it was, only coming down onto what is under it, or not at all.
        final ShipFlightModel.State stopped = new ShipFlightModel.State(0, next.sink(), next.throttle(),
                next.charge());
        final StructurePose dropped = turned.movedTo(now.x(), now.y() + Math.min(0, dy), now.z());
        final ShipGround.Contact under = ShipGround.meet(level, body, frameOf(body, dropped), most);
        if (!under.blocked()) {
            return still(now, dropped.movedTo(dropped.x(), dropped.y() + under.rise(), dropped.z()),
                    under.touching() ? stood(stopped) : stopped);
        }
        return new Step(now, with(stood(stopped)));
    }

    /* The step to {@code to}, or none where it barely moved: a ship at rest sends nothing. */
    private Step still(final StructurePose now, final StructurePose to, final ShipFlightModel.State next) {
        if (to.distanceTo(now) < STILL && to.angleTo(now) < STILL) {
            return new Step(now, with(next));
        }
        return new Step(to, with(next));
    }

    /* Its flight standing on the land: it sinks no more. */
    private static ShipFlightModel.State stood(final ShipFlightModel.State next) {
        return new ShipFlightModel.State(next.speed(), 0, next.throttle(), next.charge());
    }

    /* The air round {@code body} standing at {@code now}: its world's at that height, and whether it is empty. */
    private static ShipFlightModel.Air air(final ServerLevel level, final StructurePose now,
                                           final MovingStructure body) {
        final DimensionRules rules = DimensionRulesData.of(level);
        final Climate climate = rules.at(now.y());
        final double gravity = rules.gravityAt(now.y());
        final double density = climate.pressure() / SEA_PRESSURE;
        final BlockPos under = BlockPos.containing(now.x(), now.y(), now.z());
        final boolean empty = density <= 0 || gravity <= 0 || !level.isLoaded(under)
                || body.worldBox().minY - level.getHeight(Heightmap.Types.MOTION_BLOCKING, under.getX(),
                under.getZ()) >= EMPTY_OVER;
        return new ShipFlightModel.Air(density, gravity, empty);
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
}
