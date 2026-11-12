/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The five faces of Quince's desktops (computers/docs/design/quince.md): the Quince System in black and white, Quince
 * OS 9 in Platinum, Quince OS X in Pulp on the Legacy and the Transition and flat from the Standard on, and quinceOS's
 * own. Unlike the rest of the mod they keep the rounded corners and the gel buttons of the systems they parody.
 *
 * <p>Each face is one palette, {@code jsc:quince/<face>}, which a resource pack can recolour; the shapes are the
 * chrome's ({@link QuinceChrome}) and the bars' ({@link QuinceBars}).
 */
@PaletteHolder
public enum QuinceLook {

    /** The Quince System: one bit, a checked grey desktop, striped titles and square-cornered menus. */
    /*
     * Each face's colours, eight to a line in the record's order: the menu bar and its menus; the window, its title
     * and its frame; the bevels and the buttons; the lists, the sidebar and the scrollbar; the desktop and the Dock.
     */

    /** The Quince System: one bit, a checked grey desktop, striped titles and square-cornered menus. */
    ONE_BIT(Palettes.declare(JsComputers.MODID, "quince/one_bit", new Colours(
            0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
            0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFFFFFFFF,
            0xFFFFFFFF, 0xFF000000, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF000000, 0xFF000000,
            0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF, 0xFF808080, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
            0xFF808080, 0x00000000, 0xFF000000, 0xFF000000))),

    /** Quince OS 9: Platinum's greys and bevels, pinstriped titles, the textured blue desktop. */
    PLATINUM(Palettes.declare(JsComputers.MODID, "quince/platinum", new Colours(
            0xFFDDDDDD, 0xFFDDDDDD, 0xFF555555, 0xFF000000, 0xFF3A3A9C, 0xFFFFFFFF, 0xFFDDDDDD, 0xFF555555,
            0xFF000000, 0xFFDDDDDD, 0xFFDDDDDD, 0xFFAAAAAA, 0xFF555555, 0xFF000000, 0xFF888888, 0xFFDDDDDD,
            0xFFFFFFFF, 0xFF888888, 0xFF555555, 0xFFE6E6E6, 0xFFE6E6E6, 0xFF555555, 0xFF000000, 0xFF3A3A9C,
            0xFFFFFFFF, 0xFF8A8A8A, 0xFFEEEEEE, 0xFF8A8A8A, 0xFFEEEEEE, 0xFFB8B8D8, 0xFFE6E6E6, 0xFFE6E6E6,
            0xFF4B5C9E, 0x00000000, 0xFFFFFFFF, 0xFF000000))),

    /** Quince OS X on the Legacy and the Transition: Pulp's gel, its brushed metal and its pinstripes. */
    PULP(Palettes.declare(JsComputers.MODID, "quince/pulp", new Colours(
            0xFFF7F7F7, 0xFFDADADA, 0xFF9A9A9A, 0xFF000000, 0xFF3D7FD8, 0xFFFFFFFF, 0xFFF7F7F7, 0xFF9A9A9A,
            0xFF000000, 0xFFEDEDED, 0xFFC2C2C2, 0xFFC4C4C4, 0xFF6A6A6A, 0xFF2A2A2A, 0xFF9A9A9A, 0xFFE8E8E8,
            0xFFFFFFFF, 0xFF8A8A8A, 0xFF6A6A6A, 0xFFFFFFFF, 0xFFD6D6D6, 0xFF2A5CA8, 0xFF000000, 0xFF3D7FD8,
            0xFFFFFFFF, 0xFF7A8696, 0xFFE4E8EE, 0xFF9A9A9A, 0xFFEEEEEE, 0xFF8FB8EE, 0xFF9ACDF8, 0xFF3D8CE0,
            0xFF174C9C, 0x73FFFFFF, 0xFFFFFFFF, 0xFF000000))),

    /** Quince OS X from the Standard on: flat lights, translucent bars, the grey shelf of the Dock. */
    FLAT(Palettes.declare(JsComputers.MODID, "quince/flat", new Colours(
            0xFFF2F2F4, 0xFFE8E8EC, 0xFFC0C0C8, 0xFF000000, 0xFF2A6FE0, 0xFFFFFFFF, 0xFFF2F2F4, 0xFFC0C0C8,
            0xFF000000, 0xFFECECEC, 0xFFDCDCDC, 0xFFDCDCDC, 0xFFB8B8BC, 0xFF2A2A2A, 0xFF9A9A9A, 0xFFF4F4F6,
            0xFFFFFFFF, 0xFFC8C8CC, 0xFFB8B8BC, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF2A6FE0, 0xFFFFFFFF, 0xFF2A6FE0,
            0xFFFFFFFF, 0xFF8A8E96, 0xFFE4E8EE, 0xFF9A9A9A, 0xFFF4F4F6, 0xFFC0C4CC, 0xFF3A82F0, 0xFF3A82F0,
            0xFF3A5A9A, 0x99E8E8EE, 0xFFFFFFFF, 0xFF3A3A3A))),

