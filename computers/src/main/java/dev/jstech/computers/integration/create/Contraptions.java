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
 * where it is set down. One doing what it cannot lose, a copy or an install, is a block Create may not move, as are the
 * big machines (the Mainframe, the Server Rack, the Mining Computer and the WAN Gateway), whose parts find their
 * origin where it stood: on the block that turns or in the way the contraption goes, Create says it cannot assemble,
 * and glued on beside, the machine stays where it is. It names no class of Create's.
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
        if (bigMachine(state.getBlock(), level.getBlockEntity(pos))) {
            return false;
        }
        return !(level instanceof ServerLevel server)
                || !(level.getBlockEntity(pos) instanceof AbstractComputerBlockEntity computer)
                || !computer.heldInPlace(server);
    }

    /** Whether a block entity of that kind is a computer's, which comes down shut down where a contraption sets it. */
    public static boolean keepsComputer(final BlockEntityType<?> kind) {
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

    /** A contraption set the block entity down where it stands now: a computer comes down shut down. */
    public static void setDown(final BlockEntity block) {
        if (block instanceof AbstractComputerBlockEntity computer && block.getLevel() instanceof ServerLevel server) {
            computer.setDownByContraption(server);
        }
    }

    /* A block of one of J's Computers' big machines: its origin, or a part bound to it. */
    private static boolean bigMachine(final Block block, @Nullable final BlockEntity entity) {
        return (block instanceof AbstractBigBlock || entity instanceof BigBlockPartBlockEntity)
                && JsComputers.MODID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }
}
