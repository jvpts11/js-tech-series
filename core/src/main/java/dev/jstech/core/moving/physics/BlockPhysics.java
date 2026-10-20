/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.explosion.BlastMath;
import dev.jstech.core.explosion.BlastStrength;
import dev.jstech.core.hazard.BlockDensity;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What a block is of in the physics of a moving structure, as data, so any mod and data pack takes part: its mass,
 * and the forces it gives, each optional. A block the map says nothing of weighs its density times the volume of its
 * shape ({@link BlockDensity}), displaces that volume in a liquid, and gives no force of its own.
 *
 * <pre>{@code
 * "values": {
 *   "mymod:rocket_engine": {"thrust": {"newtons": 400000, "kind": "rocket"}},
 *   "mymod:wing": {"wing": {"area": 1, "stall": 15, "lift": 1.2}},
 *   "mymod:wheel": {"wheel": {"radius": 0.5, "stiffness": 60000, "damping": 6000, "steer": 30, "drive": 4000,
 *                   "brake": 8000}},
 *   "mymod:float": {"displacement": 1.0}, "mymod:hover_coil": {"levitation": 20000},
 *   "mymod:electromagnet": {"magnet": {"newtons": 50000, "reach": 6, "power": "any"}},
 *   "mymod:burner": {"burner": {"watts": 300000, "power": "any"}}
 * }
 * }</pre>
 *
 * A block's way is its {@code facing}, or its horizontal facing; where it has none, north. An engine pushes towards
 * its way. A wheel rolls along its way, its spring holding it from the structure's down.
 *
 * @param mass         its mass, in kilograms; its density times its shape's volume when not said
 * @param thrust       the thrust it gives
 * @param wing         the lift it gives as a wing
 * @param displacement the volume it displaces in a liquid, in cubic metres; its shape's when not said
 * @param levitation   the weight it holds up, in newtons, as it resists falling without ever pushing up
 * @param magnet       the pull it gives magnetic blocks of the world near it
 * @param wheel        how it rolls, as a wheel on a spring
 * @param burner       the heat it gives the balloon it opens into
 * @param drag         how much its faces drag, as a share of a flat face's
 */
