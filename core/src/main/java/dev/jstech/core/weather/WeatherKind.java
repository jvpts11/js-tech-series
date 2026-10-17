/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.light.LightColours;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of weather: what falls and in what colour, how far it lets anyone see and in what fog, how hard it makes the
 * wind blow, what it does to a body out in it, and how often its lightning strikes. It comes and goes with its world
 * event, which says where, when and for how long, and warns of it.
 *
 * <pre>{@code
 * { "event": "mymod:acid_rain", "falls": "rain", "colour": "#9CE05A", "density": 0.6,
 *   "fog_colour": "#7C9A5A", "sight": 0.3, "harm": { "damage_type": "mymod:acid", "amount": 1, "every": 2 } }
 * }</pre>
 *
 * @param event     the world event that brings it
 * @param falls     what falls in it
 * @param colour    what falls, in its colour
 * @param density   how thick it falls, 0 to 1
 * @param fogColour the fog's colour in it, if it has fog of its own
 * @param sight     how much it closes the view in, 0 for not at all to 1 for a few blocks
 * @param wind      how hard it makes the wind blow, as a share of the wind without it
 * @param harm      what it does to a body out in it, if anything
 * @param effect    the effect it gives a body out in it, if any
 * @param lightning how many times a minute its lightning strikes near someone out in it
 * @param sheltered whether a roof keeps it off; false for what reaches indoors too, as a choking dust might
 */
public record WeatherKind(ResourceLocation event, Fall falls, int colour, double density, Optional<Integer> fogColour,
                          double sight, double wind, Optional<Harm> harm, Optional<Effect> effect, double lightning,
                          boolean sheltered) {

    /** The most a weather may make the wind blow, and the most strikes a minute. */
    public static final double MOST_WIND = 20.0;
    public static final double MOST_LIGHTNING = 60.0;
    public static final Codec<WeatherKind> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("event").forGetter(WeatherKind::event),
            StableCodecs.byName(Fall.class).optionalFieldOf("falls", Fall.NONE).forGetter(WeatherKind::falls),
            LightColours.COLOUR.optionalFieldOf("colour", 0xFFFFFF).forGetter(WeatherKind::colour),
            Codec.doubleRange(0, 1).optionalFieldOf("density", 0.5).forGetter(WeatherKind::density),
            LightColours.COLOUR.optionalFieldOf("fog_colour").forGetter(WeatherKind::fogColour),
            Codec.doubleRange(0, 1).optionalFieldOf("sight", 0.0).forGetter(WeatherKind::sight),
            Codec.doubleRange(0, MOST_WIND).optionalFieldOf("wind", 1.0).forGetter(WeatherKind::wind),
            Harm.CODEC.optionalFieldOf("harm").forGetter(WeatherKind::harm),
            Effect.CODEC.optionalFieldOf("effect").forGetter(WeatherKind::effect),
            Codec.doubleRange(0, MOST_LIGHTNING).optionalFieldOf("lightning", 0.0).forGetter(WeatherKind::lightning),
            Codec.BOOL.optionalFieldOf("sheltered", true).forGetter(WeatherKind::sheltered))
            .apply(instance, WeatherKind::new));

    /** What falls in a weather. */
    public enum Fall implements IStableName {
        /** Nothing: a weather of fog, wind or lightning alone. */
        NONE("none"),
        /** Drops falling fast, rain of any liquid. */
        RAIN("rain"),
        /** Flakes drifting down, snow of any ice. */
        SNOW("snow"),
        /** Dust blown along with the wind. */
        DUST("dust");

        private final String serializedName;

        Fall(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /**
     * What a weather does to a body out in it.
     *
     * @param damageType the kind of harm, a damage type by id
     * @param amount     how much, in half hearts
     * @param every      every how many seconds
     */
    public record Harm(ResourceLocation damageType, float amount, int every) {

        public static final Codec<Harm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("damage_type").forGetter(Harm::damageType),
                Codec.floatRange(0, 1_000).optionalFieldOf("amount", 1F).forGetter(Harm::amount),
                Codec.intRange(1, 3_600).optionalFieldOf("every", 1).forGetter(Harm::every))
                .apply(instance, Harm::new));
    }

    /**
     * The effect a weather gives a body out in it, renewed while it stays out.
     *
     * @param effect    the effect, by id
     * @param amplifier how strong, from 0
     * @param seconds   how long each renewal lasts
     */
    public record Effect(ResourceLocation effect, int amplifier, int seconds) {

        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(Effect::effect),
                Codec.intRange(0, 255).optionalFieldOf("amplifier", 0).forGetter(Effect::amplifier),
                Codec.intRange(1, 3_600).optionalFieldOf("seconds", 4).forGetter(Effect::seconds))
                .apply(instance, Effect::new));
    }
}
