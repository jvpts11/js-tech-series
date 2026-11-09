/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.gui.Tube;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Quince's computers, from the Russet 128K to the Russet Studio (computers/docs/design/quince.md): the all-in-ones of
 * every era, the qRusset G3 in six flavours and the Advanced qRusset in seven colours, the towers of the Legacy, the
 * Transition and the Standard, and the Studio.
 *
 * <p>A tower's power button is where its model draws it on the front, in sixty-fourths from the front's top left as it
 * is seen; an all-in-one's and the Studio's is behind, low on the back as the back is seen, where the machines they are
 * drawn after had theirs. How much heat each case carries away is a first estimate, to tune in play: a little for the
 * fanless all-in-ones, more for the towers with their fans.
 */
public final class QuinceMachines {

    /* The power buttons behind: the Russet 128K's switch low on the back's left, the qRussets' and the Studio's. */
    private static final FrontArea BACK_128K = new FrontArea(14, 40, 26, 52);
    private static final FrontArea BACK_QRUSSET = new FrontArea(8, 36, 20, 48);
    private static final FrontArea BACK_STUDIO = new FrontArea(10, 44, 22, 56);
    /*
     * The all-in-ones' glass where their models set it: the tubes of the Russet 128K (black and white) and of the
     * qRusset G3 six sixty-fourths behind the face, the flat panels of the later ones ten.
     */
    private static final CaseGlass GLASS_128K = new CaseGlass(new FrontArea(17, 12, 47, 32), 6, Tube.WHITE);
    private static final CaseGlass GLASS_G3 = new CaseGlass(new FrontArea(12, 14, 52, 40), 6, Tube.COLOUR);
    private static final CaseGlass GLASS_WHITE = new CaseGlass(new FrontArea(5, 4, 59, 40), 10, Tube.COLOUR);
    private static final CaseGlass GLASS_THIN = new CaseGlass(new FrontArea(5, 3, 59, 39), 10, Tube.COLOUR);

    public static final QuinceCase RUSSET_128K = aio("russet_128k", HardwareEra.VINTAGE, QuinceBoards.RUSSET_128K,
            BACK_128K, GLASS_128K, 40);
    /** The qRusset G3's flavours, as their models name them, in the order they came out. */
    public static final List<String> G3_FLAVOURS = List.of("bondi", "blueberry", "strawberry", "lime", "grape",
            "tangerine");
    public static final List<QuinceCase> QRUSSET_G3 = G3_FLAVOURS.stream()
            .map(flavour -> aio("qrusset_g3_" + flavour, HardwareEra.LEGACY, QuinceBoards.QRUSSET_G3, BACK_QRUSSET,
                    GLASS_G3, 60))
            .toList();
    public static final QuinceCase RUSSET_POWER_G4 = tower("russet_power_g4", HardwareEra.LEGACY,
            QuinceBoards.RUSSET_POWER_G4, new FrontArea(30, 48, 34, 52), 150);
    public static final QuinceCase QRUSSET_WHITE = aio("qrusset_white", HardwareEra.TRANSITION,
            QuinceBoards.QRUSSET_TRANSITION, BACK_QRUSSET, GLASS_WHITE, 100);
    public static final QuinceCase RUSSET_PRO_TRANSITION = tower("russet_pro_t", HardwareEra.TRANSITION,
            QuinceBoards.RUSSET_PRO_TRANSITION, new FrontArea(52, 52, 56, 56), 300);
    public static final QuinceCase QRUSSET_THIN = aio("qrusset_thin", HardwareEra.STANDARD,
            QuinceBoards.QRUSSET_STANDARD, BACK_QRUSSET, GLASS_THIN, 150);
    public static final QuinceCase RUSSET_PRO_STANDARD = tower("russet_pro_s", HardwareEra.STANDARD,
            QuinceBoards.RUSSET_PRO_STANDARD, new FrontArea(8, 8, 12, 12), 400);
    /** The Advanced qRusset's colours, as their models name them. */
    public static final List<String> COLOURS = List.of("blue", "green", "pink", "silver", "yellow", "orange",
            "purple");
    public static final List<QuinceCase> QRUSSET_COLOURS = COLOURS.stream()
            .map(colour -> aio("qrusset_colour_" + colour, HardwareEra.ADVANCED, QuinceBoards.QRUSSET_ADVANCED,
                    BACK_QRUSSET, GLASS_THIN, 150))
            .toList();
    public static final QuinceCase RUSSET_STUDIO = new QuinceCase("quince_russet_studio", HardwareEra.ADVANCED,
            QuinceBoards.RUSSET_STUDIO, QuinceCase.Kind.STUDIO, BACK_STUDIO, null, 250);
    /** Every one of them, era by era. */
    public static final List<QuinceCase> ALL = all();

    private QuinceMachines() {
    }

    /** The machine whose model is {@code model}, or null when none is. */
    @Nullable
    public static QuinceCase byModel(final String model) {
        for (final QuinceCase machine : ALL) {
            if (machine.model().equals(model)) {
                return machine;
            }
        }
        return null;
    }

    private static List<QuinceCase> all() {
        final List<QuinceCase> out = new ArrayList<>();
        out.add(RUSSET_128K);
        out.addAll(QRUSSET_G3);
        out.add(RUSSET_POWER_G4);
        out.add(QRUSSET_WHITE);
        out.add(RUSSET_PRO_TRANSITION);
        out.add(QRUSSET_THIN);
        out.add(RUSSET_PRO_STANDARD);
        out.addAll(QRUSSET_COLOURS);
        out.add(RUSSET_STUDIO);
        return List.copyOf(out);
    }

    private static QuinceCase aio(final String name, final Era era, final FormFactor board, final FrontArea power,
                                  final CaseGlass glass, final int coolWatt) {
        return new QuinceCase("quince_" + name, era, board, QuinceCase.Kind.ALL_IN_ONE, power, glass, coolWatt);
    }

    private static QuinceCase tower(final String name, final Era era, final FormFactor board, final FrontArea power,
                                    final int coolWatt) {
        return new QuinceCase("quince_" + name, era, board, QuinceCase.Kind.TOWER, power, null, coolWatt);
    }
}
