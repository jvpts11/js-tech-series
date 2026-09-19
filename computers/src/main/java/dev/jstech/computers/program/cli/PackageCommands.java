/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import dev.jstech.computers.os.PackageManagerKind;
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
    static final class PackageManagerCommand implements ICliCommand {
        private final PackageManagerKind kind;

        PackageManagerCommand(final PackageManagerKind kind) {
            this.kind = kind;
        }

        @Override public String name() { return kind.command(); }

        @Override public String summary() { return "install and remove packages from the network mirror"; }

        /*
         * Removal is listed here on purpose: a verb the shell accepts but never advertises may as well
         * not exist, since the only way to find it is to already know it.
         */
        @Override public String usage() {
            return switch (kind) {
                case PACMAN -> "-S <package> | -R <package> | -Ss [term] | -Q | -Syu";
                case EMERGE -> "[--ask] <package> | --unmerge <package> | --search [term] | --sync";
                case PKG -> "install <package> | delete <package> | search [term] | info | update";
                default -> "install <package> | remove <package> | search [term] | list | update";
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
                        default -> ctx.out().error("usage: pacman " + usage());
                    }
                }
                case EMERGE -> {
                    switch (verb) {
                        case "--search", "-s" -> search(ctx, arg);
                        case "--sync" -> sync(ctx);
                        case "" -> ctx.out().error("usage: emerge " + usage());
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
                        default -> ctx.out().error("usage: pkg " + usage());
                    }
                }
                default -> {
                    switch (verb) {
                        case "install" -> install(ctx, arg, false);
                        case "search" -> search(ctx, arg);
                        case "list" -> installed(ctx);
                        case "update", "upgrade" -> sync(ctx);
                        case "remove", "purge", "erase" -> remove(ctx, arg);
                        default -> ctx.out().error("usage: " + kind.command() + " " + usage());
                    }
                }
            }
        }

        private void sync(final CliContext ctx) {
            if (!ctx.computer().mirrorReachable()) {
                ctx.out().error(problem("could not resolve mirror://"));
                ctx.out().dim("  This computer is not on a network whose Mainframe runs the Mirror service.");
                return;
            }
            if (kind == PackageManagerKind.PKG) {
                catalogue(ctx);
                ctx.out().line("All repositories are up to date.");
                return;
            }
            ctx.out().dim(kind == PackageManagerKind.PACMAN
                    ? ":: Synchronizing package databases (mirror://mainframe) ... done"
                    : "Reading package lists from mirror://mainframe ... Done");
        }

        /** What pkg says before anything that reads the repository, which is that it looked at it first. */
        private static void catalogue(final CliContext ctx) {
            ctx.out().line("Updating Mirror repository catalogue...");
            ctx.out().line("Mirror repository is up to date.");
        }

        /** A repository that could not be read, the way this manager opens such a line: pkg names itself. */
        private String problem(final String what) {
            return (kind == PackageManagerKind.PKG ? "pkg: " : "Err: ") + what;
        }

        /** A package the manager would not install or remove, opened in its own way too. */
        private String refusal(final String why) {
            return (kind == PackageManagerKind.PKG ? "pkg: " : "E: ") + why;
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
                ctx.out().error("usage: " + kind.command() + " " + usage());
                return;
            }
            final ICliPackages.Installing result = ctx.computer().packageInstall(name, ask);
            if (result.tool() != null) {
                // A manager that builds what it installs holds the terminal from here until the build is over.
                ctx.out().start(result.tool());
                return;
            }
            if (kind == PackageManagerKind.PKG) {
                ctx.out().line("Updating Mirror repository catalogue...");
                if (result.ok()) {
                    ctx.out().line("Mirror repository is up to date.");
                }
            } else {
                ctx.out().dim("Resolving mirror://mainframe ...");
            }
            if (result.ok()) {
                lines(ctx, result.message());
            } else {
                ctx.out().error(refusal(result.message()));
            }
        }

        private void remove(final CliContext ctx, final String pkg) {
            if (pkg.isBlank()) {
                ctx.out().error("usage: " + kind.command() + " " + usage());
                return;
            }
            final String name = pkg.trim().split("\\s+")[0];
            final ICliComputer.OpResult result = ctx.computer().packageRemove(name);
            if (result.ok()) {
                lines(ctx, result.message());
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
                ctx.out().error(problem("could not resolve mirror://"));
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
                ctx.out().row(p.name() + (p.installed() ? "  [installed]" : p.community() ? "  [community]" : ""),
                        p.description());
            }
            if (!any) {
                ctx.out().dim("no packages match '" + needle + "'");
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
                ctx.out().dim(ctx.computer().mirrorReachable() ? "no packages installed" : "could not resolve mirror://");
            }
        }
    }
}
