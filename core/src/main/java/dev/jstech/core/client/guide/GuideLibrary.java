/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideContents;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import dev.jstech.core.guide.GuideStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every manual the player's game has, read from the loaded resource packs: the chapters, sections and entries of every
 * mod, the manuals and the styles. Read again whenever the packs are, so a pack's change shows the next time a manual
 * is opened.
 */
public final class GuideLibrary {

    private final Map<String, GuideChapter> chapters;
    private final Map<String, GuideSection> sections;
    private final Map<String, GuideEntry> entries;
    private final Map<String, GuideManual> manuals;
    private final Map<String, GuideStyle> styles;
    private final Map<String, String> pagesOfItems = new HashMap<>();

    private static volatile GuideLibrary loaded = new GuideLibrary(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

    public GuideLibrary(final Map<String, GuideChapter> chapters, final Map<String, GuideSection> sections,
                        final Map<String, GuideEntry> entries, final Map<String, GuideManual> manuals,
                        final Map<String, GuideStyle> styles) {
        this.chapters = Map.copyOf(chapters);
        this.sections = Map.copyOf(sections);
        this.entries = Map.copyOf(entries);
        this.manuals = Map.copyOf(manuals);
        this.styles = Map.copyOf(styles);
        final List<GuideEntry> sorted = new ArrayList<>(this.entries.values());
        sorted.sort(Comparator.comparing(GuideEntry::id));
        for (final GuideEntry entry : sorted) {
            for (final String item : entry.items()) {
                this.pagesOfItems.putIfAbsent(item, entry.id());
            }
        }
    }

    /** The manuals as the packs last gave them. */
    public static GuideLibrary loaded() {
        return loaded;
    }

    /** Takes a newly read library as the one in use. */
    public static void use(final GuideLibrary library) {
        loaded = library;
    }

    /** The manual of that id. */
    public Optional<GuideManual> manual(final String id) {
        return Optional.ofNullable(this.manuals.get(id));
    }

    /** The style of that id. */
    public Optional<GuideStyle> style(final String id) {
        return Optional.ofNullable(this.styles.get(id));
    }

    /** The chapter of that namespace. */
    public Optional<GuideChapter> chapter(final String namespace) {
        return Optional.ofNullable(this.chapters.get(namespace));
    }

    /** The entry of that id. */
    public Optional<GuideEntry> entry(final String id) {
        return Optional.ofNullable(this.entries.get(id));
    }

    /** The entry an item's page is, when one covers it. */
    public Optional<String> pageOf(final String item) {
        return Optional.ofNullable(this.pagesOfItems.get(item));
    }

    /**
     * The manual the manual key opens an entry in: of those holding the entry's chapter, one written for that chapter
     * before one that holds every chapter, so a mod's item opens in the mod's own manual when it has one; then the one
     * of the highest priority, and of two alike the one whose id sorts first.
     */
    public Optional<GuideManual> manualFor(final String entry) {
        final GuideEntry found = this.entries.get(entry);
        if (found == null) {
            return Optional.empty();
        }
        final String chapter = found.namespace();
        return this.manuals.values().stream().filter(manual -> manual.holds(chapter))
                .filter(manual -> this.chapters.containsKey(chapter))
                .min(Comparator.comparing((GuideManual manual) -> !manual.names(chapter))
                        .thenComparingInt(manual -> -manual.priority())
                        .thenComparing(GuideManual::id));
    }

    /**
     * What a manual holds, in print order: its chapters by their order, each chapter's sections by theirs, each
     * section's entries by theirs. A chapter with no entries is left out, and so is an entry whose section is missing.
     */
    public GuideContents contentsOf(final GuideManual manual) {
        final List<GuideChapter> held = new ArrayList<>(this.chapters.values().stream()
                .filter(chapter -> manual.holds(chapter.namespace())).toList());
        held.sort(Comparator.comparingInt(GuideChapter::order).thenComparing(GuideChapter::namespace));
        final List<GuideContents.Chapter> out = new ArrayList<>();
        for (final GuideChapter chapter : held) {
            final List<GuideSection> chapterSections = new ArrayList<>(this.sections.values().stream()
                    .filter(section -> section.chapter().equals(chapter.namespace())).toList());
            chapterSections.sort(Comparator.comparingInt(GuideSection::order).thenComparing(GuideSection::id));
            final List<GuideContents.Section> parts = new ArrayList<>();
            for (final GuideSection section : chapterSections) {
                final List<GuideEntry> inSection = new ArrayList<>(this.entries.values().stream()
                        .filter(entry -> entry.section().equals(section.id())).toList());
                inSection.sort(Comparator.comparingInt(GuideEntry::order).thenComparing(GuideEntry::id));
                if (!inSection.isEmpty()) {
                    parts.add(new GuideContents.Section(section, inSection));
                }
            }
            if (!parts.isEmpty()) {
                out.add(new GuideContents.Chapter(chapter, parts));
            }
        }
        return new GuideContents(out);
    }

    /** How many manuals the packs hold. */
    public int manualCount() {
        return this.manuals.size();
    }

    /** Every manual the packs hold, the highest priority first, and of two alike the one whose id sorts first. */
    public List<GuideManual> manuals() {
        final List<GuideManual> sorted = new ArrayList<>(this.manuals.values());
        sorted.sort(Comparator.comparingInt((GuideManual manual) -> -manual.priority()).thenComparing(GuideManual::id));
        return sorted;
    }
}
