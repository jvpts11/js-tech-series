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
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.signal.Heard;
import dev.jstech.core.signal.IRadio;
import dev.jstech.core.signal.RadioBlockEntity;
import dev.jstech.core.signal.Reading;
import java.util.Map;

/**
 * J's Core's sensors and signals as a program reaches them through its {@link RedstoneService}
 * (computers/docs/design/sigma.md). A sensor or a radio joins the machine through a Redstone Interface linked to it,
 * standing in front of the interface's lens, and is found by the interface's name: Sensors reads what the sensor
 * reads; Signals reads a radio's frequency and value, sends through a transmitter by setting the interface to emit,
 * and hears every frequency heard where the machine stands.
 */
final class SignalCalls {

    private static final String STRING = "string";
    private static final String INT = "int";

    private SignalCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        sensors(bindings);
        signals(bindings);
    }

    private static void sensors(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        redstone(bindings, "Sensors", "Read", (redstone, call, target, arguments, line) -> {
            final RedstoneInterfaceBlockEntity sensor = redstone.named(text(arguments));
            if (sensor == null) {
                return null;
            }
            final Values.ListValue all = new Values.ListValue();
            for (final Reading reading : redstone.sensed(sensor)) {
                final Values.Obj made = new Values.Obj("SensorReading");
                made.set("Name", reading.name().english());
                made.set("Value", reading.value());
                made.set("Least", reading.least());
                made.set("Most", reading.most());
                made.set("Unit", reading.unit() == null ? "" : reading.unit().english());
                all.items().add(made);
            }
            return all;
        }, STRING);
        redstone(bindings, "Sensors", "Names", (redstone, call, target, arguments, line) -> {
            final Values.ListValue names = new Values.ListValue();
            for (final RedstoneInterfaceBlockEntity sensor : redstone.all()) {
                if (!redstone.sensed(sensor).isEmpty()) {
                    names.items().add(sensor.answersTo());
                }
            }
            return names;
        });
    }

    private static void signals(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        redstone(bindings, "Signals", "Radio", (redstone, call, target, arguments, line) -> {
            final RedstoneInterfaceBlockEntity sensor = redstone.named(text(arguments));
            final RadioBlockEntity radio = sensor == null ? null : redstone.radioBefore(sensor);
            if (radio == null) {
                return null;
            }
            final RedstoneService.Radio read = redstone.read(radio);
            final Values.Obj made = new Values.Obj("RadioInfo");
            made.set("Role", read.role());
            made.set("Band", read.band());
            made.set("Frequency", read.frequency());
            made.set("Value", read.value());
            made.set("Clash", read.clash());
            return made;
        }, STRING);
        redstone(bindings, "Signals", "Send", (redstone, call, target, arguments, line) -> {
            final String name = text(arguments);
            final RedstoneInterfaceBlockEntity sensor = redstone.named(name);
            if (sensor == null) {
                throw new Halt(Halt.Reason.NO_OBJECT, line, RedstoneCalls.GONE.with(name));
            }
            final int value = arguments.length > 1 && arguments[1] instanceof Number number ? number.intValue() : 0;
            if (value < 0 || value > RedstoneInterfaceBlockEntity.MAX_STRENGTH) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line, RedstoneCalls.NOT_A_STRENGTH.with(value));
            }
            final RadioBlockEntity radio = redstone.radioBefore(sensor);
            if (radio == null || radio.role() != IRadio.Role.TRANSMITTER) {
                return false;
            }
            sensor.emit(value, RedstoneCalls.by(call));
            return sensor.live();
        }, STRING, INT);
        redstone(bindings, "Signals", "Heard", (redstone, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final Heard one : redstone.heard()) {
                final Values.Obj made = new Values.Obj("HeardSignal");
                made.set("Band", one.frequency().band().serializedName());
                made.set("Frequency", one.frequency().number());
                made.set("Bars", one.bars());
                made.set("Value", one.value());
                made.set("Clash", one.clash());
                made.set("Relayed", one.relayed());
                all.items().add(made);
            }
            return all;
        });
    }

    private static String text(final Object[] arguments) {
        return arguments.length == 0 || arguments[0] == null ? "" : String.valueOf(arguments[0]);
    }

    private static void redstone(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String owner,
                                 final String name, final MachineCalls.IServiceFunction<RedstoneService> function,
                                 final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::redstone, owner, name, function, parameters);
    }
}
