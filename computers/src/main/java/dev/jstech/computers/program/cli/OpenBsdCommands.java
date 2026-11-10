/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.OpenBsdServices;
import dev.jstech.computers.os.Platform;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * The tools OpenBSD keeps a machine with, secure by default: {@code rcctl} for its daemons, {@code syspatch} for the
 * release's patches, {@code pfctl} for its firewall and {@code doas} for running a command as another user
 * (computers/docs/design/operating-systems.md).
 *
 * <p>The player is root until accounts come, so doas runs what it is given as it is, and pf's rules wait for the
 * network's security; both are here as the system has them.
 */
final class OpenBsdCommands {

    private OpenBsdCommands() {
    }

    /** Every one of them, each on OpenBSD alone. */
    static List<ICliCommand> all() {
        return List.of(new Rcctl(), new Syspatch(), new Pfctl(), new Doas());
    }

    /** {@code rcctl}: which daemons start with the system, and starting and stopping them now. */
    @TextHolder
    static final class Rcctl implements ICliCommand {

        /* What rcctl ls lists, which is typed the same in every language. */
        private static final List<String> LISTS = List.of("all", "on", "off", "started", "stopped");

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.openbsd.rcctl.summary",
                "switch the system's daemons on and off, and start and stop them");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.openbsd.rcctl.about",
                "Every daemon this system runs is named here: the base system's, xenodm when a desktop is"
                        + " installed, and every service installed, which comes switched off. enable and disable say"
                        + " whether a daemon starts with the system; start and stop act on it now.");
        private static final TextKey ABOUT_BASE = TextKey.of("jsc.cli.openbsd.rcctl.about.base",
                "cron and sshd are this machine's own switches, so starting one switches it on and stopping it"
                        + " switches it off. pf, pflogd and syslogd are the base system's and always run.");
        private static final TextKey OPTION_LS = TextKey.of("jsc.cli.openbsd.rcctl.option.ls",
                "list the daemons: all, on, off, started or stopped");
        private static final TextKey OPTION_ENABLE = TextKey.of("jsc.cli.openbsd.rcctl.option.enable",
                "start the daemon with the system from now on");
        private static final TextKey OPTION_DISABLE = TextKey.of("jsc.cli.openbsd.rcctl.option.disable",
                "leave the daemon stopped when the system comes up");
        private static final TextKey OPTION_START = TextKey.of("jsc.cli.openbsd.rcctl.option.start",
                "start the daemon now; one switched off needs -f");
        private static final TextKey OPTION_STOP = TextKey.of("jsc.cli.openbsd.rcctl.option.stop",
                "stop the daemon now, until the machine starts over");
        private static final TextKey OPTION_CHECK = TextKey.of("jsc.cli.openbsd.rcctl.option.check",
                "say whether the daemon is running");
        private static final TextKey EXAMPLE_ON = TextKey.of("jsc.cli.openbsd.rcctl.example.on",
                "every daemon switched on here");
        private static final TextKey EXAMPLE_XENODM = TextKey.of("jsc.cli.openbsd.rcctl.example.xenodm",
                "come up at the desktop from the next start on");
        private static final TextKey NO_SUCH = TextKey.of("jsc.cli.openbsd.rcctl.no_such",
                "rcctl: service %s does not exist");
        private static final TextKey ALWAYS_ON = TextKey.of("jsc.cli.openbsd.rcctl.always_on",
                "rcctl: %s is part of the base system and always runs");
        private static final TextKey NEEDS_FORCE = TextKey.of("jsc.cli.openbsd.rcctl.needs_force",
                "%1$s: need -f to force start since %1$s_flags=NO");

        @Override
        public CommandScope scope() {
            return CommandScope.on(Platform.OPENBSD);
        }

        @Override
        public String name() {
            return "rcctl";
        }

        @Override
        public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override
        public Text summary() {
            return SUMMARY.text();
        }

        @Override
        public Text usage() {
            return Text.literal("ls all|on|off|started|stopped | enable|disable daemon | [-f] start|stop|check daemon");
        }

        @Override
        public List<Text> description() {
            return List.of(ABOUT.text(), ABOUT_BASE.text());
        }

        @Override
        public List<Option> options() {
            return List.of(new Option("ls", OPTION_LS), new Option("enable", OPTION_ENABLE),
                    new Option("disable", OPTION_DISABLE), new Option("start", OPTION_START),
                    new Option("stop", OPTION_STOP), new Option("check", OPTION_CHECK));
        }

        @Override
        public List<Example> examples() {
            return List.of(new Example("rcctl ls on", EXAMPLE_ON), new Example("rcctl enable xenodm", EXAMPLE_XENODM));
        }

        @Override
        public List<String> seeAlso() {
            return List.of("syspatch", "pkg_add");
        }

        @Override
        public void run(final CliContext ctx) {
            final List<String> words = new ArrayList<>(ctx.args());
            final boolean force = words.remove("-f");
            final String verb = words.isEmpty() ? "" : words.getFirst().toLowerCase(Locale.ROOT);
            final String daemon = words.size() > 1 ? words.get(1) : "";
            if (verb.equals("ls") && words.size() == 2) {
                list(ctx, daemon.toLowerCase(Locale.ROOT));
                return;
            }
            if (daemon.isEmpty() || words.size() > 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final OpenBsdServices.Daemon found = find(ctx.computer(), daemon);
            if (found == null) {
                ctx.out().error(NO_SUCH.with(daemon));
                return;
            }
            switch (verb) {
                case "enable", "disable" -> {
                    if (!ctx.computer().enableDaemon(daemon, verb.equals("enable"))) {
                        ctx.out().error(ALWAYS_ON.with(daemon));
                    }
                }
                case "start" -> {
                    if (!found.enabled() && !force) {
                        ctx.out().error(NEEDS_FORCE.with(daemon));
                    } else if (found.fixed() || ctx.computer().startDaemon(daemon, true)) {
                        said(ctx, daemon, true);
                    }
                }
                case "stop" -> {
                    if (found.fixed()) {
                        ctx.out().error(ALWAYS_ON.with(daemon));
                    } else if (ctx.computer().startDaemon(daemon, false)) {
                        said(ctx, daemon, true);
                    }
                }
                case "check" -> said(ctx, daemon, found.started());
                default -> ctx.out().error(CliTexts.USAGE.with(name(), usage()));
            }
        }

        /* The daemons the word asks for, a name to a line, as rcctl ls prints them. */
        private void list(final CliContext ctx, final String which) {
            if (!LISTS.contains(which)) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            for (final OpenBsdServices.Daemon daemon : ctx.computer().daemons()) {
                final boolean listed = switch (which) {
                    case "on" -> daemon.enabled();
                    case "off" -> !daemon.enabled();
                    case "started" -> daemon.started();
                    case "stopped" -> !daemon.started();
                    default -> true;
                };
                if (listed) {
                    ctx.out().line(daemon.name());
                }
            }
        }

        @Nullable
        private static OpenBsdServices.Daemon find(final ICliComputer computer, final String name) {
            for (final OpenBsdServices.Daemon daemon : computer.daemons()) {
                if (daemon.name().equals(name)) {
                    return daemon;
                }
            }
            return null;
        }

        /* What rc says of a daemon it acted on or was asked about: its name, and (ok) or (failed). */
        private static void said(final CliContext ctx, final String daemon, final boolean ok) {
            ctx.out().line(CliLine.build().plain(daemon)
                    .add(ok ? "(ok)" : "(failed)", ok ? CliStyle.OK : CliStyle.ERROR).done());
        }
    }

    /** {@code syspatch}: the patches published for this release, listed, installed and taken back off. */
    @TextHolder
    static final class Syspatch implements ICliCommand {

        /* The release's patches in the order they were published, with how many kilobytes each one's file is. */
        private static final List<String> PATCHES = List.of("001_ksh", "002_sshd");
        private static final List<Integer> SIZES_KB = List.of(612, 1204);
        /* How many kilobytes the Mirror sends in a second. */
        private static final int KB_PER_SECOND = 1000;

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.openbsd.syspatch.summary",
                "install the release's patches from the network mirror");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.openbsd.syspatch.about",
                "Installs every patch published for this release of the system that is not installed yet, fetched"
                        + " from the network's Mirror, and links the kernel anew after them.");
        private static final TextKey OPTION_CHECK = TextKey.of("jsc.cli.openbsd.syspatch.option.check",
                "list the patches not installed yet");
        private static final TextKey OPTION_LIST = TextKey.of("jsc.cli.openbsd.syspatch.option.list",
                "list the patches installed");
        private static final TextKey OPTION_REVERT = TextKey.of("jsc.cli.openbsd.syspatch.option.revert",
                "take the last patch installed back off");
        private static final TextKey EXAMPLE_CHECK = TextKey.of("jsc.cli.openbsd.syspatch.example.check",
                "what there is to install");
        private static final TextKey EXAMPLE_ALL = TextKey.of("jsc.cli.openbsd.syspatch.example.all",
                "install all of it");
        private static final TextKey INSTALLING = TextKey.of("jsc.cli.openbsd.syspatch.installing",
                "Installing patch %s");
        private static final TextKey REVERTING = TextKey.of("jsc.cli.openbsd.syspatch.reverting",
                "Reverting patch %s");
        private static final TextKey RELINKING = TextKey.of("jsc.cli.openbsd.syspatch.relinking",
                "Relinking to create unique kernel... done.");
        private static final TextKey ERRATA = TextKey.of("jsc.cli.openbsd.syspatch.errata",
                "Errata can be reviewed under /var/syspatch");
        private static final TextKey NO_MIRROR = TextKey.of("jsc.cli.openbsd.syspatch.no_mirror",
                "syspatch: https://mirror.mainframe/pub/OpenBSD/syspatch/7.5/: no route to the Mirror");

        @Override
        public CommandScope scope() {
            return CommandScope.on(Platform.OPENBSD);
        }

        @Override
        public String name() {
            return "syspatch";
        }

        @Override
        public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override
        public Text summary() {
            return SUMMARY.text();
        }

        @Override
        public Text usage() {
            return Text.literal("[-c | -l | -r]");
        }

        @Override
        public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override
        public List<Option> options() {
            return List.of(new Option("-c", OPTION_CHECK), new Option("-l", OPTION_LIST),
                    new Option("-r", OPTION_REVERT));
        }

        @Override
        public List<Example> examples() {
            return List.of(new Example("syspatch -c", EXAMPLE_CHECK), new Example("syspatch", EXAMPLE_ALL));
        }

        @Override
        public List<String> seeAlso() {
            return List.of("rcctl", "pkg_add");
        }

        @Override
        public void run(final CliContext ctx) {
            final int installed = Math.min(PATCHES.size(), ctx.computer().patchesInstalled());
            final String flag = ctx.hasArgs() ? ctx.arg(0) : "";
            switch (flag) {
                case "-l" -> PATCHES.subList(0, installed).forEach(ctx.out()::line);
                case "-c" -> {
                    if (reachesMirror(ctx)) {
                        PATCHES.subList(installed, PATCHES.size()).forEach(ctx.out()::line);
                    }
                }
                case "-r" -> {
                    if (installed > 0) {
                        ctx.out().line(REVERTING.with(PATCHES.get(installed - 1)));
                        ctx.computer().setPatchesInstalled(installed - 1);
                    }
                }
                case "" -> install(ctx, installed);
                default -> ctx.out().error(CliTexts.USAGE.with(name(), usage()));
            }
        }

        /* Every patch not installed yet, fetched, verified and installed, then the kernel linked anew. */
        private void install(final CliContext ctx, final int installed) {
            if (installed >= PATCHES.size() || !reachesMirror(ctx)) {
                return;
            }
            for (int at = installed; at < PATCHES.size(); at++) {
                final int kb = SIZES_KB.get(at);
                ctx.out().line(String.format(Locale.ROOT, "%-38s %4s |%s| %5d KB    00:%02d",
                        "Get/Verify syspatch75-" + PATCHES.get(at) + ".tgz", "100%", "*".repeat(25), kb,
                        kb / KB_PER_SECOND));
                ctx.out().line(INSTALLING.with(PATCHES.get(at)));
            }
            ctx.out().line(RELINKING);
            ctx.out().line(ERRATA);
            ctx.computer().setPatchesInstalled(PATCHES.size());
        }

        private static boolean reachesMirror(final CliContext ctx) {
            if (ctx.computer().mirrorReachable()) {
                return true;
            }
            ctx.out().error(NO_MIRROR);
            return false;
        }
    }

    /** {@code pfctl}: pf's state as the firewall keeps it, and the rules it was given. */
    @TextHolder
    static final class Pfctl implements ICliCommand {

        /* How many lookups a second pf makes of its state table on a quiet machine, and how often it adds one. */
        private static final double SEARCHES_PER_SECOND = 1.2;
        private static final int SECONDS_PER_INSERT = 25;
        private static final int CURRENT_ENTRIES = 4;

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.openbsd.pfctl.summary",
                "show the state of pf, the system's firewall, and its rules");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.openbsd.pfctl.about",
                "pf is always on and keeps the rules in /etc/pf.conf. What they let through and what they stop"
                        + " comes with the network's security.");
        private static final TextKey OPTION_INFO = TextKey.of("jsc.cli.openbsd.pfctl.option.info",
                "how long pf has run and what its state table holds");
        private static final TextKey OPTION_RULES = TextKey.of("jsc.cli.openbsd.pfctl.option.rules",
                "the rules pf runs by");
        private static final TextKey EXAMPLE_INFO = TextKey.of("jsc.cli.openbsd.pfctl.example.info",
                "pf's state");
        private static final TextKey ENABLED_FOR = TextKey.of("jsc.cli.openbsd.pfctl.enabled_for",
                "Status: Enabled for %s days %s");
        private static final TextKey STATE_TABLE = TextKey.of("jsc.cli.openbsd.pfctl.state_table", "State Table");
        private static final TextKey TOTAL = TextKey.of("jsc.cli.openbsd.pfctl.total", "Total");
        private static final TextKey RATE = TextKey.of("jsc.cli.openbsd.pfctl.rate", "Rate");

        @Override
        public CommandScope scope() {
            return CommandScope.on(Platform.OPENBSD);
        }

        @Override
        public String name() {
            return "pfctl";
        }

        @Override
        public CommandGroup group() {
            return CommandGroup.NETWORK;
        }

        @Override
        public Text summary() {
            return SUMMARY.text();
        }

        @Override
        public Text usage() {
            return Text.literal("-si | -sr");
        }

        @Override
        public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override
        public List<Option> options() {
            return List.of(new Option("-si", OPTION_INFO), new Option("-sr", OPTION_RULES));
        }

        @Override
        public List<Example> examples() {
            return List.of(new Example("pfctl -si", EXAMPLE_INFO));
        }

        @Override
        public List<String> seeAlso() {
            return List.of("rcctl");
        }

        @Override
        public void run(final CliContext ctx) {
            final String flag = ctx.hasArgs() ? ctx.arg(0) : "";
            switch (flag) {
                case "-si" -> info(ctx);
                case "-sr" -> {
                    ctx.out().line("block return all");
                    ctx.out().line("pass all flags S/SA");
                }
                default -> ctx.out().error(CliTexts.USAGE.with(name(), usage()));
            }
        }

        /* How long pf has run, which is how long the system has, and its state table over that time. */
        private static void info(final CliContext ctx) {
            final ICliComputer.SystemInfo system = ctx.computer().systemInfo();
            final long seconds = system == null ? 0L : system.uptimeTicks() / 20L;
            final String clock = String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600 % 24, seconds / 60 % 60,
                    seconds % 60);
            final long inserts = seconds / SECONDS_PER_INSERT;
            final long current = Math.min(CURRENT_ENTRIES, inserts);
            ctx.out().line(CliLine.of(CliSpan.plain(ENABLED_FOR.with(seconds / 86_400, clock)), CliSpan.pad(48),
                    CliSpan.plain("Debug: err")));
            ctx.out().blank();
            ctx.out().line(CliLine.of(CliSpan.plain(STATE_TABLE.text()), CliSpan.pad(36), CliSpan.plain(TOTAL.text()),
                    CliSpan.pad(56), CliSpan.plain(RATE.text())));
            row(ctx, "current entries", current, "");
            row(ctx, "searches", Math.round(seconds * SEARCHES_PER_SECOND),
                    String.format(Locale.ROOT, "%.1f/s", seconds == 0 ? 0.0 : SEARCHES_PER_SECOND));
            row(ctx, "inserts", inserts, String.format(Locale.ROOT, "%.1f/s", 1.0 / SECONDS_PER_INSERT));
            row(ctx, "removals", inserts - current, String.format(Locale.ROOT, "%.1f/s", 1.0 / SECONDS_PER_INSERT));
        }

        private static void row(final CliContext ctx, final String counter, final long total, final String rate) {
            ctx.out().line(String.format(Locale.ROOT, "  %-28s %12d %18s", counter, total, rate).stripTrailing());
        }
    }

    /**
     * {@code doas}: a command run as another user, root by default. The player is root until accounts come and the
     * one rule permits the wheel group, which root is in, so it runs what it is given exactly as typed alone.
     */
    @TextHolder
    static final class Doas implements ICliCommand, CliShell.IRunsTheRest {

        /* The one file doas reads its rules from, and the one user there is besides the one logged in. */
        private static final String CONFIG = "/etc/doas.conf";
        private static final List<String> USERS = List.of("root", "player");

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.openbsd.doas.summary",
                "run a command as another user");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.openbsd.doas.about",
                "Runs the command as root, or as the user -u names, when /etc/doas.conf permits it. Until there are"
                        + " accounts, you are root and every command is permitted.");
        private static final TextKey OPTION_USER = TextKey.of("jsc.cli.openbsd.doas.option.user",
                "run it as that user instead of root");
        private static final TextKey OPTION_CHECK = TextKey.of("jsc.cli.openbsd.doas.option.check",
                "say whether the rules in that file permit the command, without running it");
        private static final TextKey EXAMPLE_CHECK = TextKey.of("jsc.cli.openbsd.doas.example.check",
                "whether the rules permit installing packages");
        private static final TextKey NO_CONFIG = TextKey.of("jsc.cli.openbsd.doas.no_config",
                "doas: can't open config file %s: No such file or directory");
        private static final TextKey UNKNOWN_USER = TextKey.of("jsc.cli.openbsd.doas.unknown_user",
                "doas: unknown user");

        @Override
        public CommandScope scope() {
            return CommandScope.on(Platform.OPENBSD);
        }

        @Override
        public String name() {
            return "doas";
        }

        @Override
        public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override
        public Text summary() {
            return SUMMARY.text();
        }

        @Override
        public Text usage() {
            return Text.literal("[-n] [-C config] [-u user] command [arg ...]");
        }

        @Override
        public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override
        public List<Option> options() {
            return List.of(new Option("-u", OPTION_USER), new Option("-C", OPTION_CHECK));
        }

        @Override
        public List<Example> examples() {
            return List.of(new Example("doas -C /etc/doas.conf pkg_add", EXAMPLE_CHECK));
        }

        @Override
        public List<String> seeAlso() {
            return List.of("rcctl");
        }

        /* The command after the switches, unless the switches are doas's own work: a check, or a user nobody is. */
        @Override
        @Nullable
        public String lineToRun(final String typedAfter) {
            final List<String> words = new ArrayList<>(List.of(typedAfter.split("\\s+")));
            words.removeIf(String::isEmpty);
            while (!words.isEmpty() && words.getFirst().startsWith("-")) {
                final String flag = words.removeFirst();
                if (flag.equals("-u") && !words.isEmpty() && USERS.contains(words.getFirst())) {
                    words.removeFirst();
                } else if (!flag.equals("-n")) {
                    return null;
                }
            }
            return words.isEmpty() ? null : String.join(" ", words);
        }

        /* What is left once the line is not one to run: the check, a user there is none of, or nothing to run. */
        @Override
        public void run(final CliContext ctx) {
            final List<String> args = ctx.args();
            final int check = args.indexOf("-C");
            if (check >= 0 && check + 1 < args.size()) {
                if (!args.get(check + 1).equals(CONFIG)) {
                    ctx.out().error(NO_CONFIG.with(args.get(check + 1)));
                } else if (check + 2 < args.size()) {
                    // The one rule permits the wheel group, and root, the one user there is, is in it.
                    ctx.out().line("permit");
                }
                return;
            }
            if (args.contains("-u")) {
                ctx.out().error(UNKNOWN_USER);
                return;
            }
            ctx.out().error(CliTexts.USAGE.with(name(), usage()));
        }
    }
}
