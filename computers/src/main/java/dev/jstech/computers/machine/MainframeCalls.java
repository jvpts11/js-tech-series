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
import java.util.Locale;
import java.util.Map;

/**
 * The machine that orchestrates the network, as a program on one of its computers reads it through its
 * {@link MainframeStatsService}. A script watching for a base that is falling behind reads exactly this.
 *
 * <p>Whether there is a Mainframe is a fair question on any machine; everything else needs a network, and asking it
 * of a machine with none stops the program.
 */
final class MainframeCalls {

    private MainframeCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        mainframe(bindings, "Online", (stats, call, target, arguments, line) -> stats.online());
        onNetwork(bindings, "PeakToday", (stats, call, target, arguments, line) -> stats.peakToday());
        onNetwork(bindings, "Stats",
                (stats, call, target, arguments, line) -> shot(stats.stats(kind(arguments))), "string");
        onNetwork(bindings, "Work", (stats, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final ICliComputer.OperationStat stat : stats.work()) {
                all.items().add(shot(stat));
            }
            return all;
        });
    }

    private static void mainframe(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                  final MachineCalls.IServiceFunction<MainframeStatsService> function,
                                  final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::mainframe, "Mainframe", name, function, parameters);
    }

    /** Binds a call or value that needs the machine to be on a network, and stops the program when it is on none. */
    private static void onNetwork(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                  final MachineCalls.IServiceFunction<MainframeStatsService> function,
                                  final String... parameters) {
        mainframe(bindings, name, (stats, call, target, arguments, line) -> {
            if (!stats.onNetwork()) {
                throw new Halt(Halt.Reason.NO_NETWORK, line, MachineCalls.NOT_ON_NETWORK.text().english());
            }
            return function.call(stats, call, target, arguments, line);
        }, parameters);
    }

    private static Values.Obj shot(final ICliComputer.OperationStat stat) {
        final Values.Obj made = new Values.Obj("WorkStat");
        made.set("Type", stat.type().toLowerCase(Locale.ROOT));
        made.set("Count", stat.count());
        made.set("AverageWait", stat.averageWait());
        made.set("AverageRun", stat.averageRun());
        made.set("ShortfallPercent", stat.shortfallPercent());
        made.set("Moved", stat.moved());
        return made;
    }

    /** The kind of Operation a call was asked about. */
    private static String kind(final Object[] arguments) {
        return arguments.length == 0 ? "" : String.valueOf(arguments[0]);
    }
}
