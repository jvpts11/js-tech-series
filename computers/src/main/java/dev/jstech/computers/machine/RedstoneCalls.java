/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

import java.util.Map;

/**
 * The calls a program makes on the Redstone Interfaces of its machine, answered by its {@link RedstoneService}: one
 * found by its name, set to read or to emit, and the strength it reads or emits. A setting is marked on the interface
 * with the program's name, as on a bus. An interface no longer linked, or a strength redstone does not have, stops the
 * program, saying why.
 */
@TextHolder
final class RedstoneCalls {

    private static final String STRING = "string";
    private static final String INT = "int";
    private static final String OWNER = "Redstone";
    /** What a setting is marked with when the program has no name to give. */
    private static final String UNNAMED = "Sigma";

    private static final TextKey GONE = TextKey.of("jsc.service.redstone.gone",
            "no Redstone Interface named '%s' is linked to this machine");
    private static final TextKey NOT_A_STRENGTH = TextKey.of("jsc.service.redstone.not_a_strength",
            "%s is not a redstone strength, which runs from 0 to 15");

    private RedstoneCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        MachineCalls.bind(bindings, MachineServices::redstone, OWNER, "Named",
                (redstone, call, target, arguments, line) -> {
                    final RedstoneInterfaceBlockEntity sensor = redstone.named(text(arguments, 0));
                    return sensor == null ? null : made(sensor.answersTo());
                }, STRING);
        MachineCalls.bind(bindings, MachineServices::redstone, OWNER, "In",
                (redstone, call, target, arguments, line) -> {
                    reached(redstone, target, line).read(by(call));
                    return target;
                });
        MachineCalls.bind(bindings, MachineServices::redstone, OWNER, "Out",
                (redstone, call, target, arguments, line) -> {
                    final int strength = number(arguments, 0);
                    if (strength < 0 || strength > RedstoneInterfaceBlockEntity.MAX_STRENGTH) {
                        throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_STRENGTH.with(strength));
                    }
                    reached(redstone, target, line).emit(strength, by(call));
                    return target;
                }, INT);
        MachineCalls.bind(bindings, MachineServices::redstone, OWNER, "Level",
                (redstone, call, target, arguments, line) -> reached(redstone, target, line).shownStrength());
    }

    private static Values.Obj made(final String name) {
        final Values.Obj made = new Values.Obj(OWNER);
        made.set("Name", name);
        return made;
    }

    /* The interface the call is made on, or the program stopped when it is no longer linked to the machine. */
    private static RedstoneInterfaceBlockEntity reached(final RedstoneService redstone, final Object target,
                                                        final int line) {
        final String name = target instanceof Values.Obj held && held.get("Name") instanceof String named ? named : "";
        final RedstoneInterfaceBlockEntity sensor = redstone.named(name);
        if (sensor == null) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, GONE.with(name));
        }
        return sensor;
    }

    /* What a setting is marked with: the class the program was started from. */
    private static String by(final IWorldCall call) {
        final String caller = call.caller();
        return caller == null || caller.isEmpty() ? UNNAMED : caller;
    }

    private static String text(final Object[] arguments, final int at) {
        return arguments.length <= at ? "" : String.valueOf(arguments[at]);
    }

    private static int number(final Object[] arguments, final int at) {
        return arguments.length > at && arguments[at] instanceof Number value ? value.intValue() : 0;
    }
}
