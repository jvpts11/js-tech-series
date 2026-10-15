/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import static dev.jstech.core.machine.MachineScreenLayout.BANK_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.BANK_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.BANK_X;
import static dev.jstech.core.machine.MachineScreenLayout.BANK_Y;
import static dev.jstech.core.machine.MachineScreenLayout.BAY_PITCH;
import static dev.jstech.core.machine.MachineScreenLayout.BAY_X;
import static dev.jstech.core.machine.MachineScreenLayout.BAY_Y;
import static dev.jstech.core.machine.MachineScreenLayout.BAYS;
import static dev.jstech.core.machine.MachineScreenLayout.SLOT;

import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.machine.UpgradeEffect;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The Upgrades tab, as the approved design draws it: the machine's upgrade bays, each with its lamp lit while it holds
 * an upgrade, what they make of its speed and its energy, and a bank of LED bars, one an effect, its middle mark the
 * machine as it was built, lit to the right for more and to the left for less.
 */
public final class UpgradesTab implements IMachineFace {

    private static final int TITLE_X = 64;
    private static final int TITLE_Y = 32;
    private static final int SUMMARY_Y = 84;
    private static final int NO_BAYS_Y = 60;
    private static final int ROW_PITCH = 7;
    private static final int SEGMENTS = 9;
    private static final int SEGMENT_PITCH = 2;
    private static final int MIDDLE = 32;
    private static final double FULL_BAR = 0.5;
    private static final int FLICKER = 6;
    private static final int SUMMARY_ROOM = 104;

    @Override
    public void draw(final MachineView v) {
        final GuiGraphics g = v.g();
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        MachineArt.engrave(g, v.font(), MachineArt.fit(v.font(), MachineWords.upper(MachineScreenTexts.UPGRADE_BAYS),
                SUMMARY_ROOM), v.x(TITLE_X), v.y(TITLE_Y), false);
        final int bays = bays(v);
        if (bays == 0) {
            MachineArt.engrave(g, v.font(), MachineArt.fit(v.font(), MachineWords.upper(MachineScreenTexts.NO_BAYS),
                    SUMMARY_ROOM), v.x(TITLE_X), v.y(NO_BAYS_Y), false);
        }
        final ProcessingMachineBlockEntity processing = v.machine() instanceof ProcessingMachineBlockEntity each
                ? each : null;
        for (int k = 0; k < bays; k++) {
            final int bx = v.x(BAY_X + k * BAY_PITCH);
            final int by = v.y(BAY_Y);
            MachineArt.bezel(g, bx - 4, by - 4, bx + SLOT + 4, by + SLOT + 14, false);
            MachineArt.slot(g, bx, by);
            final boolean held = processing != null && !processing.getInventory()
                    .getStackInSlot(processing.layout().firstUpgrade() + k).isEmpty();
            MachineArt.lamp(g, bx + 6, by + SLOT + 3, held ? c.lampRun() : MachineArt.rgb(90, 90, 90), held);
        }
        final UpgradeEffect effect = processing != null ? processing.upgradeEffect() : UpgradeEffect.NONE;
        if (bays > 0) {
            MachineArt.engrave(g, v.font(), MachineArt.fit(v.font(), summary(effect), SUMMARY_ROOM), v.x(TITLE_X),
                    v.y(SUMMARY_Y), false);
        }
        bank(v, effect);
    }

    @Override
    public List<Component> tooltip(final MachineView v, final double x, final double y) {
        for (int k = 0; k < bays(v); k++) {
            if (MachineScreenLayout.on(x, y, BAY_X + k * BAY_PITCH, BAY_Y, SLOT, SLOT)) {
                return List.of(GameText.component(MachineScreenTexts.TIP_BAY));
            }
        }
        if (MachineScreenLayout.on(x, y, BANK_X - 3, BANK_Y - 3, BANK_WIDTH + 6, BANK_HEIGHT + 6)) {
            final UpgradeEffect effect = v.machine() instanceof ProcessingMachineBlockEntity processing
                    ? processing.upgradeEffect() : UpgradeEffect.NONE;
            return List.of(GameText.component(MachineScreenTexts.TIP_BANK), GameText.component(
                    MachineScreenTexts.SPEED_AND_ENERGY.with(times(effect.speed()), times(effect.energy()))));
        }
        return List.of();
    }

