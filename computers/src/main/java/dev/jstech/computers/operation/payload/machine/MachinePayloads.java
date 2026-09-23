/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.machine;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.block.IKvmScreenOpener;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.os.RemoteControlApp;
import dev.jstech.computers.menu.ClusterManagementComputerMenu;
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.computers.menu.PersonalComputerMenu;
import dev.jstech.computers.menu.ServerAssemblyMenu;
import dev.jstech.computers.menu.ServerRackMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.KvmSelectPayload;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.computers.operation.payload.OpenKvmPayload;
import dev.jstech.computers.operation.payload.RackBayPowerPayload;
import dev.jstech.computers.operation.payload.RemoteControlPayload;
import dev.jstech.computers.operation.payload.RemoteHostsPayload;
import dev.jstech.computers.operation.payload.RenamePcPayload;
import dev.jstech.computers.operation.payload.RenameServerPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * The payloads that act on a machine itself: its power, remote control, the KVM switch, the power of a rack bay and
 * renaming a computer or a server.
 */
public final class MachinePayloads {

    private MachinePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RackBayPowerPayload.TYPE, RackBayPowerPayload.STREAM_CODEC,
                ComputerAccess.menu(ServerRackMenu.class,
                        ServerRackMenu::rackPos, RackBayPowerPayload::rackPos),
                MachinePayloads::handleRackBayPower);
        ComputerAccess.accept(registrar, MachinePowerPayload.TYPE, MachinePowerPayload.STREAM_CODEC,
                ComputerAccess.machine(MachinePowerPayload::hostPos), MachinePayloads::handleMachinePower);
        registrar.playToClient(OpenKvmPayload.TYPE, OpenKvmPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(MachinePayloads::handleOpenKvm));
        ComputerAccess.accept(registrar, RemoteControlPayload.TYPE, RemoteControlPayload.STREAM_CODEC,
                ComputerAccess.machine(RemoteControlPayload::hostPos), MachinePayloads::handleRemoteControl);
        registrar.playToClient(RemoteHostsPayload.TYPE, RemoteHostsPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(MachinePayloads::handleRemoteHosts));
        ComputerAccess.accept(registrar, KvmSelectPayload.TYPE, KvmSelectPayload.STREAM_CODEC,
                ComputerAccess.screen(KvmSelectPayload::rackPos), MachinePayloads::handleKvmSelect);
        ComputerAccess.accept(registrar, RenameServerPayload.TYPE, RenameServerPayload.STREAM_CODEC,
                ComputerAccess.menu(ServerAssemblyMenu.class),
                MachinePayloads::handleRenameServer);
        // A desk computer, a crafting computer and a cluster management computer are each renamed in their own assembly.
        ComputerAccess.accept(registrar, RenamePcPayload.TYPE, RenamePcPayload.STREAM_CODEC,
                ComputerAccess.anyOf(
                        ComputerAccess.menu(PersonalComputerMenu.class, PersonalComputerMenu::pcPos, RenamePcPayload::pcPos),
                        ComputerAccess.menu(CraftingComputerMenu.class, CraftingComputerMenu::computerPos,
                                RenamePcPayload::pcPos),
                        ComputerAccess.menu(ClusterManagementComputerMenu.class,
                                ClusterManagementComputerMenu::computerPos,
                                RenamePcPayload::pcPos)),
                MachinePayloads::handleRenamePc);
    }

    private static void handleRackBayPower(final RackBayPowerPayload payload, final ServerPlayer player,
                                           final ServerLevel level) {
        if (level.getBlockEntity(payload.rackPos())
                instanceof ServerRackBlockEntity rack) {
            rack.toggleBayPower(payload.slot());
        }
    }

    private static void handleRemoteControl(final RemoteControlPayload payload, final ServerPlayer player,
                                            final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof IComputerTerminalHost host)) {
            return;
        }
        final var cli = new ServerCliComputer(host, level);
        if (payload.action() == RemoteControlPayload.ACTION_LIST) {
            final List<RemoteHostsPayload.Entry> entries = new ArrayList<>();
            cli.remoteMachines().forEach((hostname, machine) -> {
                if (entries.size() >= RemoteHostsPayload.MAX_HOSTS) {
                    return;
                }
                final var remote = new ServerCliComputer(
                        (IComputerTerminalHost) machine,
                        level);
                final var os = machine instanceof IOsHost h
                        ? h.installedOs() : null;
                entries.add(new RemoteHostsPayload.Entry(machine.getBlockPos().asLong(), hostname,
                        remote.type(), os == null ? "" : os.displayName(), remote.running()));
            });
            PacketDistributor.sendToPlayer(player, new RemoteHostsPayload(entries));
            return;
        }
        /*
         * Take over: put the chosen machine's own session on this monitor, exactly as walking to
         * it would. Reachability is re-checked here so a stale window cannot reach off-network.
         */
        final BlockPos target = BlockPos.of(payload.targetPos());
        final boolean reachable = cli.remoteMachines().values().stream()
                .anyMatch(machine -> machine.getBlockPos().equals(target));
        if (!reachable) {
            return;
        }
        /*
         * Mark the screen as showing the remote machine BEFORE opening it: every menu validates
         * through the monitor, and without this the new session is torn down on its first tick
         * for showing a computer the cable does not link.
         */
        if (level.getBlockEntity(payload.monitorPos())
                instanceof MonitorBlockEntity monitor) {
            monitor.setRemoteSession(target);
        }
        player.closeContainer();
        MachineOperators.note(level, target, player);
        JscEvents.award(player, JscEvents.REMOTE_CONTROL);
        MonitorBlock.bootOrPost(
                player, level, payload.monitorPos(), target);
    }

    private static void handleRemoteHosts(final RemoteHostsPayload payload, final Player player) {
        RemoteControlApp.accept(payload);
    }

    private static void handleOpenKvm(final OpenKvmPayload payload, final Player player) {
        IKvmScreenOpener.Holder.open(payload);
    }

    private static void handleKvmSelect(final KvmSelectPayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        if (!(level.getBlockEntity(payload.rackPos())
                instanceof ServerRackBlockEntity rack)) {
            return;
        }
        // The switch has to be there for the monitor to address a bay at all.
        if (rack.computerSlots().size() > 1 && !rack.hasKvmSwitch()) {
            return;
        }
        rack.setActiveChannel(payload.slot());
        // With the channel set, the rack answers as that machine: start its session.
        MonitorBlock.openSelectedChannel(
                player, level, payload.monitorPos(), payload.rackPos());
    }

    private static void handleMachinePower(final MachinePowerPayload payload, final ServerPlayer player,
                                           final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        /*
         * The screen closes either way: a machine that just powered off has nothing to show, and
         * a restart comes back through the power-on self-test like any other cold start.
         */
        player.closeContainer();
        switch (payload.action()) {
            case MachinePowerPayload.ACTION_SHUTDOWN -> computer.setPowered(false);
            case MachinePowerPayload.ACTION_RESTART -> {
                computer.setPowered(false);
                computer.setPowered(true);
                JscEvents.award(player, JscEvents.POWER_CYCLED);
                MonitorBlock.openPost(
                        player, level, payload.monitorPos(), payload.hostPos());
            }
            default -> {
                // Logging off leaves the machine running; the screen is already closed.
            }
        }
    }

    /*
     * Its gate has already made sure the player is in that computer's assembly. A supercomputer node is a
     * rack computer: it is renamed through the Server assembly like any other server.
     */
    private static void handleRenamePc(final RenamePcPayload payload, final ServerPlayer player,
                                       final ServerLevel level) {
        if (level.getBlockEntity(payload.pcPos()) instanceof IOsHost computer) {
            computer.setCustomName(payload.name());
        }
    }

    private static void handleRenameServer(final RenameServerPayload payload, final ServerPlayer player,
                                           final ServerLevel level) {
        if (player.containerMenu instanceof ServerAssemblyMenu menu) {
            menu.setServerName(payload.name());
        }
    }
}
