/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellRunPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Set;

import static dev.jstech.computers.operation.payload.program.ConsolePayloads.CLI_WIDTH;

/**
 * The payloads of a shell window on the desktop: a command line run on the machine and the output that comes back.
 */
public final class DesktopShellPayloads {

    private DesktopShellPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, DesktopShellRunPayload.TYPE, DesktopShellRunPayload.STREAM_CODEC,
                ComputerAccess.machine(DesktopShellRunPayload::hostPos), DesktopShellPayloads::handleDesktopShellRun);
        registrar.playToClient(DesktopShellOutputPayload.TYPE, DesktopShellOutputPayload.STREAM_CODEC,
                DesktopShellPayloads::handleDesktopShellOutput);
    }

    private static void handleDesktopShellRun(final DesktopShellRunPayload payload,
                                              final IPayloadContext context) {
        context.enqueueWork(() -> {
            final java.util.List<DesktopShellOutputPayload.WireLine> wire = new java.util.ArrayList<>();
            boolean clear = false;
            boolean busy = false;
            String prompt = "C:\\>";
            /* Set when the command was one that gives the terminal to an editor. */
            dev.jstech.computers.program.cli.CliShell.HandOver handOver = null;
            if (context.player() instanceof ServerPlayer player
                    && player.level() instanceof ServerLevel level
                    && level.getBlockEntity(payload.hostPos())
                            instanceof dev.jstech.computers.terminal.IComputerTerminalHost host) {
                final var computer =
                        new dev.jstech.computers.program.ServerCliComputer(host, level);
                // The window's own shell: its directory is its own, and so is the reply.
                computer.useSession(payload.session());
                /*
                 * A program has the terminal: everything typed goes to it, not to the shell, and what it
                 * printed since the last time keeps coming until it returns.
                 */
                final var running = computer.foreground();
                if (running != null) {
                    busy = drainForeground(running, payload.line(), wire);
                    context.reply(new DesktopShellOutputPayload(false, busy, computer.prompt(), wire,
                            payload.session()));
                    return;
                }
                /*
                 * A setup holds the prompt the way a running program does: the bar redraws until it is
                 * done, and the one thing typed that means anything is the interrupt, which cancels it.
                 */
                final var console = host.console();
                final var setup = console == null ? null : console.setup();
                if (setup != null && host instanceof dev.jstech.computers.os.IOsHost machine) {
                    if (INTERRUPT.equals(payload.line())) {
                        dev.jstech.computers.os.install.SetupRunner.cancel(machine, level, payload.hostPos());
                        context.reply(new DesktopShellOutputPayload(false, false, computer.prompt(), wire,
                                payload.session()));
                        return;
                    }
                    wire.add(new DesktopShellOutputPayload.WireLine(
                            (setup.removing() ? "Removing " : "Setting up ") + setup.name() + "  "
                                    + (setup.permille() / 10) + "%  (Ctrl+C to cancel)",
                            dev.jstech.computers.program.cli.CliStyle.DIM.ordinal()));
                    context.reply(new DesktopShellOutputPayload(false, true, computer.prompt(), wire,
                            payload.session()));
                    return;
                }
                final var shell = dev.jstech.computers.program.cli.CliCommands.shellFor(
                        computer, CLI_WIDTH);
                final var response = shell.run(payload.line(), computer);
                clear = response.clearScreen();
                handOver = response.handOver();
                for (final var cliLine : response.lines()) {
                    wire.add(new DesktopShellOutputPayload.WireLine(cliLine.text(), cliLine.style().ordinal()));
                }
                prompt = computer.prompt();
                /*
                 * The command just run may have been one that starts a program at this terminal, in
                 * which case the prompt does not come back with this reply.
                 */
                busy = computer.foreground() != null;
                /*
                 * The reboot verbs work from the desktop's terminal window too: the desktop closes and the
                 * monitor either replays the POST (plain reboot) or enters the firmware setup.
                 */
                final BlockPos monitorPos = player.containerMenu
                        instanceof dev.jstech.computers.menu.DesktopMenu desktop
                        ? desktop.monitorPos() : null;
                if (monitorPos != null && computer.firmwareRebootRequested()) {
                    player.closeContainer();
                    dev.jstech.computers.block.MonitorBlock.openFirmware(
                            player, level, monitorPos, payload.hostPos());
                    return;
                }
                if (monitorPos != null && computer.rebootRequested()) {
                    if (level.getBlockEntity(payload.hostPos()) instanceof dev.jstech.computers.os.IOsHost be) {
                        be.setNeedsPost(true);
                    }
                    player.closeContainer();
                    dev.jstech.computers.block.MonitorBlock.openPost(
                            player, level, monitorPos, payload.hostPos());
                    return;
                }
            }
            context.reply(new DesktopShellOutputPayload(clear, busy, prompt, wire,
                    handOver == null ? "" : handOver.editor(),
                    handOver == null ? "" : handOver.path(), payload.session()));
        });
    }

    /**
     * What the shell sends when the player asks the program in front to stop.
     *
     * <p>The value is the single byte U+0003, the one a terminal has always sent for this and one no
     * keyboard puts into a line of text, so nothing a player writes can be mistaken for it. An empty
     * line means something else entirely: the shell asking whether there is more output to show.
     */
    public static final String INTERRUPT = "";

    /**
     * Answers a line typed while a program has the terminal, and says whether it still has it.
     *
     * <p>What a program prints reaches the terminal from the machine's own tick, so nothing is
     * collected here: this is only the keyboard. While a program is in front, a line typed is the
     * program's to read, and the interrupt is the one thing that means something to the terminal itself.
     */
    private static boolean drainForeground(
            final dev.jstech.computers.cannon.machine.MachinePrograms processes, final String typed,
            final java.util.List<DesktopShellOutputPayload.WireLine> wire) {
        if (!INTERRUPT.equals(typed)) {
            processes.offerInput(typed);
            return true;
        }
        final int id = processes.held();
        processes.release();
        processes.stop(id);
        wire.add(new DesktopShellOutputPayload.WireLine("^C",
                dev.jstech.computers.program.cli.CliStyle.DIM.ordinal()));
        return false;
    }

    private static void handleDesktopShellOutput(final DesktopShellOutputPayload payload,
                                                 final IPayloadContext context) {
        context.enqueueWork(() -> {
            /*
             * A computer has one console and this is what it said, so it goes to every window looking at
             * it: the terminal window and an editor's terminal panel. Each ignores it when it is not open.
             */
            dev.jstech.computers.client.os.ShellViews.accept(payload);
        });
    }
}
