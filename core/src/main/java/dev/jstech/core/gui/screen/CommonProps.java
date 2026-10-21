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
import java.util.Optional;

/**
 * What every component of a screen's file may say, whatever kind it is: the name the screen's code finds it by, its
 * size and how it shares room, the room around it, where it stands, a tooltip, and the values it is shown and usable
 * by.
 *
 * @param id        the name the screen's code finds it by
 * @param width     its own width, or none to be as wide as it needs
 * @param height    its own height, or none to be as tall as it needs
 * @param minWidth  the narrowest it may be
 * @param minHeight the shortest it may be
 * @param maxWidth  the widest it may be
 * @param maxHeight the tallest it may be
 * @param grow      its share of the room its row or column leaves
 * @param shrink    how readily it gives room up
 * @param margin    the room around it
 * @param alignSelf where it stands across its row or column
 * @param span      how many grid columns it takes
 * @param tooltip   the translation key of what the pointer resting on it shows
 * @param narration the translation key of the name the game's narrator calls it by, for one with no words of its
 *                  own to read (a switch, a slider, a field beside its label)
 * @param visibleIf the name of a value that says whether it is shown
 * @param enabledIf the name of a value that says whether it can be used
 */
public record CommonProps(Optional<String> id, Optional<Integer> width, Optional<Integer> height, int minWidth,
                          int minHeight, Optional<Integer> maxWidth, Optional<Integer> maxHeight,
                          Optional<Integer> grow, Optional<Integer> shrink, Insets margin, Optional<Align> alignSelf,
                          int span, Optional<String> tooltip, Optional<String> narration, Optional<String> visibleIf,
                          Optional<String> enabledIf) {

    /** Nothing said. */
    public static final CommonProps NONE = new CommonProps(Optional.empty(), Optional.empty(), Optional.empty(), 0, 0,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Insets.NONE, Optional.empty(), 1,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    private static final int MOST = 4096;

    /** The fields as a file writes them, beside the component's own. */
    public static final MapCodec<CommonProps> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("id").forGetter(CommonProps::id),
            Codec.intRange(0, MOST).optionalFieldOf("width").forGetter(CommonProps::width),
            Codec.intRange(0, MOST).optionalFieldOf("height").forGetter(CommonProps::height),
            Codec.intRange(0, MOST).optionalFieldOf("min_width", 0).forGetter(CommonProps::minWidth),
            Codec.intRange(0, MOST).optionalFieldOf("min_height", 0).forGetter(CommonProps::minHeight),
            Codec.intRange(0, MOST).optionalFieldOf("max_width").forGetter(CommonProps::maxWidth),
            Codec.intRange(0, MOST).optionalFieldOf("max_height").forGetter(CommonProps::maxHeight),
            Codec.intRange(0, 100).optionalFieldOf("grow").forGetter(CommonProps::grow),
            Codec.intRange(0, 100).optionalFieldOf("shrink").forGetter(CommonProps::shrink),
            ScreenCodecs.INSETS.optionalFieldOf("margin", Insets.NONE).forGetter(CommonProps::margin),
            ScreenCodecs.ALIGN.optionalFieldOf("align_self").forGetter(CommonProps::alignSelf),
            Codec.intRange(1, 64).optionalFieldOf("span", 1).forGetter(CommonProps::span),
            Codec.STRING.optionalFieldOf("tooltip").forGetter(CommonProps::tooltip),
            Codec.STRING.optionalFieldOf("narration").forGetter(CommonProps::narration),
            Codec.STRING.optionalFieldOf("visible_if").forGetter(CommonProps::visibleIf),
            Codec.STRING.optionalFieldOf("enabled_if").forGetter(CommonProps::enabledIf)
    ).apply(i, CommonProps::new));

    /** Sets on {@code box} what these say of its size and its place, leaving what they do not say as it was. */
    public FlexBox applyTo(final FlexBox box) {
        width.ifPresent(box::width);
        height.ifPresent(box::height);
        maxWidth.ifPresent(box::maxWidth);
        maxHeight.ifPresent(box::maxHeight);
        grow.ifPresent(box::grow);
        shrink.ifPresent(box::shrink);
        alignSelf.ifPresent(box::alignSelf);
        if (minWidth > 0) {
            box.minWidth(minWidth);
        }
        if (minHeight > 0) {
            box.minHeight(minHeight);
        }
        if (!margin.equals(Insets.NONE)) {
            box.margin(margin);
        }
        return box.span(span);
    }
}
