/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.gui.flex.Align;
import dev.jstech.core.gui.flex.FlexBox;
import dev.jstech.core.gui.flex.Insets;
import dev.jstech.core.gui.flex.Justify;
import dev.jstech.core.gui.flex.Track;
import java.util.List;
import java.util.Optional;

/**
 * What a component that holds others says of how it lays them out: the room between them and around them, where they
 * stand and where the room left goes, whether a row wraps, and a grid's columns. A value it does not set comes from
 * the style the screen is drawn in, so a style with more room between its controls gives every screen that room.
 *
 * @param gap     the room between two of what it holds, or none for the style's
 * @param padding the room inside it, around what it holds, or none for the style's
 * @param align   where what it holds stands across its rows or columns
 * @param justify where the room nobody grows into goes
 * @param wrap    whether a row that runs out of room goes on under it
 * @param columns a grid's columns
 */
public record ContainerProps(Optional<Integer> gap, Optional<Insets> padding, Align align, Justify justify,
                             boolean wrap, List<Track> columns) {

    /** Nothing said: the style's gap and padding, stretched, gathered at the start. */
    public static final ContainerProps NONE = new ContainerProps(Optional.empty(), Optional.empty(), Align.STRETCH,
            Justify.START, false, List.of());

    /** The fields as a file writes them. */
    public static final MapCodec<ContainerProps> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 256).optionalFieldOf("gap").forGetter(ContainerProps::gap),
            ScreenCodecs.INSETS.optionalFieldOf("padding").forGetter(ContainerProps::padding),
            ScreenCodecs.ALIGN.optionalFieldOf("align", Align.STRETCH).forGetter(ContainerProps::align),
            ScreenCodecs.JUSTIFY.optionalFieldOf("justify", Justify.START).forGetter(ContainerProps::justify),
            Codec.BOOL.optionalFieldOf("wrap", false).forGetter(ContainerProps::wrap),
            ScreenCodecs.TRACK.listOf().optionalFieldOf("columns", List.of()).forGetter(ContainerProps::columns)
    ).apply(i, ContainerProps::new));

    public ContainerProps {
        columns = List.copyOf(columns);
    }

    /** Sets on {@code box} how it lays out what it holds, the style's gap and padding where these say none. */
    public FlexBox applyTo(final FlexBox box, final int styleGap, final int stylePadding) {
        return box.gap(gap.orElse(styleGap)).padding(padding.orElse(Insets.of(stylePadding))).align(align)
                .justify(justify).wrap(wrap);
    }
}
