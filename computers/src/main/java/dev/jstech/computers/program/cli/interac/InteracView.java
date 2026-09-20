/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import dev.jstech.computers.program.cli.CliText;
import dev.jstech.computers.program.cli.ICliComputer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the full-screen view shows, read off the machine.
 *
 * <p>The machine draws the screen and the terminal only shows it, which is how the view at a monitor, in a
 * window on a desktop, and over a session opened on another machine is one and the same view. It is also what
 * keeps the network out of the terminal: nothing of it is held there between one key and the next.
 */
public final class InteracView {

    /** What marks an action as one to carry out rather than one still being typed. */
    public static final String NOW = "!";

    /** How many rows a tab gathers before it stops, which is far more than a glass holds. */
    private static final int MOST_ROWS = 256;

    /** How wide the name column of the panel beside the list is. */
    private static final int ASIDE_NAME_W = 15;

    /** How wide the number column of that panel is. */
    private static final int ASIDE_COUNT_W = 10;

    private InteracView() {
    }

    /**
     * The screen for that state: carries out what was asked of it, gathers the rows of the tab that is up, and
     * says what the picked row is.
     *
     * <p>How wide it is drawn is part of the state, because the glass asking for it is what says so and the
     * machine drawing it has no glass of its own.
     */
    public static List<String> screen(final ICliComputer computer, final InteracState state) {
        final String message = act(computer, state);
        final List<InteracScreen.Row> rows = rowsOf(computer, state);
        final int picked = Math.min(state.selected(), Math.max(0, rows.size() - 1));
        return InteracScreen.render(state.picking(picked),
                new InteracScreen.Data(glance(computer, state), rows, aside(computer, state, rows, picked),
                        message, asking(state, rows, picked)));
    }

    /** How many rows that state has, so a terminal knows where the list ends without drawing it. */
    public static int rowCount(final ICliComputer computer, final InteracState state) {
        return rowsOf(computer, state).size();
    }

    /**
     * The network at a glance, for the bar along the top: what the status page of the window says.
     *
     * <p>Said in fewer words on a glass with no room for them all, and the one word that never goes is the
     * one about the Mainframe being away: everything else is a number a player can read off the list.
     */
    private static String glance(final ICliComputer computer, final InteracState state) {
        final ICliComputer.NetSummary net = computer.network();
        if (!net.linked()) {
            return "no network";
        }
        final int ops = computer.activeOps().size();
        final String held = CliText.group(computer.networkUse().stored()) + " held";
        if (!InteracScreen.showsPanel(state.columns())) {
            return net.servers() + " srv  " + held + "  " + ops + (ops == 1 ? " op" : " ops")
                    + (net.mainframePresent() ? "" : "  no Mainframe");
        }
        return net.servers() + " servers  " + held + "  " + ops + " in flight  "
                + (net.mainframePresent() ? "Mainframe up" : "no Mainframe");
    }

    /** The rows of the tab that is up, narrowed by what is being searched for. */
    private static List<InteracScreen.Row> rowsOf(final ICliComputer computer, final InteracState state) {
        return switch (state.tab()) {
            case InteracState.TAB_SERVERS -> servers(computer, state.search());
            case InteracState.TAB_LOCKED -> items(computer.locks(), state.search());
            case InteracState.TAB_OPS -> operations(computer, state.search());
            case InteracState.TAB_STARRED -> favourites(computer, state.search());
            default -> items(computer.query(null, "", MOST_ROWS), state.search());
        };
    }

    private static List<InteracScreen.Row> items(final List<ICliComputer.StoredItem> stock,
                                                 final String search) {
        final List<InteracScreen.Row> rows = new ArrayList<>();
        for (final ICliComputer.StoredItem row : InteracRows.filtered(stock, search, "count")) {
            rows.add(new InteracScreen.Row(row.name(), CliText.group(row.quantity()), row.detail()));
        }
        return rows;
    }

