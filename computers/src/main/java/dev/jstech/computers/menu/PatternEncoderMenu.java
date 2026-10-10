/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.PatternEncoderBlock;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.gui.layout.PatternEncoderLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The Pattern Encoder's bay panel: the one media slot, the player's inventory, and two buttons (eject, cancel the
 * queue). Everything the panel displays about the job comes off the block entity, which the server keeps synced; the
 * authoring itself happens on the linked computer's Pattern Studio.
 *
 * <p>Eject is the encoder's own eject button: on an encoder with a disc tray it opens and closes the tray, and a disc
 * goes into the slot or comes out of it only while the tray is out; on the Vintage encoder it hands the floppy back.
 */
public class PatternEncoderMenu extends CoreMenu {

    private final PatternEncoderBlockEntity blockEntity;

    public static final int BUTTON_EJECT = 0;
    public static final int BUTTON_CANCEL = 1;

    public static final int MEDIA_SLOT = 0;

    public PatternEncoderMenu(final int containerId, final Inventory playerInventory,
                              final PatternEncoderBlockEntity be) {
        super(ComputingMenus.PATTERN_ENCODER_MENU.get(), containerId, playerInventory,
                MenuValidity.block(be.getLevel(), be.getBlockPos(), PatternEncoderBlock.class));
        this.blockEntity = be;
        final GuiLayout layout = PatternEncoderLayout.layout();
        final GuiLayout.SlotPosition mediaAt = layout.slotAt("media");
        final SlotGroup media = slots(new SlotItemHandler(be.media(), MEDIA_SLOT, mediaAt.x(), mediaAt.y()) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return blockEntity.tray().reaches(stack) && super.mayPlace(stack);
            }

            @Override
            public boolean mayPickup(final Player player) {
                return !blockEntity.locked() && blockEntity.tray().reaches(getItem());
            }
        });
        final PlayerSlots playerSlots = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(media, playerSlots.all());
        shiftClick(playerSlots.all(), media);
        button(BUTTON_EJECT, this::eject);
        button(BUTTON_CANCEL, player -> blockEntity.cancelAll());
    }

    public static PatternEncoderMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                 final RegistryFriendlyByteBuf buf) {
        return new PatternEncoderMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, PatternEncoderBlockEntity.class));
    }

    public PatternEncoderBlockEntity blockEntity() {
        return blockEntity;
    }

    public BlockPos blockEntityPos() {
        return blockEntity.getBlockPos();
    }

    public ItemStack mediaStack() {
        return slots.get(MEDIA_SLOT).getItem();
    }

    /**
     * Presses the eject button of an encoder with a tray; on one without, ejects the medium to the player, unless the
     * bay is locked or empty, dropping it when the player has no room.
     */
    private void eject(final Player player) {
        if (blockEntity.hasTray()) {
            PatternEncoderBlock.pressEjectButton(blockEntity, player);
            return;
        }
        if (blockEntity.locked()) {
            return;
        }
        final ItemStack ejected = blockEntity.ejectMedia();
        if (ejected.isEmpty()) {
            return;
        }
        if (!player.addItem(ejected)) {
            final BlockPos pos = blockEntity.getBlockPos();
            Containers.dropItemStack(player.level(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ejected);
        }
    }
}
