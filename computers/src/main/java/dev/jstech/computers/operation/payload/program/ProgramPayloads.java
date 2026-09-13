/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.OpenComputerUiPayload;
import dev.jstech.computers.operation.payload.OpenProgramPayload;
import dev.jstech.computers.operation.payload.ProcessActionPayload;
import dev.jstech.computers.operation.payload.ProcessListPayload;
import dev.jstech.computers.operation.payload.RunProgramPayload;
import dev.jstech.computers.operation.payload.UiEventPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.operation.payload.UninstallProgramPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

import static dev.jstech.computers.operation.payload.files.FilePayloads.readDiskFile;
import static dev.jstech.computers.operation.payload.program.ConsolePayloads.launchProgram;

/**
 * The payloads that open and run programs on a computer, carry their windows and window events, list and act on its
 * processes, and uninstall a program.
 */
public final class ProgramPayloads {

    private ProgramPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RunProgramPayload.TYPE, RunProgramPayload.STREAM_CODEC,
                ComputerAccess.machine(RunProgramPayload::hostPos), ProgramPayloads::handleRunProgram);
        registrar.playToClient(UiWindowPayload.TYPE, UiWindowPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ProgramPayloads::handleUiWindow));
        ComputerAccess.accept(registrar, UiEventPayload.TYPE, UiEventPayload.STREAM_CODEC,
                ComputerAccess.machine(UiEventPayload::hostPos), ProgramPayloads::handleUiEvent);
        ComputerAccess.accept(registrar, OpenProgramPayload.TYPE, OpenProgramPayload.STREAM_CODEC,
                ComputerAccess.machine(OpenProgramPayload::hostPos), ProgramPayloads::handleOpenProgram);
        registrar.playToClient(ProcessListPayload.TYPE, ProcessListPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ProgramPayloads::handleProcessList));
        ComputerAccess.accept(registrar, ProcessActionPayload.TYPE, ProcessActionPayload.STREAM_CODEC,
                ComputerAccess.machine(ProcessActionPayload::hostPos), ProgramPayloads::handleProcessAction);
        ComputerAccess.accept(registrar, UninstallProgramPayload.TYPE, UninstallProgramPayload.STREAM_CODEC,
                ComputerAccess.machine(UninstallProgramPayload::hostPos), ProgramPayloads::handleUninstallProgram);
        registrar.playToClient(OpenComputerUiPayload.TYPE, OpenComputerUiPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ProgramPayloads::handleOpenComputerUi));
    }

    /** The processes running on the host at {@code hostPos}: the IQL Engine and its jobs if it is a Mainframe with the Engine installed, else an empty list (a computer with no service). */
    public static void dispatchProcesses(final ServerPlayer player, final BlockPos hostPos,
                                         final ServerLevel level) {
        final List<ProcessListPayload.ProcessLine> lines = new ArrayList<>();
        if (level.getBlockEntity(hostPos) instanceof MainframeBlockEntity mainframe
                && mainframe.isIqlEngineInstalled()) {
            final boolean running = mainframe.isIqlEngineRunning();
            lines.add(new ProcessListPayload.ProcessLine(ProcessListPayload.KIND_SERVICE, "IQL Engine",
                    running ? "running" : "stopped",
                    running ? "the network's query and job engine" : "stopped, start it to run jobs"));
            for (final dev.jstech.computers.program.iql.IqlSavedObject job
                    : mainframe.iqlCatalog().ofType(
                            dev.jstech.computers.program.iql.IqlDefinition.ObjectType.JOB)) {
                final boolean paused = mainframe.isJobPaused(job.name());
                final String state = paused ? "paused" : running ? "active" : "idle";
                lines.add(new ProcessListPayload.ProcessLine(ProcessListPayload.KIND_JOB, job.name(),
                        state, jobDetail(job)));
            }
        }
        PacketDistributor.sendToPlayer(player, new ProcessListPayload(lines));
    }

    private static String jobDetail(final dev.jstech.computers.program.iql.IqlSavedObject job) {
        return switch (job.triggerKind()) {
            case EVERY -> "every " + job.triggerSpec();
            case WHEN -> "when " + job.triggerSpec();
            case NONE -> job.body();
        };
    }

    private static void handleProcessList(final ProcessListPayload payload, final Player player) {
        if (player.containerMenu instanceof ComputerTerminalMenu menu) {
            menu.setProcesses(payload.processes());
        }
    }

    private static void handleProcessAction(final ProcessActionPayload payload, final ServerPlayer player,
                                            final ServerLevel level) {
        if (!(player.containerMenu instanceof ComputerTerminalMenu menu)
                || !menu.hostPos().equals(payload.hostPos())
                || !(level.getBlockEntity(payload.hostPos()) instanceof MainframeBlockEntity mainframe)) {
            return;
        }
        if (payload.kind() == ProcessListPayload.KIND_SERVICE) {
            switch (payload.action()) {
                case ProcessActionPayload.ACTION_STOP -> mainframe.setIqlEngineRunning(false);
                case ProcessActionPayload.ACTION_START -> mainframe.setIqlEngineRunning(true);
                case ProcessActionPayload.ACTION_RESTART -> {
                    mainframe.setIqlEngineRunning(false);
                    mainframe.setIqlEngineRunning(true);
                }
                default -> { /* END has no meaning for a service */ }
            }
        } else {
            switch (payload.action()) {
                case ProcessActionPayload.ACTION_END -> mainframe.pauseJob(payload.name());
                case ProcessActionPayload.ACTION_RESTART -> mainframe.restartJob(payload.name());
                default -> { /* a job has only End/Restart */ }
            }
        }
        dispatchProcesses(player, payload.hostPos(), level);
    }

    private static void handleOpenComputerUi(final OpenComputerUiPayload payload, final Player player) {
        // Only the firmware setup is a client-only screen; the desktop opens as a server-side menu.
        dev.jstech.computers.block.IFirmwareScreenOpener.Holder.open(
                payload.host(), payload.monitorPos(),
                dev.jstech.computers.os.FirmwareKind.byId(payload.firmwareKind()),
                payload.name());
    }

    private static void handleOpenProgram(final OpenProgramPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof dev.jstech.computers.terminal.IComputerTerminalHost terminalHost)) {
            return;
        }
        /*
         * Anti-spoof: either this host's terminal menu is open, or the player is within reach of the
         * monitor they used (the desktop shell is a client-only screen with no server-side menu, so a
         * program opened from it cannot be validated against an open container).
         */
        final boolean viaTerminal = player.containerMenu instanceof ComputerTerminalMenu terminal
                && terminal.hostPos().equals(payload.hostPos());
        /*
         * The desktop path is only valid when the monitor is actually a linked peripheral of this host,
         * so a player near any monitor cannot open a program bound to a foreign computer.
         */
        final boolean nearMonitor = player.distanceToSqr(
                net.minecraft.world.phys.Vec3.atCenterOf(payload.monitorPos())) <= 64.0
                && terminalHost instanceof dev.jstech.core.peripheral.IPeripheralOwner owner
                && owner.linkedEndpoints().contains(payload.monitorPos().asLong());
        if (!viaTerminal && !nearMonitor) {
            return;
        }
        final String id = payload.programId();
        if (id.equals(dev.jstech.computers.program.Programs.COMMAND_PROMPT.toString())
                || id.equals("command_prompt")) {
            final net.minecraft.network.chat.Component title =
                    player.level().getBlockState(payload.hostPos()).getBlock().getName();
            /*
             * The host's board-derived era drives the prompt's GUI skin; capture it at open time. It is
             * not re-synced afterwards because the board is only swapped in the computer's own assembly
             * GUI, never from the running prompt.
             */
            final dev.jstech.core.tier.HardwareEra hostEra =
                    player.level().getBlockEntity(payload.hostPos())
                            instanceof dev.jstech.computers.os
                                    .IOsHost host ? host.displayEra() : null;
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (windowId, inv, p) -> new dev.jstech.computers.menu.CommandPromptMenu(
                            windowId, inv, payload.monitorPos(), payload.hostPos(), hostEra), title),
                    buf -> dev.jstech.computers.menu.CommandPromptMenu.writeOpenBuffer(
                            buf, payload.monitorPos(), payload.hostPos(), hostEra));
        } else {
            /*
             * The NMS and other windowed programs open through the shared launcher, which checks they
             * are installed and (for the NMS) that the IQL Engine is running on the network's Mainframe.
             */
            launchProgram(player, terminalHost, payload.monitorPos(), payload.hostPos(), id);
        }
    }

    private static void handleUiWindow(final UiWindowPayload payload, final Player player) {
        dev.jstech.computers.client.os.DesktopScreen.acceptWindow(payload);
    }

    /**
     * What a player did to a widget of a program's window, handed to the program that owns it. Only what a
     * keyboard and a mouse can do is taken, and only from a player at that machine's desktop.
     */
    private static void handleUiEvent(final UiEventPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        if (!(player.containerMenu instanceof dev.jstech.computers.menu.DesktopMenu desktop)
                || !payload.hostPos().equals(desktop.hostPos())
                || !UiEventPayload.KINDS.contains(payload.kind())
                || !(level.getBlockEntity(payload.hostPos())
                        instanceof dev.jstech.computers.blockentity.AbstractComputerBlockEntity computer)) {
            return;
        }
        computer.cannon().deliverUiEvent(payload.program(), payload.window(), payload.widget(),
                payload.kind(), payload.values());
    }

    private static void handleRunProgram(final RunProgramPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof dev.jstech.computers.blockentity.AbstractComputerBlockEntity computer)) {
            return;
        }
        final String name = dev.jstech.computers.os.fs.FsPaths.fileName(payload.path());
        final java.util.List<DesktopShellOutputPayload.WireLine> wire = new java.util.ArrayList<>();
        final java.util.Optional<String> listing = readDiskFile(level, computer, payload.path());
        if (listing.isEmpty()) {
            wire.add(new DesktopShellOutputPayload.WireLine(name + ": file not found",
                    dev.jstech.computers.program.cli.CliStyle.ERROR.id()));
            PacketDistributor.sendToPlayer(player,
                    new DesktopShellOutputPayload(false, false, "", wire, payload.session()));
            return;
        }
        final int room = dev.jstech.computers.cannon.machine.MachinePrograms.DEFAULT_HEAP_MB;
        if (!computer.ramLedger().fits(room)) {
            wire.add(new DesktopShellOutputPayload.WireLine(name + ": not enough memory to run it",
                    dev.jstech.computers.program.cli.CliStyle.ERROR.id()));
            PacketDistributor.sendToPlayer(player,
                    new DesktopShellOutputPayload(false, false, "", wire, payload.session()));
            return;
        }
        final var started = computer.cannon().start(name, listing.get(), room, computer, java.util.List.of(), 0,
                dev.jstech.computers.cannon.machine.MachinePrograms.DEFAULT_PRIORITY);
        if (!started.ok()) {
            wire.add(new DesktopShellOutputPayload.WireLine(started.message(),
                    dev.jstech.computers.program.cli.CliStyle.ERROR.id()));
            PacketDistributor.sendToPlayer(player,
                    new DesktopShellOutputPayload(false, false, "", wire, payload.session()));
            return;
        }
        computer.setChanged();
        final var one = computer.cannon().byId(started.id());
        final boolean console = one != null && !one.process().isService();
        if (console) {
            computer.cannon().hold(started.id());
        } else {
            wire.add(new DesktopShellOutputPayload.WireLine(started.message(),
                    dev.jstech.computers.program.cli.CliStyle.OK.id()));
        }
        PacketDistributor.sendToPlayer(player,
                new DesktopShellOutputPayload(false, console, "", wire, payload.session()));
    }

    private static void handleUninstallProgram(final UninstallProgramPayload payload, final ServerPlayer player,
                                               final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos())
                instanceof dev.jstech.computers.terminal.IComputerTerminalHost host) {
            final String id = payload.programId();
            final String name = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            new dev.jstech.computers.program.ServerCliComputer(host, level)
                    .packageRemove(name);
        }
    }
}
