/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The bay door's console, at the end of the control room's row, two blocks wide: its screen draws the stepped door in
 * outline, an arrow up to open it and one down to shut it. Pressed on the upper half of its screen it opens its
 * hangar's door, the lower door and the slot over it together; on the lower half it shuts it. The door opens by
 * itself, too, for a crawler going through it.
 */
public final class DoorConsoleBlock extends HangarFittingBlock {

    private final MapCodec<DoorConsoleBlock> codec;

    /** Which half of the door's picture its screen shows, as seen from its chair; dark until its hangar forms. */
    public static final EnumProperty<HangarConsoleBlock.Screen> SCREEN = EnumProperty.create("screen",
            HangarConsoleBlock.Screen.class, HangarConsoleBlock.Screen.OFF, HangarConsoleBlock.Screen.LEFT,
            HangarConsoleBlock.Screen.RIGHT);
    /* Where on its block the screen's middle is, up from its foot: above it opens, below it shuts. */
    private static final double SCREEN_MIDDLE = 17.0 / 16;

    public DoorConsoleBlock(final BlockBehaviour.Properties properties) {
        super(properties, HangarConsoleBlock.BOXES);
        this.codec = simpleCodec(DoorConsoleBlock::new);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(SCREEN, HangarConsoleBlock.Screen.OFF));
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        final Optional<HangarTerminalBlockEntity> hangar = HangarTerminalBlockEntity.holding(level, pos);
        if (hangar.isEmpty()) {
            player.displayClientMessage(GameText.component(HangarTexts.NO_HANGAR), true);
            return InteractionResult.CONSUME;
        }
        final boolean open = hit.getLocation().y - pos.getY() >= SCREEN_MIDDLE;
        hangar.get().door(open);
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4F, 1.0F);
        player.displayClientMessage(GameText.component(open ? HangarTexts.DOOR_OPENING : HangarTexts.DOOR_SHUTTING),
                true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SCREEN);
    }
}
