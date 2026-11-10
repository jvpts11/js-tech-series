/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.item.AssembledTooltip;
import dev.jstech.computers.item.PartTooltips;
import dev.jstech.computers.os.BoardFirmware;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A computer and its board's firmware (computers/docs/design/survival.md): a board out of the chain is blank, and a
 * computer with one turns its fans and nothing more; a one-block computer carried as an item keeps its parts, its name
 * and whether it was never started, stands with them placed, and its tooltip lists them with what it still lacks.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class AssembledComputerGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);

    private AssembledComputerGameTests() {
    }

    @GameTest(template = ARENA)
    public static void blankBoard_turnsTheFansAndNothingMore(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        helper.assertTrue(pc.isRunning() && !pc.firmwareMissing(), "a board of the creative tab carries firmware");
        final ItemStackHandler hardware = pc.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                BoardFirmware.blanked(hardware.getStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT)));
        helper.assertTrue(pc.firmwareMissing(), "the board is blank");
        helper.assertTrue(pc.poweredUp(), "switched on, its fans turn");
        helper.assertTrue(!pc.isRunning(), "and nothing more comes up");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void board_firmwareIsItsErasUntilBlanked(final GameTestHelper helper) {
        final ItemStack board = new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get());
        helper.assertTrue(BoardFirmware.of(board).isPresent() && !BoardFirmware.blank(board), "it carries its era's");
        final ItemStack blank = BoardFirmware.blanked(board);
        helper.assertTrue(BoardFirmware.of(blank).isEmpty() && BoardFirmware.blank(blank), "blanked, it has none");
        helper.assertTrue(!BoardFirmware.blank(BoardFirmware.written(blank)), "written, it has its era's again");
        helper.assertTrue(keys(PartTooltips.lines(blank)).contains(BoardFirmware.WRITTEN_BY.key()),
                "a blank board's tooltip says where its firmware comes from");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void computer_asAnItem_standsAgainWithItsPartsAndName(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(AT);
        pc.setCustomName("Lab");
        final List<ItemStack> held = parts(pc.getHardware());
        final ItemStack whole = pc.asItem();
        final BlockPos again = AT.east(2);
        helper.setBlock(again, ComputingModule.PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity placed = (PersonalComputerBlockEntity) helper.getBlockEntity(again);
        placed.applyComponentsFromItemStack(whole);
        final List<ItemStack> standing = parts(placed.getHardware());
        helper.assertTrue(standing.size() == held.size(), "every part stands again: " + standing);
        for (int i = 0; i < held.size(); i++) {
            helper.assertTrue(ItemStack.isSameItem(held.get(i), standing.get(i)), "each in its seat: " + standing);
        }
        helper.assertTrue("Lab".equals(placed.customName()), "with its name: " + placed.customName());
        helper.assertTrue(!placed.neverStarted(), "a computer that ran is no longer one never started");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tooltip_listsThePartsAndWhatTheComputerLacks(final GameTestHelper helper) {
        final ItemStack computer = new ItemStack(ComputingModule.PERSONAL_COMPUTER.get());
        final List<ItemStack> parts = new ArrayList<>();
        parts.add(BoardFirmware.blanked(new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get())));
        parts.add(new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
        computer.set(ComputingComponents.ASSEMBLED_HARDWARE.get(), ItemContainerContents.fromItems(parts));
        final List<Component> lines = new ArrayList<>();
        AssembledTooltip.append(computer, lines);
        final List<String> keys = keys(lines);
        helper.assertTrue(keys.contains("jsc.assembled.board") && keys.contains("jsc.assembled.cpu"),
                "it lists its board and its processor: " + keys);
        helper.assertTrue(keys.contains("jsc.assembled.needs_preinstaller"), "it says it needs a Preinstaller");
        helper.succeed();
    }

    private static List<ItemStack> parts(final ItemStackHandler hardware) {
        final List<ItemStack> out = new ArrayList<>();
        for (int slot = 0; slot < hardware.getSlots(); slot++) {
            if (!hardware.getStackInSlot(slot).isEmpty()) {
                out.add(hardware.getStackInSlot(slot).copy());
            }
        }
        return out;
    }

    /* The keys of every sentence in {@code lines}, the sentences put inside them too. */
    private static List<String> keys(final List<Component> lines) {
        final List<String> out = new ArrayList<>();
        for (final Component line : lines) {
            collect(line, out);
        }
        return out;
    }

    private static void collect(final Component line, final List<String> out) {
        if (line.getContents() instanceof TranslatableContents translated) {
            out.add(translated.getKey());
            for (final Object arg : translated.getArgs()) {
                if (arg instanceof Component nested) {
                    collect(nested, out);
                }
            }
        }
        for (final Component sibling : line.getSiblings()) {
            collect(sibling, out);
        }
    }
}
