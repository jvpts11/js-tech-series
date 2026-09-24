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
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.CliText;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.CommandGroup;
import dev.jstech.computers.program.cli.CommandScope;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
@TextHolder
public final class InteracCommand implements ICliCommand, CliShell.IHandOver {

    /** The word it is typed as, which is also what its complaints open with. */
    private static final String NAME = "interac";

    /** The words it takes, which is also the list a verb written as an option is looked for in. */
    private static final List<String> VERBS = List.of("status", "list", "get", "put", "fill", "fav", "where",
            "info", "craft", "ops", "cancel", "lock", "unlock", "locks", "stats");

    /** How many rows a listing shows before it says how many more there were. */
    private static final int MOST_ROWS = 16;

    /** How wide the name column of a listing is, leaving room for a count and where it is. */
    private static final int NAME_WIDTH = 26;

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.interac.summary",
            "work the data network: what it holds, where, and what it is doing");
    private static final TextKey USAGE = TextKey.of("jsc.cli.interac.usage",
            "[list [text]] [get n item] [put n item|hand] [craft n item] [where item] [info item] [ops]");
    private static final TextKey ABOUT = TextKey.of("jsc.cli.interac.about", "Shows what the data network is"
            + " holding and moves it about, without writing a line of IQL. On its own it takes the whole terminal:"
            + " headings for the network, the servers, what is held back, the work in flight and what has been"
            + " starred, a list under them, and the ten numbered keys along the foot. Arrows move, Tab goes to the"
            + " next heading, typing looks for something, and F10 gives the terminal back.");
    private static final TextKey ABOUT_WORDS = TextKey.of("jsc.cli.interac.about.words", "With words it does the"
            + " one thing asked and gives the prompt back; an operation that takes time prints its number and its"
            + " result appears before the next prompt.");
    private static final TextKey ABOUT_NAMES = TextKey.of("jsc.cli.interac.about.names", "An item is named the"
            + " way you would say it: cobblestone, minecraft:cobblestone or \"oak log\". A name that fits more than"
            + " one thing is answered with the things it fits, so that the next line you type names the one you"
            + " meant.");
    private static final TextKey OPTION_TO = TextKey.of("jsc.cli.interac.option.to",
            "leave what is taken in this computer, not your hands");
    private static final TextKey OPTION_SORT = TextKey.of("jsc.cli.interac.option.sort",
            "order a listing by how much there is; --sort name by name");
    private static final TextKey EXAMPLE_WHOLE = TextKey.of("jsc.cli.interac.example.whole",
            "the whole network on the whole glass, worked with the keyboard");
    private static final TextKey EXAMPLE_STATUS = TextKey.of("jsc.cli.interac.example.status",
            "the network at a glance: whose it is and what it holds");
    private static final TextKey EXAMPLE_LIST = TextKey.of("jsc.cli.interac.example.list",
            "what it holds whose name has stone in it");
    private static final TextKey EXAMPLE_GET = TextKey.of("jsc.cli.interac.example.get",
            "42 into your own hands, through this computer");
    private static final TextKey EXAMPLE_PUT = TextKey.of("jsc.cli.interac.example.put",
            "what you are holding goes into the network");
    private static final TextKey EXAMPLE_WHERE = TextKey.of("jsc.cli.interac.example.where",
            "which servers hold it, and how much each has");

    private static final TextKey GET_USAGE =
            TextKey.of("jsc.cli.interac.get_usage", "get <quantity> <item> [--to local]");
    private static final TextKey FILL_USAGE = TextKey.of("jsc.cli.interac.fill_usage", "fill <fluid>");
    private static final TextKey CRAFT_USAGE = TextKey.of("jsc.cli.interac.craft_usage", "craft <quantity> <item>");
    private static final TextKey CANCEL_USAGE =
            TextKey.of("jsc.cli.interac.cancel_usage", "cancel <id>   (see 'interac ops')");
    private static final TextKey ITEM_USAGE = TextKey.of("jsc.cli.interac.item_usage", "%s <item>");
    private static final TextKey NO_SUCH_WORD =
            TextKey.of("jsc.cli.interac.no_such_word", "no such word '%s'. Try 'interac /?'");
    private static final TextKey NOT_CALLED =
            TextKey.of("jsc.cli.interac.not_called", "nothing here is called \"%s\"");
    private static final TextKey FITS =
            TextKey.of("jsc.cli.interac.fits", "\"%s\" fits %s things: %s. Say which one.");

    private static final TextKey NETWORK = TextKey.of("jsc.cli.interac.network", "Network");
    private static final TextKey MAINFRAME = TextKey.of("jsc.cli.interac.mainframe", "Mainframe");
    private static final TextKey ONLINE = TextKey.of("jsc.cli.interac.online", "online");
    private static final TextKey NONE = TextKey.of("jsc.cli.interac.none", "none");
    private static final TextKey SERVERS = TextKey.of("jsc.cli.interac.servers", "%s servers");
    private static final TextKey IN_FLIGHT = TextKey.of("jsc.cli.interac.in_flight", "%s operations in flight");
    private static final TextKey STORED = TextKey.of("jsc.cli.interac.stored", "Stored");
    private static final TextKey STORED_VALUE = TextKey.of("jsc.cli.interac.stored_value", "%s of %s in %s kinds");

    /* The headings line up with the columns written under them, so a translation keeps their spacing. */
    private static final TextKey LIST_HEADER =
            TextKey.of("jsc.cli.interac.list_header", "ITEM                          COUNT  WHERE");
    private static final TextKey OPS_HEADER =
            TextKey.of("jsc.cli.interac.ops_header", "ID    WHAT      ITEM                      PROGRESS");

    private static final TextKey HOLDS_NOTHING =
            TextKey.of("jsc.cli.interac.holds_nothing", "the network holds nothing");
    private static final TextKey NOTHING_MATCHES =
            TextKey.of("jsc.cli.interac.nothing_matches", "nothing matches \"%s\"");
    private static final TextKey KIND = TextKey.of("jsc.cli.interac.kind", "%s kind");
    private static final TextKey KINDS = TextKey.of("jsc.cli.interac.kinds", "%s kinds");
    private static final TextKey MATCH = TextKey.of("jsc.cli.interac.match", "match \"%s\"");
    private static final TextKey SHOWING = TextKey.of("jsc.cli.interac.showing", "showing the first %s");
    private static final TextKey NOTHING_STARRED =
            TextKey.of("jsc.cli.interac.nothing_starred", "nothing is starred on this computer");
    private static final TextKey IN_ALL = TextKey.of("jsc.cli.interac.in_all", "in all");
    private static final TextKey NO_SERVER = TextKey.of("jsc.cli.interac.no_server", "no server holds any");
    private static final TextKey STORED_COUNT = TextKey.of("jsc.cli.interac.stored_count", "%s stored");
    /* The two headings of an item's detail, which the full-screen view writes the same way beside its list. */
    static final TextKey MADE_FROM = TextKey.of("jsc.cli.interac.made_from", "Made from");
    static final TextKey USED_IN = TextKey.of("jsc.cli.interac.used_in", "Used in");
    private static final TextKey NO_RECIPE =
            TextKey.of("jsc.cli.interac.no_recipe", "the network knows no recipe for it");
    private static final TextKey NOTHING_IN_FLIGHT =
            TextKey.of("jsc.cli.interac.nothing_in_flight", "nothing in flight");
    private static final TextKey NO_LOCKS = TextKey.of("jsc.cli.interac.no_locks", "no items are locked");
    private static final TextKey PEAK = TextKey.of("jsc.cli.interac.peak", "peak %s in flight today");
    private static final TextKey NONE_SETTLED =
            TextKey.of("jsc.cli.interac.none_settled", "no operations settled in the last hour");
    private static final TextKey STAT_ROW = TextKey.of("jsc.cli.interac.stat_row",
            "%s ops  wait %s  run %s  fail %s%%  moved %s");

    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().needing(CommandScope.Need.NETWORK);
    }

    @Override
    public String name() {
        return NAME;
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
        return USAGE.text();
    }

    @Override
    public List<Text> description() {
        return List.of(ABOUT.text(), ABOUT_WORDS.text(), ABOUT_NAMES.text());
    }

    @Override
    public List<Option> options() {
        return List.of(
                new Option("--to local   (/LOCAL)", OPTION_TO),
                new Option("--sort count (/S:COUNT)", OPTION_SORT));
    }

    @Override
    public List<Example> examples() {
        return List.of(
                new Example("interac", EXAMPLE_WHOLE),
                new Example("interac status", EXAMPLE_STATUS),
                new Example("interac list stone --sort count", EXAMPLE_LIST),
                new Example("interac get 42 cobblestone", EXAMPLE_GET),
                new Example("interac put hand", EXAMPLE_PUT),
                new Example("interac where diamond", EXAMPLE_WHERE));
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
            default -> ctx.out().error(CliTexts.SAID_BY.with(NAME, NO_SUCH_WORD.with(words.verb())));
        }
    }

    /** The network at a glance: whose it is, whether it is orchestrated, and what it is holding. */
    private static void status(final CliContext ctx) {
        final ICliComputer computer = ctx.computer();
        final ICliComputer.NetSummary net = computer.network();
        ctx.out().line(CliLine.build()
                .add(NETWORK, CliStyle.DIM).add(" ", CliStyle.DIM).add(computer.networkId(), CliStyle.ACCENT)
                .add("   ", CliStyle.DIM).add(MAINFRAME, CliStyle.DIM).add(" ", CliStyle.DIM)
                .add(net.mainframePresent() ? ONLINE : NONE,
                        net.mainframePresent() ? CliStyle.OK : CliStyle.ERROR)
                .add("   ", CliStyle.PLAIN).add(SERVERS.with(net.servers()), CliStyle.PLAIN)
                .add("   ", CliStyle.PLAIN).add(IN_FLIGHT.with(computer.activeOps().size()), CliStyle.PLAIN)
                .done());
        final ICliComputer.ServerUse use = computer.networkUse();
        ctx.out().row(STORED.text(), STORED_VALUE.with(CliText.group(use.stored()), CliText.group(use.capacity()),
                CliText.group(net.indexedTypes())));
    }

    /** What the network holds, most first, with where each thing lives. */
    private static void list(final CliContext ctx, final InteracWords words) {
        final String text = words.item();
        final List<ICliComputer.StoredItem> stock = ctx.computer().query(null, "", MOST_ROWS * 8);
        final List<ICliComputer.StoredItem> rows = InteracRows.filtered(stock, text,
                words.option("sort", "count"));
        if (rows.isEmpty()) {
            ctx.out().dim(text.isEmpty() ? HOLDS_NOTHING.text() : NOTHING_MATCHES.with(text));
            return;
        }
        ctx.out().header(LIST_HEADER);
        for (int i = 0; i < rows.size() && i < MOST_ROWS; i++) {
            final ICliComputer.StoredItem row = rows.get(i);
            // The name is in the reader's language, so its column is padded where the line is read.
            ctx.out().line(CliLine.of(CliSpan.plain(row.name()), CliSpan.pad(NAME_WIDTH),
                    CliSpan.plain(CliText.padLeft(CliText.group(row.quantity()), 9) + "  "),
                    CliSpan.plain(row.detail())));
        }
        final CliLine.Builder footer = CliLine.build()
                .add(rows.size() == 1 ? KIND.with(rows.size()) : KINDS.with(rows.size()), CliStyle.DIM);
        if (!text.isEmpty()) {
            footer.add(" ", CliStyle.DIM).add(MATCH.with(text), CliStyle.DIM);
        }
        if (rows.size() > MOST_ROWS) {
            footer.add(", ", CliStyle.DIM).add(SHOWING.with(MOST_ROWS), CliStyle.DIM);
        }
        ctx.out().line(footer.done());
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
            ctx.out().error(CliTexts.USAGE.with(NAME, GET_USAGE));
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
            ctx.out().error(CliTexts.USAGE.with(NAME, FILL_USAGE));
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
                ctx.out().dim(NOTHING_STARRED);
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
        ctx.out().line(CliLine.build().plain(CliText.pad(one.name(), NAME_WIDTH)
                + CliText.padLeft(CliText.group(one.quantity()), 9) + "  ").plain(IN_ALL.text()).done());
        if (holdings.isEmpty()) {
            ctx.out().dim(NO_SERVER);
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
        ctx.out().line(CliLine.build().plain(CliText.pad(detail.name(), NAME_WIDTH) + CliText.pad(detail.id(), 30))
                .plain(STORED_COUNT.with(CliText.group(detail.stored()))).done());
        row(ctx, MADE_FROM, detail.madeBy());
        row(ctx, USED_IN, detail.usedIn());
        if (detail.madeBy().isEmpty() && detail.usedIn().isEmpty()) {
            ctx.out().dim(NO_RECIPE);
        }
    }

    /** Asks the network to make something, which is an operation like any other. */
    private static void craft(final CliContext ctx, final InteracWords words) {
        final long quantity = words.quantity();
        if (quantity < 0L) {
            ctx.out().error(CliTexts.USAGE.with(NAME, CRAFT_USAGE));
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
            ctx.out().dim(NOTHING_IN_FLIGHT);
            return;
        }
        ctx.out().header(OPS_HEADER);
        for (final ICliComputer.ActiveOp op : ops) {
            ctx.out().line(CliText.pad(op.id(), 6) + CliText.pad(op.type().toLowerCase(Locale.ROOT), 10)
                    + CliText.pad(op.item(), NAME_WIDTH)
                    + CliText.group(op.progress()) + "/" + CliText.group(op.total())
                    + "  " + op.status().toLowerCase(Locale.ROOT));
        }
    }

    private static void cancel(final CliContext ctx, final InteracWords words) {
        if (words.wordCount() == 0) {
            ctx.out().error(CliTexts.USAGE.with(NAME, CANCEL_USAGE));
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
            ctx.out().dim(NO_LOCKS);
            return;
        }
        for (final ICliComputer.StoredItem row : held) {
            ctx.out().row(row.name(), Text.literal(CliText.group(row.quantity())));
        }
    }

    private static void stats(final CliContext ctx) {
        final List<ICliComputer.OperationStat> stats = ctx.computer().operationStats();
        ctx.out().dim(PEAK.with(ctx.computer().peakOperationsToday()));
        if (stats.isEmpty()) {
            ctx.out().dim(NONE_SETTLED);
            return;
        }
        for (final ICliComputer.OperationStat stat : stats) {
            ctx.out().row(Text.literal(stat.type()), STAT_ROW.with(stat.count(), ticks(stat.averageWait()),
                    ticks(stat.averageRun()), stat.shortfallPercent(), CliText.group(stat.moved())));
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
            ctx.out().error(CliTexts.USAGE.with(NAME, ITEM_USAGE.with(verb)));
            return null;
        }
        final List<ICliComputer.ItemMatch> matches = ctx.computer().matching(text);
        if (matches.isEmpty()) {
            ctx.out().error(CliTexts.SAID_BY.with(NAME, NOT_CALLED.with(text)));
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        final StringBuilder names = new StringBuilder();
        for (final ICliComputer.ItemMatch match : matches) {
            names.append(names.isEmpty() ? "" : ", ").append(match.name());
        }
        ctx.out().error(CliTexts.SAID_BY.with(NAME, FITS.with(text, matches.size(), names.toString())));
        return null;
    }

    /** Prints one of a detail's lists under its heading, and nothing at all when it is empty. */
    private static void row(final CliContext ctx, final TextKey heading, final List<Text> lines) {
        for (int i = 0; i < lines.size(); i++) {
            ctx.out().row(i == 0 ? heading.text() : Text.EMPTY, lines.get(i));
        }
    }

    private static void say(final CliContext ctx, final ICliComputer.OpResult result) {
        ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
    }

}
