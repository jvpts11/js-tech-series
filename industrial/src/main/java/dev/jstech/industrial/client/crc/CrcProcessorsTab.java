/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.crc;

import static dev.jstech.industrial.crc.CrcLayout.BAY;
import static dev.jstech.industrial.crc.CrcLayout.BAY_Y;
import static dev.jstech.industrial.crc.CrcLayout.COOLANT_LAMP_X;
import static dev.jstech.industrial.crc.CrcLayout.COOLANT_LAMP_Y;
import static dev.jstech.industrial.crc.CrcLayout.COOLING_WORD_ROOM;
import static dev.jstech.industrial.crc.CrcLayout.DATA_BAY;
import static dev.jstech.industrial.crc.CrcLayout.DATA_LABEL_X;
import static dev.jstech.industrial.crc.CrcLayout.DATA_X;
import static dev.jstech.industrial.crc.CrcLayout.FACE_HEIGHT;
import static dev.jstech.industrial.crc.CrcLayout.FACE_WIDTH;
import static dev.jstech.industrial.crc.CrcLayout.FACE_X;
import static dev.jstech.industrial.crc.CrcLayout.FACE_Y;
import static dev.jstech.industrial.crc.CrcLayout.HEAT_PORT_LAMP_Y;
import static dev.jstech.industrial.crc.CrcLayout.LAMP_PITCH;
import static dev.jstech.industrial.crc.CrcLayout.LAMP_SIZE;
import static dev.jstech.industrial.crc.CrcLayout.LAMP_WORD_GAP;
import static dev.jstech.industrial.crc.CrcLayout.LAMP_WORD_ROOM;
import static dev.jstech.industrial.crc.CrcLayout.LAMP_X;
import static dev.jstech.industrial.crc.CrcLayout.LAMP_Y;
import static dev.jstech.industrial.crc.CrcLayout.READOUT_COUNTER_X;
import static dev.jstech.industrial.crc.CrcLayout.READOUT_DIGITS;
import static dev.jstech.industrial.crc.CrcLayout.READOUT_UNIT_X;
import static dev.jstech.industrial.crc.CrcLayout.READOUT_WORD_X;
import static dev.jstech.industrial.crc.CrcLayout.READOUT_WORD_Y;
import static dev.jstech.industrial.crc.CrcLayout.READOUT_Y;
import static dev.jstech.industrial.crc.CrcLayout.ROCKER_HEIGHT;
import static dev.jstech.industrial.crc.CrcLayout.ROCKER_WIDTH;
import static dev.jstech.industrial.crc.CrcLayout.ROCKER_WORD_X;
import static dev.jstech.industrial.crc.CrcLayout.ROCKER_X;
import static dev.jstech.industrial.crc.CrcLayout.ROCKER_Y;
import static dev.jstech.industrial.crc.CrcLayout.SECTIONS;
import static dev.jstech.industrial.crc.CrcLayout.SECTION_LABEL_Y;
import static dev.jstech.industrial.crc.CrcLayout.SLOT;
import static dev.jstech.industrial.crc.CrcLayout.THERMOMETER_BOTTOM;
import static dev.jstech.industrial.crc.CrcLayout.THERMOMETER_TOP;
import static dev.jstech.industrial.crc.CrcLayout.THERMOMETER_X;

import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.machine.MachineWords;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.crc.CrcBlockEntity;
import dev.jstech.industrial.crc.CrcLayout;
import dev.jstech.industrial.crc.CrcPart;
import dev.jstech.industrial.crc.CrcPartItem;
import dev.jstech.industrial.crc.CrcReadings;
import dev.jstech.industrial.crc.CrcScreenTexts;
import dev.jstech.industrial.research.IndustrialResearch;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Processors tab, as the approved design draws it: the CRC's name and class on their plates and the register of
 * bulbs beside them; its three sections of places, each a bay of nine; its data, a slot for each kind, the empty ones
 * showing the kind they take; the lamps of its coolant and its Heat Port; what it computes, draws and sheds on
 * mechanical counters; its thermometer; its switch; and the lamps RUN, WAIT and HOT. Past 90 degrees the part that
 * burns first is rimmed in red.
 */
final class CrcProcessorsTab implements ICrcTab {

