/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A pressure suit: its four pieces, which seal it only worn together; the air its tanks hold, the one thing it spends,
 * the clock of every walk outside; and the cold and the heat it stands. Sealed and with air in its tanks, its wearer
 * breathes where there is no air, and stands any cold or heat within its range.
 *
 * <pre>{@code
 * {"name": "jsspace.suit.lunar", "helmet": "jsspace:lunar_helmet", "chest": "jsspace:lunar_suit",
 *  "legs": "jsspace:lunar_trousers", "feet": "jsspace:lunar_boots", "air": 14400, "coldest": -150, "hottest": 120}
 * }</pre>
 *
 * @param name    the key of its name in the language files
 * @param helmet  its helmet's item
 * @param chest   the item worn on the chest, which carries its tanks and its pack
 * @param legs    its trousers' item
 * @param feet    its boots' item
 * @param air     how many seconds of breathing its tanks hold, full, before any module adds to them
 * @param coldest the coldest it stands, in degrees Celsius
 * @param hottest the hottest
 */
public record SpaceSuit(String name, ResourceLocation helmet, ResourceLocation chest, ResourceLocation legs,
                        ResourceLocation feet, int air, double coldest, double hottest) {

    /** As a data pack's file writes one. */
    public static final Codec<SpaceSuit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(SpaceSuit::name),
            ResourceLocation.CODEC.fieldOf("helmet").forGetter(SpaceSuit::helmet),
            ResourceLocation.CODEC.fieldOf("chest").forGetter(SpaceSuit::chest),
            ResourceLocation.CODEC.fieldOf("legs").forGetter(SpaceSuit::legs),
            ResourceLocation.CODEC.fieldOf("feet").forGetter(SpaceSuit::feet),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("air").forGetter(SpaceSuit::air),
            Codec.doubleRange(-273.15, 10_000).fieldOf("coldest").forGetter(SpaceSuit::coldest),
            Codec.doubleRange(-273.15, 10_000).fieldOf("hottest").forGetter(SpaceSuit::hottest)
    ).apply(instance, SpaceSuit::new));

    public SpaceSuit {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(helmet, "helmet");
        Objects.requireNonNull(chest, "chest");
        Objects.requireNonNull(legs, "legs");
        Objects.requireNonNull(feet, "feet");
    }

    /** Whether it stands a place {@code celsius} warm. */
    public boolean stands(final double celsius) {
        return celsius >= this.coldest && celsius <= this.hottest;
    }
}
