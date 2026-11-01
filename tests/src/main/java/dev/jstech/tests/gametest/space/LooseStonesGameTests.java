/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.JsSpace;
import dev.jstech.space.geology.LooseStones;
import dev.jstech.space.geology.LooseStonesBlock;
import dev.jstech.space.geology.StoneArrangements;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Loose stones: small stones lying on the ground, never a whole block, walked through, broken to be picked up whole,
 * lying only on ground that holds them and going with it when it goes; in the Moon's and Mars' own ground, every one
 * on ground that holds it, however the landforms round it cut the ground after it was laid.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LooseStonesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos STONES = new BlockPos(1, 2, 1);
    private static final List<BlockEntry<LooseStonesBlock>> KINDS = List.of(LooseStones.MARE_BASALT_STONES,
            LooseStones.ANORTHOSITE_STONES, LooseStones.MARTIAN_BASALT_STONES);
    /*
     * How many chunks either way of a stretch's middle are made and read; how far from a world's middle the stretch
     * may lie, in chunks; and how many stones a failure names.
     */
    private static final int SPAN = 5;
    private static final int MANY = 5;
    /* A chunk's width, and how many chunks a stretch keeps clear of a world's border. */
    private static final double CHUNK = 16;
    private static final int EDGE = 16;
    /* How many places a stony biome is looked for at, and the height its biome is read at, about the ground. */
    private static final int TRIES = 400;
    private static final int BIOME_Y = 64;

    private LooseStonesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void stones_areSmallAndWalkedThroughInEveryArrangementAndFacing(final GameTestHelper helper) {
        for (final BlockEntry<LooseStonesBlock> kind : KINDS) {
            for (int arrangement = 0; arrangement < StoneArrangements.ALL.size(); arrangement++) {
                for (final Direction facing : Direction.Plane.HORIZONTAL) {
                    final BlockState state = kind.get().defaultBlockState()
                            .setValue(LooseStonesBlock.ARRANGEMENT, arrangement)
                            .setValue(LooseStonesBlock.FACING, facing);
                    final String id = kind.getId() + " " + arrangement + " " + facing;
                    final AABB outline = state.getShape(helper.getLevel(), BlockPos.ZERO).bounds();
                    helper.assertTrue(outline.maxY < 1 && outline.getXsize() < 1 && outline.getZsize() < 1,
                            id + " is smaller than a block: " + outline);
                    helper.assertTrue(state.getCollisionShape(helper.getLevel(), BlockPos.ZERO,
                            CollisionContext.empty()).isEmpty(), id + " is walked through");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void stones_areBrokenAtOnceAndPickedUpWhole(final GameTestHelper helper) {
        for (final BlockEntry<LooseStonesBlock> kind : KINDS) {
            final BlockState state = kind.get().defaultBlockState();
            helper.assertTrue(state.getDestroySpeed(helper.getLevel(), BlockPos.ZERO) == 0,
                    kind.getId() + " breaks at once");
            final List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(STONES), null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(kind.get().asItem()),
                    kind.getId() + " gives itself back; got " + drops);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void stones_goWithTheGroundUnderThem(final GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        final BlockState stones = LooseStones.MARE_BASALT_STONES.get().defaultBlockState();
        helper.assertTrue(stones.canSurvive(helper.getLevel(), helper.absolutePos(STONES)),
                "stones lie on stone");
        helper.setBlock(STONES, stones);
        helper.assertFalse(stones.canSurvive(helper.getLevel(), helper.absolutePos(STONES.above())),
                "and not on air");
        helper.setBlock(GROUND, Blocks.AIR);
        helper.succeedWhen(() -> helper.assertBlockNotPresent(LooseStones.MARE_BASALT_STONES.get(), STONES));
    }

    @GameTest(template = ARENA)
    public static void stones_lieAsChanceHasItTheSameAtTheSamePlace(final GameTestHelper helper) {
        final LooseStonesBlock block = LooseStones.MARE_BASALT_STONES.get();
        final BlockPos here = helper.absolutePos(STONES);
        int differ = 0;
        for (int x = 0; x < 16; x++) {
            final BlockState first = placed(helper, block, here.east(x));
            helper.assertTrue(first.equals(placed(helper, block, here.east(x))),
                    "stones placed again at the same place lie the same way");
            differ += first.equals(placed(helper, block, here)) ? 0 : 1;
        }
        helper.assertTrue(differ > 0, "and other places lie other ways");
        helper.succeed();
    }

    /*
     * A wide stretch of each world is made whole on the spot, its landforms and stones laid: long ticks. The test
     * server keeps its worlds from one run to the next, so each run reads a stretch never made before, picked at random
     * anywhere in the world in a biome where stones lie: land made in an earlier run would show how it was made then.
     */
    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void stones_inTheMoonsAndMarsGroundAllLieOnGroundThatHoldsThem(final GameTestHelper helper) {
        final Map<ResourceKey<Level>, TagKey<Biome>> stony = Map.of(MoonWorld.world().dimension(), biomes(
                "moon_anorthosite"), MarsWorld.world().dimension(), biomes("mars_stony"));
        for (final Map.Entry<ResourceKey<Level>, TagKey<Biome>> each : stony.entrySet()) {
            final ResourceKey<Level> key = each.getKey();
            final ServerLevel world = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key,
                    key.location());
            final ChunkPos middle = stonyStretch(helper, world, each.getValue());
            final int fromX = middle.x;
            final int fromZ = middle.z;
            int lying = 0;
            final List<BlockPos> floating = new ArrayList<>();
            for (int cx = fromX - SPAN; cx < fromX + SPAN; cx++) {
                for (int cz = fromZ - SPAN; cz < fromZ + SPAN; cz++) {
                    final LevelChunk chunk = world.getChunk(cx, cz);
                    for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                        for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                            final int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15);
                            final int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                            for (int y = floor; y <= top; y++) {
                                final BlockPos at = new BlockPos(x, y, z);
                                final BlockState state = world.getBlockState(at);
                                if (state.getBlock() instanceof LooseStonesBlock) {
                                    lying++;
                                    if (!state.canSurvive(world, at)) {
                                        floating.add(at);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            final String stretch = key.location() + " round chunk " + fromX + ", " + fromZ;
            helper.assertTrue(lying > 0, "the ground of " + stretch + " has stones to look at");
            helper.assertTrue(floating.isEmpty(), floating.size() + " of the " + lying + " stones of " + stretch
                    + " lie over nothing that holds them, as at " + floating.stream().limit(MANY).toList());
        }
        helper.succeed();
    }

    /*
     * A chunk at random within {@code world}'s border, clear of its edge, whose biome, as its generator lays it about
     * the ground, is one {@code stony} names, where stones lie.
     */
    private static ChunkPos stonyStretch(final GameTestHelper helper, final ServerLevel world,
                                         final TagKey<Biome> stony) {
        final RandomState random = world.getChunkSource().randomState();
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(world.dimension())
                .orElseThrow(() -> new GameTestAssertException(world.dimension().location() + " has its size"));
        final int west = (int) (bounds.minX() / CHUNK) + EDGE;
        final int east = (int) (bounds.maxX() / CHUNK) - EDGE;
        final int north = (int) (bounds.minZ() / CHUNK) + EDGE;
        final int south = (int) (bounds.maxZ() / CHUNK) - EDGE;
        for (int tried = 0; tried < TRIES; tried++) {
            final ChunkPos at = new ChunkPos(ThreadLocalRandom.current().nextInt(west, east),
                    ThreadLocalRandom.current().nextInt(north, south));
            final Holder<Biome> biome = world.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                    QuartPos.fromBlock(at.getMiddleBlockX()), QuartPos.fromBlock(BIOME_Y),
                    QuartPos.fromBlock(at.getMiddleBlockZ()), random.sampler());
            if (biome.is(stony)) {
                return at;
            }
        }
        throw new GameTestAssertException("no biome of " + stony.location() + " in " + TRIES + " places of "
                + world.dimension().location());
    }

    private static TagKey<Biome> biomes(final String name) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, name));
    }

    /* The way {@code block} lies when placed at {@code at}, as a player placing it from above would. */
    private static BlockState placed(final GameTestHelper helper, final LooseStonesBlock block, final BlockPos at) {
        return block.getStateForPlacement(new BlockPlaceContext(helper.getLevel(), null, InteractionHand.MAIN_HAND,
                new ItemStack(block), new BlockHitResult(at.getCenter(), Direction.UP, at, false)));
    }
}
