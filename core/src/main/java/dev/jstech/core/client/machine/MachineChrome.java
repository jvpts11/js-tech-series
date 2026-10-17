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
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
    private static final int PLATE_TEXT_ROOM = NAME_PLATE_MOST - 16;
    private static final int LAMP_WORD_ROOM = 38;

    private MachineChrome() {
    }

    /** The tab at {@code x, y}, raised when it is the one shown, with the icon of the machine's tab {@code index}. */
    public static void tab(final GuiGraphics g, final int x, final int y, final int index, final boolean active) {
        MachineArt.tab(g, x, y, active);
        g.blit(TAB_ICONS, x + TAB_ICON, y + TAB_ICON, index * ICON, 0, ICON, ICON, ICON * TABS, ICON);
    }

    /** The machine's name on its aluminium plate, and its class, or FE, on the yellow one. */
    public static void plates(final MachineView view, final Component title) {
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final GuiGraphics g = view.g();
        MachineArt.plate(g, view.font(), view.x(PLATE_X), view.y(NAME_PLATE_Y), MachineArt.fit(view.font(),
                MachineWords.upper(GameText.of(title)), PLATE_TEXT_ROOM), c.aluminium(), c.plateInk());
        final Optional<EnergyClass> port = view.machine().power().portClass();
        final String rating = port.map(each -> MachineWords.upper(each.ratingKey().map(TextKey::text)
                .orElse(each.name().text()))).orElse(MachineWords.upper(MachineScreenTexts.FE));
        MachineArt.plate(g, view.font(), view.x(PLATE_X), view.y(CLASS_PLATE_Y), MachineArt.fit(view.font(),
                rating, PLATE_TEXT_ROOM), c.classPlate(), c.classInk());
    }

    /** The three lamps, the one of the machine's state lit, their words engraved beside them. */
    public static void lamps(final MachineView view) {
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final MachineStatus.Lamp lit = view.machine().status().lamp();
        final int[] colours = {c.lampRun(), c.lampWait(), c.lampFault()};
        final List<MachineStatus.Lamp> lamps = List.of(MachineStatus.Lamp.RUN, MachineStatus.Lamp.WAIT,
                MachineStatus.Lamp.FAULT);
        for (int i = 0; i < lamps.size(); i++) {
            MachineArt.lamp(view.g(), view.x(LAMP_X), view.y(LAMP_Y + i * LAMP_PITCH), colours[i],
                    lamps.get(i) == lit);
            MachineArt.engrave(view.g(), view.font(), MachineArt.fit(view.font(), MachineWords.upper(
                    LAMP_WORDS.get(i)), LAMP_WORD_ROOM), view.x(LAMP_WORD_X), view.y(LAMP_Y + 1 + i * LAMP_PITCH),
                    false);
        }
    }

    /** How many lamps a machine's face has. */
    public static int lampCount() {
        return LAMP_WORDS.size();
    }
}
