/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.machine.MachineBlockEntity;
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
import java.util.Comparator;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * What a Multiblock Builder makes of what it is aimed at, worked out alike on either side from the world that side
 * holds: a terminal's own shape at its least size, standing as the terminal faces, or the multiblock a Structure
 * Projector shows, where and as it shows it, its terminal to be placed too; what of it is in place, missing and wrong;
 * the places still to fill and the blocks that fit each; the places that take several blocks, grouped; the blocks to
 * take out first; and what building it takes from the player's source.
 *
 * @param name    what the multiblock is called
 * @param preview its places, turned as it stands
 * @param facing  the way its terminal faces, or is to face once placed
 * @param view    what of it is in place, missing and wrong
 * @param wants   the places still to fill, the bottom layer first and the terminal's last, each with the blocks that
 *                fit it
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
     * What the builder set as {@code state}, held by {@code player}, makes of what stands at {@code target} in
     * {@code level}: the terminal of a multiblock, or a Structure Projector showing one; none for anything else, or a
     * shape that cannot be laid out.
     */
    public static Optional<BuilderPlan> of(final Level level, final BlockPos target, final Player player,
                                           final BuilderState state) {
        return aimed(level, target).map(aimed -> plan(level, aimed, player, state));
    }

    /**
     * Whether the builder can be aimed at what stands at {@code at}: a multiblock's terminal, or a projector showing
     * one.
     */
    public static boolean aimable(final Level level, final BlockPos at) {
        final BlockEntity entity = level.getBlockEntity(at);
        return entity instanceof MachineBlockEntity machine && machine.structure() != null
                || entity instanceof StructureProjectorBlockEntity projector && projector.shape().isPresent();
    }

    /** How many places already hold the right block, its terminal's apart. */
    public int already() {
        return (int) this.view.places().stream().filter(place -> !place.ghost().terminal()
                && place.state() == ProjectorView.State.PLACED).count();
    }

    /**
     * The blocks to take out before it is built, the highest first, so nothing above falls in: whatever stands where
     * it must be left empty, and any other block standing in one of its places.
     */
    public List<ProjectorView.Place> toTakeOut() {
        final List<ProjectorView.Place> out = new ArrayList<>();
        for (final List<ProjectorView.Place> kind : List.of(this.view.clears(), this.view.places())) {
            for (final ProjectorView.Place place : kind) {
                if (place.state() == ProjectorView.State.WRONG) {
                    out.add(place);
                }
            }
        }
        out.sort(Comparator.comparingInt((ProjectorView.Place place) -> place.pos().getY()).reversed());
        return out;
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

    /*
     * What the builder aimed at {@code target} builds: the multiblock a projector shows, where and as it shows it, its
     * terminal placed too; or a terminal's, standing as the terminal faces, which says how the whole stands.
     */
    private static Optional<Aimed> aimed(final Level level, final BlockPos target) {
        final BlockEntity entity = level.getBlockEntity(target);
        if (entity instanceof StructureProjectorBlockEntity projector) {
            final Optional<IStructureShape> shape = projector.shape();
            final Optional<ShapePreview> preview = projector.preview();
            final Optional<ResourceLocation> id = projector.settings().chosen();
            if (shape.isEmpty() || preview.isEmpty() || id.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Aimed(shape.get(), preview.get(), projector.terminalOf(preview.get()),
                    facing(projector.orientation(), shape.get()), Projectables.name(id.get()), true));
        }
        if (!(entity instanceof MachineBlockEntity machine) || machine.structure() == null) {
            return Optional.empty();
        }
        final IStructureShape shape = machine.structure().shape();
        final Optional<ShapePreview> laid = shape.preview(0);
        if (laid.isEmpty()) {
            return Optional.empty();
        }
        final BlockState standing = level.getBlockState(target);
        final Direction facing = standing.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? standing.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        final Orientation stands = Orientation.turns().stream().filter(turn -> facing(turn, shape) == facing)
                .findFirst().orElse(Orientation.NORTH);
        return Optional.of(new Aimed(shape, laid.get().withTerminal(MultiblockBlocks.idOf(standing.getBlock()))
                .turned(stands), target, facing, nameOf(shape, standing), false));
    }

    /* The plan of {@code aimed}: what is missing and wrong, and what building it takes from the player's source. */
    private static BuilderPlan plan(final Level level, final Aimed aimed, final Player player,
                                    final BuilderState state) {
        final ProjectorView view = ProjectorView.of(level, aimed.preview(), aimed.terminal());
        final List<BuildOrder.Want> wants = new ArrayList<>();
        final Map<String, Group> groups = new LinkedHashMap<>();
        BuildOrder.Want terminal = null;
        for (final ProjectorView.Place place : view.places()) {
            if (place.state() != ProjectorView.State.MISSING || place.ghost().terminal() && !aimed.placesTerminal()) {
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
            if (place.ghost().terminal()) {
                terminal = want;
                continue;
            }
            wants.add(want);
            if (fits.size() > 1) {
                groups.merge(want.key(), new Group(want.key(), fits, 1),
                        (known, more) -> new Group(known.key(), known.fits(), known.places() + 1));
            }
        }
        // The terminal goes in last, as a player builds a multiblock, the whole there for it to form.
        if (terminal != null) {
            wants.add(terminal);
        }
        final IBuildSource source = BuildSources.get(state.source());
        final Map<String, Integer> held = new HashMap<>();
        final List<Quota> quotas = aimed.shape().quotas();
        final BuildOrder order = BuildOrder.plan(wants, state.chosen(), block -> held.computeIfAbsent(block, id -> {
            final Block known = MultiblockBlocks.blockOf(id);
            return known == null ? 0 : source.count(player, known.asItem());
        }), quotas, standing(level, view, quotas));
        return new BuilderPlan(aimed.name(), aimed.preview(), aimed.facing(), view, wants,
                List.copyOf(groups.values()), order);
    }

    /* The way the terminal of {@code shape} faces when the shape stands as {@code stands} says. */
    private static Direction facing(final Orientation stands, final IStructureShape shape) {
        final int[] drawn = shape.terminalFaces().apply(0, 0, -1);
        final int[] standing = stands.apply(drawn[0], drawn[1], drawn[2]);
        final Direction facing = Direction.fromDelta(standing[0], standing[1], standing[2]);
        return facing == null ? Direction.NORTH : facing;
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

    /**
     * What the builder is aimed at, laid out: the shape, its places as it stands, where its terminal stands and which
     * way it faces, what it is called, and whether the builder places its terminal too.
     */
    private record Aimed(IStructureShape shape, ShapePreview preview, BlockPos terminal, Direction facing, Text name,
                         boolean placesTerminal) {
    }
}
