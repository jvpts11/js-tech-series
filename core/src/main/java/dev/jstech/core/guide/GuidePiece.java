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

    /** A key of the keyboard in the middle of a sentence, drawn as a key with its name on it: [Ctrl]. */
    record Key(int x, int y, String name) implements GuidePiece {
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
     * A listing's frame, or the part of it on this page: the strip over it with its label, its file's name and its
     * Copy button, the paper its code is printed on, and the rule beside the numbers of its lines. The code itself is
     * laid over it as text.
     *
     * @param label    "Listing 3-4."
     * @param name     the file's name, with "(continued)" after it on a page the listing runs on to
     * @param gutter   how wide the column of line numbers is, or 0 when its lines are not numbered
     * @param language the language that colours it, for when it is opened over the whole book
     * @param lines    all its lines as typed, for its Copy button and for when it is opened over the whole book
     */
    record Listing(int x, int y, int width, int height, String label, String name, int gutter, String language,
                   List<String> lines) implements GuidePiece {

        public Listing {
            lines = List.copyOf(lines);
        }
    }

    /**
     * A session at a terminal, or the part of it on this page, drawn in its terminal's own colours: the strip with
     * its name, and its rows, each in stretches of what was typed, its prompt and what the system said.
     *
     * @param look  the palette of the terminal's colours
     * @param title what its strip says, with "(continued)" on a page it runs on to
     * @param rows  its rows, already cut to the width of the page as a terminal cuts them
     */
    record Session(int x, int y, int width, int height, String look, String title, List<List<SessionRun>> rows)
            implements GuidePiece {

        public Session {
            rows = rows.stream().map(List::copyOf).toList();
        }
    }

    /** A stretch of a session's row, in one of its terminal's colours. */
    record SessionRun(String text, SessionInk ink) {
    }

    /** What a stretch of a session is, and so its colour: a prompt, what was typed, or what the system said. */
    enum SessionInk {
        PROMPT, TYPED, ANSWER
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
     * @param image      the texture, or empty for a drawing
     * @param drawing    the kind of drawing, or empty for an image
     * @param frames     how many pictures the texture holds one under the other, shown in turn; 1 for a still one
     * @param frameTicks how many ticks each is shown
     */
    record Picture(int x, int y, int width, int height, String image, String drawing, String data, int frames,
                   int frameTicks) implements GuidePiece {

        /** A still picture. */
        public Picture(final int x, final int y, final int width, final int height, final String image,
                       final String drawing, final String data) {
            this(x, y, width, height, image, drawing, data, 1, 1);
        }
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

    /**
     * A scene in its frame, the strip of its controls along the frame's foot.
     *
     * @param height the frame's height, the strip's included
     * @param id     what tells this scene from every other in the manual, so how a reader turned it is kept
     * @param scene  the scene, its blocks, its first view and its balloons
     */
    record Scene(int x, int y, int width, int height, String id, GuideBlock.Scene scene) implements GuidePiece {
    }

    /**
     * A line of a scene's legend: its number in a balloon at the line's start, and the room the line takes, which
     * lights with the part it names under the pointer. Its words are pieces of their own after it.
     *
     * @param scene  the id of the scene it belongs to
     * @param number the balloon's number
     */
    record SceneKey(int x, int y, int width, int height, String scene, int number) implements GuidePiece {
    }

    /**
     * The switch over a figure drawn more than one way ({@link GuideBlock.Modes}): a word for each way, side by side
     * at the column's right, the way shown filled in.
     *
     * @param group  the figure, which its ways' pieces name
     * @param labels the word for each way, in the reader's language
     */
    record Switch(int x, int y, int width, int height, String group, List<String> labels) implements GuidePiece {

        public Switch {
            labels = List.copyOf(labels);
        }
    }

    /**
     * A piece of one way of drawing a figure drawn more than one way: shown only while the figure's switch stands at
     * its way.
     *
     * @param group the figure's switch
     * @param way   which of its ways it belongs to, from the first, 0
     * @param piece the piece itself
     */
    record Way(String group, int way, GuidePiece piece) implements GuidePiece {

        @Override
        public int x() {
            return this.piece.x();
        }

        @Override
        public int y() {
            return this.piece.y();
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
