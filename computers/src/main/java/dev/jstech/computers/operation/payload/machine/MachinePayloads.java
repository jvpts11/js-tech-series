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
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.audio.IMachineCue;
import dev.jstech.computers.audio.ProgramCue;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.block.IKvmScreenOpener;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.client.os.RemoteControlApp;
import dev.jstech.computers.machine.RemoteComputerService;
import dev.jstech.computers.menu.AbstractAssemblyComputerMenu;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.menu.RedstoneInterfaceMenu;
import dev.jstech.computers.menu.ServerAssemblyMenu;
import dev.jstech.computers.menu.ServerRackMenu;
import dev.jstech.computers.menu.SpeakerMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.KvmSelectPayload;
import dev.jstech.computers.monitor.IMonitorPictureSink;
import dev.jstech.computers.monitor.MonitorPicturePayload;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.computers.operation.payload.OpenKvmPayload;
import dev.jstech.computers.operation.payload.RackBayPowerPayload;
import dev.jstech.computers.operation.payload.RemoteControlPayload;
import dev.jstech.computers.operation.payload.RemoteHostsPayload;
import dev.jstech.computers.operation.payload.RenamePcPayload;
import dev.jstech.computers.operation.payload.RenameRedstoneInterfacePayload;
import dev.jstech.computers.operation.payload.RenameServerPayload;
import dev.jstech.computers.operation.payload.RenameSpeakerPayload;
import dev.jstech.computers.operation.payload.MachineSoundPayload;
import dev.jstech.computers.operation.payload.TestSoundPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.audio.Audio;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * The payloads that act on a machine itself: its power, remote control, the KVM switch, the power of a rack bay,
 * renaming a computer, a server or a speaker, the sounds its desktop and programs raise, and trying its sound.
 */
public final class MachinePayloads {

