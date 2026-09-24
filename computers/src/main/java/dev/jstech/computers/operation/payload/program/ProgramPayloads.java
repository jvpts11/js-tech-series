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
import dev.jstech.computers.machine.ProgramService;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.OpenComputerUiPayload;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.operation.payload.ProcessActionPayload;
import dev.jstech.computers.operation.payload.ProcessListPayload;
import dev.jstech.computers.operation.payload.RunProgramPayload;
import dev.jstech.computers.operation.payload.UiEventPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.operation.payload.UninstallProgramPayload;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.core.text.Text;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

import static dev.jstech.computers.operation.payload.files.FilePayloads.readDiskFile;

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
                    running ? ProcessListPayload.ProcessState.RUNNING : ProcessListPayload.ProcessState.STOPPED,
                    (running ? ProcessListPayload.ENGINE_SERVING : ProcessListPayload.ENGINE_STOPPED).text()));
            for (final IqlSavedObject job
                    : mainframe.iqlCatalog().ofType(
                            IqlDefinition.ObjectType.JOB)) {
                final boolean paused = mainframe.isJobPaused(job.name());
                final ProcessListPayload.ProcessState state = paused ? ProcessListPayload.ProcessState.PAUSED
                        : running ? ProcessListPayload.ProcessState.ACTIVE : ProcessListPayload.ProcessState.IDLE;
                lines.add(new ProcessListPayload.ProcessLine(ProcessListPayload.KIND_JOB, job.name(),
                        state, jobDetail(job)));
            }
        }
        PacketDistributor.sendToPlayer(player, new ProcessListPayload(lines));
    }

    /* A job is described by its own trigger clause, which is the query language's words and not the game's. */
    private static Text jobDetail(final IqlSavedObject job) {
        return Text.literal(switch (job.triggerKind()) {
            case EVERY -> "every " + job.triggerSpec();
            case WHEN -> "when " + job.triggerSpec();
            case NONE -> job.body();
        });
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
        final List<WireLine> wire = new ArrayList<>();
        final Function<String, ICliComputer.FsResult> disk =
                path -> readDiskFile(level, computer, path)
                        .map(ICliComputer.FsResult::content)
                        .orElse(ICliComputer.FsResult.fail(CliTexts.FILE_NOT_FOUND.text()));
        final var launch = ProgramLauncher.launch(computer, payload.path(), disk,
                List.of(), IProgramParent.NONE,
                ProgramPriority.MEDIUM, 0);
        if (!launch.ok()) {
            final Text why = switch (launch.refusal()) {
                case UNREADABLE -> CliTexts.SAID_BY.with(name, launch.said());
                case NO_MEMORY -> ProgramService.NO_ROOM.with(name, launch.roomMb(), launch.freeMb());
                case NO_RUNNER, NOT_STARTED -> launch.said();
            };
            wire.add(new WireLine(why, CliStyle.ERROR.id()));
            PacketDistributor.sendToPlayer(player,
                    new DesktopShellOutputPayload(false, false, "", wire, payload.session()));
            return;
        }
        final var one = computer.programs().byId(launch.id());
        final boolean console = one != null && !one.process().isService();
        if (console) {
            computer.programs().hold(launch.id());
        } else {
            wire.add(new WireLine(launch.said(),
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
