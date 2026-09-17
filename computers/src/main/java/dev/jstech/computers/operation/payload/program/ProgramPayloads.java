/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.block.IFirmwareScreenOpener;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.machine.ProgramLauncher;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.DesktopMenu;
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
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.tier.HardwareEra;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
            for (final IqlSavedObject job
                    : mainframe.iqlCatalog().ofType(
                            IqlDefinition.ObjectType.JOB)) {
                final boolean paused = mainframe.isJobPaused(job.name());
                final String state = paused ? "paused" : running ? "active" : "idle";
                lines.add(new ProcessListPayload.ProcessLine(ProcessListPayload.KIND_JOB, job.name(),
                        state, jobDetail(job)));
            }
        }
        PacketDistributor.sendToPlayer(player, new ProcessListPayload(lines));
    }

    private static String jobDetail(final IqlSavedObject job) {
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
        IFirmwareScreenOpener.Holder.open(
                payload.host(), payload.monitorPos(),
                FirmwareKind.byId(payload.firmwareKind()),
                payload.name());
    }

    private static void handleOpenProgram(final OpenProgramPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof IComputerTerminalHost terminalHost)) {
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
                Vec3.atCenterOf(payload.monitorPos())) <= 64.0
                && terminalHost instanceof IPeripheralOwner owner
                && owner.linkedEndpoints().contains(payload.monitorPos().asLong());
        if (!viaTerminal && !nearMonitor) {
            return;
        }
        final String id = payload.programId();
        if (id.equals(Programs.COMMAND_PROMPT.toString())
                || id.equals("command_prompt")) {
            final Component title =
                    player.level().getBlockState(payload.hostPos()).getBlock().getName();
            /*
             * The host's board-derived era drives the prompt's GUI skin; capture it at open time. It is
             * not re-synced afterwards because the board is only swapped in the computer's own assembly
             * GUI, never from the running prompt.
             */
            final HardwareEra hostEra =
                    player.level().getBlockEntity(payload.hostPos())
                            instanceof IOsHost host ? host.displayEra() : null;
            player.openMenu(new SimpleMenuProvider(
                    (windowId, inv, p) -> new CommandPromptMenu(
                            windowId, inv, payload.monitorPos(), payload.hostPos(), hostEra), title),
                    buf -> CommandPromptMenu.writeOpenBuffer(
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
        DesktopScreen.acceptWindow(payload);
    }

    /**
     * What a player did to a widget of a program's window, handed to the program that owns it. Only what a
     * keyboard and a mouse can do is taken, and only from a player at that machine's desktop.
     */
    private static void handleUiEvent(final UiEventPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        if (!(player.containerMenu instanceof DesktopMenu desktop)
                || !payload.hostPos().equals(desktop.hostPos())
                || !UiEventPayload.KINDS.contains(payload.kind())
                || !(level.getBlockEntity(payload.hostPos())
                        instanceof AbstractComputerBlockEntity computer)) {
            return;
        }
        computer.programs().deliverUiEvent(payload.program(), payload.window(), payload.widget(),
                payload.kind(), payload.values());
    }

    private static void handleRunProgram(final RunProgramPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof AbstractComputerBlockEntity computer)) {
            return;
        }
        final String name = FsPaths.fileName(payload.path());
        final List<DesktopShellOutputPayload.WireLine> wire = new ArrayList<>();
        final Function<String, ICliComputer.FsResult> disk =
                path -> readDiskFile(level, computer, path)
                        .map(ICliComputer.FsResult::ok)
                        .orElse(ICliComputer.FsResult.fail("file not found"));
        final var launch = ProgramLauncher.launch(computer, payload.path(), disk,
                List.of(), IProgramParent.NONE,
                ProgramPriority.MEDIUM, 0);
        if (!launch.ok()) {
            final String why = switch (launch.refusal()) {
                case UNREADABLE -> name + ": " + launch.message();
                case NO_MEMORY -> name + ": not enough memory to run it";
                case NO_RUNNER, NOT_STARTED -> launch.message();
            };
            wire.add(new DesktopShellOutputPayload.WireLine(why, CliStyle.ERROR.id()));
            PacketDistributor.sendToPlayer(player,
                    new DesktopShellOutputPayload(false, false, "", wire, payload.session()));
            return;
        }
        final var one = computer.programs().byId(launch.id());
        final boolean console = one != null && !one.process().isService();
        if (console) {
            computer.programs().hold(launch.id());
        } else {
            wire.add(new DesktopShellOutputPayload.WireLine(launch.message(),
                    CliStyle.OK.id()));
        }
        PacketDistributor.sendToPlayer(player,
                new DesktopShellOutputPayload(false, console, "", wire, payload.session()));
    }

    private static void handleUninstallProgram(final UninstallProgramPayload payload, final ServerPlayer player,
                                               final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos())
                instanceof IComputerTerminalHost host) {
            final String id = payload.programId();
            final String name = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            new ServerCliComputer(host, level)
                    .packageRemove(name);
        }
    }
}