    /** quinceOS: its own, the corners round, the sidebar the window's full height, the Dock floating. */
    QUINCE_OS(Palettes.declare(JsComputers.MODID, "quince/quinceos", new Colours(
            0xFFE9E2F2, 0xFFE2DAEE, 0xFFC9BFD8, 0xFF000000, 0xFF6A5AE0, 0xFFFFFFFF, 0xFFF4F2F6, 0xFFC9BFD8,
            0xFF000000, 0xFFF4F2F6, 0xFFF4F2F6, 0xFFF4F2F6, 0xFFB8B8BC, 0xFF2A2A2A, 0xFF9A9A9A, 0xFFF4F2F6,
            0xFFFFFFFF, 0xFFC8C8CC, 0xFFB8B8BC, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF6A5AE0, 0xFFFFFFFF, 0xFF6A5AE0,
            0xFFFFFFFF, 0xFF8A84A0, 0xFFE6E1EE, 0xFF9A9A9A, 0xFFF4F2F6, 0xFFC8C0DA, 0xFF7A6AF0, 0xFF7A6AF0,
            0xFF8AA8F0, 0x6EFFFFFF, 0xFFFFFFFF, 0xFF3A3A3A)));

    /** How round each face's windows are at the corners; quinceOS is the one that rounds them. */
    private static final int QUINCE_OS_ROUND = 5;

    private final Palette<Colours> palette;

    QuinceLook(final Palette<Colours> palette) {
        this.palette = palette;
    }

    /**
     * The face of the Quince desktop under {@code desktopId} on hardware of {@code era}, or null when that is no
     * desktop of Quince's: Quince OS X and its Server wear Pulp up to the Transition and the flat face after it.
     */
    @Nullable
    public static QuinceLook of(@Nullable final ResourceLocation desktopId, @Nullable final Era era) {
        if (desktopId == null || !JsComputers.MODID.equals(desktopId.getNamespace())) {
            return null;
        }
        return switch (desktopId.getPath()) {
            case "quince_system" -> ONE_BIT;
            case "quince_os_9" -> PLATINUM;
            case "quince_os_x", "quince_os_x_server" ->
                    era != null && era.isAtMost(HardwareEra.TRANSITION) ? PULP : FLAT;
            case "quinceos" -> QUINCE_OS;
            default -> null;
        };
    }

    /**
     * The face of the Quince desktop under {@code desktopId} on the machine at {@code host}, by the era this game has
     * that machine at, or null when that is no desktop of Quince's.
     */
    @Nullable
    public static QuinceLook ofMachine(@Nullable final ResourceLocation desktopId, final BlockPos host) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Era era = minecraft.level != null && minecraft.level.getBlockEntity(host) instanceof IOsHost machine
                ? machine.displayEra() : null;
        return of(desktopId, era);
    }

    /** This face's colours, as its palette has them now. */
    public Colours colours() {
        return this.palette.get();
    }

    /** Whether this is one of the classic faces, with its menu bar alone and no Dock. */
    public boolean classic() {
        return this == ONE_BIT || this == PLATINUM;
    }

    /** How many pixels a window's corners are rounded by. */
    public int round() {
        return this == QUINCE_OS ? QUINCE_OS_ROUND : 0;
    }

    /** Whether a window's buttons are the three lights at its left, as on Quince OS X and after it. */
    public boolean lights() {
        return !classic();
    }

    /**
     * A face's colours.
     *
     * @param barTop        the menu bar at its top
     * @param barBottom     the menu bar at its foot
     * @param barLine       the line under the menu bar
     * @param barInk        what the menu bar is written in
     * @param hot           an open menu's title and a hovered item
     * @param hotInk        what is written on {@code hot}
     * @param menu          a pulled-down menu's ground
     * @param menuEdge      a pulled-down menu's border
     * @param ink           a window's text
     * @param titleTop      a title bar at its top
     * @param titleBottom   a title bar at its foot
     * @param stripe        the pinstripes of a classic title, or the metal of a Pulp window
     * @param frame         a window's outline
     * @param titleInk      a title in front
     * @param titleIdle     a title behind
     * @param face          a dialog's and a panel's ground
     * @param light         the lit edge of a bevel
     * @param shade         the shaded edge of a bevel
     * @param shadow        a bevel's darkest edge and a window's drop shadow
     * @param button        a push button at its top
     * @param buttonFoot    a push button at its foot
     * @param defaultRim    the rim of the default button
     * @param defaultInk    what the default button is written in
     * @param select        a selected row
     * @param selectInk     what a selected row is written in
     * @param heading       a sidebar's headings
     * @param sidebar       a window's sidebar
     * @param dim           a dimmed item
     * @param track         a scrollbar's track
     * @param thumb         a scrollbar's thumb
     * @param defaultTop    the default button at its top
     * @param defaultFoot   the default button at its foot
     * @param desktop       the desktop's own colour, under its picture
     * @param dock          the Dock's shelf, translucent
     * @param dockLine      the Dock's lit edge
     * @param running       the mark under a running program in the Dock
     */
    public record Colours(int barTop, int barBottom, int barLine, int barInk, int hot, int hotInk, int menu,
                          int menuEdge, int ink, int titleTop, int titleBottom, int stripe, int frame, int titleInk,
                          int titleIdle, int face, int light, int shade, int shadow, int button, int buttonFoot,
                          int defaultRim, int defaultInk, int select, int selectInk, int heading, int sidebar, int dim,
                          int track, int thumb, int defaultTop, int defaultFoot, int desktop, int dock, int dockLine,
                          int running) {
    }
}
