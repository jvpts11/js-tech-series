/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePlaces;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import dev.jstech.tests.JsTests;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * The places block entities keep, carried with the blocks they name: every way the game writes a place followed and
 * a place outside left as it is, a beehive of the game's own keeping its flower as the structure is made, turned and
 * set down, and a fixer declared for a type asked instead.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StructurePlacesGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_structure_places";
    /* A type no block entity has, to declare left alone. */
    private static final ResourceLocation LEFT_ALONE = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "keeps_no_places");
    private static final String FLOWER = "flower_pos";
    /* The move a fixer was last asked about. */
    private static final AtomicReference<BlockMove> ASKED = new AtomicReference<>();
    private static boolean fixerDeclared;

    private StructurePlacesGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void moved_followsEveryWayAPlaceIsWrittenAndLeavesThoseOutside(final GameTestHelper helper) {
        final BlockPos was = new BlockPos(10, 64, 10);
        final BlockPos went = new BlockPos(-3, 70, 5);
        final BlockPos outside = new BlockPos(11, 64, 10);
        final ServerLevel level = helper.getLevel();
        final BlockMove move = new BlockMove(level, level, Map.of(was, went), Rotation.NONE);
        final CompoundTag saved = new CompoundTag();
        saved.put("target", NbtUtils.writeBlockPos(was));
        saved.put("elsewhere", NbtUtils.writeBlockPos(outside));
        saved.putInt("linkedXPos", was.getX());
        saved.putInt("linkedYPos", was.getY());
        saved.putInt("linkedZPos", was.getZ());
        saved.putLong("homePos", was.asLong());
        saved.putLong("count", was.asLong());
        final CompoundTag inner = new CompoundTag();
        final ListTag list = new ListTag();
        list.add(NbtUtils.writeBlockPos(was));
        inner.put("ends", list);
        saved.put("network", inner);
        final CompoundTag fixed = StructurePlaces.moved(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(
                BlockEntityType.CHEST), saved, move);
        helper.assertTrue(fixed != null, "something in it moved");
        helper.assertTrue(went.equals(place(fixed.get("target"))), "three numbers in a row follow: " + fixed);
        helper.assertTrue(outside.equals(place(fixed.get("elsewhere"))), "a place outside is left as it was");
        helper.assertTrue(new BlockPos(fixed.getInt("linkedXPos"), fixed.getInt("linkedYPos"),
                fixed.getInt("linkedZPos")).equals(went), "an X, a Y and a Z side by side follow");
        helper.assertTrue(BlockPos.of(fixed.getLong("homePos")).equals(went), "a packed place named so follows");
        helper.assertTrue(fixed.getLong("count") == was.asLong(), "a number not named a place is left as it was");
        final Tag deep = fixed.getCompound("network").getList("ends", Tag.TAG_INT_ARRAY).get(0);
        helper.assertTrue(went.equals(place(deep)), "and a place deep in a list follows");
        helper.assertTrue(was.equals(place(saved.get("target"))), "what was saved is not changed itself");
        StructurePlaces.leaveAlone(LEFT_ALONE);
        helper.assertTrue(StructurePlaces.moved(LEFT_ALONE, saved, move) == null,
                "a type left alone is left alone");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void beehive_keepsItsFlowerAsItsStructureIsMadeTurnedAndSetDown(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos hive = new BlockPos(3, 3, 3);
        final BlockPos flower = new BlockPos(4, 3, 3);
        final BlockPos far = new BlockPos(7, 3, 7);
        helper.setBlock(hive, Blocks.BEEHIVE);
        helper.setBlock(flower, Blocks.STONE);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.BEEHIVE);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
        tend(level, helper.absolutePos(hive), helper.absolutePos(flower));
        tend(level, helper.absolutePos(new BlockPos(3, 3, 4)), helper.absolutePos(far));
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(hive),
                helper.absolutePos(new BlockPos(4, 3, 4)));
        helper.assertTrue(made.ok() && made.blocks() == 4, "four blocks make a structure");
        final MovingStructure structure = made.structure();
        int followed = 0;
        int stayed = 0;
        final Box yard = structure.bounds();
        for (final BlockPos each : BlockPos.betweenClosed(yard.minX(), yard.minY(), yard.minZ(), yard.maxX(),
                yard.maxY(), yard.maxZ())) {
            if (level.getBlockEntity(each) instanceof BeehiveBlockEntity aboard) {
                final BlockPos tended = flowerOf(level, aboard);
                if (YardLayout.isYardBlock(tended.getX()) && level.getBlockState(tended).is(Blocks.STONE)) {
                    followed++;
                } else if (tended.equals(helper.absolutePos(far))) {
                    stayed++;
                }
            }
        }
        helper.assertTrue(followed == 1 && stayed == 1, "aboard, one hive's flower followed it into the yard, the"
                + " other's, on the land, stayed: " + followed + " and " + stayed);
        final StructurePose at = structure.pose();
        structure.teleport(StructurePose.turned(at.x() + 0.2, at.y(), at.z() - 0.3, 0, 1, 0, Math.toRadians(90)));
        final StructureResult down = MovingStructures.setDown(structure);
        helper.assertTrue(down.ok(), "it is set down turned");
        int beside = 0;
        for (final BlockPos each : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 2, 0)),
                helper.absolutePos(new BlockPos(8, 5, 8)))) {
            if (level.getBlockEntity(each) instanceof BeehiveBlockEntity landed) {
                final BlockPos tended = flowerOf(level, landed);
                if (tended.distManhattan(each) == 1 && level.getBlockState(tended).is(Blocks.STONE)) {
                    beside++;
                }
            }
        }
        helper.assertTrue(beside == 1, "set down, the hive's flower is the stone beside it, turned with it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void fixer_isAskedInsteadForTheTypeItWasDeclaredFor(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        synchronized (ASKED) {
            if (!fixerDeclared) {
                fixerDeclared = true;
                StructurePlaces.fixer(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BlockEntityType.JUKEBOX),
                        (saved, move) -> {
                            ASKED.set(move);
                            return null;
                        });
            }
        }
        final BlockPos jukebox = new BlockPos(4, 3, 4);
        helper.setBlock(jukebox, Blocks.JUKEBOX);
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(jukebox),
                helper.absolutePos(jukebox));
        helper.assertTrue(made.ok(), "a structure is made");
        final BlockMove asked = ASKED.get();
        helper.assertTrue(asked != null && asked.movedHere(helper.absolutePos(jukebox)),
                "the jukebox's fixer was asked, with its move");
        MovingStructures.takeAway(made.structure());
        helper.succeed();
    }

    /* Has the beehive at {@code hive} remember {@code flower} as its flower. */
    private static void tend(final ServerLevel level, final BlockPos hive, final BlockPos flower) {
        final BlockEntity entity = level.getBlockEntity(hive);
        if (entity instanceof BeehiveBlockEntity beehive) {
            final CompoundTag saved = beehive.saveWithoutMetadata(level.registryAccess());
            saved.put(FLOWER, NbtUtils.writeBlockPos(flower));
            beehive.loadWithComponents(saved, level.registryAccess());
        }
    }

    /* The flower {@code hive} remembers. */
    private static BlockPos flowerOf(final ServerLevel level, final BeehiveBlockEntity hive) {
        final BlockPos flower = place(hive.saveWithoutMetadata(level.registryAccess()).get(FLOWER));
        return flower == null ? BlockPos.ZERO : flower;
    }

    /* The place three numbers in a row write, or none. */
    private static @Nullable BlockPos place(final @Nullable Tag tag) {
        if (tag instanceof IntArrayTag array && array.size() == 3) {
            final int[] at = array.getAsIntArray();
            return new BlockPos(at[0], at[1], at[2]);
        }
        return null;
    }

    /* Loads the chunks round the yard's next plot, so a structure made comes into it at once. */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }
}