    /* The numerals of the sections, as engraved. */
    private static final List<String> NUMERALS = List.of("I", "II", "III");
    /* The words on the counters' left, in their order. */
    private static final List<TextKey> READOUTS = List.of(CrcScreenTexts.RESEARCH_RATE, CrcScreenTexts.DRAW,
            CrcScreenTexts.SHED);
    private static final List<TextKey> READOUT_TIPS = List.of(CrcScreenTexts.TIP_RESEARCH, CrcScreenTexts.TIP_DRAW,
            CrcScreenTexts.TIP_SHED);
    /* An empty data slot shows the kind it takes, dimmed under the slot's grey. */
    private static final int GHOST_VEIL = 0xC0 << 24 | 0x8B8B8B;
    private static final double WATTS_A_KILOWATT = 1000.0;

    @Override
    public void draw(final CrcView view) {
        final GuiGraphics g = view.g();
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        CrcArt.face(g, view.x(FACE_X), view.y(FACE_Y), view.x(FACE_X + FACE_WIDTH), view.y(FACE_Y + FACE_HEIGHT));
        plates(view);
        lamps(view);
        for (int section = 0; section < SECTIONS; section++) {
            final int x = CrcLayout.sectionX(section);
            CrcArt.engrave(g, view.font(), MachineWords.upper(CrcScreenTexts.SECTION.with(NUMERALS.get(section))),
                    view.x(x + BAY / 2), view.y(SECTION_LABEL_Y), true);
            bay(view, x, BAY);
        }
        for (int i = 0; i < CrcBlockEntity.PLACES; i++) {
            final GuiLayout.SlotPosition at = CrcLayout.place(i);
            MachineArt.slot(g, view.x(at.x() - 1), view.y(at.y() - 1));
        }
        final int burnsFirst = view.hot() ? burnsFirst(view.menu()) : -1;
        if (burnsFirst >= 0) {
            final GuiLayout.SlotPosition at = CrcLayout.place(burnsFirst);
            MachineArt.frame(g, view.x(at.x() - 2), view.y(at.y() - 2), view.x(at.x() + SLOT - 2),
                    view.y(at.y() + SLOT - 2), c.red());
        }
        CrcArt.engrave(g, view.font(), MachineWords.upper(CrcScreenTexts.DATA), view.x(DATA_LABEL_X),
                view.y(SECTION_LABEL_Y), true);
        bay(view, DATA_X, DATA_BAY);
        data(view);
        final CrcReadings read = view.read();
        cooling(view, COOLANT_LAMP_Y, read.coolant(), CrcScreenTexts.COOLANT);
        cooling(view, HEAT_PORT_LAMP_Y, read.heatPort(), CrcScreenTexts.HEAT_PORT);
        final long[] values = {Math.round(read.points()), Math.round(read.draw() / WATTS_A_KILOWATT),
                Math.round(read.shed() / WATTS_A_KILOWATT)};
        final List<TextKey> units = List.of(CrcScreenTexts.PER_SECOND, CrcScreenTexts.KILOWATTS,
                CrcScreenTexts.KILOWATTS);
        for (int i = 0; i < READOUTS.size(); i++) {
            CrcArt.engrave(g, view.font(), MachineWords.upper(READOUTS.get(i)), view.x(READOUT_WORD_X[i]),
                    view.y(READOUT_WORD_Y), READOUT_COUNTER_X[i] - 3 - READOUT_WORD_X[i]);
            CrcArt.counter(g, view.font(), view.x(READOUT_COUNTER_X[i]), view.y(READOUT_Y),
                    CrcArt.digits(values[i], READOUT_DIGITS), i == 2 && read.made() > read.shed() + 1);
            CrcArt.engrave(g, view.font(), GameText.resolve(units.get(i)), view.x(READOUT_UNIT_X[i]),
                    view.y(READOUT_WORD_Y), false);
        }
        CrcArt.thermometer(g, view.font(), view.x(THERMOMETER_X), view.y(THERMOMETER_TOP),
                view.y(THERMOMETER_BOTTOM), view.celsius());
        MachineArt.rocker(g, view.x(ROCKER_X), view.y(ROCKER_Y), view.crc().control().on());
        CrcArt.engrave(g, view.font(), MachineWords.upper(MachineScreenTexts.ON), view.x(ROCKER_WORD_X),
                view.y(ROCKER_Y + 2), false);
        CrcArt.engrave(g, view.font(), MachineWords.upper(MachineScreenTexts.OFF), view.x(ROCKER_WORD_X),
                view.y(ROCKER_Y + 15), false);
    }

