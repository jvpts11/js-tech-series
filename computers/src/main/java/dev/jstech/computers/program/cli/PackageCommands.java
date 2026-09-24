/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;

/**
 * The package managers of each family, one command per manager, over the network Mirror.
 *
 * <p>Moved here from PosixSystemCommands, which keeps what a system says about itself.
 */
final class PackageCommands {

    private PackageCommands() {
    }

    /**
     * A distribution's package manager ({@code apt}, {@code dnf}, {@code pacman}, {@code emerge}), speaking its
     * own flags but all resolving packages against the network's Mirror service. Only the manager the installed
     * OS ships is available, so {@code apt} does not exist on Arch and {@code pacman} does not exist on Ubuntu.
     */
    @TextHolder
    static final class PackageManagerCommand implements ICliCommand {

        private final PackageManagerKind kind;

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.package.summary",
                "install and remove packages from the network mirror");
        /* Each manager's own flags; the words in brackets are the reader's. */
        private static final TextKey USAGE_PACMAN = TextKey.of("jsc.cli.package.pacman.usage",
                "-S <package> | -R <package> | -Ss [term] | -Q | -Syu");
        private static final TextKey USAGE_EMERGE = TextKey.of("jsc.cli.package.emerge.usage",
                "[--ask] <package> | --unmerge <package> | --search [term] | --sync");
        private static final TextKey USAGE_PKG = TextKey.of("jsc.cli.package.pkg.usage",
                "install <package> | delete <package> | search [term] | info | update");
        private static final TextKey USAGE = TextKey.of("jsc.cli.package.usage",
                "install <package> | remove <package> | search [term] | list | update");
        /* How the apt family opens a line about a repository it could not read, and one about a refusal. */
        private static final TextKey ERR = TextKey.of("jsc.cli.package.err", "Err: %s");
        private static final TextKey REFUSED = TextKey.of("jsc.cli.package.refused", "E: %s");
        private static final TextKey NO_MIRROR = TextKey.of("jsc.cli.package.no_mirror",
                "could not resolve mirror://");
        private static final TextKey NOT_ON_A_MIRROR = TextKey.of("jsc.cli.package.not_on_a_mirror",
                "  This computer is not on a network whose Mainframe runs the Mirror service.");
        private static final TextKey ALL_UP_TO_DATE = TextKey.of("jsc.cli.package.pkg.all_up_to_date",
                "All repositories are up to date.");
        private static final TextKey SYNCHRONIZING = TextKey.of("jsc.cli.package.pacman.synchronizing",
                ":: Synchronizing package databases (mirror://mainframe) ... done");
        private static final TextKey READING_LISTS = TextKey.of("jsc.cli.package.reading_lists",
                "Reading package lists from mirror://mainframe ... Done");
        private static final TextKey UPDATING_CATALOGUE = TextKey.of("jsc.cli.package.pkg.updating_catalogue",
                "Updating Mirror repository catalogue...");
        private static final TextKey CATALOGUE_UP_TO_DATE = TextKey.of("jsc.cli.package.pkg.catalogue_up_to_date",
                "Mirror repository is up to date.");
        private static final TextKey RESOLVING = TextKey.of("jsc.cli.package.resolving",
                "Resolving mirror://mainframe ...");
        /* A package's name, marked when this computer has it or another player wrote it. */
        private static final TextKey INSTALLED_TAG = TextKey.of("jsc.cli.package.installed_tag", "%s  [installed]");
        private static final TextKey COMMUNITY_TAG = TextKey.of("jsc.cli.package.community_tag", "%s  [community]");
        private static final TextKey NO_MATCH = TextKey.of("jsc.cli.package.no_match", "no packages match '%s'");
        private static final TextKey NONE_INSTALLED = TextKey.of("jsc.cli.package.none_installed",
                "no packages installed");

        PackageManagerCommand(final PackageManagerKind kind) {
            this.kind = kind;
        }

        /** Each manager is its own system's, and it reaches the Mirror over the network. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).forManager(this.kind)
                    .needing(CommandScope.Need.NETWORK);
        }

        @Override public String name() { return kind.command(); }

        @Override public CommandGroup group() { return CommandGroup.SOFTWARE; }

        @Override public Text summary() { return SUMMARY.text(); }

        /*
         * Removal is listed here on purpose: a verb the shell accepts but never advertises may as well
         * not exist, since the only way to find it is to already know it.
         */
        @Override public Text usage() {
            return switch (kind) {
                case PACMAN -> USAGE_PACMAN.text();
                case EMERGE -> USAGE_EMERGE.text();
                case PKG -> USAGE_PKG.text();
                default -> USAGE.text();
            };
        }

        @Override public boolean available(final ICliComputer computer) {
            return computer.packageManager() == kind;
        }

