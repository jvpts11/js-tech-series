/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import static dev.jstech.core.machine.MachineScreenLayout.CLASS_PLATE_Y;
import static dev.jstech.core.machine.MachineScreenLayout.LAMP_PITCH;
import static dev.jstech.core.machine.MachineScreenLayout.LAMP_WORD_X;
import static dev.jstech.core.machine.MachineScreenLayout.LAMP_X;
import static dev.jstech.core.machine.MachineScreenLayout.LAMP_Y;
import static dev.jstech.core.machine.MachineScreenLayout.NAME_PLATE_MOST;
import static dev.jstech.core.machine.MachineScreenLayout.NAME_PLATE_Y;
import static dev.jstech.core.machine.MachineScreenLayout.PLATE_X;
import static dev.jstech.core.machine.MachineScreenLayout.TAB_ICON;
import static dev.jstech.core.machine.MachineScreenLayout.TABS;

import dev.jstech.core.JsCore;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * What every machine's screen draws round what its tab shows, whichever screen it is: a tab with its icon, the
 * machine's name and class on their plates, and its three lamps with their words. The standard machine screen draws
 * them, and so does a machine's own screen on the tabs it shares with every machine.
 */
public final class MachineChrome {

    /** The tabs' icons, in the order of the tabs: Main, Sides, Redstone, Upgrades, Protection. */
    public static final ResourceLocation TAB_ICONS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "textures/gui/machine/tabs.png");
    private static final List<TextKey> LAMP_WORDS = List.of(MachineScreenTexts.RUN, MachineScreenTexts.WAIT,
            MachineScreenTexts.FAULT);
    private static final int ICON = 16;
    /* What a plate takes besides its words: its screws and the room round them. */
    private static final int PLATE_MARGIN = 16;
    private static final int PLATE_TEXT_ROOM = NAME_PLATE_MOST - PLATE_MARGIN;
    private static final int LAMP_WORD_ROOM = 38;

    private MachineChrome() {
    }

    /** The tab at {@code x, y}, raised when it is the one shown, with the icon of the machine's tab {@code index}. */
    public static void tab(final GuiGraphics g, final int x, final int y, final int index, final boolean active) {
        MachineArt.tab(g, x, y, active);
        g.blit(TAB_ICONS, x + TAB_ICON, y + TAB_ICON, index * ICON, 0, ICON, ICON, ICON * TABS, ICON);
    }

    /** A tab of a machine's own at {@code x, y}, raised when it is the one shown, with its icon drawn as an item. */
    public static void tab(final GuiGraphics g, final int x, final int y, final ItemStack icon, final boolean active) {
        MachineArt.tab(g, x, y, active);
        g.renderItem(icon, x + TAB_ICON, y + TAB_ICON);
    }

    /** The machine's name on its aluminium plate, and its class, or FE, on the yellow one. */
    public static void plates(final MachineView view, final Component title) {
        plates(view, MachineWords.upper(GameText.of(title)), NAME_PLATE_MOST);
    }

    /**
     * The same, the name written as {@code name}, its plate at most {@code most} wide, as a face of the machine's own
     * gives it room.
     */
    public static void plates(final MachineView view, final String name, final int most) {
        plates(view.g(), view.font(), view.x(0), view.y(0), name, most, rating(view.machine()));
    }

    /**
     * The plates of a face whose top left stands at ({@code x}, {@code y}) on the screen: {@code name} on the aluminium
     * one, at most {@code most} wide, and {@code rating} on the yellow one; for a face shown with no machine behind it,
     * as a recipe viewer's page shows one.
     */
    public static void plates(final GuiGraphics g, final Font font, final int x, final int y, final String name,
                              final int most, final String rating) {
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        MachineArt.plate(g, font, x + PLATE_X, y + NAME_PLATE_Y, MachineArt.fit(font, name, most - PLATE_MARGIN),
                c.aluminium(), c.plateInk());
        MachineArt.plate(g, font, x + PLATE_X, y + CLASS_PLATE_Y, MachineArt.fit(font, rating, PLATE_TEXT_ROOM),
                c.classPlate(), c.classInk());
    }

    /** What a machine's class plate says: its energy port's class, or FE. */
    public static String rating(final MachineBlockEntity machine) {
        final Optional<EnergyClass> port = machine.power().portClass();
        return port.map(each -> MachineWords.upper(each.ratingKey().map(TextKey::text).orElse(each.name().text())))
                .orElse(MachineWords.upper(MachineScreenTexts.FE));
    }

    /** The three lamps, the one of the machine's state lit, their words engraved beside them. */
    public static void lamps(final MachineView view) {
        lamps(view.g(), view.font(), view.x(0), view.y(0), view.machine().status().lamp());
    }

    /** The three lamps of a face whose top left stands at ({@code x}, {@code y}) on the screen, {@code lit} lit. */
    public static void lamps(final GuiGraphics g, final Font font, final int x, final int y,
                             final MachineStatus.Lamp lit) {
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final int[] colours = {c.lampRun(), c.lampWait(), c.lampFault()};
        final List<MachineStatus.Lamp> lamps = List.of(MachineStatus.Lamp.RUN, MachineStatus.Lamp.WAIT,
                MachineStatus.Lamp.FAULT);
        for (int i = 0; i < lamps.size(); i++) {
            MachineArt.lamp(g, x + LAMP_X, y + LAMP_Y + i * LAMP_PITCH, colours[i], lamps.get(i) == lit);
            MachineArt.engrave(g, font, MachineArt.fit(font, MachineWords.upper(LAMP_WORDS.get(i)), LAMP_WORD_ROOM),
                    x + LAMP_WORD_X, y + LAMP_Y + 1 + i * LAMP_PITCH, false);
        }
    }

    /** How many lamps a machine's face has. */
    public static int lampCount() {
        return LAMP_WORDS.size();
    }
}
