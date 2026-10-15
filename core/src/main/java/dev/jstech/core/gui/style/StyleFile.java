/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.palette.PaletteRoles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A style as its file writes it, in a resource pack's {@code assets/<namespace>/jstech/styles/<name>.json}: what it
 * sets, and the style it builds on for the rest. A resource pack replaces a style by writing a file of the same name,
 * and adds one by writing a new name.
 *
 * <pre>{@code
 * {
 *   "name": "jscore.style.high_contrast",
 *   "parent": "jscore:jstech",
 *   "colours": {"screen": "#FF000000", "text": "#FFFFFFFF", "accent": "#FFFFFF00"},
 *   "pieces": {"button": {"kind": "flat", "fill": "panel", "edge": "text"}},
 *   "metrics": {"gap": 5},
 *   "finish": {"double_bevel": false}
 * }
 * }</pre>
 *
 * @param name       the translation key of the style's name, as the picker shows it
 * @param parent     the style it builds on, whose every part it keeps unless it sets its own
 * @param palette    a palette whose colours it takes, by the roles' names, under those it sets itself
 * @param colours    colours by role key, {@code "#AARRGGBB"} or {@code "#RRGGBB"}
 * @param pieces     how it draws each piece, by the piece's key
 * @param metrics    the spacing it sets
 * @param finish     what it lays over its flat pieces
 * @param font       the font its text is drawn in
 * @param clickSound the sound of a press
 * @param textShadow whether its text is drawn with the game's shadow under it
 */
public record StyleFile(Optional<String> name, Optional<ResourceLocation> parent, Optional<String> palette,
                        Map<String, String> colours, Map<String, StyleSprite> pieces, StyleMetrics.Partial metrics,
                        StyleFinish.Partial finish, Optional<ResourceLocation> font,
                        Optional<ResourceLocation> clickSound, Optional<Boolean> textShadow) {

    /** The folder of a resource pack's assets the styles are read from. */
    public static final String FOLDER = "jstech/styles";

    public static final Codec<StyleFile> CODEC = RecordCodecBuilder.<StyleFile>create(i -> i.group(
            Codec.STRING.optionalFieldOf("name").forGetter(StyleFile::name),
            ResourceLocation.CODEC.optionalFieldOf("parent").forGetter(StyleFile::parent),
            Codec.STRING.optionalFieldOf("palette").forGetter(StyleFile::palette),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("colours", Map.of())
                    .forGetter(StyleFile::colours),
            Codec.unboundedMap(Codec.STRING, StyleSprite.CODEC).optionalFieldOf("pieces", Map.of())
                    .forGetter(StyleFile::pieces),
            StyleMetrics.Partial.CODEC.optionalFieldOf("metrics", StyleMetrics.Partial.NONE)
                    .forGetter(StyleFile::metrics),
            StyleFinish.Partial.CODEC.optionalFieldOf("finish", StyleFinish.Partial.NONE).forGetter(StyleFile::finish),
            ResourceLocation.CODEC.optionalFieldOf("font").forGetter(StyleFile::font),
            ResourceLocation.CODEC.optionalFieldOf("click_sound").forGetter(StyleFile::clickSound),
            Codec.BOOL.optionalFieldOf("text_shadow").forGetter(StyleFile::textShadow)
    ).apply(i, StyleFile::new)).validate(StyleFile::check);

    public StyleFile {
        colours = Map.copyOf(colours);
        pieces = Map.copyOf(pieces);
    }

    /* A file naming a role or a piece no style has, or writing a colour that is not one, says which and how to fix. */
    private static DataResult<StyleFile> check(final StyleFile file) {
        final List<String> wrong = new ArrayList<>();
        file.colours().forEach((key, colour) -> {
            if (StyleRole.byKey(key).isEmpty()) {
                wrong.add("\"" + key + "\" is no colour role");
            } else if (PaletteRoles.parse(colour) == null) {
                wrong.add("\"" + colour + "\" for " + key + " is no colour; write #AARRGGBB or #RRGGBB");
            }
        });
        file.pieces().keySet().forEach(key -> {
            if (StylePiece.byKey(key).isEmpty()) {
                wrong.add("\"" + key + "\" is no piece of a screen");
            }
        });
        return wrong.isEmpty() ? DataResult.success(file)
                : DataResult.error(() -> String.join("; ", wrong), file);
    }
}
