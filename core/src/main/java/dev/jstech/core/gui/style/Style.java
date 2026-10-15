/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import dev.jstech.core.palette.PaletteRoles;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A style with every part decided: its own and those it took from the styles it builds on. What a component draws comes
 * from here, its colours by role, its pieces, its spacing, its font and the sound of a press; nothing a component draws
 * is its own.
 *
 * <p>The colours are read through the style's layers each time they change: a palette a resource pack recolours while a
 * screen is open is seen on the next frame.
 */
public final class Style {

    private final ResourceLocation id;
    private final String nameKey;
    private final List<Layer> layers;
    private final Map<StylePiece, StyleSprite> pieces;
    private final StyleMetrics metrics;
    private final StyleFinish finish;
    private final ResourceLocation font;
    private final ResourceLocation clickSound;
    private final boolean textShadow;
    private final IPaletteSource palettes;
    /* The colours worked out last, and the palette records they were worked out from. */
    private final Map<StyleRole, Integer> colours = new EnumMap<>(StyleRole.class);
    private final Record[] readFrom;
    private boolean worked;

    Style(final ResourceLocation id, final String nameKey, final List<Layer> layers,
          final Map<StylePiece, StyleSprite> pieces, final StyleMetrics metrics, final StyleFinish finish,
          final ResourceLocation font, final ResourceLocation clickSound, final boolean textShadow,
          final IPaletteSource palettes) {
        this.id = Objects.requireNonNull(id, "id");
        this.nameKey = Objects.requireNonNull(nameKey, "nameKey");
        this.layers = List.copyOf(layers);
        this.pieces = new EnumMap<>(pieces);
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.finish = Objects.requireNonNull(finish, "finish");
        this.font = Objects.requireNonNull(font, "font");
        this.clickSound = Objects.requireNonNull(clickSound, "clickSound");
        this.textShadow = textShadow;
        this.palettes = Objects.requireNonNull(palettes, "palettes");
        this.readFrom = new Record[this.layers.size()];
    }

    public ResourceLocation id() {
        return id;
    }

    /** The translation key of the style's name. */
    public String nameKey() {
        return nameKey;
    }

    /** The colour of {@code role}, as the style's layers set it now. */
    public int colour(final StyleRole role) {
        if (!worked || palettesMoved()) {
            work();
        }
        return colours.get(role);
    }

    /** A colour a flat piece names: a role's key, or a colour of its own written {@code #AARRGGBB}. */
    public int colour(final String roleOrColour) {
        final Integer own = PaletteRoles.parse(roleOrColour);
        if (own != null) {
            return own;
        }
        return StyleRole.byKey(roleOrColour).map(this::colour).orElseGet(() -> StylePalette.COLOURS.get().unset());
    }

    /** How the style draws {@code piece}. */
    public StyleSprite piece(final StylePiece piece) {
        return pieces.getOrDefault(piece, StyleSprite.NONE);
    }

    public StyleMetrics metrics() {
        return metrics;
    }

    public StyleFinish finish() {
        return finish;
    }

    /** The font the style's text is drawn in. */
    public ResourceLocation font() {
        return font;
    }

    /** The sound of a press. */
    public ResourceLocation clickSound() {
        return clickSound;
    }

    /** Whether the style draws its text with the game's shadow under it. */
    public boolean textShadow() {
        return textShadow;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    /* Whether a palette the colours were read from has been replaced since, as a resource pack's reload does. */
    private boolean palettesMoved() {
        for (int i = 0; i < layers.size(); i++) {
            final Optional<String> palette = layers.get(i).palette();
            if (palette.isPresent() && palettes.palette(palette.get()).orElse(null) != readFrom[i]) {
                return true;
            }
        }
        return false;
    }

    private void work() {
        final Map<String, Integer>[] fromPalettes = readPalettes();
        for (final StyleRole role : StyleRole.values()) {
            colours.put(role, find(role, fromPalettes));
        }
        worked = true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer>[] readPalettes() {
        final Map<String, Integer>[] out = new Map[layers.size()];
        for (int i = 0; i < layers.size(); i++) {
            final Optional<String> palette = layers.get(i).palette();
            final Record record = palette.flatMap(palettes::palette).orElse(null);
            readFrom[i] = record;
            out[i] = record == null ? Map.of() : PaletteRoles.read(record);
        }
        return out;
    }

    /* The first layer, from this style down to the one it builds on last, that sets the role; then its fallback. */
    private int find(final StyleRole role, final Map<String, Integer>[] fromPalettes) {
        StyleRole asked = role;
        while (asked != null) {
            final Integer found = inLayers(asked, fromPalettes);
            if (found != null) {
                return found;
            }
            asked = asked.fallback().orElse(null);
        }
        return StylePalette.COLOURS.get().unset();
    }

    @Nullable
    private Integer inLayers(final StyleRole role, final Map<String, Integer>[] fromPalettes) {
        for (int i = 0; i < layers.size(); i++) {
            final Integer own = PaletteRoles.parse(layers.get(i).colours().get(role.key()));
            if (own != null) {
                return own;
            }
            final Integer fromPalette = fromPalettes[i].get(StyleFiles.camel(role.key()));
            if (fromPalette != null) {
                return fromPalette;
            }
        }
        return null;
    }

    /**
     * One style's own colours in the chain: those it sets and the palette it takes the rest from.
     *
     * @param colours colours by role key
     * @param palette the palette it takes colours from
     */
    record Layer(Map<String, String> colours, Optional<String> palette) {
    }
}
