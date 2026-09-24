/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.CommandOutputPayload;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.operation.payload.ConsoleInitPayload;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.RequestConsoleInitPayload;
import dev.jstech.computers.operation.payload.RunCommandPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.SshTerminal;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * The command prompt's payloads: a typed command, the lines it prints and the console a prompt opens with.
 */
public final class ConsolePayloads {

    /**
     * How many characters wide a command prompt is: the shell lays its output out to this and long lines wrap
     * at it. The number the glass draws, so a table a command lines up here is lined up on the screen too.
     */
    static final int CLI_WIDTH = TermBuffer.MONITOR_COLUMNS;

    private ConsolePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RunCommandPayload.TYPE, RunCommandPayload.STREAM_CODEC,
                ComputerAccess.machine(RunCommandPayload::hostPos), ConsolePayloads::handleRunCommand,
                (player, payload) -> notListening(player));
        registrar.playToClient(CommandOutputPayload.TYPE, CommandOutputPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ConsolePayloads::handleCommandOutput));
        ComputerAccess.accept(registrar, RequestConsoleInitPayload.TYPE, RequestConsoleInitPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestConsoleInitPayload::hostPos), ConsolePayloads::handleRequestConsoleInit);
        registrar.playToClient(ConsoleInitPayload.TYPE, ConsoleInitPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ConsolePayloads::handleConsoleInit));
    }

    /**
     * Tells a terminal that the machine is not listening to it, which is the one thing it must never guess.
     *
     * <p>The terminal writes what was typed the moment it is typed and waits for the machine to answer, so a
     * line that goes nowhere leaves a command on the glass with nothing under it: a prompt, a command, and
     * silence, over and over, which reads as a machine that has broken rather than as one that this window
     * no longer reaches.
     */
    private static void notListening(final ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CommandOutputPayload(false, "",
                List.of(new WireLine(ConsoleTexts.NOT_ATTACHED.text(), CliStyle.ERROR.id())), "", ""));
    }

    private static void handleRunCommand(final RunCommandPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (!(player.containerMenu instanceof CommandPromptMenu menu)
                || !menu.hostPos().equals(payload.hostPos())
                || !(level.getBlockEntity(payload.hostPos())
                        instanceof IComputerTerminalHost host)) {
            notListening(player);
            return;
        }
        /*
         * A tool is in front of the terminal: what is typed is the tool's, not the shell's. Its question is
         * answered, Ctrl+C stops it, and anything else goes nowhere, as it would at a real terminal.
         */
        final TerminalTools.Turn turn = TerminalTools.typed(host, level, payload.line());
        if (turn != null) {
            final var here = new ServerCliComputer(host, level);
            PacketDistributor.sendToPlayer(player, new CommandOutputPayload(
                    turn.ended() ? SshTerminal.prompt(here, here) : "", turn.lines(),
                    TerminalTools.settled(turn.keyboard(), here, here)));
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
        /*
         * The shell is told who is typing at it, which is what the words about the player need: taking items
         * into your own hands, or handing over what you are holding. A session opened on another machine is
         * built without anybody, since nobody is standing in front of that one.
         */
        final var localComputer =
                new ServerCliComputer(host, level, player);
        var computer = localComputer;
        final var session = SshTerminal.targetOf(host, level, payload.line());
        if (session != null) {
            computer = new ServerCliComputer(
                    session, level);
        }
        /*
         * The shell speaks the installed OS kernel's family (DOS verbs on MC-DOS/Frames, POSIX on Linux), or
         * the live installer's verbs while a live medium is booted.
         */
        final var shell = CliCommands.shellFor(
                computer, CLI_WIDTH);
        final var response = shell.run(payload.line(), computer);
        final List<WireLine> wire = new ArrayList<>(response.lines().size());
        for (final var cliLine : response.lines()) {
            wire.add(WireLine.of(cliLine));
        }
        final String prompt = SshTerminal.prompt(localComputer, computer);
        final var handOver = response.handOver();
        /*
         * The command may have left a tool running, in which case the prompt does not come back with this
         * reply: what the tool says first goes out with it, and the rest arrives from the machine's own tick.
         */
        TerminalKeyboard keyboard = TerminalKeyboard.PROMPT;
        if (response.started() != null) {
            final TerminalTools.Turn opening = TerminalTools.started(host, level, payload.line(), response.started());
            wire.addAll(opening.lines());
            keyboard = opening.keyboard();
        }
        PacketDistributor.sendToPlayer(player, new CommandOutputPayload(response.clearScreen(), prompt, wire,
                handOver == null ? "" : handOver.editor(),
                handOver == null ? "" : handOver.path(), false,
                TerminalTools.settled(keyboard, localComputer, computer)));
        if (computer.firmwareRebootRequested()) {
            // "reboot --firmware": leave the terminal and enter the boot manager on the same monitor.
            player.closeContainer();
            MonitorBlock.openFirmware(
                    player, level, menu.monitorPos(), payload.hostPos());
            return;
        }
        if (computer.rebootRequested()) {
            /*
             * A plain "reboot": the system closes down in front of whoever is watching, and the POST replays on
             * the same monitor when it has finished, after which whatever the boot target now is (a freshly
             * installed OS included) comes up. A machine whose system has nothing to show on its way down goes
             * straight to the self-test, which is what the restart itself falls back to.
             */
            if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost be) {
                be.restart();
                /*
                 * Whoever typed it is at a terminal, not at a monitor session, so the machine's own goodbye
                 * never reached them: the closing-down went on behind the prompt they were still sitting in,
                 * and nothing moved until they left and came back.
                 */
                if (be.goingDown()) {
                    player.closeContainer();
                    MonitorBlock.openSystemDown(player, level, menu.monitorPos(), payload.hostPos(), be);
                    return;
                }
            }
            player.closeContainer();
            MonitorBlock.openPost(
                    player, level, menu.monitorPos(), payload.hostPos());
            return;
        }
        // Persist the typed line on the computer so the history survives closing the prompt or Monitor.
        if (host.console() != null && !payload.line().isBlank()) {
            host.console().pushHistory(payload.line().trim());
            ((BlockEntity) host).setChanged();
        }
    }

    static void launchProgram(final ServerPlayer player,
            final IComputerTerminalHost host,
            final BlockPos monitorPos, final BlockPos hostPos, final String name) {
        ProgramSpec program = null;
        for (final var candidate : Programs.all()) {
            if (candidate.commandName().equalsIgnoreCase(name)
                    || candidate.id().getPath().equalsIgnoreCase(name)
                    || candidate.id().toString().equalsIgnoreCase(name)) {
                program = candidate;
                break;
            }
        }
        if (program == null) {
            sendConsoleLine(player, ConsoleTexts.NO_SUCH_PROGRAM.with(name), OperationRecord.STATUS_FAILED);
            return;
        }
        final boolean installed = program.preinstalled()
                || (host.console() != null && host.console().isInstalled(program.id().toString()));
        if (!installed) {
            sendConsoleLine(player, ConsoleTexts.NOT_INSTALLED.with(program.commandName(), program.commandName()),
                    OperationRecord.STATUS_FAILED);
            return;
        }
        /*
         * Program run gate: the installed OS platform must be one the program supports, and the computer
         * must meet its CPU/VRAM minimums. Null-safe: programs with no declared requirement always pass.
         */
        if (player.level() instanceof ServerLevel osLevel
                && osLevel.getBlockEntity(hostPos) instanceof IOsHost osComputer
                && !OsRegistry.canRunProgram(
                        osComputer.installedOsId(), program.id(),
                        osComputer.maxCpuMhz(), osComputer.totalVramMb(),
                        osComputer.console() != null
                                && osComputer.console().builtFromSource(program.id().toString()))) {
            sendConsoleLine(player, ConsoleTexts.CANNOT_RUN.with(program.commandName()),
                    OperationRecord.STATUS_FAILED);
            player.displayClientMessage(GameText.component(ConsoleTexts.CANNOT_RUN_MESSAGE.with(
                    program.commandName())), false);
            return;
        }
        if (program.id().equals(Programs.NMS)) {
            // The NMS is now a desktop window opened from its Frames desktop icon, not a server-side menu.
            sendConsoleLine(player, ConsoleTexts.NMS_FROM_ICON.text(), -1);
            player.displayClientMessage(GameText.component(ConsoleTexts.NMS_FROM_ICON_MESSAGE), false);
        } else {
            sendConsoleLine(player, ConsoleTexts.ALREADY_OPEN.with(program.commandName()), -1);
        }
    }

    /*
     * One styled line back to the open Command Prompt (status -1 = dim, FAILED = red, else green). It goes as one
     * line in the player's language, and the glass wraps it to its columns the way it wraps everything it shows.
     */
    private static void sendConsoleLine(final ServerPlayer player, final Text text, final int status) {
        final CliStyle style = status == OperationRecord.STATUS_FAILED
                ? CliStyle.ERROR
                : status < 0 ? CliStyle.DIM
                : CliStyle.OK;
        // An empty prompt means "keep the current prompt", so this helper does not change the directory.
        PacketDistributor.sendToPlayer(player, new CommandOutputPayload(false, "",
                List.of(new WireLine(text, style.id()))));
    }

    private static void handleRequestConsoleInit(final RequestConsoleInitPayload payload, final ServerPlayer player,
                                                 final ServerLevel level) {
        if (player.containerMenu instanceof CommandPromptMenu menu
                && menu.hostPos().equals(payload.hostPos())
                && level.getBlockEntity(payload.hostPos())
                        instanceof IComputerTerminalHost host) {
            sendConsoleInit(player, host);
        }
    }

    private static void sendConsoleInit(final ServerPlayer player,
            final IComputerTerminalHost host) {
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
        final var cli = host instanceof BlockEntity
                ? new ServerCliComputer(host, player.serverLevel())
                : null;
        for (final var command : CliCommands.commandsFor(
                ServerCliComputer.shellFamilyOf(host), live)) {
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
        if (live && host instanceof IOsHost computer) {
            for (int i = 0; i < computer.diskSlots(); i++) {
                if (computer.diskInSlot(i).getItem()
                        instanceof DiskItem) {
                    devices.add("sd" + (char) ('a' + i));
                }
            }
        } else if (ServerCliComputer.shellFamilyOf(host)
                == ShellFamily.POSIX
                && player.level() instanceof ServerLevel serverLevel) {
            for (final var mount : new ServerCliComputer(
                    host, serverLevel).mounts()) {
                if (devices.size() < ConsoleInitPayload.MAX_DEVICES && mount.ready()) {
                    devices.add(mount.device());
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new ConsoleInitPayload(
                ((BlockEntity) host).getBlockPos(),
                List.copyOf(history), commands, devices));
        /*
         * A monitor opened while a tool is in front has to be told so, or it would show a prompt the machine
         * is not going to read from: the fetch would go on scrolling past a terminal that looked idle. One
         * opened on an idle machine is told what its prompt is instead, in the shell's own colours, so that
         * the terminal never has to guess at a prompt it has not been shown yet.
         */
        final TerminalKeyboard keyboard = TerminalTools.keyboardOf(host.console());
        if (keyboard.busy()) {
            PacketDistributor.sendToPlayer(player, new CommandOutputPayload("", List.of(), keyboard));
        } else if (cli != null) {
            PacketDistributor.sendToPlayer(player, new CommandOutputPayload(SshTerminal.prompt(cli, cli), List.of(),
                    TerminalTools.settled(keyboard, cli, cli)));
        }
    }

    private static void handleConsoleInit(final ConsoleInitPayload payload, final Player player) {
        CommandPromptScreen.acceptInit(payload);
    }

    private static void handleCommandOutput(final CommandOutputPayload payload, final Player player) {
        CommandPromptScreen.accept(payload);
    }

}
