/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.boot.DiskCheck;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OpenBSD's installer at the terminal (computers/docs/design/operating-systems.md): the questions in the real one's
 * order, each answered by what is typed after it or by its default in brackets with Enter, and what the terminal has
 * printed by the page the installer is on.
 *
 * <p>Only the questions whose answer changes something the game has wait for one: the machine's name, sshd, X and
 * xenodm, the root disk and whether to take it whole, where the sets come from and which of them go, and the way out.
 * The rest (the network, root's password, a user, the time zone, the layout) are printed with their default taken,
 * as an answer file would give it.
 *
 * <p>Pure: the screen keeps the answers given on the page it is on and asks this what Enter means and what to show.
 */
@TextHolder
public final class OpenBsdInstall {

    /* The sets' file names, which are data in every language, and how big each is in kilobytes, as the real ones. */
    private static final String[] SET_NAMES = {"bsd", "bsd.rd", "base75.tgz", "comp75.tgz", "man75.tgz"};
    private static final int[] SET_KB = {18_117, 4_552, 346_112, 75_824, 8_019};
    /* The three sets a player may leave out, by their place among the five, and the bit that leaves each out. */
    private static final int[][] OPTIONAL = {{1, InstallerFlow.NO_BSD_RD}, {3, InstallerFlow.NO_COMP},
            {4, InstallerFlow.NO_MAN}};
    public static final int SETS_LEFT_OUT = InstallerFlow.NO_BSD_RD | InstallerFlow.NO_COMP | InstallerFlow.NO_MAN;
    /* What the medium is called, and the word for fetching the sets from the Mirror over the network. */
    private static final String MEDIUM = "cd0";
    private static final String HTTP = "http";
    private static final int BAR_WIDTH = 26;
    private static final int TICKS_A_SECOND = 20;

    private static final TextKey CONTROL_KEYS = TextKey.of("jsc.install.openbsd.control_keys",
            "erase ^?, werase ^W, kill ^U, intr ^C, status ^T");
    private static final TextKey WELCOME = TextKey.of("jsc.install.openbsd.welcome",
            "Welcome to the %s installation program.");
    private static final TextKey CHOOSE = TextKey.of("jsc.install.openbsd.choose",
            "(I)nstall, (U)pgrade, (A)utoinstall or (S)hell?");
    private static final TextKey ESCAPE_1 = TextKey.of("jsc.install.openbsd.escape_1",
            "At any prompt except password prompts you can escape to a shell by");
    private static final TextKey ESCAPE_2 = TextKey.of("jsc.install.openbsd.escape_2",
            "typing '!'. Default answers are shown in []'s and are selected by");
    private static final TextKey ESCAPE_3 = TextKey.of("jsc.install.openbsd.escape_3",
            "pressing RETURN.  You can exit this program at any time by pressing");
    private static final TextKey ESCAPE_4 = TextKey.of("jsc.install.openbsd.escape_4",
            "Control-C, but this can leave your system in an inconsistent state.");
    private static final TextKey HOSTNAME = TextKey.of("jsc.install.openbsd.hostname",
            "System hostname? (short form, e.g. 'foo')");
    private static final TextKey INTERFACES = TextKey.of("jsc.install.openbsd.interfaces",
            "Available network interfaces are: %s.");
    private static final TextKey INTERFACE = TextKey.of("jsc.install.openbsd.interface",
            "Network interface to configure? (name, lladdr, '?', or 'done') [%s]");
    private static final TextKey IPV4 = TextKey.of("jsc.install.openbsd.ipv4",
            "IPv4 address for %s? (or 'autoconf' or 'none') [autoconf]");
    private static final TextKey DOMAIN = TextKey.of("jsc.install.openbsd.domain", "Using DNS domainname %s");
    private static final TextKey PASSWORD = TextKey.of("jsc.install.openbsd.password",
            "Password for root account? (will not echo)");
    private static final TextKey PASSWORD_AGAIN = TextKey.of("jsc.install.openbsd.password_again",
            "Password for root account? (again)");
    private static final TextKey SSHD = TextKey.of("jsc.install.openbsd.sshd", "Start sshd(8) by default? [yes]");
    private static final TextKey X = TextKey.of("jsc.install.openbsd.x",
            "Do you expect to run the X Window System? [yes]");
    private static final TextKey XENODM = TextKey.of("jsc.install.openbsd.xenodm",
            "Do you want the X Window System to be started by xenodm(1)? [no]");
    private static final TextKey USER = TextKey.of("jsc.install.openbsd.user",
            "Setup a user? (enter a lower-case loginname, or 'no') [no]");
    private static final TextKey TIMEZONE = TextKey.of("jsc.install.openbsd.timezone",
            "What timezone are you in? ('?' for list) [UTC]");
    private static final TextKey DISKS = TextKey.of("jsc.install.openbsd.disks", "Available disks are: %s.");
    private static final TextKey ROOT_DISK = TextKey.of("jsc.install.openbsd.root_disk",
            "Which disk is the root disk? ('?' for details) [%s]");
    private static final TextKey WHOLE = TextKey.of("jsc.install.openbsd.whole",
            "Use (W)hole disk or (E)dit the MBR? [whole]");
    private static final TextKey NO_SUCH_DISK = TextKey.of("jsc.install.openbsd.no_such_disk",
            "'%s' is not a valid choice.");
    private static final TextKey TOO_SMALL = TextKey.of("jsc.install.openbsd.too_small",
            "%s is too small: the system needs %s MB.");
    private static final TextKey NO_ROOM_BESIDE = TextKey.of("jsc.install.openbsd.no_room_beside",
            "There is not enough free space on %s beside what it holds.");
    private static final TextKey SETTING_MBR = TextKey.of("jsc.install.openbsd.setting_mbr",
            "Setting OpenBSD MBR partition to whole %s...done.");
    private static final TextKey KEEPING_MBR = TextKey.of("jsc.install.openbsd.keeping_mbr",
            "Using the free space on %s beside what it holds.");
    private static final TextKey LAYOUT = TextKey.of("jsc.install.openbsd.layout",
            "The auto-allocated layout for %s is:");
    private static final TextKey USE_LAYOUT = TextKey.of("jsc.install.openbsd.use_layout",
            "Use (A)uto layout, (E)dit auto layout, or create (C)ustom layout? [a]");
    private static final TextKey INITIALIZE = TextKey.of("jsc.install.openbsd.initialize",
            "Which disk do you wish to initialize? (or 'done') [done]");
    private static final TextKey SETS_TITLE = TextKey.of("jsc.install.openbsd.sets_title", "Let's install the sets!");
    private static final TextKey LOCATION = TextKey.of("jsc.install.openbsd.location",
            "Location of sets? (%s or 'done') [%s]");
    private static final TextKey HTTP_SERVER = TextKey.of("jsc.install.openbsd.http_server",
            "HTTP Server? (hostname, list#, 'done' or '?') [%s]");
    private static final TextKey PATHNAME = TextKey.of("jsc.install.openbsd.pathname",
            "Pathname to the sets? (or 'done') [%s]");
    private static final TextKey SELECT_1 = TextKey.of("jsc.install.openbsd.select_1",
            "Select sets by entering a set name, a file name pattern or 'all'. De-select");
    private static final TextKey SELECT_2 = TextKey.of("jsc.install.openbsd.select_2",
            "sets by prepending a '-', e.g.: '-man*'. Selected sets are labelled '[X]'.");
    private static final TextKey SET_NAMES_ASK = TextKey.of("jsc.install.openbsd.set_names",
            "Set name(s)? (or 'abort' or 'done') [done]");
    private static final TextKey INSTALLING = TextKey.of("jsc.install.openbsd.installing", "Installing %s");
    private static final TextKey SETS_IN = TextKey.of("jsc.install.openbsd.sets_in", "... and when the sets are in:");
    private static final TextKey SAVING = TextKey.of("jsc.install.openbsd.saving",
            "Saving configuration files... done.");
    private static final TextKey DEVICE_NODES = TextKey.of("jsc.install.openbsd.device_nodes",
            "Making all device nodes... done.");
    private static final TextKey RELINKING = TextKey.of("jsc.install.openbsd.relinking",
            "Relinking to create unique kernel... done.");
    private static final TextKey CONGRATULATIONS = TextKey.of("jsc.install.openbsd.congratulations",
            "CONGRATULATIONS! Your %s install has been successfully completed!");
    private static final TextKey MAIL_1 = TextKey.of("jsc.install.openbsd.mail_1",
            "When you login to your new system the first time, please read your mail");
    private static final TextKey MAIL_2 = TextKey.of("jsc.install.openbsd.mail_2", "using the 'mail' command.");
    private static final TextKey EXIT = TextKey.of("jsc.install.openbsd.exit",
            "Exit to (S)hell, (H)alt or (R)eboot? [reboot]");

    private OpenBsdInstall() {
    }

    /**
     * What Enter does with {@code typed} on the page the installer is on, given what was answered on it so far: the
     * actions to send the machine, in order, and what the screen keeps of the page.
     */
    public static Step enter(final InstallerFlow flow, final Answers answers, final String typed) {
        final String word = typed.strip().toLowerCase(Locale.ROOT);
        return switch (flow.page()) {
            case WELCOME -> word.equals("i") || word.equals("install")
                    ? Step.of(answers, Action.of(Action.NEXT)) : Step.again(answers);
            case NAME -> typed.isBlank() ? Step.again(answers)
                    : Step.of(answers, Action.named(typed.strip()), Action.of(Action.NEXT));
            case SERVICES -> services(flow, answers, word);
            case DISK -> disk(flow, answers, word, typed);
            case MIRROR -> mirror(flow, answers, word);
            case COMPONENTS -> sets(flow, answers, word);
            case DONE -> switch (word) {
                case "", "r", "reboot" -> Step.of(answers, Action.of(Action.REBOOT));
                case "h", "halt" -> Step.of(answers, Action.of(Action.HALT));
                default -> Step.again(answers);
            };
            default -> Step.again(answers);
        };
    }

    /**
     * Everything on the terminal by now, top to bottom; the last line is the question waiting, or none while the sets
     * are copied.
     *
     * @param echoes    what was typed on this page that the installer did not take, and what it said back, which
     *                  stand before the question it asks again
     * @param ticksDone how far the copy has run, which is how far each set's bar has got
     * @param memoryMb  the machine's memory, which the layout's swap is the size of; zero when unknown
     */
    public static List<Line> lines(final InstallerFlow flow, final Answers answers, final List<Line> echoes,
                                   final int ticksDone, final int memoryMb) {
        final List<Line> out = new ArrayList<>();
        final int stage = flow.stageIndex();
        if (stage < indexOf(flow, InstallerPage.MIRROR)) {
            first(flow, answers, out);
        } else if (stage < indexOf(flow, InstallerPage.COPY)) {
            second(flow, answers, out, memoryMb);
        } else {
            third(flow, out, ticksDone);
        }
        out.addAll(echoes);
        final Text asking = asking(flow, answers);
        if (asking != null) {
            out.add(new Line(List.of(asking), "", true, false));
        }
        return out;
    }

    /** The question waiting on this page, answered with what the player typed: what is echoed when it is refused. */
    public static Line refused(final InstallerFlow flow, final Answers answers, final String typed) {
        final Text asking = asking(flow, answers);
        return new Line(List.of(asking == null ? Text.EMPTY : asking), typed, false, false);
    }

    /** A line the installer says back to an answer it refused, under the answer. */
    public static Line said(final Text said) {
        return Line.of(said);
    }

    /**
     * What one answer to "Set name(s)?" leaves of the sets: a name or a pattern selects what it matches, the same with
     * a {@code -} in front leaves it out, and {@code all} selects every one. Only the three that may go are touched:
     * the kernel and the base system always stay.
     *
     * @param leftOut the sets left out before it, as the installer's option bits
     */
    public static int applySets(final int leftOut, final String answer) {
        int out = leftOut & SETS_LEFT_OUT;
        for (final String token : answer.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            final boolean drop = token.startsWith("-");
            final String pattern = (drop || token.startsWith("+") ? token.substring(1) : token)
                    .toLowerCase(Locale.ROOT);
            for (final int[] optional : OPTIONAL) {
                if (pattern.equals("all") || matches(SET_NAMES[optional[0]], pattern)) {
                    out = drop ? out | optional[1] : out & ~optional[1];
                }
            }
        }
        return out;
    }

    /**
     * The room the sets left out keep off the disk, out of a system that takes {@code footprintMb} whole: each set's
     * share of the system, by how big the real sets are against one another.
     *
     * @param leftOut the sets left out, as the installer's option bits
     */
    public static int savedMb(final int leftOut, final int footprintMb) {
        long all = 0L;
        for (final int kb : SET_KB) {
            all += kb;
        }
        long saved = 0L;
        for (final int[] optional : OPTIONAL) {
            if ((leftOut & optional[1]) != 0) {
                saved += SET_KB[optional[0]];
            }
        }
        return (int) (footprintMb * saved / all);
    }

    /* The first screen: the welcome, the name, the services and the root disk. */
    private static void first(final InstallerFlow flow, final Answers answers, final List<Line> out) {
        final int stage = flow.stageIndex();
        out.add(Line.of(CONTROL_KEYS.text()));
        out.add(Line.of(Text.EMPTY));
        out.add(Line.of(WELCOME.with("OpenBSD/IA-32 7.5")));
        if (stage <= indexOf(flow, InstallerPage.WELCOME)) {
            return;
        }
        out.add(Line.answered(CHOOSE.text(), "i"));
        out.add(Line.of(ESCAPE_1.text()));
        out.add(Line.of(ESCAPE_2.text()));
        out.add(Line.of(ESCAPE_3.text()));
        out.add(Line.of(ESCAPE_4.text()));
        out.add(Line.of(Text.EMPTY));
        if (stage <= indexOf(flow, InstallerPage.NAME)) {
            return;
        }
        out.add(Line.answered(HOSTNAME.text(), flow.computerName()));
        out.add(Line.of(Text.EMPTY));
        if (flow.mirrorAnswers()) {
            // The network the machine is on, configured the way each answer left at its default does.
            out.add(Line.of(INTERFACES.with("em0")));
            out.add(Line.of(INTERFACE.with("em0")));
            out.add(Line.of(IPV4.with("em0")));
            out.add(Line.of(INTERFACE.with("done")));
            out.add(Line.of(DOMAIN.with("my.domain")));
            out.add(Line.of(Text.EMPTY));
        }
        out.add(Line.of(PASSWORD.text()));
        out.add(Line.of(PASSWORD_AGAIN.text()));
        final boolean onServices = flow.page() == InstallerPage.SERVICES;
        // On the services page each question answered so far stands; past it, all of them as they were given.
        final int answered = onServices ? answers.services() : 3;
        final boolean sshd = onServices ? answers.sshd() : flow.sshdEnabled();
        final boolean x = onServices ? answers.x() : !flow.has(InstallerFlow.NO_X);
        final boolean xenodm = onServices ? answers.xenodm() : flow.has(InstallerFlow.XENODM);
        if (answered > 0) {
            out.add(Line.answered(SSHD.text(), sshd ? "" : "no"));
        }
        if (answered > 1) {
            out.add(Line.answered(X.text(), x ? "" : "no"));
        }
        if (answered > 2 && x) {
            out.add(Line.answered(XENODM.text(), xenodm ? "yes" : ""));
        }
        if (onServices) {
            return;
        }
        out.add(Line.of(USER.text()));
        out.add(Line.of(TIMEZONE.text()));
        out.add(Line.of(Text.EMPTY));
        out.add(Line.of(DISKS.with(String.join(" ", labels(flow)))));
        if (answers.rootChosen() && flow.target() != null) {
            out.add(Line.answered(ROOT_DISK.with(defaultDisk(flow)), flow.target().label()));
        }
    }

    /* The second screen: the disk laid out, and where the sets come from and which go. */
    private static void second(final InstallerFlow flow, final Answers answers, final List<Line> out,
                               final int memoryMb) {
        final InstallerFlow.Disk disk = flow.target();
        final String label = disk == null ? "wd0" : disk.label();
        final boolean beside = beside(flow);
        out.add(Line.of((beside ? KEEPING_MBR : SETTING_MBR).with(label)));
        out.add(Line.of(LAYOUT.with(label)));
        final int sizeMb = disk == null ? 0 : beside ? disk.freeMb() : disk.sizeMb();
        final int swapMb = Math.max(0, Math.min(memoryMb, sizeMb / 4));
        final int rootMb = Math.max(0, sizeMb - swapMb);
        out.add(Line.cells(Table.LAYOUT, "#", "size", "offset", "fstype", ""));
        out.add(Line.cells(Table.LAYOUT, "  a:", megabytes(rootMb), "64", "4.2BSD", "# /"));
        out.add(Line.cells(Table.LAYOUT, "  b:", megabytes(swapMb), String.valueOf(64L + rootMb * 2_048L), "swap",
                ""));
        out.add(Line.cells(Table.LAYOUT, "  c:", megabytes(sizeMb), "0", "unused", ""));
        out.add(Line.of(USE_LAYOUT.text()));
        out.add(Line.of(Text.literal("/dev/r" + label + "a: " + rootMb + ".0MB in " + rootMb * 2_048L
                + " sectors of 512 bytes")));
        out.add(Line.of(Text.literal("/dev/" + label + "a (" + DiskCheck.OPENBSD_DUID
                + ".a) on /mnt type ffs (rw, local)")));
        out.add(Line.of(Text.EMPTY));
        final List<String> others = new ArrayList<>(labels(flow));
        others.remove(label);
        if (!others.isEmpty()) {
            out.add(Line.of(DISKS.with(String.join(" ", others))));
            out.add(Line.of(INITIALIZE.text()));
            out.add(Line.of(Text.EMPTY));
        }
        out.add(Line.of(SETS_TITLE.text()));
        if (flow.page() == InstallerPage.MIRROR) {
            return;
        }
        out.add(Line.answered(location(flow), flow.useMirror() ? HTTP : ""));
        if (flow.useMirror()) {
            out.add(Line.of(HTTP_SERVER.with(flow.mirrorHost())));
        }
        out.add(Line.of(PATHNAME.with("7.5/IA-32")));
        out.add(Line.of(Text.EMPTY));
        out.add(Line.of(SELECT_1.text()));
        out.add(Line.of(SELECT_2.text()));
        int leftOut = answers.setsFrom();
        setList(leftOut, out);
        for (final String typed : answers.sets()) {
            out.add(Line.answered(SET_NAMES_ASK.text(), typed));
            leftOut = applySets(leftOut, typed);
            setList(leftOut, out);
        }
    }

    /* The third screen: the sets with their bars, and what follows once they are in. */
    private static void third(final InstallerFlow flow, final List<Line> out, final int ticksDone) {
        final List<InstallerFlow.Step> steps = flow.steps();
        final boolean over = flow.page() == InstallerPage.DONE;
        final int on = flow.stepAt(ticksDone);
        for (int i = 0; i < steps.size() && (over || i <= on); i++) {
            final int set = setIndexOf(steps.get(i).label());
            if (set < 0) {
                continue;
            }
            final boolean done = over || i < on;
            final int permille = done ? 1000 : flow.stepPermille(ticksDone);
            final int seconds = steps.get(i).ticks() * permille / 1000 / TICKS_A_SECOND;
            final int stars = BAR_WIDTH * permille / 1000;
            out.add(new Line(List.of(INSTALLING.with(SET_NAMES[set]), Text.literal(permille / 10 + "%"),
                    Text.literal("|" + "*".repeat(stars) + " ".repeat(BAR_WIDTH - stars) + "|"),
                    Text.literal(size(set)), Text.literal(String.format(Locale.ROOT, "%02d:%02d%s",
                    seconds / 60, seconds % 60, done ? "" : " ETA"))), "", false, false, Table.BARS));
        }
        if (!over) {
            return;
        }
        out.add(Line.of(Text.EMPTY));
        out.add(Line.of(SETS_IN.text()));
        out.add(Line.of(Text.EMPTY));
        out.add(Line.of(LOCATION.with(locations(flow), "done")));
        out.add(Line.of(SAVING.text()));
        out.add(Line.of(DEVICE_NODES.text()));
        out.add(Line.of(RELINKING.text()));
        out.add(Line.of(Text.EMPTY));
        out.add(new Line(List.of(CONGRATULATIONS.with("OpenBSD")), "", false, true));
        out.add(Line.of(Text.EMPTY));
        out.add(Line.of(MAIL_1.text()));
        out.add(Line.of(MAIL_2.text()));
        out.add(Line.of(Text.EMPTY));
    }

    /* The question waiting on this page, or null while the sets are copied. */
    private static Text asking(final InstallerFlow flow, final Answers answers) {
        return switch (flow.page()) {
            case WELCOME -> CHOOSE.text();
            case NAME -> HOSTNAME.text();
            case SERVICES -> switch (answers.services()) {
                case 0 -> SSHD.text();
                case 1 -> X.text();
                default -> XENODM.text();
            };
            case DISK -> answers.rootChosen() ? WHOLE.text() : ROOT_DISK.with(defaultDisk(flow));
            case MIRROR -> location(flow);
            case COMPONENTS -> SET_NAMES_ASK.text();
            case DONE -> EXIT.text();
            default -> null;
        };
    }

    private static Step services(final InstallerFlow flow, final Answers answers, final String word) {
        final Boolean yes = yesNo(word, answers.services() < 2);
        if (yes == null) {
            return Step.again(answers);
        }
        final Answers next = switch (answers.services()) {
            case 0 -> answers.withServices(1, yes, answers.x(), answers.xenodm());
            case 1 -> answers.withServices(yes ? 2 : 3, answers.sshd(), yes, false);
            default -> answers.withServices(3, answers.sshd(), answers.x(), yes);
        };
        if (next.services() < 3) {
            return Step.of(next);
        }
        int options = flow.options() & ~(InstallerFlow.NO_X | InstallerFlow.XENODM);
        options |= next.x() ? 0 : InstallerFlow.NO_X;
        options |= next.x() && next.xenodm() ? InstallerFlow.XENODM : 0;
        return Step.of(next, Action.of(Action.SSHD, next.sshd() ? 1 : 0), Action.of(Action.OPTIONS, options),
                Action.of(Action.NEXT));
    }

    private static Step disk(final InstallerFlow flow, final Answers answers, final String word, final String typed) {
        if (!answers.rootChosen()) {
            final InstallerFlow.Disk chosen = diskNamed(flow, word.isEmpty() ? defaultDisk(flow) : word);
            if (chosen == null) {
                return Step.said(answers, NO_SUCH_DISK.with(typed.strip()));
            }
            if (chosen.sizeMb() < flow.footprintMb()) {
                return Step.said(answers, TOO_SMALL.with(chosen.label(), flow.footprintMb()));
            }
            final Answers next = answers.withRoot(true);
            // A disk with nothing on it is taken whole without a question; one that holds something is asked about.
            if (empty(chosen)) {
                return Step.of(next, Action.of(Action.SELECT_DISK, chosen.slot()), Action.of(Action.NEXT));
            }
            return Step.of(next, Action.of(Action.SELECT_DISK, chosen.slot()));
        }
        final InstallerFlow.Disk disk = flow.target();
        if (disk == null) {
            return Step.of(answers.withRoot(false));
        }
        return switch (word) {
            case "", "w", "whole" -> Step.of(answers, Action.of(Action.ERASE, disk.slot()), Action.of(Action.NEXT));
            case "e", "edit" -> disk.freeMb() >= flow.footprintMb()
                    ? Step.of(answers, Action.of(Action.CANCEL_ERASE), Action.of(Action.NEXT))
                    : Step.said(answers, NO_ROOM_BESIDE.with(disk.label()));
            default -> Step.again(answers);
        };
    }

    private static Step mirror(final InstallerFlow flow, final Answers answers, final String word) {
        if (word.isEmpty() || word.equals(MEDIUM)) {
            return Step.of(answers, Action.of(Action.MIRROR, 0), Action.of(Action.NEXT));
        }
        if (word.equals(HTTP) && flow.mirrorAnswers()) {
            return Step.of(answers, Action.of(Action.MIRROR, 1), Action.of(Action.NEXT));
        }
        return Step.again(answers);
    }

    private static Step sets(final InstallerFlow flow, final Answers answers, final String word) {
        if (word.isEmpty() || word.equals("done")) {
            return Step.of(answers, Action.of(Action.NEXT));
        }
        final List<String> typed = new ArrayList<>(answers.sets());
        typed.add(word);
        int leftOut = answers.setsFrom();
        for (final String one : typed) {
            leftOut = applySets(leftOut, one);
        }
        return Step.of(answers.withSets(typed), Action.of(Action.OPTIONS, flow.options() & ~SETS_LEFT_OUT | leftOut));
    }

    /* Whether a set's file name answers to that name or pattern, as the installer reads a trailing star. */
    private static boolean matches(final String name, final String pattern) {
        if (pattern.endsWith("*")) {
            return name.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return name.equals(pattern) || name.replace("75.tgz", "").equals(pattern);
    }

    /* The two rows of sets, each marked selected or not, in the real one's three columns. */
    private static void setList(final int leftOut, final List<Line> out) {
        out.add(Line.cells(Table.SETS, mark(0, leftOut) + SET_NAMES[0], mark(2, leftOut) + SET_NAMES[2],
                mark(4, leftOut) + SET_NAMES[4]));
        out.add(Line.cells(Table.SETS, mark(1, leftOut) + SET_NAMES[1], mark(3, leftOut) + SET_NAMES[3], ""));
    }

    private static String mark(final int set, final int leftOut) {
        for (final int[] optional : OPTIONAL) {
            if (optional[0] == set && (leftOut & optional[1]) != 0) {
                return "[ ] ";
            }
        }
        return "[X] ";
    }

    /* Whether the disk is taken beside what it holds rather than whole. */
    private static boolean beside(final InstallerFlow flow) {
        final InstallerFlow.Disk disk = flow.target();
        return disk != null && disk.slot() != flow.eraseSlot() && !empty(disk);
    }

    private static boolean empty(final InstallerFlow.Disk disk) {
        return !disk.hasSystem() && disk.freeMb() >= disk.sizeMb();
    }

    private static Text location(final InstallerFlow flow) {
        return LOCATION.with(locations(flow), MEDIUM);
    }

    private static String locations(final InstallerFlow flow) {
        return flow.mirrorAnswers() ? MEDIUM + " " + HTTP : MEDIUM;
    }

    private static Boolean yesNo(final String word, final boolean defaultYes) {
        return switch (word) {
            case "" -> defaultYes;
            case "y", "yes" -> Boolean.TRUE;
            case "n", "no" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static List<String> labels(final InstallerFlow flow) {
        final List<String> out = new ArrayList<>();
        for (final InstallerFlow.Disk disk : flow.disks()) {
            out.add(disk.label());
        }
        return out;
    }

    /* The disk the question offers in brackets: the one the installer had chosen, the first with room. */
    private static String defaultDisk(final InstallerFlow flow) {
        final InstallerFlow.Disk target = flow.target();
        if (target != null) {
            return target.label();
        }
        return flow.disks().isEmpty() ? "" : flow.disks().getFirst().label();
    }

    private static InstallerFlow.Disk diskNamed(final InstallerFlow flow, final String name) {
        for (final InstallerFlow.Disk disk : flow.disks()) {
            if (disk.label().equalsIgnoreCase(name)) {
                return disk;
            }
        }
        return null;
    }

    private static int indexOf(final InstallerFlow flow, final InstallerPage page) {
        final List<InstallerStyle.Stage> stages = flow.style().stages();
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).page() == page) {
                return i;
            }
        }
        return stages.size();
    }

    private static int setIndexOf(final Text label) {
        for (int i = 0; i < SET_NAMES.length; i++) {
            if (label.equals(Text.literal(SET_NAMES[i]))) {
                return i;
            }
        }
        return -1;
    }

    private static String size(final int set) {
        final int kb = SET_KB[set];
        return kb >= 100_000 ? kb / 1_024 + " MB" : kb + " KB";
    }

    private static String megabytes(final int mb) {
        return mb + ".0M";
    }

    /** Which of the installer's tables a row of columns belongs to, which is where the screen puts each column. */
    public enum Table {
        /** A line of words, no columns. */
        NONE,
        /** The disk's layout: the partition, its size, its offset, its file system and where it is mounted. */
        LAYOUT,
        /** The sets, three to a row, each marked selected or not. */
        SETS,
        /** A set being copied: its name, how far, the bar, its size and the time. */
        BARS
    }

    /**
     * One line of the terminal: what the installer printed, a part for each column when it prints a row of a table,
     * what was typed after it, whether it is the question waiting now (which the screen draws its cursor after),
     * whether it is the one line the installer prints bright, and the table it is a row of.
     */
    public record Line(List<Text> parts, String typed, boolean open, boolean loud, Table table) {

        public Line {
            parts = List.copyOf(parts);
            typed = typed == null ? "" : typed;
            table = table == null ? Table.NONE : table;
        }

        public Line(final List<Text> parts, final String typed, final boolean open, final boolean loud) {
            this(parts, typed, open, loud, Table.NONE);
        }

        static Line of(final Text text) {
            return new Line(List.of(text), "", false, false);
        }

        static Line answered(final Text question, final String answer) {
            return new Line(List.of(question), answer, false, false);
        }

        static Line cells(final Table table, final String... cells) {
            final List<Text> parts = new ArrayList<>(cells.length);
            for (final String cell : cells) {
                parts.add(Text.literal(cell));
            }
            return new Line(parts, "", false, false, table);
        }
    }

    /**
     * What the screen keeps of the page it is on: on the services page how many of its questions are answered and
     * how; on the disk page whether the root disk has been named; on the sets page what was typed at it and the
     * sets left out when it began.
     */
    public record Answers(int services, boolean sshd, boolean x, boolean xenodm, boolean rootChosen,
                          List<String> sets, int setsFrom) {

        public Answers {
            sets = List.copyOf(sets);
        }

        /** What a page starts with, its sets as the installer holds them. */
        public static Answers fresh(final InstallerFlow flow) {
            return new Answers(0, true, true, false, false, List.of(), flow.options() & SETS_LEFT_OUT);
        }

        Answers withServices(final int answered, final boolean sshdOn, final boolean xOn, final boolean xenodmOn) {
            return new Answers(answered, sshdOn, xOn, xenodmOn, this.rootChosen, this.sets, this.setsFrom);
        }

        Answers withRoot(final boolean chosen) {
            return new Answers(this.services, this.sshd, this.x, this.xenodm, chosen, this.sets, this.setsFrom);
        }

        Answers withSets(final List<String> typed) {
            return new Answers(this.services, this.sshd, this.x, this.xenodm, this.rootChosen, typed, this.setsFrom);
        }
    }

    /**
     * What Enter did: the page's answers as they now stand, whether the installer refused what was typed (and asks
     * again, the answer echoed before it), what it said back when it said something, and what to send the machine.
     */
    public record Step(Answers answers, boolean refused, Text said, List<Action> actions) {

        public Step {
            actions = List.copyOf(actions);
        }

        static Step of(final Answers answers, final Action... actions) {
            return new Step(answers, false, Text.EMPTY, List.of(actions));
        }

        static Step again(final Answers answers) {
            return new Step(answers, true, Text.EMPTY, List.of());
        }

        static Step said(final Answers answers, final Text said) {
            return new Step(answers, true, said, List.of());
        }
    }

    /**
     * One thing to tell the machine: which, about what, and the name typed where it is one.
     *
     * @param kind  one of the constants here, which the screen turns into the installer's own payload
     * @param value the disk, the choice or the option bits the action is about
     * @param text  the name typed, for {@link #NAME}
     */
    public record Action(int kind, int value, String text) {

        public static final int NEXT = 0;
        public static final int NAME = 1;
        public static final int SSHD = 2;
        public static final int OPTIONS = 3;
        public static final int SELECT_DISK = 4;
        public static final int ERASE = 5;
        public static final int CANCEL_ERASE = 6;
        public static final int MIRROR = 7;
        public static final int REBOOT = 8;
        public static final int HALT = 9;

        static Action of(final int kind) {
            return new Action(kind, 0, "");
        }

        static Action of(final int kind, final int value) {
            return new Action(kind, value, "");
        }

        static Action named(final String name) {
            return new Action(NAME, 0, name);
        }
    }
}
