/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.wreck;

import dev.jstech.core.explosion.Detonation;
import dev.jstech.core.explosion.Explosions;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.region.Box;
import dev.jstech.space.JsSpace;
import dev.jstech.space.config.SpaceServerConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * A stage striking the ground: its speed's energy and what propellant it still held go off as one blast of J's Core's,
 * its shell of aluminium thrown as fragments, and leave a crater; once the blast has gone out, its wreckage lies in the
 * crater and round it, holding about a third of the blocks the stage was made of to be salvaged. A server that turns
 * the damage of falling stages off keeps the ground whole: the stage then only lies there as wreckage. Under the sea
 * the wreckage settles on the bottom.
 */
public final class StageImpacts {

    /** The explosive a stage's fall is, whose blasts J's Space's setting governs. */
    public static final ResourceLocation EXPLOSIVE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "stage_impact");
    /** The part of a stage's blocks its wreckage gives back: one of every three. */
    public static final int SALVAGED_ONE_IN = 3;
    /**
     * What a kilogram of propellant left in a stage gives as it goes off in the fall, in joules: about a tenth of as
     * much TNT, as spilt propellants that mix and burn do. An estimate, to be set by playing.
     */
    public static final double PROPELLANT_JOULES = 420_000;
    /* A piece of wreckage for each tonne of the stage, from one to two dozen. */
    private static final double KILOGRAMS_A_PIECE = 1_000;
    private static final int MOST_PIECES = 24;
    /* How far round the middle wreckage lies with no crater, in blocks, as it spreads. */
    private static final double SPREAD = 2.0;
    /* How far above and below where it struck the ground is looked for, a crater's floor or the sea's bottom. */
    private static final int MOST_LIFT = 3;
    private static final int FARTHEST_DOWN = 48;
    /* The falls whose blasts are still going out, by level; touched on the server's thread only. */
    private static final Map<ResourceKey<Level>, List<Pending>> PENDING = new HashMap<>();

    private StageImpacts() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                tick(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                PENDING.remove(level.dimension());
            }
        });
    }

    /**
     * A stage of {@code dryKilograms} empty, still holding {@code leftoverKilograms} of propellant, striking the ground
     * at {@code at} at {@code speed} metres a second, made of {@code made}: the blast and its crater where the server
     * lets falling stages do damage, and the wreckage, chosen by {@code seed}.
     */
    public static Impact strike(final ServerLevel level, final Vec3 at, final double dryKilograms,
                                final double leftoverKilograms, final double speed, final List<ItemStack> made,
                                final long seed) {
        final RandomSource random = RandomSource.create(seed);
        final List<ItemStack> salvage = salvaged(made, random);
        final int pieces = pieces(dryKilograms, salvage.size());
        final double mass = Math.max(0, dryKilograms) + Math.max(0, leftoverKilograms);
        final double joules = 0.5 * mass * speed * speed + Math.max(0, leftoverKilograms) * PROPELLANT_JOULES;
        if (SpaceServerConfig.fallingStagesDoDamage() && joules > 0) {
            final Optional<Detonation> blast = Explosions.blast(level, at, EXPLOSIVE, joules)
                    .casing(Elements.of(Element.ALUMINIUM), Math.max(0, dryKilograms)).detonate();
            if (blast.isPresent()) {
                PENDING.computeIfAbsent(level.dimension(), key -> new ArrayList<>())
                        .add(new Pending(blast.get(), salvage, pieces, random.nextLong()));
                return new Impact(true, joules, salvage, pieces);
            }
        }
        scatter(level, at, SPREAD + Math.sqrt(pieces), salvage, pieces, random);
        return new Impact(false, joules, salvage, pieces);
    }

    /** What {@code structure} is made of, a stack for each kind of block's item, as a stage's blocks are. */
    public static List<ItemStack> blocksOf(final MovingStructure structure) {
        final Map<Item, Integer> counted = new LinkedHashMap<>();
        final Box box = structure.bounds();
        final Level level = structure.level();
        for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            final BlockState state = level.getBlockState(pos);
            final Item item = state.getBlock().asItem();
            if (!state.isAir() && item != Items.AIR) {
                counted.merge(item, 1, Integer::sum);
            }
        }
        final List<ItemStack> made = new ArrayList<>();
        counted.forEach((item, count) -> made.add(new ItemStack(item, count)));
        return List.copyOf(made);
    }

    /** Whether falls are still going out in {@code level}, their wreckage yet to lie. */
    public static boolean pending(final ServerLevel level) {
        return !PENDING.getOrDefault(level.dimension(), List.of()).isEmpty();
    }

    /* The blocks given back, a third of each kind's, the odd one by chance, in stacks of no more than a stack. */
    private static List<ItemStack> salvaged(final List<ItemStack> made, final RandomSource random) {
        final List<ItemStack> salvage = new ArrayList<>();
        for (final ItemStack stack : made) {
            final double share = stack.getCount() / (double) SALVAGED_ONE_IN;
            int count = (int) Math.floor(share);
            if (random.nextDouble() < share - count) {
                count++;
            }
            while (count > 0) {
                final int taken = Math.min(count, stack.getMaxStackSize());
                salvage.add(stack.copyWithCount(taken));
                count -= taken;
            }
        }
        return List.copyOf(salvage);
    }

    private static int pieces(final double dryKilograms, final int stacks) {
        final int byMass = (int) Math.clamp(Math.round(dryKilograms / KILOGRAMS_A_PIECE), 1, MOST_PIECES);
        final int byStacks = (stacks + RocketWreckageBlockEntity.SLOTS - 1) / RocketWreckageBlockEntity.SLOTS;
        return Math.max(byMass, byStacks);
    }

    private static void tick(final ServerLevel level) {
        final List<Pending> waiting = PENDING.get(level.dimension());
        if (waiting == null || waiting.isEmpty()) {
            return;
        }
        final Iterator<Pending> each = waiting.iterator();
        while (each.hasNext()) {
            final Pending fall = each.next();
            if (fall.blast().finished()) {
                each.remove();
                scatter(level, fall.blast().centre(), Math.max(SPREAD, fall.blast().fireball()), fall.salvage(),
                        fall.pieces(), RandomSource.create(fall.seed()));
            }
        }
    }

    /* Lays {@code pieces} of wreckage within {@code radius} of {@code at}, sharing {@code salvage} among them. */
    private static void scatter(final ServerLevel level, final Vec3 at, final double radius,
                                final List<ItemStack> salvage, final int pieces, final RandomSource random) {
        final List<List<ItemStack>> shares = new ArrayList<>();
        for (int piece = 0; piece < pieces; piece++) {
            shares.add(new ArrayList<>());
        }
        for (int k = 0; k < salvage.size(); k++) {
            shares.get(k % pieces).add(salvage.get(k));
        }
        for (final List<ItemStack> share : shares) {
            final double angle = random.nextDouble() * Math.PI * 2;
            final double far = Math.sqrt(random.nextDouble()) * radius;
            final int x = (int) Math.floor(at.x + Math.cos(angle) * far);
            final int z = (int) Math.floor(at.z + Math.sin(angle) * far);
            place(level, x, (int) Math.floor(at.y), z).ifPresent(wreck -> wreck.hold(share));
        }
    }

    /*
     * A piece of wreckage on the ground of the column {@code x, z} near the height {@code y} it struck at, or under the
     * water on its bottom: looked for downwards, through air, plants and water, to the first firm block.
     */
    private static Optional<RocketWreckageBlockEntity> place(final ServerLevel level, final int x, final int y,
                                                             final int z) {
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
            return Optional.empty();
        }
        final int top = Math.min(level.getMaxBuildHeight() - 1, y + MOST_LIFT);
        final int bottom = Math.max(level.getMinBuildHeight() + 1, y - FARTHEST_DOWN);
        // Struck into a slope, the ground is above: up to it, never down into a cave under it.
        final boolean buried = !level.getBlockState(new BlockPos(x, top, z)).canBeReplaced();
        final int step = buried ? 1 : -1;
        final int end = buried ? Math.min(level.getMaxBuildHeight() - 1, top + FARTHEST_DOWN) : bottom;
        for (int at = top; buried ? at <= end : at >= end; at += step) {
            final BlockPos pos = new BlockPos(x, at, z);
            if (!level.getBlockState(pos).canBeReplaced() || level.getBlockState(pos.below()).canBeReplaced()) {
                continue;
            }
            level.setBlock(pos, RocketWreckage.BLOCK.get().defaultBlockState(), 3);
            return level.getBlockEntity(pos) instanceof RocketWreckageBlockEntity wreck ? Optional.of(wreck)
                    : Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * What a fall did.
     *
     * @param crater  whether it went off as a blast and left a crater
     * @param joules  the energy it struck with, in joules
     * @param salvage what its wreckage holds to be salvaged
     * @param pieces  how many pieces of wreckage it leaves
     */
    public record Impact(boolean crater, double joules, List<ItemStack> salvage, int pieces) {

        public Impact {
            salvage = List.copyOf(salvage);
        }
    }

    /* A fall whose blast is going out, its wreckage to lie once it has. */
    private record Pending(Detonation blast, List<ItemStack> salvage, int pieces, long seed) {

        Pending {
            salvage = List.copyOf(salvage);
        }
    }
}
