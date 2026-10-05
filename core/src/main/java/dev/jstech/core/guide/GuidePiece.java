/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

/**
 * One thing drawn on a page of a laid-out manual, at a place in the page's own pixels (its top left corner is 0, 0).
 *
 * <p>Colours are named by their role in the style ({@link GuideStyle#INK}, {@link GuideStyle#LINK}), so the same
 * layout is drawn in any style. A piece that leads somewhere names its target: an entry's, a section's or a
 * chapter's id, or a page.
 */
public sealed interface GuidePiece {

    /** Where the piece starts, across the page. */
    int x();

    /** Where the piece starts, down the page. */
    int y();

    /**
     * A run of text.
     *
     * @param link where clicking it leads, or empty
     */
    record Text(int x, int y, String text, TextSize size, String colour, String link) implements GuidePiece {

        public Text {
            link = link == null ? "" : link;
        }
    }

    /** A line of contents or of the index: words on the left, a page on the right, dots between. */
    record Leader(int x, int y, int width, String left, String right, TextSize size, String colour, String link,
                  String icon) implements GuidePiece {

        public Leader {
            link = link == null ? "" : link;
            icon = icon == null ? "" : icon;
        }
    }

    /** A line across the page. */
    record Rule(int x, int y, int width, String colour) implements GuidePiece {
    }

    /** A filled box with an edge, for a figure's frame, a warning, a table's head. */
    record Box(int x, int y, int width, int height, String fill, String edge) implements GuidePiece {
    }

    /** An item, drawn at a whole-number scale; hovering it shows its name. */
    record Item(int x, int y, String item, int scale) implements GuidePiece {
    }

    /** Recipes of a type, read from the running game: the first one of them and as many after it as fit. */
    record Recipes(int x, int y, int width, String type, String output, int first, int count) implements GuidePiece {
    }

    /** A special block another mod draws in the room given. */
    record Custom(int x, int y, int width, int height, String type, String data) implements GuidePiece {
    }
}
