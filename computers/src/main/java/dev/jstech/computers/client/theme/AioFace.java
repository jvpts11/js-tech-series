/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.theme;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.hardware.QuinceMachines;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The face of one of Quince's all-in-ones round its own screen (computers/docs/design/quince.md): sat at one, the
 * player sees the desktop inside the machine's front, not inside a monitor's. The Russet 128K's beige, the qRusset
 * G3's ice white over the colour of its shell, the white qRusset, the thin one's black glass over its aluminium chin,
 * and the coloured one's chin in its colour.
 *
 * <p>The shape is a slim monitor's, the eight-pixel shell and the chin under the glass; the colours are the palettes
 * {@code jsc:monitor/<machine>}, which a resource pack can recolour.
 *
 * @param label the machine's name on its chin
 * @param chin  whether the chin is its own colour under the face, as the thin and the coloured ones' are
 * @param foot  whether the shell's colour shows along the foot, as the G3's translucent one does
 * @param inks  the face's colours
 */
@PaletteHolder
public record AioFace(String label, boolean chin, boolean foot, Palette<Inks> inks) {

    private static final AioFace RUSSET_128K = new AioFace("Russet 128K", false, false,
            Palettes.declare(JsComputers.MODID, "monitor/russet_128k", new Inks(
                    0xFFD9CFB2, 0xFFECE4CC, 0xFFA89E80, 0xFF8A8068, 0xFFD9CFB2, 0xFFD9CFB2, 0xFFD9CFB2)));
    private static final AioFace QRUSSET_WHITE = new AioFace("qRusset", false, false,
            Palettes.declare(JsComputers.MODID, "monitor/qrusset_white", new Inks(
                    0xFFF2F2F0, 0xFFFFFFFF, 0xFFC8C8C4, 0xFF9A9A96, 0xFFF2F2F0, 0xFFF2F2F0, 0xFFF2F2F0)));
    private static final AioFace QRUSSET_THIN = new AioFace("qRusset", true, false,
            Palettes.declare(JsComputers.MODID, "monitor/qrusset_thin", new Inks(
                    0xFF2A2B2E, 0xFF3A3B3E, 0xFF1A1B1E, 0xFF8A8E94, 0xFFC9CCD0, 0xFFE2E4E7, 0xFFC9CCD0)));
    /**
     * The G3's flavours, in the order of {@code QuinceMachines.G3_FLAVOURS}: the ice white face, and its shell's two
     * tones along the foot.
     */
    private static final List<AioFace> G3 = List.of(
            g3(Palettes.declare(JsComputers.MODID, "monitor/qrusset_g3_bondi", new Inks(
                    0xFFE8ECEE, 0xFFFFFFFF, 0xFFB8C0C6, 0xFF8A949C, 0xFFE8ECEE, 0xFF2FA4C4, 0xFF1B88A8))),
            g3(Palettes.declare(JsComputers.MODID, "monitor/qrusset_g3_blueberry", new Inks(
                    0xFFE8ECEE, 0xFFFFFFFF, 0xFFB8C0C6, 0xFF8A949C, 0xFFE8ECEE, 0xFF3A5CC8, 0xFF2A44A0))),
            g3(Palettes.declare(JsComputers.MODID, "monitor/qrusset_g3_strawberry", new Inks(
                    0xFFE8ECEE, 0xFFFFFFFF, 0xFFB8C0C6, 0xFF8A949C, 0xFFE8ECEE, 0xFFE0506A, 0xFFB8384E))),
            g3(Palettes.declare(JsComputers.MODID, "monitor/qrusset_g3_lime", new Inks(
                    0xFFE8ECEE, 0xFFFFFFFF, 0xFFB8C0C6, 0xFF8A949C, 0xFFE8ECEE, 0xFF7CC83A, 0xFF5CA02A))),
            g3(Palettes.declare(JsComputers.MODID, "monitor/qrusset_g3_grape", new Inks(
                    0xFFE8ECEE, 0xFFFFFFFF, 0xFFB8C0C6, 0xFF8A949C, 0xFFE8ECEE, 0xFF8A4AB0, 0xFF6A3290))),
            g3(Palettes.declare(JsComputers.MODID, "monitor/qrusset_g3_tangerine", new Inks(
                    0xFFE8ECEE, 0xFFFFFFFF, 0xFFB8C0C6, 0xFF8A949C, 0xFFE8ECEE, 0xFFF0903A, 0xFFC86E22))));
    /** The coloured qRusset's colours, in the order of {@code QuinceMachines.COLOURS}: its white face over its chin. */
    private static final List<AioFace> COLOURED = List.of(
            coloured(Palettes.declare(JsComputers.MODID, "monitor/qrusset_colour_blue", new Inks(
                    0xFFF4F6FA, 0xFFFFFFFF, 0xFFD2D8E2, 0xFF8A94A8, 0xFFC8D8EE, 0xFFC8D8EE, 0xFFC8D8EE))),
            coloured(Palettes.declare(JsComputers.MODID, "monitor/qrusset_colour_green", new Inks(
                    0xFFF4F6FA, 0xFFFFFFFF, 0xFFD2D8E2, 0xFF8A94A8, 0xFFC8E6CC, 0xFFC8E6CC, 0xFFC8E6CC))),
            coloured(Palettes.declare(JsComputers.MODID, "monitor/qrusset_colour_pink", new Inks(
                    0xFFF4F6FA, 0xFFFFFFFF, 0xFFD2D8E2, 0xFF8A94A8, 0xFFF2CCD8, 0xFFF2CCD8, 0xFFF2CCD8))),
            coloured(Palettes.declare(JsComputers.MODID, "monitor/qrusset_colour_silver", new Inks(
                    0xFFF4F6FA, 0xFFFFFFFF, 0xFFD2D8E2, 0xFF8A94A8, 0xFFDCDEE2, 0xFFDCDEE2, 0xFFDCDEE2))),
            coloured(Palettes.declare(JsComputers.MODID, "monitor/qrusset_colour_yellow", new Inks(
                    0xFFF4F6FA, 0xFFFFFFFF, 0xFFD2D8E2, 0xFF8A94A8, 0xFFF4E6B4, 0xFFF4E6B4, 0xFFF4E6B4))),
            coloured(Palettes.declare(JsComputers.MODID, "monitor/qrusset_colour_orange", new Inks(
                    0xFFF4F6FA, 0xFFFFFFFF, 0xFFD2D8E2, 0xFF8A94A8, 0xFFF4D2B4, 0xFFF4D2B4, 0xFFF4D2B4))),
            coloured(Palettes.declare(JsComputers.MODID, "monitor/qrusset_colour_purple", new Inks(
                    0xFFF4F6FA, 0xFFFFFFFF, 0xFFD2D8E2, 0xFF8A94A8, 0xFFDCCCEE, 0xFFDCCCEE, 0xFFDCCCEE))));

