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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;

/**
 * The commands that reach the data network: what it holds, what it is doing and the statements sent to it.
 *
 * <p>Moved here from BuiltinCommands, which keeps the lists that say which system gets which command.
 */
@TextHolder
final class NetworkCommands {

    /** What every one of these needs: a data network under the machine, on any system. */
    private static final CommandScope ON_THE_NETWORK =
            CommandScope.everywhere().needing(CommandScope.Need.NETWORK);

    /** Said alike by every command here that finds the machine on no network at all. */
    private static final TextKey NOT_ON_NETWORK = TextKey.of("jsc.cli.network.not_on_network", "not on a network");

    private NetworkCommands() {
    }

    @TextHolder
    static final class Net implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.network.net.summary", "summarise the network");
        private static final TextKey NETWORK = TextKey.of("jsc.cli.network.net.network", "network");
        private static final TextKey MAINFRAME = TextKey.of("jsc.cli.network.net.mainframe", "mainframe");
        private static final TextKey PRESENT = TextKey.of("jsc.cli.network.net.present", "present");
        private static final TextKey MISSING = TextKey.of("jsc.cli.network.net.missing", "MISSING");
        private static final TextKey SERVERS = TextKey.of("jsc.cli.network.net.servers", "servers");
        private static final TextKey COMPUTERS = TextKey.of("jsc.cli.network.net.computers", "computers");
        private static final TextKey SUBFRAMES = TextKey.of("jsc.cli.network.net.subframes", "subframes");
        private static final TextKey INDEXED_TYPES =
                TextKey.of("jsc.cli.network.net.indexed_types", "indexed types");

        /** On every machine, since saying "not on a network" is half of what it is for. */
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "net";
        }

        @Override public CommandGroup group() {
            return CommandGroup.NETWORK;
        }

        @Override public List<String> aliases() {
            return List.of("network");
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.NetSummary n = ctx.computer().network();
            if (!n.linked()) {
                ctx.out().error(NOT_ON_NETWORK);
                return;
            }
            ctx.out().row(NETWORK, ctx.computer().networkId());
            ctx.out().row(MAINFRAME.text(), n.mainframePresent() ? PRESENT.text() : MISSING.text());
            ctx.out().row(SERVERS, CliText.group(n.servers()));
            ctx.out().row(COMPUTERS, CliText.group(n.personalComputers()));
            ctx.out().row(SUBFRAMES, CliText.group(n.subframes()));
            ctx.out().row(INDEXED_TYPES, CliText.group(n.indexedTypes()));
        }
    }

    @TextHolder
    static final class Operation implements ICliCommand {

        private static final int QUERY_LIMIT = 64;

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.network.iql.summary", "run an IQL statement on the network");
        private static final TextKey USAGE = TextKey.of("jsc.cli.network.iql.usage", "<statement>");
        private static final TextKey USAGE_HINT = TextKey.of("jsc.cli.network.iql.usage_hint",
                "usage: iql <statement>   e.g. iql SELECT 64 Cobblestone");
        private static final TextKey SYNTAX = TextKey.of("jsc.cli.network.iql.syntax", "syntax: %s");
        private static final TextKey NO_ROWS = TextKey.of("jsc.cli.network.iql.no_rows", "no rows");

        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        /** The language's own name. The words it was typed as before still reach it, as a shell's old names do. */
        @Override public String name() {
            return "iql";
        }

        @Override public CommandGroup group() {
            return CommandGroup.NETWORK;
        }

        @Override public List<String> aliases() {
            return List.of("operation", "op", "sql");
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(USAGE_HINT);
                return;
            }
            final IqlParseResult parsed = IqlParser.tryParse(ctx.rest(0));
            if (!parsed.ok()) {
                ctx.out().error(SYNTAX.with(parsed.error()));
                return;
            }
            final IqlOperation op = parsed.operation();
            if (op.verb() == IqlVerb.QUERY || op.verb() == IqlVerb.COUNT) {
                if (!ctx.computer().onNetwork()) {
                    ctx.out().error(NOT_ON_NETWORK);
                    return;
                }
                final int limit = op.limit() > 0 ? op.limit() : QUERY_LIMIT;
                final List<ICliComputer.StoredItem> items = ctx.computer().queryObject(op.item(),
                        op.where(), "", limit);
                if (items.isEmpty()) {
                    ctx.out().dim(NO_ROWS);
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
    @TextHolder
    static final class Ssh implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.network.ssh.summary", "open a shell on another computer of this network");
        private static final TextKey USAGE = TextKey.of("jsc.cli.network.ssh.usage", "[hostname]");
        private static final TextKey NO_HOSTS =
                TextKey.of("jsc.cli.network.ssh.no_hosts", "no other computers reachable on this network");
        private static final TextKey REACHABLE = TextKey.of("jsc.cli.network.ssh.reachable", "Reachable hosts:");
        private static final TextKey NODE = TextKey.of("jsc.cli.network.ssh.node", "node %s");
        private static final TextKey OFFLINE = TextKey.of("jsc.cli.network.ssh.offline", "(offline)");
        private static final TextKey HOW = TextKey.of("jsc.cli.network.ssh.how",
                "ssh <host name | machine name | node | os> to connect; exit to come back.");

        @Override public CommandScope scope() {
            return ON_THE_NETWORK;
        }

        @Override public String name() {
            return "ssh";
        }

        @Override public CommandGroup group() {
            return CommandGroup.NETWORK;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer computer = ctx.computer();
            if (!ctx.hasArgs()) {
                final List<ICliComputer.RemoteHost> hosts = computer.reachableHosts();
                if (hosts.isEmpty()) {
                    ctx.out().error(CliTexts.SAID_BY.with(name(), NO_HOSTS));
                    return;
                }
                ctx.out().line(REACHABLE);
                for (final ICliComputer.RemoteHost host : hosts) {
                    ctx.out().line(hostRow(host, ctx.out().width()));
                }
                ctx.out().blank();
                ctx.out().line(HOW);
                return;
            }
            final ICliComputer.OpResult result = computer.sshConnect(ctx.arg(0));
            if (result.ok()) {
                ctx.out().ok(result.message());
            } else {
                ctx.out().error(result.message());
            }
        }

        /**
         * One reachable machine, its host name on the left and the rest pushed to the right edge.
         *
         * <p>It names what the player can actually type: the host name, the machine's own name and its node id all
         * address it. Put together a run at a time rather than as one sentence, because only two of its words are
         * words to translate and the rest are the machine's own names.
         */
        private static CliLine hostRow(final ICliComputer.RemoteHost host, final int width) {
            final List<CliSpan> spans = new ArrayList<>();
            spans.add(CliSpan.plain("  " + host.hostname()));
            spans.add(CliSpan.fill(width, true));
            if (!host.name().isEmpty() && !host.name().equalsIgnoreCase(host.hostname())) {
                spans.add(CliSpan.plain("\"" + host.name() + "\"  "));
            }
            spans.add(CliSpan.plain(NODE.with(host.nodeId())));
            spans.add(CliSpan.plain("  " + host.type()));
            if (!host.os().isEmpty()) {
                spans.add(CliSpan.plain("  " + host.os()));
            }
            if (!host.running()) {
                spans.add(CliSpan.plain("  "));
                spans.add(CliSpan.plain(OFFLINE.text()));
            }
            return new CliLine(spans);
        }
    }
}