        @Override public void run(final CliContext ctx) {
            final String verb = ctx.hasArgs() ? ctx.arg(0) : "";
            final String arg = ctx.argCount() > 1 ? ctx.rest(1) : "";
            switch (kind) {
                case PACMAN -> {
                    switch (verb) {
                        case "-S" -> install(ctx, arg, false);
                        case "-Ss" -> search(ctx, arg);
                        case "-Q" -> installed(ctx);
                        case "-Syu", "-Sy" -> sync(ctx);
                        case "-R", "-Rs", "-Rns" -> remove(ctx, arg);
                        default -> ctx.out().error(usageLine());
                    }
                }
                case EMERGE -> {
                    switch (verb) {
                        case "--search", "-s" -> search(ctx, arg);
                        case "--sync" -> sync(ctx);
                        case "" -> ctx.out().error(usageLine());
                        case "--ask", "-a", "-av" -> install(ctx, arg, true);
                        case "--unmerge", "-C", "--depclean", "-c" -> remove(ctx, arg);
                        default -> install(ctx, ctx.rest(0), false);
                    }
                }
                case PKG -> {
                    // Its own words for the same things: a package is deleted, and what is installed is info.
                    switch (verb) {
                        case "install", "add" -> install(ctx, arg, false);
                        case "search" -> search(ctx, arg);
                        case "info", "query" -> installed(ctx);
                        case "update", "upgrade" -> sync(ctx);
                        case "delete", "remove" -> remove(ctx, arg);
                        default -> ctx.out().error(usageLine());
                    }
                }
                default -> {
                    switch (verb) {
                        case "install" -> install(ctx, arg, false);
                        case "search" -> search(ctx, arg);
                        case "list" -> installed(ctx);
                        case "update", "upgrade" -> sync(ctx);
                        case "remove", "purge", "erase" -> remove(ctx, arg);
                        default -> ctx.out().error(usageLine());
                    }
                }
            }
        }

        /** How this manager is typed, said when it was typed some other way. */
        private Text usageLine() {
            return CliTexts.USAGE.with(kind.command(), usage());
        }

        private void sync(final CliContext ctx) {
            if (!ctx.computer().mirrorReachable()) {
                ctx.out().error(problem(NO_MIRROR));
                ctx.out().dim(NOT_ON_A_MIRROR);
                return;
            }
            if (kind == PackageManagerKind.PKG) {
                catalogue(ctx);
                ctx.out().line(ALL_UP_TO_DATE);
                return;
            }
            ctx.out().dim(kind == PackageManagerKind.PACMAN ? SYNCHRONIZING : READING_LISTS);
        }

        /** What pkg says before anything that reads the repository, which is that it looked at it first. */
        private static void catalogue(final CliContext ctx) {
            ctx.out().line(UPDATING_CATALOGUE);
            ctx.out().line(CATALOGUE_UP_TO_DATE);
        }

        /** A repository that could not be read, the way this manager opens such a line: pkg names itself. */
        private Text problem(final TextKey what) {
            return kind == PackageManagerKind.PKG ? CliTexts.SAID_BY.with(kind.command(), what) : ERR.with(what);
        }

        /** A package the manager would not install or remove, opened in its own way too. */
        private Text refusal(final Text why) {
            return kind == PackageManagerKind.PKG ? CliTexts.SAID_BY.with(kind.command(), why) : REFUSED.with(why);
        }

        /**
         * Installs the first package named, passing over whatever options were typed round it.
         *
         * @param ask whether the manager was told to list what it would do and ask first
         */
        private void install(final CliContext ctx, final String typed, final boolean ask) {
            String name = "";
            for (final String word : typed.trim().split("\\s+")) {
                if (!word.isEmpty() && !word.startsWith("-")) {
                    name = word;
                    break;
                }
            }
            if (name.isEmpty()) {
                ctx.out().error(usageLine());
                return;
            }
            final ICliPackages.Installing result = ctx.computer().packageInstall(name, ask);
            if (result.tool() != null) {
                // A manager that builds what it installs holds the terminal from here until the build is over.
                ctx.out().start(result.tool());
                return;
            }
            if (kind == PackageManagerKind.PKG) {
                ctx.out().line(UPDATING_CATALOGUE);
                if (result.ok()) {
                    ctx.out().line(CATALOGUE_UP_TO_DATE);
                }
            } else {
                ctx.out().dim(RESOLVING);
            }
            if (result.ok()) {
                lines(ctx, result.message().english());
            } else {
                ctx.out().error(refusal(result.message()));
            }
        }

        private void remove(final CliContext ctx, final String pkg) {
            if (pkg.isBlank()) {
                ctx.out().error(usageLine());
                return;
            }
            final String name = pkg.trim().split("\\s+")[0];
            final ICliComputer.OpResult result = ctx.computer().packageRemove(name);
            if (result.ok()) {
                lines(ctx, result.message().english());
            } else {
                ctx.out().error(refusal(result.message()));
            }
        }

        /** A manager speaks in several lines; the last of them is the one that says it went well. */
        private static void lines(final CliContext ctx, final String message) {
            final String[] parts = message.split("\n");
            for (int i = 0; i < parts.length; i++) {
                if (i == parts.length - 1) {
                    ctx.out().ok(parts[i]);
                } else {
                    ctx.out().line(parts[i]);
                }
            }
        }

        private void search(final CliContext ctx, final String term) {
            if (!ctx.computer().mirrorReachable()) {
                ctx.out().error(problem(NO_MIRROR));
                return;
            }
            final String needle = term.trim().toLowerCase(Locale.ROOT);
            boolean any = false;
            for (final ICliComputer.PackageInfo p : ctx.computer().packagesAvailable()) {
                if (!needle.isEmpty() && !p.name().contains(needle)
                        && !p.description().toLowerCase(Locale.ROOT).contains(needle)) {
                    continue;
                }
                any = true;
                final Text named = p.installed() ? INSTALLED_TAG.with(p.name())
                        : p.community() ? COMMUNITY_TAG.with(p.name()) : Text.literal(p.name());
                ctx.out().row(named, Text.literal(p.description()));
            }
            if (!any) {
                ctx.out().dim(NO_MATCH.with(needle));
            }
        }

        private void installed(final CliContext ctx) {
            boolean any = false;
            for (final ICliComputer.PackageInfo p : ctx.computer().packagesAvailable()) {
                if (p.installed()) {
                    any = true;
                    ctx.out().row(p.name(), p.description());
                }
            }
            if (!any) {
                ctx.out().dim(ctx.computer().mirrorReachable() ? NONE_INSTALLED : NO_MIRROR);
            }
        }
    }
}
