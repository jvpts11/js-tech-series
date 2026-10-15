/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * Turns the style files a game read into styles: each one's chain of parents followed to its root, what a style does
 * not set taken from the style it builds on, and what no style sets taken from the defaults, every piece drawn flat in
 * the style's colours. A file whose parent is missing, a chain that runs in a circle, a palette that is not there and a
 * colour no style in the chain sets are said, with the style they were found in; the style is made all the same, so
 * one slip in a resource pack costs a look, not a screen.
 */
public final class StyleFiles {

    /** The style every screen wears unless the player or its mod says otherwise: the game's own. */
    public static final ResourceLocation GAME = ResourceLocation.fromNamespaceAndPath("jscore", "game");

    private static final ResourceLocation DEFAULT_FONT = ResourceLocation.withDefaultNamespace("default");
    private static final ResourceLocation DEFAULT_CLICK = ResourceLocation.withDefaultNamespace("ui.button.click");

    private StyleFiles() {
    }

    /**
     * The styles of {@code files}, by id, their palettes found through {@code palettes}.
     *
     * @param problems where what is wrong in the files is said, a line each
     */
    public static Map<ResourceLocation, Style> resolve(final Map<ResourceLocation, StyleFile> files,
                                                       final IPaletteSource palettes, final List<String> problems) {
        final Map<ResourceLocation, Style> out = new LinkedHashMap<>();
        for (final Map.Entry<ResourceLocation, StyleFile> entry : files.entrySet()) {
            out.put(entry.getKey(), resolve(entry.getKey(), files, palettes, problems));
        }
        return out;
    }

