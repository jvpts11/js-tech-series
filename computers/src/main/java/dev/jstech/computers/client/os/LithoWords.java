/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.chain.machine.MaskWriting;
import dev.jstech.computers.chain.machine.MouldCutting;
import dev.jstech.computers.lithography.FilmPrinting;
import dev.jstech.computers.lithography.LithographyLook;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;

/**
 * The words of the lithography program that tell a Lithography Station from an Injection Moulder and a printer, for
 * its window and its text screen alike: a station's wafer, layer, mask slot and masks, a moulder's plastic, part,
 * mould slot and moulds, a printer's tray and films.
 */
final class LithoWords {

    private LithoWords() {
    }

    /** What the machine is linked on: a station's or moulder's peripheral port, a printer's device port. */
    static TextKey linked(final LithographyLook look) {
        return look.printer() ? LithographyTexts.DEVICE_PORT : LithographyTexts.PERIPHERAL_PORT;
    }

    /** The row of what it works on: the wafer, the plastic, or the tray. */
    static TextKey wafer(final LithographyLook look) {
        return switch (look.machine()) {
            case STATION -> LithographyTexts.ROW_WAFER;
            case MOULDER -> LithographyTexts.ROW_PLASTIC;
            case PRINTER -> LithographyTexts.ROW_TRAY;
        };
    }

    /** The row of how far its work is: the layer, or the part. */
    static TextKey layer(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.ROW_PART : LithographyTexts.ROW_LAYER;
    }

    /** The row of the process its computer runs for it: litho, or mould. A printer has none. */
    static TextKey process(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.ROW_MOULD : LithographyTexts.ROW_LITHO;
    }

    /** The row of its tool's slot: the mask slot, or the mould slot. A printer has none. */
    static TextKey slot(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.ROW_MOULD_SLOT : LithographyTexts.ROW_SLOT;
    }

    /** What its button does: Write Mask, Cut Mould, or Print Film. */
    static TextKey button(final LithographyLook look) {
        return switch (look.machine()) {
            case STATION -> LithographyTexts.WRITE_MASK;
            case MOULDER -> LithographyTexts.CUT_MOULD;
            case PRINTER -> LithographyTexts.PRINT_FILM;
        };
    }

    /** What the foot of its list of {@code kind} says of its tools, of which only a printer's Layouts are used up. */
    static TextKey neverUsedUp(final LithographyLook look, final String kind) {
        return switch (look.machine()) {
            case STATION -> LithographyTexts.NEVER_USED_UP;
            case MOULDER -> LithographyTexts.TOOL_NEVER_USED_UP;
            case PRINTER -> FilmPrinting.Kind.named(kind).orElse(FilmPrinting.Kind.PHOTOTOOLS)
                    == FilmPrinting.Kind.LAYOUTS ? LithographyTexts.LAYOUT_USED_UP
                    : LithographyTexts.PHOTOTOOL_NEVER_USED_UP;
        };
    }

    /** The status of its tool being made, of the name of the tool. */
    static TextKey making(final LithographyLook look) {
        return switch (look.machine()) {
            case STATION -> LithographyTexts.WRITING_THE;
            case MOULDER -> LithographyTexts.CUTTING_THE;
            case PRINTER -> LithographyTexts.PRINTING_THE;
        };
    }

    /** What a blank in its slot, or Blank Film in its tray, lets the program do, of the name of the tool picked. */
    static TextKey blankHint(final LithographyLook look) {
        return switch (look.machine()) {
            case STATION -> LithographyTexts.BLANK_HINT;
            case MOULDER -> LithographyTexts.MOULD_BLANK_HINT;
            case PRINTER -> LithographyTexts.FILM_BLANK_HINT;
        };
    }

    /** What a tool in its slot is for, of what stands in the slot. */
    static TextKey toolHint(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.MOULD_HINT : LithographyTexts.MASK_HINT;
    }

    /** What to do with an empty slot, or a tray with no Blank Film. */
    static TextKey emptyHint(final LithographyLook look) {
        return switch (look.machine()) {
            case STATION -> LithographyTexts.EMPTY_HINT;
            case MOULDER -> LithographyTexts.MOULD_EMPTY_HINT;
            case PRINTER -> FilmPrinting.NO_BLANK;
        };
    }

    /** The title of the list of {@code kind}: a family of moulds, a kind of film, or a kind of chip. */
    static Text kind(final String kind) {
        return MouldCutting.Kind.named(kind).map(MouldCutting::title)
                .or(() -> FilmPrinting.Kind.named(kind).map(FilmPrinting::title))
                .orElseGet(() -> MaskWriting.title(MaskWriting.Kind.of(kind)));
    }
}
