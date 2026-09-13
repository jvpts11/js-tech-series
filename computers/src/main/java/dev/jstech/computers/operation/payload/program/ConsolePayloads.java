/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.CommandOutputPayload;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.ConsoleInitPayload;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.RequestConsoleInitPayload;
import dev.jstech.computers.operation.payload.RunCommandPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * The command prompt's payloads: a typed command, the lines it prints and the console a prompt opens with.
 */
public final class ConsolePayloads {

    private ConsolePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RunCommandPayload.TYPE, RunCommandPayload.STREAM_CODEC,
                ComputerAccess.machine(RunCommandPayload::hostPos), ConsolePayloads::handleRunCommand);
        registrar.playToClient(CommandOutputPayload.TYPE, CommandOutputPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ConsolePayloads::handleCommandOutput));
        ComputerAccess.accept(registrar, RequestConsoleInitPayload.TYPE, RequestConsoleInitPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestConsoleInitPayload::hostPos), ConsolePayloads::handleRequestConsoleInit);
        registrar.playToClient(ConsoleInitPayload.TYPE, ConsoleInitPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ConsolePayloads::handleConsoleInit));
    }

    /*
     * Command Prompt: a typed line runs through the shell against the open host and the styled
     * output is streamed back. The CLI is an alternative interface over the same network operations.
     */

    static final int CLI_WIDTH = 50;

    private static void handleRunCommand(final RunCommandPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (!(player.containerMenu instanceof dev.jstech.computers.menu.CommandPromptMenu menu)
                || !menu.hostPos().equals(payload.hostPos())
                || !(level.getBlockEntity(payload.hostPos())
                        instanceof dev.jstech.computers.terminal.IComputerTerminalHost host)) {
            return;
        }
        // "run/open <program>" launches another installed program from the prompt.
        final String[] parts = payload.line().trim().split("\\s+", 2);
        if (parts.length == 2 && (parts[0].equalsIgnoreCase("run") || parts[0].equalsIgnoreCase("open"))) {
            if (host.console() != null && !payload.line().isBlank()) {
                host.console().pushHistory(payload.line().trim());
            }
            launchProgram(player, host, menu.monitorPos(), payload.hostPos(), parts[1].trim());
            return;
        }
        /*
         * An open ssh session runs the line on the remote machine, in its own shell family, the
         * local terminal is only the window. Everything else (ssh itself, exit) stays local.
         */
        final var localComputer =
                new dev.jstech.computers.program.ServerCliComputer(host, level);
        var computer = localComputer;
        final var session = sshTargetOf(host, level, payload.line());
        if (session != null) {
            computer = new dev.jstech.computers.program.ServerCliComputer(
                    session, level);
        }
        /*
         * The shell speaks the installed OS kernel's family (DOS verbs on MC-DOS/Frames, POSIX on Linux), or
         * the live installer's verbs while a live medium is booted.
         */
        final var shell = dev.jstech.computers.program.cli.CliCommands.shellFor(
                computer, CLI_WIDTH);
        final var response = shell.run(payload.line(), computer);
        final List<CommandOutputPayload.WireLine> wire = new ArrayList<>(response.lines().size());
        for (final var cliLine : response.lines()) {
            wire.add(new CommandOutputPayload.WireLine(cliLine.text(), cliLine.style().ordinal()));
        }
        final String prompt = computer.prompt();
        final var handOver = response.handOver();
        PacketDistributor.sendToPlayer(player, new CommandOutputPayload(response.clearScreen(), prompt, wire,
                handOver == null ? "" : handOver.editor(),
                handOver == null ? "" : handOver.path()));
        if (computer.firmwareRebootRequested()) {
            // "reboot --firmware": leave the terminal and enter the boot manager on the same monitor.
            player.closeContainer();
            dev.jstech.computers.block.MonitorBlock.openFirmware(
                    player, level, menu.monitorPos(), payload.hostPos());
            return;
        }
        if (computer.rebootRequested()) {
            /*
             * A plain "reboot": the terminal closes and the POST replays on the same monitor, after
             * which whatever the boot target now is (a freshly installed OS included) comes up.
             */
            if (level.getBlockEntity(payload.hostPos())
                    instanceof dev.jstech.computers.os.IOsHost be) {
                be.setNeedsPost(true);
            }
            player.closeContainer();
            dev.jstech.computers.block.MonitorBlock.openPost(
                    player, level, menu.monitorPos(), payload.hostPos());
            return;
        }
        // Persist the typed line on the computer so the history survives closing the prompt or Monitor.
        if (host.console() != null && !payload.line().isBlank()) {
            host.console().pushHistory(payload.line().trim());
            ((net.minecraft.world.level.block.entity.BlockEntity) host).setChanged();
        }
    }

    static void launchProgram(final ServerPlayer player,
            final dev.jstech.computers.terminal.IComputerTerminalHost host,
            final BlockPos monitorPos, final BlockPos hostPos, final String name) {
        dev.jstech.computers.os.ProgramSpec program = null;
        for (final var candidate : dev.jstech.computers.program.Programs.all()) {
            if (candidate.commandName().equalsIgnoreCase(name)
                    || candidate.id().getPath().equalsIgnoreCase(name)
                    || candidate.id().toString().equalsIgnoreCase(name)) {
                program = candidate;
                break;
            }
        }
        if (program == null) {
            sendConsoleLine(player, "no such program: " + name, OperationRecord.STATUS_FAILED);
            return;
        }
        final boolean installed = program.preinstalled()
                || (host.console() != null && host.console().isInstalled(program.id().toString()));
        if (!installed) {
            sendConsoleLine(player, program.commandName() + " is not installed - try: install "
                    + program.commandName(), OperationRecord.STATUS_FAILED);
            return;
        }
        /*
         * Program run gate: the installed OS platform must be one the program supports, and the computer
         * must meet its CPU/VRAM minimums. Null-safe: programs with no declared requirement always pass.
         */
        if (player.level() instanceof ServerLevel osLevel
                && osLevel.getBlockEntity(hostPos) instanceof dev.jstech.computers.os
                        .IOsHost osComputer
                && !dev.jstech.computers.os.OsRegistry.canRunProgram(
                        osComputer.installedOsId(), program.id(),
                        osComputer.maxCpuMhz(), osComputer.totalVramMb())) {
            sendConsoleLine(player, program.commandName()
                    + " cannot run on this computer's OS or hardware", OperationRecord.STATUS_FAILED);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "The " + program.commandName() + " cannot run on this computer's OS or hardware."), false);
            return;
        }
        if (program.id().equals(dev.jstech.computers.program.Programs.NMS)) {
            // The NMS is now a desktop window opened from its Frames desktop icon, not a server-side menu.
            sendConsoleLine(player, "open the NMS from its desktop icon on a Frames computer", -1);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Open the NMS from its desktop icon."), false);
        } else {
            sendConsoleLine(player, "the " + program.commandName() + " is already open", -1);
        }
    }

    /** One styled line back to the open Command Prompt (status -1 = dim, FAILED = red, else green). */
    private static void sendConsoleLine(final ServerPlayer player, final String text, final int status) {
        final dev.jstech.computers.program.cli.CliStyle style = status == OperationRecord.STATUS_FAILED
                ? dev.jstech.computers.program.cli.CliStyle.ERROR
                : status < 0 ? dev.jstech.computers.program.cli.CliStyle.DIM
                : dev.jstech.computers.program.cli.CliStyle.OK;
        final List<CommandOutputPayload.WireLine> wire = new ArrayList<>();
        for (final String line : wrapToConsole(text)) {
            wire.add(new CommandOutputPayload.WireLine(line, style.ordinal()));
        }
        // An empty prompt means "keep the current prompt", so this helper does not change the directory.
        PacketDistributor.sendToPlayer(player, new CommandOutputPayload(false, "", wire));
    }

    /** Word-wraps a direct console message to the console width so a long line never overflows the prompt. */
    private static List<String> wrapToConsole(final String text) {
        final List<String> lines = new ArrayList<>();
        for (final String paragraph : text.split("\n", -1)) {
            String remaining = paragraph;
            while (remaining.length() > CLI_WIDTH) {
                int cut = remaining.lastIndexOf(' ', CLI_WIDTH);
                if (cut <= 0) {
                    cut = CLI_WIDTH;
                }
                lines.add(remaining.substring(0, cut));
                remaining = remaining.substring(cut).stripLeading();
            }
            lines.add(remaining);
        }
        return lines;
    }

    private static void handleRequestConsoleInit(final RequestConsoleInitPayload payload, final ServerPlayer player,
                                                 final ServerLevel level) {
        if (player.containerMenu instanceof dev.jstech.computers.menu.CommandPromptMenu menu
                && menu.hostPos().equals(payload.hostPos())
                && level.getBlockEntity(payload.hostPos())
                        instanceof dev.jstech.computers.terminal.IComputerTerminalHost host) {
            sendConsoleInit(player, host);
        }
    }

    private static void sendConsoleInit(final ServerPlayer player,
            final dev.jstech.computers.terminal.IComputerTerminalHost host) {
        final var console = host.console();
        final List<String> history = console == null ? List.of() : console.history();
        final List<ConsoleInitPayload.WireCommand> commands = new ArrayList<>();
        /*
         * Tab completion offers the installed shell family's verbs (ls/cat on Linux, dir/type on DOS), or the
         * live installer's while a live medium is booted.
         */
        final boolean live = console != null && console.liveInstall() != null;
        /*
         * Completion and hints offer only what this machine can run: a verb another kind of computer owns
         * (the cluster command outside a Cluster Management Computer) is no command here, and must not be
         * hinted as one.
         */
        final var cli = host instanceof net.minecraft.world.level.block.entity.BlockEntity
                ? new dev.jstech.computers.program.ServerCliComputer(host, player.serverLevel())
                : null;
        for (final var command : dev.jstech.computers.program.cli.CliCommands.commandsFor(
                dev.jstech.computers.program.ServerCliComputer.shellFamilyOf(host), live)) {
            if (commands.size() >= ConsoleInitPayload.MAX_COMMANDS) {
                break;
            }
            if (cli != null && !command.available(cli)) {
                continue;
            }
            commands.add(new ConsoleInitPayload.WireCommand(command.name(), command.usage()));
        }
        /*
         * The devices Tab can complete for /dev/ arguments (mkfs, mount, grub-install): the disks in slot
         * order during a live install, or the mounted drives' device names on an installed POSIX system.
         */
        final List<String> devices = new ArrayList<>();
        if (live && host instanceof dev.jstech.computers.os
                .IOsHost computer) {
            for (int i = 0; i < computer.diskSlots(); i++) {
                if (computer.diskInSlot(i).getItem()
                        instanceof dev.jstech.computers.item.DiskItem) {
                    devices.add("sd" + (char) ('a' + i));
                }
            }
        } else if (dev.jstech.computers.program.ServerCliComputer.shellFamilyOf(host)
                == dev.jstech.computers.os.ShellFamily.POSIX
                && player.level() instanceof ServerLevel serverLevel) {
            for (final var mount : new dev.jstech.computers.program.ServerCliComputer(
                    host, serverLevel).mounts()) {
                if (devices.size() < ConsoleInitPayload.MAX_DEVICES && mount.ready()) {
                    devices.add(mount.device());
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new ConsoleInitPayload(
                ((net.minecraft.world.level.block.entity.BlockEntity) host).getBlockPos(),
                List.copyOf(history), commands, devices));
    }

    private static void handleConsoleInit(final ConsoleInitPayload payload, final Player player) {
        dev.jstech.computers.client.CommandPromptScreen.acceptInit(payload);
    }

    private static void handleCommandOutput(final CommandOutputPayload payload, final Player player) {
        dev.jstech.computers.client.CommandPromptScreen.accept(payload);
    }

    /**
     * The machine an open ssh session points at, or null when the line must run locally. {@code ssh}
     * and {@code exit} always run on the local terminal: one opens the session, the other closes it.
     * A session whose machine went away (broken, unpowered) is dropped, so the shell falls back home
     * instead of talking to a ghost.
     */
    @org.jetbrains.annotations.Nullable
    private static dev.jstech.computers.terminal.IComputerTerminalHost sshTargetOf(
            final dev.jstech.computers.terminal.IComputerTerminalHost host,
            final ServerLevel level, final String line) {
        final var console = host.console();
        if (console == null || console.sshTarget() == null) {
            return null;
        }
        final String verb = line.trim().split("\\s+", 2)[0].toLowerCase(java.util.Locale.ROOT);
        if (verb.equals("ssh") || verb.equals("exit") || verb.equals("logout")) {
            return null;
        }
        final var target = level.getBlockEntity(BlockPos.of(console.sshTarget()));
        if (target instanceof dev.jstech.computers.terminal.IComputerTerminalHost remote
                && remote.computerRunning()) {
            return remote;
        }
        console.setSshTarget(null);
        return null;
    }
}
