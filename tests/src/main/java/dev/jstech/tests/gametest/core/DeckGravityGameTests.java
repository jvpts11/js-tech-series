/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.moving.DeckGravity;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEntity;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A moving structure's deck pull: a structure that pulls towards its own floor holds in its field what stands in its
 * box and a block round it, pulls along its floor's down however it is turned, as hard as its dimension pulls, and
 * keeps doing so through its save and on to another dimension.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DeckGravityGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_deck_gravity";
    private static final double CLOSE = 1.0e-6;
    /* How far over the floor's top a place two blocks clear of the field stands. */
    private static final double CLEAR = DeckGravity.REACH + 2;
    /* How many chunks round the place it arrives at are made before it is sent. */
    private static final int LAND_ROUND = 3;

    private DeckGravityGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void field_holdsWhatStandsInItAndRoundItOnly(final GameTestHelper helper) {
        final MovingStructure floor = floor(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(floor.ready(), "the floor is made"))
                .thenExecute(() -> {
                    final Level level = helper.getLevel();
                    final AABB box = floor.worldBox();
                    final Vec3 standing = new Vec3(box.getCenter().x, box.maxY + 0.5, box.getCenter().z);
                    final Vec3 above = new Vec3(box.getCenter().x, box.maxY + CLEAR, box.getCenter().z);
                    helper.assertTrue(DeckGravity.fieldAt(level, standing) == null, "a floor pulling the world's way"
                            + " holds nothing in a field of its own");
                    floor.deckGravity(true);
                    helper.assertTrue(floor.deckGravity() && floor.entity().deckGravity(), "set to pull towards its"
                            + " floor, it does, and its players' games are told");
                    helper.assertTrue(DeckGravity.fieldAt(level, standing) == floor, "one standing on it is in its"
                            + " field");
                    helper.assertTrue(DeckGravity.fieldAt(level, above) == null, "two blocks clear of it, one is"
                            + " not");
                    floor.deckGravity(false);
                    helper.assertTrue(DeckGravity.fieldAt(level, standing) == null && !floor.entity().deckGravity(),
                            "set back to the world's way, nobody is in its field");
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void down_isTheFloorsDownHoweverTheStructureIsTurned(final GameTestHelper helper) {
        final MovingStructure floor = floor(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(floor.ready(), "the floor is made"))
                .thenExecute(() -> {
                    floor.deckGravity(true);
                    final Vec3 level = DeckGravity.down(floor);
                    helper.assertTrue(level.distanceTo(new Vec3(0, -1, 0)) < CLOSE, "level, its floor pulls down: "
                            + level);
                    final StructurePose pose = floor.pose();
                    // Rolled on its side, a quarter turn about the north-south line: its floor faces east.
                    floor.teleport(StructurePose.turned(pose.x(), pose.y(), pose.z(), 0, 0, 1, Math.PI / 2));
                    final Vec3 rolled = DeckGravity.down(floor);
                    helper.assertTrue(rolled.distanceTo(new Vec3(1, 0, 0)) < CLOSE, "rolled on its side, it pulls"
                            + " towards where its floor's underside faces: " + rolled);
                    // Out from its floor, two blocks clear along the way it faces, the field ends as it did above.
                    final Vec3 middle = floor.worldBox().getCenter();
                    helper.assertTrue(DeckGravity.fieldAt(helper.getLevel(), middle) == floor, "its field turns"
                            + " with it");
                    helper.assertTrue(DeckGravity.fieldAt(helper.getLevel(), middle.add(-CLEAR - 1, 0, 0)) == null,
                            "and ends two blocks clear of its floor, the way the floor faces now");
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    /* A land is made first, which a loaded server takes a while over: hence the time. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void strength_isTheDimensionsOwnPull(final GameTestHelper helper) {
        final ServerLevel half = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(),
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                        "deck_gravity_half_land")), TestWorldGen.HALF_G_LAND.id());
        helper.assertTrue(Math.abs(DeckGravity.strength(helper.getLevel()) - 1) < CLOSE, "in the overworld a deck"
                + " pulls as the ground does: " + DeckGravity.strength(helper.getLevel()));
        helper.assertTrue(Math.abs(DeckGravity.strength(half) - 0.5) < CLOSE, "in a land of half a g, half as hard: "
                + DeckGravity.strength(half));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void deckGravity_isKeptInItsSave(final GameTestHelper helper) {
        final MovingStructure floor = floor(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(floor.ready(), "the floor is made"))
                .thenExecute(() -> {
                    floor.deckGravity(true);
                    final CompoundTag saved = new CompoundTag();
                    floor.entity().saveWithoutId(saved);
                    final MovingStructureEntity copy = MovingStructures.ENTITY.get().create(helper.getLevel());
                    helper.assertTrue(copy != null, "a structure's entity can be made");
                    copy.load(saved);
                    helper.assertTrue(copy.structure() != null && copy.structure().deckGravity()
                            && copy.deckGravity(), "read back, it still pulls towards its floor");
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    /*
     * The land it arrives at is made by the game's own generation, which takes real seconds while the test server
     * runs thousands of ticks a second: the test loads that land itself first.
     */
    @GameTest(template = ARENA, batch = "jscore_deck_gravity_travel", timeoutTicks = 6000)
    public static void deckGravity_goesWithItToAnotherDimension(final GameTestHelper helper) {
        final ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "there is a Nether");
        final MovingStructure floor = floor(helper);
        final Vec3 there = new Vec3(helper.absolutePos(BlockPos.ZERO).getX() + 0.5, 180,
                helper.absolutePos(BlockPos.ZERO).getZ() + 0.5);
        final int landX = Mth.floor(there.x) >> 4;
        final int landZ = Mth.floor(there.z) >> 4;
        for (int dx = -LAND_ROUND; dx <= LAND_ROUND; dx++) {
            for (int dz = -LAND_ROUND; dz <= LAND_ROUND; dz++) {
                nether.getChunk(landX + dx, landZ + dz);
            }
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(floor.ready(), "the floor is made"))
                .thenExecute(() -> {
                    floor.deckGravity(true);
                    helper.assertTrue(MovingStructures.travel(floor, nether, there).ok(), "it is sent to the Nether");
                })
                .thenWaitUntil(() -> {
                    final MovingStructure arrived = MovingStructures.byId(nether, floor.id());
                    helper.assertTrue(arrived != null && arrived.ready(), "it arrives in the Nether");
                })
                .thenExecute(() -> {
                    final MovingStructure arrived = MovingStructures.byId(nether, floor.id());
                    helper.assertTrue(arrived.deckGravity() && arrived.entity().deckGravity(), "and still pulls"
                            + " towards its floor there");
                    MovingStructures.takeAway(arrived);
                })
                .thenSucceed();
    }

    /* A floor of stone three blocks square, made a structure, its plot of the yard loaded first. */
    private static MovingStructure floor(final GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        final ServerLevel level = helper.getLevel();
        final int plot = StructureYard.nextPlot(level);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk((YardLayout.centreX(plot) >> 4) + dx, (YardLayout.centreZ(plot) >> 4) + dz);
            }
        }
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(3, 3, 3)),
                helper.absolutePos(new BlockPos(5, 3, 5)));
        helper.assertTrue(made.ok() && made.structure() != null, "a floor is made");
        return made.structure();
    }
}
