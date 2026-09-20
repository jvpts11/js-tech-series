/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.client.os.ShellViews;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellRunPayload;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.SshTerminal;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static dev.jstech.computers.operation.payload.program.ConsolePayloads.CLI_WIDTH;

/**
 * The payloads of a shell window on the desktop: a command line run on the machine and the output that comes back.
 */
public final class DesktopShellPayloads {

    /**
     * What the shell sends when the player asks the program in front to stop.
     *
     * <p>The value is the single character U+0003, the one a terminal has always sent for this and one no
     * keyboard puts into a line of text, so nothing a player writes can be mistaken for it. An empty
     * line means something else entirely: the shell asking whether there is more output to show.
     */
    public static final String INTERRUPT = String.valueOf((char) 0x03);

    private DesktopShellPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, DesktopShellRunPayload.TYPE, DesktopShellRunPayload.STREAM_CODEC,
                ComputerAccess.machine(DesktopShellRunPayload::hostPos), DesktopShellPayloads::handleDesktopShellRun);
        registrar.playToClient(DesktopShellOutputPayload.TYPE, DesktopShellOutputPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(DesktopShellPayloads::handleDesktopShellOutput));
    }

    private static void handleDesktopShellRun(final DesktopShellRunPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        final List<WireLine> wire = new ArrayList<>();
        boolean clear = false;
        boolean busy = false;
        TerminalKeyboard keyboard = TerminalKeyboard.PROMPT;
        String prompt = "C:\\>";
        /* Set when the command was one that gives the terminal to an editor. */
        CliShell.HandOver handOver = null;
        if (level.getBlockEntity(payload.hostPos())
                instanceof IComputerTerminalHost host) {
            // Told who is typing, for the words that hand something to the player or take it from them.
            final var computer =
                    new ServerCliComputer(host, level, player);
            // The window's own shell: its directory is its own, and so is the reply.
            computer.useSession(payload.session());
            /*
             * A program has the terminal: everything typed goes to it, not to the shell, and what it
             * printed since the last time keeps coming until it returns.
             */
            final var running = computer.foreground();
            if (running != null) {
                busy = drainForeground(running, payload.line(), wire);
                PacketDistributor.sendToPlayer(player, new DesktopShellOutputPayload(false, busy,
                        SshTerminal.prompt(computer, computer),
                        wire, payload.session()));
                return;
            }
            /*
             * A tool is in front of the terminal: what is typed is the tool's, not the shell's. Sent to every
             * window rather than the one that typed, since a tool at a machine's console is in all of them.
             */
            final TerminalTools.Turn turn = TerminalTools.typed(host, level, payload.line());
            if (turn != null) {
                PacketDistributor.sendToPlayer(player, new DesktopShellOutputPayload(
                        turn.ended() ? SshTerminal.prompt(computer, computer) : "", turn.lines(), turn.keyboard()));
                return;
            }
            /*
             * A setup holds the prompt the way a running program does: the bar redraws until it is
             * done, and the one thing typed that means anything is the interrupt, which cancels it.
             */
            final var console = host.console();
            final var setup = console == null ? null : console.setup();
            if (setup != null && host instanceof IOsHost machine) {
                if (INTERRUPT.equals(payload.line())) {
                    SetupRunner.cancel(machine, level, payload.hostPos());
                    PacketDistributor.sendToPlayer(player, new DesktopShellOutputPayload(false, false,
                            SshTerminal.prompt(computer, computer),
                            wire, payload.session()));
                    return;
                }
                wire.add(new WireLine(
                        (setup.removing() ? "Removing " : "Setting up ") + setup.name() + "  "
                                + (setup.permille() / 10) + "%  (Ctrl+C to cancel)",
                        CliStyle.DIM.id()));
                PacketDistributor.sendToPlayer(player, new DesktopShellOutputPayload(false, true,
                        SshTerminal.prompt(computer, computer),
                        wire, payload.session()));
                return;
            }
            /*
             * An open ssh session runs the line on the far machine, in its own shell family; this window is
             * only the glass it is read through. Everything else, ssh itself and exit, stays here.
             */
            var shellOn = computer;
            final var target = SshTerminal.targetOf(
                    host, level, payload.line());
            if (target != null) {
                shellOn = new ServerCliComputer(target, level);
            }
            /*
             * Laid out to the width of the window that asked, not to a monitor's. A machine writes in
             * columns, and columns wider than the glass they are read on fold in half.
             */
            final var shell = CliCommands.shellFor(
                    shellOn, payload.columns());
            final var response = shell.run(payload.line(), shellOn);
            clear = response.clearScreen();
            handOver = response.handOver();
            for (final var cliLine : response.lines()) {
                wire.add(WireLine.of(cliLine));
            }
            prompt = SshTerminal.prompt(computer, shellOn);
            /*
             * The command just run may have been one that starts a program at this terminal, in
             * which case the prompt does not come back with this reply.
             */
            busy = computer.foreground() != null;
            /*
             * Or one that leaves a tool running in front of it, which says first whatever it has to say at
             * once; the rest arrives from the machine's own tick.
             */
            if (response.started() != null) {
                final TerminalTools.Turn opening =
                        TerminalTools.started(host, level, payload.line(), response.started());
                wire.addAll(opening.lines());
                keyboard = opening.keyboard();
                busy = busy || keyboard.busy();
            }
            /*
             * The reboot verbs work from the desktop's terminal window too: the desktop closes and the
             * monitor either replays the POST (plain reboot) or enters the firmware setup.
             */
            final BlockPos monitorPos = player.containerMenu
                    instanceof DesktopMenu desktop
                    ? desktop.monitorPos() : null;
            if (monitorPos != null && computer.firmwareRebootRequested()) {
                player.closeContainer();
                MonitorBlock.openFirmware(
                        player, level, monitorPos, payload.hostPos());
                return;
            }
            if (monitorPos != null && computer.rebootRequested()) {
                /*
                 * The system closes down in front of whoever is watching and the self-test follows when it has
                 * finished; one with nothing to show on its way down starts over at once, and then the POST is
                 * opened here as it always was.
                 */
                if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost be) {
                    be.restart();
                    /*
                     * Whoever asked for it is at the desktop, not at a monitor session, so the machine's own
                     * goodbye never reached them: the closing-down went on behind the desktop they were still
                     * sitting in, and nothing moved until they left and came back.
                     */
                    if (be.goingDown()) {
                        player.closeContainer();
                        MonitorBlock.openSystemDown(player, level, monitorPos, payload.hostPos(), be);
                        return;
                    }
                }
                player.closeContainer();
                MonitorBlock.openPost(
                        player, level, monitorPos, payload.hostPos());
                return;
            }
        }
        PacketDistributor.sendToPlayer(player, new DesktopShellOutputPayload(clear, busy, prompt, wire,
                handOver == null ? "" : handOver.editor(),
                handOver == null ? "" : handOver.path(), payload.session(), false, false, keyboard));
    }

    /**
     * Answers a line typed while a program has the terminal, and says whether it still has it.
     *
     * <p>What a program prints reaches the terminal from the machine's own tick, so nothing is
     * collected here: this is only the keyboard. While a program is in front, a line typed is the
     * program's to read, and the interrupt is the one thing that means something to the terminal itself.
     */
    private static boolean drainForeground(
            final MachinePrograms processes, final String typed,
            final List<WireLine> wire) {
        if (!INTERRUPT.equals(typed)) {
            processes.offerInput(typed);
            return true;
        }
        final int id = processes.held();
        processes.release();
        processes.stop(id);
        wire.add(new WireLine("^C",
                CliStyle.DIM.id()));
        return false;
    }

    private static void handleDesktopShellOutput(final DesktopShellOutputPayload payload, final Player player) {
        /*
         * A computer has one console and this is what it said, so it goes to every window looking at
         * it: the terminal window and an editor's terminal panel. Each ignores it when it is not open.
         */
        ShellViews.accept(payload);
    }
}
