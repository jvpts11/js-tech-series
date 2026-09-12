/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.cannon.run.Values;
import dev.jstech.computers.operation.payload.CcFilesPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Looking into a ComputerCraft computer's folders from the file explorer of one of our machines.
 *
 * <p>It cannot be answered while the asking is going on: the folder belongs to another mod's computer,
 * which answers when it gets to it, some ticks later. So the player's question is put across like any
 * other, the explorer is left showing that it is waiting, and what comes back is sent to that player
 * alone. A player who has closed the explorer, walked away or logged out is simply not there to send to,
 * and nothing is kept waiting for them.
 *
 * <p>Reading a folder over there needs the Gateway's file setting, the same as reading a file does: it
 * is the same reaching in, and a Gateway with files off shows nothing rather than a listing.
 */
public final class GatewayBrowse {

    /** How long the player's question is kept, in ticks; a little past what the explorer waits. */
    private static final int WAIT = 120;

    private GatewayBrowse() {
    }

    /**
     * Starts asking that computer what the folder holds.
     *
     * @return why it could not be asked, or an empty text when the asking is under way
     */
    public static String ask(final NetworkGatewayBlockEntity gateway, final ServerPlayer player,
                             final int computer, final String path) {
        if (gateway.permissions().files() == GatewayPermissions.FileAccess.OFF) {
            return "this Gateway has the shared folders off";
        }
        if (computer == 0) {
            /*
             * The computers themselves, which this side already knows: no computer has to be asked
             * anything, so the answer goes back at once rather than through a question and a wait.
             */
            final List<String> named = new ArrayList<>();
            for (final NetworkGatewayBlockEntity.AttachedComputer one : gateway.attachedComputers()) {
                if (gateway.hasAgent(one.id())) {
                    named.add(one.id() + "/");
                }
            }
            PacketDistributor.sendToPlayer(player, new CcFilesPayload(0, path, named,
                    named.isEmpty() ? "no ComputerCraft computer with an agent is attached" : ""));
            return "";
        }
        if (!gateway.hasAgent(computer)) {
            return "computer " + computer + " has no agent running";
        }
        final long until = gateway.getLevel() == null ? 0L : gateway.getLevel().getGameTime() + WAIT;
        final GatewayRpcBroker.Waiting who =
                new GatewayRpcBroker.Waiting.ByPlayer(player.getUUID(), path);
        final GatewayRequestId asked = gateway.ask(computer,
                who, "list", List.of(path.isEmpty() ? "/" : path), until);
        return asked == null ? "computer " + computer + " cannot be reached" : "";
    }

    /**
     * The answer, on its way to the player who asked for it.
     *
     * @return whether there was a player still there to send it to
     */
    public static boolean answered(final NetworkGatewayBlockEntity gateway,
                                   final GatewayRpcBroker.Waiting.ByPlayer who,
                                   final int computer, final Object value) {
        if (gateway.getLevel() == null) {
            return false;
        }
        final ServerPlayer player = gateway.getLevel().getServer().getPlayerList().getPlayer(who.id());
        if (player == null) {
            return false;
        }
        PacketDistributor.sendToPlayer(player,
                new CcFilesPayload(computer, who.path(), namesOf(value), ""));
        return true;
    }

    /** Tells the player that nothing came back, so the explorer stops waiting and says so. */
    public static void nothing(final NetworkGatewayBlockEntity gateway,
                               final GatewayRpcBroker.Waiting.ByPlayer who, final int computer,
                               final String why) {
        if (gateway.getLevel() == null) {
            return;
        }
        final ServerPlayer player = gateway.getLevel().getServer().getPlayerList().getPlayer(who.id());
        if (player != null) {
            PacketDistributor.sendToPlayer(player, new CcFilesPayload(computer, who.path(), List.of(), why));
        }
    }

    /* What the agent answered a list with, as names the explorer can show. */
    private static List<String> namesOf(final Object value) {
        final List<String> names = new ArrayList<>();
        if (value instanceof Values.ListValue list) {
            for (final Object one : list.items()) {
                if (names.size() >= CcFilesPayload.NAMES_MAX) {
                    break;
                }
                names.add(String.valueOf(one));
            }
        }
        return names;
    }
}
