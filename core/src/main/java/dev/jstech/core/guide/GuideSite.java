/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A manual as a website of plain files, to put online or open from a disk: a front page with the manual's contents
 * and a search box, a page an entry, a style sheet, and the search's words in one script beside them, so the search
 * runs in the browser with no server. It is built from the manual read as text ({@link ManualReader}), every word
 * already in the reader's language; the recipes its pages show come in words from the game that has them.
 *
 * <p>Pure: a test builds a site with no game at all, and whoever writes it to a disk writes each file as it is.
 */
public final class GuideSite {

    /** The front page, with the contents and the search; every page of the site leads back to it. */
    public static final String FRONT = "index.html";
    /** The style sheet every page reads. */
    public static final String STYLE = "style.css";
    /** The search's script, with the words of every entry in it. */
    public static final String SEARCH = "search.js";
    /** The most a search lists, as the manual's own search does. */
    public static final int MOST = GuideSearch.MOST;

    private static final String ENTRIES = "entries/";
    private static final String HTML = ".html";
    /* The search: every entry holding each word typed, in print order, its count said above the list. */
    private static final String SEARCH_SCRIPT = """
            (function () {
              const box = document.getElementById("words");
              const count = document.getElementById("count");
              const list = document.getElementById("found");
              if (!box || !count || !list) {
                return;
              }
              box.addEventListener("input", function () {
                const words = box.value.toLowerCase().split(/\\s+/).filter(function (word) {
                  return word.length > 0;
                });
                list.textContent = "";
                if (words.length === 0) {
                  count.textContent = "";
                  return;
                }
                const found = ENTRIES.filter(function (entry) {
                  return words.every(function (word) {
                    return entry.w.indexOf(word) >= 0;
                  });
                });
                count.textContent = found.length === 0 ? FOUND_NONE : FOUND.replace("%s", String(found.length));
                found.slice(0, MOST).forEach(function (entry) {
                  const item = document.createElement("li");
                  const link = document.createElement("a");
                  link.href = entry.u;
                  link.textContent = entry.n + " " + entry.t;
                  item.appendChild(link);
                  list.appendChild(item);
                });
              });
            })();
            """;
    /* A plain page, like paper, that follows the reader's light or dark setting. */
    private static final String STYLE_SHEET = """
            :root {
              --paper: #f6f1e4;
              --ink: #2b2a26;
              --faint: #6f6a5c;
              --rule: #d6ccb4;
              --link: #1f4f8a;
              --note: #ece3c8;
              --warning: #8a2f1f;
              color-scheme: light;
            }
            @media (prefers-color-scheme: dark) {
              :root {
                --paper: #1d1c19;
                --ink: #e8e2d2;
                --faint: #a39d8d;
                --rule: #3d3a33;
                --link: #8fb8ea;
                --note: #2a2823;
                --warning: #e99a87;
                color-scheme: dark;
              }
            }
            body {
              margin: 0;
              padding: 0 16px 48px;
              background: var(--paper);
              color: var(--ink);
              font: 16px/1.55 Georgia, "Times New Roman", serif;
            }
            header, main {
              max-width: 44rem;
              margin: 0 auto;
            }
            header {
              padding: 24px 0 8px;
              border-bottom: 1px solid var(--rule);
            }
            h1 {
              font-size: 1.8rem;
              line-height: 1.2;
              margin: 0.4em 0;
            }
            h2 {
              font-size: 1.2rem;
              margin: 1.6em 0 0.4em;
            }
            h2.warning, p.warning {
              color: var(--warning);
            }
            a {
              color: var(--link);
            }
            .home {
              font-weight: bold;
              text-decoration: none;
            }
            .number {
              color: var(--faint);
              font-variant-numeric: tabular-nums;
            }
            .contents ol {
              list-style: none;
              padding-left: 1.2em;
            }
            .contents > ol {
              padding-left: 0;
            }
            .search label {
              display: block;
              font-weight: bold;
              margin: 24px 0 6px;
            }
            .search input {
              width: 100%;
              box-sizing: border-box;
              padding: 8px 10px;
              font: inherit;
              color: inherit;
              background: transparent;
              border: 1px solid var(--rule);
            }
            .count, .caption, .parts {
              color: var(--faint);
            }
            p.note {
              background: var(--note);
              padding: 8px 12px;
            }
            p.item {
              margin: 0.3em 0;
            }
            .marker {
              font-weight: bold;
            }
            dt {
              font-weight: bold;
            }
            dd {
              margin: 0 0 0.6em 1.2em;
            }
            .table {
              overflow-x: auto;
            }
            table {
              border-collapse: collapse;
              margin: 1em 0;
            }
            caption {
              text-align: left;
              color: var(--faint);
              padding-bottom: 4px;
            }
            th, td {
              border: 1px solid var(--rule);
              padding: 4px 8px;
              text-align: left;
              vertical-align: top;
            }
            pre {
              overflow-x: auto;
              padding: 10px 12px;
              background: var(--note);
              font: 14px/1.45 Consolas, Menlo, monospace;
            }
            figure {
              margin: 1em 0;
            }
            figcaption {
              color: var(--faint);
            }
            .turn {
              display: flex;
              flex-wrap: wrap;
              justify-content: space-between;
              gap: 12px;
              margin-top: 32px;
              padding-top: 12px;
              border-top: 1px solid var(--rule);
            }
            """;

