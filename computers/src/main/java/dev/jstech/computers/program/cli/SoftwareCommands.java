/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.Programs;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * The commands that install, list and remove software, and the services a network runs.
 *
 * <p>Moved here from BuiltinCommands, which keeps the lists that say which system gets which command.
 */
final class SoftwareCommands {

    /** What a network's own services are worked from: the Mainframe that orchestrates it, on any system. */
    private static final CommandScope MAINFRAME_SERVICE = CommandScope.everywhere()
            .onHost(HostScope.MAINFRAME).needing(CommandScope.Need.NETWORK);

    private SoftwareCommands() {
    }

    /**
     * The package manager every Frames edition ships with: one verb set over the network Mirror, so a
     * player who never touches a Linux distribution still installs, removes, searches and updates
     * software the same way. Linux distributions keep their own managers (apt, dnf, pacman, emerge);
     * this is the Frames-side equivalent, and it speaks to the same Mirror.
     */
    static final class Pckmgr implements ICliCommand {
        /** The Frames family's package manager, over the network's Mirror. MC-DOS installs from media alone. */
        @Override public CommandScope scope() {
            return CommandScope.on(Platform.MC_NET, Platform.FRAMES).needing(CommandScope.Need.NETWORK);
        }

        @Override public String name() {
            return "pckmgr";
        }

        @Override public String summary() {
            return "install, remove, search and update packages from the network mirror";
        }

        @Override public String usage() {
            return "pckmgr install|remove|search|list|update [name]";
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer computer = ctx.computer();
            final String verb = ctx.hasArgs() ? ctx.arg(0).toLowerCase(Locale.ROOT) : "";
            switch (verb) {
                // This manager ships what it installs already built, so everything it has to say is said at once.
                case "install" -> requireName(ctx, name -> computer.packageInstall(name, false).said());
                case "remove", "uninstall" -> requireName(ctx, computer::packageRemove);
                case "update", "upgrade" -> report(ctx, computer.packageUpdate());
                /*
                 * search always looks at the whole shelf; list shows this computer's packages unless
                 * --available asks for everything the mirror offers.
                 */
                case "search" -> listPackages(ctx, ctx.argCount() > 1 ? ctx.arg(1) : "", false);
                case "list" -> {
                    final String flag = ctx.argCount() > 1 ? ctx.arg(1) : "";
                    /*
                     * A mistyped flag must say so: silently listing something else is how a typo
                     * becomes "the feature is broken".
                     */
                    if (!flag.isEmpty() && !flag.equalsIgnoreCase("--available")) {
                        ctx.out().error("pckmgr list: unknown option " + flag + " (did you mean --available?)");
                    } else {
                        listPackages(ctx, "", flag.isEmpty());
                    }
                }
                default -> {
                    ctx.out().error("usage: " + usage());
                    ctx.out().line("  install <name>   fetch and set up a package");
                    ctx.out().line("  remove <name>    uninstall a package");
                    ctx.out().line("  search [text]    find packages the mirror offers");
                    ctx.out().line("  list [--available]  installed packages, or everything on offer");
                    ctx.out().line("  update           bring installed packages to the current build");
                }
            }
        }

        private static void requireName(final CliContext ctx,
                                        final Function<String, ICliComputer.OpResult> action) {
            if (ctx.argCount() < 2) {
                ctx.out().error("pckmgr: this verb needs a package name");
                return;
            }
            report(ctx, action.apply(ctx.arg(1)));
        }

        private static void report(final CliContext ctx, final ICliComputer.OpResult result) {
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            // A manager speaks in several lines; the last of them is the one that says it went well.
            final String[] parts = result.message().split("\n");
            for (int i = 0; i < parts.length; i++) {
                if (i == parts.length - 1) {
                    ctx.out().ok(parts[i]);
                } else {
                    ctx.out().line(parts[i]);
                }
            }
        }

        /**
         * Lists packages from the mirror, marking what this computer already has.
         *
         * @param filter        matches name or description; empty matches everything
         * @param onlyInstalled true for plain {@code list} (this computer's packages), false for
         *                      {@code search} and {@code list --available} (the whole shelf)
         */
        private static void listPackages(final CliContext ctx, final String filter,
                                         final boolean onlyInstalled) {
            final List<ICliComputer.PackageInfo> packages = ctx.computer().packagesAvailable();
            if (packages.isEmpty()) {
                ctx.out().error("could not resolve mirror:// - no package source reachable");
                return;
            }
            final String needle = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
            int shown = 0;
            for (final ICliComputer.PackageInfo info : packages) {
                if (onlyInstalled && !info.installed()) {
                    continue;
                }
                if (!needle.isEmpty() && !info.name().toLowerCase(Locale.ROOT).contains(needle)
                        && !info.description().toLowerCase(Locale.ROOT).contains(needle)) {
                    continue;
                }
                /*
                 * Something another player wrote says so. Whether to install it is then an informed
                 * choice rather than a guess about where it came from.
                 */
                final String state = info.installed() ? "installed" : info.community() ? "community" : "available";
                ctx.out().row("  " + info.name() + "  [" + state + "]", info.description());
                shown++;
            }
            if (shown == 0) {
                ctx.out().line(onlyInstalled ? "No packages installed."
                        : needle.isEmpty() ? "The mirror offers nothing for this computer."
                                : "No package matches " + filter + ".");
            }
        }
    }

