/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * Every multiblock that can be shown before it is built, by id: in the Structure Projector's list, in the recipe
 * viewers, in the manuals. Every pattern declared or in a file is one, unless its mod declared it not projectable, and
 * so is every multiblock machine declared as data whose shape is not a pattern named by id (that pattern is listed on
 * its own). Read on either side: the players' games hold the same files.
 */
public final class Projectables {

    /** How the key of a multiblock's name begins; its mod's language files give each multiblock its own. */
    public static final String KEY_PREFIX = "multiblock.";
    /* Where the shown multiblocks come from: the patterns first, then what other kits add, in the order added. */
    private static final List<Supplier<Map<ResourceLocation, IStructureShape>>> SOURCES =
            new CopyOnWriteArrayList<>(List.of(MultiblockPatterns::projectable));
    /* Who knows a multiblock's terminal block besides its pattern: the kits whose terminals name their shape. */
    private static final List<Function<ResourceLocation, Optional<String>>> TERMINALS =
            new CopyOnWriteArrayList<>();

    private Projectables() {
    }

    /** Adds the multiblocks {@code source} gives, after those known already; an id known already keeps its first. */
    public static void addSource(final Supplier<Map<ResourceLocation, IStructureShape>> source) {
        SOURCES.add(source);
    }

    /**
     * Adds a way to find the block of a multiblock's terminal, by the multiblock's id, for the multiblocks whose
     * shape does not name it: asked in the order added, after the pattern.
     */
    public static void addTerminals(final Function<ResourceLocation, Optional<String>> source) {
        TERMINALS.add(source);
    }

    /**
     * The id of the block of the terminal of the multiblock of {@code id}: the one its pattern names, else the
     * first a kit knows, else none.
     */
    public static Optional<String> terminalOf(final ResourceLocation id) {
        final Optional<String> named = MultiblockPatterns.get(id).flatMap(MultiblockPattern::terminalBlock);
        if (named.isPresent()) {
            return named;
        }
        for (final Function<ResourceLocation, Optional<String>> source : TERMINALS) {
            final Optional<String> known = source.apply(id);
            if (known.isPresent()) {
                return known;
            }
        }
        return Optional.empty();
    }

    /**
     * The multiblock of {@code id} laid out to be shown, its repeated part or its sides {@code grow} past their
     * least, with its terminal's block at its place when it is known; none for an id that cannot be shown.
     */
    public static Optional<ShapePreview> preview(final ResourceLocation id, final int grow) {
        return get(id).flatMap(shape -> shape.preview(grow)).map(preview -> preview.terminalBlock().isPresent()
                ? preview : terminalOf(id).map(preview::withTerminal).orElse(preview));
    }

    /** Every multiblock that can be shown, by id, in the order of their sources and then of their ids. */
    public static Map<ResourceLocation, IStructureShape> all() {
        final Map<ResourceLocation, IStructureShape> out = new LinkedHashMap<>();
        for (final Supplier<Map<ResourceLocation, IStructureShape>> source : SOURCES) {
            source.get().forEach(out::putIfAbsent);
        }
        return out;
    }

    /** The multiblock of {@code id} that can be shown, or none. */
    public static Optional<IStructureShape> get(final ResourceLocation id) {
        return Optional.ofNullable(all().get(id));
    }

    /**
     * What a multiblock is called: the sentence of the key {@code multiblock.<namespace>.<path>}, which its mod's
     * language files give it, or its id's path with its words capitalised when none does.
     */
    public static Text name(final ResourceLocation id) {
        final String english = words(id.getPath());
        return TextKey.of(KEY_PREFIX + id.getNamespace() + "." + id.getPath().replace('/', '.'),
                english.isBlank() ? id.getPath() : english).text();
    }

    /* "coke_oven" as "Coke Oven", the last part of a path that has folders. */
    private static String words(final String path) {
        final String last = path.substring(path.lastIndexOf('/') + 1);
        final StringBuilder out = new StringBuilder();
        for (final String word : last.split("_")) {
            if (!word.isEmpty()) {
                if (!out.isEmpty()) {
                    out.append(' ');
                }
                out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
            }
        }
        return out.toString();
    }
}