    private GuideSite() {
    }

    /**
     * Every file of the site by its path in it, the front page first.
     *
     * @param reader   the manual, read as text
     * @param text     the words the site's own links and search say, in the reader's language
     * @param recipes  the recipes the manual's pages show, in words
     * @param language the reader's language as a web page names it, such as {@code en-us}
     */
    public static Map<String, String> files(final ManualReader reader, final IGuideText text,
                                            final IRecipeWords recipes, final String language) {
        final Map<String, String> files = new LinkedHashMap<>();
        files.put(FRONT, front(reader, text, language));
        files.put(STYLE, STYLE_SHEET);
        files.put(SEARCH, search(reader, text, recipes));
        for (final String entry : reader.entries()) {
            reader.article(entry).ifPresent(article -> files.put(pathOf(entry),
                    entryPage(reader, article, text, recipes, language)));
        }
        return files;
    }

    /** Where an entry's page is in the site: {@code entries/namespace/path.html}. */
    public static String pathOf(final String entry) {
        return ENTRIES + GuideIds.namespace(entry) + "/" + GuideIds.path(entry) + HTML;
    }

    private static String front(final ManualReader reader, final IGuideText text, final String language) {
        final StringBuilder out = head(reader.title(), "", language);
        out.append("<header><h1>").append(escape(reader.title())).append("</h1></header>\n<main>\n");
        out.append("<section class=\"search\">\n<label for=\"words\">")
                .append(escape(text.text(GuideTexts.SEARCH.key()))).append("</label>\n")
                .append("<input id=\"words\" type=\"search\" autocomplete=\"off\" spellcheck=\"false\">\n")
                .append("<p id=\"count\" class=\"count\"></p>\n<ol id=\"found\" class=\"found\"></ol>\n")
                .append("</section>\n<nav class=\"contents\">\n<h2>")
                .append(escape(text.text(GuideTexts.CONTENTS.key()))).append("</h2>\n");
        contents(reader, reader.chapters(), out);
        out.append("</nav>\n</main>\n<script src=\"").append(SEARCH).append("\"></script>\n");
        return foot(out);
    }

    /* The chapters, their sections and their entries, each list inside the one it belongs to. */
    private static void contents(final ManualReader reader, final List<String> ids, final StringBuilder out) {
        out.append("<ol>\n");
        for (final String id : ids) {
            final Optional<ManualReader.Node> node = reader.node(id);
            if (node.isEmpty()) {
                continue;
            }
            final ManualReader.Node held = node.get();
            final String number = "<span class=\"number\">" + escape(held.number()) + "</span> ";
            out.append("<li id=\"").append(anchorOf(id)).append("\">");
            if (held.kind() == ManualReader.Kind.ENTRY) {
                out.append("<a href=\"").append(pathOf(id)).append("\">").append(number)
                        .append(escape(held.title())).append("</a>");
            } else {
                out.append(number).append(escape(held.title())).append('\n');
                contents(reader, reader.children(id).stream().map(ManualReader.Node::id).toList(), out);
            }
            out.append("</li>\n");
        }
        out.append("</ol>\n");
    }

