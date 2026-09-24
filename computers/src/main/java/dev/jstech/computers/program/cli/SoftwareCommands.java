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
import dev.jstech.computers.program.iql.IqlVerb;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    /**
     * A package manager over the network's Mirror, under whichever name its family gave it.
     *
     * <p>The Frames family calls it {@code pckmgr} and the network appliance calls it {@code netgetter}, and
     * they are the same manager fetching from the same mirror: one implementation wearing two names, so a
     * fix to how packages install is a fix on both machines. MC-DOS has neither and installs from media.
     */
    @TextHolder
    static final class Pckmgr implements ICliCommand {

        private final String name;
        private final Set<Platform> systems;

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.software.pckmgr.summary",
                "install, remove, search and update packages from the network mirror");
        private static final TextKey USAGE = TextKey.of("jsc.cli.software.pckmgr.usage",
                "install|remove|search|list|update [name]");
        private static final TextKey UNKNOWN_OPTION = TextKey.of("jsc.cli.software.pckmgr.unknown_option",
                "%s list: unknown option %s (did you mean --available?)");
        /* The verbs as the usage lists them, each with what it does; the spacing keeps the second column straight. */
        private static final TextKey VERB_INSTALL = TextKey.of("jsc.cli.software.pckmgr.verb.install",
                "  install <name>   fetch and set up a package");
        private static final TextKey VERB_REMOVE = TextKey.of("jsc.cli.software.pckmgr.verb.remove",
                "  remove <name>    uninstall a package");
        private static final TextKey VERB_SEARCH = TextKey.of("jsc.cli.software.pckmgr.verb.search",
                "  search [text]    find packages the mirror offers");
        private static final TextKey VERB_LIST = TextKey.of("jsc.cli.software.pckmgr.verb.list",
                "  list [--available]  installed packages, or everything on offer");
        private static final TextKey VERB_UPDATE = TextKey.of("jsc.cli.software.pckmgr.verb.update",
                "  update           bring installed packages to the current build");
        private static final TextKey NEEDS_NAME = TextKey.of("jsc.cli.software.pckmgr.needs_name",
                "this verb needs a package name");
        private static final TextKey NO_MIRROR = TextKey.of("jsc.cli.software.pckmgr.no_mirror",
                "could not resolve mirror:// - no package source reachable");
        /* A package's name and where it stands with this computer, as a row of the listing opens. */
        private static final TextKey LISTED = TextKey.of("jsc.cli.software.pckmgr.listed", "  %s  [%s]");
        private static final TextKey INSTALLED = TextKey.of("jsc.cli.software.pckmgr.installed", "installed");
        private static final TextKey COMMUNITY = TextKey.of("jsc.cli.software.pckmgr.community", "community");
        private static final TextKey AVAILABLE = TextKey.of("jsc.cli.software.pckmgr.available", "available");
        private static final TextKey NONE_INSTALLED = TextKey.of("jsc.cli.software.pckmgr.none_installed",
                "No packages installed.");
        private static final TextKey NOTHING_OFFERED = TextKey.of("jsc.cli.software.pckmgr.nothing_offered",
                "The mirror offers nothing for this computer.");
        private static final TextKey NO_MATCH = TextKey.of("jsc.cli.software.pckmgr.no_match",
                "No package matches %s.");

        Pckmgr(final String name, final Set<Platform> systems) {
            this.name = name;
            this.systems = systems;
        }

        @Override public CommandScope scope() {
            return CommandScope.on(this.systems).needing(CommandScope.Need.NETWORK);
        }

        @Override public String name() {
            return this.name;
        }

        @Override public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
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
                        ctx.out().error(UNKNOWN_OPTION.with(this.name, flag));
                    } else {
                        listPackages(ctx, "", flag.isEmpty());
                    }
                }
                default -> {
                    ctx.out().error(CliTexts.USAGE.with(this.name, usage()));
                    ctx.out().line(VERB_INSTALL);
                    ctx.out().line(VERB_REMOVE);
                    ctx.out().line(VERB_SEARCH);
                    ctx.out().line(VERB_LIST);
                    ctx.out().line(VERB_UPDATE);
                }
            }
        }

        private void requireName(final CliContext ctx,
                                 final Function<String, ICliComputer.OpResult> action) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.SAID_BY.with(this.name, NEEDS_NAME));
                return;
            }
            report(ctx, action.apply(ctx.arg(1)));
        }

        private static void report(final CliContext ctx, final ICliComputer.OpResult result) {
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            PackageCommands.done(ctx, result.message());
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
                ctx.out().error(NO_MIRROR);
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
                final TextKey state = info.installed() ? INSTALLED : info.community() ? COMMUNITY : AVAILABLE;
                ctx.out().row(LISTED.with(info.name(), state), Text.literal(info.description()));
                shown++;
            }
            if (shown == 0) {
                ctx.out().line(onlyInstalled ? NONE_INSTALLED.text()
                        : needle.isEmpty() ? NOTHING_OFFERED.text() : NO_MATCH.with(filter));
            }
        }
    }

    @TextHolder
    static final class ProgramsList implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.software.programs.summary",
                "list installed programs");
        private static final TextKey NONE = TextKey.of("jsc.cli.software.programs.none", "no programs installed");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "programs";
        }

        @Override public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override public List<String> aliases() {
            return List.of("apps");
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.ProgramInfo> programs = ctx.computer().programs();
            if (programs.isEmpty()) {
                ctx.out().dim(NONE);
                return;
            }
            for (final ICliComputer.ProgramInfo program : programs) {
                ctx.out().row("  " + program.name(), program.id());
            }
        }
    }

    @TextHolder
    static final class Install implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.software.install.summary",
                "install a program on this computer");
        private static final TextKey USAGE = TextKey.of("jsc.cli.software.install.usage", "<program-id>");
        private static final TextKey SEE_PROGRAMS = TextKey.of("jsc.cli.software.install.see_programs",
                "usage: install <program-id>   (see 'programs')");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "install";
        }

        @Override public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(SEE_PROGRAMS);
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().install(ctx.arg(0));
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    @TextHolder
    static final class Uninstall implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.software.uninstall.summary",
                "remove an installed program from this computer");
        private static final TextKey USAGE = TextKey.of("jsc.cli.software.uninstall.usage", "<program-id>");
        private static final TextKey SEE_PROGRAMS = TextKey.of("jsc.cli.software.uninstall.see_programs",
                "usage: uninstall <program-id>   (see 'programs')");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "uninstall";
        }

        @Override public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(SEE_PROGRAMS);
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().packageRemove(ctx.arg(0));
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    @TextHolder
    static final class Store implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.software.store.summary",
                "list programs you can install on this computer");
        private static final TextKey NOTHING_ELSE = TextKey.of("jsc.cli.software.store.nothing_else",
                "nothing else to install");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "store";
        }

        @Override public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override public List<String> aliases() {
            return List.of("available");
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            boolean any = false;
            for (final ProgramSpec program
                    : Programs.all()) {
                if (program.preinstalled()) {
                    continue;
                }
                // The line to type to install it, which is the same whatever language the reader has.
                ctx.out().row(Text.literal("  " + program.commandName()),
                        Text.literal("install " + program.commandName()));
                any = true;
            }
            if (!any) {
                ctx.out().dim(NOTHING_ELSE);
            }
        }
    }

    @TextHolder
    static final class IqlEngineCommand implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.software.iqlengine.summary",
                "start/stop the network's IQL Engine service");

        /** A service of the network, run from the machine that orchestrates it. */
        @Override public CommandScope scope() {
            return MAINFRAME_SERVICE;
        }

        @Override public String name() {
            return "iqlengine";
        }

        /** Starting and stopping one of the network's services, which is where the services command stands too. */
        @Override public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override public List<String> aliases() {
            return List.of("engine");
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        /** Nothing but the words it takes, which are typed as they are in every language. */
        @Override public Text usage() {
            return Text.literal("start|stop|status");
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

    @TextHolder
    static final class Services implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.software.services.summary",
                "list the network's services and their state");
        private static final TextKey NONE = TextKey.of("jsc.cli.software.services.none", "no services");

        @Override public CommandScope scope() {
            return MAINFRAME_SERVICE;
        }

        @Override public String name() {
            return "services";
        }

        @Override public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public boolean available(final ICliComputer computer) {
            return ICliCommand.super.available(computer) && computer.iqlEngineInstalled();
        }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.ServiceStatus> services = ctx.computer().services();
            if (services.isEmpty()) {
                ctx.out().dim(NONE);
                return;
            }
            for (final ICliComputer.ServiceStatus service : services) {
                ctx.out().row("  " + service.name(), service.state());
            }
        }
    }

    @TextHolder
    static final class Maint implements ICliCommand {

        private final String verb;
        private final IqlVerb action;

        /* What each upkeep does, in its own sentence, since a verb's word is not a word a translator can move. */
        private static final TextKey ANALYZE_SUMMARY = TextKey.of("jsc.cli.software.maint.analyze.summary",
                "mainframe: analyze the storage index");
        private static final TextKey REINDEX_SUMMARY = TextKey.of("jsc.cli.software.maint.reindex.summary",
                "mainframe: reindex the storage index");
        private static final TextKey VACUUM_SUMMARY = TextKey.of("jsc.cli.software.maint.vacuum.summary",
                "mainframe: vacuum the storage index");
        /* Any other upkeep a later verb brings, named by the word it is typed as. */
        private static final TextKey OTHER_SUMMARY = TextKey.of("jsc.cli.software.maint.summary",
                "mainframe: %s the storage index");

        Maint(final String verb, final IqlVerb action) {
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

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return switch (this.action) {
                case ANALYZE -> ANALYZE_SUMMARY.text();
                case REINDEX -> REINDEX_SUMMARY.text();
                case VACUUM -> VACUUM_SUMMARY.text();
                default -> OTHER_SUMMARY.with(this.verb);
            };
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
    @TextHolder
    static final class MirrorCommand implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.software.mirror.summary",
                "install or check the Mirror package service on the Mainframe");

        @Override public CommandScope scope() {
            return MAINFRAME_SERVICE;
        }

        @Override public String name() { return "mirror"; }

        @Override public CommandGroup group() { return CommandGroup.SOFTWARE; }

        @Override public Text summary() { return SUMMARY.text(); }

        /** Nothing but the words it takes, which are typed as they are in every language. */
        @Override public Text usage() { return Text.literal("install|status"); }

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
