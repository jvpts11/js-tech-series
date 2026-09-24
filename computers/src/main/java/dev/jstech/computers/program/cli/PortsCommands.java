/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.Platform;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
    @TextHolder
    static final class Portsnap implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.ports.portsnap.summary",
                "fetch the ports tree from the network mirror and lay it out");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.ports.portsnap.about",
                "Brings the ports tree from the network's Mirror as one snapshot and lays it out under /usr/ports:"
                        + " a folder for every program the Mirror serves, filed by category. The commands are done"
                        + " in the order they are typed.");
        private static final TextKey ABOUT_ROOM = TextKey.of("jsc.cli.ports.portsnap.about.room",
                "The tree is files on the disk and takes the room they take.");
        private static final TextKey OPTION_FETCH = TextKey.of("jsc.cli.ports.portsnap.option.fetch",
                "bring the snapshot from the Mirror");
        private static final TextKey OPTION_EXTRACT = TextKey.of("jsc.cli.ports.portsnap.option.extract",
                "lay the tree out afresh from the snapshot, replacing any tree there");
        private static final TextKey OPTION_UPDATE = TextKey.of("jsc.cli.ports.portsnap.option.update",
                "bring a tree already laid out up to date with the snapshot");
        private static final TextKey OPTION_AUTO = TextKey.of("jsc.cli.ports.portsnap.option.auto",
                "fetch, then extract or update, whichever the machine needs");
        private static final TextKey EXAMPLE_FIRST = TextKey.of("jsc.cli.ports.portsnap.example.first",
                "the first time: fetch the tree and lay it out");
        private static final TextKey EXAMPLE_AFTER = TextKey.of("jsc.cli.ports.portsnap.example.after",
                "afterwards: bring the tree up to date");

        @Override
        public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD).needing(CommandScope.Need.FILES);
        }

        @Override
        public String name() {
            return "portsnap";
        }

        @Override
        public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override
        public Text summary() {
            return SUMMARY.text();
        }

        /** Nothing but the words it takes, which are typed as they are in every language. */
        @Override
        public Text usage() {
            return Text.literal("fetch | extract | update | auto ...");
        }

        @Override
        public List<Text> description() {
            return List.of(ABOUT.text(), ABOUT_ROOM.text());
        }

        @Override
        public List<Option> options() {
            return List.of(new Option("fetch", OPTION_FETCH),
                    new Option("extract", OPTION_EXTRACT),
                    new Option("update", OPTION_UPDATE),
                    new Option("auto", OPTION_AUTO));
        }

        @Override
        public List<Example> examples() {
            return List.of(new Example("portsnap fetch extract", EXAMPLE_FIRST),
                    new Example("portsnap fetch update", EXAMPLE_AFTER));
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
    @TextHolder
    static final class Make implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.ports.make.summary",
                "build the port in this folder of the ports tree");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.ports.make.about",
                "In a port's folder of the ports tree, builds that port on this machine from its source, fetched"
                        + " from the network's Mirror, for as long as this machine's processor takes over it. What a"
                        + " port installs is built for this machine, and asks a little less of it than the package"
                        + " does: less memory, less disk and a lesser processor.");
        private static final TextKey ABOUT_CLEAN = TextKey.of("jsc.cli.ports.make.about.clean",
                "A build without clean leaves its work folder, and a later install finds it done.");
        private static final TextKey OPTION_BUILD = TextKey.of("jsc.cli.ports.make.option.build",
                "build the port, which is what make does when told nothing");
        private static final TextKey OPTION_INSTALL = TextKey.of("jsc.cli.ports.make.option.install",
                "build the port if it is not built, and install it");
        private static final TextKey OPTION_REINSTALL = TextKey.of("jsc.cli.ports.make.option.reinstall",
                "build and install it again over the one installed");
        private static final TextKey OPTION_DEINSTALL = TextKey.of("jsc.cli.ports.make.option.deinstall",
                "take the installed port off the machine");
        private static final TextKey OPTION_CLEAN = TextKey.of("jsc.cli.ports.make.option.clean",
                "take away the port's work folder");
        private static final TextKey EXAMPLE_FOLDER = TextKey.of("jsc.cli.ports.make.example.folder",
                "go to the port's folder");
        private static final TextKey EXAMPLE_INSTALL = TextKey.of("jsc.cli.ports.make.example.install",
                "build it, install it and clean up after it");

        @Override
        public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD).needing(CommandScope.Need.FILES);
        }

        @Override
        public String name() {
            return "make";
        }

        @Override
        public CommandGroup group() {
            return CommandGroup.SOFTWARE;
        }

        @Override
        public Text summary() {
            return SUMMARY.text();
        }

        /** Nothing but the words it takes, which are typed as they are in every language. */
        @Override
        public Text usage() {
            return Text.literal("[build] [install | reinstall | deinstall] [clean]");
        }

        @Override
        public List<Text> description() {
            return List.of(ABOUT.text(), ABOUT_CLEAN.text());
        }

        @Override
        public List<Option> options() {
            return List.of(new Option("build", OPTION_BUILD),
                    new Option("install", OPTION_INSTALL),
                    new Option("reinstall", OPTION_REINSTALL),
                    new Option("deinstall", OPTION_DEINSTALL),
                    new Option("clean", OPTION_CLEAN));
        }

        @Override
        public List<Example> examples() {
            return List.of(new Example("cd /usr/ports/sysutils/screenfetch", EXAMPLE_FOLDER),
                    new Example("make install clean", EXAMPLE_INSTALL));
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

    /**
     * What the ports answered: the work left running in front of the terminal, or said at once. A message of
     * several lines stays one piece of text, broken into rows where it is read, so it is read in its reader's
     * language.
     */
    private static void said(final CliContext ctx, final ICliPackages.Installing result) {
        if (result.tool() != null) {
            ctx.out().start(result.tool());
            return;
        }
        ctx.out().styled(result.message(), result.ok() ? CliStyle.PLAIN : CliStyle.ERROR);
    }
}
