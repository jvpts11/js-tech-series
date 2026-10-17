/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jstech.core.audio.acoustics.ListeningPlace;
import dev.jstech.core.audio.acoustics.SoundConditions;
import dev.jstech.core.id.StableNames;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Reads when an ambience or a layer of music is heard, as a resource pack writes it under {@code when}: {@code
 * dimensions} and {@code biomes} (ids, or tags written {@code #namespace:path}), {@code weather} ({@code clear},
 * {@code rain}, {@code thunder}), {@code time} with its {@code from} and {@code to} in ticks of the day, {@code
 * indoors} true or false, {@code depth} with its {@code min} and {@code max} in blocks below the open sky, and {@code
 * near}, a list of {@code what}, {@code radius} and {@code least}. What a file leaves out holds everywhere.
 */
final class SoundConditionsJson {

    private static final StableNames<ListeningPlace.Weather> WEATHER = StableNames.of(ListeningPlace.Weather.class);

    private SoundConditionsJson() {
    }

    /** The conditions written under {@code when}, or anywhere and always when there is none. */
    static SoundConditions read(@Nullable final JsonObject when) {
        if (when == null) {
            return SoundConditions.ANYWHERE;
        }
        final Set<ListeningPlace.Weather> weather = new LinkedHashSet<>();
        for (final String one : strings(when, "weather")) {
            final ListeningPlace.Weather named = WEATHER.find(one.toLowerCase(Locale.ROOT));
            if (named == null) {
                throw new IllegalArgumentException("no weather is called " + one + "; there are " + WEATHER.names());
            }
            weather.add(named);
        }
        final JsonObject time = when.has("time") ? when.getAsJsonObject("time") : new JsonObject();
        final JsonObject depth = when.has("depth") ? when.getAsJsonObject("depth") : new JsonObject();
        final List<SoundConditions.Near> near = new ArrayList<>();
        if (when.has("near")) {
            for (final JsonElement element : when.getAsJsonArray("near")) {
                final JsonObject one = element.getAsJsonObject();
                near.add(new SoundConditions.Near(id(one.get("what").getAsString()), one.get("radius").getAsInt(),
                        one.has("least") ? one.get("least").getAsInt() : 1));
            }
        }
        return new SoundConditions(ids(when, "dimensions"), ids(when, "biomes"), weather,
                time.has("from") ? time.get("from").getAsInt() : 0, time.has("to") ? time.get("to").getAsInt() : 0,
                when.has("indoors") ? Optional.of(when.get("indoors").getAsBoolean()) : Optional.empty(),
                depth.has("min") ? depth.get("min").getAsInt() : 0,
                depth.has("max") ? depth.get("max").getAsInt() : Integer.MAX_VALUE, near);
    }

    /* A list of ids or tags, each written in full. */
    private static Set<String> ids(final JsonObject when, final String key) {
        final Set<String> out = new LinkedHashSet<>();
        for (final String one : strings(when, key)) {
            out.add(id(one));
        }
        return out;
    }

    /* An id, or a tag written {@code #namespace:path}, with the game's namespace where it gives none. */
    private static String id(final String written) {
        return written.startsWith("#") ? "#" + ResourceLocation.parse(written.substring(1))
                : ResourceLocation.parse(written).toString();
    }

    private static List<String> strings(final JsonObject when, final String key) {
        final List<String> out = new ArrayList<>();
        if (!when.has(key)) {
            return out;
        }
        final JsonArray array = when.getAsJsonArray(key);
        for (final JsonElement element : array) {
            out.add(element.getAsString());
        }
        return out;
    }
}
