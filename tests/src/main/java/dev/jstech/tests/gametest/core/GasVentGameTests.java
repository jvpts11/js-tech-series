/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.hazard.GasClouds;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.lines.GasVentBlockEntity;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.core.material.Material;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Gas Vent at a gas line's end: it takes a gas only at the inlet on its front, all of what it is given, and lets it
 * out above it as a cloud of the gas, saying what it lets out.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GasVentGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos VENT = new BlockPos(3, 2, 3);
    private static final long GIVEN = 500L;
    private static final int AFTER_A_SECOND = 25;
    /* How near the vent a cloud's centre is when it is the vent's: the arenas beside it are further. */
    private static final double NEAR = 3;

    private GasVentGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void vent_letsOutAsACloudTheGasItsFrontIsGiven(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(VENT, SharedPipes.GAS_VENT.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final BlockPos at = helper.absolutePos(VENT);
        final IMatterHandler front = level.getCapability(MatterCapabilities.GAS, at, Direction.NORTH);
        helper.assertTrue(front != null, "a gas line at its front feeds it");
        helper.assertTrue(level.getCapability(MatterCapabilities.GAS, at, Direction.EAST) == null
                && level.getCapability(MatterCapabilities.GAS, at, Direction.UP) == null, "and no other face");
        final Material chlorine = Elements.of(Element.CHLORINE);
        helper.assertTrue(front.insert(new MatterStack(chlorine, GIVEN), false) == GIVEN, "it takes all it is given");
        helper.runAfterDelay(AFTER_A_SECOND, () -> {
            final double share = GasClouds.shareAt(level, at.above(), chlorine);
            helper.assertTrue(share > 0, "the chlorine is let out above it as a cloud: " + share);
            final List<Text> lines = new ArrayList<>();
            ((GasVentBlockEntity) helper.getBlockEntity(VENT)).describe(lines, false);
            helper.assertTrue(lines.size() == 1 && lines.get(0).english().contains(GIVEN + " mB of Chlorine"),
                    "it says what it let out in the second: " + lines.stream().map(Text::english).toList());
            clear(level, at);
            helper.succeed();
        });
    }

    private static void clear(final ServerLevel level, final BlockPos at) {
        final Vec3 centre = Vec3.atCenterOf(at);
        GasClouds.STATE.set(level, new GasClouds.Book(GasClouds.of(level).stream()
                .filter(cloud -> cloud.centre().distanceTo(centre) > NEAR).toList()));
    }
}
