/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import dev.jstech.computers.program.cli.CliContext;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.CliText;
import dev.jstech.computers.program.cli.CommandGroup;
import dev.jstech.computers.program.cli.CommandScope;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.ICliComputer;
import java.util.List;
import java.util.Locale;

/**
 * The network at the prompt: what it holds, where, what makes a thing, and the work in flight.
 *
 * <p>Working a network from a terminal meant writing IQL, which is a language to learn before anything can be
 * asked at all. This is the other half: a few words that do what the graphical Network Interactor does, on
 * every system that has a prompt and a cable, with nothing to install.
 *
 * <p>An item is named the way a player says it, so {@code cobblestone}, {@code minecraft:cobblestone} and
 * {@code "oak log"} all work; a name that fits several things is answered with the several rather than
 * guessed at.
 */
public final class InteracCommand implements ICliCommand, CliShell.IHandOver {

    /** The words it takes, which is also the list a verb written as an option is looked for in. */
    private static final List<String> VERBS = List.of("status", "list", "get", "put", "fill", "fav", "where",
            "info", "craft", "ops", "cancel", "lock", "unlock", "locks", "stats");

    /** How many rows a listing shows before it says how many more there were. */
    private static final int MOST_ROWS = 16;

    /** How wide the name column of a listing is, leaving room for a count and where it is. */
    private static final int NAME_WIDTH = 26;

    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().needing(CommandScope.Need.NETWORK);
    }

    @Override
    public String name() {
        return "interac";
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.NETWORK;
    }

    @Override
    public String summary() {
        return "work the data network: what it holds, where, and what it is doing";
    }

    @Override
    public String usage() {
        return "[list [text]] [get n item] [put n item|hand] [craft n item] [where item] [info item] [ops]";
    }

    @Override
    public List<String> description() {
        return List.of(
                "Shows what the data network is holding and moves it about, without writing a line of IQL.",
                "On its own it takes the whole terminal: headings for the network, the servers, what is held",
                "back, the work in flight and what has been starred, a list under them, and the ten numbered",
                "keys along the foot. Arrows move, Tab goes to the next heading, typing looks for something,",
                "and F10 gives the terminal back.",
                "",
                "With words it does the one thing asked and gives the prompt back; an operation that takes",
                "time prints its number and its result appears before the next prompt.",
                "",
                "An item is named the way you would say it: cobblestone, minecraft:cobblestone or \"oak log\".",
                "A name that fits more than one thing is answered with the things it fits, so that the next",
                "line you type names the one you meant.");
    }

    @Override
    public List<Option> options() {
        return List.of(
                new Option("--to local   (/LOCAL)", "leave what is taken in this computer, not your hands"),
                new Option("--sort count (/S:COUNT)", "order a listing by how much there is; --sort name by name"));
    }

    @Override
    public List<Example> examples() {
        return List.of(
                new Example("interac", "the whole network on the whole glass, worked with the keyboard"),
                new Example("interac status", "the network at a glance: whose it is and what it holds"),
                new Example("interac list stone --sort count", "what it holds whose name has stone in it"),
                new Example("interac get 42 cobblestone", "42 into your own hands, through this computer"),
                new Example("interac put hand", "what you are holding goes into the network"),
                new Example("interac where diamond", "which servers hold it, and how much each has"));
    }

    @Override
    public List<String> seeAlso() {
        return List.of("iql", "net", "ssh");
    }

    /**
     * With nothing said after it, the terminal is given to the full-screen view; with words, the one thing
     * asked is done at the prompt and the prompt is kept.
     *
     * <p>What is handed over is not a file on a disk but the state the view opens in, which is how the
     * machine is asked for a screen: the same way it is asked for a file.
     */
    @Override
    public String fileOf(final ICliComputer computer, final List<String> args) {
        return args.isEmpty() ? InteracState.OPENING.path() : null;
    }

    @Override
    public void run(final CliContext ctx) {
        // What it does is its manual page, which the shell itself answers for; there is no second copy here.
        if (!ctx.hasArgs()) {
            // The terminal has been given away; there is nothing to print behind it.
            return;
        }
        final InteracWords words = InteracWords.of(ctx.args()).withVerbFrom(VERBS);
        switch (words.verb()) {
            case "", "status" -> status(ctx);
            case "list", "ls" -> list(ctx, words);
            case "get" -> get(ctx, words);
            case "put" -> put(ctx, words);
            case "fill" -> fill(ctx, words);
            case "fav" -> fav(ctx, words);
            case "where" -> where(ctx, words);
            case "info" -> info(ctx, words);
            case "craft" -> craft(ctx, words);
            case "ops" -> ops(ctx);
            case "cancel" -> cancel(ctx, words);
            case "lock" -> lock(ctx, words);
            case "unlock" -> unlock(ctx, words);
            case "locks" -> locks(ctx);
            case "stats" -> stats(ctx);
            default -> ctx.out().error("interac: no such word '" + words.verb() + "'. Try 'interac /?'");
        }
    }

    /** The network at a glance: whose it is, whether it is orchestrated, and what it is holding. */
    private static void status(final CliContext ctx) {
        final ICliComputer computer = ctx.computer();
        final ICliComputer.NetSummary net = computer.network();
        ctx.out().line(CliLine.build()
                .add("Network ", CliStyle.DIM).add(computer.networkId(), CliStyle.ACCENT)
                .add("   Mainframe ", CliStyle.DIM)
                .add(net.mainframePresent() ? "online" : "none",
                        net.mainframePresent() ? CliStyle.OK : CliStyle.ERROR)
                .add("   " + net.servers() + " servers", CliStyle.PLAIN)
                .add("   " + computer.activeOps().size() + " operations in flight", CliStyle.PLAIN)
                .done());
        final ICliComputer.ServerUse use = computer.networkUse();
        ctx.out().row("Stored", CliText.group(use.stored()) + " of " + CliText.group(use.capacity())
                + " in " + CliText.group(net.indexedTypes()) + " kinds");
    }

    /** What the network holds, most first, with where each thing lives. */
    private static void list(final CliContext ctx, final InteracWords words) {
        final String text = words.item();
        final List<ICliComputer.StoredItem> stock = ctx.computer().query(null, "", MOST_ROWS * 8);
        final List<ICliComputer.StoredItem> rows = InteracRows.filtered(stock, text,
                words.option("sort", "count"));
        if (rows.isEmpty()) {
            ctx.out().dim(text.isEmpty() ? "the network holds nothing" : "nothing matches \"" + text + "\"");
            return;
        }
        ctx.out().header(CliText.pad("ITEM", NAME_WIDTH) + CliText.padLeft("COUNT", 9) + "  WHERE");
        for (int i = 0; i < rows.size() && i < MOST_ROWS; i++) {
            final ICliComputer.StoredItem row = rows.get(i);
            ctx.out().line(CliText.pad(row.name(), NAME_WIDTH)
                    + CliText.padLeft(CliText.group(row.quantity()), 9) + "  " + row.detail());
        }
        ctx.out().dim(rows.size() + (rows.size() == 1 ? " kind" : " kinds")
                + (text.isEmpty() ? "" : " match \"" + text + "\"")
                + (rows.size() > MOST_ROWS ? ", showing the first " + MOST_ROWS : ""));
    }

    /**
     * Takes something out of the network: into the player's own hands, or into this computer's storage.
     *
     * <p>Into the hands is what a player means by taking something, so that is what it does with nothing
     * said. {@code --to local} leaves it in the machine, which is also the only way a session opened on
     * another machine can ask, since nobody is standing in front of that one.
     */
    private static void get(final CliContext ctx, final InteracWords words) {
        final long quantity = words.quantity();
        if (quantity < 0L) {
            ctx.out().error("usage: interac get <quantity> <item> [--to local]");
            return;
        }
        final ICliComputer.ItemMatch one = resolve(ctx, words.item(), "get");
        if (one == null) {
            return;
        }
        final boolean local = words.option("to", "").equals("local") || words.has("local");
        say(ctx, local ? ctx.computer().select(one.id(), quantity) : ctx.computer().takeToHand(one.id(), quantity));
    }

    /**
     * Hands something to the network: what the player is holding, or what this computer is holding.
     *
     * <p>{@code put hand} is the one that reads as it is meant, so the word {@code hand} stands where an item
     * name would; anything else names something out of this machine's own storage.
     */
    private static void put(final CliContext ctx, final InteracWords words) {
        final String named = words.item();
        if (named.equalsIgnoreCase("hand") || named.isEmpty()) {
            say(ctx, ctx.computer().storeFromHand(words.quantity()));
            return;
        }
        final long quantity = words.quantity();
        final ICliComputer.ItemMatch one = resolve(ctx, named, "put");
        if (one == null) {
            return;
        }
        say(ctx, ctx.computer().insert(one.id(), Math.max(0L, quantity)));
    }

    /** Fills what the player is holding with a fluid the network has. */
    private static void fill(final CliContext ctx, final InteracWords words) {
        if (words.item().isEmpty()) {
            ctx.out().error("usage: interac fill <fluid>");
            return;
        }
        say(ctx, ctx.computer().fillHeld(words.item()));
    }

    /**
     * What this computer has starred, and starring or unstarring one thing.
     *
     * <p>The same stars the window shows, since they belong to the computer: what is starred at the prompt is
     * starred in the Network Interactor when it is next opened.
     */
    private static void fav(final CliContext ctx, final InteracWords words) {
        if (words.item().isEmpty()) {
            final List<String> starred = ctx.computer().favourites();
            if (starred.isEmpty()) {
                ctx.out().dim("nothing is starred on this computer");
                return;
            }
            for (final String id : starred) {
                ctx.out().line("  " + id);
            }
            return;
        }
        final ICliComputer.ItemMatch one = resolve(ctx, words.item(), "fav");
        if (one == null) {
            return;
        }
        final String id = "item|" + one.id();
        final boolean starred = ctx.computer().favourites().contains(id);
        say(ctx, ctx.computer().setConfig(starred ? "unfavourite" : "favourite", id));
    }

    /** Which servers hold a thing, and how much each holds. */
    private static void where(final CliContext ctx, final InteracWords words) {
        final ICliComputer.ItemMatch one = resolve(ctx, words.item(), "where");
        if (one == null) {
            return;
        }
        final List<ICliComputer.Holding> holdings = ctx.computer().find(one.id());
        ctx.out().line(CliText.pad(one.name(), NAME_WIDTH)
                + CliText.padLeft(CliText.group(one.quantity()), 9) + "  in all");
        if (holdings.isEmpty()) {
            ctx.out().dim("no server holds any");
            return;
        }
        for (final ICliComputer.Holding holding : holdings) {
            ctx.out().line("   " + CliText.pad(holding.server(), NAME_WIDTH - 3)
                    + CliText.padLeft(CliText.group(holding.quantity()), 9));
        }
    }

    /** What a thing is, how much there is, what makes it and what it goes into. */
    private static void info(final CliContext ctx, final InteracWords words) {
        final ICliComputer.ItemMatch one = resolve(ctx, words.item(), "info");
        if (one == null) {
            return;
        }
        final ICliComputer.ItemDetail detail = ctx.computer().itemDetail(one.id());
        ctx.out().line(CliText.pad(detail.name(), NAME_WIDTH) + CliText.pad(detail.id(), 30)
                + CliText.group(detail.stored()) + " stored");
        row(ctx, "Made from", detail.madeBy());
        row(ctx, "Used in", detail.usedIn());
        if (detail.madeBy().isEmpty() && detail.usedIn().isEmpty()) {
            ctx.out().dim("the network knows no recipe for it");
        }
    }

    /** Asks the network to make something, which is an operation like any other. */
    private static void craft(final CliContext ctx, final InteracWords words) {
        final long quantity = words.quantity();
        if (quantity < 0L) {
            ctx.out().error("usage: interac craft <quantity> <item>");
            return;
        }
        final ICliComputer.ItemMatch one = resolve(ctx, words.item(), "craft");
        if (one == null) {
            return;
        }
        say(ctx, ctx.computer().craft(one.id(), quantity));
    }

    private static void ops(final CliContext ctx) {
        final List<ICliComputer.ActiveOp> ops = ctx.computer().activeOps();
        if (ops.isEmpty()) {
            ctx.out().dim("nothing in flight");
            return;
        }
        ctx.out().header(CliText.pad("ID", 6) + CliText.pad("WHAT", 10) + CliText.pad("ITEM", NAME_WIDTH)
                + "PROGRESS");
        for (final ICliComputer.ActiveOp op : ops) {
            ctx.out().line(CliText.pad(op.id(), 6) + CliText.pad(op.type().toLowerCase(Locale.ROOT), 10)
                    + CliText.pad(op.item(), NAME_WIDTH)
                    + CliText.group(op.progress()) + "/" + CliText.group(op.total())
                    + "  " + op.status().toLowerCase(Locale.ROOT));
        }
    }

    private static void cancel(final CliContext ctx, final InteracWords words) {
        if (words.wordCount() == 0) {
            ctx.out().error("usage: interac cancel <id>   (see 'interac ops')");
            return;
        }
        say(ctx, ctx.computer().cancelOperation(words.word(0)));
    }

    private static void lock(final CliContext ctx, final InteracWords words) {
        final ICliComputer.ItemMatch one = resolve(ctx, words.item(), "lock");
        if (one == null) {
            return;
        }
        say(ctx, ctx.computer().lock(one.id(), Math.max(0L, words.quantity())));
    }

    private static void unlock(final CliContext ctx, final InteracWords words) {
        final ICliComputer.ItemMatch one = resolve(ctx, words.item(), "unlock");
        if (one == null) {
            return;
        }
        say(ctx, ctx.computer().unlock(one.id()));
    }

    private static void locks(final CliContext ctx) {
        final List<ICliComputer.StoredItem> held = ctx.computer().locks();
        if (held.isEmpty()) {
            ctx.out().dim("no items are locked");
            return;
        }
        for (final ICliComputer.StoredItem row : held) {
            ctx.out().row(row.name(), CliText.group(row.quantity()));
        }
    }

    private static void stats(final CliContext ctx) {
        final List<ICliComputer.OperationStat> stats = ctx.computer().operationStats();
        ctx.out().dim("peak " + ctx.computer().peakOperationsToday() + " in flight today");
        if (stats.isEmpty()) {
            ctx.out().dim("no operations settled in the last hour");
            return;
        }
        for (final ICliComputer.OperationStat stat : stats) {
            ctx.out().row(stat.type(), stat.count() + " ops  wait " + ticks(stat.averageWait())
                    + "  run " + ticks(stat.averageRun()) + "  fail " + stat.shortfallPercent()
                    + "%  moved " + CliText.group(stat.moved()));
        }
    }

    /** Ticks while they are few, and seconds once there are enough of them to be worth reading as seconds. */
    private static String ticks(final int ticks) {
        return ticks >= 1200 ? ticks / 20 + "s" : ticks + "t";
    }

    /**
     * The one thing that name stands for, or null once the machine has said why there is none.
     *
     * <p>A name that fits several is not a mistake and is not guessed at: the several are shown, with how much
     * of each the network holds, so the next line the player types names the one they meant.
     */
    private static ICliComputer.ItemMatch resolve(final CliContext ctx, final String text, final String verb) {
        if (text.isEmpty()) {
            ctx.out().error("usage: interac " + verb + " <item>");
            return null;
        }
        final List<ICliComputer.ItemMatch> matches = ctx.computer().matching(text);
        if (matches.isEmpty()) {
            ctx.out().error("interac: nothing here is called \"" + text + "\"");
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        final StringBuilder names = new StringBuilder();
        for (final ICliComputer.ItemMatch match : matches) {
            names.append(names.isEmpty() ? "" : ", ").append(match.name());
        }
        ctx.out().error("interac: \"" + text + "\" fits " + matches.size() + " things: " + names
                + ". Say which one.");
        return null;
    }

    /** Prints one of a detail's lists under its heading, and nothing at all when it is empty. */
    private static void row(final CliContext ctx, final String heading, final List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            ctx.out().row(i == 0 ? heading : "", lines.get(i));
        }
    }

    private static void say(final CliContext ctx, final ICliComputer.OpResult result) {
        ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
    }

}
