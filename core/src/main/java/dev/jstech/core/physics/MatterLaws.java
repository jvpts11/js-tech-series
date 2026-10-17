/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.physics;

import dev.jstech.core.explosion.Explosions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Matter that does not behave as usual, on the server: an explosion where its law says going out at its speed, the
 * blocks it breaks breaking as it reaches them rather than all at once, and liquids that rise instead of falling, each
 * pool of them climbing a block at a time while there is room above within the area.
 */
public final class MatterLaws {

    /* Liquids rise a block every so many ticks, so many blocks at most, in areas no larger than so many blocks. */
    private static final int RISE_EVERY = 4;
    private static final int MOST_RISING = 512;
    private static final long LARGEST_RISING = 32_768;
    private static final int MOST_BROKEN = 256;
    private static final double TICKS_A_SECOND = 20.0;
    private static final Map<ResourceKey<Level>, PriorityQueue<Due>> PENDING = new HashMap<>();

    private MatterLaws() {
    }

    static void listen() {
        NeoForge.EVENT_BUS.addListener(ExplosionEvent.Detonate.class, MatterLaws::onDetonate);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                tick(level);
            }
        });
    }

    /** How many blocks an explosion going out slowly in {@code level} has still to break. */
    public static int pending(final ServerLevel level) {
        final PriorityQueue<Due> queue = PENDING.get(level.dimension());
        return queue == null ? 0 : queue.size();
    }

    /*
     * An explosion where matter is slow keeps its blocks, to be broken as it reaches them; a blast of the Core's own
     * already goes out at the pace the laws give it.
     */
    private static void onDetonate(final ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || Explosions.isBlast(event.getExplosion())) {
            return;
        }
        final Vec3 centre = event.getExplosion().center();
        final double speed = Physics.lawsAt(level, centre).matter().map(Laws.Matter::explosionSpeed).orElse(0.0);
        if (speed <= 0 || event.getAffectedBlocks().isEmpty()) {
            return;
        }
        final PriorityQueue<Due> queue = PENDING.computeIfAbsent(level.dimension(),
                key -> new PriorityQueue<>((a, b) -> Long.compare(a.at(), b.at())));
        final long now = level.getGameTime();
        for (final BlockPos pos : event.getAffectedBlocks()) {
            final double away = Math.sqrt(pos.distToCenterSqr(centre));
            queue.add(new Due(pos.immutable(), now + (long) Math.ceil(away / speed * TICKS_A_SECOND)));
        }
        event.getAffectedBlocks().clear();
    }

    private static void tick(final ServerLevel level) {
        final PriorityQueue<Due> queue = PENDING.get(level.dimension());
        final long now = level.getGameTime();
        int broken = 0;
        while (queue != null && !queue.isEmpty() && queue.peek().at() <= now && broken < MOST_BROKEN) {
            final BlockPos pos = queue.poll().pos();
            if (level.isLoaded(pos) && !level.getBlockState(pos).isAir()) {
                level.destroyBlock(pos, true);
            }
            broken++;
        }
        if (now % RISE_EVERY != 0) {
            return;
        }
        for (final PhysicsArea area : Physics.areas(level)) {
            if (area.laws().matter().map(Laws.Matter::fluidsRise).orElse(false)) {
                rise(level, area);
            }
        }
    }

    /* Each source of a liquid in the area with room above it rises a block, the highest first. */
    private static void rise(final ServerLevel level, final PhysicsArea area) {
        final long blocks = (long) (area.box().maxX() - area.box().minX() + 1)
                * (area.box().maxY() - area.box().minY() + 1) * (area.box().maxZ() - area.box().minZ() + 1);
        if (blocks > LARGEST_RISING) {
            return;
        }
        final List<BlockPos> rising = new ArrayList<>();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int y = area.box().maxY() - 1; y >= area.box().minY() && rising.size() < MOST_RISING; y--) {
            for (int x = area.box().minX(); x <= area.box().maxX(); x++) {
                for (int z = area.box().minZ(); z <= area.box().maxZ(); z++) {
                    at.set(x, y, z);
                    if (!level.isLoaded(at)) {
                        continue;
                    }
                    final BlockState state = level.getBlockState(at);
                    if (state.getBlock() instanceof LiquidBlock && state.getFluidState().isSource()
                            && level.getBlockState(at.above()).isAir()) {
                        rising.add(at.immutable());
                    }
                }
            }
        }
        for (final BlockPos pos : rising) {
            final BlockState liquid = level.getBlockState(pos);
            level.setBlockAndUpdate(pos.above(), liquid);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    /**
     * A block an explosion breaks when it reaches it.
     *
     * @param pos where it is
     * @param at  the game tick it breaks at
     */
    private record Due(BlockPos pos, long at) {
    }
}