    private static String entryPage(final ManualReader reader, final ManualReader.Article article,
                                    final IGuideText text, final IRecipeWords recipes, final String language) {
        final String home = up(pathOf(article.id()));
        final StringBuilder out = head(article.number() + " " + article.title() + " - " + reader.title(), home,
                language);
        out.append("<header><a class=\"home\" href=\"").append(home).append(FRONT).append("\">")
                .append(escape(reader.title())).append("</a></header>\n<main>\n<article>\n<h1><span class=\"number\">")
                .append(escape(article.number())).append("</span> ").append(escape(article.title())).append("</h1>\n");
        for (final ManualReader.Piece piece : article.pieces()) {
            piece(reader, piece, home, text, recipes, out);
        }
        out.append("</article>\n<nav class=\"turn\">\n");
        reader.previous(article.id()).ifPresent(previous -> out.append("<a rel=\"prev\" href=\"").append(home)
                .append(pathOf(previous)).append("\">").append(escape(text.text(GuideTexts.SITE_PREVIOUS.key(),
                        reader.label(previous)))).append("</a>\n"));
        reader.next(article.id()).ifPresent(next -> out.append("<a rel=\"next\" href=\"").append(home)
                .append(pathOf(next)).append("\">").append(escape(text.text(GuideTexts.SITE_NEXT.key(),
                        reader.label(next)))).append("</a>\n"));
        out.append("</nav>\n</main>\n");
        return foot(out);
    }

    private static void piece(final ManualReader reader, final ManualReader.Piece piece, final String home,
                              final IGuideText text, final IRecipeWords recipes, final StringBuilder out) {
        switch (piece) {
            case ManualReader.Piece.Heading heading -> out.append(heading.warning() ? "<h2 class=\"warning\">"
                    : "<h2>").append(escape(heading.text())).append("</h2>\n");
            case ManualReader.Piece.Paragraph paragraph -> out.append(switch (paragraph.tone()) {
                case PLAIN -> "<p>";
                case NOTE -> "<p class=\"note\">";
                case WARNING -> "<p class=\"warning\">";
            }).append(escape(paragraph.text())).append("</p>\n");
            case ManualReader.Piece.Item item -> out.append("<p class=\"item\"><span class=\"marker\">")
                    .append(escape(item.marker())).append("</span> ").append(escape(item.text())).append("</p>\n");
            case ManualReader.Piece.Term term -> out.append("<dl><dt>").append(escape(term.term()))
                    .append("</dt><dd>").append(escape(term.text())).append("</dd></dl>\n");
            case ManualReader.Piece.Table table -> table(table, out);
            case ManualReader.Piece.Code code -> out.append("<figure class=\"code\"><figcaption>")
                    .append(escape(code.caption())).append("</figcaption><pre><code>")
                    .append(escape(String.join("\n", code.lines()))).append("</code></pre></figure>\n");
            case ManualReader.Piece.Parts parts -> out.append("<p class=\"parts\">").append(escape(parts.line()))
                    .append("</p>\n");
            case ManualReader.Piece.Picture picture -> {
                if (!picture.caption().isEmpty()) {
                    out.append("<p class=\"caption\">").append(escape(picture.caption())).append("</p>\n");
                }
            }
            case ManualReader.Piece.Recipes rows -> recipes(recipes.of(rows.type(), rows.output()), text, out);
            case ManualReader.Piece.Links links -> {
                out.append("<p class=\"links\">").append(escape(links.lead()));
                for (int i = 0; i < links.links().size(); i++) {
                    out.append(i == 0 ? " " : ", ");
                    link(reader, links.links().get(i), home, out);
                }
                out.append("</p>\n");
            }
        }
    }

    /* A link leads to an entry's page, or to a chapter or section on the front page; another book's is only named. */
    private static void link(final ManualReader reader, final ManualReader.Link link, final String home,
                             final StringBuilder out) {
        final Optional<ManualReader.Node> node = reader.node(link.target());
        if (node.isEmpty()) {
            out.append("<span>").append(escape(link.text())).append("</span>");
            return;
        }
        final String href = node.get().kind() == ManualReader.Kind.ENTRY ? home + pathOf(link.target())
                : home + FRONT + "#" + anchorOf(link.target());
        out.append("<a href=\"").append(href).append("\">").append(escape(link.text())).append("</a>");
    }

