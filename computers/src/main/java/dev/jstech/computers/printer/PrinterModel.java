/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import org.jetbrains.annotations.Nullable;

/**
 * The printer of each era. A printer is generic, it prints whatever a program sends it; what changes from one era to
 * the next is the machine: how it puts ink on paper (a dot matrix head, an inkjet's bands, a laser's toner), the sheet
 * it turns out (the fanfold with its tractor holes, or a plain sheet), and how long a page takes, which is the length
 * of the sound it makes while one comes out.
 *
 * <p>Pure enum (no Minecraft imports), so what each printer does can be tested without the game.
 */
@TextHolder
public enum PrinterModel implements IStableName {

    /** The dot matrix of the eighties, on its stand, the fanfold paper on the shelf under it. */
    EPSILON_FX_80("epsilon_fx_80", HardwareEra.VINTAGE, Sheet.FANFOLD, Ink.DOT_MATRIX, 144, "epsilon",
            "printer_dotmatrix"),
    /** The colour inkjet of the turn of the century. */
    PAKARD_DESKJOT_940("pakard_deskjot_940", HardwareEra.LEGACY, Sheet.PLAIN, Ink.INKJET_COARSE, 120, "deskjot",
            "printer_inkjet"),
    /** The all-in-one of the late two-thousands: a finer inkjet under a scanner lid. */
    PAKARD_FOTOSMART_C4280("pakard_fotosmart_c4280", HardwareEra.TRANSITION, Sheet.PLAIN, Ink.INKJET_FINE, 100,
            "fotosmart", "printer_aio"),
    /** The small mono laser of the twenty-tens. */
    PAKARD_LASERJOT_1102("pakard_laserjot_1102", HardwareEra.STANDARD, Sheet.PLAIN, Ink.LASER, 70, "laserjot",
            "printer_laser"),
    /** The ink tank printer of today, its four inks in windows at its side. */
    EPSILON_ECOTONK_ET_2720("epsilon_ecotonk_et_2720", HardwareEra.ADVANCED, Sheet.PLAIN, Ink.CLEAN, 110, "ecotonk",
            "printer_inktank");

    private final String serializedName;
    private final HardwareEra era;
    private final Sheet sheet;
    private final Ink ink;
    private final int pageTicks;
    private final String queueName;
    private final String icon;

    private static final StableNames<PrinterModel> NAMES = StableNames.of(PrinterModel.class);

    private static final TextKey FX_80 = TextKey.of("jsc.printer.model.epsilon_fx_80", "Epsilon FX-80");
    private static final TextKey DESKJOT = TextKey.of("jsc.printer.model.pakard_deskjot_940", "Pakard DeskJot 940");
    private static final TextKey FOTOSMART = TextKey.of("jsc.printer.model.pakard_fotosmart_c4280",
            "Pakard FotoSmart C4280");
    private static final TextKey LASERJOT = TextKey.of("jsc.printer.model.pakard_laserjot_1102",
            "Pakard LaserJot 1102");
    private static final TextKey ECOTONK = TextKey.of("jsc.printer.model.epsilon_ecotonk_et_2720",
            "Epsilon EcoTonk ET-2720");
    private static final TextKey A_LINE = TextKey.of("jsc.printer.pace.line", "a line at a time");
    private static final TextKey A_BAND = TextKey.of("jsc.printer.pace.band", "a band at a time");
    private static final TextKey A_PAGE = TextKey.of("jsc.printer.pace.page", "a page at a time");

    PrinterModel(final String serializedName, final HardwareEra era, final Sheet sheet, final Ink ink,
                 final int pageTicks, final String queueName, final String icon) {
        this.serializedName = serializedName;
        this.era = era;
        this.sheet = sheet;
        this.ink = ink;
        this.pageTicks = pageTicks;
        this.queueName = queueName;
        this.icon = icon;
    }

    /** The printer of {@code era}; an era after the Advanced has the Advanced's until it has its own. */
    public static PrinterModel of(final HardwareEra era) {
        for (final PrinterModel model : values()) {
            if (model.era == era) {
                return model;
            }
        }
        return EPSILON_ECOTONK_ET_2720;
    }

    /** The printer a name stands for, or null for a name no printer declares. */
    @Nullable
    public static PrinterModel find(@Nullable final String name) {
        return NAMES.find(name);
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    public HardwareEra era() {
        return era;
    }

    /** The paper it turns out. */
    public Sheet sheet() {
        return sheet;
    }

    /** How it puts a picture on paper. */
    public Ink ink() {
        return ink;
    }

    /** Ticks one page takes to come out, the length of the sound the printer makes for it. */
    public int pageTicks() {
        return pageTicks;
    }

    /** The name its queue goes by in {@code lp} and {@code lpstat}: the request ids are this, a dash and a number. */
    public String queueName() {
        return queueName;
    }

    /** The device icon the systems show it with. */
    public String icon() {
        return icon;
    }

    /** The name it was sold under. */
    public TextKey displayName() {
        return switch (this) {
            case EPSILON_FX_80 -> FX_80;
            case PAKARD_DESKJOT_940 -> DESKJOT;
            case PAKARD_FOTOSMART_C4280 -> FOTOSMART;
            case PAKARD_LASERJOT_1102 -> LASERJOT;
            case EPSILON_ECOTONK_ET_2720 -> ECOTONK;
        };
    }

    /** How the page comes out, as the printer's window says under the page count. */
    public TextKey pace() {
        return switch (ink) {
            case DOT_MATRIX -> A_LINE;
            case LASER -> A_PAGE;
            case INKJET_COARSE, INKJET_FINE, CLEAN -> A_BAND;
        };
    }

    /** The paper a printer turns out. */
    public enum Sheet {
        /** Continuous paper with green bars and the tractor holes down both sides. */
        FANFOLD,
        /** A plain office sheet. */
        PLAIN
    }

    /** How a printer puts a picture on paper. */
    public enum Ink {
        /** Black dots from a nine-pin head, one pass of the head every eight rows. */
        DOT_MATRIX,
        /** Cyan, magenta, yellow and black dots, coarse, the head's passes showing as bands. */
        INKJET_COARSE,
        /** The same inks, finer, with no bands. */
        INKJET_FINE,
        /** Toner in a grey halftone. */
        LASER,
        /** The picture as it is. */
        CLEAN
    }
}
