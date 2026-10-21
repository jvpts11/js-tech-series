/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.modpack;

import dev.jstech.core.guide.GuideSite;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The generated reference as a website of plain files, to put online or open from a disk: a front page listing its
 * sections, a page a section, and the manuals' style sheet. Each section is a kind of thing a modpack writes or names,
 * every data format with a real example and every id, taken from the game's own registries ({@link References}).
 *
 * <p>Pure: a test builds a site with no game at all, and whoever writes it to a disk writes each file as it is.
 */
public final class ReferenceSite {

    /** The front page, which leads to every section. */
    public static final String FRONT = "index.html";
    /** The style sheet every page reads, the manuals' own. */
    public static final String STYLE = "style.css";
    private static final String SECTIONS = "sections/";

    private ReferenceSite() {
    }

    /** Every file of the site of {@code sections}, by its path in the site, the front page first. */
    public static Map<String, String> files(final List<Section> sections) {
        final Map<String, String> files = new LinkedHashMap<>();
        files.put(FRONT, front(sections));
        files.put(STYLE, GuideSite.styleSheet());
        for (final Section section : sections) {
            files.put(pathOf(section), page(section));
        }
        return files;
    }

    /** Where a section's page is in the site: {@code sections/<id>.html}. */
    public static String pathOf(final Section section) {
        return SECTIONS + section.id() + ".html";
    }

    private static String front(final List<Section> sections) {
        final StringBuilder out = head("J's Core reference", STYLE)
                .append("<header><h1>J&#39;s Core reference</h1>")
                .append("<p>Every data format a modpack writes for the J&#39;s Tech Series, with a real example, and "
                        + "every id, taken from the game that wrote this.</p></header>\n<main>\n<ul>\n");
        for (final Section section : sections) {
            out.append("<li><a href=\"").append(pathOf(section)).append("\">").append(escape(section.title()))
                    .append("</a> (").append(section.entries().size()).append(")</li>\n");
        }
        return out.append("</ul>\n</main>\n</body>\n</html>\n").toString();
    }

    private static String page(final Section section) {
        final StringBuilder out = head(section.title(), "../" + STYLE)
                .append("<header><p><a href=\"../").append(FRONT).append("\">J&#39;s Core reference</a></p><h1>")
                .append(escape(section.title())).append("</h1><p>").append(escape(section.about()))
                .append("</p></header>\n<main>\n");
        for (final Entry entry : section.entries()) {
            out.append("<h2 id=\"").append(escape(entry.id())).append("\">").append(escape(entry.id()))
                    .append("</h2>\n");
            if (!entry.where().isEmpty()) {
                out.append("<p><code>").append(escape(entry.where())).append("</code></p>\n");
            }
            if (!entry.example().isEmpty()) {
                out.append("<pre><code>").append(escape(entry.example())).append("</code></pre>\n");
            }
        }
        return out.append("</main>\n</body>\n</html>\n").toString();
    }

    private static StringBuilder head(final String title, final String style) {
        return new StringBuilder("<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n<title>")
                .append(escape(title)).append("</title>\n<link rel=\"stylesheet\" href=\"").append(style)
                .append("\">\n</head>\n<body>\n");
    }

    private static String escape(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * One section of the reference.
     *
     * @param id      its page's name in the site
     * @param title   what it is called
     * @param about   what it holds, in a sentence
     * @param entries what it lists
     */
    public record Section(String id, String title, String about, List<Entry> entries) {

        public Section {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(about, "about");
            entries = List.copyOf(entries);
        }
    }

    /**
     * One thing a section lists.
     *
     * @param id      its id
     * @param where   where a modpack writes or finds it, or nothing
     * @param example an example as it is written, or nothing
     */
    public record Entry(String id, String where, String example) {

        public Entry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(where, "where");
            Objects.requireNonNull(example, "example");
        }
    }
}
