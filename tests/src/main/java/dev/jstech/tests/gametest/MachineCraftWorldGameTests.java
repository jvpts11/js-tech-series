/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The autocraft blocks drop what they are made of when broken, so a player never loses a crafting cable or a part on
 * it by mining it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineCraftWorldGameTests {

    private static final String ARENA = "empty";

    private MachineCraftWorldGameTests() {
    }

    /** A crafting cable with an interface, a router and a bus on it drops the cable and all three parts. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void craftingParts_dropWithTheirCable(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final CableBlockEntity cable = TestCables.lay(helper, pos, ComputingModule.CRAFTING_CABLE);
        final IFacePart face = new CraftingInterfacePart(HardwareEra.LEGACY);
        final IFacePart router = new CraftingRouterPart();
        final IFacePart bus = new ReceivingBusPart();
        cable.addPart(Direction.NORTH, face);
        cable.addPart(Direction.SOUTH, router);
        cable.addPart(Direction.UP, bus);
        helper.startSequence()
                .thenExecute(() -> helper.getLevel().destroyBlock(helper.absolutePos(pos), true))
                .thenExecuteAfter(2, () -> {
                    helper.assertItemEntityPresent(ComputingModule.CRAFTING_CABLE.asItem(), pos, 2.0);
                    helper.assertItemEntityPresent(face.partItem().getItem(), pos, 2.0);
                    helper.assertItemEntityPresent(router.partItem().getItem(), pos, 2.0);
                    helper.assertItemEntityPresent(bus.partItem().getItem(), pos, 2.0);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void craftingCable_dropsItsItemWhenBroken(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        TestCables.lay(helper, pos, ComputingModule.CRAFTING_CABLE);
        helper.startSequence()
                .thenExecute(() -> helper.getLevel().destroyBlock(helper.absolutePos(pos), true))
                .thenExecuteAfter(2, () -> helper.assertItemEntityPresent(
                        ComputingModule.CRAFTING_CABLE.asItem(), pos, 2.0))
                .thenSucceed();
    }
}
