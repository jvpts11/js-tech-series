/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideFamilies;
import dev.jstech.core.guide.GuideIds;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Manuals written in Markdown, read from their files in the reader's language: under each namespace's
 * {@code jstech/manuals/<folder>/}, a folder for each language, and in it a file for each entry, section, chapter and
 * manual, each saying in its header what it is ({@code kind}, an entry unless it says) and its title in its first
 * heading. A file the reader's language does not have is read in English. Pure.
 *
 * <pre>{@code
 * ---
 * section: mymod:machines
 * order: 3
 * icon: mymod:kiln
 * covers: [mymod:kiln]
 * ---
 * # Kiln
 *
 * ## What it is
 *
 * A machine that fires clay into bricks with energy.
 * }</pre>
 *
 * <p>An entry's header names its {@code section}, and may give its {@code id} (its namespace and its file's name
 * unless it says), {@code order}, {@code icon}, the items it {@code covers}, a family of them it
 * {@code covers_family} ({@link GuideFamilies}), the items it {@code shows} under its title and the knowledge it
 * {@code needs}. A section's header gives {@code kind: section}, its {@code chapter} (the mod's main chapter unless
 * it says), {@code order} and {@code icon}. A chapter's gives {@code kind: chapter}, {@code order}, {@code tab} and
 * {@code own_books_only}, its paragraphs being what it is about; the main chapter is the file named {@code chapter}.
 * A manual's gives {@code kind: manual}, {@code style}, {@code chapters}, {@code cover}, {@code edition},
 * {@code part_number}, {@code priority} and {@code icon}, its paragraphs being its "About this manual"; its id is its
 * namespace and its folder.
 */
public final class MarkdownManuals {

    private static final String KIND = "kind";
    private static final String MAIN_CHAPTER = "chapter";

    private MarkdownManuals() {
    }

    /**
     * Every manual, chapter, section and entry {@code files} write, each read in {@code language} where it has a file
     * in it and in English where not, with what they do not say right; {@code json} reads the blocks written as JSON.
     */
    public static Gathered gather(final Collection<ManualFile> files, final String language, final IJsonBlocks json) {
        final Map<String, ManualFile> chosen = new TreeMap<>();
        for (final ManualFile file : files) {
            if (file.language().equals(language) || file.language().equals(ManualFile.ENGLISH)) {
                final String key = file.namespace() + "/" + file.folder() + "/" + file.path();
                final ManualFile before = chosen.get(key);
                if (before == null || file.language().equals(language)) {
                    chosen.put(key, file);
                }
            }
        }
        final Gathering gathering = new Gathering();
        for (final ManualFile file : chosen.values()) {
            gathering.read(file, json);
        }
        return new Gathered(Map.copyOf(gathering.chapters), Map.copyOf(gathering.sections),
                Map.copyOf(gathering.entries), Map.copyOf(gathering.manuals), List.copyOf(gathering.problems));
    }

    /* {@code value}, with the namespace {@code namespace} put before it when it names none. */
    private static String qualified(final String namespace, final String value) {
        return value.contains(":") ? value : GuideIds.of(namespace, value);
    }

    /* The words of every paragraph of {@code blocks}, a paragraph each. */
    private static List<String> paragraphs(final List<GuideBlock> blocks) {
        final List<String> said = new ArrayList<>();
        for (final GuideBlock block : blocks) {
            if (block instanceof GuideBlock.Paragraph paragraph) {
                said.add(paragraph.key());
            }
        }
        return said;
    }

    /**
     * What the files say.
     *
     * @param chapters each chapter, by id
     * @param sections each section, by id
     * @param entries  each entry, by id
     * @param manuals  each manual, by id
     * @param problems what they do not say right, each with its file and its line
     */
    public record Gathered(Map<String, GuideChapter> chapters, Map<String, GuideSection> sections,
                           Map<String, GuideEntry> entries, Map<String, GuideManual> manuals, List<String> problems) {
    }

