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
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
 *
 * <p>What it says is declared here. The status line travels to the Gateway Manager as words for now, so it is
 * handed over in English, and the Gateway's log keeps what it is told in English, the machine's language.
 */
@TextHolder
public final class GatewayManager {

    /** How many of the log's lines the Status tab shows under "recent". */
    public static final int RECENT = 4;
    private static final int CC_ID_NONE = -1;

    private static final TextKey NO_PORTS =
            TextKey.of("jsc.service.gateway.no_ports", "this computer has no peripheral ports");
    private static final TextKey SELECT_FIRST =
            TextKey.of("jsc.service.gateway.select_first", "select a gateway first");
    private static final TextKey BLINKING = TextKey.of("jsc.service.gateway.blinking", "%s is blinking");
    private static final TextKey READ_ALLOWED =
            TextKey.of("jsc.service.gateway.read_allowed", "reading the network allowed");
    private static final TextKey READ_DENIED =
            TextKey.of("jsc.service.gateway.read_denied", "reading the network denied");
    private static final TextKey OPERATIONS_ALLOWED =
            TextKey.of("jsc.service.gateway.operations_allowed", "operations allowed");
    private static final TextKey OPERATIONS_DENIED =
            TextKey.of("jsc.service.gateway.operations_denied", "operations denied");
    private static final TextKey CEILING = TextKey.of("jsc.service.gateway.ceiling", "priority ceiling %s");
    private static final TextKey CALLS_A_TICK = TextKey.of("jsc.service.gateway.calls_a_tick", "%s calls a tick");
    private static final TextKey TEST_SENT_ONE =
            TextKey.of("jsc.service.gateway.test_sent_one", "jsc_test sent to %s computer");
    private static final TextKey TEST_SENT_MANY =
            TextKey.of("jsc.service.gateway.test_sent_many", "jsc_test sent to %s computers");
    private static final TextKey NEEDS_AGENT = TextKey.of("jsc.service.gateway.needs_agent",
            "that needs the agent on the ComputerCraft computer, which is not there yet");
    private static final TextKey NO_MAINFRAME =
            TextKey.of("jsc.service.gateway.no_mainframe", "no Mainframe on this network");
    private static final TextKey BUFFER_EMPTY = TextKey.of("jsc.service.gateway.buffer_empty", "the buffer is empty");
    private static final TextKey ON_THEIR_WAY =
            TextKey.of("jsc.service.gateway.on_their_way", "%s items on their way to the network");
    private static final TextKey SECONDS_AGO = TextKey.of("jsc.service.gateway.seconds_ago", "%s s ago");
    private static final TextKey MINUTES_AGO = TextKey.of("jsc.service.gateway.minutes_ago", "%s min ago");
    private static final TextKey HOURS_AGO = TextKey.of("jsc.service.gateway.hours_ago", "%s h ago");
    private static final TextKey WHERE = TextKey.of("jsc.service.gateway.where", "at %s, %s, %s · %s");

