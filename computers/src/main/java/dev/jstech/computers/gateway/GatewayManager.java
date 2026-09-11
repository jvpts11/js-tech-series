/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.operation.payload.GatewayManagerActionPayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.Detail;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.Head;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireComputer;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireGateway;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireLog;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireShare;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.peripheral.IPeripheralOwner;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What the Gateway Manager and the {@code gateway} command do on the server: gather the state of the
 * Gateways on a host computer's ports, and carry out an action on one of them. One implementation for
 * the program and the shell, so they never disagree.
 */
public final class GatewayManager {

    /** How many of the log's lines the Status tab shows under "recent". */
    public static final int RECENT = 4;
    private static final int CC_ID_NONE = -1;

    private GatewayManager() {
    }

    /** The Gateways on {@code host}'s ports; none when the block is not a computer with ports. */
    public static List<NetworkGatewayBlockEntity> gatewaysOf(final ServerLevel level, @Nullable final BlockEntity host) {
        return host instanceof IPeripheralOwner owner ? NetworkGateways.linkedTo(level, owner) : List.of();
    }

    /** The name a host signs the Gateway's log with. */
    public static String hostNameOf(@Nullable final BlockEntity host) {
        if (host instanceof IOsHost os && os.console() != null && !os.console().computerName().isEmpty()) {
            return os.console().computerName();
        }
        return "host";
    }

    /** Where a Gateway stands and how it is linked, for the rail. */
    public static String where(final NetworkGatewayBlockEntity gateway) {
        final BlockPos p = gateway.getBlockPos();
        return "at " + p.getX() + ", " + p.getY() + ", " + p.getZ() + " · " + gateway.linkKind();
    }

    /** The whole state, with {@code selected} (a Gateway's position) in detail and {@code status} to show. */
    public static GatewayManagerStatePayload state(final ServerLevel level, @Nullable final BlockEntity host,
                                                   final long selected, final String status) {
        final List<NetworkGatewayBlockEntity> gateways = gatewaysOf(level, host);
        final long now = level.getGameTime();
        final List<WireGateway> rail = new ArrayList<>(gateways.size());
        int calls = 0;
        int reachable = 0;
        NetworkGatewayBlockEntity chosen = null;
        for (final NetworkGatewayBlockEntity g : gateways) {
            if (rail.size() >= GatewayManagerStatePayload.MAX_GATEWAYS) {
                break;
            }
            rail.add(new WireGateway(g.getBlockPos().asLong(), g.name(), where(g), g.online(), g.ccOnline()));
            calls += g.stats().lastMinute(GatewayStats.Kind.CALL, now);
            if (g.ccOnline()) {
                reachable++;
            }
            if (g.getBlockPos().asLong() == selected) {
                chosen = g;
            }
        }
        final Head head = new Head(hostNameOf(host), ComputerCraftIntegration.isLoaded(),
                ComputerCraftIntegration.installedVersion(), calls, reachable);
        final Detail detail = chosen == null ? Detail.none() : detail(level, host, chosen, now);
        return new GatewayManagerStatePayload(head, rail, detail, status);
    }

    private static Detail detail(final ServerLevel level, @Nullable final BlockEntity host,
                                 final NetworkGatewayBlockEntity g, final long now) {
        final ServerCliComputer shell = host instanceof IComputerTerminalHost terminal
                ? new ServerCliComputer(terminal, level) : null;
        final ICliComputer.NetSummary net = shell == null ? null : shell.network();
        final MainframeBlockEntity mainframe = shell == null ? null : shell.mainframe();
        final IGatewayBridge bridge = g.bridge();
        final List<ItemStack> buffer = new ArrayList<>(NetworkGatewayBlockEntity.BUFFER_SLOTS);
        final ItemStackHandler slots = g.buffer();
        for (int i = 0; i < NetworkGatewayBlockEntity.BUFFER_SLOTS; i++) {
            buffer.add(slots.getStackInSlot(i).copy());
        }
        final List<WireLog> log = new ArrayList<>();
        for (final GatewayLog.Entry e : g.log().entries()) {
            if (log.size() >= GatewayManagerStatePayload.MAX_ROWS) {
                break;
            }
            log.add(new WireLog(GatewayLog.clock(e.dayTime()), e.who(), e.what(), e.result(), e.tone().ordinal()));
        }
        final List<WireLog> recent = new ArrayList<>(log.subList(0, Math.min(RECENT, log.size())));
        final List<WireComputer> computers = new ArrayList<>();
        for (final NetworkGatewayBlockEntity.AttachedComputer c : g.attachedComputers()) {
            if (computers.size() >= GatewayManagerStatePayload.MAX_ROWS) {
                break;
            }
            computers.add(new WireComputer(c.id(), "", true, false, ago(now, c.lastSeen())));
        }
        final GatewayPermissions perms = g.permissions();
        return new Detail(g.getBlockPos().asLong(), g.name(), g.linkKind(),
                net == null ? 0 : net.indexedTypes(), net == null ? 0 : net.servers(),
                mainframe != null && mainframe.isRunning(), 0,
                g.ccOnline(), bridge == null ? 0 : bridge.computersOnWire(), bridge == null ? 0 : bridge.devicesOnWire(),
                0, computers.size(),
                g.stats().lastMinute(GatewayStats.Kind.CALL, now),
                g.stats().lastMinute(GatewayStats.Kind.OPERATION, now),
                g.stats().lastMinute(GatewayStats.Kind.FILE, now),
                g.peripheralName(), CC_ID_NONE, buffer, recent,
                perms.read(), perms.operations(), perms.filesIndex(), perms.ceilingIndex(), perms.capIndex(),
                computers, shares(shell, perms), log);
    }

