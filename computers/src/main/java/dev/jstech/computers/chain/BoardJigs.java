/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import static dev.jstech.computers.chain.ChainParts.part;

import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.world.item.Item;

/**
 * The board jigs (computers/docs/design/recipes.md): the steel fixture a motherboard is assembled on, one for each
 * form factor and one for each of Quince's logic boards. A jig is cut on a Mould Blank at the Injection Moulder by the
 * computer linked to it, as a mould is, and lent to every job that assembles a board on it, at the Electronics
 * Assembly Line or the bench, never used up; so no board's lot makes another of its era and socket that differs only
 * in its size.
 */
public final class BoardJigs {

    /* What the jig of the EATX boards with two processor sockets holds, laid out unlike the one-socket boards'. */
    private static final String DUAL_EATX = "dual_eatx";

    /**
     * A jig for each form factor of the boards, and the two-socket EATX boards' own, in the order of the eras that
     * first make them.
     */
    public static final List<Jig> FORM_FACTORS = List.of(
            formFactor(HardwareEra.VINTAGE, FormFactor.AT).named("Board Jig (AT)"),
            formFactor(HardwareEra.VINTAGE, FormFactor.BABY_AT).named("Board Jig (Baby-AT)"),
            formFactor(HardwareEra.VINTAGE, FormFactor.EEB).named("Board Jig (EEB)"),
            formFactor(HardwareEra.VINTAGE, FormFactor.MTX).named("Board Jig (MTX)"),
            formFactor(HardwareEra.LEGACY, FormFactor.ATX).named("Board Jig (ATX)"),
            formFactor(HardwareEra.LEGACY, FormFactor.EATX).named("Board Jig (EATX)"),
            new Declaration(HardwareEra.LEGACY, DUAL_EATX, "board_jig_dual_eatx").named("Board Jig (Dual EATX)"),
            formFactor(HardwareEra.TRANSITION, FormFactor.MNG).named("Board Jig (MNG)"));
    /** A jig for each of Quince's logic boards, in the order of the eras. */
    public static final List<Jig> QUINCE = List.of(
            quince(HardwareEra.VINTAGE, "russet_128k").named("Board Jig (Russet 128K)"),
            quince(HardwareEra.LEGACY, "qrusset_g3").named("Board Jig (qRusset G3)"),
            quince(HardwareEra.LEGACY, "power_g4").named("Board Jig (Russet Power G4)"),
            quince(HardwareEra.LEGACY, "qserve_legacy").named("Board Jig (qServe, Legacy)"),
            quince(HardwareEra.TRANSITION, "pro_2006").named("Board Jig (Russet Pro, Transition)"),
            quince(HardwareEra.TRANSITION, "qrusset_white").named("Board Jig (qRusset, Transition)"),
            quince(HardwareEra.TRANSITION, "qserve_transition").named("Board Jig (qServe, Transition)"),
            quince(HardwareEra.TRANSITION, "mainframe_t").named("Board Jig (Quince Mainframe, Transition)"),
            quince(HardwareEra.STANDARD, "qrusset_thin").named("Board Jig (qRusset, Standard)"),
            quince(HardwareEra.STANDARD, "pro_2019").named("Board Jig (Russet Pro, Standard)"),
            quince(HardwareEra.STANDARD, "qserve_standard").named("Board Jig (qServe, Standard)"),
            quince(HardwareEra.STANDARD, "mainframe_s").named("Board Jig (Quince Mainframe, Standard)"),
            quince(HardwareEra.ADVANCED, "russet_studio").named("Board Jig (Russet Studio)"),
            quince(HardwareEra.ADVANCED, "qrusset_colour").named("Board Jig (qRusset, Advanced)"),
            quince(HardwareEra.ADVANCED, "qserve_advanced").named("Board Jig (qServe, Advanced)"),
            quince(HardwareEra.ADVANCED, "mainframe_a").named("Board Jig (Quince Mainframe, Advanced)"));

    private BoardJigs() {
    }

    /** Declares the jigs, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every jig, the form factors' first. */
    public static List<Jig> all() {
        return Stream.concat(FORM_FACTORS.stream(), QUINCE.stream()).toList();
    }

    /** The jig of the boards of {@code formFactor}; none for a form factor whose boards take no jig. */
    public static Optional<Jig> of(final FormFactor formFactor) {
        return FORM_FACTORS.stream().filter(jig -> jig.boards().equals(formFactor.id())).findFirst();
    }

    /** The jig {@code board} is assembled on: its form factor's, or the two-socket EATX boards' own. */
    public static Optional<Jig> of(final MotherboardSpec board) {
        if (board.formFactor() == FormFactor.EATX && board.cpuSlots() > 1) {
            return FORM_FACTORS.stream().filter(jig -> jig.boards().equals(DUAL_EATX)).findFirst();
        }
        return of(board.formFactor());
    }

    /** The jig of Quince's logic board {@code board}, "russet_128k". */
    public static Jig ofQuince(final String board) {
        return QUINCE.stream().filter(jig -> jig.boards().equals(board)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no jig of the Quince board " + board));
    }

    /* The jig of {@code formFactor}'s boards, "board_jig_atx", to be named. */
    private static Declaration formFactor(final Era era, final FormFactor formFactor) {
        return new Declaration(era, formFactor.id(), "board_jig_" + formFactor.id().substring(formFactor.id()
                .indexOf(':') + 1));
    }

    /* The jig of Quince's logic board {@code board}, "board_jig_quince_russet_128k", to be named. */
    private static Declaration quince(final Era era, final String board) {
        return new Declaration(era, board, "board_jig_quince_" + board);
    }

    /**
     * One jig.
     *
     * @param era    the era it is first used in
     * @param boards what it holds: a form factor's id, "jsc:atx", the two-socket EATX boards' "dual_eatx", or a
     *               Quince logic board's, "russet_128k"
     * @param item   its item
     */
    public record Jig(Era era, String boards, ItemEntry<Item> item) {
    }

    /* A jig declared by its era, what it holds and its item's id, named last, which registers it. */
    private record Declaration(Era era, String boards, String id) {

        Jig named(final String english) {
            return new Jig(era, boards, part(id, era).named(english).register());
        }
    }
}
