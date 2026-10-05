/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.List;
import java.util.Objects;

/**
 * One piece of a manual's entry: a paragraph, a heading, a figure, a table, the recipes of a kind, numbered steps, a
 * warning, the problems and their fixes, a word explained, links to other entries, or a special block another mod
 * draws.
 *
 * <p>An entry is a list of these, laid out on the pages in the order given. Words are never held here, only the keys
 * of the sentences, so a manual reads in each player's language and a resource pack can reword it. Ids are text in
 * the {@code namespace:path} shape, so the layout of a manual can be worked out and tested with no game around it.
 */
public sealed interface GuideBlock {

    /** A paragraph of running text. */
    record Paragraph(String key) implements GuideBlock {

        public Paragraph {
            Objects.requireNonNull(key, "key");
        }
    }

    /** A heading inside an entry, such as "What it is". */
    record Heading(String key) implements GuideBlock {

        public Heading {
            Objects.requireNonNull(key, "key");
        }
    }

    /**
     * A figure: an item drawn large in a frame, with its numbered caption under it ("Figure 3-9. ...").
     *
     * @param item       the item drawn, {@code namespace:path}
     * @param captionKey the caption's sentence
     */
    record Figure(String item, String captionKey) implements GuideBlock {

        public Figure {
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(captionKey, "captionKey");
        }
    }

    /**
     * A numbered table of properties and their values ("Table 3-7. ..."), drawn in the style's table font so the
     * figures line up.
     */
    record Table(String captionKey, List<TableRow> rows) implements GuideBlock {

        public Table {
            Objects.requireNonNull(captionKey, "captionKey");
            rows = List.copyOf(rows);
        }
    }

    /** One row of a table: what it is, and its value. */
    record TableRow(String labelKey, GuideValue value) {

        public TableRow {
            Objects.requireNonNull(labelKey, "labelKey");
            Objects.requireNonNull(value, "value");
        }
    }

    /**
     * Every recipe of a recipe type, read from the game as it runs: the inputs, an arrow with the time and the energy,
     * and the outputs.
     *
     * @param type   the recipe type, such as {@code minecraft:smelting} or a machine's kind
     * @param output only the recipes that make this item, or empty for every recipe of the type
     */
    record Recipes(String type, String output) implements GuideBlock {

        public Recipes {
            Objects.requireNonNull(type, "type");
            output = output == null ? "" : output;
        }
    }

    /** Numbered steps, one sentence each, in the order they are done. */
    record Steps(List<String> keys) implements GuideBlock {

        public Steps {
            keys = List.copyOf(keys);
        }
    }

    /** A warning drawn in a box of its own, for what costs a player something if missed. */
    record Warning(String key) implements GuideBlock {

        public Warning {
            Objects.requireNonNull(key, "key");
        }
    }

    /** What can go wrong: each problem as a player sees it, and what to do about it. */
    record Problems(List<Problem> problems) implements GuideBlock {

        public Problems {
            problems = List.copyOf(problems);
        }
    }

    /** One problem and its fix. */
    record Problem(String problemKey, String fixKey) {

        public Problem {
            Objects.requireNonNull(problemKey, "problemKey");
            Objects.requireNonNull(fixKey, "fixKey");
        }
    }

    /**
     * A word explained where the entry first uses it: the word, then what it means. The index lists every such word
     * with the page that explains it, which makes the index the manual's glossary.
     */
    record Define(String termKey, String definitionKey) implements GuideBlock {

        public Define {
            Objects.requireNonNull(termKey, "termKey");
            Objects.requireNonNull(definitionKey, "definitionKey");
        }
    }

    /** Links to other entries ("See 3.2.2 and 3.4.3"), each shown by the number it has in the manual. */
    record SeeAlso(List<String> entries) implements GuideBlock {

        public SeeAlso {
            entries = List.copyOf(entries);
        }
    }

    /**
     * A block of a kind another mod draws, such as a multiblock turning in three dimensions.
     *
     * @param type   the kind, as the mod registered its renderer
     * @param height how many pixels tall it is drawn
     * @param data   what the renderer is handed, as JSON text
     */
    record Custom(String type, int height, String data) implements GuideBlock {

        public Custom {
            Objects.requireNonNull(type, "type");
            data = data == null ? "{}" : data;
            if (height < 1) {
                throw new IllegalArgumentException("a special block is at least one pixel tall; got " + height);
            }
        }
    }

    /** The value in a table row: a sentence, an amount with its unit, or text that is data. */
    sealed interface GuideValue {

        /** A sentence to translate, such as "PC, Server, Mainframe". */
        record Words(String key) implements GuideValue {

            public Words {
                Objects.requireNonNull(key, "key");
            }
        }

        /** A number, written in the reader's way of writing numbers, and the symbol of its unit ("FE/t"). */
        record Amount(long value, String unit) implements GuideValue {

            public Amount {
                unit = unit == null ? "" : unit;
            }
        }

        /** Text that reads the same in every language: a model number, a bus's name. */
        record Literal(String text) implements GuideValue {

            public Literal {
                Objects.requireNonNull(text, "text");
            }
        }
    }
}
