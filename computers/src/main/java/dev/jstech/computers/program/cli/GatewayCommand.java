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
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The shell's way into the Network Gateways on this computer's ports, for players at a prompt or on a
 * system with no desktop: the same five views and the same actions as the Gateway Manager.
 */
public final class GatewayCommand implements ICliCommand {

    @Override
    public String name() {
        return "gateway";
    }

    @Override
    public String summary() {
        return "manage the Network Gateways on this computer's ports (the bridge to ComputerCraft)";
    }

    @Override
    public String usage() {
        return "list | <name> [status|perms|computers|log] | <name> rename <new> | <name> identify"
                + " | <name> set read|operations on|off"
                + " | <name> set priority low|medium|high | <name> set cap 4|8|16 | <name> clear | <name> test";
    }

    @Override
    public void run(final CliContext ctx) {
        if (!(ctx.computer().hostBlock() instanceof BlockEntity host) || !(host.getLevel() instanceof ServerLevel level)) {
            ctx.out().error("gateway: this terminal is not on a computer");
            return;
        }
        final String first = ctx.arg(0).toLowerCase(Locale.ROOT);
        if (first.isEmpty() || first.equals("list") || first.equals("ls")) {
            list(ctx, level, host);
            return;
        }
        final NetworkGatewayBlockEntity gateway = find(level, host, first);
        if (gateway == null) {
            ctx.out().error("gateway: no gateway named " + first + " on this computer");
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
            default -> ctx.out().error("usage: gateway " + usage());
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
            ctx.out().dim("no gateways on this computer's ports");
            return;
        }
        ctx.out().header(String.format(Locale.ROOT, "%-16s %-6s %-6s %s", "NAME", "LINK", "CC", "WHERE"));
        for (final WireGateway g : state.gateways()) {
            ctx.out().line(String.format(Locale.ROOT, "%-16s %-6s %-6s %s", g.name(), g.linked() ? "up" : "down",
                    g.ccLinked() ? "up" : "down", g.where()));
        }
        if (!state.head().ccInstalled()) {
            ctx.out().dim("CC: Tweaked is not installed: the ComputerCraft side never comes up");
        }
    }

    private static void status(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final GatewayManagerStatePayload state = state(level, host, pos);
        final Detail d = state.detail();
        ctx.out().header(d.name() + " · this side");
        ctx.out().line("  linked to " + state.head().hostName() + ", " + d.link());
        ctx.out().line("  network: " + d.types() + " types, " + d.servers() + " servers, mainframe "
                + (d.mainframeOnline() ? "online" : "offline"));
        ctx.out().line("  budget: " + (d.budgetPermille() / 10) + "% of this computer's tick");
        ctx.out().header(d.name() + " · ComputerCraft side");
        if (!state.head().ccInstalled()) {
            ctx.out().dim("  CC: Tweaked is not installed");
        } else {
            ctx.out().line("  CC: Tweaked " + state.head().ccVersion() + ", " + (d.ccOnline() ? "reachable" : "nothing attached"));
            ctx.out().line("  wired network: " + d.wiredComputers() + " computers, " + d.wiredDevices() + " devices");
            ctx.out().line("  served this minute: " + d.calls() + " calls, " + d.operations() + " operations");
        }
        ctx.out().line("  seen from CC as " + d.peripheralName());
        int used = 0;
        for (final var stack : d.buffer()) {
            if (!stack.isEmpty()) {
                used++;
            }
        }
        ctx.out().line("  buffer: " + used + " of " + GatewayManagerStatePayload.BUFFER_SLOTS + " slots in use");
        if (!d.recent().isEmpty()) {
            ctx.out().header("recent");
            for (final WireLog row : d.recent()) {
                ctx.out().line("  " + row.when() + "  " + row.who() + "  " + row.what() + "  " + row.result());
            }
        }
    }

    private static void perms(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final Detail d = state(level, host, pos).detail();
        ctx.out().header(d.name() + " · permissions");
        ctx.out().line("  read the network: " + (d.read() ? "on" : "off"));
        ctx.out().line("  operations: " + (d.operationsAllowed() ? "on" : "off"));
        ctx.out().line("  priority ceiling: " + GatewayPermissions.ceilingAt(d.ceiling()).name().toLowerCase(Locale.ROOT));
        ctx.out().line("  calls a tick: " + GatewayPermissions.capAt(d.cap()));
    }

    private static void computers(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final Detail d = state(level, host, pos).detail();
        final List<WireComputer> rows = d.computers();
        if (rows.isEmpty()) {
            ctx.out().dim("no ComputerCraft computer attached to " + d.name());
            return;
        }
        ctx.out().header(String.format(Locale.ROOT, "%-5s %-16s %-6s %-10s %s", "ID", "LABEL", "STATE", "AGENT", "LAST SEEN"));
        for (final WireComputer c : rows) {
            ctx.out().line(String.format(Locale.ROOT, "%-5d %-16s %-6s %-10s %s", c.id(),
                    c.label().isEmpty() ? "(no label)" : c.label(), c.on() ? "on" : "off",
                    c.agent() ? "answering" : "none", c.lastSeen()));
        }
    }

    private static void log(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final Detail d = state(level, host, pos).detail();
        if (d.log().isEmpty()) {
            ctx.out().dim(d.name() + " has done nothing yet");
            return;
        }
        ctx.out().header(String.format(Locale.ROOT, "%-9s %-14s %-30s %s", "WHEN", "WHO", "WHAT", "RESULT"));
        for (final WireLog row : d.log()) {
            ctx.out().line(String.format(Locale.ROOT, "%-9s %-14s %-30s %s", row.when(), row.who(), row.what(), row.result()));
        }
    }

    private static void set(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos) {
        final String what = ctx.arg(2).toLowerCase(Locale.ROOT);
        final String to = ctx.arg(3).toLowerCase(Locale.ROOT);
        switch (what) {
            case "read", "operations" -> {
                if (!to.equals("on") && !to.equals("off")) {
                    ctx.out().error("usage: gateway <name> set " + what + " on|off");
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
                    ctx.out().error("usage: gateway <name> set priority low|medium|high");
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
                    ctx.out().error("usage: gateway <name> set cap 4|8|16");
                    return;
                }
                act(ctx, level, host, pos, GatewayManagerActionPayload.ACTION_SET_CAP, index, "");
            }
            default -> ctx.out().error("usage: gateway <name> set read|operations|priority|cap ...");
        }
    }

    private static void act(final CliContext ctx, final ServerLevel level, final BlockEntity host, final long pos,
                            final int action, final int value, final String text) {
        final String said = GatewayManager.act(level, host, pos, action, value, text);
        if (said.isEmpty()) {
            ctx.out().dim("done");
        } else {
            ctx.out().ok(said);
        }
    }
}
