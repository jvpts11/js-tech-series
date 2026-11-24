/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.chain.machine.ChainFaceWord;
import dev.jstech.computers.chain.machine.ChainFaces;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.printer.LargeScalePrinterBlockEntity;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.FaceSlot;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.material.MatterState;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The chain machines' own faces (computers/docs/design/survival.md) as the machines are: each face's ports show every
 * item slot of its machine once, inside the face and none over another, so the menu places every slot where the
 * picture lets it in; each sight glass shows a tank the machine has, no two the same; and each word the face writes is
 * one a player reads in their language, or data (a number, a unit, a formula).
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChainFacesGameTests {

    private static final String ARENA = "empty";
    private static final int SLOT = 18;
    /* What a face writes as it is: numbers and dashes, and the readings the game puts in. */
    private static final Pattern NUMBER = Pattern.compile("[-0-9.%]*|\\{[a-z]+}");
    /* The units, symbols and formulas the faces write, which no language translates. */
    private static final Set<String> SYMBOLS = Set.of("A", "C", "V", "Ar", "ArF", "Cl2", "EUV", "FeCl3", "H2", "H2O",
            "HCl", "He", "Hg", "Kr", "KrF", "MPa", "N2", "O2", "Sn", "TCS", "cN", "kA", "kHz", "mTorr", "um");

    private ChainFacesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void faces_showEverySlotOnceInsideTheFace(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ChainMachine machine : ChainMachines.all()) {
            if (ChainFaces.of(machine).isEmpty()) {
                continue;
            }
            final List<FaceSlot> slots = ChainFaces.slots(machine);
            final List<Integer> bins = ChainFaces.of(machine).get().tray().map(FacePicture.Tray::slots)
                    .orElse(List.of());
            final Set<Integer> shown = new HashSet<>();
            for (final FaceSlot slot : slots) {
                if (!shown.add(slot.index())) {
                    wrong.add(machine.id() + " shows slot " + slot.index() + " twice");
                }
                // A bin of the row under the window stands in that row, where the screen draws it, not on the face.
                if (bins.contains(slot.index())) {
                    final GuiLayout.SlotPosition at = MachineScreenLayout.traySlot(bins.indexOf(slot.index()));
                    if (slot.x() != at.x() || slot.y() != at.y()) {
                        wrong.add(machine.id() + "'s bin " + slot.index() + " stands outside its row");
                    }
                } else if (slot.x() - 1 < MachineScreenLayout.FACE_X || slot.y() - 1 < MachineScreenLayout.FACE_Y
                        || slot.x() - 1 + SLOT > MachineScreenLayout.FACE_X + MachineScreenLayout.FACE_WIDTH
                        || slot.y() - 1 + SLOT > MachineScreenLayout.FACE_Y + MachineScreenLayout.FACE_HEIGHT) {
                    wrong.add(machine.id() + "'s slot " + slot.index() + " stands outside its face");
                }
                for (final FaceSlot other : slots) {
                    if (other != slot && Math.abs(other.x() - slot.x()) < SLOT && Math.abs(other.y() - slot.y()) < SLOT
                            && other.index() > slot.index()) {
                        wrong.add(machine.id() + "'s slots " + slot.index() + " and " + other.index() + " overlap");
                    }
                }
            }
            final int items = machine.layout().firstUpgrade();
            for (int index = 0; index < items; index++) {
                if (!shown.contains(index) && !fedByItsInlet(machine, index)) {
                    wrong.add(machine.id() + " shows no port for slot " + index);
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), "every face shows its machine's slots: " + wrong);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faces_showOnlyTanksTheMachineHas(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ChainMachine machine : ChainMachines.all()) {
            for (final Optional<FacePicture> picture : List.of(ChainFaces.of(machine),
                    machine.id().equals("optical_polisher") ? ChainFaces.of(ChainFaces.POLISHER_FIBRE)
                            : Optional.<FacePicture>empty())) {
                if (picture.isEmpty()) {
                    continue;
                }
                final int tanks = tanks(machine);
                final Set<Integer> shown = new HashSet<>();
                final Set<String> liquids = new HashSet<>();
                for (final FacePicture.Glass glass : picture.get().glasses()) {
                    // A glass of a liquid shows it in whichever tank of its half (in, or out) holds it: a tank of
                    // that half and a liquid no other glass of the half shows; any other glass a tank of its own.
                    final boolean own = glass.fluid().map(fluid -> liquids.add(fluid + "@"
                            + (glass.tank() < machine.layout().inputTanks() ? "in" : "out")))
                            .orElseGet(() -> shown.add(glass.tank()));
                    if (glass.tank() < 0 || glass.tank() >= tanks || !own) {
                        wrong.add(machine.id() + "'s glass of tank " + glass.tank() + " (it has " + tanks + ")");
                    }
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), "every glass shows a tank of its own: " + wrong);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faces_writeOnlyWordsAPlayerReadsInTheirLanguage(final GameTestHelper helper) {
        final List<String> untranslated = new ArrayList<>();
        for (final ChainMachine machine : ChainMachines.all()) {
            for (final Optional<FacePicture> picture : List.of(ChainFaces.of(machine),
                    ChainFaces.of(ChainFaces.POLISHER_FIBRE))) {
                if (picture.isEmpty()) {
                    continue;
                }
                picture.get().plate().filter(plate -> ChainFaceWord.of(plate).isEmpty())
                        .ifPresent(plate -> untranslated.add(machine.id() + ": " + plate));
                for (final FacePicture.Word word : picture.get().words()) {
                    for (final String part : word.text().trim().split(" +")) {
                        if (ChainFaceWord.of(part).isEmpty() && !SYMBOLS.contains(part)
                                && !NUMBER.matcher(part).matches()) {
                            untranslated.add(machine.id() + ": " + part);
                        }
                    }
                }
            }
        }
        helper.assertTrue(untranslated.isEmpty(), "every word on a face is translated or data: " + untranslated);
        helper.succeed();
    }

    /* Whether slot {@code index} of {@code machine} is one its inlet fills, no port of its own: the printer's feeder. */
    private static boolean fedByItsInlet(final ChainMachine machine, final int index) {
        return machine.id().equals("large_scale_printer") && index > 0
                && index < LargeScalePrinterBlockEntity.INPUTS;
    }

    /* How many tanks {@code machine} has: its liquid tanks, then each state of matter's. */
    private static int tanks(final ChainMachine machine) {
        final ChainMachineBlockEntity made = ChainMachines.typeOf(machine).create(BlockPos.ZERO,
                ChainMachines.terminalOf(machine).get().defaultBlockState());
        if (made == null) {
            throw new IllegalStateException("no terminal for " + machine.id());
        }
        int count = made.layout().inputTanks() + made.layout().outputTanks();
        for (final MatterState state : MatterState.values()) {
            count += made.matter().inputs(state).size() + made.matter().outputs(state).size();
        }
        return count;
    }
}
