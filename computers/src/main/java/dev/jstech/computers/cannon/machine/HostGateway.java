/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.machine;

import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.cannon.CannonCosts;
import dev.jstech.computers.cannon.run.Halt;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Values;
import dev.jstech.computers.gateway.GatewayManager;
import dev.jstech.computers.gateway.GatewayRefusedException;
import dev.jstech.computers.gateway.IGatewayBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The Gateways this machine has, and the ComputerCraft side they reach.
 *
 * <p>A Gateway is a device of the machine's, so a program asks the machine for it and the machine answers:
 * which ones there are, what is on the wire of the chosen one, and what a thing on that wire says when it
 * is called. Whether a computer on the other side runs a program of ours is not asked here; that needs the
 * agent, which is its own thing.
 *
 * <p>Everything is charged: reaching across to another mod's computer is the dearest thing a program can
 * do short of asking the network for work, and the prices say so.
 */
public final class HostGateway {

    /** A look at what Gateways there are, which the machine already knows. */
    private static final int GLANCE = CannonCosts.GLANCE;
    /** Gathering what is on the wire, which means asking the other side. */
    private static final int GATHER = CannonCosts.GATHER;
    /** Calling something on the other side, and what each thing handed over adds to it. */
    private static final int CALL = 100;
    private static final int PER_ARGUMENT = 5;
    /** Saying something to a computer over there, or turning one on and off. */
    private static final int SEND = 20;

    private HostGateway() {
    }

    /** Whether this is one of the things answered here. */
    public static boolean handles(final String owner) {
        return "Gateway".equals(owner);
    }

