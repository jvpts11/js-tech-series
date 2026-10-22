/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.hazard.ItemHazards;
import dev.jstech.core.hazard.Pollution;
import dev.jstech.core.hazard.PollutionCleaner;
import dev.jstech.core.jstechseries.hazard.IonizingRadiation;
import dev.jstech.core.jstechseries.hazard.SeriesPollution;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Shielded containers and the cleaning of pollution: a radioactive thing carried loose gives its carrier its whole
 * rate, in an ordinary box all of it still, and in a box with lead walls five centimetres thick a thirtieth of it, as
 * a block of lead does; a cleaner takes up to its rate a second from its chunk's pollution and keeps what it took, and
 * never more than lay there. The test mod makes echo shards radioactive and the black shulker box lead-walled.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShieldingGameTests {

    private static final String ARENA = "empty";
    /* The test mod's echo shard gives off ten millisieverts an hour at a block; four of them. */
    private static final double SHARDS = 40;
    private static final int TICKS_A_SECOND = 20;
    private static final BlockPos SCRUBBER = new BlockPos(4, 2, 4);

    private ShieldingGameTests() {
    }

    @GameTest(template = ARENA)
    public static void shielding_aLeadWalledBoxStopsNearlyAllOfWhatItHolds(final GameTestHelper helper) {
        final Player carrier = helper.makeMockPlayer(GameType.SURVIVAL);
        carrier.getInventory().items.set(0, new ItemStack(Items.ECHO_SHARD, 4));
        helper.assertTrue(Math.abs(rays(carrier) - SHARDS) < 1.0e-6, "loose, the shards give their whole rate: "
                + rays(carrier));
        carrier.getInventory().items.set(0, boxed(Items.WHITE_SHULKER_BOX));
        helper.assertTrue(Math.abs(rays(carrier) - SHARDS) < 1.0e-6, "an ordinary box shields nothing: "
                + rays(carrier));
        helper.assertTrue(ItemHazards.carried(carrier, ItemHazards.RADIOACTIVE) == 0,
                "though nothing loose is left to leave its dust");
        carrier.getInventory().items.set(0, boxed(Items.BLACK_SHULKER_BOX));
        final double lead = SHARDS * Math.exp(-0.0003 * 11_340);
        helper.assertTrue(Math.abs(rays(carrier) - lead) < 1.0e-6, "five centimetres of lead let a thirtieth through, "
                + "as a block of lead does: " + rays(carrier));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cleaner_takesUpToItsRateASecondAndNeverMoreThanLay(final GameTestHelper helper) {
        final BlockPos at = helper.absolutePos(SCRUBBER);
        final ChunkPos chunk = new ChunkPos(at);
        final double before = Pollution.at(helper.getLevel(), chunk, SeriesPollution.SOOT);
        Pollution.emit(helper.getLevel(), at, SeriesPollution.SOOT, 10);
        final double laid = Pollution.at(helper.getLevel(), chunk, SeriesPollution.SOOT);
        final PollutionCleaner cleaner = new PollutionCleaner(SeriesPollution.SOOT, 3);
        double first = 0;
        for (int tick = 0; tick < TICKS_A_SECOND; tick++) {
            first += cleaner.tick(helper.getLevel(), at);
        }
        helper.assertTrue(Math.abs(first - 3) < 1.0e-3 && Math.abs(cleaner.taken() - 3) < 1.0e-3,
                "a second takes its rate, and keeps it: " + first);
        helper.assertTrue(Math.abs(Pollution.at(helper.getLevel(), chunk, SeriesPollution.SOOT) - (laid - 3)) < 1.0e-3,
                "from the chunk");
        for (int tick = 0; tick < 10 * TICKS_A_SECOND; tick++) {
            cleaner.tick(helper.getLevel(), at);
        }
        helper.assertTrue(Math.abs(cleaner.empty() - laid) < 1.0e-3, "in ten seconds it takes what lay there and no "
                + "more: " + laid + " lay, the chunk had " + before + " before");
        helper.assertTrue(Pollution.at(helper.getLevel(), chunk, SeriesPollution.SOOT) < 1.0e-3 && cleaner.taken() == 0,
                "the chunk clean, the cleaner emptied");
        helper.succeed();
    }

    /* What the rays of what {@code carrier} carries give it, through the walls of its containers. */
    private static double rays(final Player carrier) {
        return ItemHazards.carriedWithin(carrier, ItemHazards.RADIOACTIVE,
                wall -> IonizingRadiation.RAYS.through(wall.density(), wall.thickness()));
    }

    /* A box of {@code box} holding four echo shards. */
    private static ItemStack boxed(final Item box) {
        final ItemStack stack = new ItemStack(box);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.ECHO_SHARD,
                4))));
        return stack;
    }
}