    private static List<InteracScreen.Row> servers(final ICliComputer computer, final String search) {
        final List<InteracScreen.Row> rows = new ArrayList<>();
        for (final ICliComputer.ServerUse use : computer.servers()) {
            if (!matches(use.name(), search)) {
                continue;
            }
            final long room = Math.max(1L, use.capacity());
            rows.add(new InteracScreen.Row(use.name(), CliText.group(use.stored()),
                    Math.round(100.0 * use.stored() / room) + "% full"));
        }
        return rows;
    }

    private static List<InteracScreen.Row> operations(final ICliComputer computer, final String search) {
        final List<InteracScreen.Row> rows = new ArrayList<>();
        for (final ICliComputer.ActiveOp op : computer.activeOps()) {
            if (!matches(op.item() + " " + op.type(), search)) {
                continue;
            }
            rows.add(new InteracScreen.Row(op.type().toLowerCase(Locale.ROOT) + " " + op.item(),
                    CliText.group(op.progress()) + "/" + CliText.group(op.total()),
                    op.status().toLowerCase(Locale.ROOT)));
        }
        return rows;
    }

    /**
     * What this computer has starred, with how much of each the network holds now.
     *
     * <p>A star is kept as the kind of thing and its identifier, which is not what a player calls it, so each
     * one is asked after by name here: a starred row reads like any other row of the list.
     */
    private static List<InteracScreen.Row> favourites(final ICliComputer computer, final String search) {
        final List<InteracScreen.Row> rows = new ArrayList<>();
        for (final String starred : computer.favourites()) {
            final String id = starred.contains("|") ? starred.substring(starred.indexOf('|') + 1) : starred;
            final List<ICliComputer.ItemMatch> found = computer.matching(id);
            final String name = found.isEmpty() ? id : found.get(0).name();
            if (!matches(name + " " + id, search)) {
                continue;
            }
            rows.add(new InteracScreen.Row(name,
                    found.isEmpty() ? "-" : CliText.group(found.get(0).quantity()), "starred"));
        }
        return rows;
    }

    /**
     * The panel beside the list: what the picked row is, where it is held and what makes it.
     *
     * <p>The first line is the name, since the list has its own headings and this panel stands beside them.
     * A tab whose rows are not things the network holds says what its row is instead of asking after an item
     * that does not exist.
     */
    private static List<String> aside(final ICliComputer computer, final InteracState state,
                                      final List<InteracScreen.Row> rows, final int picked) {
        if (!InteracScreen.showsPanel(state.columns())) {
            /* No panel is drawn on a glass this narrow, so nothing is asked of the network to fill one. */
            return List.of();
        }
        if (rows.isEmpty()) {
            return List.of("nothing here");
        }
        final InteracScreen.Row row = rows.get(picked);
        if (state.tab() == InteracState.TAB_SERVERS) {
            return List.of(row.name(), "", CliText.pad("holding", ASIDE_NAME_W)
                    + CliText.padLeft(row.count(), ASIDE_COUNT_W), CliText.pad("filled", ASIDE_NAME_W)
                    + CliText.padLeft(row.detail(), ASIDE_COUNT_W));
        }
        final String named = state.tab() == InteracState.TAB_OPS
                ? row.name().substring(row.name().indexOf(' ') + 1) : row.name();
        final List<ICliComputer.ItemMatch> found = computer.matching(named);
        if (found.isEmpty()) {
            return List.of(named, "", row.detail());
        }
        return detail(computer.itemDetail(found.get(0).id()), named);
    }

    /** One item written out for the panel: what it is, who holds it, what makes it and what it goes into. */
    private static List<String> detail(final ICliComputer.ItemDetail detail, final String named) {
        final List<String> out = new ArrayList<>();
        out.add(detail.name().isEmpty() ? named : detail.name());
        out.add(detail.id());
        out.add("");
        out.add(CliText.pad("Held by", ASIDE_NAME_W) + CliText.padLeft(CliText.group(detail.stored()),
                ASIDE_COUNT_W));
        for (final ICliComputer.Holding holding : detail.where()) {
            out.add(CliText.pad("  " + holding.server(), ASIDE_NAME_W)
                    + CliText.padLeft(CliText.group(holding.quantity()), ASIDE_COUNT_W));
        }
        added(out, "Made from", detail.madeBy());
        added(out, "Used in", detail.usedIn());
        return out;
    }

