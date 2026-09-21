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

    static final class Operation implements ICliCommand {

        private static final int QUERY_LIMIT = 64;

        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        /** The language's own name. The words it was typed as before still reach it, as a shell's old names do. */
        @Override public String name() {
            return "iql";
        }

        @Override public List<String> aliases() {
            return List.of("operation", "op", "sql");
        }

        @Override public String summary() {
            return "run an IQL statement on the network";
        }

        @Override public String usage() {
            return "<statement>";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: iql <statement>   e.g. iql SELECT 64 Cobblestone");
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
