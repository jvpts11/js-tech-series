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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Breaking the rest of an area or a vein with the block a tool breaks. The blocks taken are those of the area of the
 * face the player broke into, or of the vein, that the tool is made for (it mines them faster than a hand, or they
 * need it for their drops), that are no harder than the block broken by more than the server's margin, and that the
 * player may break where they stand: each is broken as the player would break it, through the game, so its drops,
 * the tool's wear, its experience and every protection's say come as for a block broken by hand. It stops when the
 * tool wears out or runs out of what it works on, and never takes more than the server allows at once.
 */
public final class AreaMining {

    /* Who is breaking an area now, so the breaks of its blocks do not break areas of their own. */
    private static final Set<UUID> BREAKING = new HashSet<>();
    /* The namespace and folder of the common ore tags, by which two blocks are of one ore. */
    private static final String COMMON = "c";
    private static final String ORES = "ores/";

    private AreaMining() {
    }

    /**
     * The blocks besides {@code origin} that {@code tool} takes, broken into through {@code face} by {@code player},
     * in the order they break: nearest first. Whether the player may break each where it is is weighed as it breaks,
     * since only the server knows the claims.
     */
    public static List<BlockPos> around(final Level level, final Player player, final ItemStack tool,
                                        final BlockPos origin, final Direction face, final MiningShape shape) {
        final BlockState aimed = level.getBlockState(origin);
        final float hardness = aimed.getDestroySpeed(level, origin);
        if (aimed.isAir() || hardness < 0) {
            return List.of();
        }
        final int most = Math.min(shape.most(), ToolBalance.mostBroken()) - 1;
        final List<BlockPos> taken = switch (shape) {
            case MiningShape.Area area -> area(level, player, tool, origin, face, area, hardness);
            case MiningShape.Vein vein -> vein(level, tool, origin, aimed, hardness, most);
        };
        return taken.size() > most ? List.copyOf(taken.subList(0, Math.max(0, most))) : taken;
    }

    /** The face of {@code pos} that {@code player} is looking at, or the face nearest their eyes. */
    public static Direction face(final Player player, final BlockPos pos) {
        final HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                && block.getBlockPos().equals(pos)) {
            return block.getDirection();
        }
        final Vec3 fromBlock = player.getEyePosition().subtract(Vec3.atCenterOf(pos));
        return Direction.getNearest(fromBlock.x, fromBlock.y, fromBlock.z);
    }

    /* A player broke a block: with a tool that works and mines an area or a vein, the rest of it goes too. */
    static void onBreak(final BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || BREAKING.contains(player.getUUID())) {
            return;
        }
        final ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || !Tools.works(level, tool)) {
            return;
        }
        final boolean known = Tools.spec(tool).isPresent();
        final Optional<MiningShape> shape = Tools.mines(level, tool);
        if (!known && shape.isEmpty()) {
            return;
        }
        final List<BlockPos> rest = shape.map(mined -> around(level, player, tool, event.getPos(),
                face(player, event.getPos()), mined)).orElse(List.of());
        Tools.spend(player, tool);
        if (!rest.isEmpty()) {
            breakAll(level, player, tool, rest);
        }
    }

    /* Breaks {@code rest} as {@code player} would, one by one, while the tool lasts and works. */
    private static void breakAll(final ServerLevel level, final ServerPlayer player, final ItemStack tool,
                                 final List<BlockPos> rest) {
        BREAKING.add(player.getUUID());
        try {
            final Actor actor = Actor.of(player);
            for (final BlockPos pos : rest) {
                if (player.getMainHandItem() != tool || tool.isEmpty() || !Tools.works(level, tool)) {
                    return;
                }
                final BlockState state = level.getBlockState(pos);
                if (state.isAir() || !Protection.may(actor, ProtectedAction.BREAK, level, pos)) {
                    continue;
                }
                if (player.gameMode.destroyBlock(pos)) {
                    /* The others near saw it break as the game showed them; the player's own game did not. */
                    player.connection.send(new ClientboundLevelEventPacket(LevelEvent.PARTICLES_DESTROY_BLOCK, pos,
                            Block.getId(state), false));
                    Tools.spend(player, tool);
                }
            }
        } finally {
            BREAKING.remove(player.getUUID());
        }
    }

    private static List<BlockPos> area(final Level level, final Player player, final ItemStack tool,
                                       final BlockPos origin, final Direction face, final MiningShape.Area area,
                                       final float hardness) {
        final Direction into = face.getOpposite();
        final Direction up;
        final Direction right;
        if (face.getAxis().isHorizontal()) {
            up = Direction.UP;
            right = into.getClockWise();
        } else {
            up = player.getDirection();
            right = up.getClockWise();
        }
        final List<BlockPos> out = new ArrayList<>();
        for (int d = 0; d < area.depth(); d++) {
            for (int v = -(area.height() - 1) / 2; v <= area.height() / 2; v++) {
                for (int u = -(area.width() - 1) / 2; u <= area.width() / 2; u++) {
                    final BlockPos pos = origin.relative(into, d).relative(up, v).relative(right, u);
                    if (!pos.equals(origin) && takes(level, tool, pos, hardness)) {
                        out.add(pos);
                    }
                }
            }
        }
        out.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        return out;
    }

    private static List<BlockPos> vein(final Level level, final ItemStack tool, final BlockPos origin,
                                       final BlockState aimed, final float hardness, final int most) {
        final List<TagKey<Block>> ores = aimed.getTags()
                .filter(tag -> COMMON.equals(tag.location().getNamespace())
                        && tag.location().getPath().startsWith(ORES))
                .toList();
        final List<BlockPos> out = new ArrayList<>();
        final Set<BlockPos> seen = new HashSet<>();
        final Deque<BlockPos> open = new ArrayDeque<>();
        seen.add(origin);
        open.add(origin);
        while (!open.isEmpty() && out.size() < most) {
            final BlockPos at = open.poll();
            for (final BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                if (out.size() >= most) {
                    break;
                }
                final BlockPos pos = next.immutable();
                if (!seen.add(pos)) {
                    continue;
                }
                final BlockState state = level.getBlockState(pos);
                if (sameVein(aimed, ores, state) && takes(level, tool, pos, hardness)) {
                    out.add(pos);
                    open.add(pos);
                }
            }
        }
        return out;
    }

    /* Whether {@code state} is of the vein of {@code aimed}: the same block, or of one of its ores. */
    private static boolean sameVein(final BlockState aimed, final List<TagKey<Block>> ores, final BlockState state) {
        if (state.is(aimed.getBlock())) {
            return true;
        }
        for (final TagKey<Block> ore : ores) {
            if (state.is(ore)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Whether {@code tool} takes the block at {@code pos}, the block broken being of {@code hardness}: a block it is
     * made for, not far harder, and no liquid.
     */
    private static boolean takes(final Level level, final ItemStack tool, final BlockPos pos, final float hardness) {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        final float its = state.getDestroySpeed(level, pos);
        if (its < 0 || its > hardness + ToolBalance.hardnessMargin()) {
            return false;
        }
        return tool.getDestroySpeed(state) > 1.0F || tool.isCorrectToolForDrops(state);
    }
}
