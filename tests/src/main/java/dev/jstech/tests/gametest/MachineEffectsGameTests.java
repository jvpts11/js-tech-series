/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.hardware.ExperienceIndex;
import dev.jstech.computers.operation.payload.desktop.MachineEffects;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Whether a desktop runs its effects is a question of its graphics, asked only of the desktops that drew them on the
 * card: Frames 7 on the graphics of its processor drops to Basic, KDE 4 on a weak card loses its effects, and GNOME 2,
 * which drew everything on the processor, looks the same on any card.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineEffectsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    /** The card slot of a crafting computer, after its crafting card. */
    private static final int CARD_SLOT = CraftingComputerBlockEntity.PCIE_SLOTS_START + 1;

    private MachineEffectsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void frames7_keepsItsEffectsOnACardOfItsAge(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningCraftingComputer(COMPUTER);
        computer.installOs(system("frames_7"));
        helper.assertFalse(MachineEffects.of(computer, computer.console().settings()).basic(),
                "Frames 7 on a Radeon HD 7970 runs its effects");
        helper.assertTrue(MachineEffects.indexOf(computer).graphics() == ExperienceIndex.HIGHEST,
                "the card scores the top of the scale; got " + MachineEffects.indexOf(computer));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void frames7_runsBasicOnTheGraphicsOfItsProcessor(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningCraftingComputer(COMPUTER);
        computer.getHardware().setStackInSlot(CARD_SLOT, ItemStack.EMPTY);
        computer.installOs(system("frames_7"));
        helper.assertTrue(MachineEffects.of(computer, computer.console().settings()).basic(),
                "Frames 7 on the processor's graphics runs Basic");
        helper.assertFalse(MachineEffects.indexOf(computer).runsEffects(),
                "the processor's graphics score under the effects; got " + MachineEffects.indexOf(computer));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void kde4_losesItsEffectsOnAWeakCardAndGnome2KeepsItsLook(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final CraftingComputerBlockEntity kde = world.placeRunningTransitionCraftingComputer(COMPUTER,
                system("ubuntu"));
        kde.getHardware().setStackInSlot(CARD_SLOT, new ItemStack(HardwareItems.GPU_RADIANCE_X1300.get()));
        kde.console().install("jsc:kde_plasma");
        helper.assertTrue(MachineEffects.of(kde, kde.console().settings()).basic(),
                "KDE 4 on a Radeon X1300 runs without its effects; index " + MachineEffects.indexOf(kde));
        final CraftingComputerBlockEntity gnome = world.placeRunningTransitionCraftingComputer(COMPUTER.east(3),
                system("ubuntu"));
        gnome.getHardware().setStackInSlot(CARD_SLOT, new ItemStack(HardwareItems.GPU_RADIANCE_X1300.get()));
        gnome.console().install("jsc:gnome");
        helper.assertFalse(MachineEffects.of(gnome, gnome.console().settings()).basic(),
                "GNOME 2 drew on the processor and looks the same on any card");
        helper.succeed();
    }

    private static ResourceLocation system(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, name);
    }
}