    /* What the Gateway's log is told: what was done, and how it went. */
    private static final TextKey LOG_SET_READ = TextKey.of("jsc.service.gateway.log.set_read", "set read %s");
    private static final TextKey LOG_SET_OPERATIONS =
            TextKey.of("jsc.service.gateway.log.set_operations", "set operations %s");
    private static final TextKey LOG_SET_CEILING =
            TextKey.of("jsc.service.gateway.log.set_ceiling", "set priority ceiling %s");
    private static final TextKey LOG_SET_CAP = TextKey.of("jsc.service.gateway.log.set_cap", "set call cap %s");
    private static final TextKey LOG_TEST_EVENT = TextKey.of("jsc.service.gateway.log.test_event", "send test event");
    private static final TextKey LOG_REACHED = TextKey.of("jsc.service.gateway.log.reached", "%s reached");
    private static final TextKey LOG_NEEDS_AGENT = TextKey.of("jsc.service.gateway.log.needs_agent", "needs the agent");
    private static final TextKey LOG_CLEAR_BUFFER = TextKey.of("jsc.service.gateway.log.clear_buffer", "clear buffer");
    private static final TextKey LOG_NO_MAINFRAME = TextKey.of("jsc.service.gateway.log.no_mainframe", "no mainframe");
    private static final TextKey LOG_CLEAR_TO_NETWORK =
            TextKey.of("jsc.service.gateway.log.clear_to_network", "clear buffer to network");
    private static final TextKey LOG_NOTHING_TO_MOVE =
            TextKey.of("jsc.service.gateway.log.nothing_to_move", "nothing to move");
    private static final TextKey LOG_MOVED_ONE =
            TextKey.of("jsc.service.gateway.log.moved_one", "%s items in %s operation");
    private static final TextKey LOG_MOVED_MANY =
            TextKey.of("jsc.service.gateway.log.moved_many", "%s items in %s operations");
    private static final TextKey LOG_TURN_ON = TextKey.of("jsc.service.gateway.log.turn_on", "turn on");

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
        return WHERE.with(p.getX(), p.getY(), p.getZ(), gateway.linkKind()).english();
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
            log.add(new WireLog(GatewayLog.clock(e.dayTime()), e.who(), e.what(), e.result(), e.tone().id()));
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
        return new Detail(g.getBlockPos().asLong(), g.name(), g.linkKind(), g.online(),
                net == null ? 0 : net.indexedTypes(), net == null ? 0 : net.servers(),
                mainframe != null && mainframe.isRunning(), g.budgetPermille(),
                g.ccOnline(), bridge == null ? 0 : bridge.computersOnWire(), bridge == null ? 0 : bridge.devicesOnWire(),
                g.stats().lastMinute(GatewayStats.Kind.CALL, now),
                g.stats().lastMinute(GatewayStats.Kind.OPERATION, now),
                g.peripheralName(), CC_ID_NONE, buffer, recent,
                perms.read(), perms.operations(), perms.ceilingIndex(), perms.capIndex(),
                computers, log);
    }

    /** "2 s ago", "4 min ago", for the computers table. */
    public static String ago(final long now, final long then) {
        final long seconds = Math.max(0L, now - then) / 20L;
        if (seconds < 60L) {
            return SECONDS_AGO.with(seconds).english();
        }
        final long minutes = seconds / 60L;
        return minutes < 60L ? MINUTES_AGO.with(minutes).english() : HOURS_AGO.with(minutes / 60L).english();
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
            return NO_PORTS.text().english();
        }
        final NetworkGatewayBlockEntity g = NetworkGateways.at(level, owner, gatewayPos);
        if (g == null) {
            return SELECT_FIRST.text().english();
        }
        final String by = hostNameOf(host);
        final GatewayPermissions perms = g.permissions();
        return switch (action) {
            case GatewayManagerActionPayload.ACTION_RENAME -> g.rename(text, by);
            case GatewayManagerActionPayload.ACTION_IDENTIFY -> {
                g.identify(by);
                yield BLINKING.with(g.name()).english();
            }
            case GatewayManagerActionPayload.ACTION_SET_READ -> {
                g.setPermissions(perms.withRead(value != 0), by, LOG_SET_READ.with(onOff(value != 0)).english());
                yield (value != 0 ? READ_ALLOWED : READ_DENIED).text().english();
            }
            case GatewayManagerActionPayload.ACTION_SET_OPERATIONS -> {
                g.setPermissions(perms.withOperations(value != 0), by,
                        LOG_SET_OPERATIONS.with(onOff(value != 0)).english());
                yield (value != 0 ? OPERATIONS_ALLOWED : OPERATIONS_DENIED).text().english();
            }
            case GatewayManagerActionPayload.ACTION_SET_CEILING -> {
                final GatewayPermissions changed = perms.withCeiling(GatewayPermissions.ceilingAt(value));
                final String ceiling = changed.ceiling().name().toLowerCase(Locale.ROOT);
                g.setPermissions(changed, by, LOG_SET_CEILING.with(ceiling).english());
                yield CEILING.with(ceiling).english();
            }
            case GatewayManagerActionPayload.ACTION_SET_CAP -> {
                final GatewayPermissions changed = perms.withCallCap(GatewayPermissions.capAt(value));
                g.setPermissions(changed, by, LOG_SET_CAP.with(changed.callCap()).english());
                yield CALLS_A_TICK.with(changed.callCap()).english();
            }
            case GatewayManagerActionPayload.ACTION_CLEAR_BUFFER -> clearBuffer(level, host, g, by);
            case GatewayManagerActionPayload.ACTION_TEST_EVENT -> {
                final int reached = g.bridge() == null ? 0 : g.bridge().sendEvent("jsc_test", g.name());
                g.logged(by, LOG_TEST_EVENT.text().english(), LOG_REACHED.with(reached).english(),
                        reached > 0 ? GatewayLog.Tone.OK : GatewayLog.Tone.BUSY);
                yield (reached == 1 ? TEST_SENT_ONE : TEST_SENT_MANY).with(reached).english();
            }
            case GatewayManagerActionPayload.ACTION_TURN_ON, GatewayManagerActionPayload.ACTION_REBOOT,
                 GatewayManagerActionPayload.ACTION_SHUTDOWN -> {
                g.logged(by, actionName(action), LOG_NEEDS_AGENT.text().english(), GatewayLog.Tone.DENIED);
                yield NEEDS_AGENT.text().english();
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
            g.logged(by, LOG_CLEAR_BUFFER.text().english(), LOG_NO_MAINFRAME.text().english(),
                    GatewayLog.Tone.DENIED);
            return NO_MAINFRAME.text().english();
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
        g.logged(by, LOG_CLEAR_TO_NETWORK.text().english(), moved == 0 ? LOG_NOTHING_TO_MOVE.text().english()
                        : (moved == 1 ? LOG_MOVED_ONE : LOG_MOVED_MANY).with(items, moved).english(),
                moved == 0 ? GatewayLog.Tone.BUSY : GatewayLog.Tone.OK);
        return moved == 0 ? BUFFER_EMPTY.text().english() : ON_THEIR_WAY.with(items).english();
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
            case GatewayManagerActionPayload.ACTION_TURN_ON -> LOG_TURN_ON.text().english();
            case GatewayManagerActionPayload.ACTION_REBOOT -> "reboot";
            case GatewayManagerActionPayload.ACTION_SHUTDOWN -> "shutdown";
            default -> "action " + action;
        };
    }
}