    /** Every folder the network's computers share, as ComputerCraft would see it through this Gateway. */
    public static List<WireShare> shares(@Nullable final ServerCliComputer shell, final GatewayPermissions perms) {
        final List<WireShare> out = new ArrayList<>();
        if (shell == null) {
            return out;
        }
        final String own = shell.hostname();
        for (final ICliComputer.ShareInfo share : shell.shares()) {
            out.add(shareRow(own, share, perms));
        }
        for (final ICliComputer.NetworkShare share : shell.networkShares()) {
            if (out.size() >= GatewayManagerStatePayload.MAX_ROWS) {
                break;
            }
            if (!share.hostname().equalsIgnoreCase(own)) {
                out.add(shareRow(share.hostname(), share.share(), perms));
            }
        }
        return out;
    }

    private static WireShare shareRow(final String hostname, final ICliComputer.ShareInfo share,
                                      final GatewayPermissions perms) {
        final String mode = perms.files() == GatewayPermissions.FileAccess.OFF ? "off"
                : share.writable() && perms.allowsWrite() ? "read & write" : "read";
        return new WireShare(hostname, share.path(), "/jsc/" + hostname.toLowerCase(Locale.ROOT) + "/" + share.name(), mode);
    }

    /** "2 s ago", "4 min ago", for the computers table. */
    public static String ago(final long now, final long then) {
        final long seconds = Math.max(0L, now - then) / 20L;
        if (seconds < 60L) {
            return seconds + " s ago";
        }
        final long minutes = seconds / 60L;
        return minutes < 60L ? minutes + " min ago" : (minutes / 60L) + " h ago";
    }

    /**
     * Carries out one action on the Gateway at {@code gatewayPos}, which must be one of {@code host}'s, and
     * says what happened for the status line.
     */
    public static String act(final ServerLevel level, @Nullable final BlockEntity host, final long gatewayPos,
                             final int action, final int value, final String text) {
        if (action == GatewayManagerActionPayload.ACTION_REFRESH) {
            return "";
        }
        if (!(host instanceof IPeripheralOwner owner)) {
            return "this computer has no peripheral ports";
        }
        final NetworkGatewayBlockEntity g = NetworkGateways.at(level, owner, gatewayPos);
        if (g == null) {
            return "select a gateway first";
        }
        final String by = hostNameOf(host);
        final GatewayPermissions perms = g.permissions();
        return switch (action) {
            case GatewayManagerActionPayload.ACTION_RENAME -> g.rename(text, by);
            case GatewayManagerActionPayload.ACTION_IDENTIFY -> {
                g.identify(by);
                yield g.name() + " is blinking";
            }
            case GatewayManagerActionPayload.ACTION_SET_READ -> {
                g.setPermissions(perms.withRead(value != 0), by, "set read " + onOff(value != 0));
                yield "reading the network " + (value != 0 ? "allowed" : "denied");
            }
            case GatewayManagerActionPayload.ACTION_SET_OPERATIONS -> {
                g.setPermissions(perms.withOperations(value != 0), by, "set operations " + onOff(value != 0));
                yield "operations " + (value != 0 ? "allowed" : "denied");
            }
            case GatewayManagerActionPayload.ACTION_SET_FILES -> {
                final GatewayPermissions.FileAccess files = GatewayPermissions.FileAccess.at(value);
                g.setPermissions(perms.withFiles(files), by, "set files " + files.label());
                yield "files " + files.label();
            }
            case GatewayManagerActionPayload.ACTION_SET_CEILING -> {
                final GatewayPermissions changed = perms.withCeiling(GatewayPermissions.ceilingAt(value));
                g.setPermissions(changed, by, "set priority ceiling " + changed.ceiling().name().toLowerCase(Locale.ROOT));
                yield "priority ceiling " + changed.ceiling().name().toLowerCase(Locale.ROOT);
            }
            case GatewayManagerActionPayload.ACTION_SET_CAP -> {
                final GatewayPermissions changed = perms.withCallCap(GatewayPermissions.capAt(value));
                g.setPermissions(changed, by, "set call cap " + changed.callCap());
                yield changed.callCap() + " calls a tick";
            }
            case GatewayManagerActionPayload.ACTION_CLEAR_BUFFER -> clearBuffer(level, host, g, by);
            case GatewayManagerActionPayload.ACTION_TEST_EVENT -> {
                final int reached = g.bridge() == null ? 0 : g.bridge().sendEvent("jsc_test", g.name());
                g.logged(by, "send test event", reached + " reached", reached > 0 ? GatewayLog.Tone.OK : GatewayLog.Tone.BUSY);
                yield "jsc_test sent to " + reached + (reached == 1 ? " computer" : " computers");
            }
            case GatewayManagerActionPayload.ACTION_TURN_ON, GatewayManagerActionPayload.ACTION_REBOOT,
                 GatewayManagerActionPayload.ACTION_SHUTDOWN -> {
                g.logged(by, actionName(action), "needs the agent", GatewayLog.Tone.DENIED);
                yield "that needs the agent on the ComputerCraft computer, which is not there yet";
            }
            default -> "";
        };
    }

