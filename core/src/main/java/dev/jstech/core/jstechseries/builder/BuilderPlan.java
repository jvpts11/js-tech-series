/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.BuildOrder;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.MultiblockBlocks;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PatternById;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.Quota;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * What a Multiblock Builder makes of the terminal it is aimed at, worked out alike on either side from the world
 * that side holds: the terminal's own shape at its least size, turned the way the terminal faces; what of it is in
 * place, missing and wrong; the places still to fill and the blocks that fit each; the places that take several blocks,
 * grouped; and what building it takes from the player's source.
 *
 * @param name    what the multiblock is called
 * @param preview its places, turned as the terminal faces
 * @param facing  the way the terminal faces
 * @param view    what of it is in place, missing and wrong
 * @param wants   the places still to fill, the bottom layer first, each with the blocks that fit it
 * @param groups  the places that take several blocks, grouped by the blocks they take
 * @param order   what building it takes from the source, each place's block chosen
 */
public record BuilderPlan(Text name, ShapePreview preview, Direction facing, ProjectorView view,
                          List<BuildOrder.Want> wants, List<Group> groups, BuildOrder order) {

    public BuilderPlan {
        wants = List.copyOf(wants);
        groups = List.copyOf(groups);
    }

    /**
     * What the builder set as {@code state}, held by {@code player}, makes of the terminal at {@code terminal} in
     * {@code level}; none where no terminal of a multiblock stands, or its shape cannot be laid out.
     */
    public static Optional<BuilderPlan> of(final Level level, final BlockPos terminal, final Player player,
                                           final BuilderState state) {
        if (!(level.getBlockEntity(terminal) instanceof MachineBlockEntity machine)) {
            return Optional.empty();
        }
        final MachineStructure structure = machine.structure();
        if (structure == null) {
            return Optional.empty();
        }
        final IStructureShape shape = structure.shape();
        final Optional<ShapePreview> laid = shape.preview(0);
        if (laid.isEmpty()) {
            return Optional.empty();
        }
        final BlockState standing = level.getBlockState(terminal);
        final Direction facing = standing.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? standing.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        final ShapePreview preview = laid.get().withTerminal(MultiblockBlocks.idOf(standing.getBlock()))
                .turned(orientation(facing));
        final ProjectorView view = ProjectorView.of(level, preview, terminal);
        final List<BuildOrder.Want> wants = new ArrayList<>();
        final Map<String, Group> groups = new LinkedHashMap<>();
        for (final ProjectorView.Place place : view.places()) {
            if (place.state() != ProjectorView.State.MISSING || place.ghost().terminal()) {
                continue;
            }
            final List<String> fits = new ArrayList<>();
            for (final Block block : MultiblockBlocks.choicesOf(place.ghost())) {
                if (block.asItem() != Items.AIR) {
                    fits.add(MultiblockBlocks.idOf(block));
                }
            }
            if (fits.isEmpty()) {
                continue;
            }
            final BuildOrder.Want want = new BuildOrder.Want(place.pos().getX(), place.pos().getY(),
                    place.pos().getZ(), fits);
            wants.add(want);
            if (fits.size() > 1) {
                groups.merge(want.key(), new Group(want.key(), fits, 1),
                        (known, more) -> new Group(known.key(), known.fits(), known.places() + 1));
            }
        }
        final IBuildSource source = BuildSources.get(state.source());
        final Map<String, Integer> held = new HashMap<>();
        final List<Quota> quotas = shape.quotas();
        final BuildOrder order = BuildOrder.plan(wants, state.chosen(), block -> held.computeIfAbsent(block, id -> {
            final Block known = MultiblockBlocks.blockOf(id);
            return known == null ? 0 : source.count(player, known.asItem());
        }), quotas, standing(level, view, quotas));
        return Optional.of(new BuilderPlan(nameOf(shape, standing), preview, facing, view, wants,
                List.copyOf(groups.values()), order));
    }

    /** How many places already hold the right block, its terminal's apart. */
    public int already() {
        return (int) this.view.places().stream().filter(place -> !place.ghost().terminal()
                && place.state() == ProjectorView.State.PLACED).count();
    }

    /** How many blocks the source holds of {@code block}, by id, for {@code player}. */
    public static int has(final Player player, final BuilderState state, final String block) {
        final Block known = MultiblockBlocks.blockOf(block);
        return known == null ? 0 : BuildSources.get(state.source()).count(player, known.asItem());
    }

    /* How many of each count of {@code quotas} already stand in their places, a merged block as the block it was. */
    private static Map<String, Integer> standing(final Level level, final ProjectorView view,
                                                 final List<Quota> quotas) {
        final Map<String, Integer> standing = new HashMap<>();
        if (quotas.isEmpty()) {
            return standing;
        }
        for (final ProjectorView.Place place : view.places()) {
            if (place.state() != ProjectorView.State.PLACED || place.ghost().terminal()) {
                continue;
            }
            final BlockState there = FormedPartBlock.originalAt(level, place.pos(), level.getBlockState(place.pos()));
            final String id = MultiblockBlocks.idOf(there.getBlock());
            for (final Quota quota : quotas) {
                if (quota.counts().matches(id)) {
                    standing.merge(quota.name(), 1, Integer::sum);
                }
            }
        }
        return standing;
    }

    /* The turn of a shape drawn facing north that stands facing {@code facing}. */
    private static Orientation orientation(final Direction facing) {
        return switch (facing) {
            case EAST -> Orientation.EAST;
            case SOUTH -> Orientation.SOUTH;
            case WEST -> Orientation.WEST;
            default -> Orientation.NORTH;
        };
    }

    /* What the multiblock is called: the name of the multiblock its shape is, else its terminal's. */
    private static Text nameOf(final IStructureShape shape, final BlockState terminal) {
        if (shape instanceof PatternById byId) {
            return Projectables.name(byId.id());
        }
        for (final Map.Entry<ResourceLocation, IStructureShape> shown : Projectables.all().entrySet()) {
            if (shown.getValue() == shape) {
                return Projectables.name(shown.getKey());
            }
        }
        return GameText.of(terminal.getBlock().getName());
    }

    /**
     * The places that take the same several blocks.
     *
     * @param key    what their choice is kept under
     * @param fits   the ids of the blocks that fit them, in the shape's order
     * @param places how many places they are
     */
    public record Group(String key, List<String> fits, int places) {

        public Group {
            fits = List.copyOf(fits);
        }
    }
}