    /** {@code snake_case} written as {@code camelCase}: a role's key as a palette record names its colour. */
    public static String camel(final String key) {
        final StringBuilder out = new StringBuilder(key.length());
        boolean upper = false;
        for (int i = 0; i < key.length(); i++) {
            final char c = key.charAt(i);
            if (c == '_') {
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }

    /** How a piece is drawn when no style in a chain says: flat, in the style's own colours. */
    public static StyleSprite defaultPiece(final StylePiece piece) {
        return switch (piece) {
            case WINDOW -> StyleSprite.Flat.of(StyleRole.SCREEN, StyleRole.OUTER);
            case PANEL, BUTTON, TAB -> StyleSprite.Flat.of(StyleRole.PANEL, StyleRole.LINE);
            case WELL -> new StyleSprite.Flat(Optional.of(StyleRole.SLOT_BG.key()), Optional.of(
                    StyleRole.SLOT_EDGE.key()), Optional.empty(), Optional.empty(), true);
            case SLOT, FIELD, CHECKBOX -> StyleSprite.Flat.of(StyleRole.SLOT_BG, StyleRole.SLOT_EDGE);
            case BUTTON_HOVER, TAB_HOVER -> StyleSprite.Flat.of(StyleRole.HOVER, StyleRole.LINE);
            case BUTTON_DISABLED -> StyleSprite.Flat.of(StyleRole.TRACK, StyleRole.LINE);
            case BUTTON_PRIMARY, SWITCH_ON -> StyleSprite.Flat.of(StyleRole.ACCENT, StyleRole.ACCENT);
            case FIELD_FOCUSED -> StyleSprite.Flat.of(StyleRole.SLOT_BG, StyleRole.ACCENT);
            case TAB_ACTIVE -> StyleSprite.Flat.of(StyleRole.TAB_ON, StyleRole.ACCENT);
            case CHECKBOX_HOVER -> StyleSprite.Flat.of(StyleRole.HOVER, StyleRole.SLOT_EDGE);
            case CHECKBOX_ON -> StyleSprite.Flat.of(StyleRole.ACCENT, StyleRole.SLOT_EDGE);
            case CHECKBOX_ON_HOVER -> StyleSprite.Flat.of(StyleRole.ACCENT, StyleRole.TEXT);
            case SLIDER, PROGRESS_TRACK -> StyleSprite.Flat.of(StyleRole.TRACK, StyleRole.LINE);
            case SLIDER_HOVER -> StyleSprite.Flat.of(StyleRole.TRACK, StyleRole.ACCENT);
            case SLIDER_HANDLE -> StyleSprite.Flat.of(StyleRole.PANEL, StyleRole.TEXT);
            case SLIDER_HANDLE_HOVER -> StyleSprite.Flat.of(StyleRole.HOVER, StyleRole.ACCENT);
            case SWITCH_OFF -> StyleSprite.Flat.of(StyleRole.TRACK, StyleRole.SLOT_EDGE);
            case SCROLL_TRACK -> StyleSprite.Flat.of(StyleRole.TRACK);
            case SCROLL_THUMB -> StyleSprite.Flat.of(StyleRole.SLOT_EDGE);
            case SEPARATOR -> StyleSprite.Flat.of(StyleRole.LINE);
            case POPUP -> StyleSprite.Flat.of(StyleRole.PANEL, StyleRole.ACCENT);
            case TOOLTIP -> StyleSprite.Flat.of(StyleRole.TOOLTIP_GROUND, StyleRole.TOOLTIP_EDGE);
            case HEADER, STATUS_BAR -> StyleSprite.Flat.of(StyleRole.RAIL, StyleRole.LINE);
            case LIST_HOVER -> StyleSprite.Flat.of(StyleRole.HOVER);
            case LIST_SELECTED -> StyleSprite.Flat.of(StyleRole.TAB_ON);
            case PROGRESS_FILL -> StyleSprite.Flat.of(StyleRole.ACCENT);
            case RADIO -> StyleSprite.Flat.of(StyleRole.SLOT_BG, StyleRole.SLOT_EDGE);
            case RADIO_HOVER -> StyleSprite.Flat.of(StyleRole.SLOT_BG, StyleRole.ACCENT);
            case POPUP_LIST -> StyleSprite.Flat.of(StyleRole.PANEL, StyleRole.ACCENT);
            case CHART -> StyleSprite.Flat.of(StyleRole.CHART_GROUND, StyleRole.CHART_LINE);
            case MENU_GROUND -> StyleSprite.Flat.of(StyleRole.SCREEN);
            case LIST_GROUND -> StyleSprite.Flat.of(StyleRole.PANEL, StyleRole.LINE);
            case SEPARATOR_FOOTER -> StyleSprite.Flat.of(StyleRole.LINE);
        };
    }

    private static Style resolve(final ResourceLocation id, final Map<ResourceLocation, StyleFile> files,
                                 final IPaletteSource palettes, final List<String> problems) {
        final List<StyleFile> chain = chain(id, files, problems);
        final List<Style.Layer> layers = new ArrayList<>();
        for (final StyleFile file : chain) {
            layers.add(new Style.Layer(file.colours(), file.palette()));
            file.palette().ifPresent(palette -> {
                if (palettes.palette(palette).isEmpty()) {
                    problems.add(id + ": the palette " + palette + " is not declared by any mod installed");
                }
            });
        }
        final Map<StylePiece, StyleSprite> pieces = new EnumMap<>(StylePiece.class);
        for (final StylePiece piece : StylePiece.values()) {
            pieces.put(piece, pieceOf(piece, chain));
        }
        StyleMetrics metrics = StyleMetrics.GAME;
        StyleFinish finish = StyleFinish.FLAT;
        for (int i = chain.size() - 1; i >= 0; i--) {
            metrics = metrics.with(chain.get(i).metrics());
            finish = finish.with(chain.get(i).finish());
        }
        checkRoles(id, chain, palettes, problems);
        return new Style(id, first(chain, StyleFile::name).orElse(id.toLanguageKey("style")), layers, pieces, metrics,
                finish, first(chain, StyleFile::font).orElse(DEFAULT_FONT),
                first(chain, StyleFile::clickSound).orElse(DEFAULT_CLICK),
                first(chain, StyleFile::textShadow).orElse(false), palettes);
    }

    /* The style's file, then its parent's, and so on to the root; a missing parent or a circle ends the chain there. */
    private static List<StyleFile> chain(final ResourceLocation id, final Map<ResourceLocation, StyleFile> files,
                                         final List<String> problems) {
        final List<StyleFile> chain = new ArrayList<>();
        final Set<ResourceLocation> seen = new LinkedHashSet<>();
        ResourceLocation at = id;
        while (at != null) {
            if (!seen.add(at)) {
                problems.add(id + ": its parents run in a circle through " + seen);
                break;
            }
            final StyleFile file = files.get(at);
            if (file == null) {
                problems.add(id + ": it builds on " + at + ", which no resource pack has");
                break;
            }
            chain.add(file);
            at = file.parent().orElse(null);
        }
        return chain;
    }

    private static StyleSprite pieceOf(final StylePiece piece, final List<StyleFile> chain) {
        for (final StyleFile file : chain) {
            final StyleSprite own = file.pieces().get(piece.key());
            if (own != null) {
                return own;
            }
        }
        return defaultPiece(piece);
    }

    /* A role that has no fallback must be set somewhere in the chain, by a colour or by a palette that has it. */
    private static void checkRoles(final ResourceLocation id, final List<StyleFile> chain,
                                   final IPaletteSource palettes, final List<String> problems) {
        final List<String> missing = new ArrayList<>();
        for (final StyleRole role : StyleRole.values()) {
            if (role.fallback().isPresent()) {
                continue;
            }
            boolean set = false;
            for (final StyleFile file : chain) {
                if (file.colours().containsKey(role.key()) || file.palette().flatMap(palettes::palette)
                        .map(record -> hasComponent(record, camel(role.key()))).orElse(false)) {
                    set = true;
                    break;
                }
            }
            if (!set) {
                missing.add(role.key());
            }
        }
        if (!missing.isEmpty()) {
            problems.add(id + ": no style in its chain sets the colours " + missing
                    + "; set them under \"colours\" or name a \"palette\"");
        }
    }

    private static boolean hasComponent(final Record record, final String name) {
        for (final RecordComponent component : record.getClass().getRecordComponents()) {
            if (component.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static <T> Optional<T> first(final List<StyleFile> chain, final Function<StyleFile, Optional<T>> part) {
        for (final StyleFile file : chain) {
            final Optional<T> own = part.apply(file);
            if (own.isPresent()) {
                return own;
            }
        }
        return Optional.empty();
    }
}