public record BlockPhysics(Optional<Double> mass, Optional<Thrust> thrust, Optional<Wing> wing,
                           Optional<Double> displacement, Optional<Double> levitation, Optional<Magnet> magnet,
                           Optional<Wheel> wheel, Optional<Burner> burner, Optional<Double> drag) {

    /** A block the map says nothing of. */
    public static final BlockPhysics NONE = new BlockPhysics(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    public static final Codec<BlockPhysics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1.0e9).optionalFieldOf("mass").forGetter(BlockPhysics::mass),
            Thrust.CODEC.optionalFieldOf("thrust").forGetter(BlockPhysics::thrust),
            Wing.CODEC.optionalFieldOf("wing").forGetter(BlockPhysics::wing),
            Codec.doubleRange(0, 1).optionalFieldOf("displacement").forGetter(BlockPhysics::displacement),
            Codec.doubleRange(0, 1.0e10).optionalFieldOf("levitation").forGetter(BlockPhysics::levitation),
            Magnet.CODEC.optionalFieldOf("magnet").forGetter(BlockPhysics::magnet),
            Wheel.CODEC.optionalFieldOf("wheel").forGetter(BlockPhysics::wheel),
            Burner.CODEC.optionalFieldOf("burner").forGetter(BlockPhysics::burner),
            Codec.doubleRange(0, 10).optionalFieldOf("drag").forGetter(BlockPhysics::drag))
            .apply(instance, BlockPhysics::new));
    /** Each block's physics, by the block. */
    public static final DataMapType<Block, BlockPhysics> PHYSICS = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "block_physics"), Registries.BLOCK, CODEC).build();

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(PHYSICS));
    }

    /** The physics of the block {@code state}: the map's, or {@link #NONE}. */
    public static BlockPhysics of(final BlockState state) {
        final BlockPhysics mapped = state.getBlockHolder().getData(PHYSICS);
        return mapped == null ? NONE : mapped;
    }

    /** The mass of the block {@code state} at {@code pos}, in kilograms: the map's, else its density by its volume. */
    public static double massOf(final BlockGetter level, final BlockPos pos, final BlockState state) {
        if (state.isAir()) {
            return 0;
        }
        return of(state).mass().orElseGet(() -> BlockDensity.of(level, pos, state) * volumeOf(level, pos, state));
    }

    /** The volume the block {@code state} at {@code pos} fills, in cubic metres: of its shape, a full block 1. */
    public static double volumeOf(final BlockGetter level, final BlockPos pos, final BlockState state) {
        if (state.isAir()) {
            return 0;
        }
        final VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) {
            return 0;
        }
        double volume = 0;
        for (final AABB box : shape.toAabbs()) {
            volume += box.getXsize() * box.getYsize() * box.getZsize();
        }
        return Math.min(1, volume);
    }

    /** The volume the block {@code state} at {@code pos} displaces in a liquid, in cubic metres. */
    public static double displacementOf(final BlockGetter level, final BlockPos pos, final BlockState state) {
        return of(state).displacement().orElseGet(() -> volumeOf(level, pos, state));
    }

    /**
     * How much energy a blow at the block {@code state} takes before it breaks it, as a strength in kilopascals: its
     * strength against a blast, the energy a cubic metre of it takes in kilojoules.
     */
    public static double strengthOf(final BlockState state) {
        final Double mapped = state.getBlockHolder().getData(BlastStrength.STRENGTH);
        return mapped != null ? mapped : BlastMath.standsFromResistance(state.getBlock().getExplosionResistance());
    }

    /** The way the block {@code state} looks: its facing, or its horizontal facing; north when it has none. */
    public static Direction wayOf(final BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    /**
     * How strong a signal reaches the block at {@code pos} from {@code side} of it, seen from the way it looks, from 0
     * to 15; from any side, the strongest.
     */
    public static int signal(final Level level, final BlockPos pos, final Direction way, final SignalSide side) {
        final Direction from = side.from(way);
        if (from == null) {
            return level.getBestNeighborSignal(pos);
        }
        return level.getSignal(pos.relative(from), from);
    }

    /**
     * Where a signal reaches a block from, as the block looks: in front of it, behind it, at its left or right as it
     * looks, above or below it, or from any side.
     */
    public enum SignalSide implements IStableName {
        ANY("any"),
        FRONT("front"),
        BACK("back"),
        LEFT("left"),
        RIGHT("right"),
        TOP("top"),
        BOTTOM("bottom");

        public static final Codec<SignalSide> CODEC = StableCodecs.byName(SignalSide.class);
        private final String serializedName;

        SignalSide(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }

        /* The side of a block looking {@code way} this is, or null for any side. */
        @Nullable Direction from(final Direction way) {
            final Direction flat = way.getAxis().isHorizontal() ? way : Direction.NORTH;
            return switch (this) {
                case ANY -> null;
                case FRONT -> way;
                case BACK -> way.getOpposite();
                case LEFT -> flat.getCounterClockWise();
                case RIGHT -> flat.getClockWise();
                case TOP -> Direction.UP;
                case BOTTOM -> Direction.DOWN;
            };
        }
    }

    /**
     * The thrust of an engine, pushing towards the way the block looks.
     *
     * @param newtons  its thrust at full throttle
     * @param kind     how it throws its thrust: a rocket's anywhere, a jet's and a propeller's by the air
     * @param exhaust  the speed a propeller throws the air back at, in metres a second: it gives nothing at it
     * @param throttle where the signal setting its throttle reaches it from, full at 15
     */
    public record Thrust(double newtons, Aerodynamics.ThrustKind kind, double exhaust, SignalSide throttle) {

        public static final Codec<Thrust> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 1.0e10).fieldOf("newtons").forGetter(Thrust::newtons),
                StableCodecs.byName(Aerodynamics.ThrustKind.class).optionalFieldOf("kind",
                        Aerodynamics.ThrustKind.ROCKET).forGetter(Thrust::kind),
                Codec.doubleRange(0, 10_000).optionalFieldOf("exhaust", 0.0).forGetter(Thrust::exhaust),
                SignalSide.CODEC.optionalFieldOf("throttle", SignalSide.ANY).forGetter(Thrust::throttle))
                .apply(instance, Thrust::new));
    }

    /**
     * A wing: its area, its stall and its most lift. Its face looks up the structure's way up, or the way the block
     * looks.
     *
     * @param area  its area, in square metres
     * @param stall its angle of attack at the stall, in degrees
     * @param lift  its most lift, as a coefficient, at the stall
     * @param face  which way its face looks
     */
    public record Wing(double area, double stall, double lift, WingFace face) {

        public static final Codec<Wing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 1_000).optionalFieldOf("area", 1.0).forGetter(Wing::area),
                Codec.doubleRange(1, 45).optionalFieldOf("stall", 15.0).forGetter(Wing::stall),
                Codec.doubleRange(0, 5).optionalFieldOf("lift", 1.2).forGetter(Wing::lift),
                StableCodecs.byName(WingFace.class).optionalFieldOf("face", WingFace.UP).forGetter(Wing::face))
                .apply(instance, Wing::new));
    }

    /** Which way a wing's face looks. */
    public enum WingFace implements IStableName {
        /** Up the structure's own way up, as a plane's wing. */
        UP("up"),
        /** The way the block looks, as a sail or a rudder. */
        FACING("facing");

        private final String serializedName;

        WingFace(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }

    /**
     * A magnet: it pulls the magnetic blocks of the world near it, and so is pulled to them.
     *
     * @param newtons its pull at a block from a magnetic block, falling with the square of the distance beyond
     * @param reach   how far it pulls, in blocks
     * @param power   where the signal that powers it, as an electromagnet, reaches it from, as strong as the
     *                signal; a magnet that says none always pulls
     */
    public record Magnet(double newtons, double reach, Optional<SignalSide> power) {

        public static final Codec<Magnet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 1.0e10).fieldOf("newtons").forGetter(Magnet::newtons),
                Codec.doubleRange(1, 16).optionalFieldOf("reach", 6.0).forGetter(Magnet::reach),
                SignalSide.CODEC.optionalFieldOf("power").forGetter(Magnet::power))
                .apply(instance, Magnet::new));
    }

    /**
     * A burner: it heats the air of the balloon above it, as a hot air balloon's does.
     *
     * @param watts its heat, in watts, at a full signal
     * @param power where the signal that lights it reaches it from, as strong as the signal; a burner that says none
     *              always burns
     */
    public record Burner(double watts, Optional<SignalSide> power) {

        public static final Codec<Burner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 1.0e10).fieldOf("watts").forGetter(Burner::watts),
                SignalSide.CODEC.optionalFieldOf("power").forGetter(Burner::power))
                .apply(instance, Burner::new));
    }

    /**
     * A wheel on a spring: it holds the structure up from the ground along the structure's down, rolls along the way
     * the block looks, steers by a signal at its left or right, drives and brakes by signals, and grips as the ground
     * lets it.
     *
     * @param radius    its radius, in metres
     * @param travel    how far its spring goes, in metres
     * @param stiffness its spring's stiffness, in newtons a metre
     * @param damping   its spring's damping, in newton seconds a metre
     * @param steer     how far it steers at a full signal, in degrees; 0 for a wheel that does not
     * @param drive     the force it drives with at a full signal, in newtons
     * @param brake     the force it brakes with at a full signal, in newtons
     * @param grip      how well its tyre grips plain ground, stone's, against sliding
     * @param left      where the signal steering it left reaches it from
     * @param right     where the signal steering it right reaches it from
     * @param throttle  where the signal driving it forward reaches it from
     * @param reverse   where the signal driving it back reaches it from
     * @param stop      where the signal braking it reaches it from
     */
    public record Wheel(double radius, double travel, double stiffness, double damping, double steer, double drive,
                        double brake, double grip, SignalSide left, SignalSide right, SignalSide throttle,
                        SignalSide reverse, SignalSide stop) {

        public static final Codec<Wheel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0.05, 8).optionalFieldOf("radius", 0.5).forGetter(Wheel::radius),
                Codec.doubleRange(0, 4).optionalFieldOf("travel", 0.3).forGetter(Wheel::travel),
                Codec.doubleRange(0, 1.0e9).optionalFieldOf("stiffness", 50_000.0).forGetter(Wheel::stiffness),
                Codec.doubleRange(0, 1.0e9).optionalFieldOf("damping", 5_000.0).forGetter(Wheel::damping),
                Codec.doubleRange(0, 90).optionalFieldOf("steer", 0.0).forGetter(Wheel::steer),
                Codec.doubleRange(0, 1.0e9).optionalFieldOf("drive", 0.0).forGetter(Wheel::drive),
                Codec.doubleRange(0, 1.0e9).optionalFieldOf("brake", 0.0).forGetter(Wheel::brake),
                Codec.doubleRange(0, 3).optionalFieldOf("grip", 0.9).forGetter(Wheel::grip),
                SignalSide.CODEC.optionalFieldOf("left", SignalSide.LEFT).forGetter(Wheel::left),
                SignalSide.CODEC.optionalFieldOf("right", SignalSide.RIGHT).forGetter(Wheel::right),
                SignalSide.CODEC.optionalFieldOf("throttle", SignalSide.BACK).forGetter(Wheel::throttle),
                SignalSide.CODEC.optionalFieldOf("reverse", SignalSide.FRONT).forGetter(Wheel::reverse),
                SignalSide.CODEC.optionalFieldOf("stop", SignalSide.TOP).forGetter(Wheel::stop))
                .apply(instance, Wheel::new));
    }
}
