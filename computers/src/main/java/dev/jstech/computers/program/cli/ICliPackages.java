/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What a command reaches of the packages a computer installs over its network's Mirror: the package manager, what the
 * Mirror offers, installing, removing, updating and publishing.
 *
 * <p>Every member answers as a computer with no Mirror in reach would.
 */
@TextHolder
public interface ICliPackages {

    /** What a package manager says when no Mirror answers, in the words a real one uses. */
    TextKey NO_MIRROR = TextKey.of("jsc.cli.packages.no_mirror", "could not resolve mirror://");
    TextKey UNABLE_TO_LOCATE = TextKey.of("jsc.cli.packages.unable_to_locate", "unable to locate package %s");
    TextKey NO_PORTS_TREE = TextKey.of("jsc.cli.packages.no_ports_tree", "this system keeps no ports tree");
    TextKey NO_TARGET = TextKey.of("jsc.cli.packages.no_target", "make: no target to make.");
    TextKey NO_MAINFRAME = TextKey.of("jsc.cli.packages.no_mainframe", "the network has no Mainframe");
    /** Two lines of a package manager's answer as one, the first over the second. */
    TextKey LINES = TextKey.of("jsc.cli.packages.lines", "%s\n%s");

    /**
     * What came of asking for a package.
     *
     * @param said what the package manager said at once, in its own words
     * @param tool what it left running in front of the terminal, for a manager whose installing takes the
     *             terminal while it happens; null when it was all said at once
     */
    record Installing(ICliComputer.OpResult said, @Nullable ITtyProcess tool) {

        /** Everything there was to say has been said. */
        public static Installing said(final ICliComputer.OpResult said) {
            return new Installing(said, null);
        }

        /** Nothing to say yet: the tool says it, from here until it ends. */
        public static Installing running(final ITtyProcess tool) {
            return new Installing(ICliComputer.OpResult.ok(""), tool);
        }

        public boolean ok() {
            return this.said.ok();
        }

        public Text message() {
            return this.said.message();
        }
    }

    /** The installed OS's package manager; {@code NONE} on media-installed platforms. */
    default PackageManagerKind packageManager() {
        return PackageManagerKind.NONE;
    }

    /** The packages the network mirror offers this computer (empty when no mirror is reachable). */
    default List<ICliComputer.PackageInfo> packagesAvailable() {
        return List.of();
    }

    /**
     * Installs the named package from the network mirror: resolves it, checks the OS/hardware gates, and
     * installs it, or, for a manager that builds from source, leaves the build running in front of the
     * terminal. The message reads like the package manager's own output; a missing mirror is the classic
     * "could not resolve" failure.
     *
     * @param ask whether the manager was told to list what it would do and ask before doing it
     */
    default Installing packageInstall(final String name, final boolean ask) {
        return Installing.said(ICliComputer.OpResult.fail(NO_MIRROR));
    }

    /**
     * Removes the named installed program (a package manager's remove verb, or the DOS-family
     * {@code uninstall} command). Mainframe services turn their agent off; a removed desktop
     * environment drops the computer back to the TTY on its next boot.
     */
    default ICliComputer.OpResult packageRemove(final String name) {
        return ICliComputer.OpResult.fail(UNABLE_TO_LOCATE.with(name));
    }

    /**
     * Brings every installed package up to the current build. Packages installed before a mod update
     * carry the version they were installed at, so this is what reconciles a repository that moved on
     * without the machine; it never installs anything new.
     */
    default ICliComputer.OpResult packageUpdate() {
        return ICliComputer.OpResult.fail(NO_MIRROR);
    }

    /**
     * Puts a built package on the network's Mirror, for anyone on the network to install.
     *
     * @param path the package file on this computer's disk
     */
    default ICliComputer.OpResult publishPackage(final String path) {
        return ICliComputer.OpResult.fail(NO_MIRROR);
    }

    /** Takes one back off the Mirror. */
    default ICliComputer.OpResult unpublishPackage(final String name) {
        return ICliComputer.OpResult.fail(NO_MIRROR);
    }

    /**
     * FreeBSD's portsnap: fetches the ports tree from the network's Mirror as one snapshot, lays it out under
     * {@code /usr/ports}, or brings a tree already there up to date, as the commands ask, leaving the work running
     * in front of the terminal.
     *
     * @param commands {@code fetch}, {@code extract}, {@code update} or {@code auto}, in the order typed
     */
    default Installing portsnap(final List<String> commands) {
        return Installing.said(
                ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(Text.literal("portsnap"), NO_PORTS_TREE)));
    }

    /**
     * make, in the folder the shell stands in: in a port's folder, builds the port on this machine and installs,
     * cleans or removes it as the targets ask; anywhere else, there is nothing to make.
     *
     * @param targets what make was asked to make, in the order typed; none means build
     */
    default Installing makePort(final List<String> targets) {
        return Installing.said(ICliComputer.OpResult.fail(NO_TARGET));
    }

    /** Whether a network mirror is reachable from this computer right now. */
    default boolean mirrorReachable() {
        return false;
    }

    /** Controls the Mirror service on the network's Mainframe: {@code install|status}. */
    default ICliComputer.OpResult mirrorControl(final String action) {
        return ICliComputer.OpResult.fail(NO_MAINFRAME);
    }

    /**
     * What the shell prints ahead of the next command, each returned once and then forgotten: what the machine
     * itself has to say, such as the programs a save could not bring back.
     */
    default List<String> drainNotices() {
        return List.of();
    }

    /**
     * A package manager's lines as one answer, one under the next. They are paired off down a balanced tree rather
     * than one inside the next, so an answer of many lines stays a shallow sentence that still travels whole to a
     * player's screen.
     */
    static Text lines(final List<Text> lines) {
        if (lines.isEmpty()) {
            return Text.EMPTY;
        }
        if (lines.size() == 1) {
            return lines.getFirst();
        }
        final int half = lines.size() / 2;
        return LINES.with(lines(lines.subList(0, half)), lines(lines.subList(half, lines.size())));
    }

    /**
     * The lines an answer was put together from, in order, so the command that prints it can colour the last one,
     * which is the line that says it went well. Words that are data are broken at their own line breaks too; a
     * declared sentence stays one line, however many rows a translator gives it.
     */
    static List<Text> rows(final Text said) {
        final List<Text> out = new ArrayList<>();
        rowsInto(out, said);
        return out;
    }

    private static void rowsInto(final List<Text> out, final Text said) {
        if (said instanceof Text.Translated sentence && sentence.key().key().equals(LINES.key())) {
            for (final Text part : sentence.args()) {
                rowsInto(out, part);
            }
        } else if (said instanceof Text.Literal words && words.value().indexOf('\n') >= 0) {
            // Split the way the lines were always split, so a trailing break adds no empty line.
            for (final String row : words.value().split("\n")) {
                out.add(Text.literal(row));
            }
        } else {
            out.add(said);
        }
    }
}
