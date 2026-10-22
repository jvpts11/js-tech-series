/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.List;
import java.util.Objects;

/**
 * One part of an effect: particles from a shape at a rate and in bursts, a beam, an electric arc, a mark on the world,
 * or what it does to the screen of whoever is near. What it emits looks as {@link Look} says and moves as
 * {@link Motion} says, by the place's physics; a beam, an arc, a mark and the screen read {@link Extra}.
 *
 * @param name     its name in its effect, for the editor and for what emits from its particles
 * @param kind     what it is
 * @param enabled  whether it plays; one turned off in the editor is kept in the file
 * @param emission what it emits and when
 * @param look     how what it emits looks
 * @param motion   how what it emits moves
 * @param extra    what a beam, an arc, a mark or the screen read
 */
public record Emitter(String name, Kind kind, boolean enabled, Emission emission, Look look, Motion motion,
                     Extra extra) {

    /* The limits and plain values a file's numbers keep to. */
    private static final double MOST_RATE = 10_000;
    private static final double MOST_SECONDS = 600;
    private static final int MOST_BURST = 10_000;
    private static final int MOST_FRAMES = 64;
    private static final double MOST_SIZE = 256;
    private static final int BRIGHTEST = 15;
    private static final double MOST_PULL = 1000;
    private static final int MOST_BRANCHES = 16;
    private static final double DEFAULT_SIZE = 0.2;
    private static final int WHITE = -1;
    private static final double DEFAULT_WIDTH = 0.1;
    private static final double DEFAULT_JAGGED = 0.15;
    private static final double DEFAULT_RADIUS = 4;
    private static final double DEFAULT_FADE = 1;
    private static final double DEFAULT_LENGTH = 8;
    private static final int ALPHA_SHIFT = 24;
    private static final int FULL_ALPHA = 255;
    private static final int RGB = 0xFFFFFF;
    public static final Codec<Emitter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Emitter::name),
            StableCodecs.byName(Kind.class).optionalFieldOf("kind", Kind.PARTICLES).forGetter(Emitter::kind),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Emitter::enabled),
            Emission.CODEC.optionalFieldOf("emission", Emission.NONE).forGetter(Emitter::emission),
            Look.CODEC.optionalFieldOf("look", Look.PLAIN).forGetter(Emitter::look),
            Motion.CODEC.optionalFieldOf("motion", Motion.FLOATING).forGetter(Emitter::motion),
            Extra.CODEC.optionalFieldOf("extra", Extra.NONE).forGetter(Emitter::extra))
            .apply(instance, Emitter::new));

    public Emitter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(emission, "emission");
        Objects.requireNonNull(look, "look");
        Objects.requireNonNull(motion, "motion");
        Objects.requireNonNull(extra, "extra");
    }

    /** It, turned on or off. */
    public Emitter withEnabled(final boolean on) {
        return new Emitter(this.name, this.kind, on, this.emission, this.look, this.motion, this.extra);
    }

    /** It, emitting as {@code changed}. */
    public Emitter withEmission(final Emission changed) {
        return new Emitter(this.name, this.kind, this.enabled, changed, this.look, this.motion, this.extra);
    }

    /** It, looking as {@code changed}. */
    public Emitter withLook(final Look changed) {
        return new Emitter(this.name, this.kind, this.enabled, this.emission, changed, this.motion, this.extra);
    }

    /** It, moving as {@code changed}. */
    public Emitter withMotion(final Motion changed) {
        return new Emitter(this.name, this.kind, this.enabled, this.emission, this.look, changed, this.extra);
    }

    /** It, its beam, arc, mark or screen reading {@code changed}. */
    public Emitter withExtra(final Extra changed) {
        return new Emitter(this.name, this.kind, this.enabled, this.emission, this.look, this.motion, changed);
    }

    /** What an emitter is. */
    public enum Kind implements IStableName {
        /** Particles. */
        PARTICLES("particles"),
        /** A straight beam to where the effect is aimed, a laser's. */
        BEAM("beam"),
        /** An electric arc to where the effect is aimed, jagged and branching, flickering as it goes. */
        ARC("arc"),
        /** A mark on the blocks around, scorch or soot, that fades. */
        MARK("mark"),
        /** What it does to the screen of whoever is near: a flash, a shake, a shockwave. */
        SCREEN("screen");

        private final String serializedName;

        Kind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** How what it emits is drawn: laid over what is behind it, or added to it as light. */
    public enum Blend implements IStableName {
        TRANSLUCENT("translucent"),
        ADDITIVE("additive");

        private final String serializedName;

        Blend(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** What its particles live in: anything, the air alone (steam over water goes out), or water alone (bubbles). */
    public enum Medium implements IStableName {
        ANY("any"),
        AIR("air"),
        WATER("water");

        private final String serializedName;

        Medium(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /**
     * What an emitter emits and when.
     *
     * @param shape    where its particles start
     * @param rate     how many a second
     * @param bursts   how many at once, at times after it starts
     * @param duration how long it emits, in seconds; 0 for as long as it is held, or once for one fired
     * @param life     how long each lives, in seconds
     * @param speed    how fast each leaves, in metres a second
     * @param children the emitters of the effect each of its particles carries, emitting from it
     */
    public record Emission(EmitterShape shape, double rate, List<Burst> bursts, double duration,
                           EffectCodecs.Range life, EffectCodecs.Range speed, List<String> children) {

        public static final Emission NONE = new Emission(EmitterShape.POINT, 0, List.of(), 0,
                new EffectCodecs.Range(1, 1), new EffectCodecs.Range(0, 0), List.of());
        public static final Codec<Emission> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EmitterShape.CODEC.optionalFieldOf("shape", EmitterShape.POINT).forGetter(Emission::shape),
                Codec.doubleRange(0, MOST_RATE).optionalFieldOf("rate", 0.0).forGetter(Emission::rate),
                Burst.CODEC.listOf().optionalFieldOf("bursts", List.of()).forGetter(Emission::bursts),
                Codec.doubleRange(0, MOST_SECONDS).optionalFieldOf("duration", 0.0).forGetter(Emission::duration),
                EffectCodecs.RANGE.optionalFieldOf("life", NONE.life()).forGetter(Emission::life),
                EffectCodecs.RANGE.optionalFieldOf("speed", NONE.speed()).forGetter(Emission::speed),
                Codec.STRING.listOf().optionalFieldOf("children", List.of()).forGetter(Emission::children))
                .apply(instance, Emission::new));

        public Emission {
            Objects.requireNonNull(shape, "shape");
            bursts = List.copyOf(bursts);
            Objects.requireNonNull(life, "life");
            Objects.requireNonNull(speed, "speed");
            children = List.copyOf(children);
        }

        /** It, emitting {@code perSecond}. */
        public Emission withRate(final double perSecond) {
            return new Emission(this.shape, perSecond, this.bursts, this.duration, this.life, this.speed,
                    this.children);
        }

        /** Its particles living {@code seconds}. */
        public Emission withLife(final EffectCodecs.Range seconds) {
            return new Emission(this.shape, this.rate, this.bursts, this.duration, seconds, this.speed,
                    this.children);
        }

        /** Its particles leaving at {@code metresASecond}. */
        public Emission withSpeed(final EffectCodecs.Range metresASecond) {
            return new Emission(this.shape, this.rate, this.bursts, this.duration, this.life, metresASecond,
                    this.children);
        }
    }

    /**
     * How many particles at once, and when.
     *
     * @param at    how long after the effect starts, in seconds
     * @param count how many
     */
    public record Burst(double at, int count) {

        public static final Codec<Burst> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, MOST_SECONDS).optionalFieldOf("at", 0.0).forGetter(Burst::at),
                Codec.intRange(1, MOST_BURST).fieldOf("count").forGetter(Burst::count))
                .apply(instance, Burst::new));
    }

    /**
     * How what an emitter emits looks over its life.
     *
     * @param sprite its picture: a particle sprite by name (frames named {@code _0}, {@code _1} after it when it has
     *               frames), or none for the game's plain one
     * @param frames how many frames its picture has, shown over its life; 1 for one
     * @param size   how big it is, in blocks, keys spread evenly over its life
     * @param colour its colour, {@code 0xAARRGGBB}, keys spread evenly over its life
     * @param spin   how fast it turns, either way, in degrees a second
     * @param blend  how it is drawn
     * @param light  the light of its own, 0 to 15; 15 at full light whatever is around
     * @param trail   how long a ribbon it leaves behind it, in seconds; 0 for none
     * @param width   a beam's, an arc's or a ribbon's width, in blocks
     * @param stretch how much of its path it is drawn along, in seconds, as a spark or a tracer streaks: as long as
     *                its size and as far as it goes in that time, along the way it goes, turned about that line to
     *                face the camera; 0 for a square
     */
    public record Look(String sprite, int frames, List<Double> size, List<Integer> colour, double spin, Blend blend,
                       int light, double trail, double width, double stretch) {

        public static final Look PLAIN = new Look("", 1, List.of(DEFAULT_SIZE), List.of(WHITE), 0, Blend.TRANSLUCENT,
                0, 0, DEFAULT_WIDTH, 0);
        public static final Codec<Look> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("sprite", "").forGetter(Look::sprite),
                Codec.intRange(1, MOST_FRAMES).optionalFieldOf("frames", 1).forGetter(Look::frames),
                Codec.doubleRange(0, MOST_SIZE).listOf().optionalFieldOf("size", PLAIN.size()).forGetter(Look::size),
                EffectCodecs.RGBA.listOf().optionalFieldOf("colour", PLAIN.colour()).forGetter(Look::colour),
                Codec.DOUBLE.optionalFieldOf("spin", 0.0).forGetter(Look::spin),
                StableCodecs.byName(Blend.class).optionalFieldOf("blend", Blend.TRANSLUCENT).forGetter(Look::blend),
                Codec.intRange(0, BRIGHTEST).optionalFieldOf("light", 0).forGetter(Look::light),
                Codec.doubleRange(0, MOST_SECONDS).optionalFieldOf("trail", 0.0).forGetter(Look::trail),
                Codec.doubleRange(0, MOST_SIZE).optionalFieldOf("width", DEFAULT_WIDTH).forGetter(Look::width),
                Codec.doubleRange(0, MOST_SECONDS).optionalFieldOf("stretch", 0.0).forGetter(Look::stretch))
                .apply(instance, Look::new));

        public Look {
            Objects.requireNonNull(sprite, "sprite");
            size = size.isEmpty() ? List.of(DEFAULT_SIZE) : List.copyOf(size);
            colour = colour.isEmpty() ? List.of(WHITE) : List.copyOf(colour);
            Objects.requireNonNull(blend, "blend");
        }

        /** It, its size keys times {@code factor}. */
        public Look withSizeTimes(final double factor) {
            return new Look(this.sprite, this.frames, this.size.stream().map(each -> each * factor).toList(),
                    this.colour, this.spin, this.blend, this.light, this.trail, this.width, this.stretch);
        }

        /** It, {@code blocks} wide as a beam, an arc or a ribbon. */
        public Look withWidth(final double blocks) {
            return new Look(this.sprite, this.frames, this.size, this.colour, this.spin, this.blend, this.light,
                    this.trail, blocks, this.stretch);
        }

        /** It, every colour key's alpha {@code alpha} (0 to 255) times as strong as its first's. */
        public Look withAlpha(final int alpha) {
            final int first = this.colour.getFirst() >>> ALPHA_SHIFT;
            final List<Integer> changed = this.colour.stream().map(each -> {
                final int own = each >>> ALPHA_SHIFT;
                final int now = first == 0 ? alpha : Math.clamp(own * alpha / first, 0, FULL_ALPHA);
                return now << ALPHA_SHIFT | each & RGB;
            }).toList();
            return new Look(this.sprite, this.frames, this.size, changed, this.spin, this.blend, this.light,
                    this.trail, this.width, this.stretch);
        }
    }

    /**
     * How what an emitter emits moves, by the place's physics ({@link EffectPhysics}).
     *
     * @param gravity  how much of the place's pull acts on it
     * @param drag     how quickly the air brings it to the wind's speed, at sea level, a second
     * @param buoyancy how hard it is lifted at sea level, metres a second a second
     * @param moves    what acts on it
     * @param collide  whether it stops at blocks
     * @param expands  whether it widens as the air thins, as a plume of gas does
     * @param medium   what it lives in
     */
    public record Motion(double gravity, double drag, double buoyancy, EffectPhysics.Moves moves, boolean collide,
                         boolean expands, Medium medium) {

        public static final Motion FLOATING = new Motion(0, 1, 0, EffectPhysics.Moves.ALL, false, false, Medium.ANY);
        public static final Codec<EffectPhysics.Moves> MOVES = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("gravity", true).forGetter(EffectPhysics.Moves::gravity),
                Codec.BOOL.optionalFieldOf("air", true).forGetter(EffectPhysics.Moves::air),
                Codec.BOOL.optionalFieldOf("wind", true).forGetter(EffectPhysics.Moves::wind))
                .apply(instance, EffectPhysics.Moves::new));
        public static final Codec<Motion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(-MOST_PULL, MOST_PULL).optionalFieldOf("gravity", 0.0).forGetter(Motion::gravity),
                Codec.doubleRange(0, MOST_PULL).optionalFieldOf("drag", 1.0).forGetter(Motion::drag),
                Codec.doubleRange(-MOST_PULL, MOST_PULL).optionalFieldOf("buoyancy", 0.0)
                        .forGetter(Motion::buoyancy),
                MOVES.optionalFieldOf("moved_by", EffectPhysics.Moves.ALL).forGetter(Motion::moves),
                Codec.BOOL.optionalFieldOf("collide", false).forGetter(Motion::collide),
                Codec.BOOL.optionalFieldOf("expands", false).forGetter(Motion::expands),
                StableCodecs.byName(Medium.class).optionalFieldOf("medium", Medium.ANY).forGetter(Motion::medium))
                .apply(instance, Motion::new));

        public Motion {
            Objects.requireNonNull(moves, "moves");
            Objects.requireNonNull(medium, "medium");
        }

        /** It, moved by {@code changed}. */
        public Motion withMoves(final EffectPhysics.Moves changed) {
            return new Motion(this.gravity, this.drag, this.buoyancy, changed, this.collide, this.expands,
                    this.medium);
        }
    }

    /**
     * What a beam, an arc, a mark or the screen read.
     *
     * @param jagged   how far an arc strays from the straight line, a share of its length
     * @param branches how many branches an arc throws off
     * @param radius   a mark's reach, or how near a player must be for the screen, in blocks
     * @param fade     how long a mark takes to fade, or a screen effect lasts, in seconds
     * @param flash    how bright a flash of the screen is, 0 to 1
     * @param shake    how hard the screen shakes, 0 to 1
     * @param shock    how strong a shockwave across the screen is, 0 to 1
     * @param length   how far a beam or an arc reaches along the effect's way when it is aimed nowhere, in blocks
     */
    public record Extra(double jagged, int branches, double radius, double fade, double flash, double shake,
                        double shock, double length) {

        public static final Extra NONE = new Extra(DEFAULT_JAGGED, 0, DEFAULT_RADIUS, DEFAULT_FADE, 0, 0, 0,
                DEFAULT_LENGTH);
        public static final Codec<Extra> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 1).optionalFieldOf("jagged", DEFAULT_JAGGED).forGetter(Extra::jagged),
                Codec.intRange(0, MOST_BRANCHES).optionalFieldOf("branches", 0).forGetter(Extra::branches),
                Codec.doubleRange(0, MOST_SIZE).optionalFieldOf("radius", DEFAULT_RADIUS).forGetter(Extra::radius),
                Codec.doubleRange(0, MOST_SECONDS).optionalFieldOf("fade", DEFAULT_FADE).forGetter(Extra::fade),
                Codec.doubleRange(0, 1).optionalFieldOf("flash", 0.0).forGetter(Extra::flash),
                Codec.doubleRange(0, 1).optionalFieldOf("shake", 0.0).forGetter(Extra::shake),
                Codec.doubleRange(0, 1).optionalFieldOf("shock", 0.0).forGetter(Extra::shock),
                Codec.doubleRange(0, MOST_SIZE).optionalFieldOf("length", DEFAULT_LENGTH).forGetter(Extra::length))
                .apply(instance, Extra::new));

        /** It, a mark fading or the screen's effect lasting {@code seconds}. */
        public Extra withFade(final double seconds) {
            return new Extra(this.jagged, this.branches, this.radius, seconds, this.flash, this.shake, this.shock,
                    this.length);
        }

        /** It, a mark or the screen reaching {@code blocks}. */
        public Extra withRadius(final double blocks) {
            return new Extra(this.jagged, this.branches, blocks, this.fade, this.flash, this.shake, this.shock,
                    this.length);
        }

        /** It, a beam or an arc aimed nowhere reaching {@code blocks}. */
        public Extra withLength(final double blocks) {
            return new Extra(this.jagged, this.branches, this.radius, this.fade, this.flash, this.shake, this.shock,
                    blocks);
        }

        /** It, the screen shaking by {@code share}. */
        public Extra withShake(final double share) {
            return new Extra(this.jagged, this.branches, this.radius, this.fade, this.flash, share, this.shock,
                    this.length);
        }
    }
}