    @Override
    public boolean clicked(final CrcView view, final double x, final double y, final int button) {
        if (CrcLayout.on(x, y, ROCKER_X, ROCKER_Y, ROCKER_X + ROCKER_WIDTH, ROCKER_Y + ROCKER_HEIGHT)) {
            view.press().accept(MachineMenu.SWITCH);
            return true;
        }
        return false;
    }

    @Override
    public List<Component> tooltip(final CrcView view, final double x, final double y) {
        if (CrcLayout.on(x, y, THERMOMETER_X - 3, THERMOMETER_TOP - 3, CrcLayout.SCALE_X + 12,
                THERMOMETER_BOTTOM + 9)) {
            return List.of(GameText.component(CrcScreenTexts.TIP_TEMPERATURE.with(Math.round(view.celsius()))));
        }
        if (CrcLayout.on(x, y, LAMP_X - 1, LAMP_Y - 1, LAMP_X + LAMP_WORD_GAP + LAMP_WORD_ROOM, LAMP_Y
                + 3 * LAMP_PITCH)) {
            return List.of(GameText.component(MachineBlockEntity.STATUS_LINE.with(view.crc().status().text())));
        }
        for (int i = 0; i < READOUTS.size(); i++) {
            if (CrcLayout.on(x, y, READOUT_WORD_X[i], READOUT_Y - 1, READOUT_UNIT_X[i] + 12, READOUT_Y
                    + CrcLayout.COUNTER_HEIGHT + 1)) {
                return List.of(GameText.component(READOUT_TIPS.get(i)));
            }
        }
        if (CrcLayout.on(x, y, COOLANT_LAMP_X - 1, COOLANT_LAMP_Y - 1, COOLANT_LAMP_X + LAMP_WORD_GAP
                + COOLING_WORD_ROOM, COOLANT_LAMP_Y + LAMP_SIZE + 1)) {
            return List.of(GameText.component(CrcScreenTexts.TIP_COOLANT));
        }
        if (CrcLayout.on(x, y, COOLANT_LAMP_X - 1, HEAT_PORT_LAMP_Y - 1, COOLANT_LAMP_X + LAMP_WORD_GAP
                + COOLING_WORD_ROOM, HEAT_PORT_LAMP_Y + LAMP_SIZE + 1)) {
            return List.of(GameText.component(CrcScreenTexts.TIP_HEAT_PORT));
        }
        if (CrcLayout.on(x, y, ROCKER_X, ROCKER_Y, ROCKER_X + ROCKER_WIDTH, ROCKER_Y + ROCKER_HEIGHT)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_SWITCH.with((view.crc().control().on()
                    ? MachineScreenTexts.ON : MachineScreenTexts.OFF).text())));
        }
        return List.of();
    }

    /** The place whose part burns first if the CRC gets hotter: its hungriest processor, the first of two alike. */
    static int burnsFirst(final MachineMenu menu) {
        int worst = -1;
        double most = 0.0;
        for (int i = 0; i < CrcBlockEntity.PLACES; i++) {
            final Optional<CrcPart> part = partIn(menu, i);
            if (part.isPresent() && part.get().processor() && part.get().watts() > most) {
                worst = i;
                most = part.get().watts();
            }
        }
        return worst;
    }

    /* The CRC's name on its aluminium plate, its class on the yellow one, and its register after them. */
    private static void plates(final CrcView view) {
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final GuiGraphics g = view.g();
        final int name = MachineArt.plate(g, view.font(), view.x(CrcLayout.PLATE_X), view.y(CrcLayout.PLATE_Y),
                MachineArt.fit(view.font(), MachineWords.upper(GameText.of(view.title())),
                        CrcLayout.NAME_PLATE_MOST - CrcLayout.PLATE_PADDING), m.aluminium(), m.plateInk());
        final Optional<EnergyClass> port = view.crc().power().portClass();
        final String rating = port.map(each -> MachineWords.upper(each.ratingKey().map(TextKey::text)
                .orElse(each.name().text()))).orElse(MachineWords.upper(MachineScreenTexts.FE));
        final int classX = CrcLayout.PLATE_X + name + CrcLayout.PLATE_GAP;
        final int rated = MachineArt.plate(g, view.font(), view.x(classX), view.y(CrcLayout.PLATE_Y),
                MachineArt.fit(view.font(), rating, CrcLayout.CLASS_PLATE_MOST - CrcLayout.PLATE_PADDING),
                m.classPlate(), m.classInk());
        CrcArt.register(g, view.x(classX + rated + CrcLayout.REGISTER_GAP), view.y(CrcLayout.REGISTER_Y),
                CrcLayout.REGISTER_COLUMNS, CrcLayout.REGISTER_ROWS, view.tick(), view.crc().running());
    }

    /* RUN, WAIT and HOT: the first two by its state, the last by its heat; a fault leaves it waiting. */
    private static void lamps(final CrcView view) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final MachineStatus.Lamp lamp = view.crc().status().lamp();
        final List<TextKey> words = List.of(MachineScreenTexts.RUN, MachineScreenTexts.WAIT, CrcScreenTexts.HOT);
        final int[] colours = {c.lampRun(), c.lampWait(), c.lampHot()};
        final boolean[] lit = {lamp == MachineStatus.Lamp.RUN, lamp != MachineStatus.Lamp.RUN, view.hot()};
        for (int i = 0; i < words.size(); i++) {
            final int y = LAMP_Y + i * LAMP_PITCH;
            MachineArt.lamp(view.g(), view.x(LAMP_X), view.y(y), colours[i], lit[i]);
            CrcArt.engrave(view.g(), view.font(), MachineWords.upper(words.get(i)), view.x(LAMP_X + LAMP_WORD_GAP),
                    view.y(y + 1), LAMP_WORD_ROOM);
        }
    }

    /* A bay's bezel and the dark behind its slots. */
    private static void bay(final CrcView view, final int x, final int side) {
        MachineArt.bezel(view.g(), view.x(x), view.y(BAY_Y), view.x(x + side), view.y(BAY_Y + side), false);
        MachineArt.fill(view.g(), view.x(x + 3), view.y(BAY_Y + 3), view.x(x + side - 3), view.y(BAY_Y + side - 3),
                CrcArt.PALETTE.get().bay());
    }

    /* The data slots, each empty one showing the kind it takes, dimmed. */
    private static void data(final CrcView view) {
        final GuiGraphics g = view.g();
        final List<ItemStack> kinds = List.of(IndustrialResearch.SAMPLE_DATA.toStack(),
                IndustrialResearch.CRYSTAL_DATA.toStack(), IndustrialResearch.SPECTRAL_DATA.toStack(),
                IndustrialResearch.PARTICLE_DATA.toStack());
        for (int k = 0; k < CrcBlockEntity.DATA.size(); k++) {
            final GuiLayout.SlotPosition at = CrcLayout.data(k);
            MachineArt.slot(g, view.x(at.x() - 1), view.y(at.y() - 1));
            final Slot slot = view.menu().faceSlots().get(CrcBlockEntity.FIRST_DATA + k);
            if (!slot.hasItem() && k < kinds.size()) {
                g.renderFakeItem(kinds.get(k), view.x(at.x()), view.y(at.y()));
                g.fill(RenderType.guiGhostRecipeOverlay(), view.x(at.x()), view.y(at.y()), view.x(at.x() + 16),
                        view.y(at.y() + 16), GHOST_VEIL);
            }
        }
    }

    /* A lamp of a way the heat leaves, its word beside it. */
    private static void cooling(final CrcView view, final int y, final boolean lit, final TextKey word) {
        MachineArt.lamp(view.g(), view.x(COOLANT_LAMP_X), view.y(y), CrcArt.PALETTE.get().lampRun(), lit);
        CrcArt.engrave(view.g(), view.font(), MachineWords.upper(word), view.x(COOLANT_LAMP_X + LAMP_WORD_GAP),
                view.y(y + 1), COOLING_WORD_ROOM);
    }

    /* The part in place {@code index}, as the menu shows it. */
    private static Optional<CrcPart> partIn(final MachineMenu menu, final int index) {
        return menu.faceSlots().get(index).getItem().getItem() instanceof CrcPartItem item ? Optional.of(item.part())
                : Optional.empty();
    }
}
