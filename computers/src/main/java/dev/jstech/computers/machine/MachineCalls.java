/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.MemberKind;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The calls the machine a program runs on answers, each bound to the call the system declares for it.
 *
 * <p>A program reaches them through the calls it loaded with, never by their names while it runs. Every binding names a
 * call the system declares as the world's, and that is checked when the bindings are made, so what the machine answers
 * cannot drift from what the compiler lets a program write. A binding says which of the machine's services answers the
 * call, and the Java answering it sees nothing more of the machine than that service.
 *
 * <p>Why a program was stopped is the program's to read, so it is handed over in English, the machine's language.
 */
@TextHolder
final class MachineCalls {

    /** Why a call that needs the network stops a program on a machine that is on none. */
    static final TextKey NOT_ON_NETWORK =
            TextKey.of("jsc.service.machine.not_on_network", "this computer is not on a network");

    private static final TextKey CANNOT_REACH =
            TextKey.of("jsc.service.machine.cannot_reach", "this machine cannot reach %s");

    /** The Java that answers a call with one of the machine's services. */
    @FunctionalInterface
    interface IServiceFunction<S> {
        Object call(S service, IWorldCall call, Object target, Object[] arguments, int line);
    }

    /**
     * One call the machine answers.
     *
     * @param id       the call it answers
     * @param service  which of the machine's services answers it, giving null on a machine that cannot reach it
     * @param function the Java that answers it with that service
     */
    record Binding<S>(MemberId id, Function<MachineServices, S> service, IServiceFunction<S> function) {

        /** The call as that machine answers it, which stops the program when the machine cannot reach the service. */
        IWorldFunction on(final MachineServices services) {
            return (call, target, arguments, line) -> {
                final S reached = this.service.apply(services);
                if (reached == null) {
                    throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, CANNOT_REACH.with(this.id.owner()).english());
                }
                return this.function.call(reached, call, target, arguments, line);
            };
        }
    }

    private static final Map<MemberId, Binding<?>> BINDINGS = build();

    private MachineCalls() {
    }

    /** The binding for that call, or null when the machine does not answer it this way. */
    static Binding<?> find(final MemberId id) {
        return BINDINGS.get(id);
    }

    private static Map<MemberId, Binding<?>> build() {
        final Map<MemberId, Binding<?>> bindings = new HashMap<>();
        FileCalls.bind(bindings);
        ComputerCalls.bind(bindings);
        NetworkCalls.bind(bindings);
        MainframeCalls.bind(bindings);
        OperationsCalls.bind(bindings);
        IqlCalls.bind(bindings);
        ProgramCalls.bind(bindings);
        RemoteComputerCalls.bind(bindings);
        GatewayCalls.bind(bindings);
        return Map.copyOf(bindings);
    }

    /** Binds a call the machine answers with one of its services, once it is sure the system declares it so. */
    static <S> void bind(final Map<MemberId, Binding<?>> bindings, final Function<MachineServices, S> service,
                         final String owner, final String name, final IServiceFunction<S> function,
                         final String... parameters) {
        final MemberId id = new MemberId(owner, name, List.of(parameters));
        final IMemberSpec declared = SystemApi.member(owner, name, id.parameters());
        if (declared == null || declared.kind() != MemberKind.WORLD) {
            throw new IllegalStateException(id.describe() + " is not a call the system declares as the world's");
        }
        if (bindings.putIfAbsent(id, new Binding<>(id, service, function)) != null) {
            throw new IllegalStateException(id.describe() + " is bound twice");
        }
    }
}
