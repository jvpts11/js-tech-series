/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.program.install.voice.PackageManagerVoices;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
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
     * What a package manager said when it did what it was asked, a line to a row: the last line is the one that
     * says it went well, so it alone is coloured so. The lines are the ones the answer was put together from, not
     * its English cut apart, so each is read in its reader's language.
     */
    static void done(final CliContext ctx, final Text said) {
        final List<Text> rows = ICliPackages.rows(said);
        for (int i = 0; i < rows.size(); i++) {
            if (i == rows.size() - 1) {
                ctx.out().ok(rows.get(i));
            } else {
                ctx.out().line(rows.get(i));
            }
        }
    }

    /**
     * A distribution's package manager ({@code apt}, {@code dnf}, {@code pacman}, {@code emerge}), speaking its
     * own flags but all resolving packages against the network's Mirror service. Only the manager the installed
     * OS ships is available, so {@code apt} does not exist on Arch and {@code pacman} does not exist on Ubuntu.
     */
    @TextHolder
    static final class PackageManagerCommand implements ICliCommand {

        private final PackageManagerKind kind;
        /* The word it is typed as: the manager's own, or one of OpenBSD's three tools, which share the one manager. */
        private final String name;

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.package.summary",
                "install and remove packages from the network mirror");
        /* Each manager's own flags; the words in brackets are the reader's. */
        private static final TextKey USAGE_PACMAN = TextKey.of("jsc.cli.package.pacman.usage",
                "-S <package> | -R <package> | -Ss [term] | -Q | -Qu | -Syu");
        private static final TextKey USAGE_EMERGE = TextKey.of("jsc.cli.package.emerge.usage",
                "[--ask] <package> | --unmerge <package> | --search [term] | --sync | -pvuD @world | --update @world");
        private static final TextKey USAGE_PKG = TextKey.of("jsc.cli.package.pkg.usage",
                "install <package> | delete <package> | search [term] | info | version -vRL= | update | upgrade");
        private static final TextKey USAGE_KEG = TextKey.of("jsc.cli.package.keg.usage",
                "install <formula> | uninstall <formula> | search [term] | list | outdated | update | upgrade");
        private static final TextKey USAGE = TextKey.of("jsc.cli.package.usage",
                "install <package> | remove <package> | search [term] | list [--upgradable] | check-update | update"
                        + " | upgrade");
        private static final TextKey USAGE_PKG_ADD = TextKey.of("jsc.cli.package.pkg_add.usage",
                "<package> | -u | -un");
        private static final TextKey USAGE_PKG_DELETE = TextKey.of("jsc.cli.package.pkg_delete.usage",
                "<package>");
        private static final TextKey USAGE_PKG_INFO = TextKey.of("jsc.cli.package.pkg_info.usage",
                "[-Q term]");
        /* What OpenBSD's tools say of a repository they could not read. */
        private static final TextKey PKG_ADD_NO_MIRROR = TextKey.of("jsc.cli.package.pkg_add.no_mirror",
                "https://mirror.mainframe/pub/OpenBSD/7.5/packages/: no route to the Mirror");
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
            this(kind, kind.command());
        }

        /** One of the tools a manager is typed as, which OpenBSD's is: pkg_add, pkg_delete and pkg_info. */
        PackageManagerCommand(final PackageManagerKind kind, final String name) {
            this.kind = kind;
            this.name = name;
        }

        /*
         * Each manager is its own system's, and reaches the Mirror over the network; off one it is still there and
         * says it cannot reach the Mirror, rather than being a word the prompt does not know.
         */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).forManager(this.kind);
        }

        @Override public String name() { return name; }

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
                case KEG -> USAGE_KEG.text();
                case PKG_ADD -> switch (name) {
                    case "pkg_delete" -> USAGE_PKG_DELETE.text();
                    case "pkg_info" -> USAGE_PKG_INFO.text();
                    default -> USAGE_PKG_ADD.text();
                };
                default -> USAGE.text();
            };
        }

        /*
         * keg came to Quince OS X with the Standard: on a machine of the ages before it, Quince OS X installs from its
         * packages and its discs alone.
         */
        @Override public boolean available(final ICliComputer computer) {
            return computer.packageManager() == kind
                    && (kind != PackageManagerKind.KEG || !computer.era().isAtMost(HardwareEra.TRANSITION));
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
                        case "-Qu" -> upgradable(ctx);
                        case "-Sy" -> sync(ctx);
                        case "-Syu", "-Su" -> upgrade(ctx);
                        case "-R", "-Rs", "-Rns" -> remove(ctx, arg);
                        default -> ctx.out().error(usageLine());
                    }
                }
                case EMERGE -> {
                    switch (verb) {
                        case "--search", "-s" -> search(ctx, arg);
                        case "--sync" -> sync(ctx);
                        // @world after it is every package, which is all an upgrade here ever brings up.
                        case "--update", "-u", "-uDN", "-avuDN" -> upgrade(ctx);
                        // The same with --pretend in front: what it would merge, and nothing merged.
                        case "-pvuD", "-puD", "-pvuDN", "-puDN" -> upgradable(ctx);
                        case "" -> ctx.out().error(usageLine());
                        case "--ask", "-a", "-av" -> install(ctx, arg, true);
                        case "--unmerge", "-C", "--depclean", "-c" -> remove(ctx, arg);
                        default -> install(ctx, ctx.rest(0), false);
                    }
                }
                case KEG -> {
                    // keg's words, after Homebrew's: a formula is uninstalled, and what is installed is listed.
                    switch (verb) {
                        case "install" -> install(ctx, arg, false);
                        case "search" -> search(ctx, arg);
                        case "list" -> installed(ctx);
                        case "update" -> sync(ctx);
                        case "upgrade" -> upgrade(ctx);
                        case "outdated" -> upgradable(ctx);
                        case "uninstall", "remove", "rm" -> remove(ctx, arg);
                        default -> ctx.out().error(usageLine());
                    }
                }
                case PKG_ADD -> {
                    // OpenBSD's three tools: pkg_add brings and upgrades, pkg_delete takes off, pkg_info lists.
                    switch (name) {
                        case "pkg_delete" -> remove(ctx, ctx.hasArgs() ? ctx.rest(0) : "");
                        case "pkg_info" -> {
                            if (verb.equals("-Q")) {
                                search(ctx, arg);
                            } else {
                                installed(ctx);
                            }
                        }
                        default -> {
                            if (verb.equals("-u")) {
                                upgrade(ctx);
                            } else if (verb.equals("-un") || verb.equals("-nu")) {
                                // -n is pkg_add's dry run: what -u would bring up, and nothing brought.
                                upgradable(ctx);
                            } else {
                                install(ctx, ctx.hasArgs() ? ctx.rest(0) : "", false);
                            }
                        }
                    }
                }
                case PKG -> {
                    // Its own words for the same things: a package is deleted, and what is installed is info.
                    switch (verb) {
                        case "install", "add" -> install(ctx, arg, false);
                        case "search" -> search(ctx, arg);
                        case "info", "query" -> installed(ctx);
                        case "update" -> sync(ctx);
                        case "upgrade" -> upgrade(ctx);
                        // Compared against the remote repository and only what is behind it, as -vRL= asks.
                        case "version" -> upgradable(ctx);
                        case "delete", "remove" -> remove(ctx, arg);
                        default -> ctx.out().error(usageLine());
                    }
                }
                default -> {
                    switch (verb) {
                        case "install" -> install(ctx, arg, false);
                        case "search" -> search(ctx, arg);
                        case "list" -> {
                            if (arg.trim().equals("--upgradable")) {
                                upgradable(ctx);
                            } else {
                                installed(ctx);
                            }
                        }
                        // dnf's own word for the list of what the repository has newer.
                        case "check-update" -> upgradable(ctx);
                        // apt's update reads the lists and its upgrade installs; dnf's two words both install.
                        case "update" -> {
                            if (kind == PackageManagerKind.DNF) {
                                upgrade(ctx);
                            } else {
                                sync(ctx);
                            }
                        }
                        case "upgrade", "full-upgrade", "dist-upgrade" -> upgrade(ctx);
                        case "remove", "purge", "erase" -> remove(ctx, arg);
                        default -> ctx.out().error(usageLine());
                    }
                }
            }
        }

        /** How this manager is typed, said when it was typed some other way. */
        private Text usageLine() {
            return CliTexts.USAGE.with(name, usage());
        }

        private void sync(final CliContext ctx) {
            if (!ctx.computer().mirrorReachable()) {
                ctx.out().error(noMirror());
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

        /**
         * Reads the repository and brings every installed package up to the version it serves, the one verb of each
         * manager that installs what moved on: a compiler updated this way knows the language's newer version.
         */
        private void upgrade(final CliContext ctx) {
            if (!ctx.computer().mirrorReachable()) {
                ctx.out().error(noMirror());
                ctx.out().dim(NOT_ON_A_MIRROR);
                return;
            }
            if (kind == PackageManagerKind.PKG) {
                catalogue(ctx);
            } else if (kind != PackageManagerKind.PKG_ADD) {
                ctx.out().dim(kind == PackageManagerKind.PACMAN ? SYNCHRONIZING : READING_LISTS);
            }
            final ICliComputer.OpResult updated = ctx.computer().packageUpdate();
            if (!updated.ok()) {
                ctx.out().error(updated.message());
                return;
            }
            done(ctx, updated.message());
        }

        /**
         * Lists what the Mirror has newer than what is installed, the way this manager's own command for it does, and
         * installs nothing: each manager's mark for an update, as Settings has its own
         * (computers/docs/design/programs.md).
         */
        private void upgradable(final CliContext ctx) {
            final ICliComputer computer = ctx.computer();
            if (!computer.mirrorReachable()) {
                ctx.out().error(noMirror());
                ctx.out().dim(NOT_ON_A_MIRROR);
                return;
            }
            final String arch = KernelNames.architecture(computer.platform(), computer.processorBits(),
                    computer.processorIsa());
            for (final Text line : PackageManagerVoices.upgradable(kind, computer.packageUpdates(), arch)) {
                ctx.out().line(line);
            }
        }

        /** What pkg says before anything that reads the repository, which is that it looked at it first. */
        private static void catalogue(final CliContext ctx) {
            ctx.out().line(UPDATING_CATALOGUE);
            ctx.out().line(CATALOGUE_UP_TO_DATE);
        }

        /**
         * The Mirror that could not be read, the way this manager opens such a line: pkg names itself, and OpenBSD's
         * tools name the tool and the address they asked.
         */
        private Text noMirror() {
            return switch (kind) {
                case PKG -> CliTexts.SAID_BY.with(name, NO_MIRROR);
                case PKG_ADD -> CliTexts.SAID_BY.with(name, PKG_ADD_NO_MIRROR);
                default -> ERR.with(NO_MIRROR);
            };
        }

        /** A package the manager would not install or remove, opened in its own way too. */
        private Text refusal(final Text why) {
            return kind == PackageManagerKind.PKG || kind == PackageManagerKind.PKG_ADD
                    ? CliTexts.SAID_BY.with(name, why) : REFUSED.with(why);
        }

        /**
         * Installs the first package named, passing over whatever options were typed round it.
         *
         * @param ask whether the manager was told to list what it would do and ask first
         */
        private void install(final CliContext ctx, final String typed, final boolean ask) {
            String pkg = "";
            for (final String word : typed.trim().split("\\s+")) {
                if (!word.isEmpty() && !word.startsWith("-")) {
                    pkg = word;
                    break;
                }
            }
            if (pkg.isEmpty()) {
                ctx.out().error(usageLine());
                return;
            }
            final ICliPackages.Installing result = ctx.computer().packageInstall(pkg, ask);
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
            } else if (kind != PackageManagerKind.PKG_ADD) {
                // pkg_add opens with the line its own voice gives, which says what it read first.
                ctx.out().dim(RESOLVING);
            }
            if (result.ok()) {
                done(ctx, result.message());
            } else {
                ctx.out().error(refusal(result.message()));
            }
        }

        private void remove(final CliContext ctx, final String pkg) {
            if (pkg.isBlank()) {
                ctx.out().error(usageLine());
                return;
            }
            final String first = pkg.trim().split("\\s+")[0];
            final ICliComputer.OpResult result = ctx.computer().packageRemove(first);
            if (result.ok()) {
                done(ctx, result.message());
            } else {
                ctx.out().error(refusal(result.message()));
            }
        }

        private void search(final CliContext ctx, final String term) {
            if (!ctx.computer().mirrorReachable()) {
                ctx.out().error(noMirror());
                return;
            }
            final String needle = term.trim().toLowerCase(Locale.ROOT);
            boolean any = false;
            for (final ICliComputer.PackageInfo p : ctx.computer().packagesAvailable()) {
                if (!p.matches(needle)) {
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
