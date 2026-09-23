/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import java.util.List;

/**
 * The commands about the computer itself: how it is doing, how it is set and how it is restarted.
 *
 * <p>Moved here from BuiltinCommands, which keeps the lists that say which system gets which command.
 */
final class MachineCommands {

    private MachineCommands() {
    }

    static final class Status implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "status";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public List<String> aliases() {
            return List.of("stat");
        }

        @Override public String summary() {
            return "show power, cpu, ram and link";
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer c = ctx.computer();
            ctx.out().styled(c.running() ? "ONLINE" : "OFFLINE", c.running() ? CliStyle.OK : CliStyle.ERROR);
            ctx.out().row("cpu", CliText.group(c.cpuCapacity()) + " it/t");
            ctx.out().row("ram", CliText.group(c.ramBuffer()) + " it");
            ctx.out().row("network", c.onNetwork() ? "linked (" + c.networkId() + ")" : "--");
        }
    }

    /** Shows or changes this computer's settings, the MC-DOS front-end for the Settings app. */
    static final class Config implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() { return "config"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public String summary() { return "show or change this computer's settings"; }

        @Override public String usage() { return "[key] [value]"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                final List<String> lines = ctx.computer().configSummary();
                if (lines.isEmpty()) {
                    ctx.out().error("this computer has no settings store");
                    return;
                }
                ctx.out().header("settings");
                for (final String line : lines) {
                    ctx.out().line(line);
                }
                ctx.out().dim("'config <key> <value>' to change one");
                return;
            }
            if (ctx.argCount() < 2) {
                ctx.out().error("usage: config <key> <value>  (or 'config' to list)");
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().setConfig(ctx.arg(0), ctx.rest(1));
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    static final class Reboot implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() { return "reboot"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public List<String> aliases() { return List.of("restart"); }

        @Override public String summary() { return "restart the computer (--firmware: into the firmware setup)"; }

        @Override public String usage() { return "[--firmware]"; }

        @Override public void run(final CliContext ctx) {
            final boolean firmware = ctx.hasArgs() && ctx.arg(0).equals("--firmware");
            if (firmware) {
                ctx.out().dim("Restarting into the firmware setup ...");
                ctx.computer().requestFirmwareReboot();
            } else {
                ctx.out().dim("The system is going down for reboot NOW!");
                ctx.computer().requestReboot();
            }
        }
    }

    static final class Devices implements ICliCommand {
        /** Only where something can hang off the computer at all. */
        @Override public CommandScope scope() {
            return CommandScope.everywhere().needing(CommandScope.Need.PORTS);
        }

        @Override public String name() {
            return "devices";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public List<String> aliases() {
            return List.of("dev", "peripherals");
        }

        @Override public String summary() {
            return "list linked peripherals";
        }

        @Override public void run(final CliContext ctx) {
            final List<String> devices = ctx.computer().peripherals();
            if (devices.isEmpty()) {
                ctx.out().dim("no peripherals linked");
                return;
            }
            for (final String device : devices) {
                ctx.out().line("  " + device);
            }
        }
    }
}