    private MachinePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.onMenu(registrar, RackBayPowerPayload.TYPE, RackBayPowerPayload.STREAM_CODEC,
                ServerRackMenu.class, ServerRackMenu::rackPos, RackBayPowerPayload::rackPos,
                MachinePayloads::handleRackBayPower);
        ComputerAccess.accept(registrar, MachinePowerPayload.TYPE, MachinePowerPayload.STREAM_CODEC,
                ComputerAccess.machine(MachinePowerPayload::hostPos), MachinePayloads::handleMachinePower);
        ComputerAccess.accept(registrar, MachineSoundPayload.TYPE, MachineSoundPayload.STREAM_CODEC,
                ComputerAccess.machine(MachineSoundPayload::hostPos), MachinePayloads::handleMachineSound);
        ComputerAccess.accept(registrar, TestSoundPayload.TYPE, TestSoundPayload.STREAM_CODEC,
                ComputerAccess.machine(TestSoundPayload::hostPos), MachinePayloads::handleTestSound);
        ComputerAccess.onMenu(registrar, RenameSpeakerPayload.TYPE, RenameSpeakerPayload.STREAM_CODEC,
                SpeakerMenu.class, SpeakerMenu::speakerPos, RenameSpeakerPayload::speakerPos,
                MachinePayloads::handleRenameSpeaker);
        ComputerAccess.onMenu(registrar, RenameRedstoneInterfacePayload.TYPE,
                RenameRedstoneInterfacePayload.STREAM_CODEC, RedstoneInterfaceMenu.class,
                RedstoneInterfaceMenu::sensorPos, RenameRedstoneInterfacePayload::sensorPos,
                MachinePayloads::handleRenameRedstoneInterface);
        registrar.playToClient(OpenKvmPayload.TYPE, OpenKvmPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(MachinePayloads::handleOpenKvm));
        registrar.playToClient(MonitorPicturePayload.TYPE, MonitorPicturePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> IMonitorPictureSink.Holder.accept(payload)));
        ComputerAccess.accept(registrar, RemoteControlPayload.TYPE, RemoteControlPayload.STREAM_CODEC,
                ComputerAccess.machineAt(RemoteControlPayload::hostPos, RemoteControlPayload::monitorPos),
                MachinePayloads::handleRemoteControl);
        registrar.playToClient(RemoteHostsPayload.TYPE, RemoteHostsPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(MachinePayloads::handleRemoteHosts));
        ComputerAccess.accept(registrar, KvmSelectPayload.TYPE, KvmSelectPayload.STREAM_CODEC,
                ComputerAccess.screenAt(KvmSelectPayload::rackPos, KvmSelectPayload::monitorPos,
                        MonitorSessionMenu.Phase.KVM),
                MachinePayloads::handleKvmSelect);
        ComputerAccess.onMenu(registrar, RenameServerPayload.TYPE, RenameServerPayload.STREAM_CODEC,
                ServerAssemblyMenu.class, MachinePayloads::handleRenameServer);
        ComputerAccess.onMenu(registrar, RenamePcPayload.TYPE, RenamePcPayload.STREAM_CODEC,
                AbstractAssemblyComputerMenu.class, AbstractAssemblyComputerMenu::computerPos,
                RenamePcPayload::pcPos, MachinePayloads::handleRenamePc);
    }

    private static void handleRenameSpeaker(final RenameSpeakerPayload payload, final SpeakerMenu menu,
                                            final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(menu.speakerPos()) instanceof SpeakerBlockEntity speaker) {
            speaker.ask(level, payload.name());
        }
    }

    private static void handleRenameRedstoneInterface(final RenameRedstoneInterfacePayload payload,
                                                      final RedstoneInterfaceMenu menu, final ServerPlayer player,
                                                      final ServerLevel level) {
        if (level.getBlockEntity(menu.sensorPos()) instanceof RedstoneInterfaceBlockEntity sensor) {
            sensor.ask(level, payload.name());
        }
    }

    /* A sound a screen raised: the bell rings through the system's own or the case's speaker, the rest as they are. */
    private static void handleMachineSound(final MachineSoundPayload payload, final ServerPlayer player,
                                           final ServerLevel level) {
        final IMachineCue cue = payload.cue();
        if (cue == null || !(level.getBlockEntity(payload.hostPos()) instanceof IOsHost host)) {
            return;
        }
        switch (cue) {
            case SystemSound system when system == SystemSound.BEEP -> host.bell(level);
            case SystemSound system -> host.systemSound(level, system);
            case ProgramCue program -> host.programSound(level, program);
        }
    }

    private static void handleTestSound(final TestSoundPayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost host) {
            host.systemSound(level, SystemSound.STARTUP);
        }
    }

    private static void handleRackBayPower(final RackBayPowerPayload payload, final ServerRackMenu menu,
                                           final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(menu.rackPos())
                instanceof ServerRackBlockEntity rack) {
            // The bay switch is the server's power button, pressed by the player.
            Audio.at(level, menu.rackPos(), ComputingSounds.POWER_BUTTON);
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
                final var remote = (IComputerTerminalHost) machine;
                final var os = machine instanceof IOsHost h
                        ? h.installedOs() : null;
                entries.add(new RemoteHostsPayload.Entry(machine.getBlockPos().asLong(), hostname,
                        RemoteComputerService.typeOf(machine), os == null ? "" : os.displayName(),
                        remote.computerRunning()));
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
         * The desktop closes either way. Shutting down and restarting then put the system's own goodbye in front
         * of the player who asked, since they sat at the desktop rather than at a monitor session the goodbye
         * would reach by itself: the machine goes dark when it ends, or tests itself again.
         */
        player.closeContainer();
        switch (payload.action()) {
            case MachinePowerPayload.ACTION_SHUTDOWN -> {
                computer.shutDown();
                showGoodbye(computer, player, level, payload);
            }
            case MachinePowerPayload.ACTION_RESTART -> {
                computer.restart();
                JscEvents.award(player, JscEvents.POWER_CYCLED);
                if (!showGoodbye(computer, player, level, payload)) {
                    // A system with nothing to show on its way down starts over at once.
                    MonitorBlock.openPost(player, level, payload.monitorPos(), payload.hostPos());
                }
            }
            default -> {
                // Logging off leaves the machine running; the screen is already closed.
            }
        }
    }

    /* The system's goodbye on the player's monitor, when it has one to say; true when it did. */
    private static boolean showGoodbye(final IOsHost computer, final ServerPlayer player, final ServerLevel level,
                                       final MachinePowerPayload payload) {
        if (!computer.goingDown()) {
            return false;
        }
        MonitorBlock.openSystemDown(player, level, payload.monitorPos(), payload.hostPos(), computer);
        return true;
    }

    // A supercomputer node is a rack computer: it is renamed through the Server assembly like any other server.
    private static void handleRenamePc(final RenamePcPayload payload, final AbstractAssemblyComputerMenu menu,
                                       final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(menu.computerPos()) instanceof IOsHost computer) {
            computer.setCustomName(payload.name());
        }
    }

    private static void handleRenameServer(final RenameServerPayload payload, final ServerAssemblyMenu menu,
                                           final ServerPlayer player, final ServerLevel level) {
        menu.setServerName(payload.name());
    }
}
