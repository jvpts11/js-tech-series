/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.style;

import dev.jstech.core.client.gui.theme.EraPalette;
import dev.jstech.core.client.gui.theme.EraStyle;
import dev.jstech.core.client.gui.theme.EraTheme;
import dev.jstech.core.gui.style.DeclaredPalettes;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.gui.style.StyleFile;
import dev.jstech.core.gui.style.StyleFiles;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSettings;
import dev.jstech.core.log.CoreLogs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * The styles this game has, read from the resource packs, and the one each screen wears: the player's choice for its
 * mod, else their choice for everything, else the mod's default, else the game's own. Every screen of every mod built
 * on the Core asks here, so a choice in the player's settings repaints them all at once.
 */
public final class Styles {

    private static final Logger LOGGER = CoreLogs.INTERFACE.logger();
    private static final Map<ResourceLocation, StyleSkin> SKINS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EraTheme> THEMES = new ConcurrentHashMap<>();
    private static final List<String> PROBLEMS = new CopyOnWriteArrayList<>();
    private static final Map<String, ResourceLocation> MOD_DEFAULTS = new ConcurrentHashMap<>();
    private static final List<Runnable> RELOADED = new CopyOnWriteArrayList<>();
    private static volatile Map<ResourceLocation, Style> loaded = Map.of();

    private Styles() {
    }

    /** Every style read, in the order of their ids, the game's first. */
    public static List<Style> all() {
        final List<Style> out = new ArrayList<>(loaded.values());
        out.sort(Comparator.comparing((Style style) -> !style.id().equals(StyleFiles.GAME))
                .thenComparing(style -> style.id().toString()));
        return out;
    }

    /** The style {@code id}, or none when no resource pack has it. */
    public static Optional<Style> get(final ResourceLocation id) {
        return Optional.ofNullable(loaded.get(id));
    }

    /** The game's own style, which every screen wears unless something says otherwise. */
    public static Style game() {
        final Style game = loaded.get(StyleFiles.GAME);
        if (game == null) {
            throw new IllegalStateException("the game's style is not loaded: the Core's resources are missing");
        }
        return game;
    }

    /**
     * Sets the style {@code modid}'s screens wear when the player chose none, from client setup. A mod sets only its
     * own default; the player's choice is always over it.
     */
    public static void setModDefault(final String modid, final ResourceLocation style) {
        MOD_DEFAULTS.put(modid, style);
    }

    /** The style {@code modid}'s screens wear: the player's choice, else the mod's default, else the game's. */
    public static Style forMod(final String modid) {
        return chosenFor(modid).orElseGet(() -> Optional.ofNullable(MOD_DEFAULTS.get(modid)).flatMap(Styles::get)
                .orElseGet(Styles::game));
    }

    /** The style the player chose for {@code modid}'s screens, for that mod or for everything, or none. */
    public static Optional<Style> chosenFor(final String modid) {
        return StyleSettings.chosenFor(modid).map(ResourceLocation::tryParse).flatMap(id -> Optional.ofNullable(id)
                .flatMap(Styles::get));
    }

    /** The skin a component draws through in {@code style}. */
    public static StyleSkin skin(final Style style) {
        return SKINS.computeIfAbsent(style.id(), id -> new StyleSkin(style));
    }

    /**
     * {@code style} as the series' era theme, for the screens that still paint through it: its colours by their roles,
     * its finish by its switches.
     */
    public static EraTheme eraTheme(final Style style) {
        return THEMES.computeIfAbsent(style.id(), id -> new EraTheme(() -> eraPalette(style), new EraStyle(
                style.finish().scanlines(), style.finish().doubleBevel(), style.finish().glowAccent(),
                style.finish().glassBands(), style.finish().accentRule(), style.metrics().smallScale())));
    }

    /** What was wrong in the style files last read, a line each. */
    public static List<String> problems() {
        return List.copyOf(PROBLEMS);
    }

    /** Runs {@code action} each time the styles are read again, for a screen that keeps what it worked out of one. */
    public static void onReload(final Runnable action) {
        RELOADED.add(action);
    }

    /** Takes the style files just read, from the resource reload. */
    static void load(final Map<ResourceLocation, StyleFile> files) {
        final List<String> problems = new ArrayList<>();
        final Map<ResourceLocation, Style> styles = new LinkedHashMap<>(StyleFiles.resolve(files,
                DeclaredPalettes.INSTANCE, problems));
        loaded = Map.copyOf(styles);
        SKINS.clear();
        THEMES.clear();
        PROBLEMS.clear();
        PROBLEMS.addAll(problems);
        for (final String problem : problems) {
            LOGGER.warn("style {}", problem);
        }
        LOGGER.debug("{} styles read", styles.size());
        RELOADED.forEach(Runnable::run);
    }

    private static EraPalette eraPalette(final Style s) {
        return new EraPalette(s.colour(StyleRole.OUTER), s.colour(StyleRole.SCREEN), s.colour(StyleRole.RAIL),
                s.colour(StyleRole.PANEL), s.colour(StyleRole.LINE), s.colour(StyleRole.TRACK),
                s.colour(StyleRole.SLOT_BG), s.colour(StyleRole.SLOT_EDGE), s.colour(StyleRole.ACCENT),
                s.colour(StyleRole.ACCENT2), s.colour(StyleRole.GREEN), s.colour(StyleRole.AMBER),
                s.colour(StyleRole.RED), s.colour(StyleRole.TEXT), s.colour(StyleRole.DIM),
                s.colour(StyleRole.TAB_ON), s.colour(StyleRole.TAB_LABEL_ON), s.colour(StyleRole.HOVER),
                s.colour(StyleRole.BEVEL_LIGHT), s.colour(StyleRole.BEVEL_DARK), s.colour(StyleRole.SCANLINE),
                s.colour(StyleRole.GLOW), s.colour(StyleRole.SHEEN));
    }
}
