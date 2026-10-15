/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.jstech.core.gui.flex.Align;
import dev.jstech.core.gui.flex.Insets;
import dev.jstech.core.gui.flex.Justify;
import dev.jstech.core.gui.flex.Track;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * How a screen's file writes the layout's values: room around a box as one number, two or four; where a box stands and
 * where the room left goes as words; a grid's columns as {@code "auto"}, {@code "2fr"} or a width in pixels.
 */
public final class ScreenCodecs {

    /** Room around a box: {@code 4}, {@code [2, 6]} (above and below, left and right) or {@code [1, 2, 3, 4]}. */
    public static final Codec<Insets> INSETS = Codec.either(Codec.intRange(0, 256), Codec.intRange(0, 256).listOf())
            .comapFlatMap(ScreenCodecs::insets, insets -> Either.right(List.of(insets.top(), insets.right(),
                    insets.bottom(), insets.left())));

    /** Where a box stands: {@code start}, {@code center}, {@code end} or {@code stretch}. */
    public static final Codec<Align> ALIGN = named(Align.class);

    /** Where the room left goes: {@code start}, {@code center}, {@code end}, {@code space_between}... */
    public static final Codec<Justify> JUSTIFY = named(Justify.class);

    /** A grid column: {@code "auto"}, {@code "1fr"}, {@code "18"} or {@code 18}. */
    public static final Codec<Track> TRACK = Codec.either(Codec.intRange(0, 4096), Codec.STRING)
            .comapFlatMap(either -> either.map(pixels -> DataResult.success(Track.fixed(pixels)), ScreenCodecs::track),
                    track -> switch (track.kind()) {
                        case FIXED -> Either.left(track.value());
                        case AUTO -> Either.right("auto");
                        case FRACTION -> Either.right(track.value() + "fr");
                    });

    private ScreenCodecs() {
    }

    /** An enum written as its constants' names in lower case. */
    public static <E extends Enum<E>> Codec<E> named(final Class<E> kind) {
        return Codec.STRING.comapFlatMap(word -> {
            for (final E constant : kind.getEnumConstants()) {
                if (constant.name().toLowerCase(Locale.ROOT).equals(word)) {
                    return DataResult.success(constant);
                }
            }
            return DataResult.error(() -> "\"" + word + "\" is not one of " + words(kind));
        }, constant -> constant.name().toLowerCase(Locale.ROOT));
    }

    private static <E extends Enum<E>> List<String> words(final Class<E> kind) {
        return Arrays.stream(kind.getEnumConstants()).map(c -> c.name().toLowerCase(Locale.ROOT)).toList();
    }

    private static DataResult<Insets> insets(final Either<Integer, List<Integer>> either) {
        return either.map(all -> DataResult.success(Insets.of(all)), list -> switch (list.size()) {
            case 1 -> DataResult.success(Insets.of(list.get(0)));
            case 2 -> DataResult.success(Insets.of(list.get(0), list.get(1)));
            case 4 -> DataResult.success(new Insets(list.get(0), list.get(1), list.get(2), list.get(3)));
            default -> DataResult.error(() -> "room around a box is one number, two or four, not " + list.size());
        });
    }

    private static DataResult<Track> track(final String text) {
        final String word = text.trim().toLowerCase(Locale.ROOT);
        if (word.equals("auto")) {
            return DataResult.success(Track.auto());
        }
        try {
            if (word.endsWith("fr")) {
                return DataResult.success(Track.fraction(Integer.parseInt(word.substring(0, word.length() - 2))));
            }
            return DataResult.success(Track.fixed(Integer.parseInt(word)));
        } catch (final NumberFormatException notANumber) {
            return DataResult.error(() -> "\"" + text + "\" is no grid column: write auto, 1fr or a width");
        }
    }
}
