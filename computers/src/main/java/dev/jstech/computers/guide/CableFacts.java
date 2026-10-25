/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 * The manual's tables of how far each cable reaches and how much it moves, read from the cables themselves as the book
 * is opened, so a reach changed in the code is the reach the book prints and no number is written twice.
 */
@TextHolder
public final class CableFacts {

    /** How far a run of each data line reaches in each era, in cables. */
    public static final String LINE_REACH = "jsc:line_reach";
    /** How many items a tick each data line moves in each era. */
    public static final String LINE_SPEED = "jsc:line_speed";
    /** How far each era's peripheral cable reaches from its computer, in cables. */
    public static final String PERIPHERAL_REACH = "jsc:peripheral_reach";

    private static final TextKey ACCESS = TextKey.of("jsc.guide.facts.line.access", "Access");
    private static final TextKey BACKBONE = TextKey.of("jsc.guide.facts.line.backbone", "Backbone");
    private static final TextKey LONG_DISTANCE = TextKey.of("jsc.guide.facts.line.long_distance", "Long distance");
    private static final TextKey HPC = TextKey.of("jsc.guide.facts.line.hpc", "High compute");
    private static final TextKey CRAFTING = TextKey.of("jsc.guide.facts.line.crafting", "Crafting");
    /** What a table says in an era a line does not exist in. */
    static final TextKey NONE = TextKey.of("jsc.guide.facts.none", "none");
    /** The eras the tables run across, in the order of their columns. */
    private static final List<Era> ERAS = HardwareEra.WITH_PARTS;
    /* Each era's peripheral cable, asked for when a page is written, never while this class loads. */
    private static final EraTable<Supplier<CableEntry>> PERIPHERAL_CABLES = EraTable.ofFive(
            () -> ComputingModule.VINTAGE_PERIPHERAL_CABLE,
            () -> ComputingModule.LEGACY_PERIPHERAL_CABLE,
            () -> ComputingModule.TRANSITION_PERIPHERAL_CABLE,
            () -> ComputingModule.PERIPHERAL_CABLE,
            () -> ComputingModule.ADVANCED_PERIPHERAL_CABLE);

    private CableFacts() {
    }

    /** Says where the manual reads these tables from: once, as the manual is declared. */
    public static void declare() {
        GuideFacts.rows(LINE_REACH, () -> lineRows(DataLink::range));
        GuideFacts.rows(LINE_SPEED, () -> lineRows(DataLink::throughput));
        GuideFacts.rows(PERIPHERAL_REACH, CableFacts::peripheralRows);
    }

    /* A row for each data line, its name and then its figure in each era, or "none" where it has no cable. */
    private static List<GuideBlock.TableRow> lineRows(final ToLongFunction<DataLink> figure) {
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        for (final DataLine line : DataLine.values()) {
            final List<GuideBlock.GuideValue> cells = new ArrayList<>();
            cells.add(new GuideBlock.GuideValue.Words(nameOf(line).key()));
            for (final Era era : ERAS) {
                final DataLink link = new DataLink(line, era);
                cells.add(link.exists() ? new GuideBlock.GuideValue.Amount(figure.applyAsLong(link), "")
                        : new GuideBlock.GuideValue.Words(NONE.key()));
            }
            rows.add(new GuideBlock.TableRow(cells));
        }
        return rows;
    }

    /* A row for each era's peripheral cable: the era, the cable's name and how far the cable says it reaches. */
    private static List<GuideBlock.TableRow> peripheralRows() {
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        for (final Era era : ERAS) {
            final CableEntry cable = peripheralCableOf(era);
            rows.add(new GuideBlock.TableRow(List.of(new GuideBlock.GuideValue.Said(era.named()),
                    new GuideBlock.GuideValue.Words(cable.asItem().getDescriptionId()),
                    new GuideBlock.GuideValue.Amount(cable.get().range(), ""))));
        }
        return rows;
    }

    private static TextKey nameOf(final DataLine line) {
        return switch (line) {
            case ACCESS -> ACCESS;
            case BACKBONE -> BACKBONE;
            case LONG_DISTANCE -> LONG_DISTANCE;
            case HPC -> HPC;
            case CRAFTING -> CRAFTING;
        };
    }

    private static CableEntry peripheralCableOf(final Era era) {
        return PERIPHERAL_CABLES.get(era).get();
    }
}
