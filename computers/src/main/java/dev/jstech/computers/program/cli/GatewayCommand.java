/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gateway.GatewayManager;
import dev.jstech.computers.gateway.GatewayPermissions;
import dev.jstech.computers.operation.payload.GatewayManagerActionPayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.Detail;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireComputer;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireGateway;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireLog;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The shell's way into the Network Gateways on this computer's ports, for players at a prompt or on a
 * system with no desktop: the same five views and the same actions as the Gateway Manager.
 *
 * <p>Its listings are tables whose columns are padded by counting characters, so a word that stands in a padded
 * column goes in in the machine's language, the one it keeps what it writes down in; everything else is the
 * reader's.
 */
@TextHolder
public final class GatewayCommand implements ICliCommand {

    /** The word it is typed as, which is also what its complaints open with. */
    private static final String NAME = "gateway";

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.gateway.summary",
            "manage the Network Gateways on this computer's ports (the bridge to ComputerCraft)");
    private static final TextKey USAGE = TextKey.of("jsc.cli.gateway.usage",
            "list | <name> [status|perms|computers|log] | <name> rename <new> | <name> identify"
                    + " | <name> set read|operations on|off"
                    + " | <name> set priority low|medium|high | <name> set cap 4|8|16 | <name> clear | <name> test");
    private static final TextKey SET_SWITCH_USAGE =
            TextKey.of("jsc.cli.gateway.set_switch_usage", "<name> set %s on|off");
    private static final TextKey SET_PRIORITY_USAGE =
            TextKey.of("jsc.cli.gateway.set_priority_usage", "<name> set priority low|medium|high");
    private static final TextKey SET_CAP_USAGE = TextKey.of("jsc.cli.gateway.set_cap_usage", "<name> set cap 4|8|16");
    private static final TextKey SET_USAGE =
            TextKey.of("jsc.cli.gateway.set_usage", "<name> set read|operations|priority|cap ...");

    private static final TextKey NOT_ON_COMPUTER =
            TextKey.of("jsc.cli.gateway.not_on_computer", "this terminal is not on a computer");
    private static final TextKey NO_GATEWAY =
            TextKey.of("jsc.cli.gateway.no_gateway", "no gateway named %s on this computer");
    private static final TextKey NONE = TextKey.of("jsc.cli.gateway.none", "no gateways on this computer's ports");
    private static final TextKey NO_CC = TextKey.of("jsc.cli.gateway.no_cc",
            "CC: Tweaked is not installed: the ComputerCraft side never comes up");
    private static final TextKey DONE = TextKey.of("jsc.cli.gateway.done", "done");

    /* The headings line up with the columns written under them, so a translation keeps their spacing. */
    private static final TextKey LIST_HEADER =
            TextKey.of("jsc.cli.gateway.list_header", "NAME             LINK   CC     WHERE");
    private static final TextKey COMPUTERS_HEADER = TextKey.of("jsc.cli.gateway.computers_header",
            "ID    LABEL            STATE  AGENT      LAST SEEN");
    private static final TextKey LOG_HEADER = TextKey.of("jsc.cli.gateway.log_header",
            "WHEN      WHO            WHAT                           RESULT");

    private static final TextKey UP = TextKey.of("jsc.cli.gateway.up", "up");
    private static final TextKey DOWN = TextKey.of("jsc.cli.gateway.down", "down");
    private static final TextKey ON = TextKey.of("jsc.cli.gateway.on", "on");
    private static final TextKey OFF = TextKey.of("jsc.cli.gateway.off", "off");
    private static final TextKey ONLINE = TextKey.of("jsc.cli.gateway.online", "online");
    private static final TextKey OFFLINE = TextKey.of("jsc.cli.gateway.offline", "offline");
    private static final TextKey NO_LABEL = TextKey.of("jsc.cli.gateway.no_label", "(no label)");
    private static final TextKey ANSWERING = TextKey.of("jsc.cli.gateway.answering", "answering");
    private static final TextKey NO_AGENT = TextKey.of("jsc.cli.gateway.no_agent", "none");

    private static final TextKey THIS_SIDE = TextKey.of("jsc.cli.gateway.this_side", "%s · this side");
    private static final TextKey LINKED_TO = TextKey.of("jsc.cli.gateway.linked_to", "linked to %s, %s");
    private static final TextKey NETWORK = TextKey.of("jsc.cli.gateway.network",
            "network: %s types, %s servers, mainframe %s");
    private static final TextKey BUDGET = TextKey.of("jsc.cli.gateway.budget", "budget: %s%% of this computer's tick");
    private static final TextKey CC_SIDE = TextKey.of("jsc.cli.gateway.cc_side", "%s · ComputerCraft side");
    private static final TextKey CC_MISSING = TextKey.of("jsc.cli.gateway.cc_missing", "CC: Tweaked is not installed");
    private static final TextKey CC_VERSION = TextKey.of("jsc.cli.gateway.cc_version", "CC: Tweaked %s, %s");
    private static final TextKey REACHABLE = TextKey.of("jsc.cli.gateway.reachable", "reachable");
    private static final TextKey NOTHING_ATTACHED =
            TextKey.of("jsc.cli.gateway.nothing_attached", "nothing attached");
    private static final TextKey WIRED = TextKey.of("jsc.cli.gateway.wired",
            "wired network: %s computers, %s devices");
    private static final TextKey SERVED = TextKey.of("jsc.cli.gateway.served",
            "served this minute: %s calls, %s operations");
    private static final TextKey SEEN_AS = TextKey.of("jsc.cli.gateway.seen_as", "seen from CC as %s");
    private static final TextKey BUFFER = TextKey.of("jsc.cli.gateway.buffer", "buffer: %s of %s slots in use");
    private static final TextKey RECENT = TextKey.of("jsc.cli.gateway.recent", "recent");

    private static final TextKey PERMISSIONS = TextKey.of("jsc.cli.gateway.permissions", "%s · permissions");
    private static final TextKey READ = TextKey.of("jsc.cli.gateway.read", "read the network: %s");
    private static final TextKey OPERATIONS = TextKey.of("jsc.cli.gateway.operations", "operations: %s");
    private static final TextKey CEILING = TextKey.of("jsc.cli.gateway.ceiling", "priority ceiling: %s");
    private static final TextKey CALLS = TextKey.of("jsc.cli.gateway.calls", "calls a tick: %s");

    private static final TextKey NO_COMPUTER =
            TextKey.of("jsc.cli.gateway.no_computer", "no ComputerCraft computer attached to %s");
    private static final TextKey NOTHING_DONE = TextKey.of("jsc.cli.gateway.nothing_done", "%s has done nothing yet");

    /**
     * On any computer with ports for peripherals, which is every computer a gateway can be hung off.
     *
     * <p>Not on having one: a gateway is plugged in and unplugged while the machine runs, and a player who has just
     * bought one would be told the verb does not exist rather than that nothing is linked yet. So the command is
     * there and says what is there, which is the answer that teaches.
     */
    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().needing(CommandScope.Need.PORTS);
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
    public void run(final CliContext ctx) {
        if (!(ctx.computer().hostBlock() instanceof BlockEntity host) || !(host.getLevel() instanceof ServerLevel level)) {
            ctx.out().error(CliTexts.SAID_BY.with(NAME, NOT_ON_COMPUTER));
            return;
        }
        final String first = ctx.arg(0).toLowerCase(Locale.ROOT);
        if (first.isEmpty() || first.equals("list") || first.equals("ls")) {
            list(ctx, level, host);
            return;
        }
        final NetworkGatewayBlockEntity gateway = find(level, host, first);
        if (gateway == null) {
            ctx.out().error(CliTexts.SAID_BY.with(NAME, NO_GATEWAY.with(first)));
            return;
        }
        final long pos = gateway.getBlockPos().asLong();
        final String verb = ctx.arg(1).toLowerCase(Locale.ROOT);
        switch (verb) {
            case "", "status" -> status(ctx, level, host, pos);
            case "perms", "permissions" -> perms(ctx, level, host, pos);
            case "computers" -> computers(ctx, level, host, pos);
            case "log" -> log(ctx, level, host, pos);
            case "rename" -> act(ctx, level, host, pos, GatewayManagerActionPayload.ACTION_RENAME, 0, ctx.rest(2));
            case "identify" -> act(ctx, level, host, pos, GatewayManagerActionPayload.ACTION_IDENTIFY, 0, "");
            case "clear" -> act(ctx, level, host, pos, GatewayManagerActionPayload.ACTION_CLEAR_BUFFER, 0, "");
            case "test" -> act(ctx, level, host, pos, GatewayManagerActionPayload.ACTION_TEST_EVENT, 0, "");
            case "set" -> set(ctx, level, host, pos);
            default -> ctx.out().error(CliTexts.USAGE.with(NAME, usage()));
        }
    }

    private static NetworkGatewayBlockEntity find(final ServerLevel level, final BlockEntity host, final String name) {
        for (final NetworkGatewayBlockEntity g : GatewayManager.gatewaysOf(level, host)) {
            if (g.name().equalsIgnoreCase(name)) {
                return g;
            }
        }
        return null;
    }

    private static GatewayManagerStatePayload state(final ServerLevel level, final BlockEntity host, final long pos) {
        return GatewayManager.state(level, host, pos, "");
    }

    private static void list(final CliContext ctx, final ServerLevel level, final BlockEntity host) {
        final GatewayManagerStatePayload state = state(level, host, 0L);
        if (state.gateways().isEmpty()) {
            ctx.out().dim(NONE);
            return;
        }
        ctx.out().header(LIST_HEADER);
        for (final WireGateway g : state.gateways()) {
            ctx.out().line(Text.literal(String.format(Locale.ROOT, "%-16s %-6s %-6s %s", g.name(),
                    (g.linked() ? UP : DOWN).text().english(), (g.ccLinked() ? UP : DOWN).text().english(),
                    g.where())));
        }
        if (!state.head().ccInstalled()) {
            ctx.out().dim(NO_CC);
        }
    }

    private static void status(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final GatewayManagerStatePayload state = state(level, host, pos);
        final Detail d = state.detail();
        ctx.out().header(THIS_SIDE.with(d.name()));
        indented(ctx, LINKED_TO.with(state.head().hostName(), d.link()), CliStyle.PLAIN);
        indented(ctx, NETWORK.with(d.types(), d.servers(), d.mainframeOnline() ? ONLINE : OFFLINE), CliStyle.PLAIN);
        indented(ctx, BUDGET.with(d.budgetPermille() / 10), CliStyle.PLAIN);
        ctx.out().header(CC_SIDE.with(d.name()));
        if (!state.head().ccInstalled()) {
            indented(ctx, CC_MISSING.text(), CliStyle.DIM);
        } else {
            indented(ctx, CC_VERSION.with(state.head().ccVersion(), d.ccOnline() ? REACHABLE : NOTHING_ATTACHED),
                    CliStyle.PLAIN);
            indented(ctx, WIRED.with(d.wiredComputers(), d.wiredDevices()), CliStyle.PLAIN);
            indented(ctx, SERVED.with(d.calls(), d.operations()), CliStyle.PLAIN);
        }
        indented(ctx, SEEN_AS.with(d.peripheralName()), CliStyle.PLAIN);
        int used = 0;
        for (final var stack : d.buffer()) {
            if (!stack.isEmpty()) {
                used++;
            }
        }
        indented(ctx, BUFFER.with(used, GatewayManagerStatePayload.BUFFER_SLOTS), CliStyle.PLAIN);
        if (!d.recent().isEmpty()) {
            ctx.out().header(RECENT);
            for (final WireLog row : d.recent()) {
                ctx.out().line("  " + row.when() + "  " + row.who() + "  " + row.what() + "  " + row.result());
            }
        }
    }

    private static void perms(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final Detail d = state(level, host, pos).detail();
        ctx.out().header(PERMISSIONS.with(d.name()));
        indented(ctx, READ.with(d.read() ? ON : OFF), CliStyle.PLAIN);
        indented(ctx, OPERATIONS.with(d.operationsAllowed() ? ON : OFF), CliStyle.PLAIN);
        // The ceiling is written the way it is typed after "set priority", so it is the command's own word.
        indented(ctx, CEILING.with(GatewayPermissions.ceilingAt(d.ceiling()).name().toLowerCase(Locale.ROOT)),
                CliStyle.PLAIN);
        indented(ctx, CALLS.with(GatewayPermissions.capAt(d.cap())), CliStyle.PLAIN);
    }

    private static void computers(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final Detail d = state(level, host, pos).detail();
        final List<WireComputer> rows = d.computers();
        if (rows.isEmpty()) {
            ctx.out().dim(NO_COMPUTER.with(d.name()));
            return;
        }
        ctx.out().header(COMPUTERS_HEADER);
        for (final WireComputer c : rows) {
            ctx.out().line(Text.literal(String.format(Locale.ROOT, "%-5d %-16s %-6s %-10s %s", c.id(),
                    c.label().isEmpty() ? NO_LABEL.text().english() : c.label(), (c.on() ? ON : OFF).text().english(),
                    (c.agent() ? ANSWERING : NO_AGENT).text().english(), c.lastSeen())));
        }
    }

    private static void log(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final Detail d = state(level, host, pos).detail();
        if (d.log().isEmpty()) {
            ctx.out().dim(NOTHING_DONE.with(d.name()));
            return;
        }
        ctx.out().header(LOG_HEADER);
        /*
         * Oldest first here, which is the other way round from the table in the manager: a terminal is
         * read from the bottom, so the newest line belongs against the prompt, where the eye already is.
         */
        final List<WireLog> rows = d.log();
        for (int i = rows.size() - 1; i >= 0; i--) {
            final WireLog row = rows.get(i);
            ctx.out().line(Text.literal(String.format(Locale.ROOT, "%-9s %-14s %-30s %s", row.when(), row.who(),
                    row.what(), row.result())));
        }
    }

    private static void set(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final String what = ctx.arg(2).toLowerCase(Locale.ROOT);
        final String to = ctx.arg(3).toLowerCase(Locale.ROOT);
        switch (what) {
            case "read", "operations" -> {
                if (!to.equals("on") && !to.equals("off")) {
                    ctx.out().error(CliTexts.USAGE.with(NAME, SET_SWITCH_USAGE.with(what)));
                    return;
                }
                act(ctx, level, host, pos, what.equals("read") ? GatewayManagerActionPayload.ACTION_SET_READ
                        : GatewayManagerActionPayload.ACTION_SET_OPERATIONS, to.equals("on") ? 1 : 0, "");
            }
            case "priority" -> {
                final int index = switch (to) {
                    case "low" -> 0;
                    case "medium", "med" -> 1;
                    case "high" -> 2;
                    default -> -1;
                };
                if (index < 0) {
                    ctx.out().error(CliTexts.USAGE.with(NAME, SET_PRIORITY_USAGE));
                    return;
                }
                act(ctx, level, host, pos, GatewayManagerActionPayload.ACTION_SET_CEILING, index, "");
            }
            case "cap" -> {
                int index = -1;
                for (int i = 0; i < GatewayPermissions.CAPS.length; i++) {
                    if (to.equals(String.valueOf(GatewayPermissions.CAPS[i]))) {
                        index = i;
                    }
                }
                if (index < 0) {
                    ctx.out().error(CliTexts.USAGE.with(NAME, SET_CAP_USAGE));
                    return;
                }
                act(ctx, level, host, pos, GatewayManagerActionPayload.ACTION_SET_CAP, index, "");
            }
            default -> ctx.out().error(CliTexts.USAGE.with(NAME, SET_USAGE));
        }
    }

    private static void act(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos,
                            final int action, final int value, final String text) {
        final String said = GatewayManager.act(level, host, pos, action, value, text);
        if (said.isEmpty()) {
            ctx.out().dim(DONE);
        } else {
            ctx.out().ok(said);
        }
    }

    /** A line set in under its heading, the indent kept out of the sentence so a translation cannot lose it. */
    private static void indented(final CliContext ctx, final Text text, final CliStyle style) {
        ctx.out().line(CliLine.build().add("  ", style).add(text, style).done());
    }
}