    /**
     * Answers one of them.
     *
     * <p>The first thing handed over is always which Gateway the program chose, or nothing at all for
     * whichever comes first, because a machine may have several and a program says which one it means.
     */
    public static IHost.Reply call(final BlockEntity machine, final String member, final List<Object> arguments,
                                   final int line) {
        final String chosen = arguments.isEmpty() ? "" : String.valueOf(arguments.getFirst());
        final List<Object> rest = arguments.isEmpty() ? List.of() : arguments.subList(1, arguments.size());
        final List<NetworkGatewayBlockEntity> mine = gatewaysOf(machine);
        if ("Online".equals(member)) {
            return IHost.Reply.of(pick(mine, chosen) != null && bridgeOf(pick(mine, chosen)) != null, GLANCE);
        }
        if ("Names".equals(member)) {
            final Values.ListValue names = new Values.ListValue();
            for (final NetworkGatewayBlockEntity gateway : mine) {
                names.items().add(gateway.name());
            }
            return IHost.Reply.of(names, GLANCE);
        }
        final NetworkGatewayBlockEntity gateway = pick(mine, chosen);
        if (gateway == null) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, chosen.isEmpty()
                    ? "this computer has no Gateway" : "this computer has no Gateway called " + chosen);
        }
        if ("Current".equals(member)) {
            return IHost.Reply.of(gateway.name(), GLANCE);
        }
        if ("Select".equals(member)) {
            // Choosing is the program's own doing; the machine only says whether there is such a Gateway.
            return IHost.Reply.of(true, GLANCE);
        }
        final IGatewayBridge bridge = bridgeOf(gateway);
        if (bridge == null) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line,
                    gateway.name() + " has no ComputerCraft side (is ComputerCraft installed?)");
        }
        return switch (member) {
            case "Computers" -> computers(bridge);
            case "Peripherals" -> peripherals(bridge);
            case "Call" -> called(gateway, bridge, rest, line);
            case "TurnOn" -> IHost.Reply.of(bridge.power(whole(rest, 0, line), "on"), SEND);
            case "Shutdown" -> IHost.Reply.of(bridge.power(whole(rest, 0, line), "off"), SEND);
            case "Reboot" -> IHost.Reply.of(bridge.power(whole(rest, 0, line), "again"), SEND);
            case "Send" -> IHost.Reply.of(bridge.eventTo(whole(rest, 0, line), "jsc_message",
                    rest.size() < 2 ? "" : String.valueOf(rest.get(1))), SEND);
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "Gateway has no " + member);
        };
    }

    /** The Gateways linked to this machine, in the order the machine lists them. */
    public static List<NetworkGatewayBlockEntity> gatewaysOf(@Nullable final BlockEntity machine) {
        if (machine == null || !(machine.getLevel() instanceof ServerLevel level)) {
            return List.of();
        }
        return GatewayManager.gatewaysOf(level, machine);
    }

    /** The one the program chose, the first one when it chose none, or null when there is no such Gateway. */
    @Nullable
    private static NetworkGatewayBlockEntity pick(final List<NetworkGatewayBlockEntity> mine,
                                                  final String chosen) {
        if (chosen.isEmpty()) {
            return mine.isEmpty() ? null : mine.getFirst();
        }
        for (final NetworkGatewayBlockEntity gateway : mine) {
            if (gateway.name().equalsIgnoreCase(chosen)) {
                return gateway;
            }
        }
        return null;
    }

    @Nullable
    private static IGatewayBridge bridgeOf(@Nullable final NetworkGatewayBlockEntity gateway) {
        return gateway == null ? null : gateway.bridge();
    }

    private static IHost.Reply computers(final IGatewayBridge bridge) {
        final Values.ListValue all = new Values.ListValue();
        for (final Map<String, Object> row : bridge.computers()) {
            final Values.Obj made = new Values.Obj("CcComputer");
            made.set("Id", row.get("Id"));
            made.set("Name", String.valueOf(row.get("Name")));
            made.set("Label", String.valueOf(row.get("Label")));
            made.set("Online", Boolean.TRUE.equals(row.get("Online")));
            all.items().add(made);
        }
        return IHost.Reply.of(all, GATHER + all.items().size());
    }

    private static IHost.Reply peripherals(final IGatewayBridge bridge) {
        final Values.ListValue all = new Values.ListValue();
        for (final Map.Entry<String, String> entry : bridge.peripherals().entrySet()) {
            final Values.Obj made = new Values.Obj("CcPeripheral");
            made.set("Name", entry.getKey());
            made.set("Type", entry.getValue());
            final Values.ListValue methods = new Values.ListValue();
            methods.items().addAll(bridge.methodsOf(entry.getKey()));
            made.set("Methods", methods);
            all.items().add(made);
        }
        return IHost.Reply.of(all, GATHER + all.items().size());
    }

    /* A call on something out on the wire: the peripheral, the method, and whatever it takes. */
    private static IHost.Reply called(final NetworkGatewayBlockEntity gateway, final IGatewayBridge bridge,
                                      final List<Object> rest, final int line) {
        if (rest.size() < 2) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, "a call names a peripheral and a method");
        }
        final String peripheral = String.valueOf(rest.get(0));
        final String method = String.valueOf(rest.get(1));
        final List<Object> passed = new ArrayList<>(rest.subList(2, rest.size()));
        try {
            final Object answered = bridge.call(peripheral, method, passed);
            gateway.logged("a program", peripheral + "." + method, "called",
                    dev.jstech.computers.gateway.GatewayLog.Tone.OK);
            return IHost.Reply.of(answered, CALL + PER_ARGUMENT * passed.size());
        } catch (final GatewayRefusedException refused) {
            gateway.logged("a program", peripheral + "." + method, refused.getMessage(),
                    dev.jstech.computers.gateway.GatewayLog.Tone.DENIED);
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, refused.getMessage());
        }
    }

    private static int whole(final List<Object> arguments, final int at, final int line) {
        if (at < arguments.size() && arguments.get(at) instanceof Number number) {
            return number.intValue();
        }
        throw new Halt(Halt.Reason.OUT_OF_RANGE, line, "this wants the number of a computer");
    }
}
