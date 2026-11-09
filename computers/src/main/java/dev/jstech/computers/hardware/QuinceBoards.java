/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.Set;

/**
 * The forms of Quince's logic boards (computers/docs/design/quince.md). Quince's hardware is closed: each of its
 * machines takes only the board made for it, and that board goes in no other case, so every machine has a form of its
 * own. There are sixteen: one for each computer, one for the qServe of each era and one for the Quince Mainframe of
 * each era. The qRusset G3's six flavours share one, as the coloured qRusset's seven colours do.
 *
 * <p>An all-in-one's board carries its graphics, and a qServe's the management controller of a rack server, so both
 * show a picture with no graphics card. The towers', the Studio's and the Mainframe's do not.
 */
public final class QuinceBoards {

    public static final FormFactor RUSSET_128K = FormFactor.own("quince_russet_128k", "Russet 128K", true);
    public static final FormFactor QRUSSET_G3 = FormFactor.own("quince_qrusset_g3", "qRusset G3", true);
    public static final FormFactor RUSSET_POWER_G4 = FormFactor.own("quince_russet_power_g4", "Russet Power G4",
            false);
    public static final FormFactor QRUSSET_TRANSITION = FormFactor.own("quince_qrusset_transition",
            "qRusset (Transition)", true);
    public static final FormFactor RUSSET_PRO_TRANSITION = FormFactor.own("quince_russet_pro_transition",
            "Russet Pro (Transition)", false);
    public static final FormFactor QRUSSET_STANDARD = FormFactor.own("quince_qrusset_standard", "qRusset (Standard)",
            true);
    public static final FormFactor RUSSET_PRO_STANDARD = FormFactor.own("quince_russet_pro_standard",
            "Russet Pro (Standard)", false);
    public static final FormFactor QRUSSET_ADVANCED = FormFactor.own("quince_qrusset_advanced", "qRusset (Advanced)",
            true);
    public static final FormFactor RUSSET_STUDIO = FormFactor.own("quince_russet_studio", "Russet Studio", false);
    public static final FormFactor QSERVE_LEGACY = FormFactor.own("quince_qserve_legacy", "qServe (Legacy)", true);
    public static final FormFactor QSERVE_TRANSITION = FormFactor.own("quince_qserve_transition",
            "qServe (Transition)", true);
    public static final FormFactor QSERVE_STANDARD = FormFactor.own("quince_qserve_standard", "qServe (Standard)",
            true);
    public static final FormFactor QSERVE_ADVANCED = FormFactor.own("quince_qserve_advanced", "qServe (Advanced)",
            true);
    public static final FormFactor MAINFRAME_TRANSITION = FormFactor.own("quince_mainframe_transition",
            "Quince Mainframe (Transition)", false);
    public static final FormFactor MAINFRAME_STANDARD = FormFactor.own("quince_mainframe_standard",
            "Quince Mainframe (Standard)", false);
    public static final FormFactor MAINFRAME_ADVANCED = FormFactor.own("quince_mainframe_advanced",
            "Quince Mainframe (Advanced)", false);
    /** Every one of them: what makes a machine one of Quince's, its firmware on its board with them. */
    public static final Set<FormFactor> ALL = Set.of(RUSSET_128K, QRUSSET_G3, RUSSET_POWER_G4, QRUSSET_TRANSITION,
            RUSSET_PRO_TRANSITION, QRUSSET_STANDARD, RUSSET_PRO_STANDARD, QRUSSET_ADVANCED, RUSSET_STUDIO,
            QSERVE_LEGACY, QSERVE_TRANSITION, QSERVE_STANDARD, QSERVE_ADVANCED, MAINFRAME_TRANSITION,
            MAINFRAME_STANDARD, MAINFRAME_ADVANCED);

    private QuinceBoards() {
    }

    /** Whether a board of {@code form} is one of Quince's. */
    public static boolean isQuince(final FormFactor form) {
        return ALL.contains(form);
    }
}