    /* The bays the machine shows, at most the three the tab holds. */
    private static int bays(final MachineView v) {
        return v.machine() instanceof ProcessingMachineBlockEntity processing
                ? Math.min(processing.layout().upgrades(), BAYS) : 0;
    }

    private static String summary(final UpgradeEffect effect) {
        return MachineWords.upper(MachineScreenTexts.SPEED_AND_ENERGY.with(times(effect.speed()),
                times(effect.energy())));
    }

    private static String times(final double factor) {
        return String.format(Locale.ROOT, "%.2f", factor);
    }

    /* The bank of bars: each effect's row, its word dim while nothing changes it. */
    private static void bank(final MachineView v, final UpgradeEffect effect) {
        final GuiGraphics g = v.g();
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final int x0 = v.x(BANK_X);
        final int y0 = v.y(BANK_Y);
        MachineArt.bezel(g, x0 - 3, y0 - 3, x0 + BANK_WIDTH + 3, y0 + BANK_HEIGHT + 3, false);
        MachineArt.fill(g, x0, y0, x0 + BANK_WIDTH, y0 + BANK_HEIGHT, MachineArt.rgb(20, 22, 26));
        final int mid = x0 + MIDDLE;
        MachineArt.vline(g, mid, y0 + 1, y0 + BANK_HEIGHT - 1, MachineArt.rgb(110, 110, 110));
        final List<Row> rows = List.of(new Row(MachineScreenTexts.SPD, effect.speed() - 1.0, c.lampRun()),
                new Row(MachineScreenTexts.NRG, effect.energy() - 1.0, c.lampWait()),
                new Row(MachineScreenTexts.PAR, 0.0, c.lampRun()), new Row(MachineScreenTexts.YLD, 0.0, c.lampRun()),
                new Row(MachineScreenTexts.BYP, 0.0, c.lampRun()), new Row(MachineScreenTexts.USE, 0.0, c.lampRun()),
                new Row(MachineScreenTexts.HEA, 0.0, c.lampRun()), new Row(MachineScreenTexts.SND, 0.0, c.lampRun()),
                new Row(MachineScreenTexts.BUF, 0.0, c.lampRun()));
        for (int k = 0; k < rows.size(); k++) {
            final Row row = rows.get(k);
            final int y = y0 + 2 + k * ROW_PITCH;
            final boolean moved = Math.abs(row.value()) > 1e-9;
            MachineArt.text(g, v.font(), MachineWords.upper(row.word()), x0 + 2, y,
                    moved ? MachineArt.rgb(150, 156, 164) : MachineArt.rgb(90, 96, 104));
            for (int seg = 0; seg < SEGMENTS; seg++) {
                MachineArt.vline(g, mid + 2 + seg * SEGMENT_PITCH, y, y + 5, MachineArt.rgb(40, 44, 50));
                MachineArt.vline(g, mid - 2 - seg * SEGMENT_PITCH, y, y + 5, MachineArt.rgb(40, 44, 50));
            }
            final int lit = (int) Math.round(Math.min(FULL_BAR, Math.abs(row.value())) / FULL_BAR * SEGMENTS);
            final int flick = moved && ((int) v.tick() + k) % FLICKER == 0 ? 1 : 0;
            for (int seg = 0; seg < Math.min(SEGMENTS, lit + flick); seg++) {
                final int x = row.value() > 0 ? mid + 2 + seg * SEGMENT_PITCH : mid - 2 - seg * SEGMENT_PITCH;
                MachineArt.vline(g, x, y, y + 5, row.colour());
            }
        }
    }

    /**
     * One row of the bank.
     *
     * @param word   its word
     * @param value  how far the effect moves the machine from how it was built
     * @param colour its bar's colour
     */
    private record Row(TextKey word, double value, int colour) {
    }
}