    /**
     * Empties the buffer into the network as INSERT operations, one per slot, signed by the host and the
     * Gateway. Whatever the network cannot hold comes back to the buffer when the operation settles.
     */
    public static String clearBuffer(final ServerLevel level, final BlockEntity host,
                                     final NetworkGatewayBlockEntity g, final String by) {
        final ServerCliComputer shell = host instanceof IComputerTerminalHost terminal
                ? new ServerCliComputer(terminal, level) : null;
        final MainframeBlockEntity mainframe = shell == null ? null : shell.mainframe();
        if (mainframe == null) {
            g.logged(by, "clear buffer", "no mainframe", GatewayLog.Tone.DENIED);
            return "no Mainframe on this network";
        }
        final ItemStackHandler slots = g.buffer();
        final String label = by + " (gateway " + g.name() + ")";
        int moved = 0;
        long items = 0L;
        for (int i = 0; i < slots.getSlots(); i++) {
            final ItemStack held = slots.getStackInSlot(i);
            if (held.isEmpty()) {
                continue;
            }
            final StorageKey key = StorageKey.of(held);
            final int count = held.getCount();
            slots.setStackInSlot(i, ItemStack.EMPTY);
            final NetworkInsertOperation op = mainframe.submitNetworkInsert(key, count, label);
            if (op == null) {
                slots.setStackInSlot(i, held);
                continue;
            }
            final ItemStackHandler back = slots;
            op.onSettle(() -> {
                final long leftover = op.leftover();
                if (leftover > 0L) {
                    returnToBuffer(back, key.stack((int) Math.min(Integer.MAX_VALUE, leftover)));
                }
            });
            moved++;
            items += count;
        }
        g.stats().count(GatewayStats.Kind.OPERATION, level.getGameTime());
        g.logged(by, "clear buffer to network", moved == 0 ? "nothing to move"
                        : items + " items in " + moved + (moved == 1 ? " operation" : " operations"),
                moved == 0 ? GatewayLog.Tone.BUSY : GatewayLog.Tone.OK);
        return moved == 0 ? "the buffer is empty" : items + " items on their way to the network";
    }

    /** Puts what the network would not take back into the first slots with room. */
    private static void returnToBuffer(final ItemStackHandler slots, final ItemStack stack) {
        ItemStack rest = stack;
        for (int i = 0; i < slots.getSlots() && !rest.isEmpty(); i++) {
            rest = slots.insertItem(i, rest, false);
        }
    }

    private static String onOff(final boolean on) {
        return on ? "on" : "off";
    }

    private static String actionName(final int action) {
        return switch (action) {
            case GatewayManagerActionPayload.ACTION_TURN_ON -> "turn on";
            case GatewayManagerActionPayload.ACTION_REBOOT -> "reboot";
            case GatewayManagerActionPayload.ACTION_SHUTDOWN -> "shutdown";
            default -> "action " + action;
        };
    }
}
