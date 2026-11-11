/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.man;

import dev.jstech.computers.os.Platform;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideIds;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.guide.markdown.CodecJsonBlocks;
import dev.jstech.core.guide.markdown.ManualFile;
import dev.jstech.core.guide.markdown.MarkdownManuals;
import dev.jstech.core.guide.markdown.ModManualFiles;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The entries of the manuals every mod of the series has, as {@code man} finds them at a prompt: by the last part of
 * the entry's id ("graphics_cards", or "graphics-cards") or by its English title. The manuals written in Markdown are
 * read from the mods' own files, in English, once; a mod that still declares its manual in Java is read as it declares
 * it.
 *
 * <p>The machine only decides that there is such an entry; the page itself is laid out on the player's side, from the
 * manuals in the player's game and in the player's language.
 */
public final class ManualEntries {

    /** The characters a typed name may spell a gap with, all read alike. */
    private static final Pattern GAPS = Pattern.compile("[-_\\s]+");

    private ManualEntries() {
    }

    /** The id of the entry a word names, when one of the manuals has it. */
    public static Optional<String> find(final String asked) {
        final String wanted = loose(asked);
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        for (final Titled entry : Read.ENTRIES) {
            if (loose(GuideIds.path(entry.entry().id())).equals(wanted) || loose(entry.title()).equals(wanted)) {
                return Optional.of(entry.entry().id());
            }
        }
        return Optional.empty();
    }

    /**
     * The id of the entry that prints the page of the command typed by that name on a system of that family, when one
     * of the manuals has it: that entry is the command's page there, written round the command's own words.
     */
    public static Optional<String> commandEntry(final Platform platform, final String name) {
        final List<String> pages = CommandGuidePages.pagesOf(platform, name.strip().toLowerCase(Locale.ROOT));
        for (final Titled entry : Read.ENTRIES) {
            for (final GuideBlock block : entry.entry().blocks()) {
                if (block instanceof GuideBlock.Command command && pages.contains(command.source())) {
                    return Optional.of(entry.entry().id());
                }
            }
        }
        return Optional.empty();
    }

    private static String loose(final String name) {
        return GAPS.matcher(name.strip().toLowerCase(Locale.ROOT)).replaceAll("_");
    }

    /** An entry with its English title. */
    private record Titled(GuideEntry entry, String title) {
    }

    /* The entries, read the first time a prompt asks, in id order for the Markdown and as declared for the rest. */
    private static final class Read {

        private static final List<Titled> ENTRIES = read();

        private Read() {
        }

        private static List<Titled> read() {
            final List<Titled> entries = new ArrayList<>();
            MarkdownManuals.gather(ModManualFiles.all(), ManualFile.ENGLISH, CodecJsonBlocks.INSTANCE).entries()
                    .values().stream().sorted((one, other) -> one.id().compareTo(other.id()))
                    .forEach(entry -> entries.add(new Titled(entry, entry.titleKey())));
            for (final ModContent content : ModContent.all()) {
                for (final ModGuide guide : content.declaredGuides()) {
                    final Map<String, String> english = guide.translations();
                    for (final GuideEntry entry : guide.declaredEntries()) {
                        entries.add(new Titled(entry, english.getOrDefault(entry.titleKey(), "")));
                    }
                }
            }
            return List.copyOf(entries);
        }
    }
}
