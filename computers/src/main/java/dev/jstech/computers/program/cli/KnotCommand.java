/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.KnotRepository;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Knot at the prompt, the window's three things as git's command line has them: {@code push} saves a file of this
 * machine as a new revision on the network's KnotHub, {@code pull} writes a revision back onto this machine, and
 * {@code log} and {@code status} read the repository. The hub may be on a network this one is federated with, which
 * lets this one read it where its port grants seeing and push to it where it grants sending.
 *
 * <p>A file is kept by its name, without the folders it stood in, so what one machine pushes another pulls by name.
 */
@TextHolder
final class KnotCommand implements ICliCommand {

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.knot.summary",
            "push a file to the network's KnotHub, bring a revision back, read its log");
    private static final TextKey USAGE_LINE = TextKey.of("jsc.cli.knot.usage",
            "push <file> -m <message> | pull <file> [revision] | log [file] | status");
    private static final TextKey NO_HUB = TextKey.of("jsc.cli.knot.no_hub",
            "no KnotHub on this network or on one it is federated with");
    private static final TextKey MAY_NOT_SEE = TextKey.of("jsc.cli.knot.may_not_see",
            "the network of the KnotHub on %s does not let this one see it");
    private static final TextKey MAY_NOT_SEND = TextKey.of("jsc.cli.knot.may_not_send",
            "the network of the KnotHub on %s does not let this one send to it");
    private static final TextKey CANNOT_READ = TextKey.of("jsc.cli.knot.cannot_read", "%s: %s");
    private static final TextKey PUSHED = TextKey.of("jsc.cli.knot.pushed", "revision %s of %s, by %s");
    private static final TextKey NOTHING_CHANGED = TextKey.of("jsc.cli.knot.nothing_changed",
            "%s is as its last revision, or the repository holds no more files: nothing pushed");
    private static final TextKey NOT_KEPT = TextKey.of("jsc.cli.knot.not_kept", "%s is not in the repository");
    private static final TextKey NO_SUCH_REVISION = TextKey.of("jsc.cli.knot.no_such_revision",
            "%s has no revision %s");
    private static final TextKey PULLED = TextKey.of("jsc.cli.knot.pulled", "%s at revision %s written to %s");
    private static final TextKey HUB_HERE = TextKey.of("jsc.cli.knot.hub_here", "KnotHub on %s (this network)");
    private static final TextKey HUB_THERE = TextKey.of("jsc.cli.knot.hub_there", "KnotHub on %s (network %s)");
    private static final TextKey ONE_REVISION = TextKey.of("jsc.cli.knot.one_revision", "1 revision");
    private static final TextKey REVISIONS = TextKey.of("jsc.cli.knot.revisions", "%s revisions");
    private static final TextKey EMPTY = TextKey.of("jsc.cli.knot.empty", "the repository holds nothing yet");
    /* The most revisions the whole repository's log lists, newest first. */
    private static final int LOG_ROWS = 20;
    /* The width of a revision number in the log, as git's short ids line up. */
    private static final int NUMBER_WIDTH = 3;

    /*
     * A program a player installs: 'command not found' until Knot is on the machine. Its id is written out, as the
     * scope reads it, so the command list loads where the game's registries do not.
     */
    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().fromPackage("jsc:knot");
    }

    @Override
    public String name() {
        return "knot";
    }

    @Override
    public Text summary() {
        return SUMMARY.text();
    }

    @Override
    public Text usage() {
        return USAGE_LINE.text();
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.PROGRAMMING;
    }

    @Override
    public void run(final CliContext ctx) {
        final String verb = ctx.arg(0).toLowerCase(Locale.ROOT);
        final boolean sends = verb.equals("push");
        final boolean namesAFile = sends || verb.equals("pull");
        final boolean known = namesAFile || verb.equals("log") || verb.equals("status");
        if (!known || namesAFile && ctx.arg(1).isEmpty()) {
            ctx.out().error(CliTexts.USAGE.with(name(), USAGE_LINE.text()));
            return;
        }
        final ICliKnot hub = ctx.computer().knot();
        if (hub == null) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), NO_HUB.text()));
            return;
        }
        if (sends ? !hub.maySend() : !hub.maySee()) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), (sends ? MAY_NOT_SEND : MAY_NOT_SEE).with(hub.host())));
            return;
        }
        switch (verb) {
            case "push" -> push(ctx, hub);
            case "pull" -> pull(ctx, hub);
            case "log" -> log(ctx, hub.repository(), ctx.arg(1).isEmpty() ? "" : nameOf(ctx.arg(1)));
            default -> status(ctx, hub);
        }
    }

    /* Saves the named file of this machine as a new revision, with the message after -m. */
    private void push(final CliContext ctx, final ICliKnot hub) {
        final String path = ctx.arg(1);
        final ICliComputer.FsResult read = ctx.computer().readFile(path);
        if (!read.ok()) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), CANNOT_READ.with(path, read.message())));
            return;
        }
        final int said = ctx.args().indexOf("-m");
        final String message = said < 0 ? "" : ctx.arg(said + 1);
        final String file = nameOf(path);
        final KnotRepository.Revision made = hub.repository().commit(file, hub.author(), message,
                read.message().english(), hub.now());
        if (made == null) {
            ctx.out().warn(NOTHING_CHANGED.with(file));
            return;
        }
        hub.changed();
        ctx.out().ok(PUSHED.with(made.number(), file, made.author()));
    }

    /* Writes the named file's newest revision, or the one numbered, onto this machine where the path says. */
    private void pull(final CliContext ctx, final ICliKnot hub) {
        final String path = ctx.arg(1);
        final String file = nameOf(path);
        final List<KnotRepository.Revision> revisions = hub.repository().revisionsOf(file);
        if (revisions.isEmpty()) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), NOT_KEPT.with(file)));
            return;
        }
        KnotRepository.Revision chosen = revisions.getLast();
        if (!ctx.arg(2).isEmpty()) {
            chosen = null;
            for (final KnotRepository.Revision revision : revisions) {
                if (String.valueOf(revision.number()).equals(ctx.arg(2))) {
                    chosen = revision;
                }
            }
            if (chosen == null) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), NO_SUCH_REVISION.with(file, ctx.arg(2))));
                return;
            }
        }
        final ICliComputer.FsResult written = ctx.computer().writeMade(path, chosen.content());
        if (!written.ok()) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), written.message()));
            return;
        }
        ctx.out().ok(PULLED.with(file, chosen.number(), path));
    }

    /* The revisions of {@code file}, or of every file when none is named, newest first. */
    private static void log(final CliContext ctx, final KnotRepository repository, final String file) {
        final List<KnotRepository.Revision> rows = new ArrayList<>(file.isEmpty()
                ? repository.recent(LOG_ROWS) : repository.revisionsOf(file).reversed());
        if (rows.isEmpty()) {
            ctx.out().line(file.isEmpty() ? EMPTY.text() : NOT_KEPT.with(file));
            return;
        }
        for (final KnotRepository.Revision revision : rows) {
            final String number = String.valueOf(revision.number());
            final String which = file.isEmpty() ? "  " + repository.fileOf(revision.number()) : "";
            ctx.out().line(" ".repeat(Math.max(0, NUMBER_WIDTH - number.length())) + number + "  "
                    + revision.author() + which + "  " + revision.message());
        }
    }

    /* Which hub this machine reached, and each file it keeps with how many revisions. */
    private static void status(final CliContext ctx, final ICliKnot hub) {
        ctx.out().line(hub.network().isEmpty() ? HUB_HERE.with(hub.host()) : HUB_THERE.with(hub.host(), hub.network()));
        final List<String> files = hub.repository().files();
        if (files.isEmpty()) {
            ctx.out().line(EMPTY.text());
            return;
        }
        int widest = 0;
        for (final String file : files) {
            widest = Math.max(widest, file.length());
        }
        for (final String file : files) {
            final int count = hub.repository().revisionsOf(file).size();
            ctx.out().line("  " + file + " ".repeat(widest - file.length() + 3)
                    + (count == 1 ? ONE_REVISION.text() : REVISIONS.with(count)).english());
        }
    }

    /* The name a file is kept by: its own, without the folders it stood in. */
    private static String nameOf(final String path) {
        return FsPaths.fileName(path.replace('\\', '/'));
    }
}