    /** One of a detail's lists under its heading, and nothing at all when it is empty. */
    private static void added(final List<String> out, final String heading, final List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        out.add("");
        out.add(heading);
        for (int i = 0; i < lines.size() && i < 3; i++) {
            out.add("  " + lines.get(i));
        }
    }

    /** The question standing at the foot, when one is. */
    private static String asking(final InteracState state, final List<InteracScreen.Row> rows,
                                 final int picked) {
        if (state.action().isEmpty() || state.action().endsWith(NOW) || rows.isEmpty()) {
            return "";
        }
        final String name = rows.get(picked).name();
        return switch (state.action()) {
            case "get" -> "Into your hands, how many " + name + "? " + state.amount() + "_";
            case "local" -> "Into this computer, how many " + name + "? " + state.amount() + "_";
            case "craft" -> "Make how many " + name + "? " + state.amount() + "_";
            case "lock" -> "Hold back how many " + name + "? " + state.amount() + "_";
            default -> "";
        };
    }

    /**
     * Carries out what the state says was asked for, and says what came of it.
     *
     * <p>Only an action marked as one to carry out is acted on: while a number is still being typed the
     * question stands at the foot and nothing has happened to the network yet.
     */
    private static String act(final ICliComputer computer, final InteracState state) {
        if (!state.action().endsWith(NOW)) {
            return "";
        }
        final String what = state.action().substring(0, state.action().length() - 1);
        if ("put".equals(what)) {
            return computer.storeFromHand(Math.max(0L, state.amount())).message();
        }
        final List<InteracScreen.Row> rows = rowsOf(computer, state);
        if (rows.isEmpty()) {
            return "there is nothing here to do that to";
        }
        if ("stop".equals(what)) {
            return stop(computer, state, rows.size());
        }
        final String name = rows.get(Math.min(state.selected(), rows.size() - 1)).name();
        final List<ICliComputer.ItemMatch> found = computer.matching(name);
        if (found.isEmpty()) {
            return "interac: nothing here is called " + name;
        }
        final String id = found.get(0).id();
        final long many = Math.max(1L, state.amount());
        return switch (what) {
            case "get" -> computer.takeToHand(id, many).message();
            case "local" -> computer.select(id, many).message();
            case "craft" -> computer.craft(id, many).message();
            case "lock" -> computer.lock(id, many).message();
            case "unlock" -> computer.unlock(id).message();
            case "fav" -> computer.setConfig(
                    computer.favourites().contains("item|" + id) ? "unfavourite" : "favourite",
                    "item|" + id).message();
            default -> "";
        };
    }

    /**
     * Calls off the operation the picked row stands for.
     *
     * <p>An operation is known by its number rather than by what it is moving, and two of them can be moving
     * the same thing, so the row is counted out of the same narrowed list the screen drew rather than looked
     * up by name.
     */
    private static String stop(final ICliComputer computer, final InteracState state, final int rowCount) {
        if (state.tab() != InteracState.TAB_OPS) {
            return "only an operation can be called off";
        }
        final List<ICliComputer.ActiveOp> kept = new ArrayList<>();
        for (final ICliComputer.ActiveOp op : computer.activeOps()) {
            if (matches(op.item() + " " + op.type(), state.search())) {
                kept.add(op);
            }
        }
        final int picked = Math.min(state.selected(), Math.min(rowCount, kept.size()) - 1);
        if (picked < 0) {
            return "nothing is in flight";
        }
        return computer.cancelOperation(kept.get(picked).id()).message();
    }

    private static boolean matches(final String text, final String search) {
        return search.isEmpty() || text.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }
}
