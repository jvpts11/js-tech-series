/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.cannon.machine.HostGateway;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.CcFilesPayload;
import dev.jstech.computers.operation.payload.RequestCcFilesPayload;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The two ends of looking into a ComputerCraft computer's folders from the explorer.
 *
 * <p>The asking is only allowed from a player who is actually at that machine's desktop: the message
 * says which machine it is about, and it is answered only if that is the machine whose desktop the
 * player has open. Anything else is a message that did not come from the screen it claims to come from,
 * and is dropped without a word.
 */
public final class GatewayBrowseHandler {

    private GatewayBrowseHandler() {
    }

    /** A player asking what one of their computers holds. */
    public static void requested(final RequestCcFilesPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.level() instanceof ServerLevel level)
                    || !(player.containerMenu instanceof DesktopMenu desktop)
                    || !payload.hostPos().equals(desktop.hostPos())
                    || !(level.getBlockEntity(payload.hostPos()) instanceof BlockEntity host)) {
                return;
            }
            final List<NetworkGatewayBlockEntity> mine = HostGateway.gatewaysOf(host);
            if (mine.isEmpty()) {
                say(player, payload, "this computer has no Gateway");
                return;
            }
            final String refused = GatewayBrowse.ask(mine.getFirst(), player, payload.computer(), payload.path());
            if (!refused.isEmpty()) {
                say(player, payload, refused);
            }
        });
    }

    /** What came back, on its way into the explorer. */
    public static void listed(final CcFilesPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> dev.jstech.computers.client.os.FilesApps.acceptCc(payload));
    }

    private static void say(final ServerPlayer player, final RequestCcFilesPayload asked, final String why) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new CcFilesPayload(asked.computer(), asked.path(), List.of(), why));
    }
}
