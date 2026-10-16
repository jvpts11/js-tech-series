/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;

/**
 * What an instrument can learn of the veins round it, which a mod's tools ask (a prospecting pick, a scanner): the
 * direction of each vein near and how strongly it shows there, never where it is; and, standing in a vein, what it is
 * made of and how large it is. Worked out from the world's seed as the generation works it out, so it answers for
 * chunks not made yet as for those made, and an ore a player has mined still counts.
 */
public final class Prospecting {

    /* A vein of this many blocks of ore, at its own middle, shows at full strength. */
    private static final double FULL = 2_000;

    private Prospecting() {
    }

    /** The veins within {@code radius} blocks across of {@code from}, nearest first, as an instrument finds them. */
    public static List<Prospect> prospect(final ServerLevel level, final BlockPos from, final int radius) {
        final List<Prospect> found = new ArrayList<>();
        if (!VeinBalance.enabled()) {
            return found;
        }
        final List<Deposits.Entry> kinds = Deposits.all();
        final ChunkGenerator generator = level.getChunkSource().getGenerator();
        final RandomState random = level.getChunkSource().randomState();
        final Vec3 here = Vec3.atCenterOf(from);
        for (final VeinField.Vein vein : Veins.field(kinds).near(level.getSeed(), from.getX(), from.getZ(), radius)) {
            final Deposits.Entry kind = kinds.get(vein.kind());
            if (!Veins.found(generator, random, kind, vein) || composition(kind).isEmpty()) {
                continue;
            }
            final Vec3 towards = new Vec3(vein.x() + 0.5, vein.y() + 0.5, vein.z() + 0.5).subtract(here);
            final double distance = towards.length();
            final double reach = radius + vein.radius();
            final double size = Math.min(1, vein.blocks(Veins.density(kind)) / FULL);
            final double strength = size * Math.max(0, 1 - distance / reach);
            if (strength > 0) {
                found.add(new Prospect(kind.id(), distance < 1 ? Vec3.ZERO : towards.normalize(), strength));
            }
        }
        return found;
    }

    /** The vein {@code at} lies in, the one whose share there is the greatest, or none. */
    public static Optional<VeinReading> sample(final ServerLevel level, final BlockPos at) {
        if (!VeinBalance.enabled()) {
            return Optional.empty();
        }
        final List<Deposits.Entry> kinds = Deposits.all();
        final ChunkGenerator generator = level.getChunkSource().getGenerator();
        final RandomState random = level.getChunkSource().randomState();
        VeinReading best = null;
        double most = 0;
        for (final VeinField.Vein vein : Veins.field(kinds).reaching(level.getSeed(), at.getX(), at.getZ(), at.getX(),
                at.getZ())) {
            final Deposits.Entry kind = kinds.get(vein.kind());
            final double share = vein.share(at.getX(), at.getY(), at.getZ());
            if (share > most && Veins.found(generator, random, kind, vein)) {
                final Map<ResourceLocation, Double> made = composition(kind);
                if (!made.isEmpty()) {
                    most = share;
                    best = new VeinReading(kind.id(), made, vein.blocks(Veins.density(kind)));
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /*
     * The share of each material a vein of {@code kind} is laid in, among those some mod uses in a rock it crosses;
     * a moved ore's vein is all its own ore.
     */
    private static Map<ResourceLocation, Double> composition(final Deposits.Entry kind) {
        final Map<ResourceLocation, Double> shares = new LinkedHashMap<>();
        if (!kind.targets().isEmpty()) {
            shares.put(kind.id(), 1.0);
            return shares;
        }
        double total = 0;
        for (final DepositType.Mineral mineral : kind.type().minerals()) {
            if (mineral.weight() > 0 && !VeinBalance.withoutOre(mineral.material()) && laidSomewhere(kind, mineral)) {
                shares.merge(mineral.material(), mineral.weight(), Double::sum);
                total += mineral.weight();
            }
        }
        final double sum = total;
        shares.replaceAll((material, weight) -> weight / sum);
        return shares;
    }

    private static boolean laidSomewhere(final Deposits.Entry kind, final DepositType.Mineral mineral) {
        for (final HostRock rock : HostRocks.all()) {
            if ((kind.type().rocks().isEmpty() || kind.type().rocks().contains(rock.id()))
                    && OreTable.oreOf(mineral.material(), rock) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * A vein an instrument finds.
     *
     * @param deposit   its kind
     * @param direction which way it lies from the instrument, a unit vector; none when the instrument stands at its
     *                  middle
     * @param strength  how strongly it shows, from 0 to 1: the larger and the nearer, the stronger
     */
    public record Prospect(ResourceLocation deposit, Vec3 direction, double strength) {
    }

    /**
     * The vein an instrument stands in.
     *
     * @param deposit     its kind
     * @param composition the share of each material its ore is, or of the moved ore its kind is named after
     * @param blocks      about how many blocks of ore it holds
     */
    public record VeinReading(ResourceLocation deposit, Map<ResourceLocation, Double> composition, int blocks) {

        public VeinReading {
            composition = Map.copyOf(composition);
        }
    }
}
