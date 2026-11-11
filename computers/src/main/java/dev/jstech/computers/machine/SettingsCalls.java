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
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * A program's own settings as it reads and writes them through its {@link ProgramSettingsService}: each kept under the
 * program's name, in the file its system keeps them in. A name that is not a plain word, or a value of more than one
 * line, stops the program, saying why; a machine with no system keeps nothing, and gives back what was asked for.
 */
@TextHolder
final class SettingsCalls {

    private static final String STRING = "string";
    private static final String OWNER = "Settings";

    private static final TextKey NOT_A_NAME = TextKey.of("jsc.service.settings.not_a_name",
            "'%s' is not a setting's name: letters, digits, dots, dashes and underscores, up to %s");
    private static final TextKey NOT_A_VALUE = TextKey.of("jsc.service.settings.not_a_value",
            "a setting holds one line of up to %s characters");

    private SettingsCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        settings(bindings, "Get", (settings, call, target, arguments, line) -> {
            final String name = name(arguments, line);
            final String fallback = text(arguments, 1);
            final ProgramSettingsService.Place place = placeOf(settings, call);
            return place == null ? fallback : settings.read(place, program(call)).getOrDefault(name, fallback);
        }, STRING, STRING);
        settings(bindings, "Set", (settings, call, target, arguments, line) -> {
            final String value = text(arguments, 1);
            if (!SettingsFormat.validValue(value)) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_VALUE.with(SettingsFormat.VALUE_MOST));
            }
            return change(settings, call, name(arguments, line), value);
        }, STRING, STRING);
        settings(bindings, "Remove", (settings, call, target, arguments, line) ->
                change(settings, call, name(arguments, line), null), STRING);
        settings(bindings, "Names", (settings, call, target, arguments, line) -> {
            final Values.ListValue names = new Values.ListValue();
            final ProgramSettingsService.Place place = placeOf(settings, call);
            if (place != null) {
                names.items().addAll(settings.read(place, program(call)).keySet());
            }
            return names;
        });
        settings(bindings, "Path", (settings, call, target, arguments, line) -> {
            final ProgramSettingsService.Place place = placeOf(settings, call);
            return place == null ? "" : settings.shown(place);
        });
    }

    /* Sets the setting {@code name} to {@code value}, or takes it away for null; false when nothing could be kept. */
    private static boolean change(final ProgramSettingsService settings, final IWorldCall call, final String name,
                                  @Nullable final String value) {
        final ProgramSettingsService.Place place = placeOf(settings, call);
        if (place == null) {
            return false;
        }
        final Map<String, String> all = new LinkedHashMap<>(settings.read(place, program(call)));
        if (value == null) {
            if (all.remove(name) == null) {
                return false;
            }
        } else {
            all.put(name, value);
        }
        return settings.write(place, program(call), all);
    }

    @Nullable
    private static ProgramSettingsService.Place placeOf(final ProgramSettingsService settings, final IWorldCall call) {
        return settings.placeOf(program(call), settings.isService(call.callerId()));
    }

    /* The name the asking program's settings are kept under. */
    private static String program(final IWorldCall call) {
        return ProgramSettingsService.programName(call.caller());
    }

    /* The setting's name the call was given, or the program stopped when it is not one. */
    private static String name(final Object[] arguments, final int line) {
        final String name = text(arguments, 0);
        if (!SettingsFormat.validName(name)) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_NAME.with(name, SettingsFormat.NAME_MOST));
        }
        return name;
    }

    private static String text(final Object[] arguments, final int at) {
        return arguments.length <= at || arguments[at] == null ? "" : String.valueOf(arguments[at]);
    }

    private static void settings(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                 final MachineCalls.IServiceFunction<ProgramSettingsService> function,
                                 final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::settings, OWNER, name, function, parameters);
    }
}
