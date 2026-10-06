/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.List;

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

    /**
     * A line of contents or of the index: words on the left, a page on the right, dots between. A line whose page is
     * not known yet when it is laid has an empty right side, and is given its page once every page is numbered.
     *
     * @param lead words drawn before the left side, bold and in the heading's colour, such as a section's number; or
     *             empty
     */
    record Leader(int x, int y, int width, String lead, String left, String right, TextSize size, String colour,
                  String link, String icon) implements GuidePiece {

        public Leader {
            lead = lead == null ? "" : lead;
            link = link == null ? "" : link;
            icon = icon == null ? "" : icon;
        }

        /** The same line with its page. */
        public Leader withRight(final String folio) {
            return new Leader(this.x, this.y, this.width, this.lead, this.left, folio, this.size, this.colour,
                    this.link, this.icon);
        }
    }

    /** A line across the page. */
    record Rule(int x, int y, int width, String colour) implements GuidePiece {
    }

    /** A dotted line across the page, as a drawing list rules its rows. */
    record Dots(int x, int y, int width, String colour) implements GuidePiece {
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

    /**
     * The plate under an entry's title holding the items it shows, a row at a time: when they are more than a row
     * holds, the row turns over to the next ones every little while.
     *
     * @param entry the entry it is under, so an item whose page is another leads there
     * @param items the items, in order
     */
    record Plate(int x, int y, int width, String entry, List<String> items) implements GuidePiece {

        public Plate {
            items = List.copyOf(items);
        }
    }

    /**
     * A picture in the room given: an image the mod ships, drawn to that size, or a drawing a mod's renderer makes.
     *
     * @param image   the texture, or empty for a drawing
     * @param drawing the kind of drawing, or empty for an image
     */
    record Picture(int x, int y, int width, int height, String image, String drawing, String data)
            implements GuidePiece {
    }

    /**
     * A block seen from above, the front and the side, with its balloons and the words under each view.
     *
     * @param labels the words under the top, front and side views, and the width of one block under the front
     */
    record Views(int x, int y, int width, String item, List<GuideBlock.Callout> callouts, List<String> labels)
            implements GuidePiece {

        public Views {
            callouts = List.copyOf(callouts);
            labels = List.copyOf(labels);
        }
    }

    /** Blocks seen from above side by side, each named under it, the optional ones outlined in dots. */
    record Plan(int x, int y, int width, List<PlanPart> parts) implements GuidePiece {

        public Plan {
            parts = List.copyOf(parts);
        }
    }

    /** One block of a plan, its name in the reader's language. */
    record PlanPart(String item, String label, boolean optional) {
    }
}
