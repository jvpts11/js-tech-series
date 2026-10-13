/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * The commands about the computer itself: how it is doing, how it is set and how it is restarted.
 *
 * <p>Moved here from BuiltinCommands, which keeps the lists that say which system gets which command.
 */
final class MachineCommands {

    private MachineCommands() {
    }

    @TextHolder
    static final class Status implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.machine.status.summary", "show power, cpu, ram and link");
        private static final TextKey ONLINE = TextKey.of("jsc.cli.machine.status.online", "ONLINE");
        private static final TextKey OFFLINE = TextKey.of("jsc.cli.machine.status.offline", "OFFLINE");
        private static final TextKey CPU = TextKey.of("jsc.cli.machine.status.cpu", "cpu");
        private static final TextKey RAM = TextKey.of("jsc.cli.machine.status.ram", "ram");
        private static final TextKey NETWORK = TextKey.of("jsc.cli.machine.status.network", "network");
        private static final TextKey LINKED = TextKey.of("jsc.cli.machine.status.linked", "linked (%s)");

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

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer c = ctx.computer();
            ctx.out().styled(c.running() ? ONLINE.text() : OFFLINE.text(), c.running() ? CliStyle.OK : CliStyle.ERROR);
            // The units are written the way every listing of the mod writes them, so they are data here.
            ctx.out().row(CPU.text(), Text.literal(CliText.group(c.cpuCapacity()) + " it/t"));
            ctx.out().row(RAM.text(), Text.literal(CliText.group(c.ramBuffer()) + " it"));
            ctx.out().row(NETWORK.text(), c.onNetwork() ? LINKED.with(c.networkId()) : Text.literal("--"));
        }
    }

    /** Shows or changes this computer's settings, the MC-DOS front-end for the Settings app. */
    @TextHolder
    static final class Config implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.machine.config.summary", "show or change this computer's settings");
        private static final TextKey USAGE = TextKey.of("jsc.cli.machine.config.usage", "[key] [value]");
        private static final TextKey SET_USAGE =
                TextKey.of("jsc.cli.machine.config.set_usage", "<key> <value>  (or 'config' to list)");
        private static final TextKey NO_STORE =
                TextKey.of("jsc.cli.machine.config.no_store", "this computer has no settings store");
        private static final TextKey SETTINGS = TextKey.of("jsc.cli.machine.config.settings", "settings");
        private static final TextKey TO_CHANGE =
                TextKey.of("jsc.cli.machine.config.to_change", "'config <key> <value>' to change one");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() { return "config"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                final List<String> lines = ctx.computer().configSummary();
                if (lines.isEmpty()) {
                    ctx.out().error(NO_STORE);
                    return;
                }
                ctx.out().header(SETTINGS);
                for (final String line : lines) {
                    ctx.out().line(line);
                }
                ctx.out().dim(TO_CHANGE);
                return;
            }
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), SET_USAGE));
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().setConfig(ctx.arg(0), ctx.rest(1));
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    @TextHolder
    static final class Reboot implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.machine.reboot.summary",
                "restart the computer (--firmware: into the firmware setup)");
        private static final TextKey USAGE = TextKey.of("jsc.cli.machine.reboot.usage", "[--firmware]");
        private static final TextKey TO_FIRMWARE =
                TextKey.of("jsc.cli.machine.reboot.to_firmware", "Restarting into the firmware setup ...");
        private static final TextKey GOING_DOWN =
                TextKey.of("jsc.cli.machine.reboot.going_down", "The system is going down for reboot NOW!");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() { return "reboot"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public List<String> aliases() { return List.of("restart"); }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            final boolean firmware = ctx.hasArgs() && ctx.arg(0).equals("--firmware");
            if (firmware) {
                ctx.out().dim(TO_FIRMWARE);
                ctx.computer().requestFirmwareReboot();
            } else {
                ctx.out().dim(GOING_DOWN);
                ctx.computer().requestReboot();
            }
        }
    }

    @TextHolder
    static final class Devices implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.machine.devices.summary", "list linked peripherals");
        private static final TextKey NONE = TextKey.of("jsc.cli.machine.devices.none", "no peripherals linked");

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

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            final List<String> devices = ctx.computer().peripherals();
            if (devices.isEmpty()) {
                ctx.out().dim(NONE);
                return;
            }
            for (final String device : devices) {
                ctx.out().line("  " + device);
            }
        }
    }
}
