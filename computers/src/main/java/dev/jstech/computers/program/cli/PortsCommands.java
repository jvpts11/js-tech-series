/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.Platform;
import java.util.List;

/**
 * FreeBSD's ports at the prompt: {@code portsnap}, which brings the tree of ports from the Mirror and lays it out
 * under {@code /usr/ports}, and {@code make}, which in a port's folder builds that port on this machine.
 *
 * <p>The other way of installing on FreeBSD. {@code pkg} installs a package somebody else built; a port is built
 * here, for this processor, and what it installs asks a little less of the machine than the package does, for the
 * price of the time the build takes.
 */
final class PortsCommands {

    private PortsCommands() {
    }

    /** portsnap: the ports tree, fetched as one snapshot and laid out, or brought up to date. */
    static final class Portsnap implements ICliCommand {

        @Override
        public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD).needing(CommandScope.Need.FILES);
        }

        @Override
        public String name() {
            return "portsnap";
        }

        @Override
        public String summary() {
            return "fetch the ports tree from the network mirror and lay it out";
        }

        @Override
        public String usage() {
            return "fetch | extract | update | auto ...";
        }

        @Override
        public List<String> description() {
            return List.of("Brings the ports tree from the network's Mirror as one snapshot and lays it out under",
                    "/usr/ports: a folder for every program the Mirror serves, filed by category. The commands",
                    "are done in the order they are typed.",
                    "",
                    "The tree is files on the disk and takes the room they take.");
        }

        @Override
        public List<Option> options() {
            return List.of(new Option("fetch", "bring the snapshot from the Mirror"),
                    new Option("extract", "lay the tree out afresh from the snapshot, replacing any tree there"),
                    new Option("update", "bring a tree already laid out up to date with the snapshot"),
                    new Option("auto", "fetch, then extract or update, whichever the machine needs"));
        }

        @Override
        public List<Example> examples() {
            return List.of(new Example("portsnap fetch extract", "the first time: fetch the tree and lay it out"),
                    new Example("portsnap fetch update", "afterwards: bring the tree up to date"));
        }

        @Override
        public List<String> seeAlso() {
            return List.of("make", "pkg");
        }

        @Override
        public void run(final CliContext ctx) {
            said(ctx, ctx.computer().portsnap(ctx.args()));
        }
    }

    /** make: in a port's folder, that port built on this machine and installed, cleaned or removed. */
    static final class Make implements ICliCommand {

        @Override
        public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD).needing(CommandScope.Need.FILES);
        }

        @Override
        public String name() {
            return "make";
        }

        @Override
        public String summary() {
            return "build the port in this folder of the ports tree";
        }

        @Override
        public String usage() {
            return "[build] [install | reinstall | deinstall] [clean]";
        }

        @Override
        public List<String> description() {
            return List.of("In a port's folder of the ports tree, builds that port on this machine from its",
                    "source, fetched from the network's Mirror, for as long as this machine's processor",
                    "takes over it. What a port installs is built for this machine, and asks a little less",
                    "of it than the package does: less memory, less disk and a lesser processor.",
                    "",
                    "A build without clean leaves its work folder, and a later install finds it done.");
        }

        @Override
        public List<Option> options() {
            return List.of(new Option("build", "build the port, which is what make does when told nothing"),
                    new Option("install", "build the port if it is not built, and install it"),
                    new Option("reinstall", "build and install it again over the one installed"),
                    new Option("deinstall", "take the installed port off the machine"),
                    new Option("clean", "take away the port's work folder"));
        }

        @Override
        public List<Example> examples() {
            return List.of(new Example("cd /usr/ports/sysutils/screenfetch", "go to the port's folder"),
                    new Example("make install clean", "build it, install it and clean up after it"));
        }

        @Override
        public List<String> seeAlso() {
            return List.of("portsnap", "pkg");
        }

        @Override
        public void run(final CliContext ctx) {
            said(ctx, ctx.computer().makePort(ctx.args()));
        }
    }

    /** What the ports answered: the work left running in front of the terminal, or said at once, line by line. */
    private static void said(final CliContext ctx, final ICliPackages.Installing result) {
        if (result.tool() != null) {
            ctx.out().start(result.tool());
            return;
        }
        for (final String line : result.message().split("\n", -1)) {
            if (result.ok()) {
                ctx.out().line(line);
            } else {
                ctx.out().error(line);
            }
        }
    }
}
