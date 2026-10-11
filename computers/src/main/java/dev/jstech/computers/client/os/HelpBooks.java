/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.help.HelpShelf;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.guide.ClientGuideText;
import dev.jstech.core.client.guide.GuideLibrary;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.text.TextFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import org.jetbrains.annotations.Nullable;

/**
 * The manuals the player's game holds, read as text for the computers' help programs: the series' Technical Reference
 * first, then each mod's own, the same entries in each.
 *
 * <p>Read again when the packs are, or when the player changes language, so a help window always says what the
 * binder in the player's hands says, in the same words.
 */
public final class HelpBooks {

    /** The library and language the readers were made from, so a change to either has them read again. */
    private static GuideLibrary readFrom;
    private static String readIn = "";
    private static List<ManualReader> readers = List.of();

    private HelpBooks() {
    }

    /** Every manual, read as text, the one of the highest priority first. */
    public static synchronized List<ManualReader> all() {
        final GuideLibrary library = GuideLibrary.loaded();
        final String language = Minecraft.getInstance().getLanguageManager().getSelected();
        if (library != readFrom || !language.equals(readIn)) {
            final List<ManualReader> read = new ArrayList<>();
            for (final GuideManual manual : library.manuals()) {
                read.add(new ManualReader(manual, library.contentsOf(manual), new Words()));
            }
            readers = List.copyOf(read);
            readFrom = library;
            readIn = language;
        }
        return readers;
    }

    /**
     * Every manual, read as text, in the order a machine's help shows them: the book written for the system it runs
     * first ({@link HelpShelf}), then the others; every manual in its own order for a machine with no system.
     */
    public static List<ManualReader> forMachine(@Nullable final BlockPos machine) {
        final Minecraft game = Minecraft.getInstance();
        if (machine == null || game.level == null || !(game.level.getBlockEntity(machine) instanceof IOsHost host)
                || host.installedOs() == null) {
            return all();
        }
        final OsDef system = host.installedOs();
        return HelpShelf.first(all(), HelpShelf.bookFor(system.platform(), system.installMode()));
    }

    /** The manual of that id, read as text. */
    public static Optional<ManualReader> manual(final String id) {
        return all().stream().filter(reader -> reader.manualId().equals(id)).findFirst();
    }

    /** The first manual, the series' own when it is there: where a help program opens. */
    public static Optional<ManualReader> first() {
        final List<ManualReader> all = all();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.getFirst());
    }

    /**
     * The entry, section or chapter a reader named, in the first manual that holds it: what is typed after a help
     * command at a terminal.
     */
    public static Optional<Found> find(final String asked) {
        for (final ManualReader reader : all()) {
            final Optional<String> found = reader.find(asked);
            if (found.isPresent()) {
                return Optional.of(new Found(reader, found.get()));
            }
        }
        return Optional.empty();
    }

    /** A chapter, section or entry found, and the manual it was found in. */
    public record Found(ManualReader reader, String id) {
    }

    /** The manuals' words in the player's language with the values they name, and numbers written the player's way. */
    private static final class Words implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            final String pattern = Language.getInstance().getOrDefault(key, key);
            if (args.length == 0) {
                return GuideFacts.fill(pattern, this);
            }
            final List<String> written = new ArrayList<>();
            for (final Object arg : args) {
                written.add(String.valueOf(arg));
            }
            return GuideFacts.fill(TextFormat.apply(pattern, written), this);
        }

        @Override
        public int width(final String text, final TextSize size) {
            return Minecraft.getInstance().font.width(text);
        }

        @Override
        public int recipeCount(final String type, final String output) {
            return 0;
        }

        @Override
        public String amount(final long value, final String unit) {
            final String number = GameLocale.count(value);
            return unit.isEmpty() ? number : number + " " + unit;
        }

        @Override
        public String itemName(final String item) {
            return ClientGuideText.itemNameOf(item);
        }
    }
}
