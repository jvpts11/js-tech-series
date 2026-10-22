/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.region.Box;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.StructureUuid;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Making a moving structure of blocks placed in the world: a box of them, a list, or every block joined to one. The
 * blocks are moved whole into a plot of the yard and the structure stands exactly where they stood, so nothing on
 * them notices; it refuses, saying why, blocks that must stay put (the tag {@code jscore:stays_put_for_structures}),
 * blocks that are another structure's already, blocks in land that is not loaded, and more blocks than the server
 * allows. A plot whose chunks have not loaded yet is waited for: the structure is made at once, and its blocks stand
 * where they are until the plot is loaded, a few ticks later ({@link MovingStructure#arriving()}). A structure may
 * also be born as one from a template ({@link #born}), its blocks laid straight into its plot.
 */
public final class StructureAssembly {

    /** The blocks no structure takes with it: bedrock, the barrier, portals and their frames. */
    public static final TagKey<Block> STAYS_PUT = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "stays_put_for_structures"));

    private StructureAssembly() {
    }

    /** Makes a structure of every block in the box from {@code from} to {@code to}, both corners included. */
    public static StructureResult box(final ServerLevel level, final BlockPos from, final BlockPos to,
                                      @Nullable final Vec3 pivot) {
        return box(level, from, to, pivot, null);
    }

    /**
     * Makes a structure of every block in the box from {@code from} to {@code to} for {@code maker}, counted against
     * their limit of moving structures ({@link PlayerLimits#MOVING_STRUCTURES}) and refused at it; with no maker, for
     * nobody, as a command or a machine makes one.
     */
    public static StructureResult box(final ServerLevel level, final BlockPos from, final BlockPos to,
                                      @Nullable final Vec3 pivot, @Nullable final UUID maker) {
        final Set<BlockPos> chosen = new LinkedHashSet<>();
        for (final BlockPos at : BlockPos.betweenClosed(from, to)) {
            if (level.isOutsideBuildHeight(at)) {
                continue;
            }
            final StructureResult kept = keptByAnother(level, at);
            if (kept != null) {
                return kept;
            }
            if (!Loaded.at(level, at)) {
                return StructureResult.refused(StructureTexts.NOT_LOADED.text());
            }
            if (!level.getBlockState(at).isAir()) {
                chosen.add(at.immutable());
                if (chosen.size() > StructureBalance.mostBlocks()) {
                    return tooMany(chosen.size());
                }
            }
        }
        return blocks(level, chosen, pivot, maker);
    }

    /**
     * Makes a structure of {@code seed} and every block joined to it face to face, the ground and the blocks that
     * stay put never taken; refused when the blocks joined are more than a structure may have, as when they reach
     * the ground, and when they reach land that is not loaded, whose blocks cannot be known.
     */
    public static StructureResult joined(final ServerLevel level, final BlockPos seed, @Nullable final Vec3 pivot) {
        return joined(level, seed, pivot, null);
    }

    /** Makes a structure of {@code seed} and every block joined to it for {@code maker}, counted as by the box. */
    public static StructureResult joined(final ServerLevel level, final BlockPos seed, @Nullable final Vec3 pivot,
                                         @Nullable final UUID maker) {
        final Set<BlockPos> found = new LinkedHashSet<>();
        final Deque<BlockPos> next = new ArrayDeque<>();
        final StructureResult kept = keptByAnother(level, seed);
        if (kept != null) {
            return kept;
        }
        if (!Loaded.at(level, seed)) {
            return StructureResult.refused(StructureTexts.NOT_LOADED.text());
        }
        if (level.getBlockState(seed).isAir()) {
            return StructureResult.refused(StructureTexts.NOTHING.text());
        }
        next.add(seed.immutable());
        found.add(seed.immutable());
        while (!next.isEmpty()) {
            final BlockPos at = next.poll();
            for (final Direction side : Direction.values()) {
                final BlockPos near = at.relative(side);
                if (found.contains(near) || level.isOutsideBuildHeight(near)) {
                    continue;
                }
                if (!Loaded.at(level, near)) {
                    return StructureResult.refused(StructureTexts.NOT_LOADED.text());
                }
                final BlockState state = level.getBlockState(near);
                if (state.isAir() || state.is(STAYS_PUT)) {
                    continue;
                }
                found.add(near);
                if (found.size() > StructureBalance.mostBlocks()) {
                    return tooMany(found.size());
                }
                next.add(near);
            }
        }
        return blocks(level, found, pivot, maker);
    }

    /**
     * Makes a structure of the blocks at {@code chosen}, air among them left out; its pivot at the world's place
     * {@code pivot}, or the middle of the blocks when null.
     */
    public static StructureResult blocks(final ServerLevel level, final Collection<BlockPos> chosen,
                                         @Nullable final Vec3 pivot) {
        return blocks(level, chosen, pivot, null);
    }

    /** Makes a structure of the blocks at {@code chosen} for {@code maker}, counted as by the box. */
    public static StructureResult blocks(final ServerLevel level, final Collection<BlockPos> chosen,
                                         @Nullable final Vec3 pivot, @Nullable final UUID maker) {
        final Set<BlockPos> blocks = new HashSet<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final BlockPos at : chosen) {
            if (level.isOutsideBuildHeight(at)) {
                continue;
            }
            final StructureResult kept = keptByAnother(level, at);
            if (kept != null) {
                return kept;
            }
            if (!Loaded.at(level, at)) {
                return StructureResult.refused(StructureTexts.NOT_LOADED.text());
            }
            final BlockState state = level.getBlockState(at);
            if (state.isAir()) {
                continue;
            }
            if (YardLayout.isYardBlock(at.getX())) {
                return StructureResult.refused(StructureTexts.IN_YARD.text());
            }
            if (state.is(STAYS_PUT)) {
                return StructureResult.refused(StructureTexts.IMMOVABLE.with(
                        BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), at.toShortString()));
            }
            blocks.add(at.immutable());
            minX = Math.min(minX, at.getX());
            minY = Math.min(minY, at.getY());
            minZ = Math.min(minZ, at.getZ());
            maxX = Math.max(maxX, at.getX());
            maxY = Math.max(maxY, at.getY());
            maxZ = Math.max(maxZ, at.getZ());
        }
        if (blocks.isEmpty()) {
            return StructureResult.refused(StructureTexts.NOTHING.text());
        }
        if (blocks.size() > StructureBalance.mostBlocks()) {
            return tooMany(blocks.size());
        }
        // A block lifted reads the blocks beside it, so the land a block beyond them must be loaded too.
        if (!Loaded.area(level, minX - 1, minZ - 1, maxX + 1, maxZ + 1)) {
            return StructureResult.refused(StructureTexts.NOT_LOADED.text());
        }
        if (maker != null && !PlayerLimits.take(level.getServer(), maker, PlayerLimits.MOVING_STRUCTURES)) {
            return StructureResult.refused(PlayerLimits.reached(level.getServer(), maker,
                    PlayerLimits.MOVING_STRUCTURES));
        }
        final StructureUuid id = StructureUuid.random();
        final int plot;
        try {
            plot = StructureYard.take(level, id);
        } catch (final IllegalStateException full) {
            if (maker != null) {
                PlayerLimits.giveBack(level.getServer(), maker, PlayerLimits.MOVING_STRUCTURES);
            }
            return StructureResult.refused(StructureTexts.YARD_FULL.text());
        }
        final int dx = YardLayout.centreX(plot) - (minX + maxX) / 2;
        final int dz = YardLayout.centreZ(plot) - (minZ + maxZ) / 2;
        final Box bounds = new Box(minX + dx, minY, minZ + dz, maxX + dx, maxY, maxZ + dz);
        StructureYard.hold(level, bounds);
        final long[] places = new long[blocks.size()];
        int each = 0;
        for (final BlockPos at : blocks) {
            places[each++] = at.asLong();
        }
        final Vec3 centre = pivot != null ? pivot
                : new Vec3((minX + maxX + 1) / 2.0, minY, (minZ + maxZ + 1) / 2.0);
        final MovingStructure structure = MovingStructures.make(level, id, plot, bounds, blocks.size(),
                centre.x + dx, centre.y, centre.z + dz, StructurePose.at(centre.x, centre.y, centre.z),
                new StructureArrival(places, dx, dz));
        if (maker != null) {
            structure.madeBy(maker);
        }
        return StructureResult.done(structure, blocks.size());
    }

    /**
     * Makes a structure born as one, of the blocks of {@code template}, standing with the middle of its bottom at the
     * world's place {@code at}: its blocks are laid straight into its plot of the yard and never stand in the world,
     * as a rocket is built in its hangar. For {@code maker}, counted as by the box; with no maker, for nobody. Refused,
     * saying why, for a template of nothing, of more blocks than the server allows, holding a block that stays put, or
     * that would stand out of the world's height.
     */
    public static StructureResult born(final ServerLevel level, final StructureTemplate template, final Vec3 at,
                                       @Nullable final UUID maker) {
        final Vec3i size = template.getSize();
        final int count = size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0 ? 0
                : StructureBirth.blocksOf(template);
        if (count == 0) {
            return StructureResult.refused(StructureTexts.NOTHING.text());
        }
        if (count > StructureBalance.mostBlocks()) {
            return tooMany(count);
        }
        final Optional<StructureBirth.Placed> stays = StructureBirth.staysPut(template);
        if (stays.isPresent()) {
            return StructureResult.refused(StructureTexts.IMMOVABLE.with(stays.get().block().toString(),
                    stays.get().at().toShortString()));
        }
        final int minY = Mth.floor(at.y);
        if (level.isOutsideBuildHeight(minY) || level.isOutsideBuildHeight(minY + size.getY() - 1)) {
            return StructureResult.refused(StructureTexts.BORN_OUT_OF_WORLD.text());
        }
        if (maker != null && !PlayerLimits.take(level.getServer(), maker, PlayerLimits.MOVING_STRUCTURES)) {
            return StructureResult.refused(PlayerLimits.reached(level.getServer(), maker,
                    PlayerLimits.MOVING_STRUCTURES));
        }
        final StructureUuid id = StructureUuid.random();
        final int plot;
        try {
            plot = StructureYard.take(level, id);
        } catch (final IllegalStateException full) {
            if (maker != null) {
                PlayerLimits.giveBack(level.getServer(), maker, PlayerLimits.MOVING_STRUCTURES);
            }
            return StructureResult.refused(StructureTexts.YARD_FULL.text());
        }
        final BlockPos corner = new BlockPos(YardLayout.centreX(plot) - size.getX() / 2, minY,
                YardLayout.centreZ(plot) - size.getZ() / 2);
        final Box bounds = new Box(corner.getX(), minY, corner.getZ(), corner.getX() + size.getX() - 1,
                minY + size.getY() - 1, corner.getZ() + size.getZ() - 1);
        StructureYard.hold(level, bounds);
        final MovingStructure structure = MovingStructures.make(level, id, plot, bounds, count,
                corner.getX() + size.getX() / 2.0, minY, corner.getZ() + size.getZ() / 2.0,
                StructurePose.at(at.x, at.y, at.z), null);
        structure.born(new StructureBirth(template, corner));
        if (maker != null) {
            structure.madeBy(maker);
        }
        return StructureResult.done(structure, count);
    }

    private static StructureResult tooMany(final int count) {
        return StructureResult.refused(StructureTexts.TOO_MANY.with(StructureBalance.mostBlocks(), count));
    }

    /*
     * The refusal of a place another mod's moving structure keeps its blocks in, or null: one kind is never made inside
     * the other. Asked before whether the place is loaded, since such a mod may keep them in chunks of its own the game
     * does not count as loaded.
     */
    private static @Nullable StructureResult keptByAnother(final ServerLevel level, final BlockPos at) {
        final IForeignKind theirs = ForeignMovers.holding(level, at);
        return theirs == null ? null : StructureResult.refused(StructureTexts.FOREIGN.with(theirs.name()));
    }
}
