/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.program.iql.IqlParseResult;
import dev.jstech.computers.program.iql.IqlParser;
import dev.jstech.computers.program.iql.IqlVerb;
import java.util.List;

/**
 * The commands that reach the data network: what it holds, what it is doing and the statements sent to it.
 *
 * <p>Moved here from BuiltinCommands, which keeps the lists that say which system gets which command.
 */
final class NetworkCommands {

    /** What every one of these needs: a data network under the machine, on any system. */
    private static final CommandScope ON_THE_NETWORK =
            CommandScope.everywhere().needing(CommandScope.Need.NETWORK);

    private NetworkCommands() {
    }

    static final class Net implements ICliCommand {
        /** On every machine, since saying "not on a network" is half of what it is for. */
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "net";
        }

        @Override public List<String> aliases() {
            return List.of("network");
        }

        @Override public String summary() {
            return "summarise the network";
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.NetSummary n = ctx.computer().network();
            if (!n.linked()) {
                ctx.out().error("not on a network");
                return;
            }
            ctx.out().row("network", ctx.computer().networkId());
            ctx.out().row("mainframe", n.mainframePresent() ? "present" : "MISSING");
            ctx.out().row("servers", CliText.group(n.servers()));
            ctx.out().row("computers", CliText.group(n.personalComputers()));
            ctx.out().row("subframes", CliText.group(n.subframes()));
            ctx.out().row("indexed types", CliText.group(n.indexedTypes()));
        }
    }

    static final class Find implements ICliCommand {
        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "find";
        }

        @Override public String summary() {
            return "show which servers hold an item";
        }

        @Override public String usage() {
            return "<item>";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: find <item>");
                return;
            }
            final List<ICliComputer.Holding> holdings = ctx.computer().find(ctx.rest(0));
            if (holdings.isEmpty()) {
                ctx.out().dim("no server holds '" + ctx.rest(0) + "'");
                return;
            }
            for (final ICliComputer.Holding holding : holdings) {
                ctx.out().row(holding.server(), CliText.group(holding.quantity()));
            }
        }
    }

    static final class Lock implements ICliCommand {
        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "lock";
        }

        @Override public List<String> aliases() {
            return List.of("hold");
        }

        @Override public String summary() {
            return "hold an item so concurrent operations wait";
        }

        @Override public String usage() {
            return "<item> | <quantity> <item>";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: lock " + usage());
                return;
            }
            if (!ctx.computer().onNetwork()) {
                ctx.out().error("not on a network");
                return;
            }
            // "lock <quantity> <item>" reserves an amount; "lock <item>" holds everything available.
            final long qty = ctx.longArg(0);
            final String item = qty > 0L && ctx.argCount() >= 2 ? ctx.rest(1) : ctx.rest(0);
            final ICliComputer.OpResult result = ctx.computer().lock(item, qty > 0L ? qty : 0L);
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    static final class Unlock implements ICliCommand {
        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "unlock";
        }

        @Override public List<String> aliases() {
            return List.of("release");
        }

        @Override public String summary() {
            return "release a held item";
        }

        @Override public String usage() {
            return "<item>";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: unlock <item>");
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().unlock(ctx.rest(0));
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    static final class Locks implements ICliCommand {
        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "locks";
        }

        @Override public List<String> aliases() {
            return List.of("holds");
        }

        @Override public String summary() {
            return "list held item types";
        }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.StoredItem> held = ctx.computer().locks();
            if (held.isEmpty()) {
                ctx.out().dim("no items are locked");
                return;
            }
            for (final ICliComputer.StoredItem row : held) {
                ctx.out().row(row.name(), CliText.group(row.quantity()));
            }
        }
    }

    static final class Ops implements ICliCommand {
        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "ops";
        }

        @Override public List<String> aliases() {
            return List.of("jobs");
        }

        @Override public String summary() {
            return "list operations in flight";
        }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.ActiveOp> ops = ctx.computer().activeOps();
            if (ops.isEmpty()) {
                ctx.out().dim("no operations running");
                return;
            }
            for (final ICliComputer.ActiveOp op : ops) {
                final String head = op.id() + "  " + op.type() + " " + op.item();
                final String tail = op.priority() + " " + op.status() + " " + CliText.group(op.progress()) + "/"
                        + CliText.group(op.total());
                ctx.out().row(head, tail);
            }
        }
    }

    static final class Stats implements ICliCommand {
        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "stats";
        }

        @Override public List<String> aliases() {
            return List.of("statistics");
        }

        @Override public String summary() {
            return "the network's operations over the last hour";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.computer().onNetwork()) {
                ctx.out().error("not on a network");
                return;
            }
            final List<ICliComputer.OperationStat> stats = ctx.computer().operationStats();
            ctx.out().dim("peak " + ctx.computer().peakOperationsToday() + " in flight today");
            if (stats.isEmpty()) {
                ctx.out().dim("no operations settled in the last hour");
                return;
            }
            for (final ICliComputer.OperationStat stat : stats) {
                final String tail = stat.count() + " ops  wait " + ticks(stat.averageWait()) + "  run "
                        + ticks(stat.averageRun()) + "  fail " + stat.shortfallPercent() + "%  moved "
                        + CliText.group(stat.moved());
                ctx.out().row(stat.type(), tail);
            }
        }

        private static String ticks(final int ticks) {
            return ticks >= 1200 ? (ticks / 20) + "s" : ticks + "t";
        }
    }

    static final class Cancel implements ICliCommand {
        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "cancel";
        }

        @Override public List<String> aliases() {
            return List.of("kill");
        }

        @Override public String summary() {
            return "stop an operation in flight";
        }

        @Override public String usage() {
            return "<id>";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: cancel <id>   (the id column of 'ops')");
                return;
            }
            if (!ctx.computer().onNetwork()) {
                ctx.out().error("not on a network");
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().cancelOperation(ctx.arg(0));
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    static final class Operation implements ICliCommand {

        private static final int QUERY_LIMIT = 64;

        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "operation";
        }

        @Override public List<String> aliases() {
            return List.of("op", "sql");
        }

        @Override public String summary() {
            return "run an IQL statement on the network";
        }

        @Override public String usage() {
            return "<statement>";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: operation <statement>   e.g. operation SELECT 64 Cobblestone");
                return;
            }
            final IqlParseResult parsed = IqlParser.tryParse(ctx.rest(0));
            if (!parsed.ok()) {
                ctx.out().error("syntax: " + parsed.error());
                return;
            }
            final IqlOperation op = parsed.operation();
            if (op.verb() == IqlVerb.QUERY || op.verb() == IqlVerb.COUNT) {
                if (!ctx.computer().onNetwork()) {
                    ctx.out().error("not on a network");
                    return;
                }
                final int limit = op.limit() > 0 ? op.limit() : QUERY_LIMIT;
                final List<ICliComputer.StoredItem> items = ctx.computer().queryObject(op.item(),
                        op.where(), "", limit);
                if (items.isEmpty()) {
                    ctx.out().dim("no rows");
                    return;
                }
                for (final ICliComputer.StoredItem item : items) {
                    ctx.out().row(item.detail().isEmpty() ? item.name() : item.name() + " · " + item.detail(),
                            CliText.group(item.quantity()));
                }
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().execute(op);
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    /**
     * A remote shell on another machine of the same network, the route that makes a headless rack
     * server administrable from any terminal. {@code ssh} with no argument lists what is reachable;
     * {@code exit} on a connected session comes back to the local shell.
     */
    static final class Ssh implements ICliCommand {
        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "ssh";
        }

        @Override public String summary() {
            return "open a shell on another computer of this network";
        }

        @Override public String usage() {
            return "ssh [hostname]";
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer computer = ctx.computer();
            if (!ctx.hasArgs()) {
                final List<ICliComputer.RemoteHost> hosts = computer.reachableHosts();
                if (hosts.isEmpty()) {
                    ctx.out().error("ssh: no other computers reachable on this network");
                    return;
                }
                ctx.out().line("Reachable hosts:");
                for (final ICliComputer.RemoteHost host : hosts) {
                    /*
                     * Name what the player can actually type: the host name, the machine's own name
                     * and its node id all address it.
                     */
                    final StringBuilder detail = new StringBuilder();
                    if (!host.name().isEmpty() && !host.name().equalsIgnoreCase(host.hostname())) {
                        detail.append('"').append(host.name()).append("\"  ");
                    }
                    detail.append("node ").append(host.nodeId()).append("  ").append(host.type());
                    if (!host.os().isEmpty()) {
                        detail.append("  ").append(host.os());
                    }
                    if (!host.running()) {
                        detail.append("  (offline)");
                    }
                    ctx.out().row("  " + host.hostname(), detail.toString());
                }
                ctx.out().line("");
                ctx.out().line("ssh <host name | machine name | node | os> to connect; exit to come back.");
                return;
            }
            final ICliComputer.OpResult result = computer.sshConnect(ctx.arg(0));
            if (result.ok()) {
                ctx.out().ok(result.message());
            } else {
                ctx.out().error(result.message());
            }
        }
    }
}
