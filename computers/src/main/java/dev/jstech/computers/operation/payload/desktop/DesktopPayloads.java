/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.SettingsApp;
import dev.jstech.computers.client.os.SystemMonitorApp;
import dev.jstech.computers.client.os.TaskManagerApp;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DesktopBalloonPayload;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.EndProcessPayload;
import dev.jstech.computers.operation.payload.RequestDesktopFilesPayload;
import dev.jstech.computers.operation.payload.RequestSettingsPayload;
import dev.jstech.computers.operation.payload.SetDesktopPrefsPayload;
import dev.jstech.computers.operation.payload.SetIconPositionPayload;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The desktop's payloads: its files and icons, its preferences and settings, and the windows it leaves open. What
 * a desktop is handed when it opens is put together by {@link DesktopListings}, and what the Settings windows read
 * by {@link SettingsSnapshots}.
 */
public final class DesktopPayloads {

    private DesktopPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        registrar.playBidirectional(DesktopWindowsPayload.TYPE, DesktopWindowsPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandlers.onMainThread(DesktopPayloads::handleDesktopWindowsOnClient),
                        ComputerAccess.guarded(DesktopWindowsPayload.TYPE,
                                ComputerAccess.machineOrClosingDesktop(DesktopWindowsPayload::host),
                                DesktopPayloads::handleDesktopWindowsOnServer)));
        ComputerAccess.accept(registrar, RequestDesktopFilesPayload.TYPE, RequestDesktopFilesPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestDesktopFilesPayload::hostPos), DesktopPayloads::handleRequestDesktopFiles);
        registrar.playToClient(DesktopFilesPayload.TYPE, DesktopFilesPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(DesktopPayloads::handleDesktopFiles));
        ComputerAccess.accept(registrar, SetDesktopPrefsPayload.TYPE, SetDesktopPrefsPayload.STREAM_CODEC,
                ComputerAccess.machine(SetDesktopPrefsPayload::hostPos), DesktopPayloads::handleSetDesktopPrefs);
        ComputerAccess.accept(registrar, RequestSettingsPayload.TYPE, RequestSettingsPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestSettingsPayload::hostPos), DesktopPayloads::handleRequestSettings);
        ComputerAccess.accept(registrar, SetSettingPayload.TYPE, SetSettingPayload.STREAM_CODEC,
                ComputerAccess.machine(SetSettingPayload::hostPos), DesktopPayloads::handleSetSetting);
        ComputerAccess.accept(registrar, EndProcessPayload.TYPE, EndProcessPayload.STREAM_CODEC,
                ComputerAccess.machine(EndProcessPayload::hostPos), DesktopPayloads::handleEndProcess);
        registrar.playToClient(SettingsSnapshotPayload.TYPE, SettingsSnapshotPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(DesktopPayloads::handleSettingsSnapshot));
        ComputerAccess.accept(registrar, SetIconPositionPayload.TYPE, SetIconPositionPayload.STREAM_CODEC,
                ComputerAccess.machine(SetIconPositionPayload::hostPos), DesktopPayloads::handleSetIconPosition);
        // A notice the machine raises from the corner of its own desktop, which takes nothing over.
        registrar.playToClient(DesktopBalloonPayload.TYPE, DesktopBalloonPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        DesktopScreen.raise(payload.hostPos(), payload.title(), payload.body(),
                                payload.opens())));
    }

    private static void handleRequestDesktopFiles(final RequestDesktopFilesPayload payload, final ServerPlayer player,
                                                  final ServerLevel level) {
        final IOsHost shown = level.getBlockEntity(payload.hostPos()) instanceof IOsHost machine ? machine : null;
        /*
         * The machine's open windows travel with the desktop listing, so the desktop that is opening
         * restores them from the machine and not from a cache in this client.
         */
        PacketDistributor.sendToPlayer(player, DesktopWindowsPayload.of(payload.hostPos(),
                shown == null ? List.of() : shown.openWindows(), shown == null ? 0 : shown.desktopWorkspace()));
        final IComputerTerminalHost terminal =
                level.getBlockEntity(payload.hostPos()) instanceof IComputerTerminalHost host ? host : null;
        PacketDistributor.sendToPlayer(player, DesktopListings.of(shown, terminal));
    }

    private static void handleSetDesktopPrefs(final SetDesktopPrefsPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            computer.console().desktop().setWallpaper(payload.wallpaper());
            computer.console().setComputerName(payload.computerName());
            computer.setChanged();
        }
    }

    private static void handleRequestSettings(final RequestSettingsPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            PacketDistributor.sendToPlayer(player, SettingsSnapshots.of(computer, payload.hostPos()));
        }
    }

    private static void handleEndProcess(final EndProcessPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos())
                instanceof AbstractComputerBlockEntity computer
                && computer.programs().stop(payload.id())) {
            JscEvents.award(player, JscEvents.TASK_ENDED);
            computer.setChanged();
            PacketDistributor.sendToPlayer(player, SettingsSnapshots.of((IOsHost) computer, payload.hostPos()));
        }
    }

    private static void handleSetSetting(final SetSettingPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer
                && computer instanceof IComputerTerminalHost host) {
            /*
             * Route through the same setConfig the MC-DOS 'config' command uses, so both front-ends
             * clamp and persist identically.
             */
            new ServerCliComputer(host, level)
                    .setConfig(payload.key(), payload.value());
            PacketDistributor.sendToPlayer(player, SettingsSnapshots.of(computer, payload.hostPos()));
        }
    }

    private static void handleSettingsSnapshot(final SettingsSnapshotPayload payload, final Player player) {
        SettingsApp.accept(payload);
        SystemMonitorApp.accept(payload);
        TaskManagerApp.accept(payload);
    }

    private static void handleSetIconPosition(final SetIconPositionPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            computer.console().desktop().setIconCell(payload.iconKey(), payload.cell());
            computer.setChanged();
        }
    }

    private static void handleDesktopFiles(final DesktopFilesPayload payload, final Player player) {
        DesktopScreen.acceptDesktop(payload);
    }

    /** A player left the monitor: the layout they left behind becomes the machine's. */
    private static void handleDesktopWindowsOnServer(final DesktopWindowsPayload payload, final ServerPlayer player,
                                                     final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IOsHost computer
                && computer.isRunning()) {
            /*
             * A machine that has since been switched off or restarted keeps its empty desktop: the
             * layout in flight belongs to a session that no longer exists. The machine keeps only the
             * windows its RAM holds: a client that claims more than fits is trimmed to what does.
             */
            if (!computer.needsPost()) {
                computer.setOpenWindows(computer.windowsWithinBudget(payload.toOpenWindows()));
                // Which workspace was up is part of the layout left behind, so it is kept with the windows.
                computer.setDesktopWorkspace(payload.workspace());
            }
        }
    }

    /** The desktop is opening: hand it the windows the machine has. */
    private static void handleDesktopWindowsOnClient(final DesktopWindowsPayload payload, final Player player) {
        DesktopScreen.applyWindows(payload);
    }
}
