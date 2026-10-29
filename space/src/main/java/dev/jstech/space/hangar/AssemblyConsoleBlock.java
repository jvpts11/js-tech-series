/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.text.GameText;
import dev.jstech.space.config.SpaceServerConfig;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Assembly Console, set on a Rocket Hangar's floor, where a rocket is designed beside it: used, it opens the design
 * screen of the hangar whose bay it stands in, as the hangar's terminal does, or says it stands in none.
 */
public final class AssemblyConsoleBlock extends Block {

    public AssemblyConsoleBlock(final Properties properties) {
        super(properties);
    }

    /**
     * The terminal of the formed hangar whose bay holds {@code pos}, among the terminals in the loaded chunks as far
     * round as the widest bay reaches.
     */
    public static Optional<HangarTerminalBlockEntity> hangarOf(final Level level, final BlockPos pos) {
        final int reach = SectionPos.blockToSectionCoord(SpaceServerConfig.FILE.get(SpaceServerConfig.HANGAR_WIDEST))
                + 1;
        final int cx = SectionPos.blockToSectionCoord(pos.getX());
        final int cz = SectionPos.blockToSectionCoord(pos.getZ());
        for (int x = cx - reach; x <= cx + reach; x++) {
            for (int z = cz - reach; z <= cz + reach; z++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (final BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof HangarTerminalBlockEntity terminal
                            && terminal.bay().filter(bay -> bay.holds(pos)).isPresent()) {
                        return Optional.of(terminal);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer designer) {
            final Optional<HangarTerminalBlockEntity> terminal = hangarOf(level, pos);
            if (terminal.isPresent()) {
                RocketDesigner.open(designer, terminal.get());
            } else {
                player.displayClientMessage(GameText.component(HangarTexts.NO_HANGAR), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
