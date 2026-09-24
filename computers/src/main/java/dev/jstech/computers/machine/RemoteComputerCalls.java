/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;

/**
 * Another computer on the network, as a running program reaches it through its computer's
 * {@link RemoteComputerService}.
 *
 * <p>Every call is made on the computer the program holds, by the host name it carries. The other computer being gone,
 * switched off, or set not to take programs from other computers each stops the program with a reason of its own.
 * The reason is the program's to read, so it is handed over in English, the machine's language.
 */
@TextHolder
final class RemoteComputerCalls {

    private static final String STRING = "string";
    private static final String STRINGS = "List<string>";

    private static final TextKey CANNOT_RUN = TextKey.of("jsc.service.remote.cannot_run", "%s cannot run programs");
    private static final TextKey NO_SUCH_COMPUTER =
            TextKey.of("jsc.service.remote.no_such_computer", "%s: no such computer on this network");
    private static final TextKey HOST_OFF = TextKey.of("jsc.service.remote.host_off", "%s is powered off");
    private static final TextKey NO_REMOTE_PROGRAMS = TextKey.of("jsc.service.remote.no_remote_programs",
            "%s does not take programs from other computers");

    /** The Java that answers a call with the other computer in hand. */
    @FunctionalInterface
    private interface IRemoteFunction {
        Object call(RemoteComputerService remotes, ServerCliComputer remote, String host, IWorldCall call,
                    Object[] arguments, int line);
    }

    private RemoteComputerCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        final IRemoteFunction start = (remotes, remote, host, call, arguments, line) -> {
            final String path = ProgramCalls.text(arguments, 0);
            final ProgramLauncher.Launch launch = remotes.start(remote, path, ProgramCalls.strings(arguments, 1),
                    remotes.parentOf(call.callerId()), ProgramCalls.priority(arguments));
            if (launch == null) {
                throw new Halt(Halt.Reason.CANNOT_START, line, CANNOT_RUN.with(host).english());
            }
            if (!launch.ok()) {
                throw new Halt(Halt.Reason.CANNOT_START, line, ProgramCalls.refusal(path, host, launch));
            }
            return ProgramCalls.handle(launch.id(), launch.name(), host);
        };
        remote(bindings, "Start", start, STRING);
        remote(bindings, "Start", start, STRING, STRINGS);
        remote(bindings, "Start", start, STRING, STRINGS, STRING);
        remote(bindings, "Shell", (remotes, remote, host, call, arguments, line) -> {
            final Values.ListValue lines = new Values.ListValue();
            lines.items().addAll(remotes.shell(remote, ProgramCalls.text(arguments, 0)));
            return lines;
        }, STRING);
        remote(bindings, "Send", (remotes, remote, host, call, arguments, line) -> remotes.send(remote,
                call.callerId(), ProgramCalls.number(arguments, 0), ProgramCalls.text(arguments, 1)), "int", STRING);
        remote(bindings, "Processes", (remotes, remote, host, call, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final ProgramView one : remotes.processes(remote)) {
                all.items().add(ProgramCalls.handle(one.id(), one.file(), host));
            }
            return all;
        });
    }

    /** Binds a call on another computer, which first has to be there, switched on and willing. */
    private static void remote(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                               final IRemoteFunction function, final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::remotes, "RemoteComputer", name,
                (remotes, call, target, arguments, line) -> {
                    final String host = target instanceof Values.Obj held && held.get("Host") instanceof String named
                            ? named : "";
                    final ServerCliComputer remote = remotes.find(host);
                    if (remote == null) {
                        throw new Halt(Halt.Reason.NO_OBJECT, line, NO_SUCH_COMPUTER.with(host).english());
                    }
                    if (!remote.running()) {
                        throw new Halt(Halt.Reason.REFUSED, line, HOST_OFF.with(host).english());
                    }
                    if (!remote.remoteAllowed()) {
                        throw new Halt(Halt.Reason.REFUSED, line, NO_REMOTE_PROGRAMS.with(host).english());
                    }
                    return function.call(remotes, remote, host, call, arguments, line);
                }, parameters);
    }
}
