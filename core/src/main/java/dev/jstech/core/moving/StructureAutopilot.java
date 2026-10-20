/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.moving.physics.PhysicsWorld;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.uuid.StructureUuid;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * An autopilot flying a moving structure by its controls ({@link StructureControls}), as a pilot at its helm would:
 * it goes to a place, holds a height, follows a route, or docks to a structure's clamp. Each tick it works out how
 * fast the structure is to go to close on its goal, no faster than its speed, and works the controls forward and back,
 * up and down, left and right by how far the structure's speed falls short of that, over how fast its engines and
 * wheels can speed it up that way, its height held against its own weight. So any structure whose engines and wheels
 * answer its controls is flown alike, a heavy rocket, a light airship or a car, never overshooting for engines too
 * strong for it. Kept with the structure, so it flies on after a restart.
 *
 * <pre>{@code
 * structure.fly(StructureAutopilot.goTo(new Vec3(120, 90, -40), 8));
 * structure.fly(StructureAutopilot.route(List.of(a, b, c), 10, true));
 * }</pre>
 *
 * @param goal  where it is to go
 * @param speed the fastest it goes, in metres a second
 */
public record StructureAutopilot(Goal goal, double speed) {

    public static final Codec<StructureAutopilot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Goal.CODEC.fieldOf("goal").forGetter(StructureAutopilot::goal),
            Codec.doubleRange(0.01, 1.0e5).fieldOf("speed").forGetter(StructureAutopilot::speed)
    ).apply(instance, StructureAutopilot::new));
    /* How many metres a second it goes for each metre it is short of its goal, before its speed caps it. */
    private static final double CLOSING = 0.5;
    /*
     * How long it takes to gain the speed it is short of, in seconds, by how fast its engines speed it up; and, for a
     * structure whose engines are not known, how hard a control is held for each metre a second it is short.
     */
    private static final double RESPONSE = 0.5;
    private static final double HOLD = 0.8;
    /* How much the height it is short of builds up into holding it up, a second, against its own weight. */
    private static final double HOLD_UP = 0.15;
    /* How near a place it counts as there, in metres; a clamp's, for docking. */
    private static final double THERE = 1.0;
    private static final double AT_CLAMP = 1.5;
    /* How far off a clamp it comes in to dock from, in metres. */
    private static final double STAND_OFF = 2.0;
    private static final double TICKS = 20;
    private static final double HALF = 0.5;

    /** Goes to {@code target}, a place of the world, and holds there, no faster than {@code speed}. */
    public static StructureAutopilot goTo(final Vec3 target, final double speed) {
        return new StructureAutopilot(new GoTo(target), speed);
    }

    /** Holds the height {@code y}, its controls left and right, forward and back, left to others. */
    public static StructureAutopilot holdAltitude(final double y, final double speed) {
        return new StructureAutopilot(new Altitude(y), speed);
    }

    /** Follows {@code points} one after another, over again when {@code loop}, else holding at the last. */
    public static StructureAutopilot route(final List<Vec3> points, final double speed, final boolean loop) {
        return new StructureAutopilot(new Route(points, 0, loop), speed);
    }

    /** Docks to the clamp at {@code clamp} of the structure {@code host}, coming in slowly from off its face. */
    public static StructureAutopilot dock(final StructureUuid host, final BlockPos clamp, final double speed) {
        return new StructureAutopilot(new Dock(host, clamp), speed);
    }

    /*
     * One tick: works the structure's controls towards its goal; gives the autopilot it goes on as, a route at its
     * next point, or null once it has docked.
     */
    @Nullable StructureAutopilot steer(final ServerLevel level, final MovingStructure structure, final double[] held) {
        final Vec3 at = centreOf(structure);
        final Vec3 going = velocityOf(structure);
        final Vec3 forward = forwardOf(level, structure);
        final Vec3 up = upOf(structure);
        final Map<StructureControls.ControlAxis, Float> told = new EnumMap<>(StructureControls.ControlAxis.class);
        StructureAutopilot next = this;
        boolean dock = false;
        Vec3 target;
        boolean heightOnly = false;
        switch (goal) {
            case GoTo to -> target = to.target();
            case Altitude height -> {
                target = new Vec3(at.x, height.y(), at.z);
                heightOnly = true;
            }
            case Route route -> {
                if (route.points().isEmpty()) {
                    return null;
                }
                target = route.points().get(Math.min(route.next(), route.points().size() - 1));
                final boolean more = route.next() < route.points().size() - 1 || route.loop();
                if (target.distanceTo(at) < THERE && more) {
                    final int following = route.next() + 1 >= route.points().size() ? 0 : route.next() + 1;
                    next = new StructureAutopilot(new Route(route.points(), following, route.loop()), speed);
                }
            }
            case Dock docking -> {
                final MovingStructure host = MovingStructures.byId(level, docking.host());
                if (host == null || structure.mount() != null) {
                    return structure.mount() != null ? null : this;
                }
                final Direction way = BlockPhysics.wayOf(level.getBlockState(docking.clamp()));
                final StructureFrame frame = host.frame();
                final double x = docking.clamp().getX() + HALF + way.getStepX() * (HALF + STAND_OFF);
                final double y = docking.clamp().getY() + HALF + way.getStepY() * (HALF + STAND_OFF);
                final double z = docking.clamp().getZ() + HALF + way.getStepZ() * (HALF + STAND_OFF);
                target = new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
                dock = target.distanceTo(at) < STAND_OFF + AT_CLAMP;
            }
        }
        final Vec3 off = target.subtract(at);
        Vec3 wanted = off.scale(CLOSING);
        if (wanted.length() > speed) {
            wanted = wanted.normalize().scale(speed);
        }
        final Vec3 lacking = wanted.subtract(going);
        final Vec3 right = forward.cross(up);
        final StructurePhysics physics = StructureMounts.rootOf(structure).physics();
        if (!heightOnly) {
            axis(told, hold(physics, forward, lacking.dot(forward)), StructureControls.ControlAxis.FORWARD,
                    StructureControls.ControlAxis.BACK);
            axis(told, hold(physics, right, lacking.dot(right)), StructureControls.ControlAxis.RIGHT,
                    StructureControls.ControlAxis.LEFT);
        }
        // Up and down hold the height against the structure's own weight, as much as its engines lifting bear it,
        // and what it is short builds up a hold for the rest: balloons or levitators bearing some of it.
        held[0] = Math.max(-1, Math.min(1, held[0] + off.dot(up) * HOLD_UP / TICKS));
        axis(told, hold(physics, up, lacking.dot(up)) + bearing(level, physics, at, up) + held[0],
                StructureControls.ControlAxis.UP, StructureControls.ControlAxis.DOWN);
        structure.controls().tell(level.getGameTime(), told, forward, up, dock);
        return next;
    }

    /*
     * How hard a control along {@code way} is held for the structure to gain {@code lacking}, in metres a second,
     * along it: by how fast its engines and wheels speed it up that way, so it gains it over a short while whatever
     * its engines; a structure that does not move by its physics, by a gain of its own.
     */
    private static double hold(@Nullable final StructurePhysics physics, final Vec3 way, final double lacking) {
        final double reach = physics == null || !physics.active() ? 0
                : physics.authority(lacking >= 0 ? way : way.reverse());
        return reach > 0 ? lacking / (reach * RESPONSE) : lacking * HOLD;
    }

    /* How hard up is held for the structure's engines lifting it to bear its weight, from 0 to 1. */
    private static double bearing(final ServerLevel level, @Nullable final StructurePhysics physics, final Vec3 at,
                                  final Vec3 up) {
        if (physics == null || !physics.active()) {
            return 0;
        }
        final double lift = physics.authority(up);
        if (lift <= 0) {
            return 0;
        }
        final Vector3d pull = PhysicsWorld.gravity(level, new Vector3d(at.x, at.y, at.z), new Vector3d());
        final double weight = -(pull.x * up.x + pull.y * up.y + pull.z * up.z);
        return Math.max(0, Math.min(1, weight / lift));
    }

    private static void axis(final Map<StructureControls.ControlAxis, Float> told, final double hold,
                             final StructureControls.ControlAxis ahead, final StructureControls.ControlAxis behind) {
        final float value = (float) Math.max(-1, Math.min(1, hold));
        told.put(ahead, Math.max(0F, value));
        told.put(behind, Math.max(0F, -value));
    }

    /* Where the structure's middle is in the world: its body's centre when it moves by its physics. */
    private static Vec3 centreOf(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        if (physics != null && physics.active()) {
            return new Vec3(physics.body().position().x, physics.body().position().y, physics.body().position().z);
        }
        return structure.worldBox().getCenter();
    }

    /* How fast the structure goes, in metres a second. */
    private static Vec3 velocityOf(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        if (physics != null && physics.active()) {
            return new Vec3(physics.body().velocity().x, physics.body().velocity().y, physics.body().velocity().z);
        }
        final double[] moved = structure.velocity();
        return new Vec3(moved[0], moved[1], moved[2]).scale(TICKS);
    }

    /* The structure's forward in the world: the way its helm looks, or its yard's north for one with none. */
    private static Vec3 forwardOf(final ServerLevel level, final MovingStructure structure) {
        Direction way = Direction.NORTH;
        for (final Map.Entry<BlockPos, BlockAboard> each : structure.aboard().blocks().entrySet()) {
            if (each.getValue().seat().isPresent() && each.getValue().seat().get().helm()) {
                way = BlockPhysics.wayOf(level.getBlockState(each.getKey()));
                break;
            }
        }
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.turnX(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnY(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnZ(way.getStepX(), way.getStepY(), way.getStepZ()));
    }

    /* The structure's up in the world. */
    private static Vec3 upOf(final MovingStructure structure) {
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.turnX(0, 1, 0), frame.turnY(0, 1, 0), frame.turnZ(0, 1, 0));
    }

    /** Where an autopilot is to go. */
    public sealed interface Goal permits GoTo, Altitude, Route, Dock {

        Codec<Goal> CODEC = Codec.STRING.partialDispatch("type", goal -> DataResult.success(goal.type()),
                Goal::codecOf);

        /** The name of its kind, by which it is written. */
        String type();

        private static DataResult<MapCodec<? extends Goal>> codecOf(final String type) {
            return switch (type) {
                case GoTo.TYPE -> DataResult.success(GoTo.CODEC);
                case Altitude.TYPE -> DataResult.success(Altitude.CODEC);
                case Route.TYPE -> DataResult.success(Route.CODEC);
                case Dock.TYPE -> DataResult.success(Dock.CODEC);
                default -> DataResult.error(() -> "unknown autopilot goal " + type);
            };
        }
    }

    /** To a place of the world, and holding there. @param target the place */
    public record GoTo(Vec3 target) implements Goal {

        public static final String TYPE = "go_to";
        public static final MapCodec<GoTo> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Vec3.CODEC.fieldOf("target").forGetter(GoTo::target)
        ).apply(instance, GoTo::new));

        @Override
        public String type() {
            return TYPE;
        }
    }

    /** Holding a height. @param y the height, in the world */
    public record Altitude(double y) implements Goal {

        public static final String TYPE = "altitude";
        public static final MapCodec<Altitude> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.fieldOf("y").forGetter(Altitude::y)
        ).apply(instance, Altitude::new));

        @Override
        public String type() {
            return TYPE;
        }
    }

    /**
     * Along places one after another.
     *
     * @param points the places, of the world
     * @param next   the one it is going to now
     * @param loop   whether it goes round again after the last
     */
    public record Route(List<Vec3> points, int next, boolean loop) implements Goal {

        public static final String TYPE = "route";
        public static final MapCodec<Route> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Vec3.CODEC.listOf().fieldOf("points").forGetter(Route::points),
                Codec.INT.optionalFieldOf("next", 0).forGetter(Route::next),
                Codec.BOOL.optionalFieldOf("loop", false).forGetter(Route::loop)
        ).apply(instance, Route::new));

        public Route {
            points = List.copyOf(points);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    /**
     * To a structure's clamp, docking there.
     *
     * @param host  the structure docked to
     * @param clamp its clamp, a place of its yard
     */
    public record Dock(StructureUuid host, BlockPos clamp) implements Goal {

        public static final String TYPE = "dock";
        public static final MapCodec<Dock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                StructureUuid.CODEC.fieldOf("host").forGetter(Dock::host),
                BlockPos.CODEC.fieldOf("clamp").forGetter(Dock::clamp)
        ).apply(instance, Dock::new));

        @Override
        public String type() {
            return TYPE;
        }
    }
}
