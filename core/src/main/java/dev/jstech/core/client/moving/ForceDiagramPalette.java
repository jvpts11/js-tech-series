/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.JsCore;
import dev.jstech.core.moving.physics.ForceSum;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the diagram of forces of a moving structure: {@code jscore:structure_forces}. */
@PaletteHolder
final class ForceDiagramPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "structure_forces", new Colours(
            0xFF4A6CA8, 0xFFFFFFFF, 0xC01C2230, 0xFF0E1218, 0xFF6E9BD8, 0xFFB4D0F0, 0xFF5E9A3A, 0xFF4E8430,
            0xE0283444, 0xFF34425A, 0xFFE8ECF2, 0xFFA8B4C4, 0xFF687282, 0xFF161C26, 0xFF3FA040, 0xFF8A9098,
            0xFF4A4E56, 0xFF14181E, 0xFFC8CED8, 0xFFF4E04A, 0xFF5FE07A, 0xFFF06A5A, 0xFF38383C, 0xD0101418,
            0xFFF04848, 0xFF50E050, 0xFFF0A020, 0xFF50A0F0, 0xFFE0D040, 0xFFB070F0, 0xFFB0B8C8, 0xFFA07A48,
            0xFF40E0E0, 0xFFFFFFFF, 0xFF101010, 0xFFFFE040, 0xFF5FE07A, 0xFFF06A5A));

    private ForceDiagramPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The diagram's colours.
     *
     * @param titleBar   the bar across the top
     * @param title      the title on it
     * @param body       the ground of the whole diagram, over the world behind it
     * @param frame      the edge round the view and the tiles
     * @param skyTop     the sky behind the structure, at the top of the view
     * @param skyBottom  the sky at the foot of the view
     * @param ground     the ground below the structure
     * @param grid       the grid on the ground
     * @param list       the ground of the list of the forces
     * @param row        a row of the list under the pointer
     * @param text       a force's name and value, and the tiles' numbers
     * @param dim        the list's hint, the tiles' names and units, and a hidden force's name
     * @param off        a force that gives nothing now
     * @param barBack    the empty part of a force's bar
     * @param switchOn   a switch turned on
     * @param switchOff  a switch turned off
     * @param knob       a switch's knob
     * @param tile       a tile's ground
     * @param tileEdge   a tile's edge
     * @param sumNumber  the number of the sum, in its tile
     * @param rising     a climb upward, in its tile
     * @param falling    a climb downward, in its tile
     * @param foot       the bar along the foot
     * @param label      the ground of an arrow's label
     * @param gravity    the arrow, label, icon and bar of gravity
     * @param buoyancy   of buoyancy, balloons with it
     * @param thrust     of thrust
     * @param drag       of drag
     * @param lift       of lift
     * @param levitation of levitation
     * @param magnetism  of magnetism
     * @param wheels     of what the wheels and the ground give
     * @param held       of what holds it from outside, as a physics tool
     * @param sum        the arrow of the sum, and the mark of the centre of mass
     * @param mark       the dark half of the mark of the centre of mass, and the edge round every arrow
     * @param chosen     the edge round the button of the view chosen
     * @param axisUp     the axes' arrow up
     * @param axisAhead  the axes' arrow ahead, where the structure's bow looks
     */
    record Colours(int titleBar, int title, int body, int frame, int skyTop, int skyBottom, int ground, int grid,
                   int list, int row, int text, int dim, int off, int barBack, int switchOn, int switchOff, int knob,
                   int tile, int tileEdge, int sumNumber, int rising, int falling, int foot, int label, int gravity,
                   int buoyancy, int thrust, int drag, int lift, int levitation, int magnetism, int wheels, int held,
                   int sum, int mark, int chosen, int axisUp, int axisAhead) {

        /** The colour a kind of force is drawn in. */
        int of(final ForceSum.ForceKind kind) {
            return switch (kind) {
                case GRAVITY -> gravity;
                case BUOYANCY, BALLOONS -> buoyancy;
                case THRUST -> thrust;
                case DRAG -> drag;
                case LIFT -> lift;
                case LEVITATION -> levitation;
                case MAGNETISM -> magnetism;
                case WHEELS -> wheels;
                case HELD -> held;
            };
        }
    }
}
