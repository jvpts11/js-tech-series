/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.creature.CoreCreatureBehaviours;
import dev.jstech.core.creature.CreatureDef;
import dev.jstech.core.creature.CreatureEntity;
import dev.jstech.core.creature.Creatures;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEntities;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Core's creatures, through the test mod's: a critter that lives as its data says, its numbers and what it does;
 * a choker that chokes in the overworld's air, which the critter breathes; where each may appear, by its biomes and its
 * air, the choker added to every biome by the Core's biome modifier; and a creature that does what its data does once
 * a data pack changes it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CreatureGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_creatures";
    private static final String DATA_BATCH = "jscore_creatures_data";

    private CreatureGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void critter_livesAsItsDataSays(final GameTestHelper helper) {
        final CreatureEntity critter = helper.spawn(TestEntities.CRITTER.get(), new BlockPos(4, 2, 4));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(critter.getMaxHealth() == 14.0F
                            && Math.abs(critter.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.3) < 1.0e-6,
                    "its health and speed are its data's: " + critter.getMaxHealth() + ", "
                            + critter.getAttributeValue(Attributes.MOVEMENT_SPEED));
            helper.assertTrue(critter.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal()
                            instanceof WaterAvoidingRandomStrollGoal) && critter.goalSelector.getAvailableGoals()
                            .stream().anyMatch(goal -> goal.getGoal() instanceof PanicGoal),
                    "and it wanders and panics, as its data says");
            critter.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void choker_chokesInAirTheCritterBreathes(final GameTestHelper helper) {
        final CreatureEntity choker = helper.spawn(TestEntities.CHOKER.get(), new BlockPos(2, 2, 2));
        final CreatureEntity critter = helper.spawn(TestEntities.CRITTER.get(), new BlockPos(6, 2, 6));
        final float chokerHealth = choker.getHealth();
        final float critterHealth = critter.getHealth();
        helper.runAfterDelay(65, () -> {
            helper.assertTrue(choker.getHealth() < chokerHealth || choker.isDeadOrDying(),
                    "the choker, needing more oxygen than the air has, chokes: " + choker.getHealth());
            helper.assertTrue(critter.getHealth() >= critterHealth, "the critter breathes it: " + critter.getHealth());
            choker.discard();
            critter.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void creatures_appearOnlyWhereTheirRulesLetThem(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.GRASS_BLOCK);
        final BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.assertTrue(!SpawnPlacements.checkSpawnRules(TestEntities.CHOKER.get(), level, MobSpawnType.NATURAL, at,
                level.getRandom()), "the choker may appear in any biome, but not in air it chokes in");
        helper.assertTrue(TestEntities.CHOKER_DEF.spawns().get(0).in(level.getBiome(at)),
                "though the biome would take it");
        helper.assertTrue(!SpawnPlacements.checkSpawnRules(TestEntities.CRITTER.get(), level, MobSpawnType.NATURAL,
                at, level.getRandom()), "the critter, of no biome but the test's empty tag's, appears nowhere");
        helper.assertTrue(TestEntities.CRITTER_DEF.breathesAt(level, at.above()), "though it breathes there");
        helper.assertTrue(level.registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS)
                .containsKey(ResourceLocation.fromNamespaceAndPath("jscore", "creatures")),
                "the Core's biome modifier of creatures is in the game's data");
        final List<MobSpawnSettings.SpawnerData> creatures = level.getBiome(at).value().getMobSettings()
                .getMobs(MobCategory.CREATURE).unwrap();
        helper.assertTrue(creatures.stream().anyMatch(data -> data.type == TestEntities.CHOKER.get()),
                "and it added the choker to the biome here, which names no biome");
        helper.succeed();
    }

    /* In a batch of its own: the files it puts in are the server's, which every creature test running beside it reads. */
    @GameTest(template = ARENA, batch = DATA_BATCH)
    public static void creature_doesWhatItsDataSaysOnceAPackChangesIt(final GameTestHelper helper) {
        final CreatureEntity critter = helper.spawn(TestEntities.CRITTER.get(), new BlockPos(4, 2, 4));
        final ResourceLocation id = TestEntities.CRITTER.getId();
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(critter.getMaxHealth() == 14.0F, "it starts as declared");
                    // A data pack's file of the critter: tougher, and only wandering.
                    filesOfCreatures(Map.of(id, CreatureDef.builder().health(30)
                            .behaviour(new CoreCreatureBehaviours.Wander(1.0)).build()));
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(critter.getMaxHealth() == 30.0F, "the file wins: " + critter.getMaxHealth());
                    helper.assertTrue(critter.goalSelector.getAvailableGoals().stream()
                            .noneMatch(goal -> goal.getGoal() instanceof PanicGoal), "and it no longer panics");
                    filesOfCreatures(Map.of());
                    critter.discard();
                })
                .thenSucceed();
    }

    /* The data packs' files of creatures, as if the server had read these. */
    private static void filesOfCreatures(final Map<ResourceLocation, CreatureDef> files) {
        Creatures.files().replace(files);
    }
}
