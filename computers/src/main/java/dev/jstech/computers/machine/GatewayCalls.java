/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gateway.GatewayLog;
import dev.jstech.computers.gateway.GatewayRefusedException;
import dev.jstech.computers.gateway.IGatewayBridge;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The machine's Gateways and the ComputerCraft side they reach, from a program, through its computer's
 * {@link GatewayBridgeService}.
 *
 * <p>A call goes through the Gateway the program chose, or the first the machine lists when it chose none. Whether that
 * Gateway is online and what the machine's Gateways are called can be asked of any machine; everything else stops the
 * program on a machine without that Gateway, and everything that reaches across stops it on a Gateway with no
 * ComputerCraft side. A call on something out on the wire is written into that Gateway's log, answered or refused.
 * Why a program was stopped is the program's to read, so it is handed over in English, the machine's language.
 */
@TextHolder
final class GatewayCalls {

    private static final TextKey NO_CC_SIDE = TextKey.of("jsc.service.gateway.no_cc_side",
            "%s has no ComputerCraft side (is ComputerCraft installed?)");
    private static final TextKey NO_GATEWAY =
            TextKey.of("jsc.service.gateway.no_gateway", "this computer has no Gateway");
    private static final TextKey NO_GATEWAY_CALLED =
            TextKey.of("jsc.service.gateway.no_gateway_called", "this computer has no Gateway called %s");

    private static final String STRING = "string";
    private static final String OBJECT = "object";
    private static final String LONG = "long";
    /** Who a call on something out on the wire is written down as in the Gateway's log. */
    private static final String PROGRAM = "a program";

    /** The Java that answers a call with the Gateway the program chose in hand. */
    @FunctionalInterface
    private interface IGatewayFunction {
        Object call(NetworkGatewayBlockEntity gateway, Object[] arguments, int line);
    }

    /** The Java that answers a call with the chosen Gateway and its ComputerCraft side in hand. */
    @FunctionalInterface
    private interface IBridgeFunction {
        Object call(NetworkGatewayBlockEntity gateway, IGatewayBridge bridge, Object[] arguments, int line);
    }

    private GatewayCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        gateways(bindings, "Online", (gateways, call, target, arguments, line) -> {
            /*
             * Whether the Gateway is there and reaching its computer, which is a question about this mod's own
             * block. Asking its bridge instead made a Gateway that is present, named and linked answer "no" for
             * no better reason than the optional mod on the other side not being installed.
             */
            final NetworkGatewayBlockEntity gateway = gateways.pick(call.gateway());
            return gateway != null && gateway.online();
        });
        gateways(bindings, "Names", (gateways, call, target, arguments, line) -> {
            final Values.ListValue names = new Values.ListValue();
            for (final NetworkGatewayBlockEntity gateway : gateways.all()) {
                names.items().add(gateway.name());
            }
            return names;
        });
        gateways(bindings, "Select", (gateways, call, target, arguments, line) -> {
            final String named = String.valueOf(arguments[0]);
            held(gateways, named, line);
            // Choosing is the program's own doing; the machine only says whether there is such a Gateway.
            call.chooseGateway(named);
            return true;
        }, STRING);
        chosen(bindings, "Current", (gateway, arguments, line) -> gateway.name());
        across(bindings, "Computers", (gateway, bridge, arguments, line) -> computers(bridge));
        across(bindings, "Peripherals", (gateway, bridge, arguments, line) -> peripherals(bridge));
        across(bindings, "Call", GatewayCalls::called, STRING, STRING);
        across(bindings, "Call", GatewayCalls::called, STRING, STRING, OBJECT);
        across(bindings, "Call", GatewayCalls::called, STRING, STRING, OBJECT, OBJECT);
        across(bindings, "Call", GatewayCalls::called, STRING, STRING, OBJECT, OBJECT, OBJECT);
        across(bindings, "TurnOn", power("on"), LONG);
        across(bindings, "Shutdown", power("off"), LONG);
        across(bindings, "Reboot", power("again"), LONG);
        across(bindings, "Send", (gateway, bridge, arguments, line) ->
                bridge.eventTo(ProgramCalls.number(arguments, 0), "jsc_message", ProgramCalls.text(arguments, 1)),
                LONG, STRING);
    }

    /** Turns a computer on the wire on, off, or off and on again, by the number it is handed. */
    private static IBridgeFunction power(final String what) {
        return (gateway, bridge, arguments, line) -> bridge.power(ProgramCalls.number(arguments, 0), what);
    }

    /** Binds a call the machine's Gateways answer, whether or not the one the program chose is there. */
    private static void gateways(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                 final MachineCalls.IServiceFunction<GatewayBridgeService> function,
                                 final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::gateways, "Gateway", name, function, parameters);
    }

    /** Binds a call on the Gateway the program chose, which stops the program when there is no such Gateway. */
    private static void chosen(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                               final IGatewayFunction function, final String... parameters) {
        gateways(bindings, name, (gateways, call, target, arguments, line) ->
                function.call(held(gateways, call.gateway(), line), arguments, line), parameters);
    }

    /** Binds a call across the chosen Gateway, which stops the program when it has no ComputerCraft side. */
    private static void across(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                               final IBridgeFunction function, final String... parameters) {
        chosen(bindings, name, (gateway, arguments, line) -> {
            final IGatewayBridge bridge = gateway.bridge();
            if (bridge == null) {
                throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, NO_CC_SIDE.with(gateway.name()));
            }
            return function.call(gateway, bridge, arguments, line);
        }, parameters);
    }

    /** The Gateway of that name, or the first one for no name; a halt when the machine has no such Gateway. */
    private static NetworkGatewayBlockEntity held(final GatewayBridgeService gateways, final String chosen,
                                                  final int line) {
        final NetworkGatewayBlockEntity gateway = gateways.pick(chosen);
        if (gateway == null) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, chosen.isEmpty()
                    ? NO_GATEWAY.text() : NO_GATEWAY_CALLED.with(chosen));
        }
        return gateway;
    }

    private static Values.ListValue computers(final IGatewayBridge bridge) {
        final Values.ListValue all = new Values.ListValue();
        for (final Map<String, Object> row : bridge.computers()) {
            final Values.Obj made = new Values.Obj("CcComputer");
            made.set("Id", row.get("Id"));
            made.set("Name", String.valueOf(row.get("Name")));
            made.set("Label", String.valueOf(row.get("Label")));
            made.set("Online", Boolean.TRUE.equals(row.get("Online")));
            all.items().add(made);
        }
        return all;
    }

    private static Values.ListValue peripherals(final IGatewayBridge bridge) {
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
        return all;
    }

    /* A call on something out on the wire: the peripheral, the method, and whatever that method takes. */
    private static Object called(final NetworkGatewayBlockEntity gateway, final IGatewayBridge bridge,
                                 final Object[] arguments, final int line) {
        final String peripheral = String.valueOf(arguments[0]);
        final String method = String.valueOf(arguments[1]);
        final List<Object> passed = new ArrayList<>(Arrays.asList(arguments).subList(2, arguments.length));
        try {
            final Object answered = bridge.call(peripheral, method, passed);
            gateway.logged(PROGRAM, peripheral + "." + method, "called", GatewayLog.Tone.OK);
            return answered;
        } catch (final GatewayRefusedException refused) {
            gateway.logged(PROGRAM, peripheral + "." + method, refused.getMessage(), GatewayLog.Tone.DENIED);
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, refused.text());
        }
    }
}
