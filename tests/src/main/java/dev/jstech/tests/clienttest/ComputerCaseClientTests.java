/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.content.BlockEntry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * The small computers' cases in every age: the Personal Computer, the Crafting Computer and the Cluster Management
 * Computer side by side, and from the Standard age on the three cases each comes in, Neutral, High Performance and
 * Aesthetic, one above the other. Each age is seen from the front and from the side that comes off; every case fills
 * its block.
 */
public final class ComputerCaseClientTests {

    private static final int SETTLE = 6;
    /** The row seen from the front, facing south, and the row turned to show its side, facing west. */
    private static final int FRONT_Z = 10;
    private static final int SIDE_Z = 6;
    /** How far from the cases the camera stands, and how far apart the ages are. */
    private static final int AWAY = 5;
    private static final int AGE_SPACING = 8;

    /** The ages, each its rows of cases from the floor up: in a row the three machines, in one case. */
    private static final List<Age> AGES = List.of(
            new Age("vintage", List.of(List.of(ComputingModule.VINTAGE_PERSONAL_COMPUTER,
                    ComputingModule.VINTAGE_CRAFTING_COMPUTER, ComputingModule.VINTAGE_CLUSTER_MANAGEMENT_COMPUTER))),
            new Age("legacy", List.of(List.of(ComputingModule.LEGACY_PERSONAL_COMPUTER,
                    ComputingModule.LEGACY_CRAFTING_COMPUTER, ComputingModule.LEGACY_CLUSTER_MANAGEMENT_COMPUTER))),
            new Age("transition", List.of(List.of(ComputingModule.TRANSITION_PERSONAL_COMPUTER,
                    ComputingModule.TRANSITION_CRAFTING_COMPUTER,
                    ComputingModule.TRANSITION_CLUSTER_MANAGEMENT_COMPUTER))),
            new Age("standard", List.of(
                    List.of(ComputingModule.PERSONAL_COMPUTER, ComputingModule.CRAFTING_COMPUTER,
                            ComputingModule.CLUSTER_MANAGEMENT_COMPUTER),
                    List.of(ComputingModule.HIGH_PERFORMANCE_PERSONAL_COMPUTER,
                            ComputingModule.HIGH_PERFORMANCE_CRAFTING_COMPUTER,
                            ComputingModule.HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER),
                    List.of(ComputingModule.AESTHETIC_PERSONAL_COMPUTER, ComputingModule.AESTHETIC_CRAFTING_COMPUTER,
                            ComputingModule.AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER))),
            new Age("advanced", List.of(
                    List.of(ComputingModule.ADVANCED_PERSONAL_COMPUTER, ComputingModule.ADVANCED_CRAFTING_COMPUTER,
                            ComputingModule.ADVANCED_CLUSTER_MANAGEMENT_COMPUTER),
                    List.of(ComputingModule.ADVANCED_HIGH_PERFORMANCE_PERSONAL_COMPUTER,
                            ComputingModule.ADVANCED_HIGH_PERFORMANCE_CRAFTING_COMPUTER,
                            ComputingModule.ADVANCED_HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER),
                    List.of(ComputingModule.ADVANCED_AESTHETIC_PERSONAL_COMPUTER,
                            ComputingModule.ADVANCED_AESTHETIC_CRAFTING_COMPUTER,
                            ComputingModule.ADVANCED_AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER))));

    private ComputerCaseClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void computerCases_fillTheirBlockInEveryAge(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            for (int age = 0; age < AGES.size(); age++) {
                final List<List<BlockEntry<?>>> rows = AGES.get(age).rows();
                for (int row = 0; row < rows.size(); row++) {
                    for (int machine = 0; machine < rows.get(row).size(); machine++) {
                        final var block = rows.get(row).get(machine).get();
                        final int x = age * AGE_SPACING + machine;
                        world.setBlock(new BlockPos(x, 2 + row, FRONT_Z), block.defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                        world.setBlock(new BlockPos(x, 2 + row, SIDE_Z), block.defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                    }
                }
            }
        });
        for (int age = 0; age < AGES.size(); age++) {
            final int middle = age * AGE_SPACING + 1;
            ctx.thenTeleport(SETTLE, new BlockPos(middle, 2, FRONT_Z + AWAY), Direction.NORTH)
                    .thenScreenshot(SETTLE, AGES.get(age).name() + "-front")
                    .thenTeleport(SETTLE, new BlockPos(middle, 2, SIDE_Z - AWAY), Direction.SOUTH)
                    .thenScreenshot(SETTLE, AGES.get(age).name() + "-side");
        }
    }

    /** An age's cases, row by row from the floor up. */
    private record Age(String name, List<List<BlockEntry<?>>> rows) {
    }
}
