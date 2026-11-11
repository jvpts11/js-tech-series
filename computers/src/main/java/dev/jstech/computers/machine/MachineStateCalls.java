/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * The machine's own readings as a program reads them through its {@link MachineStateService}: Power, Thermal,
 * Wireless and DiskHealth. Each is a glance at what the machine already knows of itself, and the records handed back
 * are pictures taken when they were asked for, as Computer's are.
 */
final class MachineStateCalls {

    private MachineStateCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        power(bindings);
        thermal(bindings);
        wireless(bindings);
        disks(bindings);
    }

    private static void power(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        state(bindings, "Power", "DrawWatts", (state, call, target, arguments, line) -> state.drawWatts());
        state(bindings, "Power", "Supply", (state, call, target, arguments, line) -> {
            final MachineStateService.Supply supply = state.supply();
            if (supply == null) {
                return null;
            }
            final Values.Obj made = new Values.Obj("SupplyInfo");
            made.set("Name", supply.name());
            made.set("RatingWatts", supply.ratingWatts());
            made.set("Units", supply.units());
            made.set("UnitsFailed", supply.unitsFailed());
            made.set("Degraded", supply.degraded());
            made.set("Failed", supply.failed());
            return made;
        });
        state(bindings, "Power", "Ups", (state, call, target, arguments, line) -> {
            final MachineStateService.Ups ups = state.ups();
            if (ups == null) {
                return null;
            }
            final Values.Obj made = new Values.Obj("UpsInfo");
            made.set("Name", ups.name());
            made.set("OnBattery", ups.onBattery());
            made.set("Bypass", ups.bypass());
            made.set("ChargePercent", ups.chargePercent());
            made.set("MinutesLeft", ups.minutesLeft());
            return made;
        });
    }

    private static void thermal(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        state(bindings, "Thermal", "Known", (state, call, target, arguments, line) -> state.thermal().known());
        heat(bindings, "Celsius", ThermalState::celsius);
        heat(bindings, "AmbientCelsius", ThermalState::ambient);
        heat(bindings, "HeatWatts", ThermalState::heatWatts);
        heat(bindings, "BudgetWatts", ThermalState::budgetWatts);
        heat(bindings, "ClockPercent", ThermalState::clockPercent);
        heat(bindings, "LastTripCelsius", ThermalState::lastTrip);
        state(bindings, "Thermal", "Throttled", (state, call, target, arguments, line) -> state.thermal().throttled());
    }

    private static void wireless(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        state(bindings, "Wireless", "Joined", (state, call, target, arguments, line) -> state.wifi().joined());
        state(bindings, "Wireless", "AccessPoint",
                (state, call, target, arguments, line) -> state.wifi().accessPoint());
        state(bindings, "Wireless", "Standard", (state, call, target, arguments, line) -> state.wifi().standard());
        state(bindings, "Wireless", "Speed", (state, call, target, arguments, line) -> state.wifi().speed());
        state(bindings, "Wireless", "Signal",
                (state, call, target, arguments, line) -> state.wifi().signalPercent());
    }

    private static void disks(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        state(bindings, "DiskHealth", "Of", (state, call, target, arguments, line) -> {
            final MachineStateService.DiskCondition disk =
                    state.disk(arguments.length == 0 ? "" : String.valueOf(arguments[0]));
            return disk == null ? null : made(disk);
        }, "string");
        state(bindings, "DiskHealth", "All", (state, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final MachineStateService.DiskCondition disk : state.disks()) {
                all.items().add(made(disk));
            }
            return all;
        });
    }

    private static Values.Obj made(final MachineStateService.DiskCondition disk) {
        final Values.Obj made = new Values.Obj("DiskHealth");
        made.set("Drive", disk.drive());
        made.set("Dead", disk.dead());
        made.set("DamagePercent", disk.damagePercent());
        made.set("BadSectorsPercent", disk.badSectorsPercent());
        made.set("CorruptedItems", disk.corruptedItems());
        made.set("LastDefragTick", disk.lastDefragTick());
        return made;
    }

    /* One whole number of the machine's heat, read off how it stands now. */
    private static void heat(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                             final ToIntFunction<ThermalState> reading) {
        state(bindings, "Thermal", name,
                (state, call, target, arguments, line) -> reading.applyAsInt(state.thermal()));
    }

    private static void state(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String owner,
                              final String name, final MachineCalls.IServiceFunction<MachineStateService> function,
                              final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::state, owner, name, function, parameters);
    }
}