    private static void table(final ManualReader.Piece.Table table, final StringBuilder out) {
        out.append("<div class=\"table\"><table>\n<caption>").append(escape(table.caption())).append("</caption>\n");
        if (!table.heads().isEmpty()) {
            out.append("<thead><tr>");
            table.heads().forEach(head -> out.append("<th>").append(escape(head)).append("</th>"));
            out.append("</tr></thead>\n");
        }
        out.append("<tbody>\n");
        for (final ManualReader.Row row : table.rows()) {
            out.append("<tr>");
            row.cells().forEach(cell -> out.append("<td>").append(escape(cell)).append("</td>"));
            out.append("</tr>\n");
        }
        out.append("</tbody>\n</table></div>\n");
    }

    /* Each recipe in a line: what it makes, from what, and what works it when that is known. */
    private static void recipes(final List<IRecipeWords.RecipeWords> shown, final IGuideText text,
                                final StringBuilder out) {
        if (shown.isEmpty()) {
            return;
        }
        out.append("<ul class=\"recipes\">\n");
        for (final IRecipeWords.RecipeWords recipe : shown) {
            final String makes = String.join(", ", recipe.makes());
            final String takes = String.join(", ", recipe.takes());
            out.append("<li>").append(escape(recipe.kind().isEmpty()
                    ? text.text(GuideTexts.SITE_RECIPE.key(), makes, takes)
                    : text.text(GuideTexts.SITE_RECIPE_BY.key(), makes, takes, recipe.kind()))).append("</li>\n");
        }
        out.append("</ul>\n");
    }

    /*
     * The search's script: every entry's number, title, page and words, its recipes' among them, and the words the
     * count says; it lists the entries holding every word typed, as the manual's help on a computer does.
     */
    private static String search(final ManualReader reader, final IGuideText text, final IRecipeWords recipes) {
        final StringBuilder out = new StringBuilder("const ENTRIES = [\n");
        for (final String entry : reader.entries()) {
            final Optional<ManualReader.Article> article = reader.article(entry);
            if (article.isEmpty()) {
                continue;
            }
            final StringBuilder words = new StringBuilder(article.get().title());
            for (final ManualReader.Piece piece : article.get().pieces()) {
                words.append(' ').append(piece.words());
                if (piece instanceof ManualReader.Piece.Recipes rows) {
                    for (final IRecipeWords.RecipeWords recipe : recipes.of(rows.type(), rows.output())) {
                        words.append(' ').append(String.join(" ", recipe.makes())).append(' ')
                                .append(String.join(" ", recipe.takes())).append(' ').append(recipe.kind());
                    }
                }
            }
            out.append("  {n: ").append(json(article.get().number())).append(", t: ")
                    .append(json(article.get().title())).append(", u: ").append(json(pathOf(entry))).append(", w: ")
                    .append(json(words.toString().toLowerCase(Locale.ROOT))).append("},\n");
        }
        out.append("];\nconst FOUND = ").append(json(text.text(GuideTexts.FOUND.key()))).append(";\n")
                .append("const FOUND_NONE = ").append(json(text.text(GuideTexts.FOUND_NONE.key()))).append(";\n")
                .append("const MOST = ").append(MOST).append(";\n").append(SEARCH_SCRIPT);
        return out.toString();
    }

    private static StringBuilder head(final String title, final String home, final String language) {
        return new StringBuilder("<!doctype html>\n<html lang=\"").append(escape(language)).append("\">\n<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n<title>")
                .append(escape(title)).append("</title>\n<link rel=\"stylesheet\" href=\"").append(home)
                .append(STYLE).append("\">\n</head>\n<body>\n");
    }

    private static String foot(final StringBuilder out) {
        return out.append("</body>\n</html>\n").toString();
    }

    /* How far a page is from the front of the site, as the start of a link back to it: one "../" a folder. */
    private static String up(final String path) {
        return "../".repeat((int) path.chars().filter(c -> c == '/').count());
    }

    /* A chapter's or section's place on the front page, its id with every character a link would quote made a dash. */
    private static String anchorOf(final String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }

    private static String escape(final String words) {
        final StringBuilder out = new StringBuilder(words.length());
        for (int i = 0; i < words.length(); i++) {
            final char c = words.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /* A string as a script writes it, quoted; a closing tag's slash is escaped so no page could end the script. */
    private static String json(final String words) {
        final StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < words.length(); i++) {
            final char c = words.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '/' -> out.append("\\/");
                default -> out.append(c < 0x20 ? String.format(Locale.ROOT, "\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return out.append('"').toString();
    }
}
