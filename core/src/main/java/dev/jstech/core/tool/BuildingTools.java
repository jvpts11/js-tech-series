/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Building tools at work: used on the face of a block, a tool that builds ({@link BuildMode}) lays out copies of it
 * along a line or over a plane, marks the corners of a region and copies it, or pastes the region it holds. Each block
 * placed comes from the player's supplies ({@link BuildSupplies}), goes only where the player may place it and where
 * nothing stands, is placed as the player would place it (every protection's say comes as for a block placed by
 * hand), and spends a use of the tool. A copy keeps only blocks, never what a block keeps inside.
 */
public final class BuildingTools {

    /** The region a building tool copied. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CopiedRegion>> COPY =
            CoreItems.CONTENT.component("copied_region", CopiedRegion.CODEC, CopiedRegion.STREAM_CODEC);
    /** The first corner of a region a building tool is copying, marked and waiting for the second. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> CORNER =
            CoreItems.CONTENT.component("copy_corner", GlobalPos.CODEC, GlobalPos.STREAM_CODEC);

    private BuildingTools() {
    }

    /** Declares the components building tools keep, from the Core. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * Where a line or a plane laid out from {@code face} of {@code clicked} would place blocks, nearest first: in front
     * of each block like the one clicked in its line or plane, where nothing stands. Whether there are blocks enough,
     * and whether the player may place them, is weighed as they are placed.
     */
    public static List<BlockPos> targets(final Level level, final Player player, final BlockPos clicked,
                                         final Direction face, final BuildMode mode) {
        final int most;
        final List<Direction> steps;
        switch (mode) {
            case BuildMode.Line line -> {
                most = line.most();
                final Direction along = face.getAxis().isHorizontal() ? face.getClockWise() : player.getDirection();
                steps = List.of(along, along.getOpposite());
            }
            case BuildMode.Plane plane -> {
                most = plane.most();
                steps = new ArrayList<>();
                for (final Direction direction : Direction.values()) {
                    if (direction.getAxis() != face.getAxis()) {
                        steps.add(direction);
                    }
                }
            }
            case BuildMode.Copy copy -> {
                return List.of();
            }
            case BuildMode.Paste paste -> {
                return List.of();
            }
        }
        final int cap = Math.min(most, ToolBalance.mostPlaced());
        final Block like = level.getBlockState(clicked).getBlock();
        final List<BlockPos> out = new ArrayList<>();
        final Set<BlockPos> seen = new HashSet<>();
        final Deque<BlockPos> open = new ArrayDeque<>();
        seen.add(clicked);
        open.add(clicked);
        while (!open.isEmpty() && out.size() < cap) {
            final BlockPos at = open.poll();
            final BlockPos front = at.relative(face);
            if (!level.getBlockState(front).canBeReplaced()) {
                continue;
            }
            out.add(front);
            for (final Direction step : steps) {
                final BlockPos next = at.relative(step);
                if (seen.add(next) && level.getBlockState(next).is(like)) {
                    open.add(next);
                }
            }
        }
        return out;
    }

