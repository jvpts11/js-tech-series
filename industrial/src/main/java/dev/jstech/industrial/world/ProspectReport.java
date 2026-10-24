/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.world;

import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.worldgen.reservoir.Reservoirs;
import dev.jstech.core.worldgen.vein.DepositType;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.core.worldgen.vein.Prospecting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What J's Industrial's prospecting tools say (industrial/docs/design/world.md): words, never coordinates, so the
 * player triangulates. The prospecting pick gives the direction and strength of the veins near ("traces of copper to
 * the south-east", "a rich vein of iron right below") and of the reservoirs of oil, gas and brine, with about how deep
 * each lies; the portable scanner, standing in a vein, what it is made of and about how much ore it holds, and in a
 * reservoir, about how much is left in it.
 */
@TextHolder
public final class ProspectReport {

    public static final TextKey TRACES = TextKey.of("jsindustrial.prospect.traces", "Traces of %s %s");
    public static final TextKey VEIN = TextKey.of("jsindustrial.prospect.vein", "A vein of %s %s");
    public static final TextKey RICH = TextKey.of("jsindustrial.prospect.rich", "A rich vein of %s %s");
    public static final TextKey NOTHING = TextKey.of("jsindustrial.prospect.nothing", "Nothing worth mining nearby");
    public static final TextKey RESERVOIR = TextKey.of("jsindustrial.prospect.reservoir",
            "%s about %s blocks down, %s");
    public static final TextKey NORTH = TextKey.of("jsindustrial.prospect.north", "to the north");
    public static final TextKey NORTH_EAST = TextKey.of("jsindustrial.prospect.north_east", "to the north-east");
    public static final TextKey EAST = TextKey.of("jsindustrial.prospect.east", "to the east");
    public static final TextKey SOUTH_EAST = TextKey.of("jsindustrial.prospect.south_east", "to the south-east");
    public static final TextKey SOUTH = TextKey.of("jsindustrial.prospect.south", "to the south");
    public static final TextKey SOUTH_WEST = TextKey.of("jsindustrial.prospect.south_west", "to the south-west");
    public static final TextKey WEST = TextKey.of("jsindustrial.prospect.west", "to the west");
    public static final TextKey NORTH_WEST = TextKey.of("jsindustrial.prospect.north_west", "to the north-west");
    public static final TextKey BELOW = TextKey.of("jsindustrial.prospect.below", "right below");
    public static final TextKey ABOVE = TextKey.of("jsindustrial.prospect.above", "right above");
    public static final TextKey HERE = TextKey.of("jsindustrial.prospect.here", "right here");
    public static final TextKey ORE_HELD = TextKey.of("jsindustrial.scan.ore_held", "About %s blocks of ore");
    public static final TextKey SHARE = TextKey.of("jsindustrial.scan.share", "%s: %s%%");
    public static final TextKey NO_VEIN = TextKey.of("jsindustrial.scan.no_vein", "No vein here");
    public static final TextKey RESERVOIR_LEFT = TextKey.of("jsindustrial.scan.reservoir_left",
            "%s: about %s buckets left");
    /** How far the pick finds veins, and reservoirs, in blocks. */
    public static final int PICK_REACH = 48;
    public static final int RESERVOIR_REACH = 128;
    /** The steps it tells a depth in, in blocks. */
    public static final int DEPTH_STEP = 10;
    /** The most findings it tells at once, the strongest first. */
    public static final int MOST_LINES = 5;
    /** Below this share of its full strength a vein shows as traces, and below the second as a vein. */
    public static final double FAINT = 1.0 / 3.0;
    public static final double CLEAR = 2.0 / 3.0;
    /* Below this share of a direction across the ground, the direction is up or down. */
    private static final double STEEP = 0.35;
    private static final TextKey[] COMPASS = {NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST,
            NORTH_WEST};
    private static final double SECTOR = 360.0 / 8;
    private static final int PERCENT = 100;
    private static final int BUCKET = 1_000;

    private ProspectReport() {
    }

    /** What the prospecting pick says at {@code at}: the veins near and the reservoirs below, the strongest first. */
    public static List<Text> pick(final ServerLevel level, final BlockPos at) {
        final List<Text> lines = new ArrayList<>();
        final List<Prospecting.Prospect> veins = new ArrayList<>(Prospecting.prospect(level, at, PICK_REACH));
        veins.sort((one, other) -> Double.compare(other.strength(), one.strength()));
        for (final Prospecting.Prospect vein : veins) {
            if (lines.size() >= MOST_LINES) {
                break;
            }
            lines.add(strength(vein.strength()).with(metalOf(vein.deposit()), direction(vein.direction())));
        }
        for (final Reservoirs.Reservoir reservoir : Reservoirs.near(level, at, RESERVOIR_REACH)) {
            if (lines.size() >= MOST_LINES) {
                break;
            }
            final Vec3 middle = new Vec3(reservoir.body().x() + 0.5, reservoir.body().y() + 0.5,
                    reservoir.body().z() + 0.5);
            final Vec3 towards = middle.subtract(Vec3.atCenterOf(at));
            final Vec3 across = new Vec3(towards.x, 0, towards.z);
            final TextKey way = across.length() <= reservoir.body().radius() ? BELOW : compass(across);
            lines.add(RESERVOIR.with(fluidName(reservoir.kind().type().fluid()), depth(at.getY() - middle.y), way));
        }
        if (lines.isEmpty()) {
            lines.add(NOTHING.text());
        }
        return lines;
    }

    /** What the portable scanner says at {@code at}: the vein it stands in, and the reservoir it stands on. */
    public static List<Text> scan(final ServerLevel level, final BlockPos at) {
        final List<Text> lines = new ArrayList<>();
        final Optional<Prospecting.VeinReading> vein = Prospecting.sample(level, at);
        if (vein.isPresent()) {
            lines.add(ORE_HELD.with(vein.get().blocks()));
            for (final Map.Entry<ResourceLocation, Double> share : vein.get().composition().entrySet()) {
                lines.add(SHARE.with(nameOf(share.getKey()), Math.round(share.getValue() * PERCENT)));
            }
        } else {
            lines.add(NO_VEIN.text());
        }
        for (final Reservoirs.Reservoir reservoir : Reservoirs.near(level, at, 0)) {
            lines.add(RESERVOIR_LEFT.with(fluidName(reservoir.kind().type().fluid()),
                    Reservoirs.left(level, reservoir) / BUCKET));
        }
        return lines;
    }

    /** How strongly a vein shows, in words: traces, a vein, a rich vein. */
    public static TextKey strength(final double strength) {
        if (strength < FAINT) {
            return TRACES;
        }
        return strength < CLEAR ? VEIN : RICH;
    }

    /** Which way {@code direction} lies, in words: a point of the compass, or right below, above or here. */
    public static TextKey direction(final Vec3 direction) {
        if (direction.lengthSqr() == 0) {
            return HERE;
        }
        final Vec3 unit = direction.normalize();
        if (Math.hypot(unit.x, unit.z) < STEEP) {
            return unit.y < 0 ? BELOW : ABOVE;
        }
        return compass(unit);
    }

    /** A depth below, in steps of {@link #DEPTH_STEP} blocks, never less than one step. */
    public static long depth(final double blocks) {
        return Math.max(DEPTH_STEP, Math.round(blocks / DEPTH_STEP) * DEPTH_STEP);
    }

    /* The point of the compass a direction across the ground lies at; north is towards negative z. */
    private static TextKey compass(final Vec3 across) {
        final double degrees = Math.toDegrees(Math.atan2(across.x, -across.z));
        final int sector = Math.floorMod((int) Math.round(degrees / SECTOR), COMPASS.length);
        return COMPASS[sector];
    }

    /*
     * What a vein of {@code deposit} is told as: the element its most of is mined for, or its most material itself;
     * the deposit's id when nothing else is known of it.
     */
    private static Text metalOf(final ResourceLocation deposit) {
        final Optional<DepositType> type = Deposits.get(deposit).map(Deposits.Entry::type);
        if (type.isEmpty() || type.get().minerals().isEmpty()) {
            return Text.literal(deposit.toString());
        }
        DepositType.Mineral most = type.get().minerals().getFirst();
        for (final DepositType.Mineral mineral : type.get().minerals()) {
            if (mineral.weight() > most.weight()) {
                most = mineral;
            }
        }
        final Optional<Material> material = Materials.byId(most.material());
        return material.map(found -> found.mineral().map(mineral -> mineral.main().material().name())
                .orElse(found.name())).orElse(Text.literal(most.material().toString()));
    }

    private static Text nameOf(final ResourceLocation material) {
        return Materials.byId(material).map(Material::name).orElse(Text.literal(material.toString()));
    }

    private static Text fluidName(final ResourceLocation fluid) {
        return BuiltInRegistries.FLUID.getOptional(fluid)
                .map(found -> GameText.of(new FluidStack(found, 1).getHoverName()))
                .orElse(Text.literal(fluid.toString()));
    }
}
