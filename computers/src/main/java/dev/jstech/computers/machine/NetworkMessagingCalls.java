/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.vm.program.ChannelNames;
import dev.jstech.computers.vm.program.ComponentValues;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import java.util.Map;

/**
 * What a program says on its network through its {@link NetworkMessagingService}: an entry of the event log, and a
 * value sent on a channel. Both are requests to the network, so a machine on none stops the program; so does a level
 * the log does not have, an entry of more than a line, or a value that cannot travel.
 */
@TextHolder
final class NetworkMessagingCalls {

    private static final String STRING = "string";
    /** The longest entry a program writes to the event log, a line of it. */
    private static final int ENTRY_MOST = 256;

    private static final TextKey NOT_A_LEVEL = TextKey.of("jsc.service.messaging.not_a_level",
            "'%s' is not a level of the event log: info, warn or error");
    private static final TextKey NOT_AN_ENTRY = TextKey.of("jsc.service.messaging.not_an_entry",
            "an entry of the event log is one line of up to %s characters");

    private NetworkMessagingCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        MachineCalls.bind(bindings, MachineServices::messaging, "EventLog", "Write",
                (messaging, call, target, arguments, line) -> {
                    final EventLevel level = levelOf(String.valueOf(arguments[0]), line);
                    final String text = String.valueOf(arguments[1]);
                    if (text.length() > ENTRY_MOST || text.indexOf('\n') >= 0) {
                        throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_AN_ENTRY.with(ENTRY_MOST));
                    }
                    if (!messaging.log(level, text)) {
                        throw new Halt(Halt.Reason.NO_NETWORK, line, MachineCalls.NOT_ON_NETWORK.text());
                    }
                    return null;
                }, STRING, STRING);
        MachineCalls.bind(bindings, MachineServices::messaging, "Channel", "Send",
                (messaging, call, target, arguments, line) -> {
                    final String channel = ChannelNames.checked(String.valueOf(arguments[0]), line);
                    final String value = ComponentValues.travelling(arguments[1], line);
                    if (!messaging.send(channel, value)) {
                        throw new Halt(Halt.Reason.NO_NETWORK, line, MachineCalls.NOT_ON_NETWORK.text());
                    }
                    return null;
                }, STRING, "object");
    }

    /* The level of the log a program names, in any letters, or the program stopped when there is none. */
    private static EventLevel levelOf(final String written, final int line) {
        return switch (written.toLowerCase(Locale.ROOT)) {
            case "info" -> EventLevel.INFO;
            case "warn" -> EventLevel.WARN;
            case "error" -> EventLevel.ERROR;
            default -> throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_LEVEL.with(written));
        };
    }
}