    /**
     * Where pasting {@code copy} from {@code face} of {@code clicked} puts each of its blocks, for a player facing
     * {@code facing}: turned as the player has turned since copying, its lowest corner in front of the face, reaching
     * away from the player, to their right and up; the lowest layer first.
     */
    public static Map<BlockPos, BlockState> pasted(final CopiedRegion copy, final BlockPos clicked,
                                                   final Direction face, final Direction facing) {
        final Rotation rotation = turn(copy.facing(), facing);
        final Map<BlockPos, BlockState> turned = new HashMap<>();
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        final Vec3i size = copy.size();
        for (int y = 0; y < size.getY(); y++) {
            for (int z = 0; z < size.getZ(); z++) {
                for (int x = 0; x < size.getX(); x++) {
                    final BlockPos at = new BlockPos(x, y, z).rotate(rotation);
                    minX = Math.min(minX, at.getX());
                    minZ = Math.min(minZ, at.getZ());
                    final BlockState state = copy.at(x, y, z);
                    if (!state.isAir()) {
                        turned.put(at, state.rotate(rotation));
                    }
                }
            }
        }
        final BlockPos anchor = clicked.relative(face);
        final Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        final int offsetX = minX;
        final int offsetZ = minZ;
        turned.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<BlockPos, BlockState> entry) -> entry.getKey().getY())
                        .thenComparingInt(entry -> entry.getKey().getX())
                        .thenComparingInt(entry -> entry.getKey().getZ()))
                .forEach(entry -> out.put(anchor.offset(entry.getKey().getX() - offsetX, entry.getKey().getY(),
                        entry.getKey().getZ() - offsetZ), entry.getValue()));
        return out;
    }

    /** What a tool holding a copy says of it in its tooltip; nothing for any other stack. */
    public static List<Text> describe(final ItemStack stack) {
        final CopiedRegion copy = stack.get(COPY.get());
        if (copy == null) {
            return List.of();
        }
        return List.of(ToolTexts.HOLDS_COPY.with(copy.count(), copy.size().getX(), copy.size().getY(),
                copy.size().getZ()));
    }

    /* A building tool used on a block: it builds, and the block clicked is not used as well. */
    static void onUse(final PlayerInteractEvent.RightClickBlock event) {
        final ItemStack stack = event.getItemStack();
        final Optional<BuildMode> mode = Tools.builds(stack);
        if (mode.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || !Tools.ready(player, stack)) {
            return;
        }
        final BlockPos clicked = event.getPos();
        final Direction face = event.getFace() != null ? event.getFace() : Direction.UP;
        final boolean placed = switch (mode.get()) {
            case BuildMode.Line line -> extend(level, player, stack, clicked, face, line);
            case BuildMode.Plane plane -> extend(level, player, stack, clicked, face, plane);
            case BuildMode.Copy copy -> {
                copy(level, player, stack, clicked);
                yield false;
            }
            case BuildMode.Paste paste -> paste(level, player, stack, clicked, face);
        };
        if (placed) {
            Tools.startWait(player, stack);
        }
    }

    /* Lays the line or plane out; whether any block was placed. */
    private static boolean extend(final ServerLevel level, final ServerPlayer player, final ItemStack tool,
                                  final BlockPos clicked, final Direction face, final BuildMode mode) {
        final BlockState state = level.getBlockState(clicked);
        boolean any = false;
        for (final BlockPos pos : targets(level, player, clicked, face, mode)) {
            final Placed placed = place(level, player, tool, pos, state, face);
            if (placed == Placed.OUT || placed == Placed.STOPPED) {
                break;
            }
            any |= placed == Placed.DONE;
        }
        return any;
    }

    /* Marks the first corner, or copies the region to the second; sneaking, clears what the tool holds. */
    private static void copy(final ServerLevel level, final ServerPlayer player, final ItemStack tool,
                             final BlockPos clicked) {
        if (player.isShiftKeyDown()) {
            tool.remove(COPY.get());
            tool.remove(CORNER.get());
            tell(player, ToolTexts.CLEARED.text());
            return;
        }
        final GlobalPos first = tool.get(CORNER.get());
        if (first == null || !first.dimension().equals(level.dimension())) {
            tool.set(CORNER.get(), GlobalPos.of(level.dimension(), clicked.immutable()));
            tell(player, first == null ? ToolTexts.CORNER.text() : ToolTexts.OTHER_WORLD.text());
            return;
        }
        final BlockPos low = BlockPos.min(first.pos(), clicked);
        final BlockPos high = BlockPos.max(first.pos(), clicked);
        final Vec3i size = high.subtract(low).offset(1, 1, 1);
        final long volume = CopiedRegion.volume(size);
        if (volume > ToolBalance.mostCopied()) {
            tell(player, ToolTexts.TOO_BIG.with(ToolBalance.mostCopied(), volume));
            return;
        }
        final List<BlockState> palette = new ArrayList<>(List.of(CopiedRegion.air()));
        final Map<BlockState, Integer> known = new HashMap<>(Map.of(CopiedRegion.air(), 0));
        final int[] blocks = new int[(int) volume];
        int i = 0;
        for (int y = 0; y < size.getY(); y++) {
            for (int z = 0; z < size.getZ(); z++) {
                for (int x = 0; x < size.getX(); x++) {
                    final BlockState state = level.getBlockState(low.offset(x, y, z));
                    blocks[i++] = state.isAir() ? 0 : known.computeIfAbsent(state, added -> {
                        palette.add(added);
                        return palette.size() - 1;
                    });
                }
            }
        }
        final CopiedRegion copy = new CopiedRegion(palette, size, blocks, player.getDirection());
        tool.set(COPY.get(), copy);
        tool.remove(CORNER.get());
        tell(player, ToolTexts.COPIED.with(copy.count()));
    }

    /* Pastes the copy the tool holds; whether any block was placed. */
    private static boolean paste(final ServerLevel level, final ServerPlayer player, final ItemStack tool,
                                 final BlockPos clicked, final Direction face) {
        final CopiedRegion copy = tool.get(COPY.get());
        if (copy == null) {
            tell(player, ToolTexts.NOTHING_COPIED.text());
            return false;
        }
        boolean any = false;
        for (final Map.Entry<BlockPos, BlockState> block : pasted(copy, clicked, face, player.getDirection())
                .entrySet()) {
            final Placed placed = place(level, player, tool, block.getKey(), block.getValue(), face);
            if (placed == Placed.STOPPED) {
                break;
            }
            any |= placed == Placed.DONE;
        }
        return any;
    }

    /* Places {@code state} at {@code pos} for {@code player}, from their supplies, as they would by hand. */
    private static Placed place(final ServerLevel level, final ServerPlayer player, final ItemStack tool,
                                final BlockPos pos, final BlockState state, final Direction face) {
        if (player.getMainHandItem() != tool || !Tools.works(level, tool)) {
            return Placed.STOPPED;
        }
        final Item item = state.getBlock().asItem();
        if (item == Items.AIR || !level.getBlockState(pos).canBeReplaced() || !state.canSurvive(level, pos)
                || !level.isUnobstructed(state, pos, CollisionContext.empty())
                || !Protection.may(Actor.of(player), ProtectedAction.PLACE, level, pos)) {
            return Placed.SKIPPED;
        }
        if (!BuildSupplies.takeOne(player, item)) {
            tell(player, ToolTexts.OUT_OF.with(GameText.of(item.getDescription())));
            return Placed.OUT;
        }
        final BlockSnapshot before = BlockSnapshot.create(level.dimension(), level, pos);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (EventHooks.onBlockPlace(player, before, face)) {
            before.restore(Block.UPDATE_ALL);
            BuildSupplies.giveBack(player, item);
            return Placed.SKIPPED;
        }
        Tools.spend(player, tool);
        return Placed.DONE;
    }

    /* The turn from facing {@code from} to facing {@code to}, both across the ground. */
    private static Rotation turn(final Direction from, final Direction to) {
        Direction at = from;
        for (final Rotation rotation : new Rotation[] {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180,
                Rotation.COUNTERCLOCKWISE_90}) {
            if (at == to) {
                return rotation;
            }
            at = at.getClockWise();
        }
        return Rotation.NONE;
    }

    private static void tell(final ServerPlayer player, final Text text) {
        player.displayClientMessage(GameText.component(text), true);
    }

    /* What came of placing one block. */
    private enum Placed {
        /* Placed. */
        DONE,
        /* Not placed there, and the next may be. */
        SKIPPED,
        /* Out of that block: no more of it. */
        OUT,
        /* The tool stopped working or left the hand: nothing more. */
        STOPPED
    }
}
