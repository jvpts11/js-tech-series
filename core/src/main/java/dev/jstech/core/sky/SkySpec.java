/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.sky;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.light.LightColours;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * What a dimension's sky holds, drawn by the Core for every dimension whose kind of place is drawn as
 * {@link Skies#EFFECTS}: its colour, its fog's and its sunsets' (the blue sunsets of Mars); its suns, each coloured and
 * sized by its kind of star and dimmer the farther it is; the planets and moons in it, with their phases, their
 * surfaces turning and their rings, which eclipse a sun as the geometry says; square stars, the band of the galaxy and
 * nebulae; auroras; clouds of their own height, colour and density; and haze.
 *
 * <pre>{@code
 * { "sky_colour": "#C99A6E", "sunset_colour": "#5E8FD6",
 *   "suns": [{ "kind": "g", "distance": 1.52 }],
 *   "bodies": [{ "texture": "mymod:textures/sky/phobos.png", "size": 0.4, "orbit_days": 0.32, "tilt": 10 }],
 *   "clouds": { "height": 180, "colour": "#E8D2B8", "density": 0.3 },
 *   "haze": { "colour": "#C08A60", "density": 0.4 } }
 * }</pre>
 *
 * @param skyColour    the sky's colour at noon, if not the biome's
 * @param fogColour    the fog's colour at noon, if not the biome's
 * @param sunsetColour the colour of its sunrises and sunsets, if not the game's orange
 * @param suns         its suns, the first the one its clock follows
 * @param bodies       the planets and moons in its sky
 * @param stars        its stars at night, or by day too
 * @param galaxy       the band of its galaxy, if it shows
 * @param nebulae      the nebulae among its stars
 * @param aurora       the aurora over its north at night, if it has one
 * @param clouds       its clouds, if not the game's; a density of 0 for none at all
 * @param haze         the haze in its air, if any
 * @param round        whether its world is round: its border from west to east goes once round it and from north to
 *                     south from pole to pole, so the sun's hour follows the longitude and its path leans with the
 *                     latitude, as on a real world
 * @param ground       whether it has ground under it: with none, as in a space, the sky goes on under the horizon
 *                     however low the eye is, never dark under it as the game draws the void under a world
 */
public record SkySpec(Optional<Integer> skyColour, Optional<Integer> fogColour, Optional<Integer> sunsetColour,
                      List<Sun> suns, List<Body> bodies, Stars stars, Optional<Galaxy> galaxy, List<Nebula> nebulae,
                      Optional<Aurora> aurora, Optional<Clouds> clouds, Optional<Haze> haze, boolean round,
                      boolean ground) {

    /** The farthest a sun may be, in astronomical units, and the nearest. */
    public static final double FARTHEST = 1_000.0;
    public static final double NEAREST = 0.01;
    /** The largest a sun or a body may look, beside the overworld's sun or moon. */
    public static final double LARGEST = 20.0;
    /** The overworld's sky: one yellow sun, its stars, and nothing else of the Core's. */
    public static final SkySpec DEFAULT = new SkySpec(Optional.empty(), Optional.empty(), Optional.empty(),
            List.of(Sun.OURS), List.of(), Stars.DEFAULT, Optional.empty(), List.of(), Optional.empty(),
            Optional.empty(), Optional.empty(), false, true);
    public static final Codec<SkySpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LightColours.COLOUR.optionalFieldOf("sky_colour").forGetter(SkySpec::skyColour),
            LightColours.COLOUR.optionalFieldOf("fog_colour").forGetter(SkySpec::fogColour),
            LightColours.COLOUR.optionalFieldOf("sunset_colour").forGetter(SkySpec::sunsetColour),
            Sun.CODEC.listOf().optionalFieldOf("suns", List.of(Sun.OURS)).forGetter(SkySpec::suns),
            Body.CODEC.listOf().optionalFieldOf("bodies", List.of()).forGetter(SkySpec::bodies),
            Stars.CODEC.optionalFieldOf("stars", Stars.DEFAULT).forGetter(SkySpec::stars),
            Galaxy.CODEC.optionalFieldOf("galaxy").forGetter(SkySpec::galaxy),
            Nebula.CODEC.listOf().optionalFieldOf("nebulae", List.of()).forGetter(SkySpec::nebulae),
            Aurora.CODEC.optionalFieldOf("aurora").forGetter(SkySpec::aurora),
            Clouds.CODEC.optionalFieldOf("clouds").forGetter(SkySpec::clouds),
            Haze.CODEC.optionalFieldOf("haze").forGetter(SkySpec::haze),
            Codec.BOOL.optionalFieldOf("round", false).forGetter(SkySpec::round),
            Codec.BOOL.optionalFieldOf("ground", true).forGetter(SkySpec::ground))
            .apply(instance, SkySpec::new));

    public SkySpec {
        suns = List.copyOf(suns);
        bodies = List.copyOf(bodies);
        nebulae = List.copyOf(nebulae);
    }

    /**
     * A sun in the sky.
     *
     * @param kind     its kind of star, which gives its colour and its size
     * @param size     how much larger than its kind it looks, for a sun nearer or farther than its distance says
     * @param offset   how far round the sky it stands from where the clock puts the first sun, in turns
     * @param distance how far it is, in astronomical units: the farther, the smaller and dimmer
     * @param drawn    whether the Core draws it where the clock puts it; a sun a mod draws itself where it stands,
     *                 as in a space where the player flies among the worlds, still lights the day and tints it
     */
    public record Sun(StarKind kind, double size, double offset, double distance, boolean drawn) {

        /** The overworld's sun. */
        public static final Sun OURS = new Sun(StarKind.G, 1.0, 0.0, 1.0, true);
        public static final Codec<Sun> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StableCodecs.byName(StarKind.class).optionalFieldOf("kind", StarKind.G).forGetter(Sun::kind),
                Codec.doubleRange(0.01, LARGEST).optionalFieldOf("size", 1.0).forGetter(Sun::size),
                Codec.doubleRange(-1, 1).optionalFieldOf("offset", 0.0).forGetter(Sun::offset),
                Codec.doubleRange(NEAREST, FARTHEST).optionalFieldOf("distance", 1.0).forGetter(Sun::distance),
                Codec.BOOL.optionalFieldOf("drawn", true).forGetter(Sun::drawn))
                .apply(instance, Sun::new));

        /** How large it looks beside the overworld's sun: its kind's size, its own, shrinking with distance. */
        public double apparentSize() {
            return Math.min(LARGEST, this.kind.size() * this.size / this.distance);
        }
    }

    /**
     * A planet or a moon in the sky: lit by the first sun from where it stands, so its phases follow, its surface
     * turning, round the sky on its own path; or, too far to show a disc, a point of its colour shining as the stars
     * do.
     *
     * @param texture   its surface, a map of the whole of it from pole to pole; none for a plain colour
     * @param colour    its colour, the surface's tint, or its whole colour with no surface
     * @param size      how large it looks beside the overworld's moon
     * @param orbitDays how many days it takes to come round to the sun again; 0 to stay where it stands from it, and
     *                  below 0 going round the other way, as a moon that rises in the west, Phobos, does
     * @param offset    where it stands from the first sun as the world begins, in turns
     * @param tilt      how far its path leans from the sun's, in degrees
     * @param turnDays  how many days its surface takes to turn once; 0 for a face it never turns
     * @param rings     its rings, if it has any
     * @param point     whether it shows as a point of light, as a planet far off does, rather than a lit disc
     * @param squash    how tall its disc is beside how wide, for a lump that is not round
     */
    public record Body(Optional<ResourceLocation> texture, int colour, double size, double orbitDays, double offset,
                       double tilt, double turnDays, Optional<Rings> rings, boolean point, double squash) {

        public static final Codec<Body> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(Body::texture),
                LightColours.COLOUR.optionalFieldOf("colour", 0xFFFFFF).forGetter(Body::colour),
                Codec.doubleRange(0.01, LARGEST).optionalFieldOf("size", 1.0).forGetter(Body::size),
                Codec.doubleRange(-1_000_000, 1_000_000).optionalFieldOf("orbit_days", 0.0)
                        .forGetter(Body::orbitDays),
                Codec.doubleRange(-1, 1).optionalFieldOf("offset", 0.5).forGetter(Body::offset),
                Codec.doubleRange(-90, 90).optionalFieldOf("tilt", 0.0).forGetter(Body::tilt),
                Codec.doubleRange(0, 1_000_000).optionalFieldOf("turn_days", 0.0).forGetter(Body::turnDays),
                Rings.CODEC.optionalFieldOf("rings").forGetter(Body::rings),
                Codec.BOOL.optionalFieldOf("point", false).forGetter(Body::point),
                Codec.doubleRange(0.1, 1).optionalFieldOf("squash", 1.0).forGetter(Body::squash))
                .apply(instance, Body::new));
    }

    /**
     * A body's rings, a flat band round it.
     *
     * @param colour  their colour
     * @param inner   how far from the body's middle they begin, in its radii
     * @param outer   how far they reach, in its radii
     * @param tilt    how far they lean towards whoever looks, in degrees: 0 seen edge on, 90 seen face on
     * @param opacity how much of the sky behind they hide, 0 to 1
     */
    public record Rings(int colour, double inner, double outer, double tilt, double opacity) {

        public static final Codec<Rings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LightColours.COLOUR.optionalFieldOf("colour", 0xD8C9A0).forGetter(Rings::colour),
                Codec.doubleRange(1, 20).optionalFieldOf("inner", 1.4).forGetter(Rings::inner),
                Codec.doubleRange(1, 20).optionalFieldOf("outer", 2.3).forGetter(Rings::outer),
                Codec.doubleRange(0, 90).optionalFieldOf("tilt", 20.0).forGetter(Rings::tilt),
                Codec.doubleRange(0, 1).optionalFieldOf("opacity", 0.8).forGetter(Rings::opacity))
                .apply(instance, Rings::new));
    }

    /**
     * Its stars at night, square as the game's are, or by day too where no air scatters the sunlight over them.
     *
     * @param count      how many there are
     * @param brightness how bright they shine, beside the overworld's
     * @param byDay      how bright they shine by day, as a share of how bright they shine at midnight: 0 for none
     */
    public record Stars(int count, double brightness, double byDay) {

        public static final Stars DEFAULT = new Stars(1_500, 1.0, 0.0);
        public static final Codec<Stars> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 20_000).optionalFieldOf("count", DEFAULT.count()).forGetter(Stars::count),
                Codec.doubleRange(0, 4).optionalFieldOf("brightness", DEFAULT.brightness())
                        .forGetter(Stars::brightness),
                Codec.doubleRange(0, 1).optionalFieldOf("by_day", DEFAULT.byDay()).forGetter(Stars::byDay))
                .apply(instance, Stars::new));
    }

    /**
     * The band of the galaxy across the night: a glow and stars crowding along a great circle of the sky.
     *
     * @param colour     its glow's colour
     * @param brightness how bright it shines, 0 to 1
     * @param tilt       how far its band leans from the sun's path, in degrees
     */
    public record Galaxy(int colour, double brightness, double tilt) {

        public static final Codec<Galaxy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LightColours.COLOUR.optionalFieldOf("colour", 0xC8D0FF).forGetter(Galaxy::colour),
                Codec.doubleRange(0, 1).optionalFieldOf("brightness", 0.5).forGetter(Galaxy::brightness),
                Codec.doubleRange(-90, 90).optionalFieldOf("tilt", 60.0).forGetter(Galaxy::tilt))
                .apply(instance, Galaxy::new));
    }

    /**
     * A nebula among the stars: a soft cloud of light of its colour.
     *
     * @param colour     its colour
     * @param yaw        where it stands round the sky, in degrees
     * @param pitch      how high it stands, in degrees
     * @param size       how wide it is, in degrees of sky
     * @param brightness how bright it glows, 0 to 1
     */
    public record Nebula(int colour, double yaw, double pitch, double size, double brightness) {

        public static final Codec<Nebula> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LightColours.COLOUR.fieldOf("colour").forGetter(Nebula::colour),
                Codec.doubleRange(-360, 360).optionalFieldOf("yaw", 0.0).forGetter(Nebula::yaw),
                Codec.doubleRange(-90, 90).optionalFieldOf("pitch", 30.0).forGetter(Nebula::pitch),
                Codec.doubleRange(1, 120).optionalFieldOf("size", 20.0).forGetter(Nebula::size),
                Codec.doubleRange(0, 1).optionalFieldOf("brightness", 0.4).forGetter(Nebula::brightness))
                .apply(instance, Nebula::new));
    }

    /**
     * An aurora: curtains of light waving over the north at night.
     *
     * @param colour    the colour at the curtains' foot
     * @param topColour the colour they fade into at the top
     * @param strength  how bright they shine, 0 to 1
     */
    public record Aurora(int colour, int topColour, double strength) {

        public static final Codec<Aurora> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LightColours.COLOUR.optionalFieldOf("colour", 0x40FF90).forGetter(Aurora::colour),
                LightColours.COLOUR.optionalFieldOf("top_colour", 0xA040FF).forGetter(Aurora::topColour),
                Codec.doubleRange(0, 1).optionalFieldOf("strength", 0.7).forGetter(Aurora::strength))
                .apply(instance, Aurora::new));
    }

    /**
     * Its clouds, a layer of their own.
     *
     * @param height  how high they float
     * @param colour  their colour at noon
     * @param density how thick they are, 0 for none to 1
     */
    public record Clouds(double height, int colour, double density) {

        public static final Codec<Clouds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(-64, 2_000).optionalFieldOf("height", 192.0).forGetter(Clouds::height),
                LightColours.COLOUR.optionalFieldOf("colour", 0xFFFFFF).forGetter(Clouds::colour),
                Codec.doubleRange(0, 1).optionalFieldOf("density", 0.8).forGetter(Clouds::density))
                .apply(instance, Clouds::new));
    }

    /**
     * The haze in its air, which closes the view in.
     *
     * @param colour  its colour at noon
     * @param density how thick it is, 0 for clear to 1 for a few blocks of sight
     */
    public record Haze(int colour, double density) {

        public static final Codec<Haze> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LightColours.COLOUR.fieldOf("colour").forGetter(Haze::colour),
                Codec.doubleRange(0, 1).optionalFieldOf("density", 0.3).forGetter(Haze::density))
                .apply(instance, Haze::new));
    }
}