    /** The face of the all-in-one whose model is {@code model}, or null for a machine that is no all-in-one. */
    @Nullable
    public static AioFace of(final String model) {
        if (model.equals("quince_russet_128k")) {
            return RUSSET_128K;
        }
        if (model.equals("quince_qrusset_white")) {
            return QRUSSET_WHITE;
        }
        if (model.equals("quince_qrusset_thin")) {
            return QRUSSET_THIN;
        }
        if (model.startsWith("quince_qrusset_g3_")) {
            final int at = QuinceMachines.G3_FLAVOURS.indexOf(model.substring("quince_qrusset_g3_".length()));
            return at < 0 || at >= G3.size() ? null : G3.get(at);
        }
        if (model.startsWith("quince_qrusset_colour_")) {
            final int at = QuinceMachines.COLOURS.indexOf(model.substring("quince_qrusset_colour_".length()));
            return at < 0 || at >= COLOURED.size() ? null : COLOURED.get(at);
        }
        return null;
    }

    private static AioFace g3(final Palette<Inks> inks) {
        return new AioFace("qRusset G3", false, true, inks);
    }

    private static AioFace coloured(final Palette<Inks> inks) {
        return new AioFace("qRusset", true, false, inks);
    }

    /**
     * A face's colours.
     *
     * @param face      the face round the glass
     * @param light     its lit top and left edges
     * @param dark      its shaded foot and right edges, and the line round the glass
     * @param label     the name on the chin
     * @param chin      the chin, where it is a colour of its own
     * @param chinLight the chin's lit top edge, or the shell's light tone along a G3's foot
     * @param chinDeep  the shell's deep tone along a G3's foot
     */
    public record Inks(int face, int light, int dark, int label, int chin, int chinLight, int chinDeep) {
    }
}
