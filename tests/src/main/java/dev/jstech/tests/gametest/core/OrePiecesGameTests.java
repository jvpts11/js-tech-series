/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.OrePiecesBlock;
import dev.jstech.core.material.RegisteredThing;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.core.worldgen.vein.OrePieces;
import dev.jstech.core.worldgen.vein.VeinField;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.testkit.LandSurvey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The pieces of an ore that lie over its veins (core/docs/design/world.md): every mineral with an ore has them; broken
 * by hand they give the ore itself, one of what its ore block gives; they lie only on solid ground, gone with it; and
 * a vein lays a few of its minerals' over itself on dry ground, the same whichever chunks it is laid in.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OrePiecesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos ON = GROUND.above();
    /* The arena the vein's pieces are laid over, its side and middle, how high it is cleared, and the vein's seed. */
    private static final int SIDE = 8;
    private static final int MIDDLE = 4;
    private static final int TOP = 5;
    private static final long SEED = 11L;
    /* How many of the land's zones a vein is looked for in, and a chunk's coordinates within its own. */
    private static final int ZONES_SEARCHED = 64;
    private static final int CHUNK_BITS = 4;
    private static final int CHUNK_MASK = 15;

    private OrePiecesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void pieces_everyMineralWithAnOreHasThem(final GameTestHelper helper) {
        final Set<Material> ores = new HashSet<>();
        for (final RegisteredThing thing : MaterialUses.registered()) {
            if (thing.rock() != null) {
                ores.add(thing.thing().material());
            }
        }
        final List<String> missing = new ArrayList<>();
        for (final Material material : ores) {
            if (MaterialUses.piecesOf(material).filter(block -> block instanceof OrePiecesBlock).isEmpty()) {
                missing.add(material.id().toString());
            }
        }
        helper.assertTrue(!ores.isEmpty() && missing.isEmpty(), "every mineral with an ore has its pieces: missing "
                + missing);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void pieces_brokenGiveOneOfWhatTheirOreGives(final GameTestHelper helper) {
        final Map.Entry<Material, Item> sample = withDrop();
        final Block pieces = MaterialUses.piecesOf(sample.getKey()).orElseThrow();
        helper.setBlock(GROUND, Blocks.STONE);
        helper.setBlock(ON, pieces);
        helper.getLevel().destroyBlock(helper.absolutePos(ON), true);
        helper.runAfterDelay(2, () -> {
            final List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(helper.absolutePos(ON)).inflate(2));
            final int count = dropped.stream().filter(item -> item.getItem().is(sample.getValue()))
                    .mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(count == 1, "broken, " + pieces.getName().getString() + " give one "
                    + sample.getValue().getDescription().getString() + ": " + count);
            dropped.forEach(ItemEntity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void pieces_lieOnlyOnSolidGround(final GameTestHelper helper) {
        final Block pieces = MaterialUses.piecesOf(withDrop().getKey()).orElseThrow();
        helper.setBlock(GROUND, Blocks.STONE);
        helper.setBlock(ON, pieces);
        helper.assertBlockPresent(pieces, ON);
        helper.setBlock(GROUND, Blocks.AIR);
        helper.runAfterDelay(1, () -> {
            helper.assertBlockPresent(Blocks.AIR, ON);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void lay_putsAVeinsPiecesOnItsDryGroundAlikeChunkByChunk(final GameTestHelper helper) {
        floor(helper, Blocks.AIR);
        final Deposits.Entry iron = Deposits.get(TestWorldGen.IRON_VEINS).orElseThrow();
        final VeinField.Vein vein = vein(helper);
        final BlockPos low = low(helper);
        final BlockPos high = high(helper);
        final int laid = OrePieces.lay(helper.getLevel(), iron, vein, low.getX(), low.getZ(), high.getX(),
                high.getZ());
        final Set<BlockPos> whole = pieces(helper);
        helper.assertTrue(laid > 0 && laid <= OrePieces.A_VEIN && whole.size() == laid,
                "a vein lays at most " + OrePieces.A_VEIN + " pieces over itself, some on dry ground: " + laid);
        final Set<Block> veins = Set.of(MaterialUses.piecesOf(SeriesMaterials.IRON).orElseThrow(),
                MaterialUses.piecesOf(SeriesMaterials.TIN).orElseThrow());
        for (final BlockPos at : whole) {
            helper.assertTrue(veins.contains(helper.getBlockState(at).getBlock()), "a piece at " + at
                    + " is of one of the vein's minerals, iron or tin");
            helper.assertBlockPresent(Blocks.STONE, at.below());
        }
        // Laid in two halves, as two chunks lay the vein each over its own columns, the pieces are the same.
        whole.forEach(at -> helper.setBlock(at, Blocks.AIR));
        final int split = (low.getX() + high.getX()) / 2;
        final int halves = OrePieces.lay(helper.getLevel(), iron, vein, low.getX(), low.getZ(), split, high.getZ())
                + OrePieces.lay(helper.getLevel(), iron, vein, split + 1, low.getZ(), high.getX(), high.getZ());
        helper.assertTrue(halves == laid && pieces(helper).equals(whole), "the vein's pieces lie alike chunk by chunk: "
                + pieces(helper) + " against " + whole);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void lay_leavesNoPiecesOnWater(final GameTestHelper helper) {
        floor(helper, Blocks.WATER);
        final BlockPos low = low(helper);
        final BlockPos high = high(helper);
        final int laid = OrePieces.lay(helper.getLevel(), Deposits.get(TestWorldGen.IRON_VEINS).orElseThrow(),
                vein(helper), low.getX(), low.getZ(), high.getX(), high.getZ());
        helper.assertTrue(laid == 0 && pieces(helper).isEmpty(), "no piece lies on water: " + laid);
        helper.succeed();
    }

    /*
     * In a generated land, the pieces of a vein's ore lie on the surface over it, a few, of its minerals, each on the
     * ground's solid top.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void testLand_laysPiecesOfItsOreOverAVein(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "pieces_" + UUID.randomUUID().toString()
                        .substring(0, 8)));
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final VeinField.Vein vein = LandSurvey.vein(land, TestWorldGen.IRON_VEINS, ZONES_SEARCHED);
            helper.assertTrue(vein != null, "the test land holds a vein of its iron");
            final Set<Block> veins = Set.of(MaterialUses.piecesOf(SeriesMaterials.IRON).orElseThrow(),
                    MaterialUses.piecesOf(SeriesMaterials.TIN).orElseThrow());
            final int reach = (int) Math.ceil(vein.radius());
            int found = 0;
            for (int x = vein.x() - reach; x <= vein.x() + reach; x++) {
                for (int z = vein.z() - reach; z <= vein.z() + reach; z++) {
                    final LevelChunk chunk = land.getChunk(x >> CHUNK_BITS, z >> CHUNK_BITS);
                    final BlockPos top = new BlockPos(x, chunk.getHeight(Heightmap.Types.WORLD_SURFACE,
                            x & CHUNK_MASK, z & CHUNK_MASK), z);
                    final BlockState there = chunk.getBlockState(top);
                    if (there.getBlock() instanceof OrePiecesBlock) {
                        helper.assertTrue(veins.contains(there.getBlock()), "the pieces at " + top
                                + " are of the vein's iron or tin, not " + there);
                        helper.assertTrue(land.getBlockState(top.below()).isFaceSturdy(land, top.below(),
                                Direction.UP), "the pieces at " + top + " lie on solid ground");
                        found++;
                    }
                }
            }
            helper.assertTrue(found > 0 && found <= OrePieces.A_VEIN, "the vein at " + vein.x() + ", " + vein.z()
                    + " has a few pieces of its ore on the surface over it: " + found);
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    /*
     * A floor of stone over the arena at y 1, {@code over} on it and open sky above: the test's ceiling over the arena
     * taken away too, so the floor is the surface the pieces look for.
     */
    private static void floor(final GameTestHelper helper, final Block over) {
        final int bottom = helper.absolutePos(BlockPos.ZERO).getY();
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                final BlockPos column = helper.absolutePos(new BlockPos(x, 0, z));
                final int top = helper.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(),
                        column.getZ()) - bottom;
                for (int y = 3; y < Math.max(top, TOP + 1); y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), over);
            }
        }
    }

    /* A vein under the arena's middle whose reach stays within the arena. */
    private static VeinField.Vein vein(final GameTestHelper helper) {
        final BlockPos middle = helper.absolutePos(new BlockPos(MIDDLE, 1, MIDDLE));
        return new VeinField.Vein(0, middle.getX(), middle.getY(), middle.getZ(), MIDDLE, 2, SEED);
    }

    private static BlockPos low(final GameTestHelper helper) {
        final BlockPos a = helper.absolutePos(BlockPos.ZERO);
        final BlockPos b = helper.absolutePos(new BlockPos(SIDE, 0, SIDE));
        return new BlockPos(Math.min(a.getX(), b.getX()), 0, Math.min(a.getZ(), b.getZ()));
    }

    private static BlockPos high(final GameTestHelper helper) {
        final BlockPos a = helper.absolutePos(BlockPos.ZERO);
        final BlockPos b = helper.absolutePos(new BlockPos(SIDE, 0, SIDE));
        return new BlockPos(Math.max(a.getX(), b.getX()), 0, Math.max(a.getZ(), b.getZ()));
    }

    /* Where in the arena pieces of any ore lie, as the test sees them. */
    private static Set<BlockPos> pieces(final GameTestHelper helper) {
        final Set<BlockPos> out = new HashSet<>();
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                for (int y = 2; y <= TOP; y++) {
                    final BlockPos at = new BlockPos(x, y, z);
                    if (helper.getBlockState(at).getBlock() instanceof OrePiecesBlock) {
                        out.add(at);
                    }
                }
            }
        }
        return out;
    }

    /* A mineral whose ore drops something, and what it drops. */
    private static Map.Entry<Material, Item> withDrop() {
        for (final Material material : MaterialUses.pieces().keySet()) {
            if (material.oreDrop().isPresent()) {
                return Map.entry(material, MaterialUses.item(material, material.oreDrop().get()).get());
            }
        }
        throw new IllegalStateException("no mineral's ore drops anything");
    }
}
