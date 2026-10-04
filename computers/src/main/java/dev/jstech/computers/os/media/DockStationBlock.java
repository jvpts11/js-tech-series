/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.menu.DockStationMenu;
import dev.jstech.core.content.Device;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Dock Station: a full block on the desk with three trays for disks of any era and the USB port for the flash
 * drive. A click with a disk puts it in the tray its size fits, a click with the stick plugs it in, a sneak-click
 * takes out the last thing put in (the stick first, then the trays from the bottom), and a plain click opens its
 * window, where each tray and the port show what they hold and its letter on the computer.
 */
@TextHolder
public class DockStationBlock extends MediaReaderBlock {

    public static final MapCodec<DockStationBlock> CODEC = simpleCodec(DockStationBlock::new);

    /** The dock's block entity, ticking its link and its letters, with its window as its menu. */
    private static final Device<DockStationBlockEntity> DEVICE =
            Device.of(() -> ComputingModule.DOCK_STATION_BE.get()).ticks(DockStationBlockEntity::dockTick)
                    .opensMenu(DockStationMenu::new);

    private static final TextKey TRAY_TAKEN = TextKey.of("jsc.dock_station.tray_taken",
            "That tray already holds a disk - sneak-click or use the window to take it out.");

    public DockStationBlock(final Properties properties) {
        super(MediaDriveType.DOCK_STATION, properties, DEVICE);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /** The dock is one model drawn by the block entity; the block itself paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldStack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DockStationBlockEntity dock)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                give(player, level, pos, ejectLast(dock));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (DockStationBlockEntity.bayOf(heldStack) < 0) {
            return super.useItemOn(heldStack, state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide()) {
            final ItemStack left = dock.insertDisk(heldStack.copyWithCount(1));
            if (left.isEmpty()) {
                heldStack.shrink(1);
            } else {
                player.displayClientMessage(GameText.component(TRAY_TAKEN), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /* The stick first, as the last thing a player reaches for, then the trays from the bottom up. */
    private static ItemStack ejectLast(final DockStationBlockEntity dock) {
        final ItemStack stick = dock.ejectMedia();
        if (!stick.isEmpty()) {
            return stick;
        }
        for (int bay = DockStationBlockEntity.BAYS - 1; bay >= 0; bay--) {
            final ItemStack disk = dock.ejectDisk(bay);
            if (!disk.isEmpty()) {
                return disk;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void give(final Player player, final Level level, final BlockPos pos, final ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
        }
    }
}
