/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import java.util.Map;

/**
 * The data network, as a program on one of its computers reads it through its {@link NetworkReadService}.
 *
 * <p>Whether there is a network, and which, is a fair question on any machine. Everything else needs one: asking it
 * of a machine with no cable stops the program, which is the honest answer to a question that has none. Reading the
 * network is far dearer than the program's own arithmetic, and a read that brings back rows is priced by how many.
 */
final class NetworkCalls {

    private static final String STRING = "string";

    private NetworkCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        network(bindings, "Online", (net, call, target, arguments, line) -> net.online());
        network(bindings, "Current", (net, call, target, arguments, line) -> net.current());
        onNetwork(bindings, "Capacity", (net, call, target, arguments, line) -> net.capacity());
        onNetwork(bindings, "Used", (net, call, target, arguments, line) -> net.used());
        onNetwork(bindings, "Total", (net, call, target, arguments, line) -> net.total(name(arguments)), STRING);
        onNetwork(bindings, "Types", (net, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            all.items().addAll(net.types());
            return all;
        });
        onNetwork(bindings, "Find", (net, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final ICliComputer.Holding holding : net.find(name(arguments))) {
                final Values.Obj made = new Values.Obj("HoldingInfo");
                made.set("Server", holding.server());
                made.set("Quantity", holding.quantity());
                all.items().add(made);
            }
            return all;
        }, STRING);
        onNetwork(bindings, "Servers", (net, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final ICliComputer.ServerUse row : net.servers()) {
                final Values.Obj made = new Values.Obj("ServerInfo");
                made.set("Name", row.name());
                made.set("Stored", row.stored());
                made.set("Capacity", row.capacity());
                all.items().add(made);
            }
            return all;
        });
        onNetwork(bindings, "Computers", (net, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final ICliComputer.RemoteHost host : net.computers()) {
                all.items().add(remote(host));
            }
            return all;
        });
        onNetwork(bindings, "Computer", (net, call, target, arguments, line) -> {
            final ICliComputer.RemoteHost host = net.computer(name(arguments));
            return host == null ? null : remote(host);
        }, STRING);
    }

    private static void network(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                final MachineCalls.IServiceFunction<NetworkReadService> function,
                                final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::network, "Network", name, function, parameters);
    }

    /** Binds a call or value that needs the machine to be on a network, and stops the program when it is on none. */
    private static void onNetwork(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                  final MachineCalls.IServiceFunction<NetworkReadService> function,
                                  final String... parameters) {
        network(bindings, name, (net, call, target, arguments, line) -> {
            if (!net.online()) {
                throw new Halt(Halt.Reason.NO_NETWORK, line, MachineCalls.NOT_ON_NETWORK.text());
            }
            return function.call(net, call, target, arguments, line);
        }, parameters);
    }

    /** What a program holds another computer by: its host name, and a picture of what it is. */
    private static Values.Obj remote(final ICliComputer.RemoteHost host) {
        final Values.Obj made = new Values.Obj("RemoteComputer");
        made.set("Host", host.hostname());
        made.set("Name", host.name().isEmpty() ? host.hostname() : host.name());
        made.set("Type", host.type());
        made.set("Os", host.os());
        made.set("Online", host.running());
        return made;
    }

    /** The item or computer a call was asked about. */
    private static String name(final Object[] arguments) {
        return arguments.length == 0 ? "" : String.valueOf(arguments[0]);
    }
}
