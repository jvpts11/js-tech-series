/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.joint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.Optional;

/**
 * What a joint holds between its two ends, each kind as data:
 * <ul>
 *   <li>a {@link Hinge}, or a bearing when driven: the ends held together, turning about its axis only, within its
 *       limits, its motor turning it as its signal says;</li>
 *   <li>a {@link Slider}, or a piston when driven: one end along the other's axis only, not turning;</li>
 *   <li>a {@link Spring} between the ends, of its stiffness and damping;</li>
 *   <li>a {@link Rope} or a chain, holding the ends no further apart than its length, a winch reeling it;</li>
 *   <li>a {@link Bar}, holding the ends at its length, as a tow bar;</li>
 *   <li>a {@link Ball}, the ends held together, turning any way;</li>
 *   <li>a {@link Weld}, the ends held together as they were, as one.</li>
 * </ul>
 */
public sealed interface JointKind permits JointKind.Hinge, JointKind.Slider, JointKind.Spring, JointKind.Rope,
        JointKind.Bar, JointKind.Ball, JointKind.Weld {

    Codec<JointKind> CODEC = Codec.STRING.partialDispatch("type", kind -> DataResult.success(kind.type()),
            JointKind::codecOf);

    /** The name of its kind, by which it is written. */
    String type();

    /** Whether it holds its ends' places to each other, as every kind but a spring and a rope does. */
    default boolean holdsTogether() {
        return true;
    }

    private static DataResult<MapCodec<? extends JointKind>> codecOf(final String type) {
        return switch (type) {
            case Hinge.TYPE -> DataResult.success(Hinge.CODEC);
            case Slider.TYPE -> DataResult.success(Slider.CODEC);
            case Spring.TYPE -> DataResult.success(Spring.CODEC);
            case Rope.TYPE -> DataResult.success(Rope.CODEC);
            case Bar.TYPE -> DataResult.success(Bar.CODEC);
            case Ball.TYPE -> DataResult.success(Ball.CODEC);
            case Weld.TYPE -> DataResult.success(Weld.CODEC);
            default -> DataResult.error(() -> "unknown joint kind " + type);
        };
    }

    /**
     * How far a joint may go, from {@code least} to {@code most}: radians a hinge turns, metres a slider slides.
     *
     * @param least the least
     * @param most  the most
     */
    record Limits(double least, double most) {

        public static final Codec<Limits> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("least").forGetter(Limits::least),
                Codec.DOUBLE.fieldOf("most").forGetter(Limits::most)
        ).apply(instance, Limits::new));
    }

    /**
     * A joint's motor: it drives at {@code speed}, radians or metres a second, with up to {@code force}, newton metres
     * or newtons; with a signal, as strong as the signal says, and back the other way as strong as its reverse's.
     *
     * @param speed   its speed at a full signal
     * @param force   the most it drives with
     * @param signal  the signal that drives it forward, or none to drive always at full speed
     * @param reverse the signal that drives it back, or none
     */
    record Drive(double speed, double force, Optional<JointSignal> signal, Optional<JointSignal> reverse) {

        public static final Codec<Drive> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("speed").forGetter(Drive::speed),
                Codec.doubleRange(0, 1.0e12).fieldOf("force").forGetter(Drive::force),
                JointSignal.CODEC.optionalFieldOf("signal").forGetter(Drive::signal),
                JointSignal.CODEC.optionalFieldOf("reverse").forGetter(Drive::reverse)
        ).apply(instance, Drive::new));

        /** A motor that always drives at {@code speed} with up to {@code force}. */
        public static Drive always(final double speed, final double force) {
            return new Drive(speed, force, Optional.empty(), Optional.empty());
        }
    }

    /**
     * A winch reeling a rope: in at {@code speed} metres a second as strong as its signal to reel in says, out as
     * strong as its signal to reel out, between {@code least} and {@code most} metres.
     *
     * @param speed the speed it reels at, at a full signal
     * @param least the shortest it reels the rope in to
     * @param most  the longest it lets the rope out to
     * @param in    the signal reeling the rope in
     * @param out   the signal letting it out
     */
    record Winch(double speed, double least, double most, Optional<JointSignal> in, Optional<JointSignal> out) {

        public static final Codec<Winch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 100).fieldOf("speed").forGetter(Winch::speed),
                Codec.doubleRange(0, 1024).optionalFieldOf("least", 0.5).forGetter(Winch::least),
                Codec.doubleRange(0, 1024).optionalFieldOf("most", 256.0).forGetter(Winch::most),
                JointSignal.CODEC.optionalFieldOf("in").forGetter(Winch::in),
                JointSignal.CODEC.optionalFieldOf("out").forGetter(Winch::out)
        ).apply(instance, Winch::new));
    }

    /**
     * A hinge, or a bearing when driven: the ends held at one place, turning about the axis only.
     *
     * @param limits how far it turns, in radians from how it was made; none for as far as it goes
     * @param drive  its motor, or none for a hinge that turns freely
     */
    record Hinge(Optional<Limits> limits, Optional<Drive> drive) implements JointKind {

        public static final String TYPE = "hinge";
        public static final MapCodec<Hinge> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Limits.CODEC.optionalFieldOf("limits").forGetter(Hinge::limits),
                Drive.CODEC.optionalFieldOf("drive").forGetter(Hinge::drive)
        ).apply(instance, Hinge::new));

        /** A hinge that turns freely, as far as it goes. */
        public static final Hinge FREE = new Hinge(Optional.empty(), Optional.empty());

        @Override
        public String type() {
            return TYPE;
        }
    }

    /**
     * A slider, or a piston when driven: the second end held on the line through the first along its axis, not
     * turning from it.
     *
     * @param limits how far along the line it goes, in metres from the first end
     * @param drive  its motor, or none for a slider that slides freely
     */
    record Slider(Optional<Limits> limits, Optional<Drive> drive) implements JointKind {

        public static final String TYPE = "slider";
        public static final MapCodec<Slider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Limits.CODEC.optionalFieldOf("limits").forGetter(Slider::limits),
                Drive.CODEC.optionalFieldOf("drive").forGetter(Slider::drive)
        ).apply(instance, Slider::new));

        @Override
        public String type() {
            return TYPE;
        }
    }

    /**
     * A spring between the ends.
     *
     * @param rest      its length at rest, in metres
     * @param stiffness its stiffness, in newtons a metre
     * @param damping   its damping, in newton seconds a metre
     */
    record Spring(double rest, double stiffness, double damping) implements JointKind {

        public static final String TYPE = "spring";
        public static final MapCodec<Spring> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.doubleRange(0, 1024).fieldOf("rest").forGetter(Spring::rest),
                Codec.doubleRange(0, 1.0e12).fieldOf("stiffness").forGetter(Spring::stiffness),
                Codec.doubleRange(0, 1.0e12).optionalFieldOf("damping", 0.0).forGetter(Spring::damping)
        ).apply(instance, Spring::new));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public boolean holdsTogether() {
            return false;
        }
    }

    /**
     * A rope or a chain: the ends no further apart than its length, slack when nearer.
     *
     * @param length its length, in metres
     * @param look   how it is drawn: a rope or a chain
     * @param winch  the winch reeling it, or none
     */
    record Rope(double length, RopeLook look, Optional<Winch> winch) implements JointKind {

        public static final String TYPE = "rope";
        public static final MapCodec<Rope> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.doubleRange(0, 1024).fieldOf("length").forGetter(Rope::length),
                StableCodecs.byName(RopeLook.class).optionalFieldOf("look", RopeLook.ROPE).forGetter(Rope::look),
                Winch.CODEC.optionalFieldOf("winch").forGetter(Rope::winch)
        ).apply(instance, Rope::new));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public boolean holdsTogether() {
            return false;
        }

        /** This rope at {@code metres} long. */
        public Rope reeled(final double metres) {
            return new Rope(metres, look, winch);
        }
    }

    /**
     * A bar between the ends, as a tow bar: the ends held at its length, each turning any way.
     *
     * @param length its length, in metres
     */
    record Bar(double length) implements JointKind {

        public static final String TYPE = "bar";
        public static final MapCodec<Bar> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.doubleRange(0, 1024).fieldOf("length").forGetter(Bar::length)
        ).apply(instance, Bar::new));

        @Override
        public String type() {
            return TYPE;
        }
    }

    /** A ball joint: the ends held at one place, each turning any way. */
    record Ball() implements JointKind {

        public static final String TYPE = "ball";
        public static final Ball INSTANCE = new Ball();
        public static final MapCodec<Ball> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public String type() {
            return TYPE;
        }
    }

    /** A weld: the ends held at one place and turned as they were, as one. */
    record Weld() implements JointKind {

        public static final String TYPE = "weld";
        public static final Weld INSTANCE = new Weld();
        public static final MapCodec<Weld> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public String type() {
            return TYPE;
        }
    }

    /** How a rope is drawn. */
    enum RopeLook implements IStableName {
        ROPE("rope"),
        CHAIN("chain");

        private final String serializedName;

        RopeLook(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
