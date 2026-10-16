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
import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.MultiblockBlocks;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PatternById;
import dev.jstech.core.multiblock.Projectables;
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
 * What a Multiblock Builder makes of the controller it is aimed at, worked out alike on either side from the world
 * that side holds: the controller's own shape at its least size, turned the way the controller faces; what of it is in
 * place, missing and wrong; the places still to fill and the blocks that fit each; the places that take several blocks,
 * grouped; and what building it takes from the player's source.
 *
 * @param name    what the multiblock is called
 * @param preview its places, turned as the controller faces
 * @param facing  the way the controller faces
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
     * What the builder set as {@code state}, held by {@code player}, makes of the controller at {@code controller} in
     * {@code level}; none where no controller of a multiblock stands, or its shape cannot be laid out.
     */
    public static Optional<BuilderPlan> of(final Level level, final BlockPos controller, final Player player,
                                           final BuilderState state) {
        if (!(level.getBlockEntity(controller) instanceof MachineBlockEntity machine)) {
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
        final BlockState standing = level.getBlockState(controller);
        final Direction facing = standing.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? standing.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        final ShapePreview preview = laid.get().withController(MultiblockBlocks.idOf(standing.getBlock()))
                .turned(orientation(facing));
        final ProjectorView view = ProjectorView.of(level, preview, controller);
        final List<BuildOrder.Want> wants = new ArrayList<>();
        final Map<String, Group> groups = new LinkedHashMap<>();
        for (final ProjectorView.Place place : view.places()) {
            if (place.state() != ProjectorView.State.MISSING || place.ghost().controller()) {
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
        final BuildOrder order = BuildOrder.plan(wants, state.chosen(), block -> held.computeIfAbsent(block, id -> {
            final Block known = MultiblockBlocks.blockOf(id);
            return known == null ? 0 : source.count(player, known.asItem());
        }));
        return Optional.of(new BuilderPlan(nameOf(shape, standing), preview, facing, view, wants,
                List.copyOf(groups.values()), order));
    }

    /** How many places already hold the right block, its controller's apart. */
    public int already() {
        return (int) this.view.places().stream().filter(place -> !place.ghost().controller()
                && place.state() == ProjectorView.State.PLACED).count();
    }

    /** How many blocks the source holds of {@code block}, by id, for {@code player}. */
    public static int has(final Player player, final BuilderState state, final String block) {
        final Block known = MultiblockBlocks.blockOf(block);
        return known == null ? 0 : BuildSources.get(state.source()).count(player, known.asItem());
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

    /* What the multiblock is called: the name of the multiblock its shape is, else its controller's. */
    private static Text nameOf(final IStructureShape shape, final BlockState controller) {
        if (shape instanceof PatternById byId) {
            return Projectables.name(byId.id());
        }
        for (final Map.Entry<ResourceLocation, IStructureShape> shown : Projectables.all().entrySet()) {
            if (shown.getValue() == shape) {
                return Projectables.name(shown.getKey());
            }
        }
        return GameText.of(controller.getBlock().getName());
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
