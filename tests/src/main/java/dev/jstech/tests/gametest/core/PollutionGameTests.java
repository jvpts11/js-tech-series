/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.Pollution;
import dev.jstech.core.hazard.PollutionKind;
import dev.jstech.core.hazard.Radiation;
import dev.jstech.core.hazard.Withering;
import dev.jstech.core.jstechseries.hazard.CarbonDioxide;
import dev.jstech.core.jstechseries.hazard.IonizingRadiation;
import dev.jstech.core.jstechseries.hazard.SeriesPollution;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Pollution on a server: given off onto a chunk, spreading and fading as its field steps, taken up where plants grow;
 * soot breathed as smog, a mask stopping most of it; acid stunting crops and withering the land; heavy metals fouling
 * the food grown on their ground, poisoning whoever eats it, and dug out with the soil; fallout raising the dose rate
 * and settling on the body; the world's carbon dioxide rising by what is burnt; and nothing at all while the world has
 * pollution off.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PollutionGameTests {

    private static final String ARENA = "empty";
    /* Pollution lies on whole chunks, which neighbouring tests may share: the tests that give it off run apart, and
     * the one that turns it off alone. */
    private static final String BATCH = "jscore_pollution";
    private static final String SETTINGS = "jscore_pollution_settings";
    private static final BlockPos FEET = new BlockPos(2, 1, 2);
    private static final double CLOSE = 1.0E-3;

    private PollutionGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void pollution_spreadsAndFadesAsItsFieldSteps(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(FEET));
        final ChunkPos beside = new ChunkPos(chunk.x + 1, chunk.z);
        final double before = Pollution.at(level, chunk, SeriesPollution.SOOT);
        Pollution.emit(level, helper.absolutePos(FEET), SeriesPollution.SOOT, 1_000);
        try {
            close(helper, before + 1_000, Pollution.at(level, chunk, SeriesPollution.SOOT), "given off onto its chunk");
            final double besideBefore = Pollution.at(level, beside, SeriesPollution.SOOT);
            SeriesPollution.SOOT.field().stepNow(level);
            helper.assertTrue(Pollution.at(level, beside, SeriesPollution.SOOT) > besideBefore,
                    "it spread to the chunk beside");
            helper.assertTrue(Pollution.at(level, chunk, SeriesPollution.SOOT) < before + 1_000,
                    "and left its own a little less");
        } finally {
            clear(level, chunk, beside);
        }
        helper.assertTrue(Pollution.growsPlants(Blocks.OAK_LEAVES.defaultBlockState())
                && Pollution.growsPlants(Blocks.GRASS_BLOCK.defaultBlockState())
                && !Pollution.growsPlants(Blocks.STONE.defaultBlockState()), "leaves and grass take it up, stone not");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void smog_isBreathedFromTheChunksSootAndMostlyStoppedByAMask(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(FEET));
        final Player bare = standing(helper);
        final Player masked = standing(helper);
        masked.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));
        Pollution.emit(level, helper.absolutePos(FEET), SeriesPollution.SOOT, 200);
        try {
            final double soot = Pollution.at(level, chunk, SeriesPollution.SOOT);
            Hazards.second(bare, level);
            Hazards.second(masked, level);
            close(helper, soot, Hazards.level(bare, SeriesPollution.SMOG), "the soot breathed");
            helper.assertTrue(Hazards.band(bare, SeriesPollution.SMOG).map(band -> band.name()
                    == SeriesPollution.COUGHING).orElse(false), "it makes the bare body cough");
            close(helper, soot * 0.1, Hazards.level(masked, SeriesPollution.SMOG), "a tenth through the mask");
        } finally {
            clear(level, chunk);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void acid_stuntsCropsAndWithersTheLand(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(FEET));
        helper.setBlock(FEET, Blocks.FARMLAND);
        helper.setBlock(FEET.above(), Blocks.WHEAT);
        final BlockPos crop = helper.absolutePos(FEET.above());
        final BlockState wheat = level.getBlockState(crop);
        // Short of what withers the land, which stops crops altogether.
        Pollution.emit(level, crop, SeriesPollution.ACID, 400);
        try {
            int grew = 0;
            for (int i = 0; i < 200; i++) {
                if (CommonHooks.canCropGrow(level, crop, wheat, true)) {
                    grew++;
                }
            }
            helper.assertTrue(grew > 40 && grew < 160, "about half the crops' growth stopped; grew " + grew);
            helper.assertTrue(Withering.contamination(level, crop) < 1, "the land bears that much");
            Pollution.emit(level, crop, SeriesPollution.ACID, 800);
            helper.assertTrue(Withering.contamination(level, crop) >= 1, "the land withers under twice as much");
        } finally {
            clear(level, chunk);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void metals_foulTheFoodOfTheirGroundAndPoisonWhoeverEatsIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(FEET));
        final BlockPos field = helper.absolutePos(FEET);
        Pollution.emit(level, field, SeriesPollution.HEAVY_METALS_IN_SOIL, 500);
        try {
            final double ground = Pollution.at(level, chunk, SeriesPollution.HEAVY_METALS_IN_SOIL);
            final ItemEntity carrot = new ItemEntity(level, field.getX(), field.getY(), field.getZ(),
                    new ItemStack(Items.CARROT));
            final List<ItemEntity> drops = new ArrayList<>(List.of(carrot));
            NeoForge.EVENT_BUS.post(new BlockDropsEvent(level, field, Blocks.CARROTS.defaultBlockState(), null, drops,
                    null, ItemStack.EMPTY));
            close(helper, ground * 0.01, SeriesPollution.metalsIn(carrot.getItem()), "the carrot carries the metals");
            final ServerPlayer eater = GameTestPlayers.join(helper, "pollution_eater");
            try {
                eater.setItemInHand(InteractionHand.MAIN_HAND, carrot.getItem());
                NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(eater, carrot.getItem(), 0,
                        ItemStack.EMPTY));
                close(helper, ground * 0.01, Hazards.level(eater, SeriesPollution.POISONING), "eaten, they poison");
                NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, field, Blocks.DIRT.defaultBlockState(),
                        eater));
                close(helper, ground * (1 - 1.0 / 64), Pollution.at(level, chunk,
                        SeriesPollution.HEAVY_METALS_IN_SOIL), "a block of soil dug out takes its share away");
            } finally {
                GameTestPlayers.leave(eater);
            }
        } finally {
            clear(level, chunk);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void fallout_raisesTheDoseRateAndSettlesOnTheBody(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(FEET));
        final Vec3 at = Vec3.atCenterOf(helper.absolutePos(FEET.above()));
        final double rate = Radiation.rateAt(level, at, IonizingRadiation.RAYS);
        Pollution.emit(level, helper.absolutePos(FEET), SeriesPollution.FALLOUT, 10_000);
        try {
            final double fallout = Pollution.at(level, chunk, SeriesPollution.FALLOUT);
            helper.assertTrue(Radiation.rateAt(level, at, IonizingRadiation.RAYS) - rate >= 9.9,
                    "ten millisieverts an hour more over it");
            final Player player = standing(helper);
            Hazards.second(player, level);
            close(helper, fallout * 0.0002, Hazards.level(player, IonizingRadiation.CONTAMINATION),
                    "it settles on whoever stands in it");
        } finally {
            clear(level, chunk);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void carbonDioxide_risesByWhatIsBurnt(final GameTestHelper helper) {
        final double before = CarbonDioxide.ppm(helper.getLevel().getServer());
        CarbonDioxide.emit(10_000_000);
        CarbonDioxide.flush(helper.getLevel().getServer());
        try {
            close(helper, before + 1, CarbonDioxide.ppm(helper.getLevel().getServer()),
                    "ten thousand tonnes raise it a part per million");
        } finally {
            CarbonDioxide.PPM.set(helper.getLevel().getServer(), before);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = SETTINGS)
    public static void pollution_turnedOffGivesOffNothing(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(FEET));
        final double before = Pollution.at(level, chunk, SeriesPollution.SOOT);
        HazardBalance.setPollution(false);
        try {
            Pollution.emit(level, helper.absolutePos(FEET), SeriesPollution.SOOT, 500);
            close(helper, before, Pollution.at(level, chunk, SeriesPollution.SOOT), "nothing given off");
            helper.assertFalse(SeriesPollution.SMOG.enabled(), "nor breathed");
        } finally {
            HazardBalance.setPollution(true);
        }
        helper.succeed();
    }

    /* The game's mock player, standing in the test's arena. */
    private static Player standing(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(FEET));
        player.setPos(at.x, at.y, at.z);
        return player;
    }

    /* Takes every series pollution off {@code chunks}, so the next test finds them clean. */
    private static void clear(final ServerLevel level, final ChunkPos... chunks) {
        for (final ChunkPos chunk : chunks) {
            for (final PollutionKind kind : Pollution.kinds()) {
                final float amount = kind.field().get(level, chunk);
                if (amount > 0) {
                    kind.field().add(level, chunk, -amount);
                }
            }
        }
    }

    private static void close(final GameTestHelper helper, final double expected, final double actual,
                              final String what) {
        helper.assertTrue(Math.abs(expected - actual) <= 1.0E-9 + Math.abs(expected) * CLOSE,
                what + ": expected " + expected + ", got " + actual);
    }
}
