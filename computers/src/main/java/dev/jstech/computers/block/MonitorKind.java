/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.Codec;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.util.StringRepresentable;

/**
 * Which monitor a monitor block is: its era, the tube its picture goes through, whether it is a flat panel that joins
 * the panels beside it into one big screen, and where on its front the glass and the power button are.
 *
 * <p>The Vintage era is the decade of the terminal tubes: the paper-white Mono I, the green Mono II, the amber one,
 * and the sixteen colours of the CGA, the only colour monitor of its time. Then the colour picture tubes of the Legacy
 * era, and the flat panels from the Transition on, which are the only ones that join.
 *
 * <p>The glass and the button are given in sixty-fourths of the block's front, counted from its top left as the front
 * is seen, the way the front's texture is drawn: {@code (x0, y0, x1, y1)}.
 */
public enum MonitorKind implements StringRepresentable {

    MONO_I("mono_i", HardwareEra.VINTAGE, Tube.WHITE, false, new int[] {8, 7, 56, 39}, new int[] {50, 48, 56, 54}),
    MONO_II("mono_ii", HardwareEra.VINTAGE, Tube.GREEN, false, new int[] {6, 9, 48, 37}, new int[] {51, 48, 57, 54}),
    AMBER("amber", HardwareEra.VINTAGE, Tube.AMBER, false, new int[] {10, 9, 54, 38}, new int[] {49, 49, 55, 55}),
    CGA("cga", HardwareEra.VINTAGE, Tube.SIXTEEN, false, new int[] {6, 9, 48, 37}, new int[] {51, 48, 57, 54}),
    LEGACY("legacy", HardwareEra.LEGACY, Tube.COLOUR, false, new int[] {8, 7, 56, 39}, new int[] {49, 49, 55, 54}),
    TRANSITION("transition", HardwareEra.TRANSITION, Tube.COLOUR, true, new int[] {5, 5, 59, 41},
            new int[] {51, 52, 57, 58}),
    STANDARD("standard", HardwareEra.STANDARD, Tube.COLOUR, true, new int[] {3, 3, 61, 42},
            new int[] {53, 52, 58, 57}),
    COLOR("advanced", HardwareEra.ADVANCED, Tube.COLOUR, true, new int[] {1, 1, 63, 42},
            new int[] {29, 57, 35, 62});

    private final String id;
    private final HardwareEra era;
    private final Tube tube;
    private final boolean flat;
    private final int[] glass;
    private final int[] button;

    public static final Codec<MonitorKind> CODEC = StringRepresentable.fromEnum(MonitorKind::values);
    /** The front is drawn this many units across. */
    public static final int FRONT = 64;

    MonitorKind(final String id, final HardwareEra era, final Tube tube, final boolean flat, final int[] glass,
                final int[] button) {
        this.id = id;
        this.era = era;
        this.tube = tube;
        this.flat = flat;
        this.glass = glass;
        this.button = button;
    }

    /** The name its model and textures go by. */
    @Override
    public String getSerializedName() {
        return id;
    }

    public HardwareEra era() {
        return era;
    }

    /** What reaches its glass of what the machine draws. */
    public Tube tube() {
        return tube;
    }

    /** Whether it is a flat panel, which joins the panels beside it into one screen. */
    public boolean flat() {
        return flat;
    }

    /** Whether it is a picture tube, heard switching on and off. */
    public boolean tubeSounds() {
        return era.isAtMost(HardwareEra.LEGACY);
    }

    /** One edge of the glass, in sixty-fourths of the front: 0 left, 1 top, 2 right, 3 bottom. */
    public int glass(final int edge) {
        return glass[edge];
    }

    /** One edge of the power button, in sixty-fourths of the front, as {@link #glass}. */
    public int button(final int edge) {
        return button[edge];
    }

    /**
     * Whether a point of the front, in sixty-fourths from its top left as seen, is on the power button. A hair of
     * room round it, since the button stands out of the bezel and a click on its edge lands beside it.
     */
    public boolean onButton(final double x, final double y) {
        return x >= button[0] - 1 && x <= button[2] + 1 && y >= button[1] - 1 && y <= button[3] + 1;
    }
}
