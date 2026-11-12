/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.create;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.core.bigblock.AbstractBigBlock;
import dev.jstech.core.bigblock.BigBlockPartBlockEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * How J's Computers' machines ride Create's contraptions, whose blocks leave the world (computers/docs/design/
 * implementation.md, "Create"). A computer rides and comes down shut down cleanly, keeping everything, and stays off
 * where it is set down; one doing what it cannot lose, a copy or an install, is a block Create may not move. The big
 * machines (the Mainframe, the Server Rack, the Mining Computer and the WAN Gateway) ride whole: each of their blocks
 * holds on to the others, so a contraption that takes one takes them all, and each part turns its way to the origin
 * as the contraption turned it. A rack comes down with its bays switched off. It names no class of Create's.
 */
public final class Contraptions {

    /* The kinds of block entity a small computer keeps, any mod's, found once the game has registered them. */
    private static volatile @Nullable Set<BlockEntityType<?>> computerKinds;

    private Contraptions() {
    }

    /**
     * Whether a contraption may take the block at {@code pos} now. Create asks it of every block beside the ones it
     * takes, too, so the answer changes nothing.
     */
    public static boolean mayCarry(final BlockState state, final Level level, final BlockPos pos) {
        return !(level instanceof ServerLevel server)
                || !(level.getBlockEntity(pos) instanceof AbstractComputerBlockEntity computer)
                || !computer.heldInPlace(server);
    }

    /**
     * Whether the blocks at {@code pos} and {@code beside} are of one of J's Computers' big machines, which a
     * contraption carries whole or not at all.
     */
    public static boolean sameMachine(final Level level, final BlockPos pos, final BlockPos beside) {
        final BlockPos origin = originOf(level, pos);
        return origin != null && origin.equals(originOf(level, beside));
    }

    /**
     * Whether a contraption sets down a block entity of that kind through {@link #setDown}: a computer's, any mod's,
     * or one of J's Computers' own, its big machines and their parts among them.
     */
    public static boolean setsDown(final BlockEntityType<?> kind) {
        if (JsComputers.MODID.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(kind).getNamespace())) {
            return true;
        }
        Set<BlockEntityType<?>> known = computerKinds;
        if (known == null) {
            known = new HashSet<>();
            for (final Block block : BuiltInRegistries.BLOCK) {
                if (block instanceof AbstractSmallComputerBlock<?> computer) {
                    known.add(computer.blockEntityType());
                }
            }
            computerKinds = known;
        }
        return known.contains(kind);
    }

    /** A contraption set the block entity down where it stands now: a computer, or a rack's bays, come down off. */
    public static void setDown(final BlockEntity block) {
        if (!(block.getLevel() instanceof ServerLevel server)) {
            return;
        }
        if (block instanceof AbstractComputerBlockEntity computer) {
            computer.setDownByContraption(server);
        } else if (block instanceof ServerRackBlockEntity rack) {
            rack.setDownByContraption();
        }
    }

    /* The origin of the J's Computers big machine whose block stands at {@code pos}; none for any other block. */
    @Nullable
    private static BlockPos originOf(final Level level, final BlockPos pos) {
        final Block block = level.getBlockState(pos).getBlock();
        if (!JsComputers.MODID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) {
            return null;
        }
        if (block instanceof AbstractBigBlock) {
            return pos.immutable();
        }
        return level.getBlockEntity(pos) instanceof BigBlockPartBlockEntity part ? part.originPos() : null;
    }
}
