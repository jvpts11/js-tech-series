/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.payload.IPayloadGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Every mod's manuals on the game's lectern, as its books go: a manual used on an empty lectern is laid on it, and
 * whoever uses the lectern then reads the manual, with a strip under it saying where the lectern is and the buttons to
 * take the manual and to close it, as the game's own lectern has. Breaking the lectern drops the manual, as it drops a
 * book.
 */
public final class ManualLecterns {

    private static final String NETWORK_VERSION = "1";
    /* How far from the lectern a player may be to take the manual off it. */
    private static final double REACH = 8.0;

    private ManualLecterns() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, ManualLecterns::onRightClick);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            event.registrar(NETWORK_VERSION).playToClient(LecternManualPayload.TYPE, LecternManualPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> GuideHooks.openOnLectern(payload.manual(),
                            payload.pos())));
            ClientPayloads.accept(event.registrar(NETWORK_VERSION), LecternTakePayload.TYPE,
                    LecternTakePayload.STREAM_CODEC, IPayloadGate.near(LecternTakePayload::pos, REACH),
                    (payload, player, level) -> takeOff(player, level, payload.pos()));
        });
    }

    /**
     * A manual used on an empty lectern goes on it, on both sides, so the player's game neither opens the manual in
     * the hand; a lectern holding a manual opens it on the player's screen, which the server alone knows it holds.
     */
    public static void onRightClick(final PlayerInteractEvent.RightClickBlock event) {
        final Level level = event.getLevel();
        final BlockPos pos = event.getPos();
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LecternBlock)) {
            return;
        }
        final ItemStack held = event.getItemStack();
        if (!state.getValue(LecternBlock.HAS_BOOK)) {
            if (held.getItem() instanceof ManualItem && !event.getEntity().isSecondaryUseActive()) {
                if (!level.isClientSide()) {
                    LecternBlock.tryPlaceBook(event.getEntity(), level, pos, state, held);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            }
            return;
        }
        // Sneaking with something in hand, the game leaves the lectern alone, and so does this.
        if (level.isClientSide() || event.getEntity().isSecondaryUseActive() && !held.isEmpty()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)
                || !(lectern.getBook().getItem() instanceof ManualItem manual)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        PacketDistributor.sendToPlayer(player, new LecternManualPayload(pos, manual.manual()));
    }

    /**
     * Take, pressed on the strip: the manual leaves the lectern at {@code pos} for the player's inventory, or their
     * feet, as the game's book does; nothing for a player who may not build, or a lectern holding no manual.
     *
     * @return whether the manual was taken
     */
    public static boolean takeOff(final ServerPlayer player, final ServerLevel level, final BlockPos pos) {
        if (!player.mayBuild() || !(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)
                || !(lectern.getBook().getItem() instanceof ManualItem)) {
            return false;
        }
        final ItemStack manual = lectern.getBook().copy();
        lectern.clearContent();
        LecternBlock.resetBookState(player, level, pos, level.getBlockState(pos), false);
        if (!player.getInventory().add(manual)) {
            player.drop(manual, false);
        }
        return true;
    }
}
