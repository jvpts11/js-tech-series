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
import dev.jstech.computers.lithography.LithographyLook;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;

/**
 * The words of the lithography program that tell a Lithography Station from an Injection Moulder, for its window and
 * its text screen alike: a station's wafer, layer, mask slot and masks, a moulder's plastic, part, mould slot and
 * moulds.
 */
final class LithoWords {

    private LithoWords() {
    }

    /** The row of what it works on: the wafer, or the plastic. */
    static TextKey wafer(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.ROW_PLASTIC : LithographyTexts.ROW_WAFER;
    }

    /** The row of how far its work is: the layer, or the part. */
    static TextKey layer(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.ROW_PART : LithographyTexts.ROW_LAYER;
    }

    /** The row of the process its computer runs for it: litho, or mould. */
    static TextKey process(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.ROW_MOULD : LithographyTexts.ROW_LITHO;
    }

    /** The row of its tool's slot: the mask slot, or the mould slot. */
    static TextKey slot(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.ROW_MOULD_SLOT : LithographyTexts.ROW_SLOT;
    }

    /** What its button does: Write Mask, or Cut Mould. */
    static TextKey button(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.CUT_MOULD : LithographyTexts.WRITE_MASK;
    }

    /** What the foot of its list says of its tools. */
    static TextKey neverUsedUp(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.TOOL_NEVER_USED_UP : LithographyTexts.NEVER_USED_UP;
    }

    /** The status of its tool being made, of the name of the tool. */
    static TextKey making(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.CUTTING_THE : LithographyTexts.WRITING_THE;
    }

    /** What a blank in its slot lets the program do, of the name of the tool picked. */
    static TextKey blankHint(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.MOULD_BLANK_HINT : LithographyTexts.BLANK_HINT;
    }

    /** What a tool in its slot is for, of what stands in the slot. */
    static TextKey toolHint(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.MOULD_HINT : LithographyTexts.MASK_HINT;
    }

    /** What to do with an empty slot. */
    static TextKey emptyHint(final LithographyLook look) {
        return look.moulder() ? LithographyTexts.MOULD_EMPTY_HINT : LithographyTexts.EMPTY_HINT;
    }

    /** The title of the list of {@code kind}: a family of moulds, or a kind of chip. */
    static Text kind(final String kind) {
        return MouldCutting.Kind.named(kind).map(MouldCutting::title)
                .orElseGet(() -> MaskWriting.title(MaskWriting.Kind.of(kind)));
    }
}
