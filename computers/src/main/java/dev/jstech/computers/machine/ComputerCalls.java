/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import java.util.Map;

/**
 * The machine a program runs on, as the program reads it through its {@link ComputerInfoService}.
 *
 * <p>What the machine is costs a glance; what it holds is gathered, since the answer is a list the machine walks to
 * build. The little records handed back are ordinary objects to the program, which holds them and pays for them like
 * anything else, and they are pictures taken when they were asked for, not live views.
 */
final class ComputerCalls {

    private ComputerCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        computer(bindings, "Name", (info, call, target, arguments, line) -> info.name());
        computer(bindings, "Cpu", (info, call, target, arguments, line) -> {
            final ComputerInfoService.Cpu cpu = info.cpu();
            final Values.Obj made = new Values.Obj("CpuInfo");
            made.set("Mhz", cpu.mhz());
            made.set("Cores", cpu.cores());
            made.set("Era", cpu.era());
            made.set("Architecture", cpu.architecture());
            return made;
        });
        computer(bindings, "Os", (info, call, target, arguments, line) -> {
            final ComputerInfoService.Os os = info.os();
            final Values.Obj made = new Values.Obj("OsInfo");
            made.set("Id", os.id());
            made.set("Name", os.name());
            return made;
        });
        computer(bindings, "RamMb", (info, call, target, arguments, line) -> info.ramMb());
        computer(bindings, "FreeRamMb", (info, call, target, arguments, line) -> info.freeRamMb());
        computer(bindings, "Online", (info, call, target, arguments, line) -> info.online());
        computer(bindings, "Disks", (info, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final ComputerInfoService.Disk disk : info.disks()) {
                final Values.Obj made = new Values.Obj("DiskInfo");
                made.set("Mount", disk.mount());
                made.set("CapacityMb", disk.capacityMb());
                made.set("UsedMb", disk.usedMb());
                all.items().add(made);
            }
            return all;
        });
        computer(bindings, "Programs", (info, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            all.items().addAll(info.programs());
            return all;
        });
        computer(bindings, "Processes", (info, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final ComputerInfoService.Running one : info.running()) {
                final Values.Obj made = new Values.Obj("ProcessInfo");
                made.set("Id", one.id());
                made.set("Name", one.name());
                made.set("State", one.state());
                made.set("HeldBytes", one.heldBytes());
                all.items().add(made);
            }
            return all;
        });
    }

    private static void computer(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                 final MachineCalls.IServiceFunction<ComputerInfoService> function) {
        MachineCalls.bind(bindings, MachineServices::computer, "Computer", name, function);
    }
}
