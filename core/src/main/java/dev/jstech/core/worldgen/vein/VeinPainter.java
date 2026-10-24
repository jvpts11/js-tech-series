/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import dev.jstech.core.material.HostRock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/**
 * Lays one vein over a box of columns and nothing outside it: each block within it, more often towards its middle,
 * becomes the ore of one of the vein's materials in the rock it was, picked by their weights among those some mod uses
 * in that rock and that lie in that part of the vein and its biome; a block that is no rock the vein crosses is left
 * as it was. A moved ore's vein replaces what its own
 * feature replaces instead. The world's generation lays veins a chunk at a time ({@link VeinFeature}); anything else
 * can lay one where it is wanted.
 */
public final class VeinPainter {

    private static final int UPDATE = 2;
    private static final long MIX_X = -7_046_029_254_386_353_131L;
    private static final long MIX_Y = 6_364_136_223_846_793_005L;
    private static final long MIX_Z = -4_417_276_706_812_531_889L;
    private static final double TO_UNIT = 1.0 / (1L << 53);

    private VeinPainter() {
    }

    /**
     * Lays {@code vein} of {@code entry} at {@code density} over the columns from {@code x0, z0} to {@code x1, z1};
     * how many blocks it turned to ore.
     */
    public static int paint(final WorldGenLevel level, final Deposits.Entry entry, final VeinField.Vein vein,
                            final double density, final int x0, final int z0, final int x1, final int z1) {
        final int fromX = Math.max(x0, (int) Math.floor(vein.x() - vein.radius()));
        final int toX = Math.min(x1, (int) Math.ceil(vein.x() + vein.radius()));
        final int fromZ = Math.max(z0, (int) Math.floor(vein.z() - vein.radius()));
        final int toZ = Math.min(z1, (int) Math.ceil(vein.z() + vein.radius()));
        final int fromY = Math.max(level.getMinBuildHeight(), (int) Math.floor(vein.y() - vein.thickness()));
        final int toY = Math.min(level.getMaxBuildHeight() - 1, (int) Math.ceil(vein.y() + vein.thickness()));
        final Zoning zoning = Zoning.of(entry.type());
        final Map<PickKey, Picks> picks = new HashMap<>();
        final RandomSource random = RandomSource.create(vein.seed());
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int laid = 0;
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                for (int y = fromY; y <= toY; y++) {
                    final double share = vein.share(x, y, z) * density;
                    if (share <= 0) {
                        continue;
                    }
                    final long hash = hash(vein.seed(), x, y, z);
                    if (unit(hash) >= share) {
                        continue;
                    }
                    final BlockState there = level.getBlockState(at.set(x, y, z));
                    final BlockState ore = entry.targets().isEmpty()
                            ? oreFor(entry, there.getBlock(), zoning.key(level, at, y > vein.y()), picks,
                                    unit(hash * MIX_Y + vein.seed()))
                            : movedOre(entry, there, random);
                    if (ore != null) {
                        level.setBlock(at, ore, UPDATE);
                        laid++;
                    }
                }
            }
        }
        return laid;
    }

    /*
     * The ore a block of rock becomes, in the part of the vein and the biomes {@code where} says, or none when it is no
     * rock the vein crosses or none of the ores that lie there is used.
     */
    private static BlockState oreFor(final Deposits.Entry entry, final Block block, final Where where,
                                     final Map<PickKey, Picks> picks, final double roll) {
        final HostRock rock = OreTable.rockOf(block);
        if (rock == null || !entry.type().rocks().isEmpty() && !entry.type().rocks().contains(rock.id())) {
            return null;
        }
        final Block ore = picks.computeIfAbsent(new PickKey(rock, where),
                key -> Picks.of(entry.type(), key)).pick(roll);
        return ore == null ? null : ore.defaultBlockState();
    }

    private static BlockState movedOre(final Deposits.Entry entry, final BlockState there, final RandomSource random) {
        for (final OreConfiguration.TargetBlockState target : entry.targets()) {
            if (target.target.test(there, random)) {
                return target.state;
            }
        }
        return null;
    }

    private static long hash(final long seed, final int x, final int y, final int z) {
        long h = seed ^ x * MIX_X ^ y * MIX_Y ^ z * MIX_Z;
        h = (h ^ h >>> 33) * MIX_Y;
        h = (h ^ h >>> 29) * MIX_X;
        return h ^ h >>> 32;
    }

    private static double unit(final long hash) {
        return (hash >>> 11) * TO_UNIT;
    }

    /*
     * Where a block of a vein lies, as its zoned materials ask: above the vein's middle or not, and which of the biomes
     * they ask for it is in, a bit each in the order of the vein's {@link Zoning}.
     */
    private record Where(boolean above, long biomes) {

        static final Where ANYWHERE = new Where(false, 0L);
    }

    /* The ores one rock gives in one place of a vein. */
    private record PickKey(HostRock rock, Where where) {
    }

    /* The biomes a vein's materials ask for, in order, and whether any lies only in a part of it. */
    private record Zoning(boolean parts, List<TagKey<Biome>> biomes) {

        static Zoning of(final DepositType type) {
            if (!type.zoned()) {
                return new Zoning(false, List.of());
            }
            boolean parts = false;
            final List<TagKey<Biome>> biomes = new ArrayList<>();
            for (final DepositType.Mineral mineral : type.minerals()) {
                parts |= mineral.zone() != DepositType.Zone.ALL;
                mineral.biomes().filter(tag -> !biomes.contains(tag)).ifPresent(biomes::add);
            }
            return new Zoning(parts, List.copyOf(biomes));
        }

        /* Where the block at {@code at} lies; the biome is asked only of a vein whose materials ask for one. */
        Where key(final WorldGenLevel level, final BlockPos at, final boolean above) {
            if (!this.parts && this.biomes.isEmpty()) {
                return Where.ANYWHERE;
            }
            long in = 0L;
            if (!this.biomes.isEmpty()) {
                final Holder<Biome> biome = level.getBiome(at);
                for (int i = 0; i < this.biomes.size() && i < Long.SIZE; i++) {
                    if (biome.is(this.biomes.get(i))) {
                        in |= 1L << i;
                    }
                }
            }
            return new Where(this.parts && above, in);
        }

        /* Whether {@code mineral} lies at {@code where}. */
        boolean holds(final DepositType.Mineral mineral, final Where where) {
            if (!mineral.zone().holds(where.above())) {
                return false;
            }
            final int index = mineral.biomes().map(this.biomes::indexOf).orElse(-1);
            return index < 0 || index < Long.SIZE && (where.biomes() & 1L << index) != 0;
        }
    }

    /* The ores of a vein's materials in one rock, by weight, those no mod uses there and those turned off left out. */
    private static final class Picks {

        private final List<Block> ores;
        private final double[] reached;

        private Picks(final List<Block> ores, final double[] reached) {
            this.ores = ores;
            this.reached = reached;
        }

        static Picks of(final DepositType type, final PickKey key) {
            final Zoning zoning = Zoning.of(type);
            final List<Block> ores = new ArrayList<>();
            final List<Double> weights = new ArrayList<>();
            double total = 0;
            for (final DepositType.Mineral mineral : type.minerals()) {
                if (!zoning.holds(mineral, key.where())) {
                    continue;
                }
                final ResourceLocation material = mineral.material();
                final Block ore = VeinBalance.withoutOre(material) ? null : OreTable.oreOf(material, key.rock());
                if (ore != null && mineral.weight() > 0) {
                    ores.add(ore);
                    weights.add(mineral.weight());
                    total += mineral.weight();
                }
            }
            final double[] reached = new double[ores.size()];
            double sum = 0;
            for (int i = 0; i < reached.length; i++) {
                sum += weights.get(i) / total;
                reached[i] = sum;
            }
            return new Picks(ores, reached);
        }

        Block pick(final double roll) {
            for (int i = 0; i < this.reached.length; i++) {
                if (roll < this.reached[i]) {
                    return this.ores.get(i);
                }
            }
            return this.ores.isEmpty() ? null : this.ores.getLast();
        }
    }
}