    static final class ProgramsList implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "programs";
        }

        @Override public List<String> aliases() {
            return List.of("apps");
        }

        @Override public String summary() {
            return "list installed programs";
        }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.ProgramInfo> programs = ctx.computer().programs();
            if (programs.isEmpty()) {
                ctx.out().dim("no programs installed");
                return;
            }
            for (final ICliComputer.ProgramInfo program : programs) {
                ctx.out().row("  " + program.name(), program.id());
            }
        }
    }

    static final class Install implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "install";
        }

        @Override public String summary() {
            return "install a program on this computer";
        }

        @Override public String usage() {
            return "<program-id>";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: install <program-id>   (see 'programs')");
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().install(ctx.arg(0));
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    static final class Uninstall implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "uninstall";
        }

        @Override public String summary() {
            return "remove an installed program from this computer";
        }

        @Override public String usage() {
            return "<program-id>";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: uninstall <program-id>   (see 'programs')");
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().packageRemove(ctx.arg(0));
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    static final class Store implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "store";
        }

        @Override public List<String> aliases() {
            return List.of("available");
        }

        @Override public String summary() {
            return "list programs you can install on this computer";
        }

        @Override public void run(final CliContext ctx) {
            boolean any = false;
            for (final ProgramSpec program
                    : Programs.all()) {
                if (program.preinstalled()) {
                    continue;
                }
                ctx.out().row("  " + program.commandName(), "install " + program.commandName());
                any = true;
            }
            if (!any) {
                ctx.out().dim("nothing else to install");
            }
        }
    }

    static final class IqlEngineCommand implements ICliCommand {
        /** A service of the network, run from the machine that orchestrates it. */
        @Override public CommandScope scope() {
            return MAINFRAME_SERVICE;
        }

        @Override public String name() {
            return "iqlengine";
        }

        @Override public List<String> aliases() {
            return List.of("engine");
        }

        @Override public String summary() {
            return "start/stop the network's IQL Engine service";
        }

        @Override public String usage() {
            return "start|stop|status";
        }

        @Override public boolean available(final ICliComputer computer) {
            // The scope says where, and the engine itself has to be installed on top of that.
            return ICliCommand.super.available(computer) && computer.iqlEngineInstalled();
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.OpResult result = ctx.computer().engineControl(ctx.hasArgs() ? ctx.arg(0) : "status");
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    static final class Services implements ICliCommand {
        @Override public CommandScope scope() {
            return MAINFRAME_SERVICE;
        }

        @Override public String name() {
            return "services";
        }

        @Override public String summary() {
            return "list the network's services and their state";
        }

        @Override public boolean available(final ICliComputer computer) {
            return ICliCommand.super.available(computer) && computer.iqlEngineInstalled();
        }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.ServiceStatus> services = ctx.computer().services();
            if (services.isEmpty()) {
                ctx.out().dim("no services");
                return;
            }
            for (final ICliComputer.ServiceStatus service : services) {
                ctx.out().row("  " + service.name(), service.state());
            }
        }
    }

    static final class Maint implements ICliCommand {

        private final String verb;
        private final String action;

        Maint(final String verb, final String action) {
            this.verb = verb;
            this.action = action;
        }

        /** The storage index is the Mainframe's, so its upkeep is offered nowhere else, whatever the system. */
        @Override public CommandScope scope() {
            return MAINFRAME_SERVICE;
        }

        @Override public String name() {
            return verb;
        }

        @Override public String summary() {
            return "mainframe: " + verb + " the storage index";
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.OpResult result = ctx.computer().maintenance(action);
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    /**
     * Lists the files on the system disk. Each entry shows the file name, its size in mB-equivalents,
     * and a {@code [RO]} marker for read-only {@code .dat} projection entries.
     */
    /**
     * Restarts the computer. With {@code --firmware} the restart lands in the firmware setup (the boot
     * manager) instead of the installed OS, which is how the player reaches it once a system is installed.
     */
    /** Installs or reports the Mirror, the Mainframe's package repository the Linux package managers use. */
    static final class MirrorCommand implements ICliCommand {
        @Override public CommandScope scope() {
            return MAINFRAME_SERVICE;
        }

        @Override public String name() { return "mirror"; }

        @Override public String summary() { return "install or check the Mirror package service on the Mainframe"; }

        @Override public String usage() { return "install|status"; }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.OpResult result = ctx.computer().mirrorControl(ctx.hasArgs() ? ctx.arg(0) : "status");
            if (result.ok()) {
                ctx.out().ok(result.message());
            } else {
                ctx.out().error(result.message());
            }
        }
    }
}