    /* The pieces read so far, each id kept from the first file that names it. */
    private static final class Gathering {

        private final Map<String, GuideChapter> chapters = new HashMap<>();
        private final Map<String, GuideSection> sections = new HashMap<>();
        private final Map<String, GuideEntry> entries = new HashMap<>();
        private final Map<String, GuideManual> manuals = new HashMap<>();
        private final Map<String, String> named = new HashMap<>();
        private final List<String> problems = new ArrayList<>();

        void read(final ManualFile file, final IJsonBlocks json) {
            final FrontMatter head = FrontMatter.of(file.text());
            final MarkdownBlocks.Parsed body = MarkdownBlocks.parse(head.body(), head.bodyLine(), json);
            body.problems().forEach(problem -> this.problems.add(problem.at(file.where())));
            if (body.title().isEmpty()) {
                this.problems.add(new ManualFileException(0, "it has no title, a first heading of one #, and is left "
                        + "out").at(file.where()));
                return;
            }
            final String title = body.title().get();
            final String ns = file.namespace();
            switch (head.get(KIND, "entry")) {
                case "section" -> {
                    final String id = qualified(ns, head.get("id", file.name()));
                    if (this.first(id, file)) {
                        this.sections.put(id, new GuideSection(id, head.number("order", 0), title,
                                head.get("icon", ""), head.get("chapter", ns), head.get("code", "")));
                    }
                }
                case "chapter" -> {
                    final String id = head.get("id", file.name().equals(MAIN_CHAPTER) ? ns
                            : GuideIds.of(ns, file.name()));
                    if (this.first(id, file)) {
                        this.chapters.put(id, new GuideChapter(id, head.number("order", 0), title,
                                head.get("tab", ""), String.join(" ", paragraphs(body.blocks())),
                                !head.yes("own_books_only", false)));
                    }
                }
                case "manual" -> {
                    final String id = qualified(ns, head.get("id", file.folder()));
                    final List<String> chapters = head.list("chapters");
                    if (head.get("style").isEmpty() || chapters.isEmpty()) {
                        this.problems.add(new ManualFileException(0, "a manual names its style and its chapters")
                                .at(file.where()));
                    } else if (this.first(id, file)) {
                        this.manuals.put(id, new GuideManual(id, title, head.list("cover"), head.get("edition", ""),
                                head.get("part_number", ""), qualified(ns, head.get("style", "")), chapters,
                                paragraphs(body.blocks()), head.number("priority", 0), head.get("icon", "")));
                    }
                }
                case "entry" -> this.entry(file, head, title, body.blocks());
                default -> this.problems.add(new ManualFileException(0, "a file is an entry, a section, a chapter or a "
                        + "manual, not " + head.get(KIND, "")).at(file.where()));
            }
        }

        private void entry(final ManualFile file, final FrontMatter head, final String title,
                           final List<GuideBlock> blocks) {
            final String ns = file.namespace();
            final String id = qualified(ns, head.get("id", file.name()));
            if (head.get("section").isEmpty()) {
                this.problems.add(new ManualFileException(0, "an entry names the section it sits in").at(file.where()));
                return;
            }
            if (!this.first(id, file)) {
                return;
            }
            final Set<String> items = new LinkedHashSet<>(head.list("covers"));
            for (final String family : head.list("covers_family")) {
                items.addAll(GuideFamilies.itemsOf(family));
            }
            this.entries.put(id, new GuideEntry(id, qualified(ns, head.get("section", "")), head.number("order", 0),
                    title, head.get("icon", ""), List.copyOf(items), head.list("shows"), blocks,
                    head.get("needs", "")));
        }

        /* Whether {@code id} is named here first; a second file naming it is left out, and said so. */
        private boolean first(final String id, final ManualFile file) {
            final String before = this.named.putIfAbsent(id, file.where());
            if (before != null) {
                this.problems.add(new ManualFileException(0, "it repeats the id " + id + " of " + before
                        + " and is left out").at(file.where()));
                return false;
            }
            return true;
        }
    }
}
